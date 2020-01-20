package frc.andrew;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Main {
    private static File imagesFolder = new File("images/");
    private static File outputFolder = new File(imagesFolder, "output");

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        File[] files = imagesFolder.listFiles();
        assert files != null;
        for (File file : files) {
            String name = file.getName();

            Mat mat = Imgcodecs.imread("images/" + file.getName());
            showBGR(name, mat);

            /*Mat blur = new Mat();
            Imgproc.blur(mat, blur, new Size(1.5, 1.5));
            showBGR("Blur", blur);*/

            Mat hsv = new Mat();
            Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV);
            //showHSV("HSV", hsv);

            Mat threshold = new Mat();
            Core.inRange(hsv, new Scalar(66, 100, 100), new Scalar(86, 255, 255), threshold);
            showGreyscale("Threshold", threshold);

            /*Mat blur = new Mat();
            Imgproc.blur(threshold, blur, new Size(5.0, 5.0));
            showGreyscale("Blur Threshold", blur);*/

            Mat contoursDrawn = mat.clone();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            for (int i = 0; i < contours.size(); i++) {
                MatOfPoint contour = contours.get(i);

                Moments p = Imgproc.moments(contour);
                int momentX = (int) (p.get_m10() / p.get_m00());
                int momentY = (int) (p.get_m01() / p.get_m00());
                Imgproc.circle(contoursDrawn, new Point(momentX, momentY), 4, new Scalar(52, 122, 235));

                Rect boundingRect = Imgproc.boundingRect(contour);
                int centerX = boundingRect.x + (boundingRect.width / 2);
                int centerY = boundingRect.y + (boundingRect.height / 2);

                double momentXRatio = (momentX - boundingRect.x) / (double) boundingRect.width;
                double momentYRatio = (momentY - boundingRect.y) / (double) boundingRect.height;

                double area = Imgproc.contourArea(contour);
                float ratio = (float) boundingRect.width / boundingRect.height;
                double solidity = Imgproc.contourArea(contour)/(boundingRect.width*boundingRect.height);

                Point textLocation = new Point(centerX + boundingRect.width / 2.3, centerY);

                double epsilon = 0.007 * Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approx, epsilon, true);

                if (solidity >= 0.06 && solidity <= .2 && ratio >= 1.2 && ratio <= 2.9 && momentYRatio >= .6) {
                    Imgproc.drawContours(contoursDrawn, contours, i, new Scalar(0, 0, 255), -1);
                    Imgproc.drawContours(contoursDrawn, contours, i, new Scalar(0, 255, 0), 1);
                    Imgproc.putText(contoursDrawn, String.format("%.2f, %.2f, %.2f", momentXRatio, momentYRatio, area), textLocation, Core.FONT_HERSHEY_SIMPLEX,
                            0.5, new Scalar(255, 0, 255), 1);

                    List<Point> points = new ArrayList<>(Arrays.asList(approx.toArray()));
                    for (Point point : points) {
                        Imgproc.circle(contoursDrawn, point, 2, new Scalar(255, 0, 0), -1);
                    }
                    if (points.size() >= 8) {

                        Point[] eightTargets = new Point[8];
                        points.sort(new Comparator<>() {
                            @Override
                            public int compare(Point point, Point t1) {
                                return -1* Double.compare(getDistanceEstimateFromMoment(point), getDistanceEstimateFromMoment(t1));
                            }

                            // 2x instead of pow(x,2)
                            private double getDistanceEstimateFromMoment(Point point) {
                                return Math.pow(centerX - point.x, 2) + Math.pow(centerY - point.y, 2);
                            }
                        });
                        for (int j = 0; j < 8; j++) {
                            eightTargets[j] = points.get(j);
                        }

                        /*// Sort ascending
                        points.sort(Comparator.comparingDouble(point -> point.x));
                        for (int j = 0; j < 4; j++) {
                            eightTargets.add(points.get(0));
                            points.remove(0);
                        }

                        // Sort descending
                        points.sort((point, t1) -> Double.compare(point.x, t1.x) * -1);
                        for (int j = 0; j < 4; j++) {
                            eightTargets.add(points.get(j));
                        }*/

                        // Draw 8 points
                        for (Point point : eightTargets) {
                            System.out.println("point: " + point.toString());
                            Imgproc.circle(contoursDrawn, point, 3, new Scalar(0, 255, 255), -1);
                        }
                    }
                }
            }

            showBGR("Contours", contoursDrawn);
        }
    }

    public static void showBGR(String name, Mat bgr) {
        HighGui.imshow(name, bgr);
        HighGui.waitKey(0);
    }

    public static void showHSV(String name, Mat mat) {
        Mat bgr = new Mat();
        Imgproc.cvtColor(mat, bgr, Imgproc.COLOR_HSV2BGR);
        showBGR(name, bgr);
    }

    public static void showGreyscale(String name, Mat mat) {
        Mat greyscale = new Mat();
        Imgproc.cvtColor(mat, greyscale, Imgproc.COLOR_GRAY2BGR);
        showBGR(name, greyscale);
    }
}