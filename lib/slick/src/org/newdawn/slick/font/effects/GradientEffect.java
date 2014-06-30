
package org.newdawn.slick.font.effects;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.Glyph;

/**
 * Paints glyphs with a gradient fill.
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class GradientEffect implements ConfigurableEffect {
	/** The top of gradients colour */
	private Color topColor = Color.cyan;
	/** The bottom of the gradient's colour */
	private Color bottomColor = Color.blue;
	/** The offset the gradient starts at */
	private int offset = 0;
	/** The scaling of the graident */
	private float scale = 1;
	/** True if the graident should cycle back and forth across the surface */
	private boolean cyclic;

	/**
	 * Default constructor for injection
	 */
	public GradientEffect() {
	}

	/**
	 * Create a new effect to apply a graident
	 * 
	 * @param topColor The colour at the top of the graident
	 * @param bottomColor The colour at the bottom of the gradient
	 * @param scale The scale of the graident
	 */
	public GradientEffect(Color topColor, Color bottomColor, float scale) {
		this.topColor = topColor;
		this.bottomColor = bottomColor;
		this.scale = scale;
	}

	/**
	 * @see org.newdawn.slick.font.effects.Effect#draw(java.awt.image.BufferedImage, java.awt.Graphics2D, org.newdawn.slick.UnicodeFont, org.newdawn.slick.font.Glyph)
	 */
	public void draw(BufferedImage image, Graphics2D g, UnicodeFont unicodeFont, Glyph glyph) {
		int ascent = unicodeFont.getAscent();
		float height = (ascent) * scale;
		float top = -glyph.getYOffset() + unicodeFont.getDescent() + offset + ascent / 2 - height / 2;
		g.setPaint(new GradientPaint(0, top, topColor, 0, top + height, bottomColor, cyclic));
		g.fill(glyph.getShape());
	}

	/**
	 * Get the colour at the top of the graident
	 * 
	 * @return The colour at the top of the gradient
	 */
	public Color getTopColor() {
		return topColor;
	}

	/**
	 * Set the colour at the top of the graident
	 * 
	 * @param topColor The colour at the top of the graident
	 */
	public void setTopColor(Color topColor) {
		this.topColor = topColor;
	}

	/**
	 * Get the colour at the bottom of the graident
	 * 
	 * @return The colour at the bottom of the gradient
	 */
	public Color getBottomColor () {
		return bottomColor;
	}

	/**
	 * Set the colour at the bottom of the graident
	 * 
	 * @param bottomColor The colour at the bottom of the graident
	 */
	public void setBottomColor(Color bottomColor) {
		this.bottomColor = bottomColor;
	}

	/**
	 * Get the offset the gradients starts at
	 * 
	 * @return The offset the gradient starts at
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * Sets the pixel offset to move the gradient up or down. 
	 * The gradient is normally centered on the glyph.
	 * 
	 * @param offset The offset the gradient is moved by
	 */
	public void setOffset (int offset) {
		this.offset = offset;
	}

	/**
	 * Get the percentage scaling being applied to the gradient across the surface
	 * 
	 * @return The scale of the graident
	 */
	public float getScale() {
		return scale;
	}

	/**
	 * Changes the height of the gradient by a percentage. The gradient is 
	 * normally the height of most glyphs in the font.
	 * 
	 * @param scale The scale to apply
	 */
	public void setScale (float scale) {
		this.scale = scale;
	}

	/**
	 * Check if the graident is repeating
	 * 
	 * @return True if the gradient is repeating
	 */
	public boolean isCyclic() {
		return cyclic;
	}

	/**
	 * If set to true, the gradient will repeat.
	 * 
	 * @param cyclic True if the graident repeats
	 */
	public void setCyclic(boolean cyclic) {
		this.cyclic = cyclic;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Gradient";
	}

	/**
	 * @see org.newdawn.slick.font.effects.ConfigurableEffect#getValues()
	 */
	public List getValues() {
		List values = new ArrayList();
		values.add(EffectUtil.colorValue("Top color", topColor));
		values.add(EffectUtil.colorValue("Bottom color", bottomColor));
		values.add(EffectUtil.intValue("Offset", offset,
			"This setting allows you to move the gradient up or down. The gradient is normally centered on the glyph."));
		values.add(EffectUtil.floatValue("Scale", scale, 0, 1, "This setting allows you to change the height of the gradient by a"
			+ "percentage. The gradient is normally the height of most glyphs in the font."));
		values.add(EffectUtil.booleanValue("Cyclic", cyclic, "If this setting is checked, the gradient will repeat."));
		return values;
	}

	/**
	 * @see org.newdawn.slick.font.effects.ConfigurableEffect#setValues(java.util.List)
	 */
	public void setValues(List values) {
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			Value value = (Value)iter.next();
			if (value.getName().equals("Top color")) {
				topColor = (Color)value.getObject();
			} else if (value.getName().equals("Bottom color")) {
				bottomColor = (Color)value.getObject();
			} else if (value.getName().equals("Offset")) {
				offset = ((Integer)value.getObject()).intValue();
			} else if (value.getName().equals("Scale")) {
				scale = ((Float)value.getObject()).floatValue();
			} else if (value.getName().equals("Cyclic")) {
				cyclic = ((Boolean)value.getObject()).booleanValue();
			}
		}
	}
}
