package com.clothauth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.clothauth.model.ClothFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public class HashGenerator {
    private static final Logger logger = LoggerFactory.getLogger(HashGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private static final int PRECISION = 6; // Number of decimal places to keep
    
    public static String generateHash(Object data, String algorithm) {
        try {
            if (data instanceof ClothFeatures) {
                return generateNormalizedHash((ClothFeatures) data, algorithm);
            }
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
    
    public static String generateMD5Hash(String data) {
        return generateHash(data, "MD5");
    }
    
    public static String generateCombinedHash(String featuresHash, String timestampHash) {
        String combined = featuresHash + ":" + timestampHash;
        return generateSHA256Hash(combined);
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static String generateNormalizedHash(ClothFeatures features, String algorithm) {
        try {
            ObjectNode normalizedFeatures = objectMapper.createObjectNode();
            
            // Normalize fabric texture
            ObjectNode textureNode = objectMapper.createObjectNode();
            Map<String, Double> texture = features.getFabricTexture();
            if (texture != null) {
                textureNode.put("mean_intensity", normalizeDouble(texture.get("mean_intensity")));
                textureNode.put("contrast", normalizeDouble(texture.get("contrast")));
                textureNode.put("std_deviation", normalizeDouble(texture.get("std_deviation")));
                textureNode.put("homogeneity", normalizeDouble(texture.get("homogeneity")));
            }
            normalizedFeatures.set("fabric_texture", textureNode);
            
            // Normalize color histogram
            ArrayNode histogramNode = objectMapper.createArrayNode();
            List<Double> histogram = features.getColorHistogram();
            if (histogram != null) {
                for (Double value : histogram) {
                    histogramNode.add(normalizeDouble(value));
                }
            }
            normalizedFeatures.set("color_histogram", histogramNode);
            
            // Normalize dimensions
            ObjectNode dimensionsNode = objectMapper.createObjectNode();
            Map<String, Double> dimensions = features.getDimensions();
            if (dimensions != null) {
                dimensionsNode.put("width", normalizeDouble(dimensions.get("width")));
                dimensionsNode.put("height", normalizeDouble(dimensions.get("height")));
                dimensionsNode.put("aspect_ratio", normalizeDouble(dimensions.get("aspect_ratio")));
                dimensionsNode.put("area", normalizeDouble(dimensions.get("area")));
            }
            normalizedFeatures.set("dimensions", dimensionsNode);
            
            // Normalize edge features
            ArrayNode edgeNode = objectMapper.createArrayNode();
            List<Double> edges = features.getEdgeFeatures();
            if (edges != null) {
                for (Double value : edges) {
                    edgeNode.add(normalizeDouble(value));
                }
            }
            normalizedFeatures.set("edge_features", edgeNode);
            
            // Normalize pattern features
            ObjectNode patternNode = objectMapper.createObjectNode();
            Map<String, Object> pattern = features.getPatternFeatures();
            if (pattern != null) {
                patternNode.put("complexity_score", normalizeDouble(((Number)pattern.get("complexity_score")).doubleValue()));
                patternNode.put("symmetry_score", normalizeDouble(((Number)pattern.get("symmetry_score")).doubleValue()));
            }
            normalizedFeatures.set("pattern_features", patternNode);
            
            // Sort all keys to ensure consistent ordering
            String jsonData = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(normalizedFeatures);
            return generateHash(jsonData, algorithm);
            
        } catch (Exception e) {
            logger.error("Error generating normalized hash: {}", e.getMessage());
            throw new RuntimeException("Normalized hash generation failed", e);
        }
    }
    
    private static double normalizeDouble(double value) {
        if (Double.isNaN(value)) return 0.0;
        if (Double.isInfinite(value)) return value > 0 ? 1.0 : 0.0;
        return BigDecimal.valueOf(value)
                .setScale(PRECISION, RoundingMode.HALF_UP)
                .doubleValue();
    }
}