package org.newdawn.slick.geom;

/**
 * A beizer curve implementation. The curve is defined by a start point, an end point
 * and two control points that it will tend towards. This is implementation is fixed
 * segmenting meaning it doesn't scale too well.
 *
 * @author kevin
 */
public class Curve extends Shape {
	/** The start point of the curve */
	private Vector2f p1;
	/** The first control point */
	private Vector2f c1;
	/** The second control point */
	private Vector2f c2;
	/** The end point of the curve */
	private Vector2f p2;
	/** The number of lines segments the curve is built out of */
	private int segments;
	
	/**
	 * Create a new curve with the default segments (20)
	 * 
	 * @param p1 The start of the curve
	 * @param c1 The first control point
	 * @param c2 The second control point
	 * @param p2 The end of the curve
	 */
	public Curve(Vector2f p1, Vector2f c1, Vector2f c2, Vector2f p2) {
		this(p1,c1,c2,p2,20);
	}

	/**
	 * Create a new curve 
	 * 
	 * @param p1 The start of the curve
	 * @param c1 The first control point
	 * @param c2 The second control point
	 * @param p2 The end of the curve
	 * @param segments The number of segments to use
	 */
	public Curve(Vector2f p1, Vector2f c1, Vector2f c2, Vector2f p2, int segments) {
		this.p1 = new Vector2f(p1);
		this.c1 = new Vector2f(c1);
		this.c2 = new Vector2f(c2);
		this.p2 = new Vector2f(p2);
	
		this.segments = segments;
		pointsDirty = true;
	}
	
	/**
	 * Get the point at a particular location on the curve
	 * 
	 * @param t A value between 0 and 1 defining the location of the curve the point is at
	 * @return The point on the curve
	 */
	public Vector2f pointAt(float t) {
		float a = 1 - t;
		float b = t;
		
		float f1 = a * a * a;
		float f2 = 3 * a * a * b;
		float f3 = 3 * a * b * b;
		float f4 = b * b * b;
		
		float nx = (p1.x * f1) + (c1.x * f2) + (c2.x * f3) + (p2.x * f4);
		float ny = (p1.y * f1) + (c1.y * f2) + (c2.y * f3) + (p2.y * f4);
		
		return new Vector2f(nx,ny);
	}

	/**
	 * @see org.newdawn.slick.geom.Shape#createPoints()
	 */
	protected void createPoints() {
		float step = 1.0f / segments;
		points = new float[(segments+1) * 2];
		for (int i=0;i<segments+1;i++) {
			float t = i * step;
		
			Vector2f p = pointAt(t);
			points[i*2] = p.x;
			points[(i*2)+1] = p.y;
		}
	}
	
	/**
	 * @see org.newdawn.slick.geom.Shape#transform(org.newdawn.slick.geom.Transform)
	 */
	public Shape transform(Transform transform) {
		float[] pts = new float[8];
		float[] dest = new float[8];
		pts[0] = p1.x; pts[1] = p1.y;
		pts[2] = c1.x; pts[3] = c1.y;
		pts[4] = c2.x; pts[5] = c2.y;
		pts[6] = p2.x; pts[7] = p2.y;
		transform.transform(pts, 0, dest, 0, 4);
		
		return new Curve(new Vector2f(dest[0],dest[1]), new Vector2f(dest[2],dest[3]),
						 new Vector2f(dest[4],dest[5]), new Vector2f(dest[6],dest[7]));
	}
	
    /**
     * True if this is a closed shape
     * 
     * @return True if this is a closed shape
     */
    public boolean closed() {
    	return false;
    }
}
