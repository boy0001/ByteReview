package com.boydti.review.object;

import java.util.List;
import java.util.UUID;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;

public class Review {
    public final String server;
    public final String area;
    public final PlotId id;
    public final UUID submitterUUID;
    @Deprecated
    public final String submitterName;
    public UUID reviewerUUID;
    @Deprecated
    public String reviewerName;
    public int timestamp;
    public String queue;
    public final List<Integer> attempts;
    public ReviewState state;
    
    public Review(String server, String area, PlotId id, UUID submitter, String submitterName, UUID reviewer, String reviewerName, String queue, ReviewState state, List<Integer> attempts,
    int timestamp) {
        this.server = server;
        this.area = area;
        this.id = id;
        this.submitterUUID = submitter;
        this.submitterName = submitterName;
        this.reviewerUUID = reviewer;
        this.reviewerName = reviewerName;
        this.queue = queue;
        this.state = state;
        this.attempts = attempts;
        this.timestamp = timestamp;
    }
    
    public Plot getPlot() {
        PlotArea area = PS.get().getPlotAreaByString(this.area);
        if (area == null) {
            return null;
        }
        return area.getOwnedPlot(id);
    }
    
    @Override
    public Review clone() {
        return new Review(this.server, this.area, this.id, this.submitterUUID, this.submitterName, this.reviewerUUID, this.reviewerName, this.queue, this.state, this.attempts, this.timestamp);
    }
    
    @Override
    public String toString() {
        return "server" + ";" + area + ";" + id;
    }
}
