package com.clothauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClothIdentity {
    @JsonProperty("cloth_id")
    private String clothId;
    
    @JsonProperty("features_hash")
    private String featuresHash;
    
    @JsonProperty("timestamp_hash")
    private String timestampHash;
    
    @JsonProperty("combined_hash")
    private String combinedHash;
    
    @JsonProperty("creation_time")
    private long creationTime;
    
    @JsonProperty("image_path")
    private String imagePath;
    
    // Constructors
    public ClothIdentity() {
        this.creationTime = System.currentTimeMillis();
    }
    
    public ClothIdentity(String clothId) {
        this();
        this.clothId = clothId;
    }
    
    // Getters and Setters
    public String getClothId() { return clothId; }
    public void setClothId(String clothId) { this.clothId = clothId; }
    
    public String getFeaturesHash() { return featuresHash; }
    public void setFeaturesHash(String featuresHash) { this.featuresHash = featuresHash; }
    
    public String getTimestampHash() { return timestampHash; }
    public void setTimestampHash(String timestampHash) { this.timestampHash = timestampHash; }
    
    public String getCombinedHash() { return combinedHash; }
    public void setCombinedHash(String combinedHash) { this.combinedHash = combinedHash; }
    
    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }
    
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}