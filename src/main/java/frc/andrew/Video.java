package frc.andrew;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
public class Video {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        //Create new MAT object
        Mat frame = new Mat();

        //Create new VideoCapture object
        VideoCapture camera = new VideoCapture(0);
        camera.set(Videoio.CV_CAP_PROP_EXPOSURE,-100);

        //Create new JFrame object
        JFrame jframe = new JFrame("Video Title");

        //Inform jframe what to do in the event that you close the program
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create a new JLabel object vidpanel
        JLabel vidPanel = new JLabel();

        //assign vidPanel to jframe
        jframe.setContentPane(vidPanel);

        //set frame size
        jframe.setSize(640, 480);

        //make jframe visible
        jframe.setVisible(true);

        while (true) {
            //If next video frame is available
            if (camera.read(frame)) {
                Mat mat = frame;

                /*Mat blur = new Mat();
                Imgproc.blur(mat, blur, new Size(1.5, 1.5));
                showBGR("Blur", blur);*/

                Mat hsv = new Mat();
                Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV);
                //showHSV("HSV", hsv);

                Mat threshold = new Mat();
                //Core.inRange(hsv, new Scalar(66, 100, 100), new Scalar(86, 255, 255), threshold);
                Core.inRange(hsv, new Scalar(70, 60, 70), new Scalar(200, 220, 160), threshold);

                Mat blur = new Mat();
                Imgproc.medianBlur(threshold, blur, 5);
                //Imgproc.blur(threshold, blur, new Size(6.0, 8.0));

                Mat dilateClose = new Mat();
                // Dilate & close
                Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3,3));
                Imgproc.dilate(blur, dilateClose, kernel);
                Imgproc.morphologyEx(dilateClose, dilateClose, Imgproc.MORPH_CLOSE, kernel);


                Mat contoursDrawn = mat.clone();
                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(dilateClose, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                long start = System.currentTimeMillis();
                contourProcess:
                {
                    for (int i = 0; i < contours.size(); i++) {
                        MatOfPoint contour = contours.get(i);

                        Moments p = Imgproc.moments(contour);
                        int momentX = (int) (p.get_m10() / p.get_m00());
                        int momentY = (int) (p.get_m01() / p.get_m00());
                        //Imgproc.circle(contoursDrawn, new Point(momentX, momentY), 4, new Scalar(52, 122, 235));

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

                        //Imgproc.drawContours(contoursDrawn, contours, i, new Scalar(0, 255, 0), 1);
                        //Imgproc.putText(contoursDrawn, "solidity, ratio: " + String.format("%.2f", solidity) + " " + ratio, textLocation, Core.FONT_HERSHEY_SIMPLEX,
                        //        0.5, new Scalar(255, 0, 255), 1);
                        //Imgproc.putText(contoursDrawn, String.format("S%.2f, R%.2f, X%.2f, Y%.2f, A%.2f", solidity, ratio, momentXRatio, momentYRatio, area), textLocation, Core.FONT_HERSHEY_SIMPLEX,
                        //        0.4, new Scalar(255, 0, 255), 1);
                        //Imgproc.drawContours(contoursDrawn, contours, i, new Scalar(0, 0, 255), -1);
                        if (area >= 500 && solidity >= 0.06 && solidity <= .3 && ratio <= 2.9 && momentYRatio >= .45 && momentXRatio >= .45 && momentXRatio <= .55) {
                            Imgproc.drawContours(contoursDrawn, contours, i, new Scalar(0, 0, 255), -1);

                            List<Point> points = new ArrayList<>(Arrays.asList(approx.toArray()));
                            for (Point point : points) {
                                //Imgproc.circle(contoursDrawn, point, 2, new Scalar(255, 0, 0), -1);
                            }
                            if (points.size() >= 4) {

                                Point[] fourTargets = new Point[8];
                            /*
                                == ALGORITHM DESCRIPTION ==
                                - Find minRect (Small rectangle, that can be angled, that encloses target)
                                - fourTarget[0] = top left corner of minRect
                                - fourTarget[1] = top right corner of minRect
                                - Then:
                                - Find height of minRect (distance between top left and bottom left)
                                - Find height of tape by multiplying height of minRect by constant (11/75)
                                - Find points near bottom line of minRect, with tolerance being the height of tape
                                - fourTarget[2] = Left-most (min x) of those points
                                - fourTarget[3] = Right-most (max x) of those points
                             */

                                var minRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
                                Point[] rectPoints = new Point[4];
                                minRect.points(rectPoints);
                                /*//Draw minRect
                                for (int j = 0; j < 4; j++) {
                                    Imgproc.line(contoursDrawn, rectPoints[j], rectPoints[(j+1)%4], new Scalar(255, 120, 255));
                                }*/

                                // Use center of normal bounding box (non-angled) to label corners of minRect
                                var sortedPoints = RectUtil.findCorners(rectPoints, new Point(centerX, centerY));
                                if (sortedPoints.bottomLeft == null || sortedPoints.bottomRight == null ||
                                        sortedPoints.topLeft == null || sortedPoints.topRight == null) {
                                    continue;
                                }
                                Imgproc.circle(contoursDrawn, sortedPoints.bottomLeft, 3, new Scalar(0, 0, 255));
                                Imgproc.circle(contoursDrawn, sortedPoints.bottomRight, 3, new Scalar(66, 179, 279));
                                Imgproc.circle(contoursDrawn, sortedPoints.topLeft, 3, new Scalar(5, 243, 255));
                                Imgproc.circle(contoursDrawn, sortedPoints.topRight, 3, new Scalar(5, 255, 5));

                                // Top left & top right of target
                                fourTargets[0] = sortedPoints.topLeft;
                                fourTargets[1] = sortedPoints.topRight;

                                // Find height of minRect via distance formula
                                var minRectHeight = Math.sqrt(
                                        Math.pow(sortedPoints.bottomLeft.x - sortedPoints.topLeft.x, 2) +
                                                Math.pow(sortedPoints.bottomLeft.y - sortedPoints.topLeft.y, 2));

                                // Now find bottom left and bottom right
                                // From photo editor
                                // Height = 75
                                // Tape height = 11
                                // Try to make tolerance a little more than half of tape height (66%)
                                double toleranceSquared = Math.pow(((minRectHeight)*(11/(double)75)), 2); // TODO : Change tolerance based on area/size of Image, otherwise it doesn't work far away
                                //System.out.println("tolerance: " + Math.sqrt(toleranceSquared));
                                List<Point> closeToBottomLine = new ArrayList<>();
                                for (Point point : points) {
                                    if (Main.distanceSquared(point, sortedPoints.bottomLeft, sortedPoints.bottomRight) <= toleranceSquared) {
                                        closeToBottomLine.add(point);
                                    }
                                }

                                // Sort bottom two points from lowest x to highest x
                                closeToBottomLine.sort(Comparator.comparing(p2 -> p2.x));
                                if (closeToBottomLine.size() < 2) {
                                    continue;
                                }
                                fourTargets[2] = closeToBottomLine.get(0);
                                fourTargets[3] = closeToBottomLine.get(closeToBottomLine.size() - 1);

                                Mat rvecs = new Mat();
                                Mat tvecs = new Mat();
                                MatOfPoint3f objectPoints = new MatOfPoint3f();
                                objectPoints.fromList(Arrays.asList(new Point3[]{
                                        new Point3(1, 1, 1)
                                }));
                                MatOfPoint2f imagePoints = new MatOfPoint2f();
                                imagePoints.fromList(Arrays.asList(fourTargets));

                                //Calib3d.solvePnP(objectPoints, imagePoints, )

                                // Text
                                Imgproc.putText(contoursDrawn, "Test", new Point(25, 75), Core.FONT_HERSHEY_SIMPLEX, 1.3, new Scalar(0, 255, 0), 2);

                                // Draw 8 points
                                for (Point point : fourTargets) {
                                    if (point == null) {
                                        continue;
                                    }
                                    //System.out.println("point: " + point.toString());
                                    Imgproc.circle(contoursDrawn, point, 3, new Scalar(0, 255, 255), -1);
                                }
                                //break contourProcess;
                            }
                        }
                    }
                }

                //Create new image icon object and convert Mat to Buffered Image
                ImageIcon image = new ImageIcon(Mat2BufferedImage(contoursDrawn));
                //Update the image in the vidPanel
                vidPanel.setIcon(image);
                //Update the vidPanel in the JFrame
                vidPanel.repaint();


            }
        }
    }

    public static BufferedImage Mat2BufferedImage(Mat m) {
        //Method converts a Mat to a Buffered Image
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if ( m.channels() > 1 ) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte [] b = new byte[bufferSize];
        m.get(0,0,b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }
}