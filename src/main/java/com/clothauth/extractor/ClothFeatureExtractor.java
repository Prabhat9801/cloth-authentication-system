package com.clothauth.extractor;

import com.clothauth.model.ClothFeatures;
import com.clothauth.utils.ImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ClothFeatureExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ClothFeatureExtractor.class);
    
    // Fixed parameters for deterministic feature extraction
    private static final int CANNY_LOW_THRESHOLD = 50;
    private static final int CANNY_HIGH_THRESHOLD = 150;
    private static final int HISTOGRAM_BINS = 256;
    private static final int LBP_RADIUS = 1;
    private static final int LBP_NEIGHBORS = 8;
    private static final int DECIMAL_PRECISION = 4; // Reduced precision for consistency
    
    public ClothFeatures extractFeatures(String imagePath) {
        logger.info("Starting feature extraction for image: {}", imagePath);
        
        ClothFeatures features = new ClothFeatures();
        
        try {
            // Extract and normalize all features with fixed parameters
            Map<String, Double> textureFeatures = ImageProcessor.extractTextureFeatures(
                imagePath, 
                CANNY_LOW_THRESHOLD,
                CANNY_HIGH_THRESHOLD,
                LBP_RADIUS,
                LBP_NEIGHBORS
            );
            features.setFabricTexture(normalizeFeatureMap(textureFeatures));
            
            List<Double> colorHistogram = ImageProcessor.extractColorHistogram(
                imagePath,
                HISTOGRAM_BINS
            );
            features.setColorHistogram(normalizeFeatureList(colorHistogram));
            
            Map<String, Double> dimensions = ImageProcessor.extractDimensions(imagePath);
            features.setDimensions(normalizeFeatureMap(dimensions));
            
            List<Double> edgeFeatures = ImageProcessor.extractEdgeFeatures(
                imagePath,
                CANNY_LOW_THRESHOLD,
                CANNY_HIGH_THRESHOLD
            );
            features.setEdgeFeatures(normalizeFeatureList(edgeFeatures));
            
            // Create deterministic pattern features
            Map<String, Object> patternFeatures = new LinkedHashMap<>();
            patternFeatures.put("complexity_score", round(calculateComplexityScore(features)));
            patternFeatures.put("symmetry_score", round(calculateSymmetryScore(imagePath)));
            features.setPatternFeatures(patternFeatures);
            
            logger.info("Feature extraction completed successfully for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error during feature extraction: {}", e.getMessage(), e);
            throw new RuntimeException("Feature extraction failed", e);
        }
        
        return features;
    }
    
    private Map<String, Double> normalizeFeatureMap(Map<String, Double> map) {
        if (map == null) return new TreeMap<>();
        
        return map.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> round(e.getValue()),
                (v1, v2) -> v1,
                TreeMap::new
            ));
    }
    
    private List<Double> normalizeFeatureList(List<Double> list) {
        if (list == null) return new ArrayList<>();
        
        return list.stream()
            .map(this::round)
            .collect(Collectors.toList());
    }
    
    private double calculateComplexityScore(ClothFeatures features) {
        Double stdDev = features.getFabricTexture().get("std_deviation");
        Double contrast = features.getFabricTexture().get("contrast");
        
        if (stdDev != null && contrast != null) {
            return (stdDev + contrast) / 2.0;
        }
        return 0.0;
    }
    
    private double calculateSymmetryScore(String imagePath) {
        try {
            Map<String, Double> dimensions = ImageProcessor.extractDimensions(imagePath);
            Double width = dimensions.get("width");
            Double height = dimensions.get("height");
            
            if (width != null && height != null) {
                double aspectRatio = width / height;
                return Math.min(Math.abs(1.0 - aspectRatio) * 100, 100.0);
            }
            return 50.0;
        } catch (Exception e) {
            logger.warn("Error calculating symmetry score: {}", e.getMessage());
            return 50.0;
        }
    }
    
    private double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        double scale = Math.pow(10, DECIMAL_PRECISION);
        return Math.round(value * scale) / scale;
    }
}