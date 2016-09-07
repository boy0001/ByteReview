package com.boydti.review.command;

import java.util.UUID;

import com.boydti.review.ByteReview;
import com.boydti.review.object.Queue;
import com.boydti.review.object.Review;
import com.boydti.review.object.ReviewState;
import com.boydti.review.util.ReviewUtil;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(
command = "review",
category = CommandCategory.ADMINISTRATION,
requiredType = RequiredType.NONE,
 description = "review a plot")
public class ReviewCmd extends Command {
    public ReviewCmd() {
        super(MainCommand.getInstance(), true);
    }

    @CommandDeclaration(command = "approve", aliases = { "allow", "accept" }, description = "Approve a review")
    public void approve(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        final Plot plot = player.getCurrentPlot();
        final Review review = ReviewUtil.getReview(plot);
        if (review.state == ReviewState.DENIED) {
            MainUtil.sendMessage(player, "&cNo pending review");
            return;
        }
        MainUtil.sendMessage(player, "&aApproving review");
        ReviewUtil.executeReview(review, player);
    }
    
    @CommandDeclaration(command = "reject", aliases = { "deny" }, description = "Reject a review")
    public void reject(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        final Plot plot = player.getCurrentPlot();
        final Review review = ReviewUtil.getReview(plot);
        if (review.state == ReviewState.DENIED) {
            MainUtil.sendMessage(player, "&cNo pending review");
            return;
        }
        MainUtil.sendMessage(player, "&aRejecting review");
        FlagManager.removePlotFlag(plot, "done");
        ReviewUtil.rejectReview(review, player);
    }
    
    @CommandDeclaration(command = "transfer", aliases = { "t" }, description = "Transfer a review")
    public void transfer(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 1) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot review transfer <queue>");
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            if (!name.equals("*")) {
                MainUtil.sendMessage(player, "&cQueue doesn't exists");
                return;
            }
        }
        final Plot plot = player.getCurrentPlot();
        final Review review = ReviewUtil.getReview(plot);
        if ((review.state == ReviewState.DENIED) && (review.attempts.size() == 0)) {
            MainUtil.sendMessage(player, "&cNo review found");
            return;
        }
        MainUtil.sendMessage(player, "&7Transfering review to: &a" + name);
        review.queue = name;
        review.reviewerName = player.getName();
        review.reviewerUUID = player.getUUID();
        ByteReview.database.setTransfer(review);
        ByteReview.database.setAssigned(review);
    }
    
    @CommandDeclaration(command = "info", aliases = { "i" }, description = "View review info")
    public void info(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        Review review;
        if (args.length == 1) {
            Plot plot = MainUtil.getPlotFromString(player, args[0], true);
            if (plot == null) {
                return;
            }
            review = ReviewUtil.getReview(plot);
        } else {
            final Plot plot = player.getCurrentPlot();
            review = ReviewUtil.getReview(plot);
            if ((review.state == ReviewState.DENIED) && (review.attempts.size() == 0)) {
                review = null;
            }
        }
        if (review == null) {
            MainUtil.sendMessage(player, "&cNo review found at this location");
            return;
        }
        MainUtil.sendMessage(player, "&8====== &6REVIEW &8======");
        MainUtil.sendMessage(player, "&7ID: &a" + review.server + ";" + review.area + ";" + review.id);
        MainUtil.sendMessage(player, "&7Owner: &a" + review.submitterName);
        if (review.reviewerName != null) {
            MainUtil.sendMessage(player, "&7Assigned: &a" + review.reviewerName);
        } else {
            MainUtil.sendMessage(player, "&7Assigned: N/A");
        }
        MainUtil.sendMessage(player, "&7Submitted: &a" + ReviewUtil.secToTime(review.timestamp));
        MainUtil.sendMessage(player, "&7State: &a" + review.state);
        if (review.attempts.size() > 1) {
            MainUtil.sendMessage(player, "&7Complexity: &a" + review.attempts.get(review.attempts.size() - 1));
            MainUtil.sendMessage(player, "&7- Previous:  &a" + review.attempts.get(review.attempts.size() - 2));
        } else {
            MainUtil.sendMessage(player, "&7Complexity: &a" + review.attempts.get(0));
        }
    }
    
    @CommandDeclaration(command = "delete", aliases = { "remove", "purge" }, description = "Delete a review")
    public void delete(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        final Plot plot = player.getCurrentPlot();
        final Review review = ReviewUtil.getReview(plot);
        if ((review.state == ReviewState.DENIED) && (review.attempts.size() == 0)) {
            MainUtil.sendMessage(player, "&cNo review found");
            return;
        }
        MainUtil.sendMessage(player, "&aDeleting review");
        FlagManager.removePlotFlag(plot, "done");
        ReviewUtil.removeReview(review);
    }

    @CommandDeclaration(command = "list", aliases = { "l" }, description = "List reviews")
    public void list(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        ((QueueCmd) command.getParent().getCommand(QueueCmd.class)).list(command, player, args, confirm, whenDone);
    }
    
    @CommandDeclaration(command = "assign", description = "Assign a user to a review")
    public void assign(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 2) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot review assign <player>");
            return;
        }
        final Plot plot = player.getCurrentPlot();
        final Review review = ReviewUtil.getReview(plot);
        if ((review.state == ReviewState.DENIED) && (review.attempts.size() == 0)) {
            MainUtil.sendMessage(player, "&cNo review found at this location");
            return;
        }
        final UUID uuid = UUIDHandler.getUUID(args[1], null);
        if (uuid == null) {
            MainUtil.sendMessage(player, C.INVALID_PLAYER, args[1]);
            return;
        }
        review.reviewerName = args[1];
        review.reviewerUUID = uuid;
        MainUtil.sendMessage(player, "&7Assigned: &a" + args[1]);
        ByteReview.database.setAssigned(review);
    }
}
