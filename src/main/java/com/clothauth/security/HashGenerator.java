package com.clothauth.security;

import com.clothauth.model.ClothFeatures;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HashGenerator {
    private static final Logger logger = LoggerFactory.getLogger(HashGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    
    public static String generateHash(Object data, String algorithm) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            return generateHash(jsonData, algorithm);
        } catch (Exception e) {
            logger.error("Error generating hash from object: {}", e.getMessage());
            throw new RuntimeException("Hash generation failed", e);
        }
    }
    
    public static String generateHash(String data, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Hash algorithm not available: {}", algorithm);
            throw new RuntimeException("Hash algorithm not available: " + algorithm, e);
        }
    }
    
    public static String generateSHA256Hash(Object data) {
        return generateHash(data, "SHA-256");
    }
    
    public static String generateSHA256Hash(String data) {
        return generateHash(data, "SHA-256");
    }
    
    public static String generateFeaturesOnlyHash(ClothFeatures features) {
        try {
            // Create a normalized copy of the features
            ClothFeatures normalizedFeatures = normalizeFeatures(features);
            return generateSHA256Hash(normalizedFeatures);
        } catch (Exception e) {
            logger.error("Error generating features-only hash: {}", e.getMessage());
            throw new RuntimeException("Features hash generation failed", e);
        }
    }
    
    public static String generateCombinedHash(String featuresHash, String timestampHash) {
        String combined = featuresHash + ":" + timestampHash;
        return generateSHA256Hash(combined);
    }
    
    private static ClothFeatures normalizeFeatures(ClothFeatures features) {
        ClothFeatures normalized = new ClothFeatures();
        
        // Normalize fabric texture (TreeMap for consistent ordering)
        if (features.getFabricTexture() != null) {
            Map<String, Double> texture = new TreeMap<>();
            features.getFabricTexture().forEach((k, v) -> 
                texture.put(k, roundToDecimal(v, 6)));
            normalized.setFabricTexture(texture);
        }
        
        // Normalize color histogram
        if (features.getColorHistogram() != null) {
            List<Double> histogram = new ArrayList<>();
            features.getColorHistogram().forEach(v -> 
                histogram.add(roundToDecimal(v, 6)));
            normalized.setColorHistogram(histogram);
        }
        
        // Normalize dimensions (TreeMap for consistent ordering)
        if (features.getDimensions() != null) {
            Map<String, Double> dimensions = new TreeMap<>();
            features.getDimensions().forEach((k, v) -> 
                dimensions.put(k, roundToDecimal(v, 6)));
            normalized.setDimensions(dimensions);
        }
        
        // Normalize edge features
        if (features.getEdgeFeatures() != null) {
            List<Double> edges = new ArrayList<>();
            features.getEdgeFeatures().forEach(v -> 
                edges.add(roundToDecimal(v, 6)));
            normalized.setEdgeFeatures(edges);
        }
        
        // Normalize pattern features (LinkedHashMap for consistent ordering)
        if (features.getPatternFeatures() != null) {
            Map<String, Object> patterns = new LinkedHashMap<>();
            Map<String, Object> original = features.getPatternFeatures();
            
            // Process in consistent order
            if (original.containsKey("complexity_score")) {
                Object score = original.get("complexity_score");
                if (score instanceof Double) {
                    patterns.put("complexity_score", roundToDecimal((Double) score, 6));
                } else {
                    patterns.put("complexity_score", score);
                }
            }
            
            normalized.setPatternFeatures(patterns);
        }
        
        return normalized;
    }
    
    private static double roundToDecimal(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}