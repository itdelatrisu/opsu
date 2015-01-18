/*
 * 11/19/04 1.0 moved to LGPL. 12/12/99 Added appendSamples() method for efficiency. MDM. 15/02/99 ,Java Conversion by E.B
 * ,ebsp@iname.com, JavaLayer
 * 
 * Declarations for output buffer, includes operating system implementation of the virtual Obuffer. Optional routines enabling
 * seeks and stops added by Jeff Tsay.
 * 
 * @(#) obuffer.h 1.8, last edit: 6/15/94 16:51:56
 * 
 * @(#) Copyright (C) 1993, 1994 Tobias Bading (bading@cs.tu-berlin.de)
 * 
 * @(#) Berlin University of Technology
 * 
 * Idea and first implementation for u-law output with fast downsampling by Jim Boucher (jboucher@flash.bu.edu)
 * 
 * LinuxObuffer class written by Louis P. Kruger (lpkruger@phoenix.princeton.edu)
 * ----------------------------------------------------------------------- This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU Library General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * ----------------------------------------------------------------------
 */

package javazoom.jl.decoder;

/**
 * Base Class for audio output.
 */
public class OutputBuffer {
	public static final int BUFFERSIZE = 2 * 1152; // max. 2 * 1152 samples per frame
	private static final int MAXCHANNELS = 2; // max. number of channels

	private Float replayGainScale;
	private int channels;
	private byte[] buffer;
	public short[] buffer2;
	
	private int[] channelPointer;
	public int[] channelPointer2;
	
	private boolean isBigEndian;

	public OutputBuffer (int channels, boolean isBigEndian) {
		this.channels = channels;
		this.isBigEndian = isBigEndian;
		buffer = new byte[BUFFERSIZE * channels];
		buffer2 = new short[BUFFERSIZE * channels];
		channelPointer = new int[channels];
		channelPointer2 = new int[channels];
		
		reset();
	}

	/**
	 * Takes a 16 Bit PCM sample.
	 */
	private void append (int channel, short value) {
		byte firstByte;
		byte secondByte;
		if (isBigEndian) {
			firstByte = (byte)(value >>> 8 & 0xFF);
			secondByte = (byte)(value & 0xFF);
		} else {
			firstByte = (byte)(value & 0xFF);
			secondByte = (byte)(value >>> 8 & 0xFF);
		}
		buffer[channelPointer[channel]] = firstByte;
		buffer[channelPointer[channel] + 1] = secondByte;
		channelPointer[channel] += channels * 2;
		
		buffer2[channelPointer2[channel] ] = value;
		channelPointer2[channel] += channels;
	}

	/**
	 * Takes 32 PCM samples.
	 */
	public void appendSamples (int channel, float[] f) {
		short s;
		if (replayGainScale != null) {
			for (int i = 0; i < 32;) {
				s = clip(f[i++] * replayGainScale);
				append(channel, s);
			}
		} else {
			for (int i = 0; i < 32;) {
				s = clip(f[i++]);
				append(channel, s);
			}
		}
	}

	public byte[] getBuffer () {
		return buffer;
	}

	public int reset () {
		try {
			int index = channels - 1;
			return channelPointer2[index] - index;
		} finally {
			// Points to byte location, implicitely assuming 16 bit samples.
			for (int i = 0; i < channels; i++){
				channelPointer[i] = i * 2;
				channelPointer2[i] = i;
				
			}
		}
	}

	public void setReplayGainScale (Float replayGainScale) {
		this.replayGainScale = replayGainScale;
	}

	public boolean isStereo () {
		return channelPointer[1] == 2;
	}

	// Clip to 16 bits.
	private final short clip (float sample) {
		return sample > 32767.0f ? 32767 : sample < -32768.0f ? -32768 : (short)sample;
	}

}
