package com.clothauth.storage;

import com.clothauth.model.ClothFeatures;
import com.clothauth.model.ClothIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LocalStorageManager {
    private static final Logger logger = LoggerFactory.getLogger(LocalStorageManager.class);
    private static final String FEATURES_DIR = "data/features";
    private static final String IDENTITIES_DIR = "data/identities";
    
    private final ObjectMapper objectMapper;
    
    public LocalStorageManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        initializeDirectories();
    }
    
    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(FEATURES_DIR));
            Files.createDirectories(Paths.get(IDENTITIES_DIR));
            logger.info("Storage directories initialized");
        } catch (IOException e) {
            logger.error("Failed to initialize storage directories: {}", e.getMessage());
            throw new RuntimeException("Storage initialization failed", e);
        }
    }
    
    public void storeClothFeatures(String clothId, ClothFeatures features) {
        try {
            String fileName = clothId + "_features.json";
            Path filePath = Paths.get(FEATURES_DIR, fileName);
            
            objectMapper.writeValue(filePath.toFile(), features);
            logger.info("Stored cloth features for ID: {} at {}", clothId, filePath);
            
        } catch (IOException e) {
            logger.error("Failed to store cloth features for ID {}: {}", clothId, e.getMessage());
            throw new RuntimeException("Failed to store cloth features", e);
        }
    }
    
    public void storeClothIdentity(String clothId, ClothIdentity identity) {
        try {
            String fileName = clothId + "_identity.json";
            Path filePath = Paths.get(IDENTITIES_DIR, fileName);
            
            objectMapper.writeValue(filePath.toFile(), identity);
            logger.info("Stored cloth identity for ID: {} at {}", clothId, filePath);
            
        } catch (IOException e) {
            logger.error("Failed to store cloth identity for ID {}: {}", clothId, e.getMessage());
            throw new RuntimeException("Failed to store cloth identity", e);
        }
    }
    
    public ClothFeatures loadClothFeatures(String clothId) {
        try {
            String fileName = clothId + "_features.json";
            Path filePath = Paths.get(FEATURES_DIR, fileName);
            
            if (!Files.exists(filePath)) {
                logger.warn("Features file not found for cloth ID: {}", clothId);
                return null;
            }
            
            ClothFeatures features = objectMapper.readValue(filePath.toFile(), ClothFeatures.class);
            logger.info("Loaded cloth features for ID: {}", clothId);
            return features;
            
        } catch (IOException e) {
            logger.error("Failed to load cloth features for ID {}: {}", clothId, e.getMessage());
            return null;
        }
    }
    
    public ClothIdentity loadClothIdentity(String clothId) {
        try {
            String fileName = clothId + "_identity.json";
            Path filePath = Paths.get(IDENTITIES_DIR, fileName);
            
            if (!Files.exists(filePath)) {
                logger.warn("Identity file not found for cloth ID: {}", clothId);
                return null;
            }
            
            ClothIdentity identity = objectMapper.readValue(filePath.toFile(), ClothIdentity.class);
            logger.info("Loaded cloth identity for ID: {}", clothId);
            return identity;
            
        } catch (IOException e) {
            logger.error("Failed to load cloth identity for ID {}: {}", clothId, e.getMessage());
            return null;
        }
    }
    
    public List<String> getAllClothIds() {
        List<String> clothIds = new ArrayList<>();
        
        try {
            File featuresDir = new File(FEATURES_DIR);
            if (featuresDir.exists() && featuresDir.isDirectory()) {
                File[] files = featuresDir.listFiles((dir, name) -> name.endsWith("_features.json"));
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        String clothId = fileName.substring(0, fileName.lastIndexOf("_features.json"));
                        clothIds.add(clothId);
                    }
                }
            }
            
            logger.info("Found {} cloth IDs in storage", clothIds.size());
            
        } catch (Exception e) {
            logger.error("Failed to get cloth IDs: {}", e.getMessage());
        }
        
        return clothIds;
    }
    
    public boolean deleteClothData(String clothId) {
        boolean success = true;
        
        try {
            // Delete features file
            Path featuresPath = Paths.get(FEATURES_DIR, clothId + "_features.json");
            if (Files.exists(featuresPath)) {
                Files.delete(featuresPath);
                logger.info("Deleted features file for cloth ID: {}", clothId);
            }
            
            // Delete identity file
            Path identityPath = Paths.get(IDENTITIES_DIR, clothId + "_identity.json");
            if (Files.exists(identityPath)) {
                Files.delete(identityPath);
                logger.info("Deleted identity file for cloth ID: {}", clothId);
            }
            
        } catch (IOException e) {
            logger.error("Failed to delete cloth data for ID {}: {}", clothId, e.getMessage());
            success = false;
        }
        
        return success;
    }
}