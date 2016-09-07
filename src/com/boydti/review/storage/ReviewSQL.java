package com.boydti.review.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import com.boydti.review.ByteReview;
import com.boydti.review.ByteSettings;
import com.boydti.review.listener.MainListener;
import com.boydti.review.object.Queue;
import com.boydti.review.object.Review;
import com.boydti.review.object.ReviewState;
import com.boydti.review.util.ReviewUtil;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.database.SQLManager;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.util.TaskManager;

public class ReviewSQL extends AReviewDB {
    private final SQLManager man;
    private boolean popQue = false;
    
    private boolean popRev = false;
    
    private final String CREATE_TABLE_REVIEW_MYSQL = "CREATE TABLE IF NOT EXISTS `review` (`id` INT(11) NOT NULL AUTO_INCREMENT,`server` VARCHAR(40),`world` VARCHAR(40),`submitterUUID` VARCHAR(40),`submitterName` VARCHAR(40),`reviewerUUID` VARCHAR(40),`reviewerName` VARCHAR(40),`queue` VARCHAR(40),`id_x` INT(11),`id_z` INT(11),`attempts` VARCHAR(40),`state` INT(11),`timestamp` INT(11),PRIMARY KEY (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0";
    
    private final String CREATE_TABLE_QUEUE_MYSQL = "CREATE TABLE IF NOT EXISTS `queue` (`id` INT(11) NOT NULL AUTO_INCREMENT,`servers` VARCHAR(40),`worlds` VARCHAR(40),`name` VARCHAR(40),`commands` VARCHAR(1000),PRIMARY KEY (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0";
    
    private final String CREATE_TABLE_REVIEW_SQLITE = "CREATE TABLE IF NOT EXISTS `review` (`id` INTEGER PRIMARY KEY AUTOINCREMENT,`server` VARCHAR(40),`world` VARCHAR(40),`submitterUUID` VARCHAR(40),`submitterName` VARCHAR(40),`reviewerUUID` VARCHAR(40),`reviewerName` VARCHAR(40),`queue` VARCHAR(40),`id_x` INT(11),`id_z` INT(11),`attempts` VARCHAR(40),`state` INT(11),`timestamp` INT(11))";
    
    private final String CREATE_TABLE_QUEUE_SQLITE = "CREATE TABLE IF NOT EXISTS `queue` (`id` INTEGER PRIMARY KEY AUTOINCREMENT,`servers` VARCHAR(40),`worlds` VARCHAR(40),`name` VARCHAR(40),`commands` VARCHAR(1000))";
    
    private final String SELECT_REVIEW = "SELECT `server`, `world`, `submitterUUID`, `reviewerUUID`, `queue`, `id_x`, `id_z`, `attempts`, `state`, `timestamp` FROM `review`";
    
    private final String SELECT_QUEUE = "SELECT `servers`, `worlds`, `name`, `commands` FROM `queue`";
    
    private final String ADD_REVIEW = "INSERT INTO `review` (`server`,`world`,`submitterUUID`,`submitterName`,`queue`,`id_x`,`id_z`, `attempts`, `state`,`timestamp`) VALUES(?,?,?,?,?,?,?,?,?,?)";
    
    private final String ADD_QUEUE = "INSERT INTO `queue` (`servers`,`worlds`,`name`) VALUES(?,?,?)";
    
    private final String DELETE_REVIEW = "DELETE FROM `review` WHERE `server` = ? AND `world` = ? AND `id_x` = ? AND `id_z` = ?";
    
    private final String CLEAR_QUEUE = "DELETE FROM `review` WHERE `queue` = ?";
    
    private final String DELETE_QUEUE = "DELETE FROM `queue` WHERE `name` = ?";
    
    final String SET_STATE = "UPDATE `review` SET `reviewerUUID` = ?, `reviewerName` = ?, `state` = ?, `timestamp` = ?, `attempts` = ? WHERE `server` = ? AND `world` = ? AND `id_x` = ? AND `id_z` = ?";
    
