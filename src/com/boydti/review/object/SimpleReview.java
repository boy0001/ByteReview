package com.boydti.review.object;

import com.intellectualcrafters.plot.database.DBFunc;
import java.util.UUID;

public class SimpleReview {
    public final long timestamp;
    public final UUID reviewer;
    public final String queue;
    
    public boolean isReviewed() {
        return this.timestamp != 0L;
    }
    
    public SimpleReview(long timestamp, UUID reviewer, String queue) {
        this.timestamp = timestamp;
        if (reviewer == null) {
            this.reviewer = DBFunc.everyone;
        } else {
            this.reviewer = reviewer;
        }
        if (this.queue == null) {
            this.queue = "default";
        } else {
            this.queue = queue;
        }
    }
}
