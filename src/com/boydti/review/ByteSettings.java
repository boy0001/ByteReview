package com.boydti.review;

import java.util.HashMap;
import java.util.Map.Entry;

import com.boydti.review.object.Queue;
import com.boydti.review.util.ReviewUtil;
import com.intellectualcrafters.plot.object.PlotArea;

public class ByteSettings {
    public static int SYNC_INTERVAL = 1;
    public static int MIN_CHANGES = 0;
    public static int UNASIGN_TIME = 240;
    public static int COOLDOWN = 0;
    public static String WEB_URL = "http://empcraft.com/reviews/";
    public static boolean APPROVE_BYPASS = false;
    public static HashMap<String, String> DEFAULTS = new HashMap<String, String>();
    
    public static String getDefault(PlotArea area) {
        if (area == null) {
            if (ByteReview.queueMap.size() == 1) {
                return ByteReview.queueMap.keySet().iterator().next();
            }
            if (ByteReview.queueMap.containsKey("default")) {
                return "default";
            }
            return null;
        }
        String value = DEFAULTS.get(area.worldname);
        if (value == null) {
            value = DEFAULTS.get(area.toString());
        }
        if (value == null) {
            if (ByteReview.queueMap.containsKey("default")) {
                return "default";
            }
            for (Entry<String, Queue> entry : ByteReview.queueMap.entrySet()) {
                Queue queue = entry.getValue();
                if ((queue.isServerAllowed(ReviewUtil.server)) && (queue.isAreaAllowed(area))) {
                    return entry.getKey();
                }
            }
            return "default";
        }
        return value;
    }
}
