package com.gizlirotalar.app.models;

import com.google.firebase.firestore.Exclude;

public class Report {
    private String id;
    private String spotId;
    private String spotName;
    private String userId;
    private String userName;
    private String reason;
    private String description;
    private String status; // pending, approved, rejected
    private long createdAt;

    public Report() { this.status = "pending"; this.createdAt = System.currentTimeMillis(); }

    public Report(String spotId, String spotName, String userId, String userName, String reason, String description) {
        this.spotId = spotId;
        this.spotName = spotName;
        this.userId = userId;
        this.userName = userName;
        this.reason = reason;
        this.description = description;
        this.status = "pending";
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isPending() { return "pending".equals(status); }

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }
    public String getSpotId() { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }
    public String getSpotName() { return spotName; }
    public void setSpotName(String spotName) { this.spotName = spotName; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
