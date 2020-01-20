package frc.andrew;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.File;
import java.util.*;

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

            Mat dilateClose = new Mat();
            // Dilate & close
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3,3));
            Imgproc.dilate(threshold, dilateClose, kernel);
            Imgproc.morphologyEx(dilateClose, dilateClose, Imgproc.MORPH_CLOSE, kernel);
            showGreyscale("Morphology", dilateClose);

            /*Mat blur = new Mat();
            Imgproc.blur(threshold, blur, new Size(5.0, 5.0));
            showGreyscale("Blur Threshold", blur);*/

            Mat contoursDrawn = mat.clone();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(dilateClose, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            long start = System.currentTimeMillis();
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

                double epsilon = 0.005 * Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approx, epsilon, true);

                Imgproc.drawContours(contoursDrawn, contours, i, new Scalar(0, 255, 0), 1);
                //Imgproc.putText(contoursDrawn, "solidity, ratio: " + String.format("%.2f", solidity) + " " + ratio, textLocation, Core.FONT_HERSHEY_SIMPLEX,
                //        0.5, new Scalar(255, 0, 255), 1);
                Imgproc.putText(contoursDrawn, String.format("%.2f, %.2f, %.2f", momentXRatio, momentYRatio, area), textLocation, Core.FONT_HERSHEY_SIMPLEX,
                        0.5, new Scalar(255, 0, 255), 1);
                Imgproc.drawContours(contoursDrawn, contours, i, new Scalar(0, 0, 255), -1);
                if (solidity >= 0.06 && solidity <= .2 && ratio <= 2.9 && momentYRatio >= .6) {
                    Imgproc.drawContours(contoursDrawn, contours, i, new Scalar(0, 0, 255), -1);

                    List<Point> points = new ArrayList<>(Arrays.asList(approx.toArray()));
                    for (Point point : points) {
                        Imgproc.circle(contoursDrawn, point, 2, new Scalar(255, 0, 0), -1);
                    }
                    if (points.size() >= 4) {

                        Point[] eightTargets = new Point[8];
                        // TODO : Better algorithm idea:
                        /*
                            Find points to left of center
                            Find points to right of center
                            For each side, closest 2 points =
                         */

                        var minRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
                        Point[] rectPoints = new Point[4];
                        minRect.points(rectPoints);



                        /*for (int j = 0; j < 4; j++) {
                            Imgproc.line(contoursDrawn, rectPoints[j], rectPoints[(j+1)%4], new Scalar(255, 120, 255));
                        }*/
                        // use center of normal bounding box
                        var sortedPoints = RectUtil.findCorners(rectPoints, new Point(centerX, centerY));
                        Imgproc.circle(contoursDrawn, sortedPoints.bottomLeft, 3, new Scalar(0, 0, 255));
                        Imgproc.circle(contoursDrawn, sortedPoints.bottomRight, 3, new Scalar(66, 179, 279));
                        Imgproc.circle(contoursDrawn, sortedPoints.topRight, 3, new Scalar(5, 243, 255));
                        Imgproc.circle(contoursDrawn, sortedPoints.topLeft, 3, new Scalar(5, 255, 5));

                        // Top left & top right of target
                        eightTargets[0] = sortedPoints.topLeft;
                        eightTargets[1] = sortedPoints.topRight;

                        // Now find bottom left and bottom right
                        double toleranceSquared = Math.pow(28, 2);
                        List<Point> closeToBottomLine = new ArrayList<>();
                        for (Point point : points) {
                            if (distanceSquared(point, sortedPoints.bottomLeft, sortedPoints.bottomRight) <= toleranceSquared) {
                                closeToBottomLine.add(point);
                            }
                        }

                        // Sort bottom two points from lowest x to highest x
                        closeToBottomLine.sort(Comparator.comparing(p2 -> p2.x));
                        if (closeToBottomLine.size() <= 2) {
                            continue;
                        }
                        eightTargets[2] = closeToBottomLine.get(0);
                        eightTargets[3] = closeToBottomLine.get(closeToBottomLine.size() - 1);

                        /*double toleranceSquared = Math.pow(28, 2);
                        List<Point> closeToTopLine = new ArrayList<>();
                        for (int j = 0; j < points.size(); j++) {
                            Point point = (Point) points.toArray()[j];
                            if (distanceSquared(point, sortedPoints.topLeft, sortedPoints.topRight) <= toleranceSquared) {
                                closeToTopLine.add(point);
                                points.remove(j); //todo :optimize with for j loop
                            }
                        }

                        if (closeToTopLine.size() <= 2) {
                            continue;
                        }

                        // Sort top four points from lowest x to highest x
                        closeToTopLine.sort(Comparator.comparingDouble(p2 -> p2.x));

                        eightTargets[0] = getIfExists(closeToTopLine, 0);
                        eightTargets[1] = getIfExists(closeToTopLine, 1);
                        eightTargets[2] = getIfExists(closeToTopLine, closeToTopLine.size() - 1);
                        eightTargets[3] = getIfExists(closeToTopLine, closeToTopLine.size() - 2);*/

                        /*List<Point> closeToBottomLine = new ArrayList<>();
                        for (Point point : points) {
                            if (distanceSquared(point, sortedPoints.bottomLeft, sortedPoints.bottomRight) <= toleranceSquared) {
                                closeToBottomLine.add(point);
                            }
                        }

                        if (closeToBottomLine.size() <= 2) {
                            continue;
                        }

                        // Sort bottom two points from lowest x to highest x
                        closeToBottomLine.sort(Comparator.comparing(p2 -> p2.x));
                        eightTargets[4] = getIfExists(closeToBottomLine, 0);
                        eightTargets[5] = getIfExists(closeToBottomLine, 1);*/

                        /*Point[] left = Arrays.copyOfRange(sorted, 0, 4);
                        Point[] right = Arrays.copyOfRange(sorted, 4, 8);

                        // Close & far left
                        Arrays.sort(left, new Comparator<>() {
                            @Override
                            public int compare(Point p1, Point p2) {
                                return Double.compare(getDistanceSquaredFromCenter(p1), getDistanceSquaredFromCenter(p2));
                            }

                            private double getDistanceSquaredFromCenter(Point p) {
                                return Math.pow((centerX - p.x), 2) + Math.pow((centerY - p.y), 2);
                            }
                        });
                        Point[] leftClose = Arrays.copyOfRange(left, 2, 4);
                        Point[] leftFar = Arrays.copyOfRange(left, 0, 2);

                        // Close & far right
                        Arrays.sort(right, new Comparator<>() {
                            @Override
                            public int compare(Point p1, Point p2) {
                                return Double.compare(getDistanceSquaredFromCenter(p1), getDistanceSquaredFromCenter(p2));
                            }

                            private double getDistanceSquaredFromCenter(Point p) {
                                return Math.pow((centerX - p.x), 2) + Math.pow((centerY - p.y), 2);
                            }
                        });
                        Point[] rightClose = Arrays.copyOfRange(right, 2, 4);
                        Point[] rightFar = Arrays.copyOfRange(right, 0, 2);

                        System.arraycopy(leftClose, 0, eightTargets,0, 2);
                        System.arraycopy(rightClose, 0, eightTargets,2, 2);
                        System.arraycopy(leftFar, 0, eightTargets,4, 2);
                        System.arraycopy(rightFar, 0, eightTargets,6, 2);*/

                        /*
                        // Sort by furthest from center to closest from center
                        points.sort(new Comparator<>() {
                            @Override
                            public int compare(Point p1, Point p2) {
                                return Double.compare(getDistanceSquaredFromCenter(p1), getDistanceSquaredFromCenter(p2));
                            }

                            private double getDistanceSquaredFromCenter(Point p) {
                                return Math.pow((centerX - p.x), 2) + Math.pow((centerY - p.y), 2);
                            }
                        });
                        Point[] sortedPoints = points.toArray(new Point[0]);
                        Point[] farFour = Arrays.copyOfRange(sortedPoints, 0, 4);
                        Point[] closeFour = Arrays.copyOfRange(sortedPoints, 4, 8);
                        System.arraycopy(farFour, 0, eightTargets, 0, 4);
                        System.arraycopy(closeFour, 0, eightTargets, 4, 4);*/

                        /*points.sort(new Comparator<>() {
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
                        }*/

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
                            if (point == null) {
                                continue;
                            }
                            System.out.println("point: " + point.toString());
                            Imgproc.circle(contoursDrawn, point, 3, new Scalar(0, 255, 255), -1);
                        }
                    }
                }
            }
            System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");

            if (Objects.equals(args[0], "output")) {
                Imgcodecs.imwrite("images/output/out-" + name, contoursDrawn);
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

    public static float distanceSquared(Point point, Point line1, Point line2) {
        return distanceSquared((float) point.x, (float) point.y, (float) line1.x, (float) line1.y, (float) line2.x, (float) line2.y);
    }

    //https://stackoverflow.com/a/30567488/4543409
    public static float distanceSquared(float x, float y, float x1, float y1, float x2, float y2) {

        float A = x - x1; // position of point rel one end of line
        float B = y - y1;
        float C = x2 - x1; // vector along line
        float D = y2 - y1;
        float E = -D; // orthogonal vector
        float F = C;

        float dot = A * E + B * F;
        float len_sq = E * E + F * F;

        return (float) dot * dot / len_sq;
    }

    public static <T> T getIfExists(List<T> list, int index) {
        if (index <= (list.size() - 1)) {
            return list.get(index);
        } else {
            return null;
        }
    }
}