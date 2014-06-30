package org.newdawn.slick.geom;

import java.util.ArrayList;

/**
 * A shape built from lines and curves. Hole support is present but 
 * restricted.
 *
 * @author kevin
 */
public class Path extends Shape {
	/** The local list of points */
	private ArrayList localPoints = new ArrayList();
	/** The current x coordinate */
	private float cx;
	/** The current y coordiante */
	private float cy;
	/** True if the path has been closed */
	private boolean closed;
	/** The list of holes placed */
	private ArrayList holes = new ArrayList();
	/** The current hole being built */
	private ArrayList hole;
	
	/**
	 * Create a new path
	 * 
	 * @param sx The start x coordinate of the path
 	 * @param sy The start y coordiante of the path
	 */
	public Path(float sx, float sy) {
		localPoints.add(new float[] {sx,sy});
		cx = sx;
		cy = sy;
		pointsDirty = true;
	}
	
	/**
	 * Start building a hole in the previously defined contour
	 * 
	 * @param sx The start point of the hole
	 * @param sy The start point of the hole
	 */
	public void startHole(float sx, float sy) {
		hole = new ArrayList();
		holes.add(hole);
	}
	
	/**
	 * Add a line to the contour or hole which ends at the specified 
	 * location.
	 * 
	 * @param x The x coordinate to draw the line to
	 * @param y The y coordiante to draw the line to
	 */
	public void lineTo(float x, float y) {
		if (hole != null) {
			hole.add(new float[] {x,y});
		} else {
			localPoints.add(new float[] {x,y});
		}
		cx = x;
		cy = y;
		pointsDirty = true;
	}
	
	/**
	 * Close the path to form a polygon
	 */
	public void close() {
		closed = true;
	}
	
	/**
	 * Add a curve to the specified location (using the default segments 10)
	 * 
	 * @param x The destination x coordinate
	 * @param y The destination y coordiante
	 * @param cx1 The x coordiante of the first control point
	 * @param cy1 The y coordiante of the first control point
	 * @param cx2 The x coordinate of the second control point
	 * @param cy2 The y coordinate of the second control point
	 */
	public void curveTo(float x, float y, float cx1, float cy1, float cx2, float cy2) {
		curveTo(x,y,cx1,cy1,cx2,cy2,10);
	}
	
	/**
	 * Add a curve to the specified location (specifing the number of segments)
	 * 
	 * @param x The destination x coordinate
	 * @param y The destination y coordiante
	 * @param cx1 The x coordiante of the first control point
	 * @param cy1 The y coordiante of the first control point
	 * @param cx2 The x coordinate of the second control point
	 * @param cy2 The y coordinate of the second control point
	 * @param segments The number of segments to use for the new curve
	 */
	public void curveTo(float x, float y, float cx1, float cy1, float cx2, float cy2, int segments) {
		// special case for zero movement
		if ((cx == x) && (cy == y)) {
			return;
		}
		
		Curve curve = new Curve(new Vector2f(cx,cy),new Vector2f(cx1,cy1),new Vector2f(cx2,cy2),new Vector2f(x,y));
		float step = 1.0f / segments;
		
		for (int i=1;i<segments+1;i++) {
			float t = i * step;
			Vector2f p = curve.pointAt(t);
			if (hole != null) {
				hole.add(new float[] {p.x,p.y});
			} else {
				localPoints.add(new float[] {p.x,p.y});
			}
			cx = p.x;
			cy = p.y;
		}
		pointsDirty = true;
	}

	/**
	 * @see org.newdawn.slick.geom.Shape#createPoints()
	 */
	protected void createPoints() {
		points = new float[localPoints.size() * 2];
		for (int i=0;i<localPoints.size();i++) {
			float[] p = (float[]) localPoints.get(i);
			points[(i*2)] = p[0];
			points[(i*2)+1] = p[1];
		}
	}

	/**
	 * @see org.newdawn.slick.geom.Shape#transform(org.newdawn.slick.geom.Transform)
	 */
	public Shape transform(Transform transform) {
		Path p = new Path(cx,cy);
		p.localPoints = transform(localPoints, transform);
		for (int i=0;i<holes.size();i++) {
			p.holes.add(transform((ArrayList) holes.get(i), transform));
		}
		p.closed = this.closed;
		
		return p;
	}

	/**
	 * Transform a list of points
	 * 
	 * @param pts The pts to transform
	 * @param t The transform to apply
	 * @return The transformed points
	 */
	private ArrayList transform(ArrayList pts, Transform t) {
		float[] in = new float[pts.size()*2];
		float[] out = new float[pts.size()*2];
	
		for (int i=0;i<pts.size();i++) {
			in[i*2] = ((float[]) pts.get(i))[0];
			in[(i*2)+1] = ((float[]) pts.get(i))[1];
		}
		t.transform(in, 0, out, 0, pts.size());
		
		ArrayList outList = new ArrayList();
		for (int i=0;i<pts.size();i++) {
			outList.add(new float[] {out[(i*2)],out[(i*2)+1]});
		}
		
		return outList;
	}
	
//    /**
//     * Calculate the triangles that can fill this shape
//     */
//    protected void calculateTriangles() {
//    	if (!trianglesDirty) {
//    		return;
//    	}
//    	if (points.length >= 6) {
//    		boolean clockwise = true;
//    		float area = 0;
//    		for (int i=0;i<(points.length/2)-1;i++) {
//    			float x1 = points[(i*2)];
//    			float y1 = points[(i*2)+1];
//    			float x2 = points[(i*2)+2];
//    			float y2 = points[(i*2)+3];
//    			
//    			area += (x1 * y2) - (y1 * x2);
//    		}
//    		area /= 2;
//    		clockwise = area > 0;
//
//    		if (clockwise) {
//	        	tris = new MannTriangulator();
//	    		for (int i=0;i<points.length;i+=2) {
//		    		tris.addPolyPoint(points[i], points[i+1]);
//		    	}
//	    		
//	    		for (int h=0;h<holes.size();h++) {
//	    			ArrayList hole = (ArrayList) holes.get(h);
//					tris.startHole();
//	    			for (int i=0;i<hole.size();i++) {
//	    				float[] pt = (float[]) hole.get(i);
//	    				tris.addPolyPoint(pt[0],pt[1]);
//	    			}
//	    		}
//	    		tris.triangulate();
//    		} else {
//            	tris = new MannTriangulator();
//    	    	for (int i=points.length-2;i>=0;i-=2) {
//    	    		tris.addPolyPoint(points[i], points[i+1]);
//    	    	}
//    	    	
//        		for (int h=0;h<holes.size();h++) {
//        			ArrayList hole = (ArrayList) holes.get(h);
//    				tris.startHole();
//        			for (int i=hole.size()-1;i>=0;i--) {
//        				float[] pt = (float[]) hole.get(i);
//        				tris.addPolyPoint(pt[0],pt[1]);
//        			}
//        		}
//        		tris.triangulate();
//    		}
//    		
//    	} else {
//    		tris.triangulate();
//    	}
//    	
//    	trianglesDirty = false;
//    }
    
    /**
     * True if this is a closed shape
     * 
     * @return True if this is a closed shape
     */
    public boolean closed() {
    	return closed;
    }
}