    final String SET_ASSIGNED = "UPDATE `review` SET `reviewerUUID` = ?, `reviewerName` = ? WHERE `server` = ? AND `world` = ? AND `id_x` = ? AND `id_z` = ?";
    
    final String SET_TRANSFER = "UPDATE `review` SET `queue` = ?, `state` = ? WHERE `server` = ? AND `world` = ? AND `id_x` = ? AND `id_z` = ?";
    
    final String SET_COMMANDS = "UPDATE `queue` SET `commands` = ? WHERE `name` = ?";
    
    final String SET_SERVERS = "UPDATE `queue` SET `servers` = ? WHERE `name` = ?";
    
    final String SET_AREAS = "UPDATE `queue` SET `worlds` = ? WHERE `name` = ?";
    
    public ReviewSQL() {
        this.man = ((SQLManager) DBFunc.dbManager);
        Connection con = this.man.getConnection();
        try {
            Statement stmt = con.createStatement();
            if (Settings.DB.USE_MYSQL) {
                stmt.addBatch(CREATE_TABLE_QUEUE_MYSQL);
                stmt.addBatch(CREATE_TABLE_REVIEW_MYSQL);
            } else {
                stmt.addBatch(CREATE_TABLE_QUEUE_SQLITE);
                stmt.addBatch(CREATE_TABLE_REVIEW_SQLITE);
            }
            stmt.executeBatch();
            stmt.clearBatch();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        populateReviews(null);
        populateQueues(null);
    }
    
    @Override
    public boolean populateReviews(final Runnable whenDone) {
        if (this.popRev) {
            return false;
        }
        this.popRev = true;
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final HashSet<Review> reviews = new HashSet<Review>();
                    Connection con = ReviewSQL.this.man.getConnection();
                    Statement stmt = con.createStatement();
                    ResultSet r = stmt.executeQuery(SELECT_REVIEW);
                    List<Review> unassign = new ArrayList<Review>();
                    String areaname;
                    while (r.next()) {
                        String server = r.getString("server");
                        areaname = r.getString("world");
                        String queue = r.getString("queue");
                        int id_x = r.getInt("id_x");
                        int id_z = r.getInt("id_z");
                        ReviewState state = ReviewUtil.getState(r.getInt("state"));
                        String attemptStr = r.getString("attempts");
                        ArrayList<Integer> attempts = new ArrayList<Integer>();
                        if ((attemptStr != null) && (attemptStr.length() > 0)) {
                            for (String attempt : attemptStr.split(",")) {
                                attempts.add(Integer.valueOf(Integer.parseInt(attempt)));
                            }
                        }
                        PlotId id = new PlotId(id_x, id_z);
                        int timestamp = r.getInt("timestamp");
                        if (server.equals(ReviewUtil.server)) {
                            PlotArea area = PS.get().getPlotAreaByString(areaname);
                            if (area == null) {
                                r.deleteRow();
                                continue;
                            }
                            Plot plot = area.getOwnedPlot(id);
                            if (plot == null) {
                                r.deleteRow();
                                continue;
                            }
                            if (!plot.getSettings().flags.containsKey("done")) {
                                FlagManager.addPlotFlag(plot, new Flag(FlagManager.getFlag("done"), "" + (timestamp)));
                            }
                        }
                        String submitterStr = r.getString("submitterUUID");
                        String reviewStr = r.getString("reviewerUUID");
                        UUID submitter = null;
                        UUID reviewer = null;
                        if (submitterStr != null) {
                            submitter = UUID.fromString(submitterStr);
                        }
                        if (reviewStr != null) {
                            reviewer = UUID.fromString(reviewStr);
                        }
                        Review review = ReviewUtil.getReview(server, areaname, id, submitter, reviewer, queue, state, attempts, timestamp);
                        if (reviewer != null) {
                            long last = MainListener.getLastPlayed(reviewer);
                            if (System.currentTimeMillis() / 60000L - last > ByteSettings.UNASIGN_TIME) {
                                review.reviewerName = null;
                                review.reviewerUUID = null;
                                unassign.add(review);
                            }
                        }
                        reviews.add(review);
                    }
                    for (Review review : unassign) {
                        ReviewSQL.this.setAssigned(review);
                    }
                    TaskManager.runTask(new Runnable() {
                        @Override
                        public void run() {
                            ByteReview.reviewMap = new HashMap<>();
                            for (Review review : reviews) {
                                if (ByteSettings.APPROVE_BYPASS) {
                                    Plot plot = ReviewUtil.getPlot(review);
                                    plot.countsTowardsMax = false;
                                }
                                ByteReview.addReview(review);
                            }
                            TaskManager.runTask(whenDone);
                        }
                    });
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ReviewSQL.this.popRev = false;
            }
        });
        return true;
    }
    
    @Override
    public boolean populateQueues(final Runnable whenDone) {
        if (this.popQue) {
            return false;
        }
        this.popQue = true;
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final HashSet<Queue> queues = new HashSet<Queue>();
                    Connection con = ReviewSQL.this.man.getConnection();
                    Statement stmt = con.createStatement();
                    ResultSet r = stmt.executeQuery(SELECT_QUEUE);
                    while (r.next()) {
                        String serverStr = r.getString("servers");
                        String areaStr = r.getString("worlds");
                        String name = r.getString("name");
                        String commands = r.getString("commands");
                        
                        Queue queue = new Queue(name);
                        if ((serverStr != null) && (serverStr.length() > 0)) {
                            for (String server : serverStr.split(";")) {
                                queue.servers.add(server);
                            }
                        } else {
                            queue.servers.add("*");
                        }
                        if ((areaStr != null) && (areaStr.length() > 0)) {
                            for (String area : areaStr.split(";")) {
                                queue.areas.add(area);
                            }
                        } else {
                            queue.areas.add("*");
                        }
                        if ((commands != null) && (commands.length() > 0)) {
                            for (String command : commands.split(";")) {
                                queue.commands.add(command);
                            }
                        }
                        queues.add(queue);
                    }
                    stmt.close();
                    TaskManager.runTask(new Runnable() {
                        @Override
                        public void run() {
                            ByteReview.queueMap = new HashMap();
                            for (Queue queue : queues) {
                                ByteReview.addQueue(queue);
                            }
                            TaskManager.runTask(whenDone);
                        }
                    });
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ReviewSQL.this.popQue = false;
            }
        });
        return true;
    }
    
    @Override
    public void addReview(final Review review) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con.prepareStatement(ADD_REVIEW);
                    //          PreparedStatement stmt = con.prepareStatement("INSERT INTO `review` (`server`,`world`,`submitterUUID`,`submitterName`,`queue`,`id_x`,`id_z`, `attempts`, `state`,`timestamp`) VALUES(?,?,?,?,?,?,?,?,?,?)");
                    stmt.setString(1, review.server);
                    stmt.setString(2, review.area);
                    stmt.setString(3, review.submitterUUID.toString());
                    stmt.setString(4, review.submitterName);
                    stmt.setString(5, review.queue);
                    stmt.setInt(6, review.id.x);
                    stmt.setInt(7, review.id.y);
                    stmt.setString(8, StringUtils.join(review.attempts, ","));
                    stmt.setInt(9, ReviewUtil.getOridinal(review.state));
                    stmt.setInt(10, review.timestamp);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void addQueue(final Queue queue) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con.prepareStatement(ADD_QUEUE);
                    //          PreparedStatement stmt = con.prepareStatement("INSERT INTO `queue` (`servers`,`worlds`,`name`) VALUES(?,?,?)");
                    stmt.setString(1, "");
                    stmt.setString(2, "");
                    stmt.setString(3, queue.name);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void removeReview(final Review review) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con.prepareStatement(DELETE_REVIEW);
                    //          PreparedStatement stmt = con.prepareStatement("DELETE FROM `review` WHERE `server` = ? AND `world` = ? AND `id_x` = ? AND `id_z` = ?");
                    stmt.setString(1, review.server);
                    stmt.setString(2, review.area);
                    stmt.setInt(3, review.id.x);
                    stmt.setInt(4, review.id.y);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void removeQueue(final Queue queue) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con.prepareStatement(DELETE_QUEUE);
                    //          PreparedStatement stmt = con.prepareStatement("DELETE FROM `queue` WHERE `name` = ?");
                    stmt.setString(1, queue.name);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void setState(final Review review) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con
                    .prepareStatement("UPDATE `review` SET `reviewerUUID` = ?, `reviewerName` = ?, `state` = ?, `timestamp` = ?, `attempts` = ? WHERE `server` = ? AND `world` = ? AND `id_x` = ? AND `id_z` = ?");
                    stmt.setString(1, review.reviewerUUID.toString());
                    stmt.setString(2, review.reviewerName);
                    stmt.setInt(3, ReviewUtil.getOridinal(review.state));
                    stmt.setInt(4, review.timestamp);
                    stmt.setString(5, StringUtils.join(review.attempts, ","));
                    stmt.setString(6, review.server);
                    stmt.setString(7, review.area);
                    stmt.setInt(8, review.id.x);
                    stmt.setInt(9, review.id.y);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void setCommands(final Queue queue) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con.prepareStatement("UPDATE `queue` SET `commands` = ? WHERE `name` = ?");
                    stmt.setString(1, StringUtils.join(queue.commands, ";"));
                    stmt.setString(2, queue.name);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void setAssigned(final Review review) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con.prepareStatement(SET_ASSIGNED);
                    //          PreparedStatement stmt = con.prepareStatement("UPDATE `review` SET `reviewerUUID` = ?, `reviewerName` = ? WHERE `server` = ? AND `world` = ? AND `id_x` = ? AND `id_z` = ?");
                    if (review.reviewerUUID == null) {
                        stmt.setNull(1, 12);
                        stmt.setNull(2, 12);
                    } else {
                        stmt.setString(1, review.reviewerUUID.toString());
                        stmt.setString(2, review.reviewerName);
                    }
                    stmt.setString(3, review.server);
                    stmt.setString(4, review.area);
                    stmt.setInt(5, review.id.x);
                    stmt.setInt(6, review.id.y);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void setTransfer(final Review review) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con.prepareStatement(SET_TRANSFER);
                    //          PreparedStatement stmt = con.prepareStatement("UPDATE `review` SET `queue` = ?, `state` = ? WHERE `server` = ? AND `world` = ? AND `id_x` = ? AND `id_z` = ?");
                    stmt.setString(1, review.queue);
                    stmt.setInt(2, ReviewUtil.getOridinal(review.state));
                    stmt.setString(3, review.server);
                    stmt.setString(4, review.area);
                    stmt.setInt(5, review.id.x);
                    stmt.setInt(6, review.id.y);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void setServers(final Queue queue) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con.prepareStatement(SET_SERVERS);
                    //          PreparedStatement stmt = con.prepareStatement("UPDATE `queue` SET `servers` = ? WHERE `name` = ?");
                    stmt.setString(1, StringUtils.join(queue.servers, ";"));
                    stmt.setString(2, queue.name);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void setWorlds(final Queue queue) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con.prepareStatement(SET_AREAS);
                    //          PreparedStatement stmt = con.prepareStatement("UPDATE `queue` SET `worlds` = ? WHERE `name` = ?");
                    stmt.setString(1, StringUtils.join(queue.areas, ";"));
                    stmt.setString(2, queue.name);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void clearQueue(final Queue queue) {
        TaskManager.runTaskAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = ReviewSQL.this.man.getConnection();
                    PreparedStatement stmt = con.prepareStatement(CLEAR_QUEUE);
                    //          PreparedStatement stmt = con.prepareStatement("DELETE FROM `review` WHERE `queue` = ?");
                    stmt.setString(1, queue.name);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
