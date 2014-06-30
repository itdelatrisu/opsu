/*
 * Triangulator0.java
 *
 * (BSD license)
 *
 * Copyright (c) 2005, Matthias Mann (www.matthiasmann.de)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the Matthias Mann nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created on 17. March 2005, 22:19
 */
package org.newdawn.slick.geom;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * A 2D Triangulator. Graciously made available by the man(n) himself.
 * 
 * @author Matthias Mann
 */
public class MannTriangulator implements Triangulator {
	/** The allowed error value */
	private static final double EPSILON = 1e-5;
	
	/** The outer countour of the shape */
	protected PointBag contour;
	/** The holes defined in the polygon */
	protected PointBag holes;
	/** The next available point bag */
	private PointBag nextFreePointBag;
	/** The next available point */
	private Point nextFreePoint;
	/** The list of triangles created (or rather points in triangles, 3xn) */
	private List triangles = new ArrayList();
	
	/** Creates a new instance of Triangulator0 */
	public MannTriangulator() {
		contour = getPointBag();
	}
	
	/**
	 * @see org.newdawn.slick.geom.Triangulator#addPolyPoint(float, float)
	 */
	public void addPolyPoint(float x, float y) {
    	addPoint(new Vector2f(x,y));
    }

	/**
	 * Reset the internal state of the triangulator
	 */
	public void reset() {
		while (holes != null) {
			holes = freePointBag(holes);
		}

		contour.clear();
		holes = null;
	}

	/**
	 * Begin adding a hole to the polygon
	 */
	public void startHole() {
		PointBag newHole = getPointBag();
		newHole.next = holes;
		holes = newHole;
	}

	/**
	 * Add a defined point to the current contour
	 * 
	 * @param pt The point to add
	 */
	private void addPoint(Vector2f pt) {
		if (holes == null) {
			Point p = getPoint(pt);
			contour.add(p);
		} else {
			Point p = getPoint(pt);
			holes.add(p);
		}
	}

	/**
	 * Triangulate the points given
	 * 
	 * @param result The array to fill with the result or use to determine type
	 * @return The resultng triangles
	 */
	private Vector2f[] triangulate(Vector2f[] result) {
		// Step 1: Compute all angles
		contour.computeAngles();
		for (PointBag hole = holes; hole != null; hole = hole.next) {
			hole.computeAngles();
		}

		// Step 2: Connect the holes with the contour (build bridges)
		while (holes != null) {
			Point pHole = holes.first;
			outer: do {
				if (pHole.angle <= 0) {
					Point pContour = contour.first;
					do {
						inner: if (pHole.isInfront(pContour)
								&& pContour.isInfront(pHole)) {
							if (!contour.doesIntersectSegment(pHole.pt,
									pContour.pt)) {
								PointBag hole = holes;
								do {
									if (hole.doesIntersectSegment(pHole.pt,
											pContour.pt)) {
										break inner;
									}
								} while ((hole = hole.next) != null);

								Point newPtContour = getPoint(pContour.pt);
								pContour.insertAfter(newPtContour);

								Point newPtHole = getPoint(pHole.pt);
								pHole.insertBefore(newPtHole);

								pContour.next = pHole;
								pHole.prev = pContour;

								newPtHole.next = newPtContour;
								newPtContour.prev = newPtHole;

								pContour.computeAngle();
								pHole.computeAngle();
								newPtContour.computeAngle();
								newPtHole.computeAngle();

								// detach the points from the hole
								holes.first = null;
								break outer;
							}
						}
					} while ((pContour = pContour.next) != contour.first);
				}
			} while ((pHole = pHole.next) != holes.first);

			// free the hole
			holes = freePointBag(holes);
		}

		// Step 3: Make sure we have enough space for the result
		int numTriangles = contour.countPoints() - 2;
		int neededSpace = numTriangles * 3 + 1; // for the null
		if (result.length < neededSpace) {
			result = (Vector2f[]) Array.newInstance(result.getClass()
					.getComponentType(), neededSpace);
		}

		// Step 4: Extract the triangles
		int idx = 0;
		for (;;) {
			Point pContour = contour.first;

			if (pContour == null) {
				break;
			}
			// Are there 2 or less points left ?
			if (pContour.next == pContour.prev) {
				break;
			}

			outer: do {
				if (pContour.angle > 0) {
					Point prev = pContour.prev;
					Point next = pContour.next;

					if (next.next == prev || prev.isInfront(next) && next.isInfront(prev)) {
						if (!contour.doesIntersectSegment(prev.pt, next.pt)) {
							result[idx++] = pContour.pt;
							result[idx++] = next.pt;
							result[idx++] = prev.pt;
							break outer;
						}
					}
				}
			} while ((pContour = pContour.next) != contour.first);
			
			// remove the point - we do it in every case to prevent endless loop
			Point prev = pContour.prev;
			Point next = pContour.next;

			contour.first = prev;
			pContour.unlink();
			freePoint(pContour);

			next.computeAngle();
			prev.computeAngle();
		}

		// Step 5: Append a null (see Collection.toArray)
		result[idx] = null;

		// Step 6: Free memory
		contour.clear();

		// Finished !
		return result;
	}

