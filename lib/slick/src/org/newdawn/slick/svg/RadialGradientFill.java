package org.newdawn.slick.svg;

import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.geom.TexCoordGenerator;
import org.newdawn.slick.geom.Transform;
import org.newdawn.slick.geom.Vector2f;

/**
 * A filler to apply a SVG radial gradient across a shape
 * 
 * @author kevin
 */
public class RadialGradientFill implements TexCoordGenerator {
	/** The centre of the gradient */
	private Vector2f centre;
	/** The radius before the gradient is complete */
	private float radius;
	/** The gradient to apply */
	private Gradient gradient;
	/** The shape being filled */
	private Shape shape;
	
	/**
	 * Create a new fill for a radial gradient
	 * 
	 * @param shape The shape being filled
	 * @param trans The transform given for the shape in the SVG
	 * @param gradient The gradient to apply across the shape
	 */
	public RadialGradientFill(Shape shape, Transform trans, Gradient gradient) {
		this.gradient = gradient;

		radius = gradient.getR();
		float x = gradient.getX1();
		float y = gradient.getY1();
		
		float[] c = new float[] {x,y};
		gradient.getTransform().transform(c, 0, c, 0, 1);
		trans.transform(c, 0, c, 0, 1);
		float[] rt = new float[] {x,y-radius};
		gradient.getTransform().transform(rt, 0, rt, 0, 1);
		trans.transform(rt, 0, rt, 0, 1);
		
		centre = new Vector2f(c[0],c[1]);
		Vector2f dis = new Vector2f(rt[0],rt[1]);
		radius = dis.distance(centre);
	}

	/**
	 * @see org.newdawn.slick.geom.TexCoordGenerator#getCoordFor(float, float)
	 */
	public Vector2f getCoordFor(float x, float y) {
		float u = centre.distance(new Vector2f(x,y));
		u /= radius;
		
		if (u > 0.99f) {
			u = 0.99f;
		}
		
		return new Vector2f(u,0);
	}
	
}
