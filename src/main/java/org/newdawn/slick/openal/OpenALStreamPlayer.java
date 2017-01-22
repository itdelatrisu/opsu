/*
 * Copyright (c) 2013, Slick2D
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Slick2D nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.newdawn.slick.openal;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.OpenALException;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * A generic tool to work on a supplied stream, pulling out PCM data and buffered it to OpenAL
 * as required.
 * 
 * @author Kevin Glass
 * @author Nathan Sweet  {@literal <misc@n4te.com>}
 * @author Rockstar play and setPosition cleanup 
 */
public class OpenALStreamPlayer {
	/** The number of buffers to maintain */
	public static final int BUFFER_COUNT = 20;  // 3
	/** The size of the sections to stream from the stream */
	private static final int sectionSize = 4096;  // 4096 * 20
	
	/** The buffer read from the data stream */
	private byte[] buffer = new byte[sectionSize];
	/** Holds the OpenAL buffer names */
	private IntBuffer bufferNames;
	/** The byte buffer passed to OpenAL containing the section */
	private ByteBuffer bufferData = BufferUtils.createByteBuffer(sectionSize);
	/** The buffer holding the names of the OpenAL buffer thats been fully played back */
	private IntBuffer unqueued = BufferUtils.createIntBuffer(1);
	/** The source we're playing back on */
    private int source;
	/** The number of buffers remaining */
    private int remainingBufferCount;
	/** True if we should loop the track */
	private boolean loop;
	/** True if we've completed streaming to buffer (but may not be done playing) */
	private boolean done = true;
	/** The stream we're currently reading from */
	private AudioInputStream audio;
	/** The source of the data */
	private String ref;
	/** The source of the data */
	private URL url;
	/** The pitch of the music */
	private float pitch;
	/** Position in seconds of the previously played buffers */
//	private float positionOffset;

	/** The stream position. */
	long streamPos = 0;

	/** The sample rate. */
	int sampleRate;

	/** The sample size. */
	int sampleSize;

	/** The play position. */
	long playedPos;

	/** The music length. */
	long musicLength = -1;

	/** The assumed time of when the music position would be 0. */
	long syncStartTime; 

	/** The last value that was returned for the music position. */
	float lastUpdatePosition = 0;

	/** The average difference between the sync time and the music position. */
	float avgDiff;

	/** The time when the music was paused. */
	long pauseTime;

	/**
	 * Create a new player to work on an audio stream
	 * 
	 * @param source The source on which we'll play the audio
	 * @param ref A reference to the audio file to stream
	 */
	public OpenALStreamPlayer(int source, String ref) {
		this.source = source;
		this.ref = ref;
		
		bufferNames = BufferUtils.createIntBuffer(BUFFER_COUNT);
		AL10.alGenBuffers(bufferNames);
	}

	/**
	 * Create a new player to work on an audio stream
	 * 
	 * @param source The source on which we'll play the audio
	 * @param url A reference to the audio file to stream
	 */
	public OpenALStreamPlayer(int source, URL url) {
		this.source = source;
		this.url = url;

		bufferNames = BufferUtils.createIntBuffer(BUFFER_COUNT);
		AL10.alGenBuffers(bufferNames);
	}
	
