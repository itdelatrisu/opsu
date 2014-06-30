package org.newdawn.slick.geom;

import java.util.ArrayList;

/**
 * Triangulates a polygon into triangles - duh. Doesn't handle
 * holes in polys
 * 
 * @author Based on Public Source from FlipCode
 */
public class BasicTriangulator implements Triangulator {
	/** The accepted error value */
	private static final float EPSILON = 0.0000000001f;
	/** The list of points to be triangulated */
	private PointList poly = new PointList();
	/** The list of points describing the triangles */
	private PointList tris = new PointList();
	/** True if we've tried to triangulate */
	private boolean tried;
	
	/**
	 * Create a new triangulator
	 */
	public BasicTriangulator() {
	}
	
	/**
	 * Add a point describing the polygon to be triangulated
	 * 
	 * @param x The x coordinate of the point
	 * @param y the y coordinate of the point
	 */
	public void addPolyPoint(float x, float y) {
		Point p = new Point(x,y);
		if (!poly.contains(p)) {
			poly.add(p);
		}
	}
	
	/**
	 * Get the number of points in the polygon
	 * 
	 * @return The number of points in the polygon
	 */
	public int getPolyPointCount() {
		return poly.size();
	}

	/**
	 * Get the coordinates of the point at the specified index
	 * 
	 * @param index The index of the point to retrieve
	 * @return The oordinates of the point at the specified index
	 */
	public float[] getPolyPoint(int index) {
		return new float[] {poly.get(index).x,poly.get(index).y};
	}
	
	/**
	 * Cause the triangulator to split the polygon
	 * 
	 * @return True if we managed the task
	 */
	public boolean triangulate() {
		tried = true;
		
		boolean worked = process(poly,tris);
		return worked;
	}
	
	/**
	 * Get a count of the number of triangles produced
	 * 
	 * @return The number of triangles produced
	 */
	public int getTriangleCount() {
		if (!tried) {
			throw new RuntimeException("Call triangulate() before accessing triangles");
		}
		return tris.size() / 3;
	}
	
	/**
	 * Get a point on a specified generated triangle
	 * 
	 * @param tri The index of the triangle to interegate
	 * @param i The index of the point within the triangle to retrieve
	 * (0 - 2)
	 * @return The x,y coordinate pair for the point
	 */
	public float[] getTrianglePoint(int tri, int i) {
		if (!tried) {
			throw new RuntimeException("Call triangulate() before accessing triangles");
		}
		
		return tris.get((tri*3)+i).toArray();
	}
	
	/** 
	 * Find the area of a polygon defined by the series of points
	 * in the list
	 * 
	 * @param contour The list of points defined the contour of the polygon
	 * (Vector2f)
	 * @return The area of the polygon defined
	 */
	private float area(PointList contour) {
		int n = contour.size();

		float A = 0.0f;

		for (int p = n - 1, q = 0; q < n; p = q++) {
			Point contourP = contour.get(p);
			Point contourQ = contour.get(q);

			A += contourP.getX() * contourQ.getY() - contourQ.getX()
					* contourP.getY();
		}
		return A * 0.5f;
	}

	/**
	 * Check if the point P is inside the triangle defined by
	 * the points A,B,C
	 * 
	 * @param Ax Point A x-coordinate
	 * @param Ay Point A y-coordinate
	 * @param Bx Point B x-coordinate
	 * @param By Point B y-coordinate
	 * @param Cx Point C x-coordinate
	 * @param Cy Point C y-coordinate
	 * @param Px Point P x-coordinate
	 * @param Py Point P y-coordinate
	 * @return True if the point specified is within the triangle
	 */
	private boolean insideTriangle(float Ax, float Ay, float Bx,
			float By, float Cx, float Cy, float Px, float Py) {
		float ax, ay, bx, by, cx, cy, apx, apy, bpx, bpy, cpx, cpy;
		float cCROSSap, bCROSScp, aCROSSbp;

		ax = Cx - Bx;
		ay = Cy - By;
		bx = Ax - Cx;
		by = Ay - Cy;
		cx = Bx - Ax;
		cy = By - Ay;
		apx = Px - Ax;
		apy = Py - Ay;
		bpx = Px - Bx;
		bpy = Py - By;
		cpx = Px - Cx;
		cpy = Py - Cy;

		aCROSSbp = ax * bpy - ay * bpx;
		cCROSSap = cx * apy - cy * apx;
		bCROSScp = bx * cpy - by * cpx;

		return ((aCROSSbp >= 0.0f) && (bCROSScp >= 0.0f) && (cCROSSap >= 0.0f));
	}

