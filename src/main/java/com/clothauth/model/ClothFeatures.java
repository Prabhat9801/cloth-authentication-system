package com.clothauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;

public class ClothFeatures {
    @JsonProperty("fabric_texture")
    private final Map<String, Double> fabricTexture;
    
    @JsonProperty("color_histogram")
    private final List<Double> colorHistogram;
    
    @JsonProperty("dimensions")
    private final Map<String, Double> dimensions;
    
    @JsonProperty("pattern_features")
    private final Map<String, Object> patternFeatures;
    
    @JsonProperty("edge_features")
    private final List<Double> edgeFeatures;
    
    @JsonIgnore // Exclude from hash calculation
    private long timestamp; // Removed final to allow modification
    
    // Constructor
    public ClothFeatures() {
        this.timestamp = System.currentTimeMillis();
        this.fabricTexture = new TreeMap<>();
        this.colorHistogram = new ArrayList<>();
        this.dimensions = new TreeMap<>();
        this.patternFeatures = new TreeMap<>();
        this.edgeFeatures = new ArrayList<>();
    }
    
    // Getters and Setters
    public Map<String, Double> getFabricTexture() {
        return fabricTexture;
    }
    
    public void setFabricTexture(Map<String, Double> fabricTexture) {
        this.fabricTexture.clear();
        if (fabricTexture != null) {
            TreeMap<String, Double> sorted = new TreeMap<>(fabricTexture);
            this.fabricTexture.putAll(sorted);
        }
    }
    
    public List<Double> getColorHistogram() {
        return colorHistogram;
    }
    
    public void setColorHistogram(List<Double> colorHistogram) {
        this.colorHistogram.clear();
        if (colorHistogram != null) {
            this.colorHistogram.addAll(colorHistogram);
        }
    }
    
    public Map<String, Double> getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(Map<String, Double> dimensions) {
        this.dimensions.clear();
        if (dimensions != null) {
            TreeMap<String, Double> sorted = new TreeMap<>(dimensions);
            this.dimensions.putAll(sorted);
        }
    }
    
    public Map<String, Object> getPatternFeatures() {
        return patternFeatures;
    }
    
    public void setPatternFeatures(Map<String, Object> patternFeatures) {
        this.patternFeatures.clear();
        if (patternFeatures != null) {
            TreeMap<String, Object> sorted = new TreeMap<>(patternFeatures);
            this.patternFeatures.putAll(sorted);
        }
    }
    
    public List<Double> getEdgeFeatures() {
        return edgeFeatures;
    }
    
    public void setEdgeFeatures(List<Double> edgeFeatures) {
        this.edgeFeatures.clear();
        if (edgeFeatures != null) {
            this.edgeFeatures.addAll(edgeFeatures);
        }
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}