	/**
	 * Initialise our connection to the underlying resource
	 * 
	 * @throws IOException Indicates a failure to open the underling resource
	 */
	private void initStreams() throws IOException {
		if (audio != null) {
			audio.close();
		}

		AudioInputStream audio;

		if (url != null) {
			audio = new OggInputStream(url.openStream());
		} else {
			if (ref.toLowerCase().endsWith(".mp3")) {
				try {
					audio = new Mp3InputStream(ResourceLoader.getResourceAsStream(ref));
				} catch (IOException e) {
					// invalid MP3: check if file is actually OGG
					try {
						audio = new OggInputStream(ResourceLoader.getResourceAsStream(ref));
					} catch (IOException e1) {
						throw e;  // invalid OGG: re-throw original MP3 exception
					}
					if (audio.getRate() == 0 && audio.getChannels() == 0)
						throw e;  // likely not OGG: re-throw original MP3 exception
				}
			} else {
				audio = new OggInputStream(ResourceLoader.getResourceAsStream(ref));
				if (audio.getRate() == 0 && audio.getChannels() == 0) {
					// invalid OGG: check if file is actually MP3
					AudioInputStream audioOGG = audio;
					try {
						audio = new Mp3InputStream(ResourceLoader.getResourceAsStream(ref));
					} catch (IOException e) {
						audio = audioOGG;  // invalid MP3: keep OGG stream
					}
				}
			}
		}
		
		this.audio = audio;
		sampleRate = audio.getRate();
		if (audio.getChannels() > 1)
			sampleSize = 4; // AL10.AL_FORMAT_STEREO16
		else
			sampleSize = 2; // AL10.AL_FORMAT_MONO16
//		positionOffset = 0;
		streamPos = 0;
		playedPos = 0;
		
	}
	
	/**
	 * Get the source of this stream
	 * 
	 * @return The name of the source of string
	 */
	public String getSource() {
		return (url == null) ? ref : url.toString();
	}
	
