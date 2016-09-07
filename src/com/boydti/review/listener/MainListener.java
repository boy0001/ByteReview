package com.boydti.review.listener;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.boydti.review.ByteReview;
import com.boydti.review.object.Review;
import com.boydti.review.util.ReviewUtil;
import com.intellectualcrafters.plot.object.OfflinePlotPlayer;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.bukkit.events.PlotDeleteEvent;
import com.plotsquared.bukkit.util.BukkitUtil;

public class MainListener implements Listener {
    
    public static HashMap<String, Plot> teleport = new HashMap<>();
    
    public static HashMap<UUID, Long> last = new HashMap<>();
    
    public static long getLastPlayed(UUID uuid) {
        if (!last.containsKey(uuid)) {
            OfflinePlotPlayer op = UUIDHandler.getUUIDWrapper().getOfflinePlayer(uuid);
            if (op == null) {
                return 0;
            }
            last.put(uuid, op.getLastPlayed() / 60000);
        }
        return last.get(uuid);
    }
    
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final PlotPlayer player = BukkitUtil.getPlayer(event.getPlayer());
        last.put(player.getUUID(), System.currentTimeMillis() / 60000);
    }
    
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final PlotPlayer player = BukkitUtil.getPlayer(event.getPlayer());
        last.put(player.getUUID(), System.currentTimeMillis() / 60000);
        ReviewUtil.notify(player);
        Plot plot = teleport.get(player.getName());
        if (plot != null) {
            Review review = ReviewUtil.getReview(plot);
            if (review == null) {
                MainUtil.sendMessage(player, "&7Cannot find review at: &a" + plot);
                return;
            }
            review.reviewerName = player.getName();
            review.reviewerUUID = player.getUUID();
            MainUtil.sendMessage(player, "&7Assigned: &a" + plot);
            ByteReview.database.setAssigned(review);
            plot.teleportPlayer(player);
            teleport.remove(player.getName());
        }
    }
    
    @EventHandler
    public void onCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final String message = event.getMessage().toLowerCase().replaceAll("/", "").trim();
        if (message.length() == 0) {
            return;
        }
        String[] split = message.split(" ");
        PluginCommand cmd = Bukkit.getServer().getPluginCommand(split[0]);
        if (cmd == null) {
            if (split[0].equals("review")) {
                final Player player = event.getPlayer();
                PlotPlayer pp = BukkitUtil.getPlayer(player);
                MainUtil.sendMessage(pp, "&7To request a review use &c/plot submit");
                MainUtil.sendMessage(pp, "&7These other commands may also be useful:");
                if (Permissions.hasPermission(pp, "plots.review")) {
                    MainUtil.sendMessage(pp, "&8 - &c/plot review");
                }
                if (Permissions.hasPermission(pp, "plots.queue")) {
                    MainUtil.sendMessage(pp, "&8 - &c/plot queue");
                }
                if (Permissions.hasPermission(pp, "plots.next")) {
                    MainUtil.sendMessage(pp, "&8 - &c/plot next");
                }
                if (Permissions.hasPermission(pp, "plots.rate")) {
                    MainUtil.sendMessage(pp, "&8 - &c/plot rate");
                }
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlotDelete(final PlotDeleteEvent event) {
        ReviewUtil.removeReview(ReviewUtil.getReview(event.getPlot()));
    }
    
    /*
     * Notify on join if your plot submission failed. TODO
     * Notify on join (staff) about any pending reviews you are designated
     */
    
    /*
     * On plot delete event, remove from review system TODO
     */
    
    /*
     * On interact with plot sign, open comment book TODO
     */
}
