package com.boydti.review.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.boydti.review.ByteReview;
import com.boydti.review.object.Queue;
import com.boydti.review.object.Review;
import com.boydti.review.object.ReviewState;
import com.boydti.review.util.ReviewUtil;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.util.MainUtil;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(
command = "submitall",
permission = "plots.submitall",
category = CommandCategory.ADMINISTRATION,
requiredType = RequiredType.CONSOLE,
description = "Submit all plots in the world to a queue",
usage = "/plot submitall [queue]")
public class SubmitAll extends Command {
    
    public SubmitAll() {
        super(MainCommand.getInstance(), true);
    }

    @Override
    public Collection tab(PlotPlayer player, String[] args, boolean space) {
        List<String> names = new ArrayList<>();
        String arg = args[0].toLowerCase();
        for (Queue queue : ByteReview.getQueues()) {
            if (queue.name.toLowerCase().startsWith(arg)) {
                names.add(queue.name);
            }
        }
        return names;
    }
    
    @Override
    public void execute(PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 2) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot submitall <world> <queue>");
            return;
        }
        if (!PS.get().isPlotWorld(args[0])) {
            MainUtil.sendMessage(player, C.NOT_IN_PLOT_WORLD);
            return;
        }
        final Queue queue = ByteReview.queueMap.get(args[1]);
        if (queue == null) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "&cInvalid queue: " + args[1] + "\n&7For a list of queues use /plot queue all");
            return;
        }
        for (final Plot plot : PS.get().getPlots()) {
            final Review review = ReviewUtil.getReview(plot);
            if (review.state == ReviewState.DENIED) {
                MainUtil.sendMessage(player, " - " + plot.toString());
                ReviewUtil.addToQueue(plot, queue, -1);
            }
        }
        MainUtil.sendMessage(player, "&aDone!");
    }
}
