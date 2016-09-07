package com.boydti.review.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;

import com.boydti.review.ByteReview;
import com.boydti.review.ByteSettings;
import com.boydti.review.object.Queue;
import com.boydti.review.object.Review;
import com.boydti.review.object.ReviewState;
import com.boydti.review.util.ReviewUtil;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.MathMan;
import com.intellectualcrafters.plot.util.Permissions;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(
command = "next",
permission = "plots.next",
category = CommandCategory.TELEPORT,
requiredType = RequiredType.NONE,
description = "teleport to the next plot in a queue",
usage = "/plot next [queue] [increment]")
public class NextCmd extends Command {
    
    public NextCmd() {
        super(MainCommand.getInstance(), true);
    }
    
    @Override
    public Collection tab(PlotPlayer player, String[] args, boolean space) {
        switch (args.length) {
            case 0:
                return ByteReview.getQueues();
            case 1:
                if (space) {
                    Queue queue = ByteReview.getQueue(args[0]);
                    if (queue != null) {
                        return Arrays.asList("1");
                    }
                    return null;
                } else {
                    if (MathMan.isInteger(args[0])) {
                        return Arrays.asList(Integer.parseInt(args[0]) + 1);
                    }
                    List<String> names = new ArrayList<>();
                    String arg = args[0].toLowerCase();
                    for (Queue queue : ByteReview.getQueues()) {
                        if (queue.name.toLowerCase().startsWith(arg)) {
                            names.add(queue.name);
                        }
                    }
                    return names;
                }
            case 2:
                if (!space && MathMan.isInteger(args[1])) {
                    return Arrays.asList(Integer.parseInt(args[1]) + 1);
                }
        }
        return null;
    }
    
    @Override
    public void execute(PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        String name;
        int increment;
        Review review = null;
        if (args.length > 0) {
            name = args[0].toLowerCase();
            if (args.length == 2) {
                try {
                    increment = Math.max(1, Integer.parseInt(args[1]));
                } catch (final NumberFormatException e) {
                    increment = 1;
                }
            } else {
                try {
                    increment = Math.max(1, Integer.parseInt(args[0]));
                } catch (final NumberFormatException e) {
                    increment = 1;
                }
            }
        } else {
            increment = 1;
            final Location loc = player.getLocation();
            final Plot plot = loc.getOwnedPlot();
            if (plot == null) {
                name = ByteSettings.getDefault(loc.getPlotArea());
            } else {
                review = ReviewUtil.getReview(plot);
                name = review.queue == null ? ByteSettings.getDefault(plot.getArea()) : review.queue;
            }
        }
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists");
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot next [queue] [increment]");
            return;
        }
        final List<Review> reviews = ReviewUtil.sort(ReviewUtil.getReviews(queue, ReviewState.PENDING));
        if (reviews.size() == 0) {
            MainUtil.sendMessage(player, "&cQueue is empty of pending reviews");
            return;
        }
        int index;
        if (review != null) {
            index = reviews.indexOf(review);
        } else {
            index = -1;
        }
        final int newIndex = (index + increment) % reviews.size();
        for (int i = newIndex; i < reviews.size() + newIndex; i++) {
            int i2 = i % reviews.size();
            review = reviews.get(i2);
            if (review.reviewerName != null && !review.reviewerName.equalsIgnoreCase(player.getName())) {
                if (Bukkit.getPlayer(review.reviewerName) == null) {
                    review.reviewerName = null;
                }
            }
            if (review.reviewerName == null || review.reviewerName.equalsIgnoreCase(player.getName())) {
                if (Permissions.hasPermission(player, "plots.review.approve")
                || Permissions.hasPermission(player, "plots.review.reject")
                || Permissions.hasPermission(player, "plots.review.delete")
                || Permissions.hasPermission(player, "plots.review.transfer")) {
                    review.reviewerName = player.getName();
                    review.reviewerUUID = player.getUUID();
                    MainUtil.sendMessage(player, "&7Assigned: &a" + name + "&7,&a" + review.submitterName + "&7,&a" + review.area + ";" + review.id);
                    ByteReview.database.setAssigned(review);
                    ReviewUtil.teleport(player, review, true);
                } else {
                    MainUtil.sendMessage(player, "&7Rate this plot using &6/plot rate next");
                    ReviewUtil.teleport(player, review, false);
                }
                return;
            }
        }
        MainUtil.sendMessage(player, "&cQueue is empty of pending reviews");
        return;
    }
}
