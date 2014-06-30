
package org.newdawn.slick.font.effects;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.Glyph;

/**
 * Strokes glyphs with an outline.
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class OutlineEffect implements ConfigurableEffect {
	/** The width of the outline in pixels */
	private float width = 2;
	/** The colour of the outline */
	private Color color = Color.black;
	/** The type of join at the line joins of the out line */
	private int join = BasicStroke.JOIN_BEVEL;
	/** The stroke used to draw the outline */
	private Stroke stroke;

	/**
	 * Default constructor for injection
	 */
	public OutlineEffect() {
	}

	/**
	 * Create a new effect to draw the outline of the text
	 * 
	 * @param width The width of the outline
	 * @param color The colour of the outline
	 */
	public OutlineEffect(int width, Color color) {
		this.width = width;
		this.color = color;
	}

	/**
	 * @see org.newdawn.slick.font.effects.Effect#draw(java.awt.image.BufferedImage, java.awt.Graphics2D, org.newdawn.slick.UnicodeFont, org.newdawn.slick.font.Glyph)
	 */
	public void draw(BufferedImage image, Graphics2D g, UnicodeFont unicodeFont, Glyph glyph) {
		g = (Graphics2D)g.create();
		if (stroke != null)
			g.setStroke(stroke);
		else
			g.setStroke(getStroke());
		g.setColor(color);
		g.draw(glyph.getShape());
		g.dispose();
	}

	/**
	 * Get the width of the outline being drawn
	 * 
	 * @return The width of the outline being drawn
	 */
	public float getWidth() {
		return width;
	}

	/**
	 * Sets the width of the outline. The glyphs will need padding so the 
	 * outline doesn't get clipped.
	 * 
	 * @param width The width of the outline being drawn
	 */
	public void setWidth (int width) {
		this.width = width;
	}

	/**
	 * Get the colour of the outline being drawn
	 * 
	 * @return The colour of the outline being drawn
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Set the colour of the outline being drawn
	 * 
	 * @param color The colour of the outline to draw
	 */
	public void setColor(Color color) {
		this.color = color;
	}

	/**
	 * Get the join type as indicated by @see BasicStroke
	 * 
	 * @return The join type between segments in the outline 
	 */
	public int getJoin() {
		return join;
	}

	/**
	 * Get the stroke being used to draw the outline
	 * 
	 * @return The stroke being used to draw the outline
	 */
	public Stroke getStroke() {
		if (stroke == null) {
			return new BasicStroke(width, BasicStroke.CAP_SQUARE, join);
		}
		
		return stroke;
	}

	/**
	 * Sets the stroke to use for the outline. If this is set, 
	 * the other outline settings are ignored.
	 * 
	 * @param stroke The stroke to be used to draw the outline
	 */
	public void setStroke (Stroke stroke) {
		this.stroke = stroke;
	}

	/**
	 * Sets how the corners of the outline are drawn. This is usually only noticeable 
	 * at large outline widths.
	 * 
	 * @param join One of: {@link BasicStroke#JOIN_BEVEL}, {@link BasicStroke#JOIN_MITER}, {@link BasicStroke#JOIN_ROUND}
	 */
	public void setJoin (int join) {
		this.join = join;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Outline";
	}

	/**
	 * @see org.newdawn.slick.font.effects.ConfigurableEffect#getValues()
	 */
	public List getValues () {
		List values = new ArrayList();
		values.add(EffectUtil.colorValue("Color", color));
		values.add(EffectUtil.floatValue("Width", width, 0.1f, 999, "This setting controls the width of the outline. "
			+ "The glyphs will need padding so the outline doesn't get clipped."));
		values.add(EffectUtil.optionValue("Join", String.valueOf(join), new String[][] { {"Bevel", BasicStroke.JOIN_BEVEL + ""},
			{"Miter", BasicStroke.JOIN_MITER + ""}, {"Round", BasicStroke.JOIN_ROUND + ""}},
			"This setting defines how the corners of the outline are drawn. "
				+ "This is usually only noticeable at large outline widths."));
		return values;
	}

	/**
	 * @see org.newdawn.slick.font.effects.ConfigurableEffect#setValues(java.util.List)
	 */
	public void setValues (List values) {
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			Value value = (Value)iter.next();
			if (value.getName().equals("Color")) {
				color = (Color)value.getObject();
			} else if (value.getName().equals("Width")) {
				width = ((Float)value.getObject()).floatValue();
			} else if (value.getName().equals("Join")) {
				join = Integer.parseInt((String)value.getObject());
			}
		}
	}
}
