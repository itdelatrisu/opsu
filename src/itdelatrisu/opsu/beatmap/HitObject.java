/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2017 Jeffrey Han
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

package itdelatrisu.opsu.beatmap;

import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.objects.curves.CatmullCurve;
import itdelatrisu.opsu.objects.curves.CircumscribedCircle;
import itdelatrisu.opsu.objects.curves.Curve;
import itdelatrisu.opsu.objects.curves.LinearBezier;
import itdelatrisu.opsu.objects.curves.Vec2f;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Data type representing a parsed hit object.
 */
public class HitObject {
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
		SLIDER_CATMULL       = 'C',
		SLIDER_BEZIER        = 'B',
		SLIDER_LINEAR        = 'L',
		SLIDER_PERFECT_CURVE = 'P';

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

	/** The container height. */
	private static int containerHeight;

	/** The offset per stack. */
	private static float stackOffset;

	/**
	 * Returns the stack position modifier, in pixels.
	 * @return stack position modifier
	 */
	public static float getStackOffset() { return stackOffset; }

	/**
	 * Sets the stack position modifier.
	 * @param offset stack position modifier, in pixels
	 */
	public static void setStackOffset(float offset) { stackOffset = offset; }

	/** Starting coordinates. */
	private float x, y;

	/** Start time (in ms). */
	private int time;

	/** Hit object type (TYPE_* bitmask). */
	private int type;

	/** Hit sound type (SOUND_* bitmask). */
	private short hitSound;

	/** Hit sound addition (sampleSet, AdditionSampleSet). */
	private byte[] addition;

	/** Addition custom sample index. */
	private byte additionCustomSampleIndex;

	/** Addition hit sound volume. */
	private int additionHitSoundVolume;

	/** Addition hit sound file. */
	private String additionHitSound;

	/** Slider curve type (SLIDER_* constant). */
	private char sliderType;

	/** Slider coordinate lists. */
	private float[] sliderX, sliderY;

	/** Slider repeat count. */
	private int repeat;

	/** Slider pixel length. */
	private float pixelLength;

	/** Spinner end time (in ms). */
	private int endTime;

	/** Slider edge hit sound type (SOUND_* bitmask). */
	private short[] edgeHitSound;

	/** Slider edge hit sound addition (sampleSet, AdditionSampleSet). */
	private byte[][] edgeAddition;

	/** Current index in combo color array. */
	private int comboIndex;

	/** Number to display in hit object. */
	private int comboNumber;

	/** Hit object index in the current stack. */
	private int stack;

