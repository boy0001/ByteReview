package com.boydti.review.command;

import com.boydti.review.ByteReview;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.Visit;
import com.plotsquared.general.commands.Command;

public class CommandManager {
    public CommandManager() {
        new NextCmd();
        new QueueCmd();
        new ReviewCmd();
        new SubmitCmd();
        new SubmitAll();
        new SyncCmd();
        
        if (ByteReview.lily != null) {
            Command visit = MainCommand.getInstance().getCommand(Visit.class);
            new Visit2(visit);
        }
    }
}
