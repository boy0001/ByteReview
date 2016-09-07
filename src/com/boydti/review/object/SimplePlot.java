package com.boydti.review.object;

import com.intellectualcrafters.plot.object.PlotId;

public class SimplePlot {
    public final String server;
    public final String area;
    public final PlotId id;
    
    public SimplePlot(String server, String area, PlotId id) {
        this.server = server;
        this.area = area;
        this.id = id;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 31 * hash + this.server.hashCode();
        hash = 31 * hash + this.area.hashCode();
        hash = 31 * hash + this.id.hashCode();
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SimplePlot other = (SimplePlot) obj;
        return (this.id.equals(other.id)) && (other.server.equals(this.server)) && (other.area.equals(this.area));
    }
}
