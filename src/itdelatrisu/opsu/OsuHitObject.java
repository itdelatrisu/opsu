/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu;

/**
 * Data type representing a hit object.
 */
public class OsuHitObject {
	/** Hit object types (bits). */
	public static final int
		TYPE_CIRCLE   = 1,
		TYPE_SLIDER   = 2,
		TYPE_NEWCOMBO = 4,  // not an object
		TYPE_SPINNER  = 8;

	/** Hit sound types (bits). */
	public static final byte
		SOUND_NORMAL  = 0,
		SOUND_WHISTLE = 2,
		SOUND_FINISH  = 4,
		SOUND_CLAP    = 8;

	/**
	 * Slider curve types.
	 * (Deprecated: only Beziers are currently used.)
	 */
	public static final char
		SLIDER_CATMULL     = 'C',
		SLIDER_BEZIER      = 'B',
		SLIDER_LINEAR      = 'L',
		SLIDER_PASSTHROUGH = 'P';

	/** Max hit object coordinates. */
	private static final int
		MAX_X = 512,
		MAX_Y = 384;

	/** The x and y multipliers for hit object coordinates. */
	private static float xMultiplier, yMultiplier;

	/** The x and y offsets for hit object coordinates. */
	private static int
		xOffset,   // offset right of border
		yOffset;   // offset below health bar

	/** Starting coordinates (scaled). */
	private float x, y;

	/** Start time (in ms). */
	private int time;

	/** Hit object type (TYPE_* bitmask). */
	private int type;

	/** Hit sound type (SOUND_* bitmask). */
	private byte hitSound;

	/** Hit sound addition (sampleSet, AdditionSampleSet, ?, ...). */
	private byte[] addition;

	/** Slider curve type (SLIDER_* constant). */
	private char sliderType;

	/** Slider coordinate lists (scaled). */
	private float[] sliderX, sliderY;

	/** Slider repeat count. */
	private int repeat;

	/** Slider pixel length. */
	private float pixelLength;

	/** Spinner end time (in ms). */
	private int endTime;

	/** Slider edge hit sound type (SOUND_* bitmask). */
	private byte[] edgeHitSound;

	/** Slider edge hit sound addition (sampleSet, AdditionSampleSet). */
	private byte[][] edgeAddition;

	/** Current index in combo color array. */
	private int comboIndex;

	/** Number to display in hit object. */
	private int comboNumber;

	/**
	 * Initializes the OsuHitObject data type with container dimensions.
	 * @param width the container width
	 * @param height the container height
	 */
	public static void init(int width, int height) {
		int swidth = width;
		int sheight = height;
		if (swidth * 3 > sheight * 4)
			swidth = sheight * 4 / 3;
		else
			sheight = swidth * 3 / 4;
		xMultiplier = swidth / 640f;
		yMultiplier = sheight / 480f;
		xOffset = (int) (width - MAX_X * xMultiplier) / 2;
		yOffset = (int) (height - MAX_Y * yMultiplier) / 2;
	}

	/**
	 * Returns the X multiplier for coordinates.
	 */
	public static float getXMultiplier() { return xMultiplier; }

	/**
	 * Returns the Y multiplier for coordinates.
	 */
	public static float getYMultiplier() { return yMultiplier; }

	/**
	 * Constructor.
	 * @param line the line to be parsed
	 */
	public OsuHitObject(String line) {
		/**
		 * [OBJECT FORMATS]
		 * Circles:
		 *   x,y,time,type,hitSound,addition
		 *   256,148,9466,1,2,0:0:0:0:
		 *
		 * Sliders:
		 *   x,y,time,type,hitSound,sliderType|curveX:curveY|...,repeat,pixelLength,edgeHitsound,edgeAddition,addition
		 *   300,68,4591,2,0,B|372:100|332:172|420:192,2,180,2|2|2,0:0|0:0|0:0,0:0:0:0:
		 *
		 * Spinners:
		 *   x,y,time,type,hitSound,endTime,addition
		 *   256,192,654,12,0,4029,0:0:0:0:
		 *
		 * NOTE: 'addition' -> sampl:add:cust:vol:hitsound (optional, defaults to "0:0:0:0:")
		 */
		String tokens[] = line.split(",");

		// common fields
		this.x = Float.parseFloat(tokens[0]) * xMultiplier + xOffset;
		this.y = Float.parseFloat(tokens[1]) * yMultiplier + yOffset;
		this.time = Integer.parseInt(tokens[2]);
		this.type = Integer.parseInt(tokens[3]);
		this.hitSound = Byte.parseByte(tokens[4]);

		// type-specific fields
		if ((type & OsuHitObject.TYPE_CIRCLE) > 0) {
			if (tokens.length > 5) {
				String[] additionTokens = tokens[5].split(":");
				this.addition = new byte[additionTokens.length];
				for (int j = 0; j < additionTokens.length; j++)
					this.addition[j] = Byte.parseByte(additionTokens[j]);
			}
		} else if ((type & OsuHitObject.TYPE_SLIDER) > 0) {
			// slider curve type and coordinates
			String[] sliderTokens = tokens[5].split("\\|");
			this.sliderType = sliderTokens[0].charAt(0);
			this.sliderX = new float[sliderTokens.length - 1];
			this.sliderY = new float[sliderTokens.length - 1];
			for (int j = 1; j < sliderTokens.length; j++) {
				String[] sliderXY = sliderTokens[j].split(":");
				this.sliderX[j - 1] = Integer.parseInt(sliderXY[0]) * xMultiplier + xOffset;
				this.sliderY[j - 1] = Integer.parseInt(sliderXY[1]) * yMultiplier + yOffset;
			}
			this.repeat = Integer.parseInt(tokens[6]);
			this.pixelLength = Float.parseFloat(tokens[7]);
			if (tokens.length > 8) {
				String[] edgeHitSoundTokens = tokens[8].split("\\|");
				this.edgeHitSound = new byte[edgeHitSoundTokens.length];
				for (int j = 0; j < edgeHitSoundTokens.length; j++)
					edgeHitSound[j] = Byte.parseByte(edgeHitSoundTokens[j]);
			}
			if (tokens.length > 9) {
				String[] edgeAdditionTokens = tokens[9].split("\\|");
				this.edgeAddition = new byte[edgeAdditionTokens.length][2];
				for (int j = 0; j < edgeAdditionTokens.length; j++) {
					String[] tedgeAddition = edgeAdditionTokens[j].split(":");
					edgeAddition[j][0] = Byte.parseByte(tedgeAddition[0]);
					edgeAddition[j][1] = Byte.parseByte(tedgeAddition[1]);
				}
			}
		} else { //if ((type & OsuHitObject.TYPE_SPINNER) > 0) {
			// some 'endTime' fields contain a ':' character (?)
			int index = tokens[5].indexOf(':');
			if (index != -1)
				tokens[5] = tokens[5].substring(0, index);
			this.endTime = Integer.parseInt(tokens[5]);
			/* TODO: 'addition' not implemented. */
		}
	}

