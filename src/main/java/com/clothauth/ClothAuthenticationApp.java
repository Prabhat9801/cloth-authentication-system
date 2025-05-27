package com.clothauth;

import com.clothauth.extractor.ClothFeatureExtractor;
import com.clothauth.model.ClothFeatures;
import com.clothauth.model.ClothIdentity;
import com.clothauth.security.HashGenerator;
import com.clothauth.storage.LocalStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class ClothAuthenticationApp {
    private static final Logger logger = LoggerFactory.getLogger(ClothAuthenticationApp.class);
    
    private final ClothFeatureExtractor featureExtractor;
    private final LocalStorageManager storageManager;
    private final Scanner scanner;
    
    public ClothAuthenticationApp() {
        this.featureExtractor = new ClothFeatureExtractor();
        this.storageManager = new LocalStorageManager();
        this.scanner = new Scanner(System.in);
    }
    
    public static void main(String[] args) {
        logger.info("Starting Cloth Authentication System");
        
        ClothAuthenticationApp app = new ClothAuthenticationApp();
        app.run();
    }
    
    public void run() {
        boolean running = true;
        
        while (running) {
            displayMenu();
            int choice = getMenuChoice();
            
            switch (choice) {
                case 1:
                    processNewCloth();
                    break;
                case 2:
                    verifyCloth();
                    break;
                case 3:
                    listStoredCloths();
                    break;
                case 4:
                    deleteClothData();
                    break;
                case 5:
                    running = false;
                    logger.info("Shutting down Cloth Authentication System");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
        
        scanner.close();
    }
    
    private void displayMenu() {
        System.out.println("\n=== Cloth Authentication System ===");
        System.out.println("1. Process New Cloth Item");
        System.out.println("2. Verify Existing Cloth");
        System.out.println("3. List All Stored Cloths");
        System.out.println("4. Delete Cloth Data");
        System.out.println("5. Exit");
        System.out.print("Enter your choice: ");
    }
    
    private int getMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private void processNewCloth() {
        System.out.print("Enter path to cloth image: ");
        String imagePath = scanner.nextLine().trim();
        
        // Validate image file exists
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.out.println("Error: Image file not found at path: " + imagePath);
            return;
        }
        
        try {
            // Generate unique cloth ID
            String clothId = generateClothId();
            System.out.println("Generated Cloth ID: " + clothId);
            
            // Step 1: Extract deep cloth features
            System.out.println("Step 1: Extracting cloth features...");
            ClothFeatures features = featureExtractor.extractFeatures(imagePath);
            
            // Step 2: Store features locally
            System.out.println("Step 2: Storing features locally...");
            storageManager.storeClothFeatures(clothId, features);
            
            // Step 3: Generate hashes
            System.out.println("Step 3: Generating hashes...");
            ClothIdentity identity = generateClothIdentity(clothId, features, imagePath);
            
            // Store identity
            storageManager.storeClothIdentity(clothId, identity);
            
            // Display results
            displayProcessingResults(clothId, identity);
            
        } catch (Exception e) {
            logger.error("Error processing cloth: {}", e.getMessage(), e);
            System.out.println("Error processing cloth: " + e.getMessage());
        }
    }
      private void verifyCloth() {
        System.out.print("Enter Cloth ID to verify: ");
        String clothId = scanner.nextLine().trim();
        
        ClothIdentity storedIdentity = storageManager.loadClothIdentity(clothId);
        if (storedIdentity == null) {
            System.out.println("Cloth ID not found: " + clothId);
            return;
        }
        
        System.out.print("Enter path to cloth image for verification: ");
        String imagePath = scanner.nextLine().trim();
        
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.out.println("Error: Image file not found at path: " + imagePath);
            return;
        }
        
        try {
            // Extract features from verification image
            System.out.println("Extracting features from verification image...");
            ClothFeatures verificationFeatures = featureExtractor.extractFeatures(imagePath);
            verificationFeatures.setTimestamp(0); // Reset timestamp for consistent hashing
              // Get the features for comparison
            ClothFeatures storedFeatures = storageManager.loadClothFeatures(clothId);
            
            // Compare features with tolerance
            double textureSimilarity = compareTextureFeatures(storedFeatures, verificationFeatures);
            double patternSimilarity = comparePatternFeatures(storedFeatures, verificationFeatures);
            double dimensionSimilarity = compareDimensions(storedFeatures, verificationFeatures);
            
            // Weights for different feature types
            double textureWeight = 0.4;
            double patternWeight = 0.4;
            double dimensionWeight = 0.2;
            
            // Calculate weighted similarity score
            double totalSimilarity = (textureSimilarity * textureWeight) +
                                   (patternSimilarity * patternWeight) +
                                   (dimensionSimilarity * dimensionWeight);
            
            // Threshold for authentication (80% similarity required)
            double threshold = 0.80;
            boolean isAuthentic = totalSimilarity >= threshold;
            
            System.out.println("\n=== Verification Results ===");
            System.out.println("Cloth ID: " + clothId);
            
            // Print similarity scores
            System.out.println("\nSimilarity Scores:");
            System.out.printf("Texture Similarity: %.2f%%\n", textureSimilarity * 100);
            System.out.printf("Pattern Similarity: %.2f%%\n", patternSimilarity * 100);
            System.out.printf("Dimension Similarity: %.2f%%\n", dimensionSimilarity * 100);
            System.out.printf("Total Weighted Similarity: %.2f%%\n\n", totalSimilarity * 100);
            
            System.out.println("Authentication Status: " + (isAuthentic ? "AUTHENTIC" : "NOT AUTHENTIC"));
            
            if (!isAuthentic) {
                System.out.println("Warning: The cloth item may have been altered or is not the original item.");
            }
            
        } catch (Exception e) {
            logger.error("Error verifying cloth: {}", e.getMessage(), e);
            System.out.println("Error verifying cloth: " + e.getMessage());
        }
    }
    
    private void listStoredCloths() {
        List<String> clothIds = storageManager.getAllClothIds();
        
        if (clothIds.isEmpty()) {
            System.out.println("No cloth items stored in the system.");
            return;
        }
        
        System.out.println("\n=== Stored Cloth Items ===");
        for (int i = 0; i < clothIds.size(); i++) {
            String clothId = clothIds.get(i);
            ClothIdentity identity = storageManager.loadClothIdentity(clothId);
            
            System.out.printf("%d. Cloth ID: %s\n", i + 1, clothId);
            if (identity != null) {
                System.out.printf("   Created: %s\n", new java.util.Date(identity.getCreationTime()));
                System.out.printf("   Hash: %s\n", identity.getCombinedHash());
                if (identity.getImagePath() != null) {
                    System.out.printf("   Image: %s\n", identity.getImagePath());
                }
            }
            System.out.println();
        }
    }
    
    private void deleteClothData() {
        System.out.print("Enter Cloth ID to delete: ");
        String clothId = scanner.nextLine().trim();
        
        ClothIdentity identity = storageManager.loadClothIdentity(clothId);
        if (identity == null) {
            System.out.println("Cloth ID not found: " + clothId);
            return;
        }
        
        System.out.print("Are you sure you want to delete all data for cloth ID '" + clothId + "'? (y/N): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if ("y".equals(confirmation) || "yes".equals(confirmation)) {
            boolean success = storageManager.deleteClothData(clothId);
            if (success) {
                System.out.println("Cloth data deleted successfully.");
            } else {
                System.out.println("Error deleting cloth data.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }
    
    private String generateClothId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
      private ClothIdentity generateClothIdentity(String clothId, ClothFeatures features, String imagePath) {
        ClothIdentity identity = new ClothIdentity(clothId);
        identity.setImagePath(imagePath);
        
        // Generate features hash without timestamp to ensure consistency
        features.setTimestamp(0); // Reset timestamp to ensure consistent hashing
        String featuresHash = HashGenerator.generateSHA256Hash(features);
        identity.setFeaturesHash(featuresHash);
        
        // Generate timestamp hash
        long timestamp = System.currentTimeMillis();
        String timestampHash = HashGenerator.generateSHA256Hash(String.valueOf(timestamp));
        identity.setTimestampHash(timestampHash);
        features.setTimestamp(timestamp); // Set the actual timestamp after hashing
        
        // Generate combined hash - using only features hash to ensure consistency
        identity.setCombinedHash(featuresHash); // Use features hash as the combined hash
        
        return identity;
    }
    
    private boolean verifyClothAuthenticity(ClothIdentity stored, ClothIdentity current) {
        // For basic verification, we compare the features hash
        // In a real-world scenario, you might allow for small variations due to lighting, angle, etc.
        return stored.getFeaturesHash().equals(current.getFeaturesHash());
    }
      private void displayProcessingResults(String clothId, ClothIdentity identity) {
        System.out.println("\n=== Processing Results ===");
        System.out.println("Cloth ID: " + clothId);
        System.out.println("Features Hash: " + identity.getFeaturesHash());
        System.out.println("Timestamp Hash: " + identity.getTimestampHash());
        System.out.println("Combined Hash: " + identity.getCombinedHash());
        System.out.println("Created: " + new java.util.Date(identity.getCreationTime()));
        System.out.println("Image Path: " + identity.getImagePath());
        System.out.println("\nCloth digital identity created successfully!");
        System.out.println("Data stored locally in: data/features/ and data/identities/");
    }
    
    private double compareTextureFeatures(ClothFeatures stored, ClothFeatures current) {
        Map<String, Double> storedTexture = stored.getFabricTexture();
        Map<String, Double> currentTexture = current.getFabricTexture();
        
        double meanDiff = Math.abs(storedTexture.get("mean_intensity") - 
                                 currentTexture.get("mean_intensity")) / 255.0;
        double contrastDiff = Math.abs(storedTexture.get("contrast") - 
                                     currentTexture.get("contrast"));
        double homogeneityDiff = Math.abs(storedTexture.get("homogeneity") - 
                                        currentTexture.get("homogeneity"));

        return 1.0 - ((meanDiff + contrastDiff + homogeneityDiff) / 3.0);
    }

    private double comparePatternFeatures(ClothFeatures stored, ClothFeatures current) {
        Map<String, Object> storedPattern = stored.getPatternFeatures();
        Map<String, Object> currentPattern = current.getPatternFeatures();
        
        double complexityDiff = Math.abs(((Number)storedPattern.get("complexity_score")).doubleValue() - 
                                       ((Number)currentPattern.get("complexity_score")).doubleValue()) / 100.0;
        double symmetryDiff = Math.abs(((Number)storedPattern.get("symmetry_score")).doubleValue() - 
                                     ((Number)currentPattern.get("symmetry_score")).doubleValue()) / 100.0;

        return 1.0 - ((complexityDiff + symmetryDiff) / 2.0);
    }

    private double compareDimensions(ClothFeatures stored, ClothFeatures current) {
        Map<String, Double> storedDim = stored.getDimensions();
        Map<String, Double> currentDim = current.getDimensions();
        
        double aspectRatioDiff = Math.abs(storedDim.get("aspect_ratio") - 
                                        currentDim.get("aspect_ratio"));
        double areaDiff = Math.abs(storedDim.get("area") - 
                                 currentDim.get("area")) / storedDim.get("area");

        return 1.0 - ((aspectRatioDiff + areaDiff) / 2.0);
    }
  }