	/**
	 * Create a new point bag (or recycle an old one)
	 * 
	 * @return The new point bag
	 */
	private PointBag getPointBag() {
		PointBag pb = nextFreePointBag;
		if (pb != null) {
			nextFreePointBag = pb.next;
			pb.next = null;
			return pb;
		}
		return new PointBag();
	}

	/**
	 * Release a pooled bag
	 * 
	 * @param pb The bag to release
	 * @return The next available bag
	 */
	private PointBag freePointBag(PointBag pb) {
		PointBag next = pb.next;
		pb.clear();
		pb.next = nextFreePointBag;
		nextFreePointBag = pb;
		return next;
	}

	/**
	 * Create or reuse a point
	 * 
	 * @param pt The point data to set
	 * @return The new point
	 */
	private Point getPoint(Vector2f pt) {
		Point p = nextFreePoint;
		if (p != null) {
			nextFreePoint = p.next;
			// initialize new point to safe values
			p.next = null;
			p.prev = null;
			p.pt = pt;
			return p;
		}
		return new Point(pt);
	}

	/**
	 * Release a point into the pool
	 * 
	 * @param p The point to release
	 */
	private void freePoint(Point p) {
		p.next = nextFreePoint;
		nextFreePoint = p;
	}

	/**
	 * Release all points
	 * 
	 * @param head The head of the points bag
	 */
	private void freePoints(Point head) {
		head.prev.next = nextFreePoint;
		head.prev = null;
		nextFreePoint = head;
	}

	/**
	 * A single point being considered during triangulation
	 *
	 * @author Matthias Mann
	 */
	private static class Point implements Serializable {
		/** The location of the point */
		protected Vector2f pt;
		/** The previous point in the contour */
		protected Point prev;
		/** The next point in the contour */
		protected Point next;
		/** The x component of the of the normal */
		protected double nx;
		/** The y component of the of the normal */
		protected double ny;
		/** The angle at this point in the path */
		protected double angle;
		/** The distance of this point from */
		protected double dist;

		/**
		 * Create a new point
		 * 
		 * @param pt The points location
		 */
		public Point(Vector2f pt) {
			this.pt = pt;
		}

		/**
		 * Remove this point from it's contour
		 */
		public void unlink() {
			prev.next = next;
			next.prev = prev;
			next = null;
			prev = null;
		}

		/**
		 * Insert a point before this one (see LinkedList)
		 * 
		 * @param p The point to insert
		 */
		public void insertBefore(Point p) {
			prev.next = p;
			p.prev = prev;
			p.next = this;
			prev = p;
		}

		/**
		 * Insert a point after this one (see LinkedList)
		 * 
		 * @param p The point to insert
		 */
		public void insertAfter(Point p) {
			next.prev = p;
			p.prev = this;
			p.next = next;
			next = p;
		}

		/**
		 * Java 5 hypot method
		 * 
		 * @param x The x component
		 * @param y The y component
		 * @return The hypotenuse
		 */
		private double hypot(double x, double y) {
			return Math.sqrt(x*x + y*y);
		}
		
		/**
		 * Compute the angle at this point
		 */
		public void computeAngle() {
			if (prev.pt.equals(pt)) {
				pt.x += 0.01f;
			}
			double dx1 = pt.x - prev.pt.x;
			double dy1 = pt.y - prev.pt.y;
			double len1 = hypot(dx1, dy1);
			dx1 /= len1;
			dy1 /= len1;

			if (next.pt.equals(pt)) {
				pt.y += 0.01f;
			}
			double dx2 = next.pt.x - pt.x;
			double dy2 = next.pt.y - pt.y;
			double len2 = hypot(dx2, dy2);
			dx2 /= len2;
			dy2 /= len2;

			double nx1 = -dy1;
			double ny1 = dx1;

			nx = (nx1 - dy2) * 0.5;
			ny = (ny1 + dx2) * 0.5;

			if (nx * nx + ny * ny < EPSILON) {
				nx = dx1;
				ny = dy2;
				angle = 1; // TODO: nx1,ny1 and nx2,ny2 facing ?
				if (dx1 * dx2 + dy1 * dy2 > 0) {
					nx = -dx1;
					ny = -dy1;
				}
			} else {
				angle = nx * dx2 + ny * dy2;
			}
		}

		/**
		 * Get the angle of this point to another
		 * 
		 * @param p The other point
		 * @return The angle between this point and another
		 */
		public double getAngle(Point p) {
			double dx = p.pt.x - pt.x;
			double dy = p.pt.y - pt.y;
			double dlen = hypot(dx, dy);

			return (nx * dx + ny * dy) / dlen;
		}

