package com.gizlirotalar.app.models;

import com.google.firebase.firestore.Exclude;

public class Rating {
    private String id;
    private String spotId;
    private String userId;
    private String userName;
    private int rating; // 1-5
    private String comment;
    private long createdAt;

    public Rating() {}

    public Rating(String spotId, String userId, String userName, int rating, String comment) {
        this.spotId = spotId;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = System.currentTimeMillis();
    }

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }
    public String getSpotId() { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
