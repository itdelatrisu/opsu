package org.newdawn.slick.openal;

import java.nio.ByteBuffer;

/**
 * Data describing the sounds in a OGG file
 * 
 * @author Kevin Glass
 */
public class OggData {
	/** The data that has been read from the OGG file */
	public ByteBuffer data;
	/** The sampling rate */
	public int rate;
	/** The number of channels in the sound file */
	public int channels;
}
