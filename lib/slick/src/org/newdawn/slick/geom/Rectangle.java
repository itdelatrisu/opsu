package org.newdawn.slick.geom;

/**
 * An axis oriented used for shape bounds
 * 
 * @author Kevin Glass
 */
public class Rectangle extends Shape {
	/** The width of the box */
	protected float width;
	/** The height of the box */
	protected float height;
	
	/**
	 * Create a new bounding box
	 * 
	 * @param x The x position of the box
	 * @param y The y position of the box
	 * @param width The width of the box
	 * @param height The hieght of the box
	 */
	public Rectangle(float x, float y, float width, float height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		maxX = x+width;
		maxY = y+height;
		checkPoints();
	}
	
	/**
	 * Check if this rectangle contains a point
	 * 
	 * @param xp The x coordinate of the point to check
	 * @param yp The y coordinate of the point to check
	 * @return True if the point is within the rectangle
	 */
	public boolean contains(float xp, float yp) {
		if (xp <= getX()) {
			return false;
		}
		if (yp <= getY()) {
			return false;
		}
		if (xp >= maxX) {
			return false;
		}
		if (yp >= maxY) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Set the bounds of this rectangle based on the given rectangle
	 * 
	 * @param other The other rectangle whose bounds should be applied
	 */
	public void setBounds(Rectangle other) {
		setBounds(other.getX(), other.getY(), other.getWidth(), other.getHeight());
	}
	
	/**
	 * Set the bounds of this rectangle
	 * 
	 * @param x The x coordinate of this rectangle
	 * @param y The y coordinate of this rectangle
	 * @param width The width to set in this rectangle
	 * @param height The height to set in this rectangle
	 */
	public void setBounds(float x, float y, float width, float height) {
		setX(x);
		setY(y);
		setSize(width, height);
	}

	/**
	 * Set the size (widtha and height) of this rectangle
	 * 
	 * @param width The width to set in this rectangle
	 * @param height The height to set in this rectangle
	 */
	public void setSize(float width, float height) {
		setWidth(width);
		setHeight(height);
	}
	
	
	/**
	 * Get the width of the box
	 * 
	 * @return The width of the box
	 */
	public float getWidth() {
		return width;
	}
	
	/**
	 * Get the height of the box
	 * 
	 * @return The height of the box
	 */
	public float getHeight() {
		return height;
	}
	
	/**
	 * Grow the rectangle at all edges by the given amounts. This will result in the
	 * rectangle getting larger around it's centre.
	 * 
	 * @param h The amount to adjust horizontally
	 * @param v The amount to ajust vertically
	 */
	public void grow(float h, float v) {
		setX(getX() - h);
		setY(getY() - v);
		setWidth(getWidth() + (h*2));
		setHeight(getHeight() + (v*2));
	}

	/**
	 * Grow the rectangle based on scaling it's size
	 * 
	 * @param h The scale to apply to the horizontal 
	 * @param v The scale to appy to the vertical
	 */
	public void scaleGrow(float h, float v) {
		grow(getWidth() * (h-1), getHeight() * (v-1));
	}
	
	/**
	 * Set the width of this box
	 * 
	 * @param width The new width of this box
	 */
	public void setWidth(float width) {
		if (width != this.width) {
	        pointsDirty = true;
			this.width = width;
			maxX = x+width;
		}
	}
	
	/**
	 * Set the heightof this box
	 * 
	 * @param height The height of this box
	 */
	public void setHeight(float height) {
		if (height != this.height) {
	        pointsDirty = true;
			this.height = height;
			maxY = y+height;
		}
	}
	
	/**
	 * Check if this box touches another
	 * 
	 * @param shape The other shape to check against
	 * @return True if the rectangles touch
	 */
	public boolean intersects(Shape shape) {
        if(shape instanceof Rectangle) {
            Rectangle other = (Rectangle)shape;
    		if ((x > (other.x + other.width)) || ((x + width) < other.x)) {
    			return false;
    		}
    		if ((y > (other.y + other.height)) || ((y + height) < other.y)) {
    			return false;
    		}
            return true;
        }
        else if(shape instanceof Circle) {
            return intersects((Circle)shape);
        }
        else {
            return super.intersects(shape);
        }
	}

	protected void createPoints() {
        float useWidth = width ;
        float useHeight = height;
        points = new float[8];
        
        points[0] = x;
        points[1] = y;
        
        points[2] = x + useWidth;
        points[3] = y;
        
        points[4] = x + useWidth;
        points[5] = y + useHeight;
        
        points[6] = x;
        points[7] = y + useHeight;
        
        maxX = points[2];
        maxY = points[5];
        minX = points[0];
        minY = points[1];
        
        findCenter();
        calculateRadius();
    }

    /**
	 * Check if a circle touches this rectangle
	 * 
	 * @param other The circle to check against
	 * @return True if they touch
	 */
	private boolean intersects(Circle other) {
		return other.intersects(this);
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[Rectangle "+width+"x"+height+"]";
	}
	
	/**
	 * Check if a rectangle contains a point (static to use it everywhere)
	 * 
	 * @param xp
	 *            The x coordinate of the point to check
	 * @param yp
	 *            The y coordinate of the point to check
	 * @param xr
	 *            The x coordinate of the rectangle
	 * @param yr
	 *            The y coordinate of the rectangle
	 * @param widthr
	 *            The width of the rectangle
	 * @param heightr The height of the rectangle
	 * @return True if the point is within the rectangle
	 */
	public static boolean contains(float xp, float yp, float xr, float yr,
			float widthr, float heightr) {
		return (xp >= xr) && (yp >= yr) && (xp <= xr + widthr)
				&& (yp <= yr + heightr);
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
        resultPolygon.checkPoints();

        return resultPolygon;
    }
}
