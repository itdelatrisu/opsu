/* 
 * Copyright (c) 2002-2004 LWJGL Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are 
 * met:
 * 
 * * Redistributions of source code must retain the above copyright 
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of 
 *   its contributors may be used to endorse or promote products derived 
 *   from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.newdawn.slick.openal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat.Encoding;

import org.lwjgl.openal.AL10;

/**
 *
 * Utitlity class for loading wavefiles.
 *
 * @author Brian Matzon <brian@matzon.dk>
 * @version $Revision: 2286 $
 */
public class AiffData {
	/** actual AIFF data */
	public final ByteBuffer data;

	/** format type of data */
	public final int format;

	/** sample rate of data */
	public final int samplerate;

	/**
	 * Creates a new AiffData
	 * 
	 * @param data actual Aiffdata
	 * @param format format of Aiff data
	 * @param samplerate sample rate of data
	 */
	private AiffData(ByteBuffer data, int format, int samplerate) {
		this.data = data;
		this.format = format;
		this.samplerate = samplerate;
	}

	/**
	 * Disposes the Aiffdata
	 */
	public void dispose() {
		data.clear();
	}

	/**
	 * Creates a AiffData container from the specified url
	 * 
	 * @param path URL to file 
	 * @return AiffData containing data, or null if a failure occured
	 */
	public static AiffData create(URL path) {
		try {
			return create(
				AudioSystem.getAudioInputStream(
					new BufferedInputStream(path.openStream())));
		} catch (Exception e) {
			org.lwjgl.LWJGLUtil.log("Unable to create from: " + path);
			e.printStackTrace();
			return null;
		}		
	}
	
	/**
	 * Creates a AiffData container from the specified in the classpath
	 * 
	 * @param path path to file (relative, and in classpath) 
	 * @return AiffData containing data, or null if a failure occured
	 */
	public static AiffData create(String path) {
		return create(AiffData.class.getClassLoader().getResource(path));
	}
	
	/**
	 * Creates a AiffData container from the specified inputstream
	 * 
	 * @param is InputStream to read from 
	 * @return AiffData containing data, or null if a failure occured
	 */
	public static AiffData create(InputStream is) {
		try {
			return create(
				AudioSystem.getAudioInputStream(is));
		} catch (Exception e) {
			org.lwjgl.LWJGLUtil.log("Unable to create from inputstream");
			e.printStackTrace();
			return null;
		}		
	}	
	
	/**	
	 * Creates a AiffData container from the specified bytes
	 *
	 * @param buffer array of bytes containing the complete Aiff file
	 * @return AiffData containing data, or null if a failure occured
	 */
	public static AiffData create(byte[] buffer) {
		try {
			return create(
				AudioSystem.getAudioInputStream(
					new BufferedInputStream(new ByteArrayInputStream(buffer))));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**	
	 * Creates a AiffData container from the specified ByetBuffer.
	 * If the buffer is backed by an array, it will be used directly, 
	 * else the contents of the buffer will be copied using get(byte[]).
	 *
	 * @param buffer ByteBuffer containing sound file
	 * @return AiffData containing data, or null if a failure occured
	 */
	public static AiffData create(ByteBuffer buffer) {
		try {
			byte[] bytes = null;
			
			if(buffer.hasArray()) {
				bytes = buffer.array();
			} else {
				bytes = new byte[buffer.capacity()];
				buffer.get(bytes);
			}
			return create(bytes);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}	

	/**
	 * Creates a AiffData container from the specified stream
	 * 
	 * @param ais AudioInputStream to read from
	 * @return AiffData containing data, or null if a failure occured
	 */
	public static AiffData create(AudioInputStream ais) {
		//get format of data
		AudioFormat audioformat = ais.getFormat();
		
		// get channels
		int channels = 0;
		if (audioformat.getChannels() == 1) {
			if (audioformat.getSampleSizeInBits() == 8) {
				channels = AL10.AL_FORMAT_MONO8;
			} else if (audioformat.getSampleSizeInBits() == 16) {
				channels = AL10.AL_FORMAT_MONO16;
			} else {
				throw new RuntimeException("Illegal sample size");
			}
		} else if (audioformat.getChannels() == 2) {
			if (audioformat.getSampleSizeInBits() == 8) {
				channels = AL10.AL_FORMAT_STEREO8;
			} else if (audioformat.getSampleSizeInBits() == 16) {
				channels = AL10.AL_FORMAT_STEREO16;
			} else {
				throw new RuntimeException("Illegal sample size");
			}
		} else {
			throw new RuntimeException("Only mono or stereo is supported");
		}

		//read data into buffer
		byte[] buf =
			new byte[audioformat.getChannels()
				* (int) ais.getFrameLength()
				* audioformat.getSampleSizeInBits()
				/ 8];
		int read = 0, total = 0;
		try {
			while ((read = ais.read(buf, total, buf.length - total)) != -1
				&& total < buf.length) {
				total += read;
			}
		} catch (IOException ioe) {
			return null;
		}

		//insert data into bytebuffer
		ByteBuffer buffer = convertAudioBytes(audioformat, buf, audioformat.getSampleSizeInBits() == 16);

		//create our result
		AiffData Aiffdata =
			new AiffData(buffer, channels, (int) audioformat.getSampleRate());

		//close stream
		try {
			ais.close();
		} catch (IOException ioe) {
		}

		return Aiffdata;
	}

	/**
	 * Convert the audio bytes into the stream
	 * 
	 * @param format The audio format being decoded
	 * @param audio_bytes The audio byts
	 * @param two_bytes_data True if we using double byte data
	 * @return The byte bufer of data
	 */
	private static ByteBuffer convertAudioBytes(AudioFormat format, byte[] audio_bytes, boolean two_bytes_data) {
		ByteBuffer dest = ByteBuffer.allocateDirect(audio_bytes.length);
		dest.order(ByteOrder.nativeOrder());
		ByteBuffer src = ByteBuffer.wrap(audio_bytes);
		src.order(ByteOrder.BIG_ENDIAN);
		if (two_bytes_data) {
			ShortBuffer dest_short = dest.asShortBuffer();
			ShortBuffer src_short = src.asShortBuffer();
			while (src_short.hasRemaining())
				dest_short.put(src_short.get());
		} else {
			while (src.hasRemaining()) {
				byte b = src.get();
				if (format.getEncoding() == Encoding.PCM_SIGNED) {
					b = (byte) (b + 127);
				}
				dest.put(b);
			}
		}
		dest.rewind();
		return dest;
	}
}
