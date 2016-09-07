package com.boydti.review.util;

import java.util.Arrays;
import java.util.Collections;

import lilypad.client.connect.api.Connect;
import lilypad.client.connect.api.event.EventListener;
import lilypad.client.connect.api.event.MessageEvent;
import lilypad.client.connect.api.request.impl.MessageRequest;
import lilypad.client.connect.api.request.impl.RedirectRequest;
import lilypad.client.connect.api.result.FutureResultListener;
import lilypad.client.connect.api.result.StatusCode;
import lilypad.client.connect.api.result.impl.RedirectResult;

import org.bukkit.Bukkit;

import com.boydti.review.listener.MainListener;
import com.boydti.review.object.Review;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.StringMan;
import com.intellectualcrafters.plot.util.TaskManager;
import com.intellectualcrafters.plot.util.UUIDHandler;

public class LilyUtil {
    public Connect connect;
    
    private final String channel = "bytereview";
    
    public LilyUtil() {
        this.connect = Bukkit.getServer().getServicesManager().getRegistration(Connect.class).getProvider();
        this.connect.registerEvents(this);
    }
    
    public boolean teleport(final PlotPlayer player, final Review review) {
        if (ReviewUtil.server.equals(review.server)) {
            return false;
        }
        String message = review.id + " " + review.area + " " + player.getName();
        sendMessage(message, review.server);
        try {
            this.connect.request(new RedirectRequest(review.server, player.getName())).registerListener(new FutureResultListener<RedirectResult>() {
                @Override
                public void onResult(final RedirectResult redirectResult) {
                    if (redirectResult.getStatusCode() == StatusCode.SUCCESS) {
                        return;
                    }
                    player.sendMessage("Could not connect");
                }
            });
            
        } catch (final Exception exception) {
            player.sendMessage("Could not connect");
            return false;
        }
        return true;
    }
    
    public String getServerName() {
        return connect.getSettings().getUsername();
    }
    
    public void sendMessage(final String message, final String server) {
        try {
            final MessageRequest request;
            if (server == null) {
                request = new MessageRequest(Collections.EMPTY_LIST, channel, message);
            } else {
                request = new MessageRequest(Arrays.asList(server), channel, message);
            }
            
            connect.request(request);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
    
    @EventListener
    public void onMessage(final MessageEvent event) {
        if (!event.getChannel().equalsIgnoreCase(channel)) {
            return;
        }
        try {
            final String message = event.getMessageAsString();
            String[] split = message.split(" ");
            switch (split[0]) {
                case "msg": {
                    String perm = split[1];
                    String msg = StringMan.join(Arrays.copyOfRange(split, 2, split.length), " ");
                    for (PlotPlayer pp : UUIDHandler.getPlayers().values()) {
                        if (Permissions.hasPermission(pp, perm)) {
                            MainUtil.sendMessage(pp, msg);
                        }
                    }
                    return;
                }
            }
            PlotId id = PlotId.fromString(split[0]);
            String areaname = split[1];
            final String user = split[2];
            Plot plot = null;
            PlotArea area = PS.get().getPlotAreaByString(areaname);
            if (area != null) {
                plot = area.getPlot(id);
                if (plot != null) {
                    MainListener.teleport.put(user, plot);
                    TaskManager.runTaskLater(new Runnable() {
                        @Override
                        public void run() {
                            MainListener.teleport.remove(user);
                        }
                    }, 20);
                    return;
                }
            }
            PS.debug("Invalid teleport location: " + message);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
    
}
