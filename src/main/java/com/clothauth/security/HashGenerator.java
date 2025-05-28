package com.clothauth.security;

import com.clothauth.model.ClothFeatures;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class HashGenerator {
    private static final Logger logger = LoggerFactory.getLogger(HashGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .disable(SerializationFeature.INDENT_OUTPUT); // Disable indentation for consistent whitespace

    private static final int DECIMAL_PRECISION = 4;
    private static final List<String> PATTERN_FEATURE_ORDER = Arrays.asList(
        "complexity_score",
        "symmetry_score"
    ); // Include all pattern features in a fixed order

    public static String generateHash(Object data, String algorithm) {
        try {
            Object normalized = normalizeDataStructure(data);
            String jsonData = objectMapper.writeValueAsString(normalized);
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
            // Create a copy with normalized collections
            Map<String, Object> normalizedFeatures = new TreeMap<>();
            
            // Process all features in fixed order
            normalizedFeatures.put("fabric_texture", normalizeMap(features.getFabricTexture()));
            normalizedFeatures.put("color_histogram", normalizeList(features.getColorHistogram()));
            normalizedFeatures.put("dimensions", normalizeMap(features.getDimensions()));
            normalizedFeatures.put("edge_features", normalizeList(features.getEdgeFeatures()));
            normalizedFeatures.put("pattern_features", normalizePatternFeatures(features.getPatternFeatures()));

            String json = objectMapper.writeValueAsString(normalizedFeatures);
            return generateSHA256Hash(json);
            
        } catch (Exception e) {
            logger.error("Error generating features-only hash: {}", e.getMessage());
            throw new RuntimeException("Features hash generation failed", e);
        }
    }

    private static Object normalizeDataStructure(Object data) {
        if (data instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) data;
            if (!map.isEmpty() && map.keySet().iterator().next() instanceof String) {
                if (map.values().iterator().next() instanceof Double) {
                    @SuppressWarnings("unchecked")
                    Map<String, Double> typedMap = (Map<String, Double>) map;
                    return normalizeMap(typedMap);
                }
            }
            return map;
        } else if (data instanceof List) {
            List<?> list = (List<?>) data;
            if (!list.isEmpty() && list.get(0) instanceof Double) {
                @SuppressWarnings("unchecked")
                List<Double> typedList = (List<Double>) list;
                return normalizeList(typedList);
            }
            return list;
        } else if (data instanceof Double) {
            return roundDecimal((Double) data);
        }
        return data;
    }

    private static Map<String, Double> normalizeMap(Map<String, Double> map) {
        if (map == null) return new TreeMap<>();
        
        return map.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> roundDecimal(e.getValue()),
                (v1, v2) -> v1,
                TreeMap::new
            ));
    }

    private static List<Double> normalizeList(List<Double> list) {
        if (list == null) return new ArrayList<>();
        
        return list.stream()
            .map(HashGenerator::roundDecimal)
            .collect(Collectors.toList());
    }

    private static Map<String, Object> normalizePatternFeatures(Map<String, Object> patternFeatures) {
        if (patternFeatures == null) return new TreeMap<>();
        
        Map<String, Object> normalized = new TreeMap<>();
        
        // Process in predefined order
        PATTERN_FEATURE_ORDER.forEach(key -> {
            Object value = patternFeatures.getOrDefault(key, 0.0);
            if (value instanceof Double) {
                normalized.put(key, roundDecimal((Double) value));
            } else {
                normalized.put(key, value);
            }
        });
        
        return normalized;
    }

    private static double roundDecimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        double scale = Math.pow(10, DECIMAL_PRECISION);
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