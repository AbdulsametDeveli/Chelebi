package com.gizlirotalar.app.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class Spot {
    @Exclude
    private String id;
    private String name;
    private String description;
    private String category;
    private double latitude;
    private double longitude;
    private int difficulty;
    private boolean dangerous;
    private String imageUrl;
    private String addedBy;
    private String addedByName;
    private long createdAt;

    public Spot() {
    }

    public Spot(String name, String description, String category, double latitude, double longitude, 
                int difficulty, boolean dangerous, String imageUrl, String addedBy, String addedByName, long createdAt) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.difficulty = difficulty;
        this.dangerous = dangerous;
        this.imageUrl = imageUrl;
        this.addedBy = addedBy;
        this.addedByName = addedByName;
        this.createdAt = createdAt;
    }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    @PropertyName("isDangerous")
    public boolean isDangerous() { return dangerous; }
    @PropertyName("isDangerous")
    public void setDangerous(boolean dangerous) { this.dangerous = dangerous; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }

    public String getAddedByName() { return addedByName; }
    public void setAddedByName(String addedByName) { this.addedByName = addedByName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // ─── Geçici istatistik alanları (Firestore'a yazılmaz) ───
    @Exclude private int commentCount;
    @Exclude private int favoriteCount;
    @Exclude private float avgRating;
    @Exclude private String lastComment = "";

    @Exclude public int getCommentCount() { return commentCount; }
    @Exclude public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    @Exclude public int getFavoriteCount() { return favoriteCount; }
    @Exclude public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
    @Exclude public float getAvgRating() { return avgRating; }
    @Exclude public void setAvgRating(float avgRating) { this.avgRating = avgRating; }
    @Exclude public String getLastComment() { return lastComment; }
    @Exclude public void setLastComment(String lastComment) { this.lastComment = lastComment; }
}
