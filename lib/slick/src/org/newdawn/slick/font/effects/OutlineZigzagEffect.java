/*
 * Copyright 2006 Jerry Huxtable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.newdawn.slick.font.effects;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.Iterator;
import java.util.List;


/**
 * An effect to generate a uniformly zigzaging line around text
 * 
 * @author Jerry Huxtable
 * @author Nathan Sweet <misc@n4te.com>
 */
public class OutlineZigzagEffect extends OutlineEffect {
	/** The amount the line moves away from the text */
	private float amplitude = 1;
	/** How often the line zigs and zags */
	private float wavelength = 3;

	/**
	 * Default constructor for injection
	 */
	public OutlineZigzagEffect() {
		setStroke(new ZigzagStroke());
	}

	/**
	 * Gets the wavelength of the wobble effect.
	 * 
	 * @return The wavelength of the wobble effect
	 */
	public float getWavelength() {
		return wavelength;
	}

	/**
	 * Sets the wavelength of the wobble effect.
	 * 
	 * @param wavelength The wavelength of the wobble effect
	 */
	public void setWavelength(float wavelength) {
		this.wavelength = wavelength;
	}

	/**
	 * Gets the amplitude of the wobble effect.
	 * 
	 * @return The amplitude of the wobble effect
	 */
	public float getAmplitude() {
		return amplitude;
	}

	/**
	 * Sets the amplitude of the wobble effect.
	 * 
	 * @param amplitude The detail of the wobble effect
	 */
	public void setAmplitude(float amplitude) {
		this.amplitude = amplitude;
	}
	
	/**
	 * Create a new effect to generate a zigzagging line around the text
	 * 
	 * @param width The width of the line
	 * @param color The colour of the line
	 */
	public OutlineZigzagEffect(int width, Color color) {
		super(width, color);
	}

	/**
	 * @see org.newdawn.slick.font.effects.OutlineEffect#toString()
	 */
	public String toString () {
		return "Outline (Zigzag)";
	}

	/**
	 * @see org.newdawn.slick.font.effects.OutlineEffect#getValues()
	 */
	public List getValues() {
		List values = super.getValues();
		values.add(EffectUtil.floatValue("Wavelength", wavelength, 1, 100, "This setting controls the wavelength of the outline. "
			+ "The smaller the value, the more segments will be used to draw the outline."));
		values.add(EffectUtil.floatValue("Amplitude", amplitude, 0.5f, 50, "This setting controls the amplitude of the outline. "
			+ "The bigger the value, the more the zigzags will vary."));
		return values;
	}

	/**
	 * @see org.newdawn.slick.font.effects.OutlineEffect#setValues(java.util.List)
	 */
	public void setValues(List values) {
		super.setValues(values);
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			Value value = (Value)iter.next();
			if (value.getName().equals("Wavelength")) {
				wavelength = ((Float)value.getObject()).floatValue();
			} else if (value.getName().equals("Amplitude")) {
				amplitude = ((Float)value.getObject()).floatValue();
			}
		}
	}

	/**
	 * A stroke to generate zigzags
	 * 
	 * @author Jerry Huxtable
	 * @author Nathan Sweet <misc@n4te.com>
	 */
	private class ZigzagStroke implements Stroke {
		/** The flattening factor applied to the path iterator */
		private static final float FLATNESS = 1;

		/** 
		 * @see java.awt.Stroke#createStrokedShape(java.awt.Shape)
		 */
		public Shape createStrokedShape (Shape shape) {
			GeneralPath result = new GeneralPath();
			PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
			float points[] = new float[6];
			float moveX = 0, moveY = 0;
			float lastX = 0, lastY = 0;
			float thisX = 0, thisY = 0;
			int type = 0;
			float next = 0;
			int phase = 0;
			while (!it.isDone()) {
				type = it.currentSegment(points);
				switch (type) {
				case PathIterator.SEG_MOVETO:
					moveX = lastX = points[0];
					moveY = lastY = points[1];
					result.moveTo(moveX, moveY);
					next = wavelength / 2;
					break;

				case PathIterator.SEG_CLOSE:
					points[0] = moveX;
					points[1] = moveY;
					// Fall into....

				case PathIterator.SEG_LINETO:
					thisX = points[0];
					thisY = points[1];
					float dx = thisX - lastX;
					float dy = thisY - lastY;
					float distance = (float)Math.sqrt(dx * dx + dy * dy);
					if (distance >= next) {
						float r = 1.0f / distance;
						while (distance >= next) {
							float x = lastX + next * dx * r;
							float y = lastY + next * dy * r;
							if ((phase & 1) == 0)
								result.lineTo(x + amplitude * dy * r, y - amplitude * dx * r);
							else
								result.lineTo(x - amplitude * dy * r, y + amplitude * dx * r);
							next += wavelength;
							phase++;
						}
					}
					next -= distance;
					lastX = thisX;
					lastY = thisY;
					if (type == PathIterator.SEG_CLOSE) result.closePath();
					break;
				}
				it.next();
			}
			return new BasicStroke(getWidth(), BasicStroke.CAP_SQUARE, getJoin()).createStrokedShape(result);
		}
	}
}
