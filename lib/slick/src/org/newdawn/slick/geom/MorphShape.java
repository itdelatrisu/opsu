package org.newdawn.slick.geom;

import java.util.ArrayList;

/**
 * A shape that morphs between a set of other shapes
 *  
 * @author kevin
 */
public class MorphShape extends Shape {
	/** The shapes to morph between */
	private ArrayList shapes = new ArrayList();
	/** The offset between the shapes */
	private float offset;
	
	/** The current shape */
	private Shape current;
	/** The next shape */
	private Shape next;
	
	/**
	 * Create a new mighty morphin shape
	 * 
	 * @param base The base shape we're starting the morph from
	 */
	public MorphShape(Shape base) {
		shapes.add(base);
		float[] copy = base.points;
		this.points = new float[copy.length];
		
		current = base;
		next = base;
	}

	/**
	 * Add a subsequent shape that we should morph too in order
	 * 
	 * @param shape The new shape that forms part of the morphing shape
	 */
	public void addShape(Shape shape) {
		if (shape.points.length != points.length) {
			throw new RuntimeException("Attempt to morph between two shapes with different vertex counts");
		}
		
		Shape prev = (Shape) shapes.get(shapes.size()-1);
		if (equalShapes(prev, shape)) {
			shapes.add(prev);
		} else {
			shapes.add(shape);
		}
		
		if (shapes.size() == 2) {
			next = (Shape) shapes.get(1);
		}
	}
	
	/**
	 * Check if the shape's points are all equal
	 * 
	 * @param a The first shape to compare
 	 * @param b The second shape to compare
	 * @return True if the shapes are equal
	 */
	private boolean equalShapes(Shape a, Shape b) {
		a.checkPoints();
		b.checkPoints();
		
		for (int i=0;i<a.points.length;i++) {
			if (a.points[i] != b.points[i]) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Set the "time" index for this morph. This is given in terms of shapes, so
	 * 0.5f would give you the position half way between the first and second shapes.
	 * 
	 * @param time The time index to represent on this shape
	 */
	public void setMorphTime(float time) {
		int p = (int) time;
		int n = p + 1;
		float offset = time - p;
		
		p = rational(p);
		n = rational(n);
		
		setFrame(p, n, offset);
	}
	
	/**
	 * Update the morph time and hence the curent frame
	 * 
	 * @param delta The amount to change the morph time by
	 */
	public void updateMorphTime(float delta) {
		offset += delta;
		if (offset < 0) {
			int index = shapes.indexOf(current);
			if (index < 0) {
				index = shapes.size() - 1;
			}
			
			int nframe = rational(index+1);
			setFrame(index, nframe, offset);
			offset += 1;
		} else if (offset > 1) {
			int index = shapes.indexOf(next);
			if (index < 1) {
				index = 0;
			}
			
			int nframe = rational(index+1);
			setFrame(index, nframe, offset);
			offset -= 1;
		} else {
			pointsDirty = true;
		}
	}
	
	/**
	 * Set the current frame
	 * 
	 * @param current The current frame
	 */
	public void setExternalFrame(Shape current) {
		this.current = current;
		next = (Shape) shapes.get(0);
		offset = 0;
	}
	
	/**
	 * Get an index that is rational, i.e. fits inside this set of shapes
	 * 
	 * @param n The index to rationalize
	 * @return The index rationalized
	 */
	private int rational(int n) {
		while (n >= shapes.size()) {
			n -= shapes.size();
		}
		while (n < 0) {
			n += shapes.size();
		}
		
		return n;
	}
	
	/**
	 * Set the frame to be represented 
	 * 
	 * @param a The index of the first shape
	 * @param b The index of the second shape
	 * @param offset The offset between the two shapes to represent
	 */
	private void setFrame(int a, int b, float offset) {
		current = (Shape) shapes.get(a);
		next = (Shape) shapes.get(b);
		this.offset = offset;
		pointsDirty = true;
	}
	
	/**
	 * @see MorphShape#createPoints()
	 */
	protected void createPoints() {
		if (current == next) {
			System.arraycopy(current.points,0,points,0,points.length);
			return;
		}
		
		float[] apoints = current.points;
		float[] bpoints = next.points;
		
		for (int i=0;i<points.length;i++) {
			points[i] = apoints[i] * (1 - offset);
			points[i] += bpoints[i] * offset;
		}
	}

	/**
	 * @see MorphShape#transform(Transform)
	 */
	public Shape transform(Transform transform) {
		createPoints();
		Polygon poly = new Polygon(points);
		
		return poly;
	}
}
