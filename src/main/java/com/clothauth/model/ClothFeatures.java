package com.clothauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ClothFeatures {
    @JsonProperty("fabric_texture")
    private Map<String, Double> fabricTexture;
    
    @JsonProperty("color_histogram")
    private List<Double> colorHistogram;
    
    @JsonProperty("dimensions")
    private Map<String, Double> dimensions;
    
    @JsonProperty("pattern_features")
    private Map<String, Object> patternFeatures;
    
    @JsonProperty("edge_features")
    private List<Double> edgeFeatures;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    // Constructors
    public ClothFeatures() {
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public Map<String, Double> getFabricTexture() { return fabricTexture; }
    public void setFabricTexture(Map<String, Double> fabricTexture) { this.fabricTexture = fabricTexture; }
    
    public List<Double> getColorHistogram() { return colorHistogram; }
    public void setColorHistogram(List<Double> colorHistogram) { this.colorHistogram = colorHistogram; }
    
    public Map<String, Double> getDimensions() { return dimensions; }
    public void setDimensions(Map<String, Double> dimensions) { this.dimensions = dimensions; }
    
    public Map<String, Object> getPatternFeatures() { return patternFeatures; }
    public void setPatternFeatures(Map<String, Object> patternFeatures) { this.patternFeatures = patternFeatures; }
    
    public List<Double> getEdgeFeatures() { return edgeFeatures; }
    public void setEdgeFeatures(List<Double> edgeFeatures) { this.edgeFeatures = edgeFeatures; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}