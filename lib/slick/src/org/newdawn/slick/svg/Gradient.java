package org.newdawn.slick.svg;

import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.ImageBuffer;
import org.newdawn.slick.geom.Transform;

/**
 * A gradient definition from an SVG file, includes the stops, name and transform.
 * 
 * @author kevin
 */
public class Gradient {
	/** The name/id given to the gradient */
	private String name;
	/** The steps in colour of the gradient */
	private ArrayList steps = new ArrayList();
	/** The first x coordiante given in the gradient (cx in radial) */
	private float x1;
	/** The second x coordiante given in the gradient (fx in radial) */
	private float x2;
	/** The first y coordiante given in the gradient (cy in radial) */
	private float y1;
	/** The first y coordiante given in the gradient (fy in radial) */
	private float y2;
	/** The radius given if any */
	private float r;
	/** The texture representing this gradient */
	private Image image;
	/** True if this gradient is radial in nature */
	private boolean radial;
	/** The transform specified for the gradient */
	private Transform transform;
	/** The name of the referenced gradient */
	private String ref;
	
	/**
	 * Create a new gradient definition
	 * 
	 * @param name The name of the gradient
	 * @param radial True if the gradient is radial
	 */
	public Gradient(String name, boolean radial) {
		this.name = name;
		this.radial = radial;
	}
	
	/**
	 * Check if the gradient is radial
	 * 
	 * @return True if the gradient is radial
	 */
	public boolean isRadial() {
		return radial;
	}
	
	/**
	 * Set the transform given for this definition
	 * 
	 * @param trans The transform given for this definition
	 */
	public void setTransform(Transform trans) {
		this.transform = trans;
	}
	
	/**
	 * Get the transform to apply during this gradient application
	 * 
	 * @return The transform given for this gradient
	 */
	public Transform getTransform() {
		return transform;
	}
	
	/**
	 * Reference another gradient, i.e. use it's colour stops
	 * 
	 * @param ref The name of the other gradient to reference
	 */
	public void reference(String ref) {
		this.ref = ref;
	}
	
	/**
	 * Resolve the gradient reference
	 * 
	 * @param diagram The diagram to resolve against
	 */
	public void resolve(Diagram diagram) {
		if (ref == null) {
			return;
		}
		
		Gradient other = diagram.getGradient(ref);
		
		for (int i=0;i<other.steps.size();i++) {
			steps.add(other.steps.get(i));
		}
	}
	
	/**
	 * Generate the image used for texturing the gradient across shapes
	 */
	public void genImage() {
		if (image == null) {
			ImageBuffer buffer = new ImageBuffer(128,16);
			for (int i=0;i<128;i++) {
				Color col = getColorAt(i / 128.0f);
				for (int j=0;j<16;j++) {
					buffer.setRGBA(i, j, col.getRedByte(), col.getGreenByte(), col.getBlueByte(), col.getAlphaByte());
				}
			}
			image = buffer.getImage();
		}
	}
	
	/**
	 * Get the image generated for this gradient
	 * 
	 * @return The image generated for the gradient
	 */
	public Image getImage() {
		genImage();
		
		return image;
	}

	/**
	 * Set the radius given in the SVG
	 * 
	 * @param r The radius for radial gradients
	 */
	public void setR(float r) {
		this.r = r;
	}
	
	/**
	 * Set the first x value given for the gradient (cx in the case of radial)
	 * 
	 * @param x1 The first x value given for the gradient
	 */
	public void setX1(float x1) {
		this.x1 = x1;
	}

	/**
	 * Set the second x value given for the gradient (fx in the case of radial)
	 * 
	 * @param x2 The second x value given for the gradient
	 */
	public void setX2(float x2) {
		this.x2 = x2;
	}

	/**
	 * Set the first y value given for the gradient (cy in the case of radial)
	 * 
	 * @param y1 The first y value given for the gradient
	 */
	public void setY1(float y1) {
		this.y1 = y1;
	}

	/**
	 * Set the second y value given for the gradient (fy in the case of radial)
	 * 
	 * @param y2 The second y value given for the gradient
	 */
	public void setY2(float y2) {
		this.y2 = y2;
	}

	/**
	 * Get the radius value given for this gradient
	 * 
	 * @return The radius value given for this gradient
	 */
	public float getR() {
		return r;
	}
	
	/**
	 * Get the first x value given for this gradient (cx in the case of radial)
	 * 
	 * @return The first x value given for this gradient
	 */
	public float getX1() {
		return x1;
	}

	/**
	 * Get the second x value given for this gradient (fx in the case of radial)
	 * 
	 * @return The second x value given for this gradient
	 */
	public float getX2() {
		return x2;
	}

	/**
	 * Get the first y value given for this gradient (cy in the case of radial)
	 * 
	 * @return The first y value given for this gradient
	 */
	public float getY1() {
		return y1;
	}

	/**
	 * Get the second y value given for this gradient (fy in the case of radial)
	 * 
	 * @return The second y value given for this gradient
	 */
	public float getY2() {
		return y2;
	}
	
	/**
	 * Add a colour step/stop to the gradient
	 * 
	 * @param location The location on the gradient the colour affects
	 * @param c The color to apply
	 */
	public void addStep(float location, Color c) {
		steps.add(new Step(location, c));
	}
	
	/**
	 * Get the intepolated colour at the given location on the gradient
	 * 
	 * @param p The point of the gradient (0 >= n >= 1)
	 * @return The interpolated colour at the given location
	 */
	public Color getColorAt(float p) {
		if (p <= 0) {
			return ((Step) steps.get(0)).col;
		}
		if (p > 1) {
			return ((Step) steps.get(steps.size()-1)).col;
		}
		
		for (int i=1;i<steps.size();i++) {
			Step prev = ((Step) steps.get(i-1));
			Step current = ((Step) steps.get(i));
			
			if (p <= current.location) {
				float dis = current.location - prev.location;
				p -= prev.location;
				float v = p / dis;
				
				Color c = new Color(1,1,1,1);
				c.a = (prev.col.a * (1 - v)) + (current.col.a * (v));
				c.r = (prev.col.r * (1 - v)) + (current.col.r * (v));
				c.g = (prev.col.g * (1 - v)) + (current.col.g * (v));
				c.b = (prev.col.b * (1 - v)) + (current.col.b * (v));
				
				return c;
			}
		}

		// shouldn't ever happen
		return Color.black;
	}
	
	/**
	 * The description of a single step on the gradient
	 * 
	 * @author kevin
	 */
	private class Step {
		/** The location on the gradient */
		float location;
		/** The colour applied */
		Color col;
		
		/**
		 * Create a new step
		 * 
		 * @param location The location on the gradient the colour affects
		 * @param c The colour to apply
		 */
		public Step(float location, Color c) {
			this.location = location;
			this.col = c;
		}
	}
}