		/**
		 * Check if this point is convave
		 * 
		 * @return True if this point remains concave
		 */
		public boolean isConcave() {
			return angle < 0;
		}

		/**
		 * Check if this point is infront of another
		 * 
		 * @param dx The other x
		 * @param dy The other y
		 * @return True if this point is infront (in the contour)
		 */
		public boolean isInfront(double dx, double dy) {
			// no nead to normalize, amplitude does not metter for side
			// detection
			boolean sidePrev = ((prev.pt.y - pt.y) * dx + (pt.x - prev.pt.x)
					* dy) >= 0;
			boolean sideNext = ((pt.y - next.pt.y) * dx + (next.pt.x - pt.x)
					* dy) >= 0;

			return (angle < 0) ? (sidePrev | sideNext) : (sidePrev & sideNext);
		}

		/**
		 * Check if this point is infront of another
		 * 
		 * @param p The other point
		 * @return True if this point is infront (in the contour)
		 */
		public boolean isInfront(Point p) {
			return isInfront(p.pt.x - pt.x, p.pt.y - pt.y);
		}
	}

	/**
	 * A bag/pool of point objects
	 *
	 * @author kevin
	 */
	protected class PointBag implements Serializable {
		/** The first point in the bag - head of the list */
		protected Point first;
		/** The next bag in the list of bags */
		protected PointBag next;

		/**
		 * Clear all the points from this bag
		 */
		public void clear() {
			if (first != null) {
				freePoints(first);
				first = null;
			}
		}

		/**
		 * Add a point to the bag
		 * 
		 * @param p The point to add
		 */
		public void add(Point p) {
			if (first != null) {
				first.insertBefore(p);
			} else {
				first = p;
				p.next = p;
				p.prev = p;
			}
		}

		/**
		 * Compute the angles for the points in this bag
		 */
		public void computeAngles() {
			if (first == null) {
				return;
			}

			Point p = first;
			do {
				p.computeAngle();
			} while ((p = p.next) != first);
		}

		/**
		 * Check if the points in this bag form a path intersecting
		 * with the specified path
		 * 
		 * @param v1 The start point of the segment
		 * @param v2 The end point of the segment
		 * @return True if points in this contour intersect with the segment
		 */
		public boolean doesIntersectSegment(Vector2f v1, Vector2f v2) {
			double dxA = v2.x - v1.x;
			double dyA = v2.y - v1.y;

			for (Point p = first;;) {
				Point n = p.next;
				if (p.pt != v1 && n.pt != v1 && p.pt != v2 && n.pt != v2) {
					double dxB = n.pt.x - p.pt.x;
					double dyB = n.pt.y - p.pt.y;
					double d = (dxA * dyB) - (dyA * dxB);

					if (Math.abs(d) > EPSILON) {
						double tmp1 = p.pt.x - v1.x;
						double tmp2 = p.pt.y - v1.y;
						double tA = (dyB * tmp1 - dxB * tmp2) / d;
						double tB = (dyA * tmp1 - dxA * tmp2) / d;

						if (tA >= 0 && tA <= 1 && tB >= 0 && tB <= 1) {
							return true;
						}
					}
				}

				if (n == first) {
					return false;
				}
				p = n;
			}
		}

		/**
		 * Get the number of points in the bag 
		 * 
		 * @return The number of points in the bag
		 */
		public int countPoints() {
			if (first == null) {
				return 0;
			}

			int count = 0;
			Point p = first;
			do {
				++count;
			} while ((p = p.next) != first);
			return count;
		}
		
		/**
		 * Check if the point provided was contained
		 * 
		 * @param point The point provided
		 * @return True if it's in the bag
		 */
		public boolean contains(Vector2f point) {
			if (first == null) {
				return false;
			}
			
			if (first.prev.pt.equals(point)) {
				return true;
			}
			if (first.pt.equals(point)) {
				return true;
			}
			return false;
		}
	}

	public boolean triangulate() {
		Vector2f[] temp = triangulate(new Vector2f[0]);

		for (int i = 0; i < temp.length; i++) {
			if (temp[i] == null) {
				break;
			} else {
				triangles.add(temp[i]);
			}
		}

		return true;
	}

	/**
	 * @see org.newdawn.slick.geom.Triangulator#getTriangleCount()
	 */
	public int getTriangleCount() {
		return triangles.size() / 3;
	}

	/**
	 * @see org.newdawn.slick.geom.Triangulator#getTrianglePoint(int, int)
	 */
	public float[] getTrianglePoint(int tri, int i) {
		Vector2f pt = (Vector2f) triangles.get((tri * 3) + i);

		return new float[] { pt.x, pt.y };
	}

}