	/**
	 * Clean up the buffers applied to the sound source
	 */
	private synchronized void removeBuffers() {
		AL10.alSourceStop(source);
		IntBuffer buffer = BufferUtils.createIntBuffer(1);

		while (AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) > 0) {
			AL10.alSourceUnqueueBuffers(source, buffer);
			buffer.clear();
		}
	}
	
	/**
	 * Start this stream playing
	 * 
	 * @param loop True if the stream should loop 
	 * @throws IOException Indicates a failure to read from the stream
	 */
	public synchronized void play(boolean loop) throws IOException {
		this.loop = loop;
		initStreams();
		
		done = false;

		AL10.alSourceStop(source);
		
		startPlayback();
		syncStartTime = getTime();
	}
	
	/**
	 * Setup the playback properties
	 * 
	 * @param pitch The pitch to play back at
	 */
	public void setup(float pitch) {
		this.pitch = pitch;
		syncPosition();
	}
	
	/**
	 * Check if the playback is complete. Note this will never
	 * return true if we're looping
	 * 
	 * @return True if we're looping
	 */
	public boolean done() {
		return done;
	}
	
	/**
	 * Poll the bufferNames - check if we need to fill the bufferNames with another
	 * section. 
	 * 
	 * Most of the time this should be reasonably quick
	 */
	public synchronized void update() {
		if (done) {
			return;
		}

		int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
		while (processed > 0) {
			unqueued.clear();
			AL10.alSourceUnqueueBuffers(source, unqueued);
			
			int bufferIndex = unqueued.get(0);

			int bufferLength = AL10.alGetBufferi(bufferIndex, AL10.AL_SIZE);

			playedPos += bufferLength;

			if (musicLength > 0 && playedPos > musicLength)
				playedPos -= musicLength;

			if (stream(bufferIndex)) {
				AL10.alSourceQueueBuffers(source, unqueued);
			} else {
				remainingBufferCount--;
				if (remainingBufferCount == 0) {
					done = true;
				}
			}
			processed--;
		}
		
		int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
		
		if (state != AL10.AL_PLAYING) {
			AL10.alSourcePlay(source);
		}
	}
	
	/**
	 * Stream some data from the audio stream to the buffer indicates by the ID
	 * 
	 * @param bufferId The ID of the buffer to fill
	 * @return True if another section was available
	 */
	public synchronized boolean stream(int bufferId) {
		try {
			int count = audio.read(buffer);
			if (count != -1) {
				streamPos += count;

				bufferData.clear();
				bufferData.put(buffer,0,count);
				bufferData.flip();

				int format = audio.getChannels() > 1 ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_MONO16;
				try {
					AL10.alBufferData(bufferId, format, bufferData, audio.getRate());
				} catch (OpenALException e) {
					Log.error("Failed to loop buffer: "+bufferId+" "+format+" "+count+" "+audio.getRate(), e);
					return false;
				}
			} else {
				if (loop) {
					musicLength = streamPos;
					initStreams();
					stream(bufferId);
				} else {
					done = true;
					return false;
				}
			}
			
			return true;
		} catch (IOException e) {
			Log.error(e);
			return false;
		}
	}

	/**
	 * Seeks to a position in the music.
	 * 
	 * @param position Position in seconds.
	 * @return True if the setting of the position was successful
	 */
	public synchronized boolean setPosition(float position) {
		try {
			long samplePos = (long) (position * sampleRate) * sampleSize;

			if (streamPos > samplePos)
				initStreams();

			long skipped = audio.skip(samplePos - streamPos);
			if (skipped >= 0)
				streamPos += skipped;
			else
				Log.warn("OpenALStreamPlayer: setPosition: failed to skip.");

			while (streamPos + buffer.length < samplePos) {
				int count = audio.read(buffer);
				if (count != -1) {
					streamPos += count;
				} else {
					if (loop) {
						initStreams();
					} else {
						done = true;
					}
					return false;
				}
			}

			playedPos = streamPos;
			syncStartTime = (long) (getTime() - (playedPos * 1000 / sampleSize / sampleRate) / pitch);

			startPlayback(); 

			return true;
		} catch (IOException e) {
			Log.error(e);
			return false;
		}
	}

	/**
	 * Starts the streaming.
	 */
	private void startPlayback() {
		removeBuffers();
		AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
		AL10.alSourcef(source, AL10.AL_PITCH, pitch);

		remainingBufferCount = BUFFER_COUNT;

		for (int i = 0; i < BUFFER_COUNT; i++) {
			stream(bufferNames.get(i));
		}

		AL10.alSourceQueueBuffers(source, bufferNames);
		AL10.alSourcePlay(source);
	}

	/**
	 * Return the current playing position in the sound
	 * 
	 * @return The current position in seconds.
	 */
	public float getALPosition() {
		float playedTime = ((float) playedPos / (float) sampleSize) / sampleRate;
		float timePosition = playedTime + AL10.alGetSourcef(source, AL11.AL_SEC_OFFSET);
		return timePosition;
	}

	/**
	 * Return the current playing position in the sound
	 * 
	 * @return The current position in seconds.
	 */
	public float getPosition() {
		float thisPosition = getALPosition();
		long thisTime = getTime();
		float dxPosition = thisPosition - lastUpdatePosition;
		float dxTime = (thisTime - syncStartTime) * pitch;

		// hard reset
		if (Math.abs(thisPosition - dxTime / 1000f) > 1 / 2f) {
			syncPosition();
			dxTime = (thisTime - syncStartTime) * pitch;
			avgDiff = 0;
		}
		if ((int) (dxPosition * 1000) != 0) { // lastPosition != thisPosition
			float diff = thisPosition * 1000 - (dxTime);

			avgDiff = (diff + avgDiff * 9) / 10;
			if (Math.abs(avgDiff) >= 1) {
				syncStartTime -= (int) (avgDiff);
				avgDiff -= (int) (avgDiff);
				dxTime = (thisTime - syncStartTime) * pitch;
			}
			lastUpdatePosition = thisPosition;
		}

		return dxTime / 1000f;
	}

	/**
	 * Synchronizes the track position.
	 */
	private void syncPosition() {
		syncStartTime = getTime() - (long) (getALPosition() * 1000 / pitch);
		avgDiff = 0;
	}

	/**
	 * Processes a track pause.
	 */
	public void pausing() {
		pauseTime = getTime();
	}

	/**
	 * Processes a track resume.
	 */
	public void resuming() {
		syncStartTime += getTime() - pauseTime;
	}
	
	/**
	 * http://wiki.lwjgl.org/index.php?title=LWJGL_Basics_4_%28Timing%29
	 * Get the time in milliseconds
	 *
	 * @return The system time in milliseconds
	 */
	public long getTime() {
	    return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}

	/**
	 * Closes the stream.
	 */
	public void close() {
		if (audio != null) {
			try {
				audio.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

