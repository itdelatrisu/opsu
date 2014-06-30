package org.newdawn.slick.geom;

import java.util.ArrayList;

import org.newdawn.slick.util.FastTrig;

/**
 * An ellipse meeting the <code>Shape</code> contract. The ellipse is actually an approximation using 
 * a series of points generated around the contour of the ellipse.
 * 
 * @author Mark
 */
public class Ellipse extends Shape {
    /**
     * Default number of segments to draw this ellipse with
     */
    protected static final int DEFAULT_SEGMENT_COUNT = 50;
    
    /**
     * The number of segments for graphical representation.
     */
    private int segmentCount;
    /**
     * horizontal radius
     */
    private float radius1;
    /**
     * vertical radius
     */
    private float radius2;

    /**
     * Creates a new Ellipse object.
     *
     * @param centerPointX x coordinate of the center of the ellipse
     * @param centerPointY y coordinate of the center of the ellipse
     * @param radius1 horizontal radius
     * @param radius2 vertical radius
     */
    public Ellipse(float centerPointX, float centerPointY, float radius1, float radius2) {
        this(centerPointX, centerPointY, radius1, radius2, DEFAULT_SEGMENT_COUNT);
    }

    /**
     * Creates a new Ellipse object.
     *
     * @param centerPointX x coordinate of the center of the ellipse
     * @param centerPointY y coordinate of the center of the ellipse
     * @param radius1 horizontal radius
     * @param radius2 vertical radius
     * @param segmentCount how fine to make the ellipse.
     */
    public Ellipse(float centerPointX, float centerPointY, float radius1, float radius2, int segmentCount) {
        this.x = centerPointX - radius1;
        this.y = centerPointY - radius2;
        this.radius1 = radius1;
        this.radius2 = radius2;
        this.segmentCount = segmentCount;
        checkPoints();
    }

    /**
     * Change the shape of this Ellipse
     * 
     * @param radius1 horizontal radius
     * @param radius2 vertical radius
     */
    public void setRadii(float radius1, float radius2) {
    	setRadius1(radius1);
    	setRadius2(radius2);
    }

    /**
     * Get the horizontal radius of the ellipse
     * 
     * @return The horizontal radius of the ellipse
     */
    public float getRadius1() {
        return radius1;
    }

    /**
     * Set the horizontal radius of the ellipse
     * 
     * @param radius1 The horizontal radius to set
     */
    public void setRadius1(float radius1) {
    	if (radius1 != this.radius1) {
	        this.radius1 = radius1;
	        pointsDirty = true;
    	}
    }

    /**
     * Get the vertical radius of the ellipse
     * 
     * @return The vertical radius of the ellipse
     */
    public float getRadius2() {
        return radius2;
    }

    /**
     * Set the vertical radius of the ellipse
     * 
     * @param radius2 The vertical radius to set
     */
    public void setRadius2(float radius2) {
    	if (radius2 != this.radius2) {
	        this.radius2 = radius2;
	        pointsDirty = true;
    	}
    }

    /**
     * Generate the points to outline this ellipse.
     *
     */
    protected void createPoints() {
        ArrayList tempPoints = new ArrayList();

        maxX = -Float.MIN_VALUE;
        maxY = -Float.MIN_VALUE;
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;

        float start = 0;
        float end = 359;
        
        float cx = x + radius1;
        float cy = y + radius2;
        
        int step = 360 / segmentCount;
        
        for (float a=start;a<=end+step;a+=step) {
            float ang = a;
            if (ang > end) {
                ang = end;
            }
            float newX = (float) (cx + (FastTrig.cos(Math.toRadians(ang)) * radius1));
            float newY = (float) (cy + (FastTrig.sin(Math.toRadians(ang)) * radius2));

            if(newX > maxX) {
                maxX = newX;
            }
            if(newY > maxY) {
                maxY = newY;
            }
            if(newX < minX) {
            	minX = newX;
            }
            if(newY < minY) {
            	minY = newY;
            }
            
            tempPoints.add(new Float(newX));
            tempPoints.add(new Float(newY));
        }
        points = new float[tempPoints.size()];
        for(int i=0;i<points.length;i++) {
            points[i] = ((Float)tempPoints.get(i)).floatValue();
        }
    }

    /**
     * @see org.newdawn.slick.geom.Shape#transform(org.newdawn.slick.geom.Transform)
     */
    public Shape transform(Transform transform) {
        checkPoints();
        
        Polygon resultPolygon = new Polygon();
        
        float result[] = new float[points.length];
        transform.transform(points, 0, result, 0, points.length / 2);
        resultPolygon.points = result;
        resultPolygon.checkPoints();

        return resultPolygon;
    }

    /**
     * @see org.newdawn.slick.geom.Shape#findCenter()
     */
    protected void findCenter() {
        center = new float[2];
        center[0] = x + radius1;
        center[1] = y + radius2;
    }

    /**
     * @see org.newdawn.slick.geom.Shape#calculateRadius()
     */
    protected void calculateRadius() {
        boundingCircleRadius = (radius1 > radius2) ? radius1 : radius2;
    }
}
