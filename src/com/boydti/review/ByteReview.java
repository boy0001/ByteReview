package com.boydti.review;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.boydti.review.command.CommandManager;
import com.boydti.review.listener.MainListener;
import com.boydti.review.object.InboxReview;
import com.boydti.review.object.Queue;
import com.boydti.review.object.Review;
import com.boydti.review.object.SimplePlot;
import com.boydti.review.storage.AReviewDB;
import com.boydti.review.storage.ReviewSQL;
import com.boydti.review.util.LilyUtil;
import com.boydti.review.util.ReviewUtil;
import com.intellectualcrafters.plot.util.CommentManager;
import com.intellectualcrafters.plot.util.TaskManager;
import com.plotsquared.bukkit.util.BukkitUtil;

public class ByteReview extends JavaPlugin {
    public static ByteReview plugin;
    public static HashMap<SimplePlot, Review> reviewMap = new HashMap();
    public static HashMap<String, Queue> queueMap = new HashMap();
    public static FileConfiguration config;
    public static LilyUtil lily;
    public static AReviewDB database;
    
    @Override
    public void onEnable() {
        database = new ReviewSQL();
        plugin = this;
        setupLily();
        setupConfiguration();
        setupCommands();
        CommentManager.addInbox(new InboxReview());
        Bukkit.getServer().getPluginManager().registerEvents(new MainListener(), this);
        TaskManager.runTaskRepeat(new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ReviewUtil.notify(BukkitUtil.getPlayer(player));
                }
                ByteReview.database.populateQueues(null);
                ByteReview.database.populateReviews(null);
            }
        }, ByteSettings.SYNC_INTERVAL * 1200);
    }
    
    public void setupLily() {
        try {
            lily = new LilyUtil();
            ReviewUtil.server = lily.getServerName();
        } catch (Throwable e) {
            ReviewUtil.server = Bukkit.getServerName();
        }
    }
    
    public static void setupCommands() {
        new CommandManager();
    }
    
    public static void setupConfiguration() {
        if (config == null) {
            plugin.saveDefaultConfig();
        }
        config = plugin.getConfig();
        Map<String, Object> options = new HashMap();
        options.put("unasign-timout-min", Integer.valueOf(ByteSettings.UNASIGN_TIME));
        options.put("min-changed", Integer.valueOf(ByteSettings.MIN_CHANGES));
        options.put("resubmit-cooldown", Integer.valueOf(ByteSettings.COOLDOWN));
        options.put("approved-bypass-claim-limit", Boolean.valueOf(ByteSettings.APPROVE_BYPASS));
        options.put("web-url", ByteSettings.WEB_URL);
        options.put("sync-interval-minutes", Integer.valueOf(ByteSettings.SYNC_INTERVAL));
        if (!config.contains("defaults")) {
            options.put("defaults.plotworld.queue", "default");
        }
        for (Map.Entry<String, Object> node : options.entrySet()) {
            if (!config.contains(node.getKey())) {
                config.set(node.getKey(), node.getValue());
            }
        }
        for (String world : config.getConfigurationSection("defaults").getKeys(false)) {
            ByteSettings.DEFAULTS.put(world.toLowerCase(), config.getString("defaults." + world + ".queue").toLowerCase());
        }
        ByteSettings.WEB_URL = config.getString("web-url");
        ByteSettings.APPROVE_BYPASS = config.getBoolean("approved-bypass-claim-limit");
        ByteSettings.COOLDOWN = config.getInt("resubmit-cooldown");
        ByteSettings.MIN_CHANGES = config.getInt("min-changed");
        ByteSettings.UNASIGN_TIME = config.getInt("unasign-timout-min");
        ByteSettings.SYNC_INTERVAL = config.getInt("sync-interval-minutes");
        plugin.saveConfig();
    }
    
    public static void addQueue(Queue queue) {
        queueMap.put(queue.name, queue);
    }
    
    public static Queue getQueue(String name) {
        return queueMap.get(name);
    }
    
    public static Collection<Queue> getQueues() {
        return queueMap.values();
    }
    
    public static void addReview(Review review) {
        SimplePlot plot = new SimplePlot(review.server, review.area, review.id);
        reviewMap.put(plot, review);
    }
}
