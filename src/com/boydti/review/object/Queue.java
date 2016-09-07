package com.boydti.review.object;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;

import com.boydti.review.ByteReview;
import com.intellectualcrafters.plot.commands.DebugExec;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.RunnableVal;
import com.intellectualcrafters.plot.util.StringMan;
import com.intellectualcrafters.plot.util.TaskManager;

public class Queue {
    public final HashSet<String> areas;
    public final HashSet<String> servers;
    public final String name;
    public final ArrayList<String> commands;
    
    public boolean isServerAllowed(String server) {
        return (this.servers.size() == 0 || this.servers.contains(server)) || (this.servers.contains("*"));
    }
    
    public boolean isAreaAllowed(PlotArea area) {
        return (this.areas.size() == 0 || this.areas.contains(area.toString())) || this.areas.contains(area.worldname) || (this.areas.contains("*"));
    }
    
    public void isPlotAllowed(final Plot plot, final RunnableVal<Object> whenDone) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                File file = new File(ByteReview.plugin.getDataFolder() + File.separator + "checks" + File.separator + name + ".js");
                if (file.exists()) {
                    try {
                        DebugExec exec = (DebugExec) MainCommand.getInstance().getCommand("debugexec");
                        String lines = StringMan.join(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8), System.getProperty("line.separator"));
                        exec.getScope().put("Plot", plot);
                        whenDone.value = exec.getEngine().eval(lines, exec.getScope());
                        exec.getScope().remove("Plot");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    whenDone.value = true;
                }
                whenDone.run();
            }
        });
    }
    
    public Queue(String name) {
        this.name = name;
        this.servers = new HashSet();
        this.areas = new HashSet();
        this.commands = new ArrayList();
    }
}