	/**
	 * Returns the starting x coordinate.
	 * @return the x coordinate
	 */
	public float getX() { return x; }

	/**
	 * Returns the starting y coordinate.
	 * @return the y coordinate
	 */
	public float getY() { return y; }

	/**
	 * Returns the start time.
	 * @return the start time (in ms)
	 */
	public int getTime() { return time; }

	/**
	 * Returns the hit object type.
	 * @return the object type (TYPE_* bitmask)
	 */
	public int getType() { return type; }

	/**
	 * Returns the hit sound type.
	 * @return the sound type (SOUND_* bitmask)
	 */
	public byte getHitSoundType() { return hitSound; }

	/**
	 * Returns the edge hit sound type.
	 * @param index the slider edge index (ignored for non-sliders)
	 * @return the sound type (SOUND_* bitmask)
	 */
	public byte getEdgeHitSoundType(int index) {
		if (edgeHitSound != null)
			return edgeHitSound[index];
		else
			return hitSound;
	}

	/**
	 * Returns the slider type.
	 * @return the slider type (SLIDER_* constant)
	 */
	public char getSliderType() { return sliderType; }

	/**
	 * Returns a list of slider x coordinates.
	 * @return the slider x coordinates
	 */
	public float[] getSliderX() { return sliderX; }

	/**
	 * Returns a list of slider y coordinates.
	 * @return the slider y coordinates
	 */
	public float[] getSliderY() { return sliderY; }

	/**
	 * Returns the slider repeat count.
	 * @return the repeat count
	 */
	public int getRepeatCount() { return repeat; }

	/**
	 * Returns the slider pixel length.
	 * @return the pixel length
	 */
	public float getPixelLength() { return pixelLength; }

	/**
	 * Returns the spinner end time.
	 * @return the end time (in ms)
	 */
	public int getEndTime() { return endTime; }

	/**
	 * Sets the current index in the combo color array.
	 * @param comboIndex the combo index
	 */
	public void setComboIndex(int comboIndex) { this.comboIndex = comboIndex; }

	/**
	 * Returns the current index in the combo color array.
	 * @return the combo index
	 */
	public int getComboIndex() { return comboIndex; }

	/**
	 * Sets the number to display in the hit object.
	 * @param comboNumber the combo number
	 */
	public void setComboNumber(int comboNumber) { this.comboNumber = comboNumber; }

	/**
	 * Returns the number to display in the hit object.
	 * @return the combo number
	 */
	public int getComboNumber() { return comboNumber; }

	/**
	 * Returns whether or not the hit object is a circle.
	 * @return true if circle
	 */
	public boolean isCircle() { return (type & TYPE_CIRCLE) > 0; }

	/**
	 * Returns whether or not the hit object is a slider.
	 * @return true if slider
	 */
	public boolean isSlider() { return (type & TYPE_SLIDER) > 0; }

	/**
	 * Returns whether or not the hit object is a spinner.
	 * @return true if spinner
	 */
	public boolean isSpinner() { return (type & TYPE_SPINNER) > 0; }

	/**
	 * Returns whether or not the hit object starts a new combo.
	 * @return true if new combo
	 */
	public boolean isNewCombo() { return (type & TYPE_NEWCOMBO) > 0; }

	/**
	 * Returns the number of extra skips on the combo colors.
	 */
	public int getComboSkip() { return (type >> TYPE_NEWCOMBO); }

	/**
	 * Returns the sample set at the given index.
	 * @param index the index (for sliders, ignored otherwise)
	 * @return the sample set, or 0 if none available
	 */
	public byte getSampleSet(int index) {
		if (edgeAddition != null)
			return edgeAddition[index][0];
		if (addition != null)
			return addition[0];
		return 0;
	}

	/**
	 * Returns the 'addition' sample set at the given index.
	 * @param index the index (for sliders, ignored otherwise)
	 * @return the sample set, or 0 if none available
	 */
	public byte getAdditionSampleSet(int index) {
		if (edgeAddition != null)
			return edgeAddition[index][1];
		if (addition != null)
			return addition[1];
		return 0;
	}
}
