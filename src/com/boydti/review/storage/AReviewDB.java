package com.boydti.review.storage;

import com.boydti.review.object.Queue;
import com.boydti.review.object.Review;

public abstract class AReviewDB {
    public abstract boolean populateReviews(Runnable paramRunnable);
    
    public abstract boolean populateQueues(Runnable paramRunnable);
    
    public abstract void addReview(Review paramReview);
    
    public abstract void setAssigned(Review paramReview);
    
    public abstract void setState(Review paramReview);
    
    public abstract void setTransfer(Review paramReview);
    
    public abstract void removeReview(Review paramReview);
    
    public abstract void addQueue(Queue paramQueue);
    
    public abstract void setCommands(Queue paramQueue);
    
    public abstract void removeQueue(Queue paramQueue);
    
    public abstract void clearQueue(Queue paramQueue);
    
    public abstract void setServers(Queue paramQueue);
    
    public abstract void setWorlds(Queue paramQueue);
}