	/**
	 * Cut a the contour and add a triangle into V to describe the 
	 * location of the cut
	 * 
	 * @param contour The list of points defining the polygon
	 * @param u The index of the first point
	 * @param v The index of the second point
	 * @param w The index of the third point
	 * @param n ?
	 * @param V The array to populate with indicies of triangles
	 * @return True if a triangle was found
	 */
	private boolean snip(PointList contour, int u, int v, int w, int n,
			int[] V) {
		int p;
		float Ax, Ay, Bx, By, Cx, Cy, Px, Py;

		Ax = contour.get(V[u]).getX();
		Ay = contour.get(V[u]).getY();

		Bx = contour.get(V[v]).getX();
		By = contour.get(V[v]).getY();

		Cx = contour.get(V[w]).getX();
		Cy = contour.get(V[w]).getY();

		if (EPSILON > (((Bx - Ax) * (Cy - Ay)) - ((By - Ay) * (Cx - Ax)))) {
			return false;
		}

		for (p = 0; p < n; p++) {
			if ((p == u) || (p == v) || (p == w)) {
				continue;
			}

			Px = contour.get(V[p]).getX();
			Py = contour.get(V[p]).getY();

			if (insideTriangle(Ax, Ay, Bx, By, Cx, Cy, Px, Py)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Process a list of points defining a polygon
	 * @param contour The list of points describing the polygon
	 * @param result The list of points describing the triangles. Groups
	 * of 3 describe each triangle 
	 * 
	 * @return True if we succeeded in completing triangulation
	 */
	private boolean process(PointList contour, PointList result) {
		result.clear();
		
		/* allocate and initialize list of Vertices in polygon */

		int n = contour.size();
		if (n < 3)
			return false;

		int[] V = new int[n];

		/* we want a counter-clockwise polygon in V */

		if (0.0f < area(contour)) {
			for (int v = 0; v < n; v++)
				V[v] = v;
		} else {
			for (int v = 0; v < n; v++)
				V[v] = (n - 1) - v;
		}

		int nv = n;

		/*  remove nv-2 Vertices, creating 1 triangle every time */
		int count = 2 * nv; /* error detection */

		for (int m = 0, v = nv - 1; nv > 2;) {
			/* if we loop, it is probably a non-simple polygon */
			if (0 >= (count--)) {
				//** Triangulator4: ERROR - probable bad polygon!
				return false;
			}

			/* three consecutive vertices in current polygon, <u,v,w> */
			int u = v;
			if (nv <= u)
				u = 0; /* previous */
			v = u + 1;
			if (nv <= v)
				v = 0; /* new v    */
			int w = v + 1;
			if (nv <= w)
				w = 0; /* next     */

			if (snip(contour, u, v, w, nv, V)) {
				int a, b, c, s, t;

				/* true names of the vertices */
				a = V[u];
				b = V[v];
				c = V[w];

				/* output Triangle */
				result.add(contour.get(a));
				result.add(contour.get(b));
				result.add(contour.get(c));

				m++;

				/* remove v from remaining polygon */
				for (s = v, t = v + 1; t < nv; s++, t++) {
					V[s] = V[t];
				}
				nv--;

				/* resest error detection counter */
				count = 2 * nv;
			}
		}

		return true;
	}

	/**
	 * A single point handled by the triangulator
	 * 
	 * @author Kevin Glass
	 */
	private class Point {
		/** The x coorindate of this point */
		private float x;
		/** The y coorindate of this point */
		private float y;
		/** The points in an array */
		private float[] array;
		
		/**
		 * Create a new point
		 * 
		 * @param x The x coordindate of the point
		 * @param y The y coordindate of the point
		 */
		public Point(float x, float y) {
			this.x = x;
			this.y = y;
			array = new float[] {x,y};
		}

		/**
		 * Get the x coordinate of the point
		 * 
		 * @return The x coordinate of the point
		 */
		public float getX() {
			return x;
		}

		/**
		 * Get the y coordinate of the point
		 * 
		 * @return The y coordinate of the point
		 */
		public float getY() {
			return y;
		}
	
		/**
		 * Convert this point into a float array
		 * 
		 * @return The contents of this point as a float array
		 */
		public float[] toArray() {
			return array;
		}
		
		/**
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return (int) (x * y * 31);
		}
		
		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object other) {
			if (other instanceof Point) {
				Point p = (Point) other;
				return (p.x == x) && (p.y == y);
			}
			
			return false;
		}
	}
	
	/**
	 * A list of type <code>Point</code>
	 * 
	 * @author Kevin Glass
	 */
	private class PointList {
		/** The list of points */
		private ArrayList points = new ArrayList();
		
		/**
		 * Create a new empty list
		 */
		public PointList() {
		}
		
		/**
		 * Check if the list contains a point
		 * 
		 * @param p The point to look for
		 * @return True if the point is in the list
		 */
		public boolean contains(Point p) {
			return points.contains(p);
		}
		
		/**
		 * Add a point to the list 
		 * 
		 * @param point The point to add
		 */
		public void add(Point point) {
			points.add(point);
		}
		
		/**
		 * Remove a point from the list
		 * 
		 * @param point The point to remove
		 */
		public void remove(Point point) {
			points.remove(point);
		}
		
		/**
		 * Get the size of the list
		 * 
		 * @return The size of the list
		 */
		public int size() {
			return points.size();
		}
		
		/**
		 * Get a point a specific index in the list
		 * 
		 * @param i The index of the point to retrieve
		 * @return The point
		 */
		public Point get(int i) {
			return (Point) points.get(i);
		}
		
		/**
		 * Clear the list
		 */
		public void clear() {
			points.clear();
		}
	}

	/**
	 * @see org.newdawn.slick.geom.Triangulator#startHole()
	 */
	public void startHole() {
		// TODO Auto-generated method stub
		
	}
}
