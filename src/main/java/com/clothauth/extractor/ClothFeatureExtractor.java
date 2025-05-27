package com.clothauth.extractor;

import com.clothauth.model.ClothFeatures;
import com.clothauth.utils.ImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

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
    
    private double calculateSymmetryScore(String imagePath) {
        try {
            Mat image = Imgcodecs.imread(imagePath);
            Mat grayImage = new Mat();
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            
            int width = grayImage.cols();
            int height = grayImage.rows();
            int midX = width / 2;
            
            // Compare left and right halves of the image
            double symmetryScore = 0;
            int pixelsCompared = 0;
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < midX; x++) {
                    double leftPixel = grayImage.get(y, x)[0];
                    double rightPixel = grayImage.get(y, width - 1 - x)[0];
                    
                    // Calculate pixel similarity (0 to 1)
                    double pixelSymmetry = 1.0 - Math.abs(leftPixel - rightPixel) / 255.0;
                    symmetryScore += pixelSymmetry;
                    pixelsCompared++;
                }
            }
            
            // Return normalized symmetry score (0 to 100)
            return (symmetryScore / pixelsCompared) * 100;
            
        } catch (Exception e) {
            logger.error("Error calculating symmetry score: {}", e.getMessage());
            return 0.0;
        }
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
}