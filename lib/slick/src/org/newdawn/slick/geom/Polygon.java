package org.newdawn.slick.geom;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A polygon implementation meeting the <code>Shape</code> contract. 
 * 
 * @author Mark
 */
public class Polygon extends Shape {
	/** Allow duplicated points */
	private boolean allowDups = false;
	/** True if the polygon is closed */
	private boolean closed = true;
	
    /**
     * Construct a new polygon with 3 or more points. 
     * This constructor will take the first set of points and copy them after
     * the last set of points to create a closed shape.
     * 
     * @param points An array of points in x, y order.
     */
    public Polygon(float points[]) {
        int length = points.length;
        
        this.points = new float[length];
        maxX = -Float.MIN_VALUE;
        maxY = -Float.MIN_VALUE;
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        x = Float.MAX_VALUE;
        y = Float.MAX_VALUE;
        
        for(int i=0;i<length;i++) {
            this.points[i] = points[i];
            if(i % 2 == 0) {
                if(points[i] > maxX) {
                    maxX = points[i];
                }
                if(points[i] < minX) {
                	minX = points[i];
                }
                if(points[i] < x) {
                    x = points[i];
                }
            }
            else {
                if(points[i] > maxY) {
                    maxY = points[i];
                }
                if(points[i] < minY) {
                	minY = points[i];
                }
                if(points[i] < y) {
                    y = points[i];
                }
            }
        }
        
        findCenter();
        calculateRadius();
        pointsDirty = true;
    }
    /**
     * Create an empty polygon
     *
     */
    public Polygon(){
        points = new float[0];
        maxX = -Float.MIN_VALUE;
        maxY = -Float.MIN_VALUE;
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
    }
    
    /**
     * Indicate if duplicate points are allow
     * 
     * @param allowDups True if duplicate points are allowed
     */
    public void setAllowDuplicatePoints(boolean allowDups) {
    	this.allowDups = allowDups;
    }
    
    /**
     * Add a point to the polygon
     * 
     * @param x The x coordinate of the point
     * @param y The y coordinate of the point
     */
    public void addPoint(float x, float y) {
    	if (hasVertex(x,y) && (!allowDups)) {
    		return;
    	}
    	
        ArrayList tempPoints = new ArrayList();
        for(int i=0;i<points.length;i++) {
            tempPoints.add(new Float(points[i]));
        }
        tempPoints.add(new Float(x));
        tempPoints.add(new Float(y));
        int length = tempPoints.size();
        points = new float[length];
        for(int i=0;i<length;i++) {
            points[i] = ((Float)tempPoints.get(i)).floatValue();
        }
        if(x > maxX) {
            maxX = x;
        }
        if(y > maxY) {
            maxY = y;
        }
        if(x < minX) {
            minX = x;
        }
        if(y < minY) {
            minY = y;
        }
        findCenter();
        calculateRadius();
        
        pointsDirty = true;
    }


    /**
     * Apply a transformation and return a new shape.  This will not alter the current shape but will 
     * return the transformed shape.
     * 
     * @param transform The transform to be applied
     * @return The transformed shape.
     */
    public Shape transform(Transform transform) {
        checkPoints();
        
        Polygon resultPolygon = new Polygon();
        
        float result[] = new float[points.length];
        transform.transform(points, 0, result, 0, points.length / 2);
        resultPolygon.points = result;
        resultPolygon.findCenter();
        resultPolygon.closed = closed;

        return resultPolygon;
    }
    
    /**
     * @see org.newdawn.slick.geom.Shape#setX(float)
     */
    public void setX(float x) {
        super.setX(x);
        
        pointsDirty = false;
    }
    
    /**
     * @see org.newdawn.slick.geom.Shape#setY(float)
     */
    public void setY(float y) {
        super.setY(y);
        
        pointsDirty = false;
    }
    
    /**
     * @see org.newdawn.slick.geom.Shape#createPoints()
     */
    protected void createPoints() {
//    	This is empty since a polygon must have it's points all the time.
    }
    
    /**
     * @see org.newdawn.slick.geom.Shape#closed()
     */
	public boolean closed() {
		return closed;
	}
	
	/**
	 * Indicate if the polygon should be closed
	 * 
	 * @param closed True if the polygon should be closed
	 */
	public void setClosed(boolean closed) {
		this.closed = closed;
	}
	
	/**
	 * Provide a copy of this polygon
	 * 
	 * @return A copy of this polygon
	 */
	public Polygon copy() {
		float[] copyPoints = new float[points.length];
		System.arraycopy(points, 0, copyPoints, 0, copyPoints.length);
		
		return new Polygon(copyPoints);
	}
}
