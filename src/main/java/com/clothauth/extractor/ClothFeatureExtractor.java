package com.clothauth.extractor;

import com.clothauth.model.ClothFeatures;
import com.clothauth.utils.ImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ClothFeatureExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ClothFeatureExtractor.class);
    
    public ClothFeatures extractFeatures(String imagePath) {
        logger.info("Starting feature extraction for image: {}", imagePath);
        
        ClothFeatures features = new ClothFeatures();
        
        try {
            // Extract fabric texture features
            Map<String, Double> textureFeatures = ImageProcessor.extractTextureFeatures(imagePath);
            features.setFabricTexture(textureFeatures);
            
            // Extract color histogram
            features.setColorHistogram(ImageProcessor.extractColorHistogram(imagePath));
            
            // Extract dimensions
            features.setDimensions(ImageProcessor.extractDimensions(imagePath));
            
            // Extract edge features
            features.setEdgeFeatures(ImageProcessor.extractEdgeFeatures(imagePath));
            
            // Extract pattern features (simplified)
            Map<String, Object> patternFeatures = new HashMap<>();
            patternFeatures.put("complexity_score", calculateComplexityScore(features));
            patternFeatures.put("symmetry_score", calculateSymmetryScore(imagePath));
            features.setPatternFeatures(patternFeatures);
            
            logger.info("Feature extraction completed successfully for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error during feature extraction: {}", e.getMessage(), e);
            throw new RuntimeException("Feature extraction failed", e);
        }
        
        return features;
    }
    
    private double calculateComplexityScore(ClothFeatures features) {
        // Simple complexity calculation based on texture variance
        Double stdDev = features.getFabricTexture().get("std_deviation");
        Double contrast = features.getFabricTexture().get("contrast");
        
        if (stdDev != null && contrast != null) {
            return (stdDev + contrast) / 2.0;
        }
        return 0.0;
    }
    
    private double calculateSymmetryScore(String imagePath) {
        // Deterministic symmetry calculation instead of random
        try {
            // Simple deterministic approach: use image dimensions and basic properties
            Map<String, Double> dimensions = ImageProcessor.extractDimensions(imagePath);
            Double width = dimensions.get("width");
            Double height = dimensions.get("height");
            
            if (width != null && height != null) {
                // Calculate symmetry based on aspect ratio and dimension relationships
                double aspectRatio = width / height;
                double symmetryScore = Math.abs(1.0 - aspectRatio) * 100;
                return Math.min(symmetryScore, 100.0); // Cap at 100
            }
            
            return 50.0; // Default neutral symmetry score
            
        } catch (Exception e) {
            logger.warn("Error calculating symmetry score: {}", e.getMessage());
            return 50.0; // Default value
        }
    }
}