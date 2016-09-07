package com.boydti.review.command;

import java.util.Collection;

import com.boydti.review.ByteReview;
import com.boydti.review.object.Review;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(command = "visit", aliases = { "home", "h", "tp" }, description = "Teleport to a plot", category = CommandCategory.TELEPORT)
public class Visit2 extends Command {
    
    private final Command visit;

    public Visit2(Command visit2) {
        super(MainCommand.getInstance(), true);
        this.visit = visit2;
    }
    
    @Override
    public void execute(PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length == 1) {
            String[] split = args[0].split(";");
            if (split.length == 4) {
                String server = split[0];
                if (ByteReview.lily.getServerName().equals(server)) {
                    ByteReview.lily.teleport(player, new Review(server, args[1], PlotId.fromString(split[2] + ";" + split[3]), null, null, null, null, null, null, null, 0));
                    return;
                }
            }
        }
        visit.execute(player, args, confirm, whenDone);
    }
    
    @Override
    public Collection tab(PlotPlayer player, String[] args, boolean space) {
        return visit.tab(player, args, space);
    }
}
