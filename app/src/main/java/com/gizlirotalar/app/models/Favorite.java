package com.gizlirotalar.app.models;

import com.google.firebase.firestore.Exclude;

public class Favorite {
    private String id;
    private String userId;
    private String spotId;
    private String spotName;
    private long createdAt;

    public Favorite() {}

    public Favorite(String userId, String spotId, String spotName) {
        this.userId = userId;
        this.spotId = spotId;
        this.spotName = spotName;
        this.createdAt = System.currentTimeMillis();
    }

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSpotId() { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }
    public String getSpotName() { return spotName; }
    public void setSpotName(String spotName) { this.spotName = spotName; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
