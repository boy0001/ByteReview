package com.boydti.review.util;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.boydti.review.ByteReview;
import com.boydti.review.ByteSettings;
import com.boydti.review.object.Queue;
import com.boydti.review.object.Review;
import com.boydti.review.object.ReviewState;
import com.boydti.review.object.SimplePlot;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RegionWrapper;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.StringMan;
import com.intellectualcrafters.plot.util.TaskManager;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.bukkit.object.BukkitPlayer;
import com.plotsquared.bukkit.util.BukkitUtil;

public class ReviewUtil {
    
    /**
     * The name of the server the plugin is on
     */
    public static String server;
    
    /**
     * The simple plot class is used for the mapping of reviews -> server, world, plot id
     * @param plot
     * @return
     */
    public static SimplePlot getSimplePlot(final Plot plot) {
        return new SimplePlot(server, plot.getArea().toString(), plot.getId());
    }
    
    /**
     * Get a the simple plot for a review
     * @param review
     * @return
     */
    public static SimplePlot getSimplePlot(final Review review) {
        return getSimplePlot(review.getPlot());
    }
    
    public static void notify(PlotPlayer player) {
        if (Permissions.hasPermission(player, "plots.queue.notify")) {
            for (final Queue queue : ByteReview.queueMap.values()) {
                if (Permissions.hasPermission(player, "plots.queue.notify." + queue.name)) {
                    HashSet<Review> reviews = ReviewUtil.getReviews(queue, ReviewState.PENDING);
                    if (reviews.size() > 0) {
                        MainUtil.sendMessage(player, "&c" + reviews.size() + " &7pending reviews for: &c" + queue.name);
                    }
                }
            }
        }
        final HashSet<Review> assigned = ReviewUtil.getReviewsReviewer(player, null, ReviewState.PENDING);
        if (assigned.size() > 0) {
            MainUtil.sendMessage(player, "&c" + assigned.size() + " assigned reviews\nTo view use: /plot queue list");
        }
    }
    
    /**
     * Get the reviews submitted by a player in a certain state. Null queue values means all queues
     * @param player
     * @param queue
     * @param state
     * @return
     */
    public static HashSet<Review> getReviews(final PlotPlayer player, final Queue queue, final ReviewState state) {
        final UUID uuid = player.getUUID();
        final HashSet<Review> reviews = new HashSet<>();
        for (final Review review : ByteReview.reviewMap.values()) {
            if ((review.state == state) && ((queue == null) || review.queue.equals(queue.name)) && uuid.equals(review.submitterUUID)) {
                reviews.add(review);
            }
        }
        return reviews;
    }
    
    /**
     * Get the plot corresponding for a review (only use if on same server as review)
     * @param review
     * @return
     */
    public static Plot getPlot(final Review review) {
        return review.getPlot();
    }
    
    /**
     * Teleport a player to a review
     * @param player
     * @param review
     */
    public static void teleport(final PlotPlayer player, final Review review, boolean book) {
        if (!review.server.equals(server)) {
            if (ByteReview.lily != null) {
                if (ByteReview.lily.teleport(player, review)) {
                    return;
                }
            }
            MainUtil.sendMessage(player, "Incorrect server. Use /server " + review.server);
            return;
        }
        Plot plot = review.getPlot();
        if (plot == null) {
            MainUtil.sendMessage(player, "Try another review. (Invalid location: " + review + ")");
            return;
        }
        if (book) {
            Player bp = ((BukkitPlayer) player).player;
            PlayerInventory inv = bp.getInventory();
            if (!inv.containsAtLeast(new ItemStack(Material.BOOK_AND_QUILL), 1)) {
                ItemStack hand = inv.getItemInHand();
                inv.setItemInHand(new ItemStack(Material.BOOK_AND_QUILL, 1));
                if (hand != null && hand.getType() != Material.AIR) {
                    inv.addItem(hand);
                }
            }
        }
        plot.teleportPlayer(player);
    }
    
