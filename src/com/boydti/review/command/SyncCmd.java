package com.boydti.review.command;

import com.boydti.review.ByteReview;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.util.MainUtil;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(command = "sync", permission = "plots.sync", category = CommandCategory.DEBUG, requiredType = RequiredType.NONE, description = "Sync the queues", usage = "/plot sync")
public class SyncCmd extends Command {
    
    public SyncCmd() {
        super(MainCommand.getInstance(), true);
    }

    @Override
    public void execute(final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (!ByteReview.database.populateQueues(new Runnable() {
            @Override
            public void run() {
                MainUtil.sendMessage(player, "&aFinished synchronizing plot queues");
                if (!ByteReview.database.populateReviews(new Runnable() {
                    @Override
                    public void run() {
                        MainUtil.sendMessage(player, "&aFinished synchronizing plot reviews");
                    }
                })) {
                    MainUtil.sendMessage(player, "&cReview sync already in progress");
                } else {
                    MainUtil.sendMessage(player, "&6Starting review sync");
                }
            }
        })) {
            MainUtil.sendMessage(player, "&cQueue sync already in progress");
        } else {
            MainUtil.sendMessage(player, "&6Starting queue sync");
        }
        ByteReview.setupConfiguration();
    }
}
