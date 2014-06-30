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
 * An effect that genrates a wobbly line around the outline of the text
 * 
 * @author Jerry Huxtable
 * @author Nathan Sweet <misc@n4te.com>
 */
public class OutlineWobbleEffect extends OutlineEffect {
	/** How often the line wobbles */
	private float detail = 1;
	/** The amount of the line wobbles */
	private float amplitude = 1;

	/**
	 * Default constructor for injection
	 */
	public OutlineWobbleEffect () {
		setStroke(new WobbleStroke());
	}

	/**
	 * Gets the detail of the wobble effect.
	 * 
	 * @return The detail of the wobble effect
	 */
	public float getDetail() {
		return detail;
	}

	/**
	 * Sets the detail of the wobble effect.
	 * 
	 * @param detail The detail of the wobble effect
	 */
	public void setDetail(float detail) {
		this.detail = detail;
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
	 * Create a new effect that generates a wobbly line around the text
	 * 
	 * @param width The width of the line
	 * @param color The colour of the line
	 */
	public OutlineWobbleEffect (int width, Color color) {
		super(width, color);
	}

	/**
	 * @see org.newdawn.slick.font.effects.OutlineEffect#toString()
	 */
	public String toString() {
		return "Outline (Wobble)";
	}

	/**
	 * @see org.newdawn.slick.font.effects.OutlineEffect#getValues()
	 */
	public List getValues() {
		List values = super.getValues();
		values.remove(2); // Remove "Join".
		values.add(EffectUtil.floatValue("Detail", detail, 1, 50, "This setting controls how detailed the outline will be. "
			+ "Smaller numbers cause the outline to have more detail."));
		values.add(EffectUtil.floatValue("Amplitude", amplitude, 0.5f, 50, "This setting controls the amplitude of the outline."));
		return values;
	}

	/**
	 * @see org.newdawn.slick.font.effects.OutlineEffect#setValues(java.util.List)
	 */
	public void setValues(List values) {
		super.setValues(values);
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			Value value = (Value)iter.next();
			if (value.getName().equals("Detail")) {
				detail = ((Float)value.getObject()).floatValue();
			} else if (value.getName().equals("Amplitude")) {
				amplitude = ((Float)value.getObject()).floatValue();
			}
		}
	}

	/**
	 * A stroke that generate a wobbly line
	 * 
	 * @author Jerry Huxtable
	 * @author Nathan Sweet <misc@n4te.com>
	 */
	private class WobbleStroke implements Stroke {
		/** The flattening factor of the stroke */
		private static final float FLATNESS = 1;

		/**
		 * @see java.awt.Stroke#createStrokedShape(java.awt.Shape)
		 */
		public Shape createStrokedShape (Shape shape) {
			GeneralPath result = new GeneralPath();
			shape = new BasicStroke(getWidth(), BasicStroke.CAP_SQUARE, getJoin()).createStrokedShape(shape);
			PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
			float points[] = new float[6];
			float moveX = 0, moveY = 0;
			float lastX = 0, lastY = 0;
			float thisX = 0, thisY = 0;
			int type = 0;
			float next = 0;
			while (!it.isDone()) {
				type = it.currentSegment(points);
				switch (type) {
				case PathIterator.SEG_MOVETO:
					moveX = lastX = randomize(points[0]);
					moveY = lastY = randomize(points[1]);
					result.moveTo(moveX, moveY);
					next = 0;
					break;

				case PathIterator.SEG_CLOSE:
					points[0] = moveX;
					points[1] = moveY;
					// Fall into....

				case PathIterator.SEG_LINETO:
					thisX = randomize(points[0]);
					thisY = randomize(points[1]);
					float dx = thisX - lastX;
					float dy = thisY - lastY;
					float distance = (float)Math.sqrt(dx * dx + dy * dy);
					if (distance >= next) {
						float r = 1.0f / distance;
						while (distance >= next) {
							float x = lastX + next * dx * r;
							float y = lastY + next * dy * r;
							result.lineTo(randomize(x), randomize(y));
							next += detail;
						}
					}
					next -= distance;
					lastX = thisX;
					lastY = thisY;
					break;
				}
				it.next();
			}

			return result;
		}

		/**
		 * Get a random wobble factor
		 * 
		 * @param x The position on the line
		 * @return The wobble factor
		 */
		private float randomize(float x) {
			return x + (float)Math.random() * amplitude * 2 - 1;
		}
	}
}