    public static String setChest(PlotPlayer player, Review review) {
        Player bp = ((BukkitPlayer) player).player;
        ItemStack item = bp.getItemInHand();
        if (item.getType() != Material.WRITTEN_BOOK && item.getType() != Material.BOOK_AND_QUILL) {
            return null;
        }
        Plot plot = getPlot(review);
        if (plot == null) {
            return null;
        }
        bp.setItemInHand(new ItemStack(Material.AIR));
        bp.updateInventory();
        RegionWrapper region = plot.getLargestRegion();
        Location bot = new Location(plot.getArea().worldname, region.minX, region.minY, region.minZ);
        org.bukkit.Location loc = BukkitUtil.getLocation(bot);
        World world = loc.getWorld();
        int top = Math.max(16, Math.min(249, world.getHighestBlockAt(loc).getY())) + 1;
        world.getBlockAt(bot.getX(), top, bot.getZ()).setType(Material.CHEST, false);
        world.getBlockAt(bot.getX(), top - 1, bot.getZ()).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(bot.getX(), top + 2, bot.getZ()).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(bot.getX(), top + 3, bot.getZ()).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(bot.getX(), top + 4, bot.getZ()).setType(Material.GOLD_BLOCK, false);
        Chest state = (Chest) world.getBlockAt(bot.getX(), top, bot.getZ()).getState();
        state.getInventory().addItem(item);
        List<String> text = new ArrayList<>();
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof BookMeta) {
                BookMeta book = (BookMeta) meta;
                if (book.hasTitle()) {
                    text.add(book.getTitle());
                }
                text.addAll(book.getPages());
            }
        }
        state.update();
        return StringMan.join(text, "\n========================\n");
    }
    
    /**
     * Get the reviews in a queue with a given status
     * @param queue
     * @param state
     * @return
     */
    public static HashSet<Review> getReviews(final Queue queue, final ReviewState state) {
        final HashSet<Review> reviews = new HashSet<>();
        for (final Review review : ByteReview.reviewMap.values()) {
            if ((review.state == state) && ((queue == null) || review.queue.equals(queue.name))) {
                reviews.add(review);
            }
        }
        return reviews;
    }
    
    /**
     * Get the reviews assigned to a player in a given state
     * @param player
     * @param queue
     * @param state
     * @return
     */
    public static HashSet<Review> getReviewsReviewer(final PlotPlayer player, final Queue queue, final ReviewState state) {
        final UUID uuid = player.getUUID();
        final HashSet<Review> reviews = new HashSet<>();
        for (final Review review : ByteReview.reviewMap.values()) {
            if ((review.state == state) && ((queue == null) || review.queue.equals(queue.name)) && uuid.equals(review.reviewerUUID)) {
                reviews.add(review);
            }
        }
        return reviews;
    }
    
    /**
     * Sort reviews by timestamp
     * @param set
     * @return
     */
    public static List<Review> sort(final HashSet<Review> set) {
        final List<Review> list = new ArrayList<>(set);
        final Comparator<Review> comparator = new Comparator<Review>() {
            @Override
            public int compare(final Review r1, final Review r2) {
                return r1.timestamp - r2.timestamp;
            }
        };
        Collections.sort(list, comparator);
        return list;
    }
    
    /**
     * Remove a review (from DB as well)
     * @param review
     */
    public static void removeReview(final Review review) {
        ByteReview.reviewMap.remove(getSimplePlot(review));
        ByteReview.database.removeReview(review);
        Bukkit.getPluginManager().callEvent(new ReviewEvent(review, null));
    }
    
    /**
     * Reject a review (in DB as well)
     * @param review
     */
    public static void rejectReview(final Review review, final PlotPlayer player) {
        String text = setChest(player, review);
        final Review before = review.clone();
        review.timestamp = (int) (System.currentTimeMillis() / 1000);
        review.state = ReviewState.DENIED;
        review.reviewerName = player.getName();
        review.reviewerUUID = player.getUUID();
        ByteReview.database.setState(review);
        ByteReview.database.setAssigned(review);
        Bukkit.getPluginManager().callEvent(new ReviewEvent(before, review));
        uploadReview(review, text);
    }
    
    public static void uploadReview(final Review review, final String text) {
        if (review == null || ByteSettings.WEB_URL.length() == 0) {
            return;
        }
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    String filename = review.area + "_" + review.id + "_" + review.submitterName + "_" + review.reviewerName + "_" + System.currentTimeMillis() + "_" + (review.state == ReviewState.APPROVED);
                    String website = ByteSettings.WEB_URL + "upload.php?" + filename;
                    String charset = "UTF-8";
                    String param = "value";
                    String boundary = Long.toHexString(System.currentTimeMillis());
                    String CRLF = "\r\n";
                    URLConnection con = new URL(website).openConnection();
                    con.setDoOutput(true);
                    con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    
                    try (OutputStream output = con.getOutputStream(); PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);) {
                        // Send normal param.
                        writer.append("--" + boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
                        writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                        writer.append(CRLF).append(param).append(CRLF).flush();
                        
                        // Send text file.
                        writer.append("--" + boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"reviewFile\"; filename=\"review.txt\"").append(CRLF);
                        writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
                        writer.append(CRLF).flush();
                        output.write((text == null ? "" : text).getBytes());
                        output.flush(); // Important before continuing with writer!
                        writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.
                        
                        // End of multipart/form-data.
                        writer.append("--" + boundary + "--").append(CRLF).flush();
                    }
                    
                    try (Reader response = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                        final char[] buffer = new char[256];
                        StringBuilder result = new StringBuilder();
                        while (true) {
                            int r = response.read(buffer);
                            if (r < 0) {
                                break;
                            }
                            result.append(buffer, 0, r);
                        }
                        if (!result.toString().equals("Success")) {
                            System.out.print(result);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Execute the commands and tasks associated with a review<br>
     *  - transfer:<queue>  -> will transfer and deny a review to another queue<br>
     *  - pending:<queue> -> will transfer and pend a review to another queue<br>
     *  (updates DB)
     * @param review
     */
    public static void executeReview(final Review review, final PlotPlayer player) {
        String text = setChest(player, review);
        final Review before = review.clone();
        review.reviewerUUID = player.getUUID();
        review.reviewerName = player.getName();
        review.timestamp = (int) (System.currentTimeMillis() / 1000);
        boolean transfer = false;
        final Queue queue = ByteReview.queueMap.get(review.queue);
        for (String command : queue.commands) {
            if (command.startsWith("transfer:")) {
                final String name = command.split(":")[1].trim();
                review.queue = name;
                review.state = ReviewState.DENIED;
                ByteReview.database.setTransfer(review);
                ByteReview.database.setAssigned(review);
                transfer = true;
                continue;
            }
            if (command.startsWith("pending:")) {
                final String name = command.split(":")[1].trim();
                review.queue = name;
                review.state = ReviewState.PENDING;
                ByteReview.database.setTransfer(review);
                ByteReview.database.setAssigned(review);
                transfer = true;
                continue;
            }
            if (command.equals("remove")) {
                review.state = ReviewState.APPROVED;
                uploadReview(review, text);
                review.state = before.state;
                removeReview(review);
                return;
            }
            command = command.replaceAll("%player%", review.submitterName);
            command = command.replaceAll("%reviewer%", review.reviewerName);
            command = command.replaceAll("%world%", review.area);
            command = command.replaceAll("%id%", review.id.toString());
            command = command.replaceAll("%idx%", review.id.x + "");
            command = command.replaceAll("%idy%", review.id.y + "");
            if (command.startsWith("console:")) {
                command = ChatColor.translateAlternateColorCodes('&', command.substring(8));
                Bukkit.broadcastMessage(command);
                continue;
            }
            if (command.startsWith("player:")) {
                command = ChatColor.translateAlternateColorCodes('&', command.substring(7));
                final PlotPlayer owner = UUIDHandler.getPlayer(review.submitterUUID);
                if (owner != null) {
                    MainUtil.sendMessage(owner, command);
                }
                continue;
            }
            if (command.startsWith("reviewer:")) {
                command = ChatColor.translateAlternateColorCodes('&', command.substring(9));
                MainUtil.sendMessage(player, command);
                continue;
            }
            final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            Bukkit.getServer().dispatchCommand(console, command);
        }
        if (!transfer) {
            final Plot plot = getPlot(review);
            plot.setFlag("done", System.currentTimeMillis() + "");
            if (ByteSettings.APPROVE_BYPASS) {
                plot.countsTowardsMax = false;
            }
            review.state = ReviewState.APPROVED;
            ByteReview.database.setState(review);
        }
        uploadReview(review, text);
        Bukkit.getPluginManager().callEvent(new ReviewEvent(before, review));
    }
    
    /**
     * Will clear a queue of all reviews (also in DB)
     * @param queue
     */
    public static void clearQueue(final Queue queue) {
        final String name = queue.name;
        for (final Iterator<Entry<SimplePlot, Review>> iter = ByteReview.reviewMap.entrySet().iterator(); iter.hasNext();) {
            final Entry<SimplePlot, Review> entry = iter.next();
            if (entry.getValue().queue.equals(name)) {
                final Review review = entry.getValue();
                ByteReview.database.removeReview(review);
                iter.remove();
                Bukkit.getPluginManager().callEvent(new ReviewEvent(review, null));
            }
        }
    }
    
    /**
     * Convert a timestamp (seconds) to a friendly string
     * @param timestamp
     * @return
     */
    public static String secToTime(int timestamp) {
        timestamp = (int) ((System.currentTimeMillis() / 1000) - timestamp);
        String time = "";
        final int days = timestamp / 86400;
        timestamp = timestamp % 86400;
        if (days != 0) {
            time += days + "d ";
        }
        
        final int hours = timestamp / 3600;
        timestamp = timestamp % 3600;
        if (hours != 0) {
            time += hours + "h ";
        }
        
        final int minutes = timestamp / 60;
        timestamp = timestamp % 60;
        if (minutes != 0) {
            time += minutes + "m ";
        }
        
        final int seconds = timestamp;
        if (seconds != 0) {
            time += seconds + "s ";
        }
        return time;
    }
    
    //    /**
    //     * Display a list of reviews (supports pagination)
    //     * @param player
    //     * @param oldReviews
    //     * @param page
    //     */
    //    public static void displayReviews(final PlotPlayer player, final HashSet<Review> oldReviews, int page) {
    //        final Review[] reviews = sort(oldReviews).toArray(new Review[oldReviews.size()]);
    //        if (page < 0) {
    //            page = 0;
    //        }
    //        // Get the total pages
    //        // int totalPages = ((int) Math.ceil(12 *
    //        final int totalPages = (int) Math.ceil(reviews.length / 12);
    //        if (page > totalPages) {
    //            page = totalPages;
    //        }
    //        // Only display 12 per page
    //        int max = (page * 12) + 12;
    //        if (max > reviews.length) {
    //            max = reviews.length;
    //        }
    //        final StringBuilder string = new StringBuilder();
    //        string.append(C.PLOT_LIST_HEADER_PAGED.s().replaceAll("plot", "review").replaceAll("%cur", page + 1 + "").replaceAll("%max", totalPages + 1 + "").replaceAll("%word%", "all")).append("\n");
    //        Review r;
    //        // This might work xD
    //        for (int x = (page * 12); x < max; x++) {
    //            r = reviews[x];
    //            String color;
    //            if (player.getUUID().equals(r.reviewerUUID)) {
    //                if (player.getUUID().equals(r.submitterUUID)) {
    //                    color = "&3";
    //                } else {
    //                    color = "&9";
    //                }
    //            } else if (player.getUUID().equals(r.submitterUUID)) {
    //                color = "&a";
    //            } else {
    //                color = "&6";
    //            }
    //            if (r.reviewerName != null) {
    //                string.append("&8[&7"
    //                + r.server
    //                + "&8]"
    //                + "[&7"
    //                + r.area
    //                + "&8]"
    //                + "[&7"
    //                + r.id
    //                + "&8]"
    //                + color
    //                + r.submitterName
    //                + "&7 : "
    //                + color
    //                + r.reviewerName
    //                + "&7 : "
    //                + color
    //                + secToTime(r.timestamp)
    //                + "\n");
    //            } else {
    //                string.append("&8[&7" + r.server + "&8]" + "[&7" + r.area + "&8]" + "[&7" + r.id + "&8]" + color + r.submitterName + "&7 : " + color + secToTime(r.timestamp) + "\n");
    //            }
    //        }
    //        MainUtil.sendMessage(player, string.toString());
    //    }
    
    /**
     * Get all the reviews in a queue
     * @param queue
     * @return
     */
    public static HashSet<Review> getAllReviews(final Queue queue) {
        if (queue == null) {
            return new HashSet<>(ByteReview.reviewMap.values());
        }
        final HashSet<Review> reviews = new HashSet<>();
        final String name = queue.name;
        for (final Review review : ByteReview.reviewMap.values()) {
            if (review.queue.equals(name)) {
                reviews.add(review);
            }
        }
        return reviews;
    }
    
    /**
     * Get all pending reviews in a queue
     * @param queue
     * @return
     */
    public static HashSet<Review> getPending(final Queue queue) {
        final HashSet<Review> reviews = new HashSet<>();
        final String name = queue.name;
        for (final Review review : ByteReview.reviewMap.values()) {
            if ((review.state == ReviewState.PENDING) && review.queue.equals(name)) {
                reviews.add(review);
            }
        }
        return reviews;
    }
    
    /**
     * Calculate the average number of blocks changed for a queue
     * @param queue
     * @return
     */
    public static int getAverageChanges(final Queue queue) {
        int blocks = 0;
        int plots = 0;
        
        final String name = queue.name;
        for (final Review review : ByteReview.reviewMap.values()) {
            if (review.queue.equals(name)) {
                for (final Integer attempt : review.attempts) {
                    blocks += attempt;
                    plots++;
                }
            }
        }
        if (plots == 0) {
            return 0;
        }
        return blocks / plots;
    }
    
    /**
     * Get all the denied reviews in a queue
     * @param queue
     * @return
     */
    public static HashSet<Review> getDenied(final Queue queue) {
        final HashSet<Review> reviews = new HashSet<>();
        final String name = queue.name;
        for (final Review review : ByteReview.reviewMap.values()) {
            if ((review.state == ReviewState.DENIED) && review.queue.equals(name)) {
                reviews.add(review);
            }
        }
        return reviews;
    }
    
    /**
     * Get the review (or null) of a plot
     * @param plot
     * @return
     */
    public static Review getReview(final Plot plot) {
        Review review = ByteReview.reviewMap.get(getSimplePlot(plot));
        if (review == null) {
            review = getReview(null, plot.getArea().toString(), plot.getId(), plot.owner, null, null, ReviewState.DENIED, null, 0);
        }
        return review;
    }
    
    public static ReviewState getState(final int i) {
        switch (i) {
            case 0: {
                return ReviewState.DENIED;
            }
            case 1: {
                return ReviewState.PENDING;
            }
            case 2: {
                return ReviewState.APPROVED;
            }
            default: {
                return ReviewState.DENIED;
            }
        }
    }
    
    public static int getOridinal(final ReviewState state) {
        switch (state) {
            case DENIED: {
                return 0;
            }
            case PENDING: {
                return 1;
            }
            case APPROVED: {
                return 2;
            }
        }
        return 0;
    }
    
    /**
     * Add a plot to the queue with the given number of changes (updates DB)
     * @param plot
     * @param queue
     * @param changes
     */
    public static void addToQueue(final Plot plot, final Queue queue, final int changes) {
        final Review review = getReview(plot);
        review.state = ReviewState.PENDING;
        review.attempts.add(changes);
        review.timestamp = (int) (System.currentTimeMillis() / 1000);
        review.queue = queue.name;
        FlagManager.addPlotFlag(plot, new Flag(FlagManager.getFlag("done"), "" + (System.currentTimeMillis() / 1000)));
        if (review.attempts.size() == 1) {
            ByteReview.database.addReview(review);
            ByteReview.reviewMap.put(getSimplePlot(plot), review);
        } else {
            ByteReview.database.setState(review);
        }
        Bukkit.getPluginManager().callEvent(new ReviewEvent(null, review));
    }
    
    /**
     * Get a new review object from a reduced number of parameters (safer to use as it has null checks)
     * @param server
     * @param world
     * @param id
     * @param submitter
     * @param reviewer
     * @param queue
     * @param timestamp
     * @return
     */
    public static Review getReview(String server, String areaname, final PlotId id, final UUID submitter, final UUID reviewer, final String queue, ReviewState state, List<Integer> attempts,
    final int timestamp) {
        if (server == null) {
            server = ReviewUtil.server;
        }
        if (areaname == null) {
            areaname = PS.get().getFirstPlotArea().worldname;
        }
        String submitterName = null;
        if (submitter != null) {
            final String sName = UUIDHandler.getName(submitter);
            if (sName == null) {
                submitterName = "unknown";
            } else {
                submitterName = sName;
            }
        }
        String reviewerName = null;
        if (reviewer != null) {
            final String rName = UUIDHandler.getName(reviewer);
            if (rName == null) {
                reviewerName = "unknown";
            } else {
                reviewerName = rName;
            }
        }
        if (state == null) {
            state = reviewer == null ? ReviewState.PENDING : ReviewState.DENIED;
        }
        if (attempts == null) {
            attempts = new ArrayList<>();
        }
        
        return new Review(server, areaname, id, submitter, submitterName, reviewer, reviewerName, queue, state, attempts, timestamp);
    }
}
