package com.boydti.review.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import com.boydti.review.ByteReview;
import com.boydti.review.object.Queue;
import com.boydti.review.object.Review;
import com.boydti.review.object.ReviewState;
import com.boydti.review.util.ReviewUtil;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotMessage;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.SchematicHandler;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(command = "queue", category = CommandCategory.ADMINISTRATION, description = "Manage plot queues")
public class QueueCmd extends Command {
    public QueueCmd() {
        super(MainCommand.getInstance(), true);
    }
    
    @CommandDeclaration(usage = "<name>", command = "create", aliases = { "c" }, requiredType = RequiredType.NONE, description = "Create a queue")
    public void create(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 2) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        final String name = args[1].toLowerCase();
        if (ByteReview.queueMap.containsKey(name)) {
            MainUtil.sendMessage(player, "&cQueue already exists");
        }
        MainUtil.sendMessage(player, "&7Created queue: &a" + name);
        final Queue queue = new Queue(name);
        ByteReview.queueMap.put(name, queue);
        ByteReview.database.addQueue(queue);
    }
    
    @CommandDeclaration(usage = "<queue> <server>", command = "addserver", aliases = { "as" }, requiredType = RequiredType.NONE, permission = "plots.queue.server", description = "Add a server to a queue")
    public void addserver(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 3) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists");
            return;
        }
        if (queue.servers.contains(args[2])) {
            MainUtil.sendMessage(player, "&cServer already added");
            return;
        }
        queue.servers.add(args[2]);
        MainUtil.sendMessage(player, "&7Added server: &a" + args[2] + "&7 to queue: &a" + queue.name);
        ByteReview.database.setServers(queue);
    }
    
    @CommandDeclaration(
    usage = "<queue> <server>",
    command = "delserver",
    aliases = { "deleteserver", "ds" },
    requiredType = RequiredType.NONE,
    permission = "plots.queue.server",
    description = "Remove a server from a queue")
    public void delserver(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 3) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists");
            return;
        }
        if (!queue.servers.contains(args[2])) {
            MainUtil.sendMessage(player, "&cServer not added: " + args[2]);
            return;
        }
        queue.servers.remove(args[2]);
        MainUtil.sendMessage(player, "&7Removed server: &a" + args[2] + "&7 from queue: &a" + queue.name);
        ByteReview.database.setServers(queue);
    }
    
    @CommandDeclaration(
    usage = "<queue> <world>",
    command = "addarea",
    aliases = { "aw", "aa" },
    requiredType = RequiredType.NONE,
    permission = "plots.queue.world",
    description = "Add a world to a queue")
    public void addworld(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 3) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists");
            return;
        }
        if (queue.areas.contains(args[2])) {
            MainUtil.sendMessage(player, "&cWorld already added");
            return;
        }
        queue.areas.add(args[2]);
        MainUtil.sendMessage(player, "&7Added area: &a" + args[2] + "&7 to queue: &a" + queue.name);
        ByteReview.database.setWorlds(queue);
    }
    
    @CommandDeclaration(
    usage = "<queue> <world>",
    command = "delarea",
    aliases = { "deletearea", "da", "deleteworld", "dw" },
    requiredType = RequiredType.NONE,
    permission = "plots.queue.world",
    description = "Remove a world from a queue")
    public void delworld(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 3) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists");
            return;
        }
        if (!queue.areas.contains(args[2])) {
            MainUtil.sendMessage(player, "&cWorld not added: " + args[2]);
            return;
        }
        queue.areas.remove(args[2]);
        MainUtil.sendMessage(player, "&7Removed area: &a" + args[2] + "&7 from queue: &a" + queue.name);
        ByteReview.database.setWorlds(queue);
    }
    
    @CommandDeclaration(
    usage = "<queue> <command>",
    command = "addcmd",
    aliases = { "addcommand", "ac" },
    requiredType = RequiredType.NONE,
    permission = "plots.queue.command",
    description = "Add a command to a queue")
    public void addcmd(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length < 3) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists");
            return;
        }
        final String cmd = StringUtils.join(Arrays.copyOfRange(args, 2, args.length), " ");
        if (queue.commands.contains(cmd)) {
            MainUtil.sendMessage(player, "&cCommand already added: " + cmd);
            return;
        }
        queue.commands.add(cmd);
        MainUtil.sendMessage(player, "&7Added command: &a" + cmd + "&7 to queue: &a" + queue.name);
        ByteReview.database.setCommands(queue);
    }
    
    @CommandDeclaration(
    command = "delcmd",
    aliases = { "delcommand", "deletecommand", "deletecmd", "dc" },
    requiredType = RequiredType.NONE,
    permission = "plots.queue.command",
    usage = "<queue> <command>",
    description = "Remove a command from a queue")
    public void delcmd(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length < 3) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists");
            return;
        }
        final String cmd = StringUtils.join(Arrays.copyOfRange(args, 2, args.length), " ");
        if ((queue.commands.size() == 0) || (!queue.commands.contains(cmd) && cmd.equals("*"))) {
            MainUtil.sendMessage(player, "&cCommand not added: " + cmd);
            return;
        }
        if (cmd.equals("*")) {
            queue.commands.clear();
        } else {
            queue.commands.remove(cmd);
        }
        MainUtil.sendMessage(player, "&7Removed command: &a" + cmd + "&7 from queue: &a" + queue.name);
        ByteReview.database.setCommands(queue);
    }
    
    @CommandDeclaration(command = "all", aliases = { "a" }, requiredType = RequiredType.NONE, description = "List all queues")
    public void all(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        MainUtil.sendMessage(player, "&8====== &6QUEUE LIST &8======");
        for (Entry<String, Queue> entry : ByteReview.queueMap.entrySet()) {
            Queue queue = entry.getValue();
            if (Permissions.hasPermission(player, "plots.submit." + queue.name)) {
                String suffix = "";
                if (Permissions.hasPermission(player, "plots.review.approve")) {
                    suffix = " &c(" + ReviewUtil.getPending(queue).size() + ")";
                }
                MainUtil.sendMessage(player, "&7 - &a" + queue.name + suffix);
            }
        }
    }
    
    @CommandDeclaration(usage = "<queue>", command = "info", aliases = { "i" }, requiredType = RequiredType.NONE, description = "View queue info")
    public void info(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 2) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists");
            return;
        }
        MainUtil.sendMessage(player, "&8====== &6QUEUE INFO &8======");
        MainUtil.sendMessage(player, "&7Name: &a" + queue.name);
        MainUtil.sendMessage(player, "&7Pending: &a" + ReviewUtil.getPending(queue).size());
        MainUtil.sendMessage(player, "&7Denied: &a" + ReviewUtil.getDenied(queue).size());
        MainUtil.sendMessage(player, "&7Average Changes: &a" + ReviewUtil.getAverageChanges(queue));
        if (queue.servers.size() == 0) {
            MainUtil.sendMessage(player, "&7Servers: &a*");
        } else {
            MainUtil.sendMessage(player, "&7Servers: &a" + StringUtils.join(queue.servers, ","));
        }
        if (queue.areas.size() == 0) {
            MainUtil.sendMessage(player, "&7Areas: &a*");
        } else {
            MainUtil.sendMessage(player, "&7Areas: &a" + StringUtils.join(queue.areas, ","));
        }
        if (queue.commands.size() > 0) {
            MainUtil.sendMessage(player, "&7Commands: &a" + StringUtils.join(queue.commands, "\n&7 - &a"));
        }
    }
    
    @CommandDeclaration(usage = "<queue> <pending|denied|approved|assigned|all> [page]", command = "list", aliases = { "l" }, requiredType = RequiredType.NONE, description = "List/sort reviews")
    public void list(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if ((args.length != 3) && (args.length != 4)) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        int page;
        if (args.length == 4) {
            try {
                page = Integer.parseInt(args[3]);
            } catch (final NumberFormatException e) {
                page = 0;
            }
        } else {
            page = 0;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            if (!name.equals("*")) {
                MainUtil.sendMessage(player, "&cQueue doesn't exists");
                return;
            }
        }
        
        HashSet<Review> reviews;
        switch (args[2].toLowerCase()) {
            case "submitted":
            case "pending": {
                reviews = ReviewUtil.getReviews(queue, ReviewState.PENDING);
                break;
            }
            case "rejected":
            case "denied":
            case "resubmit": {
                reviews = ReviewUtil.getReviews(queue, ReviewState.DENIED);
                break;
            }
            case "approved": {
                reviews = ReviewUtil.getReviews(queue, ReviewState.APPROVED);
                break;
            }
            case "assigned": {
                reviews = ReviewUtil.getReviewsReviewer(player, queue, ReviewState.PENDING);
                break;
            }
            case "all": {
                reviews = ReviewUtil.getAllReviews(queue);
                break;
            }
            default: {
                MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot queue list <queue> <pending|denied|approved|assigned|all> [page]");
                return;
            }
        }
        List<Review> sorted = ReviewUtil.sort(reviews);
        this.<Review> paginate(player, sorted, 12, page, new RunnableVal3<Integer, Review, PlotMessage>() {
            @Override
            public void run(Integer i, Review review, PlotMessage message) {
                PlotMessage tooltip = new PlotMessage()
                        .text("ID=").color("$1").text("" + review).color("$2")
                        .text("\nQueue=").color("$1").text(review.queue).color("$2")
                        .text("\nSubmitted=").color("$1").text(review.submitterName).color("$2")
                        .text("\nAssigned=").color("$1").text(review.reviewerName).color("$2")
                        .text("\nAttempts=").color("$1").text(review.attempts + "").color("$2")
                        .text("\nState=").color("$1").text(review.state + "").color("$2")
                        .text("\nTime=").color("$1").text(ReviewUtil.secToTime(review.timestamp)).color("$2");

                String visit = "/plot visit " + review.toString();
                message.text("[").color("$3")
                        .text(i + "").command(visit).tooltip(visit).color("$1")
                        .text("]").color("$3")
                        .text(" " + review.toString()).tooltip(tooltip).command("/plot review info " + review.toString()).color("$1").text(" - ").color("$2")
                        .text(review.submitterName).color("$3");
                
            }
        }, getCommandString() + " " + args[0], C.PLOT_INFO_HEADER.s());
    }
    
    @CommandDeclaration(usage = "<queue>", command = "delete", requiredType = RequiredType.NONE, description = "Delete a queue")
    public void delete(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 2) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists");
            return;
        }
        ReviewUtil.clearQueue(queue);
        ByteReview.queueMap.remove(name);
        MainUtil.sendMessage(player, "&7Deleted queue: &a" + name);
        ByteReview.database.removeQueue(queue);
    }
    
    @CommandDeclaration(usage = "<queue>", command = "clear", requiredType = RequiredType.NONE, description = "Clear a queue")
    public void clear(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 2) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, getUsage());
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists");
            return;
        }
        ReviewUtil.clearQueue(queue);
        MainUtil.sendMessage(player, "&7Cleared queue: &a" + name);
        ByteReview.database.clearQueue(queue);
    }
    
    @CommandDeclaration(command = "export", aliases = { "exportall"}, requiredType = RequiredType.NONE, description = "Export the contents of a queue as schematics")
    public void export(Command command, final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) {
        if (args.length != 5) {
            MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot queue export <name> <state> <directory> <naming-scheme>");
            MainUtil.sendMessage(player, "&6Name: &7Name of the queue");
            MainUtil.sendMessage(player, "&6State: &7[approved,denied,pending]");
            MainUtil.sendMessage(player, "&6Directory: &7Output directory (* => output set in settings.yml, root = server root)");
            MainUtil.sendMessage(player, "&6Name scheme: &7File names with placeholders [%id%,%idx%,%idy%,%world%,%owner%]");
            return;
        }
        final String name = args[1].toLowerCase();
        final Queue queue = ByteReview.queueMap.get(name);
        if (queue == null) {
            MainUtil.sendMessage(player, "&cQueue doesn't exists: " + args[1]);
            return;
        }
        ReviewState state;
        switch (args[2].toLowerCase()) {
            case "approved": {
                state = ReviewState.APPROVED;
                break;
            }
            case "denied": {
                state = ReviewState.DENIED;
                break;
            }
            case "pending": {
                state = ReviewState.PENDING;
                break;
            }
            default: {
                MainUtil.sendMessage(player, "&cInvalid state: " + args[2] + " from [approved,denied,pending]");
                return;
            }
        }
        String outputDir = args[3];
        if (outputDir.equals("*")) {
            outputDir = Settings.SCHEMATIC_SAVE_PATH;
        }
        String namingScheme = args[4];
        if (namingScheme.toLowerCase().endsWith(".schematic")) {
            MainUtil.sendMessage(player, "&cInvalid naming scheme: " + args[4] + " - `.schematic` is automatically appended");
            return;
        }
        if (namingScheme.equals("*")) {
            namingScheme = "%idx%;%idy%,%world%,%owner%";
        }
        
        final HashSet<Review> reviews = ReviewUtil.getAllReviews(queue);
        final ArrayList<Plot> plots = new ArrayList<Plot>();
        final String server = ReviewUtil.server;
        for (final Review review : reviews) {
            if (review.state != state) {
                continue;
            }
            if (!review.server.equals(server)) {
                continue;
            }
            final Plot plot = review.getPlot();
            if (plot != null) {
                plots.add(plot);
            }
        }
        
        SchematicHandler.manager.exportAll(plots, new File(outputDir), namingScheme, new Runnable() {
            @Override
            public void run() {
                MainUtil.sendMessage(player, "&aExport complete!");
            }
        });
    }
}
