package frc.andrew;

import org.opencv.core.Point;

public class RectUtil {

    public static RectanglePoints findCorners(Point[] points, Point center) {
        if (points.length != 4) {
            throw new IllegalArgumentException("Not a rectangle");
        }
        Point topLeft = null, topRight = null, bottomLeft = null, bottomRight = null;
        for (Point point : points) {
            if (point.x < center.x && point.y < center.y) {
                topLeft = point;
            } else if (point.x > center.x && point.y < center.y) {
                topRight = point;
            } else if (point.x < center.x && point.y > center.y) {
                bottomLeft = point;
            } else if (point.x > center.x && point.y > center.y) {
                bottomRight = point;
            }
        }
        return new RectanglePoints(topLeft, topRight, bottomLeft, bottomRight);
    }

    public static class RectanglePoints {
        public Point topLeft, topRight, bottomLeft, bottomRight;

        public RectanglePoints(Point topLeft, Point topRight, Point bottomLeft, Point bottomRight) {
            this.topLeft = topLeft;
            this.topRight = topRight;
            this.bottomLeft = bottomLeft;
            this.bottomRight = bottomRight;
        }
    }
}
