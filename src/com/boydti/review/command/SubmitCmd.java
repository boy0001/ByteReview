package com.boydti.review.command;

import com.boydti.review.ByteReview;
import com.boydti.review.ByteSettings;
import com.boydti.review.object.Queue;
import com.boydti.review.object.Review;
import com.boydti.review.object.ReviewState;
import com.boydti.review.util.ReviewUtil;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.generator.HybridUtils;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotAnalysis;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.StringMan;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(
command = "submit",
permission = "plots.submit",
category = CommandCategory.SETTINGS,
requiredType = RequiredType.NONE,
description = "Submit a plot to a queue",
aliases = { "done" },
usage = "/plot submit [queue]")
public class SubmitCmd extends SubCommand {
    
    @Override
    public boolean onCommand(final PlotPlayer player, final String... args) {
        final Plot plot = player.getCurrentPlot();
        if (plot == null) {
            MainUtil.sendMessage(player, C.NOT_IN_PLOT);
            return false;
        }
        if ((plot.owner == null) || (!plot.isAdded(player.getUUID()) && !Permissions.hasPermission(player, "plots.admin.command.submit"))) {
            sendMessage(player, C.NO_PLOT_PERMS);
            return false;
        }
        
        // Do not allow if plot is already submitted
        final Review review = ReviewUtil.getReview(plot);
        if ((review.state != ReviewState.DENIED) && (review.attempts.size() != 0)) {
            MainUtil.sendMessage(player, "&cReview already pending");
            return false;
        }
        
        String queueStr;
        if (review.queue != null) {
            queueStr = review.queue;
        } else {
            if (args.length == 0) {
                if (!ByteReview.queueMap.containsKey(ByteSettings.getDefault(plot.getArea()))) {
                    MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot submit <queue>");
                    return false;
                }
                queueStr = ByteSettings.getDefault(plot.getArea());
            } else {
                queueStr = args[0].toLowerCase();
            }
        }
        
        final int diff = (int) ((System.currentTimeMillis() / 1000) - review.timestamp);
        if (diff < ByteSettings.COOLDOWN) {
            MainUtil.sendMessage(player, "&cYou need to wait "
            + ReviewUtil.secToTime((int) ((System.currentTimeMillis() / 500) - ByteSettings.COOLDOWN - review.timestamp))
            + "before you can resubmit this plot for approval.");
            return false;
        }
        
        // Do not allow if invalid queue
        final Queue queue = ByteReview.queueMap.get(queueStr);
        if ((queue == null) || !Permissions.hasPermission(player, "plots.submit." + queueStr)) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "&cInvalid queue: " + queueStr + "\n&7For a list of queues use /plot queue all");
            return false;
        }
        
        if (!queue.isServerAllowed(ReviewUtil.server) || !queue.isAreaAllowed(plot.getArea())) {
            MainUtil.sendMessage(player, "&cThat queue does not accept submissions from this world!");
            return false;
        }
        
        // Do not run if 1 or more tasks are running for a plot
        if (plot.getRunning() != 0) {
            MainUtil.sendMessage(player, C.WAIT_FOR_TIMER);
            return false;
        }
        
        // Indicate that 1 task is running for the plot
        MainUtil.sendMessage(player, "&6Your plot is being processed...");
        plot.addRunning();
        queue.isPlotAllowed(plot, new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                if (value == null) {
                    
                }
                if (value instanceof String) {
                    MainUtil.sendMessage(player, (String) value);
                    return;
                }
                if (value instanceof Boolean) {
                    if (((Boolean) value)) {
                        HybridUtils.manager.analyzePlot(plot, new RunnableVal<PlotAnalysis>() {
                            @Override
                            public void run(PlotAnalysis value) {
                                plot.removeRunning();
                                if (value.getComplexity() < Settings.CLEAR_THRESHOLD) {
                                    MainUtil.sendMessage(player, "&cYour plot was rejected for lack of detail");
                                    return;
                                }
                                if (ByteReview.lily != null) {
                                    ByteReview.lily.sendMessage("msg plots.queue.notify." + review.queue + " &9" + player.getName() + "&7 submitted their plot to &9" + queue.name + "&7.", null);
                                } else {
                                    for (PlotPlayer pp : UUIDHandler.getPlayers().values()) {
                                        if (Permissions.hasPermission(pp, "plots.queue.notify." + review.queue)) {
                                            MainUtil.sendMessage(pp, "&9" + player.getName() + "&7 submitted their plot to &9" + queue.name + "&7.");
                                        }
                                    }
                                }
                                ReviewUtil.addToQueue(plot, queue, value.getComplexity());
                                MainUtil.sendMessage(player, "&aSuccessfully submitted plot to: " + queue.name);
                            }
                        });
                    }
                    return;
                }
                MainUtil.sendMessage(player, "&cYour plot was rejected: " + StringMan.getString(value));
            }
        });
        
        return true;
    }
}
