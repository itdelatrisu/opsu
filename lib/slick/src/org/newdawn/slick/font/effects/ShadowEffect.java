
package org.newdawn.slick.font.effects;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.Glyph;

/**
 * An effect to generate soft shadows beneath text 
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class ShadowEffect implements ConfigurableEffect {
	/** The number of kernels to apply */
	public static final int NUM_KERNELS = 16;
	/** The blur kernels applied across the effect */
	public static final float[][] GAUSSIAN_BLUR_KERNELS = generateGaussianBlurKernels(NUM_KERNELS);

	/** The colour of the shadow to render */
	private Color color = Color.black;
	/** The transparency factor of the shadow */
	private float opacity = 0.6f;
	/** The distance on the x axis of the shadow from the text */
	private float xDistance = 2;
	/** The distance on the y axis of the shadow from the text */
	private float yDistance = 2;
	/** The size of the kernel used to blur the shadow */
	private int blurKernelSize = 0;
	/** The number of passes applied to create the blur */
	private int blurPasses = 1;

	/**
	 * Default constructor for injection
	 */
	public ShadowEffect() {
	}

	/**
	 * Create a new effect to apply a drop shadow to text
	 * 
	 * @param color The colour of the shadow to generate
	 * @param xDistance The distance from the text on the x axis the shadow should be rendered
	 * @param yDistance The distance from the text on the y axis the shadow should be rendered
	 * @param opacity The transparency factor of the shadow
	 */
	public ShadowEffect (Color color, int xDistance, int yDistance, float opacity) {
		this.color = color;
		this.xDistance = xDistance;
		this.yDistance = yDistance;
		this.opacity = opacity;
	}

	/**
	 * @see org.newdawn.slick.font.effects.Effect#draw(java.awt.image.BufferedImage, java.awt.Graphics2D, org.newdawn.slick.UnicodeFont, org.newdawn.slick.font.Glyph)
	 */
	public void draw(BufferedImage image, Graphics2D g, UnicodeFont unicodeFont, Glyph glyph) {
		g = (Graphics2D)g.create();
		g.translate(xDistance, yDistance);
		g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(opacity * 255)));
		g.fill(glyph.getShape());

		// Also shadow the outline, if one exists.
		for (Iterator iter = unicodeFont.getEffects().iterator(); iter.hasNext();) {
			Effect effect = (Effect)iter.next();
			if (effect instanceof OutlineEffect) {
				Composite composite = g.getComposite();
				g.setComposite(AlphaComposite.Src); // Prevent shadow and outline shadow alpha from combining.

				g.setStroke(((OutlineEffect)effect).getStroke());
				g.draw(glyph.getShape());

				g.setComposite(composite);
				break;
			}
		}

		g.dispose();
		if (blurKernelSize > 1 && blurKernelSize < NUM_KERNELS && blurPasses > 0) blur(image);
	}

	/**
	 * Apply blurring to the generate image
	 * 
	 * @param image The image to be blurred
	 */
	private void blur(BufferedImage image) {
		float[] matrix = GAUSSIAN_BLUR_KERNELS[blurKernelSize - 1];
		Kernel gaussianBlur1 = new Kernel(matrix.length, 1, matrix);
		Kernel gaussianBlur2 = new Kernel(1, matrix.length, matrix);
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		ConvolveOp gaussianOp1 = new ConvolveOp(gaussianBlur1, ConvolveOp.EDGE_NO_OP, hints);
		ConvolveOp gaussianOp2 = new ConvolveOp(gaussianBlur2, ConvolveOp.EDGE_NO_OP, hints);
		BufferedImage scratchImage = EffectUtil.getScratchImage();
		for (int i = 0; i < blurPasses; i++) {
			gaussianOp1.filter(image, scratchImage);
			gaussianOp2.filter(scratchImage, image);
		}
	}

	/**
	 * Get the colour of the shadow generated
	 * 
	 * @return The colour of the shadow generated
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Set the colour of the shadow to be generated
	 * 
	 * @param color The colour ofthe shadow to be generated
	 */
	public void setColor(Color color) {
		this.color = color;
	}

	/**
	 * Get the distance on the X axis from the text the shadow should
	 * be generated at
	 * 
	 * @return The distance on the X axis the shadow will be from the text
	 */
	public float getXDistance() {
		return xDistance;
	}

	/**
	 * Sets the pixels to offset the shadow on the x axis. The glyphs will need padding so the 
	 * shadow doesn't get clipped.
	 * 
	 * @param distance The offset on the x axis
	 */
	public void setXDistance(float distance) {
		xDistance = distance;
	}

	/**
	 * Get the distance on the Y axis from the text the shadow should
	 * be generated at
	 * 
	 * @return The distance on the Y axis the shadow will be from the text
	 */
	public float getYDistance() {
		return yDistance;
	}

	/**
	 * Sets the pixels to offset the shadow on the y axis. The glyphs will need 
	 * padding so the shadow doesn't get clipped.
	 * 
	 * @param distance The offset on the y axis
	 */
	public void setYDistance (float distance) {
		yDistance = distance;
	}

	/**
	 * Get the size of the kernel used to apply the blur
	 * 
	 * @return The blur kernel size
	 */
	public int getBlurKernelSize() {
		return blurKernelSize;
	}

	/**
	 * Sets how many neighboring pixels are used to blur the shadow. Set to 0 for no blur.
	 * 
	 * @param blurKernelSize The size of the kernel to apply the blur with
	 */
	public void setBlurKernelSize (int blurKernelSize) {
		this.blurKernelSize = blurKernelSize;
	}

	/**
	 * Get the number of passes to apply the kernel for blurring
	 * 
	 * @return The number of passes
	 */
	public int getBlurPasses() {
		return blurPasses;
	}

	/**
	 * Sets the number of times to apply a blur to the shadow. Set to 0 for no blur.
	 * 
	 * @param blurPasses The number of passes to apply when blurring
	 */
	public void setBlurPasses (int blurPasses) {
		this.blurPasses = blurPasses;
	}

	/**
	 * Get the opacity of the shadow, i.e. how transparent it is
	 * 
	 * @return The opacity of the shadow
	 */
	public float getOpacity() {
		return opacity;
	}

	/**
	 * Set the opacity of the shadow, i.e. how transparent it is
	 * 
	 * @param opacity The opacity of the shadow
	 */
	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Shadow";
	}

	/**
	 * @see org.newdawn.slick.font.effects.ConfigurableEffect#getValues()
	 */
	public List getValues() {
		List values = new ArrayList();
		values.add(EffectUtil.colorValue("Color", color));
		values.add(EffectUtil.floatValue("Opacity", opacity, 0, 1, "This setting sets the translucency of the shadow."));
		values.add(EffectUtil.floatValue("X distance", xDistance, Float.MIN_VALUE, Float.MAX_VALUE, "This setting is the amount of pixels to offset the shadow on the"
			+ " x axis. The glyphs will need padding so the shadow doesn't get clipped."));
		values.add(EffectUtil.floatValue("Y distance", yDistance, Float.MIN_VALUE, Float.MAX_VALUE, "This setting is the amount of pixels to offset the shadow on the"
			+ " y axis. The glyphs will need padding so the shadow doesn't get clipped."));

		List options = new ArrayList();
		options.add(new String[] {"None", "0"});
		for (int i = 2; i < NUM_KERNELS; i++)
			options.add(new String[] {String.valueOf(i)});
		String[][] optionsArray = (String[][])options.toArray(new String[options.size()][]);
		values.add(EffectUtil.optionValue("Blur kernel size", String.valueOf(blurKernelSize), optionsArray,
			"This setting controls how many neighboring pixels are used to blur the shadow. Set to \"None\" for no blur."));

		values.add(EffectUtil.intValue("Blur passes", blurPasses,
			"The setting is the number of times to apply a blur to the shadow. Set to \"0\" for no blur."));
		return values;
	}

	/**
	 * @see org.newdawn.slick.font.effects.ConfigurableEffect#setValues(java.util.List)
	 */
	public void setValues(List values) {
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			Value value = (Value)iter.next();
			if (value.getName().equals("Color")) {
				color = (Color)value.getObject();
			} else if (value.getName().equals("Opacity")) {
				opacity = ((Float)value.getObject()).floatValue();
			} else if (value.getName().equals("X distance")) {
				xDistance = ((Float)value.getObject()).floatValue();
			} else if (value.getName().equals("Y distance")) {
				yDistance = ((Float)value.getObject()).floatValue();
			} else if (value.getName().equals("Blur kernel size")) {
				blurKernelSize = Integer.parseInt((String)value.getObject());
			} else if (value.getName().equals("Blur passes")) {
				blurPasses = ((Integer)value.getObject()).intValue();
			}
		}
	}

	/**
	 * Generate the blur kernels which will be repeatedly applied when blurring images
	 * 
	 * @param level The number of kernels to generate
	 * @return The kernels generated
	 */
	private static float[][] generateGaussianBlurKernels(int level) {
		float[][] pascalsTriangle = generatePascalsTriangle(level);
		float[][] gaussianTriangle = new float[pascalsTriangle.length][];
		for (int i = 0; i < gaussianTriangle.length; i++) {
			float total = 0.0f;
			gaussianTriangle[i] = new float[pascalsTriangle[i].length];
			for (int j = 0; j < pascalsTriangle[i].length; j++)
				total += pascalsTriangle[i][j];
			float coefficient = 1 / total;
			for (int j = 0; j < pascalsTriangle[i].length; j++)
				gaussianTriangle[i][j] = coefficient * pascalsTriangle[i][j];
		}
		return gaussianTriangle;
	}

	/**
	 * Generate Pascal's triangle
	 * 
	 * @param level The level of the triangle to generate
	 * @return The Pascal's triangle kernel
	 */
	private static float[][] generatePascalsTriangle(int level) {
		if (level < 2) level = 2;
		float[][] triangle = new float[level][];
		triangle[0] = new float[1];
		triangle[1] = new float[2];
		triangle[0][0] = 1.0f;
		triangle[1][0] = 1.0f;
		triangle[1][1] = 1.0f;
		for (int i = 2; i < level; i++) {
			triangle[i] = new float[i + 1];
			triangle[i][0] = 1.0f;
			triangle[i][i] = 1.0f;
			for (int j = 1; j < triangle[i].length - 1; j++)
				triangle[i][j] = triangle[i - 1][j - 1] + triangle[i - 1][j];
		}
		return triangle;
	}
}