	/**
	 * Initializes the HitObject data type with container dimensions.
	 * @param width the container width
	 * @param height the container height
	 */
	public static void init(int width, int height) {
		containerHeight = height;
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
	 * Returns the X offset for coordinates.
	 */
	public static int getXOffset() { return xOffset; }

	/**
	 * Returns the Y offset for coordinates.
	 */
	public static int getYOffset() { return yOffset; }

	/**
	 * Constructor.
	 * @param line the line to be parsed
	 */
	public HitObject(String line) {
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
		this.x = Float.parseFloat(tokens[0]);
		this.y = Float.parseFloat(tokens[1]);
		this.time = Integer.parseInt(tokens[2]);
		this.type = Integer.parseInt(tokens[3]);
		this.hitSound = Byte.parseByte(tokens[4]);

		// type-specific fields
		int additionIndex;
		if ((type & HitObject.TYPE_CIRCLE) > 0)
			additionIndex = 5;
		else if ((type & HitObject.TYPE_SLIDER) > 0) {
			additionIndex = 10;

			// slider curve type and coordinates
			String[] sliderTokens = tokens[5].split("\\|");
			this.sliderType = sliderTokens[0].charAt(0);
			this.sliderX = new float[sliderTokens.length - 1];
			this.sliderY = new float[sliderTokens.length - 1];
			for (int j = 1; j < sliderTokens.length; j++) {
				String[] sliderXY = sliderTokens[j].split(":");
				this.sliderX[j - 1] = Integer.parseInt(sliderXY[0]);
				this.sliderY[j - 1] = Integer.parseInt(sliderXY[1]);
			}
			this.repeat = Integer.parseInt(tokens[6]);
			this.pixelLength = Float.parseFloat(tokens[7]);
			if (tokens.length > 8) {
				String[] edgeHitSoundTokens = tokens[8].split("\\|");
				this.edgeHitSound = new short[edgeHitSoundTokens.length];
				for (int j = 0; j < edgeHitSoundTokens.length; j++)
					edgeHitSound[j] = Short.parseShort(edgeHitSoundTokens[j]);
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
		} else { //if ((type & HitObject.TYPE_SPINNER) > 0) {
			additionIndex = 6;

			// some 'endTime' fields contain a ':' character (?)
			int index = tokens[5].indexOf(':');
			if (index != -1)
				tokens[5] = tokens[5].substring(0, index);
			this.endTime = Integer.parseInt(tokens[5]);
		}

		// addition
		if (tokens.length > additionIndex) {
			String[] additionTokens = tokens[additionIndex].split(":");
			if (additionTokens.length > 1) {
				this.addition = new byte[2];
				addition[0] = Byte.parseByte(additionTokens[0]);
				addition[1] = Byte.parseByte(additionTokens[1]);
			}
			if (additionTokens.length > 2)
				this.additionCustomSampleIndex = Byte.parseByte(additionTokens[2]);
			if (additionTokens.length > 3)
				this.additionHitSoundVolume = Integer.parseInt(additionTokens[3]);
			if (additionTokens.length > 4)
				this.additionHitSound = additionTokens[4];
		}
	}

	/**
	 * Returns the raw starting x coordinate.
	 */
	public float getX() { return x; }

	/**
	 * Returns the raw starting y coordinate.
	 */
	public float getY() { return y; }

	/**
	 * Returns the scaled starting x coordinate.
	 */
	public float getScaledX() { return (x - stack * stackOffset) * xMultiplier + xOffset; }

	/**
	 * Returns the scaled starting y coordinate.
	 */
	public float getScaledY() {
		if (GameMod.HARD_ROCK.isActive())
			return containerHeight - ((y + stack * stackOffset) * yMultiplier + yOffset);
		else
			return (y - stack * stackOffset) * yMultiplier + yOffset;
	}

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
	 * Returns the name of the hit object type.
	 */
	public String getTypeName() {
		if (isCircle())
			return "circle";
		else if (isSlider())
			return "slider";
		else if (isSpinner())
			return "spinner";
		else
			return "unknown object type";
	}

	/**
	 * Returns the hit sound type.
	 * @return the sound type (SOUND_* bitmask)
	 */
	public short getHitSoundType() { return hitSound; }

	/**
	 * Returns the edge hit sound type.
	 * @param index the slider edge index (ignored for non-sliders)
	 * @return the sound type (SOUND_* bitmask)
	 */
	public short getEdgeHitSoundType(int index) {
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
	 * Returns a list of raw slider x coordinates.
	 */
	public float[] getSliderX() { return sliderX; }

	/**
	 * Returns a list of raw slider y coordinates.
	 */
	public float[] getSliderY() { return sliderY; }

	/**
	 * Returns a list of scaled slider x coordinates.
	 * Note that this method will create a new array.
	 */
	public float[] getScaledSliderX() {
		if (sliderX == null)
			return null;

		float[] x = new float[sliderX.length];
		for (int i = 0; i < x.length; i++)
			x[i] = (sliderX[i] - stack * stackOffset) * xMultiplier + xOffset;
		return x;
	}

	/**
	 * Returns a list of scaled slider y coordinates.
	 * Note that this method will create a new array.
	 */
	public float[] getScaledSliderY() {
		if (sliderY == null)
			return null;

		float[] y = new float[sliderY.length];
		if (GameMod.HARD_ROCK.isActive()) {
			for (int i = 0; i < y.length; i++)
				y[i] = containerHeight - ((sliderY[i] + stack * stackOffset) * yMultiplier + yOffset);
		} else {
			for (int i = 0; i < y.length; i++)
				y[i] = (sliderY[i] - stack * stackOffset) * yMultiplier + yOffset;
		}
		return y;
	}

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
	 * Returns the time duration of the slider (excluding repeats), in milliseconds.
	 * @param sliderMultiplier the beatmap's slider movement speed multiplier
	 * @param beatLength the beat length
	 * @return the slider segment length
	 */
	public float getSliderTime(float sliderMultiplier, float beatLength) {
		return beatLength * (pixelLength / sliderMultiplier) / 100f;
	}

	/**
	 * Returns the slider curve.
	 * @param scaled whether to use scaled coordinates
	 * @return a new Curve instance
	 */
	public Curve getSliderCurve(boolean scaled) {
		if (sliderType == SLIDER_PERFECT_CURVE && sliderX.length == 2) {
			Vec2f nora = new Vec2f(sliderX[0] - x, sliderY[0] - y).nor();
			Vec2f norb = new Vec2f(sliderX[0] - sliderX[1], sliderY[0] - sliderY[1]).nor();
			if (Math.abs(norb.x * nora.y - norb.y * nora.x) < 0.00001f)
				return new LinearBezier(this, false, scaled);  // vectors parallel, use linear bezier instead
			else
				return new CircumscribedCircle(this, scaled);
		} else if (sliderType == SLIDER_CATMULL)
			return new CatmullCurve(this, scaled);
		else
			return new LinearBezier(this, sliderType == SLIDER_LINEAR, scaled);
	}

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

	/**
	 * Returns the custom sample index (addition).
	 */
	public byte getCustomSampleIndex() { return additionCustomSampleIndex; }

	/**
	 * Returns the hit sound volume (addition).
	 */
	public int getHitSoundVolume() { return additionHitSoundVolume; }

	/**
	 * Returns the hit sound file (addition).
	 */
	public String getHitSoundFile() { return additionHitSound; }

	/**
	 * Sets the hit object index in the current stack.
	 * @param stack index in the stack
	 */
	public void setStack(int stack) { this.stack = stack; }

	/**
	 * Returns the hit object index in the current stack.
	 * @return index in the stack
	 */
	public int getStack() { return stack; }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		NumberFormat nf = new DecimalFormat("###.#####");

		// common fields
		sb.append(nf.format(x)); sb.append(',');
		sb.append(nf.format(y)); sb.append(',');
		sb.append(time); sb.append(',');
		sb.append(type); sb.append(',');
		sb.append(hitSound); sb.append(',');

		// type-specific fields
		if (isCircle())
			;
		else if (isSlider()) {
			sb.append(getSliderType());
			sb.append('|');
			for (int i = 0; i < sliderX.length; i++) {
				sb.append(nf.format(sliderX[i])); sb.append(':');
				sb.append(nf.format(sliderY[i])); sb.append('|');
			}
			sb.setCharAt(sb.length() - 1, ',');
			sb.append(repeat); sb.append(',');
			sb.append(pixelLength); sb.append(',');
			if (edgeHitSound != null) {
				for (int i = 0; i < edgeHitSound.length; i++) {
					sb.append(edgeHitSound[i]); sb.append('|');
				}
				sb.setCharAt(sb.length() - 1, ',');
			}
			if (edgeAddition != null) {
				for (int i = 0; i < edgeAddition.length; i++) {
					sb.append(edgeAddition[i][0]); sb.append(':');
					sb.append(edgeAddition[i][1]); sb.append('|');
				}
				sb.setCharAt(sb.length() - 1, ',');
			}
		} else if (isSpinner()) {
			sb.append(endTime);
			sb.append(',');
		}

		// addition
		if (addition != null) {
			for (int i = 0; i < addition.length; i++) {
				sb.append(addition[i]); sb.append(':');
			}
			sb.append(additionCustomSampleIndex); sb.append(':');
			sb.append(additionHitSoundVolume); sb.append(':');
			if (additionHitSound != null)
				sb.append(additionHitSound);
		} else
			sb.setLength(sb.length() - 1);

		return sb.toString();
	}
}
