/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014 Jeffrey Han
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
	/**
	 * Hit object types (bits).
	 */
	public static final int
		TYPE_CIRCLE   = 1,
		TYPE_SLIDER   = 2,
		TYPE_NEWCOMBO = 4,  // not an object
		TYPE_SPINNER  = 8;

	/**
	 * Hit sound types.
	 */
	public static final byte
		SOUND_NORMAL        = 0,
		SOUND_WHISTLE       = 2,
		SOUND_FINISH        = 4,
		SOUND_WHISTLEFINISH = 6,
		SOUND_CLAP          = 8;

	/**
	 * Slider curve types.
	 * (Deprecated: only Beziers are currently used.)
	 */
	public static final char
		SLIDER_CATMULL     = 'C',
		SLIDER_BEZIER      = 'B',
		SLIDER_LINEAR      = 'L',
		SLIDER_PASSTHROUGH = 'P';

	/**
	 * Max hit object coordinates.
	 */
	public static final int
		MAX_X = 512,
		MAX_Y = 384;

	// parsed fields (coordinates are scaled)
	public float x, y;          // start coordinates
	public int time;            // start time, in ms
	public int type;            // hit object type
	public byte hitSound;       // hit sound type
	public char sliderType;     // slider curve type (sliders only)
	public float[] sliderX;     // slider x coordinate list (sliders only)
	public float[] sliderY;     // slider y coordinate list (sliders only)
	public int repeat;          // slider repeat count (sliders only)
	public float pixelLength;   // slider pixel length (sliders only)
	public int endTime;         // end time, in ms (spinners only)

	// additional v10+ parameters not implemented...
	// addition -> sampl:add:cust:vol:hitsound
	// edge_hitsound, edge_addition (sliders only)

	// extra fields
	public int comboIndex;      // current index in Color array
	public int comboNumber;     // number to display in hit object

	/**
	 * Constructor with all required fields.
	 */
	public OsuHitObject(float x, float y, int time, int type, byte hitSound) {
		this.x = x;
		this.y = y;
		this.time = time;
		this.type = type;
		this.hitSound = hitSound;
	}

}