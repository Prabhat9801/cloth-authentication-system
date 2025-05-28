package com.clothauth.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ImageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);
    private static final int DECIMAL_PRECISION = 4; // Match HashGenerator precision
    private static final int COLOR_CONVERT_CODE = Imgproc.COLOR_BGR2RGB;
    private static final Size GAUSSIAN_BLUR_SIZE = new Size(3, 3);
    private static final double GAUSSIAN_BLUR_SIGMA = 0.0;
    
    static {
        // Load OpenCV native library
        nu.pattern.OpenCV.loadLocally();
    }
    
    public static Map<String, Double> extractTextureFeatures(
        String imagePath, 
        int cannyLowThreshold,
        int cannyHighThreshold,
        int lbpRadius,
        int lbpNeighbors
    ) {
        TreeMap<String, Double> textureFeatures = new TreeMap<>();
        
        try {
            // Read image in grayscale mode for consistency
            Mat image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_GRAYSCALE);
            if (image.empty()) {
                throw new RuntimeException("Could not load image: " + imagePath);
            }
            
            // Apply Gaussian blur to reduce noise
            Mat blurredImage = new Mat();
            Imgproc.GaussianBlur(image, blurredImage, GAUSSIAN_BLUR_SIZE, GAUSSIAN_BLUR_SIGMA);
            
            // Calculate LBP features
            Mat lbp = new Mat();
            calculateLBP(blurredImage, lbp, lbpRadius, lbpNeighbors);
            
            // Calculate statistical features
            Scalar mean = Core.mean(lbp);
            MatOfDouble std = new MatOfDouble();
            Core.meanStdDev(lbp, new MatOfDouble(), std);
            
            // Store features in sorted order
            textureFeatures.put("contrast", roundToDecimal(calculateContrast(blurredImage)));
            textureFeatures.put("homogeneity", roundToDecimal(calculateHomogeneity(blurredImage)));
            textureFeatures.put("mean_intensity", roundToDecimal(mean.val[0]));
            textureFeatures.put("std_deviation", roundToDecimal(std.get(0, 0)[0]));
            
            logger.info("Extracted texture features for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error extracting texture features: {}", e.getMessage());
            textureFeatures.put("contrast", 0.0);
            textureFeatures.put("homogeneity", 0.0);
            textureFeatures.put("mean_intensity", 0.0);
            textureFeatures.put("std_deviation", 0.0);
        }
        
        return textureFeatures;
    }
    
    public static List<Double> extractColorHistogram(String imagePath, int bins) {
        List<Double> histogram = new ArrayList<>();
        
        try {
            // Read image in BGR mode
            Mat image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_COLOR);
            if (image.empty()) {
                throw new RuntimeException("Could not load image: " + imagePath);
            }
            
            // Convert to RGB with consistent parameters
            Mat rgbImage = new Mat();
            Imgproc.cvtColor(image, rgbImage, COLOR_CONVERT_CODE);
            
            // Split channels
            List<Mat> channels = new ArrayList<>();
            Core.split(rgbImage, channels);
            
            // Calculate histogram for each channel
            MatOfInt histSize = new MatOfInt(bins);
            MatOfFloat ranges = new MatOfFloat(0f, 256f);
            MatOfInt channels_arr = new MatOfInt(0);
            Mat mask = new Mat();
            
            for (Mat channel : channels) {
                Mat hist = new Mat();
                Imgproc.calcHist(
                    Arrays.asList(channel),
                    channels_arr,
                    mask,
                    hist,
                    histSize,
                    ranges
                );
                
                // Normalize with consistent parameters
                Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX, -1, new Mat());
                
                // Round values for consistency
                for (int i = 0; i < hist.rows(); i++) {
                    histogram.add(roundToDecimal(hist.get(i, 0)[0]));
                }
            }
            
            logger.info("Extracted color histogram for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error extracting color histogram: {}", e.getMessage());
            // Return default histogram
            for (int i = 0; i < bins * 3; i++) {
                histogram.add(0.0);
            }
        } finally {
            // Release OpenCV resources
            System.gc();
        }
        
        return histogram;
    }
    
    public static List<Double> extractEdgeFeatures(
        String imagePath,
        int cannyLowThreshold,
        int cannyHighThreshold
    ) {
        List<Double> edgeFeatures = new ArrayList<>();
        
        try {
            // Read image in grayscale mode
            Mat image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_GRAYSCALE);
            if (image.empty()) {
                throw new RuntimeException("Could not load image: " + imagePath);
            }
            
            // Apply Gaussian blur with consistent parameters
            Mat blurredImage = new Mat();
            Imgproc.GaussianBlur(image, blurredImage, GAUSSIAN_BLUR_SIZE, GAUSSIAN_BLUR_SIGMA);
            
            // Apply Canny edge detection with consistent thresholds
            Mat edges = new Mat();
            Imgproc.Canny(blurredImage, edges, cannyLowThreshold, cannyHighThreshold);
            
            // Calculate edge statistics
            Scalar edgeSum = Core.sumElems(edges);
            double edgeDensity = roundToDecimal(edgeSum.val[0] / (edges.rows() * edges.cols()));
            
            // Calculate edge orientation
            Mat sobelX = new Mat(), sobelY = new Mat();
            Imgproc.Sobel(blurredImage, sobelX, CvType.CV_64F, 1, 0, 3);
            Imgproc.Sobel(blurredImage, sobelY, CvType.CV_64F, 0, 1, 3);
            
            double orientation = calculateEdgeOrientation(sobelX, sobelY);
            
            edgeFeatures.add(edgeDensity);
            edgeFeatures.add(roundToDecimal(orientation));
            
            logger.info("Extracted edge features for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error extracting edge features: {}", e.getMessage());
            edgeFeatures.add(0.0);
            edgeFeatures.add(0.0);
        }
        
        return edgeFeatures;
    }
    
    public static Map<String, Double> extractDimensions(String imagePath) {
        TreeMap<String, Double> dimensions = new TreeMap<>();
        
        try {
            Mat image = Imgcodecs.imread(imagePath);
            if (image.empty()) {
                throw new RuntimeException("Could not load image: " + imagePath);
            }
            
            // Store dimensions in sorted order
            dimensions.put("area", roundToDecimal((double) (image.cols() * image.rows())));
            dimensions.put("aspect_ratio", roundToDecimal((double) image.cols() / image.rows()));
            dimensions.put("height", roundToDecimal((double) image.rows()));
            dimensions.put("width", roundToDecimal((double) image.cols()));
            
            logger.info("Extracted dimensions for image: {}", imagePath);
            
        } catch (Exception e) {
            logger.error("Error extracting dimensions: {}", e.getMessage());
            dimensions.put("area", 0.0);
            dimensions.put("aspect_ratio", 1.0);
            dimensions.put("height", 0.0);
            dimensions.put("width", 0.0);
        }
        
        return dimensions;
    }
    
    private static void calculateLBP(Mat src, Mat dst, int radius, int neighbors) {
        dst.create(src.size(), src.type());
        
        for (int i = radius; i < src.rows() - radius; i++) {
            for (int j = radius; j < src.cols() - radius; j++) {
                double center = src.get(i, j)[0];
                int lbpValue = 0;
                
                for (int n = 0; n < neighbors; n++) {
                    double x = j + radius * Math.cos(2 * Math.PI * n / neighbors);
                    double y = i - radius * Math.sin(2 * Math.PI * n / neighbors);
                    
                    int x1 = (int) Math.floor(x);
                    int y1 = (int) Math.floor(y);
                    
                    // Use bilinear interpolation for consistent sampling
                    double tx = x - x1;
                    double ty = y - y1;
                    
                    double value = (1 - tx) * (1 - ty) * src.get(y1, x1)[0] +
                                 tx * (1 - ty) * src.get(y1, x1 + 1)[0] +
                                 (1 - tx) * ty * src.get(y1 + 1, x1)[0] +
                                 tx * ty * src.get(y1 + 1, x1 + 1)[0];
                    
                    if (value >= center) {
                        lbpValue |= (1 << n);
                    }
                }
                
                dst.put(i, j, lbpValue);
            }
        }
    }

    private static double calculateContrast(Mat image) {
        double contrast = 0;
        Mat glcm = calculateGLCM(image);
        
        int size = (int) glcm.total();
        int dim = (int) Math.sqrt(size);
        
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                contrast += glcm.get(i, j)[0] * Math.pow(i - j, 2);
            }
        }
        
        return contrast;
    }
    
    private static double calculateHomogeneity(Mat image) {
        double homogeneity = 0;
        Mat glcm = calculateGLCM(image);
        
        int size = (int) glcm.total();
        int dim = (int) Math.sqrt(size);
        
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                homogeneity += glcm.get(i, j)[0] / (1 + Math.pow(i - j, 2));
            }
        }
        
        return homogeneity;
    }
    
    private static Mat calculateGLCM(Mat image) {
        // Calculate GLCM with fixed parameters for consistency
        int levels = 8;
        Mat scaled = new Mat();
        image.convertTo(scaled, CvType.CV_32F);
        Core.normalize(scaled, scaled, 0, levels - 1, Core.NORM_MINMAX);
        scaled.convertTo(scaled, CvType.CV_8U);
        
        Mat glcm = Mat.zeros(levels, levels, CvType.CV_64F);
        
        // Calculate horizontal GLCM with distance 1
        for (int i = 0; i < image.rows(); i++) {
            for (int j = 0; j < image.cols() - 1; j++) {
                int val1 = (int) scaled.get(i, j)[0];
                int val2 = (int) scaled.get(i, j + 1)[0];
                glcm.put(val1, val2, glcm.get(val1, val2)[0] + 1);
            }
        }
        
        // Normalize GLCM
        Core.normalize(glcm, glcm, 0, 1, Core.NORM_MINMAX);
        
        return glcm;
    }
    
    private static double calculateEdgeOrientation(Mat sobelX, Mat sobelY) {
        Scalar meanX = Core.mean(sobelX);
        Scalar meanY = Core.mean(sobelY);
        double orientation = Math.atan2(meanY.val[0], meanX.val[0]);
        return orientation;
    }
    
    private static double roundToDecimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        double scale = Math.pow(10, DECIMAL_PRECISION);
        return Math.round(value * scale) / scale;
    }
}