package com.boydti.review.util;

import com.boydti.review.object.Review;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ReviewEvent extends Event {
    private static HandlerList handlers = new HandlerList();
    private final Review review;
    private final Review previous;
    
    public ReviewEvent(Review previous, Review review) {
        this.review = review;
        this.previous = previous;
    }
    
    public Review getTo() {
        return this.review;
    }
    
    public Review getFrom() {
        return this.previous;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    public HandlerList getHandlers() {
        return handlers;
    }
}
