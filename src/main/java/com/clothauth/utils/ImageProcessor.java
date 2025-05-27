package com.clothauth.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ImageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);
    private static final int BINS = 256;
    private static final float[] RANGE = {0, 256};
    
    static {
        // Load OpenCV native library
        nu.pattern.OpenCV.loadLocally();
    }
    
    public static Map<String, Double> extractTextureFeatures(String imagePath) {
        Map<String, Double> textureFeatures = new HashMap<>();
        
        try {
            Mat image = Imgcodecs.imread(imagePath);
            if (image.empty()) {
                throw new RuntimeException("Could not load image: " + imagePath);
            }
            
            // Convert to grayscale and normalize
            Mat grayImage = new Mat();
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            Core.normalize(grayImage, grayImage, 0, 255, Core.NORM_MINMAX);
            
            // Apply Gaussian blur for noise reduction
            Mat blurredImage = new Mat();
            Imgproc.GaussianBlur(grayImage, blurredImage, new Size(3, 3), 0);
            
            // Calculate LBP
            Mat lbp = new Mat();
            calculateLBP(blurredImage, lbp);
            
            // Normalize LBP
            Core.normalize(lbp, lbp, 0, 255, Core.NORM_MINMAX);
            
            // Calculate statistical features
            Scalar mean = Core.mean(lbp);
            MatOfDouble stdMat = new MatOfDouble();
            Core.meanStdDev(lbp, new MatOfDouble(), stdMat);
            
            double meanIntensity = mean.val[0] / 255.0; // Normalize to [0,1]
            double stdDeviation = stdMat.get(0, 0)[0] / 255.0; // Normalize to [0,1]
            
            textureFeatures.put("mean_intensity", meanIntensity);
            textureFeatures.put("std_deviation", stdDeviation);
            textureFeatures.put("contrast", calculateContrast(blurredImage));
            textureFeatures.put("homogeneity", calculateHomogeneity(blurredImage));
            
            logger.info("Extracted normalized texture features for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error extracting texture features: {}", e.getMessage());
            textureFeatures.put("mean_intensity", 0.0);
            textureFeatures.put("std_deviation", 0.0);
            textureFeatures.put("contrast", 0.0);
            textureFeatures.put("homogeneity", 0.0);
        }
        
        return textureFeatures;
    }
    
    public static List<Double> extractColorHistogram(String imagePath) {
        List<Double> histogram = new ArrayList<>();
        
        try {
            Mat image = Imgcodecs.imread(imagePath);
            if (image.empty()) {
                throw new RuntimeException("Could not load image: " + imagePath);
            }
            
            // Split into BGR channels (OpenCV default order)
            List<Mat> channels = new ArrayList<>();
            Core.split(image, channels);
            
            // Process channels in fixed order (B, G, R)
            for (int i = 0; i < 3; i++) {
                Mat hist = new Mat();
                Imgproc.calcHist(
                    Arrays.asList(channels.get(i)),
                    new MatOfInt(0),
                    new Mat(),
                    hist,
                    new MatOfInt(BINS),
                    new MatOfFloat(RANGE)
                );
                
                // Normalize histogram
                Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
                
                // Add normalized values
                for (int bin = 0; bin < BINS; bin++) {
                    histogram.add(hist.get(bin, 0)[0]);
                }
            }
            
            logger.info("Extracted color histogram for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error extracting color histogram: {}", e.getMessage());
            // Return default histogram
            for (int i = 0; i < BINS * 3; i++) {
                histogram.add(0.0);
            }
        }
        
        return histogram;
    }
    
    public static List<Double> extractEdgeFeatures(String imagePath) {
        List<Double> edgeFeatures = new ArrayList<>();
        
        try {
            Mat image = Imgcodecs.imread(imagePath);
            if (image.empty()) {
                throw new RuntimeException("Could not load image: " + imagePath);
            }
            
            // Convert to grayscale and normalize
            Mat grayImage = new Mat();
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            Core.normalize(grayImage, grayImage, 0, 255, Core.NORM_MINMAX);
            
            // Apply Gaussian blur to reduce noise
            Mat blurredImage = new Mat();
            Imgproc.GaussianBlur(grayImage, blurredImage, new Size(3, 3), 0);
            
            // Apply Canny edge detection with fixed thresholds
            Mat edges = new Mat();
            Imgproc.Canny(blurredImage, edges, 50, 150);
            
            // Calculate edge density (normalized)
            Scalar edgeSum = Core.sumElems(edges);
            double edgeDensity = edgeSum.val[0] / (edges.rows() * edges.cols() * 255.0);
            
            // Calculate Sobel gradients
            Mat sobelX = new Mat(), sobelY = new Mat();
            Imgproc.Sobel(blurredImage, sobelX, CvType.CV_64F, 1, 0, 3);
            Imgproc.Sobel(blurredImage, sobelY, CvType.CV_64F, 0, 1, 3);
            
            // Normalize gradients
            Core.normalize(sobelX, sobelX, 0, 1, Core.NORM_MINMAX);
            Core.normalize(sobelY, sobelY, 0, 1, Core.NORM_MINMAX);
            
            edgeFeatures.add(edgeDensity);
            edgeFeatures.add(calculateEdgeOrientation(sobelX, sobelY));
            
            logger.info("Extracted normalized edge features for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error extracting edge features: {}", e.getMessage());
            edgeFeatures.add(0.0);
            edgeFeatures.add(0.0);
        }
        
        return edgeFeatures;
    }
    
    public static Map<String, Double> extractDimensions(String imagePath) {
        Map<String, Double> dimensions = new HashMap<>();
        
        try {
            Mat image = Imgcodecs.imread(imagePath);
            if (image.empty()) {
                throw new RuntimeException("Could not load image: " + imagePath);
            }
            
            dimensions.put("width", (double) image.cols());
            dimensions.put("height", (double) image.rows());
            dimensions.put("aspect_ratio", (double) image.cols() / image.rows());
            dimensions.put("area", (double) (image.cols() * image.rows()));
            
            logger.info("Extracted dimensions for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error extracting dimensions: {}", e.getMessage());
            dimensions.put("width", 0.0);
            dimensions.put("height", 0.0);
            dimensions.put("aspect_ratio", 1.0);
            dimensions.put("area", 0.0);
        }
        
        return dimensions;
    }
    
    // Helper methods
    private static void calculateLBP(Mat src, Mat dst) {
        dst.create(src.size(), src.type());
        
        for (int i = 1; i < src.rows() - 1; i++) {
            for (int j = 1; j < src.cols() - 1; j++) {
                double center = src.get(i, j)[0];
                int lbpValue = 0;
                
                // Compare with 8 neighbors
                if (src.get(i-1, j-1)[0] >= center) lbpValue |= 1;
                if (src.get(i-1, j)[0] >= center) lbpValue |= 2;
                if (src.get(i-1, j+1)[0] >= center) lbpValue |= 4;
                if (src.get(i, j+1)[0] >= center) lbpValue |= 8;
                if (src.get(i+1, j+1)[0] >= center) lbpValue |= 16;
                if (src.get(i+1, j)[0] >= center) lbpValue |= 32;
                if (src.get(i+1, j-1)[0] >= center) lbpValue |= 64;
                if (src.get(i, j-1)[0] >= center) lbpValue |= 128;
                
                dst.put(i, j, lbpValue);
            }
        }
    }
    
    private static double calculateContrast(Mat image) {
        Mat normalized = new Mat();
        Core.normalize(image, normalized, 0, 1, Core.NORM_MINMAX);
        
        Scalar mean = Core.mean(normalized);
        MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(normalized, new MatOfDouble(), std);
        
        return std.get(0, 0)[0]; // Already normalized
    }
    
    private static double calculateHomogeneity(Mat image) {
        Mat normalized = new Mat();
        Core.normalize(image, normalized, 0, 1, Core.NORM_MINMAX);
        
        Mat laplacian = new Mat();
        Imgproc.Laplacian(normalized, laplacian, CvType.CV_64F);
        Scalar variance = Core.mean(laplacian);
        
        // Normalize homogeneity to [0,1]
        return 1.0 / (1.0 + Math.abs(variance.val[0]));
    }
      private static double calculateEdgeOrientation(Mat sobelX, Mat sobelY) {
        Mat normalizedX = new Mat();
        Mat normalizedY = new Mat();
        Core.normalize(sobelX, normalizedX, 0, 1, Core.NORM_MINMAX);
        Core.normalize(sobelY, normalizedY, 0, 1, Core.NORM_MINMAX);
        
        double sumSin = 0.0;
        double sumCos = 0.0;
        int nonZeroCount = 0;
        double threshold = 0.1; // Fixed threshold for normalized gradients
        
        int rows = normalizedX.rows();
        int cols = normalizedX.cols();
        
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                double dx = normalizedX.get(y, x)[0];
                double dy = normalizedY.get(y, x)[0];
                double magnitude = Math.sqrt(dx * dx + dy * dy);
                
                // Only consider strong edges after normalization
                if (magnitude > threshold) {
                    // Use atan2 and accumulate sin/cos components for better average
                    double angle = Math.atan2(dy, dx);
                    sumSin += Math.sin(angle);
                    sumCos += Math.cos(angle);
                    nonZeroCount++;
                }
            }
        }
        
        if (nonZeroCount > 0) {
            // Calculate average angle using arctan of average sin/cos
            double avgAngle = Math.toDegrees(Math.atan2(sumSin / nonZeroCount, sumCos / nonZeroCount));
            if (avgAngle < 0) avgAngle += 180;
            return avgAngle / 180.0; // Normalize to [0,1]
        }
        return 0.0;
    }
}