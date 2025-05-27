package com.clothauth.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ImageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);
    
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
            
            // Convert to grayscale
            Mat grayImage = new Mat();
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            
            // Calculate texture features using Local Binary Pattern approximation
            Mat lbp = new Mat();
            calculateLBP(grayImage, lbp);
            
            // Calculate statistical features
            Scalar mean = Core.mean(lbp);
            MatOfDouble std = new MatOfDouble();
            Core.meanStdDev(lbp, new MatOfDouble(), std);
            
            textureFeatures.put("mean_intensity", mean.val[0]);
            textureFeatures.put("std_deviation", std.get(0, 0)[0]);
            textureFeatures.put("contrast", calculateContrast(grayImage));
            textureFeatures.put("homogeneity", calculateHomogeneity(grayImage));
            
            logger.info("Extracted texture features for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error extracting texture features: {}", e.getMessage());
            // Return default values
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
            
            // Split channels
            List<Mat> channels = new ArrayList<>();
            Core.split(image, channels);
            
            // Calculate histogram for each channel
            for (Mat channel : channels) {
                Mat hist = new Mat();
                Imgproc.calcHist(
                    Arrays.asList(channel),
                    new MatOfInt(0),
                    new Mat(),
                    hist,
                    new MatOfInt(256),
                    new MatOfFloat(0, 256)
                );
                
                // Normalize and add to histogram
                Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
                for (int i = 0; i < hist.rows(); i++) {
                    histogram.add(hist.get(i, 0)[0]);
                }
            }
            
            logger.info("Extracted color histogram for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error extracting color histogram: {}", e.getMessage());
            // Return default histogram
            for (int i = 0; i < 768; i++) { // 256 * 3 channels
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
            
            // Convert to grayscale
            Mat grayImage = new Mat();
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            
            // Apply Canny edge detection
            Mat edges = new Mat();
            Imgproc.Canny(grayImage, edges, 50, 150);
            
            // Calculate edge statistics
            Scalar edgeSum = Core.sumElems(edges);
            double edgeDensity = edgeSum.val[0] / (edges.rows() * edges.cols());
            
            // Calculate edge orientation histogram
            Mat sobelX = new Mat(), sobelY = new Mat();
            Imgproc.Sobel(grayImage, sobelX, CvType.CV_64F, 1, 0, 3);
            Imgproc.Sobel(grayImage, sobelY, CvType.CV_64F, 0, 1, 3);
            
            edgeFeatures.add(edgeDensity);
            edgeFeatures.add(calculateEdgeOrientation(sobelX, sobelY));
            
            logger.info("Extracted edge features for image: {}", imagePath);
            
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
        Scalar mean = Core.mean(image);
        MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(image, new MatOfDouble(), std);
        return std.get(0, 0)[0] / mean.val[0];
    }
    
    private static double calculateHomogeneity(Mat image) {
        // Simplified homogeneity calculation
        Mat laplacian = new Mat();
        Imgproc.Laplacian(image, laplacian, CvType.CV_64F);
        Scalar variance = Core.mean(laplacian);
        return 1.0 / (1.0 + Math.abs(variance.val[0]));
    }
    
    private static double calculateEdgeOrientation(Mat sobelX, Mat sobelY) {
        double sumOrientation = 0;
        int count = 0;
        
        for (int i = 0; i < sobelX.rows(); i++) {
            for (int j = 0; j < sobelX.cols(); j++) {
                double gx = sobelX.get(i, j)[0];
                double gy = sobelY.get(i, j)[0];
                if (Math.abs(gx) > 10 || Math.abs(gy) > 10) {
                    sumOrientation += Math.atan2(gy, gx);
                    count++;
                }
            }
        }
        
        return count > 0 ? sumOrientation / count : 0;
    }
}