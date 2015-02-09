/*
Copyright (c) 2013, Slick2D

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the Slick2D nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ÅgAS ISÅh AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.newdawn.slick.openal;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.sound.midi.SysexMessage;

import org.lwjgl.BufferUtils;
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
 * @author Nathan Sweet <misc@n4te.com>
 * @author Rockstar play and setPosition cleanup 
 */
public class OpenALStreamPlayer {
	/** The number of buffers to maintain */
	public static final int BUFFER_COUNT = 9;
	/** The size of the sections to stream from the stream */
	private static final int sectionSize = 4096;
	
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
	/** True if we've completed play back */
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
	//private float positionOffset;
	
	long streamPos = 0;
	int sampleRate;
	int sampleSize;
	long playedPos;
	
	long musicLength = -1;
	
	long lastUpdateTime = System.currentTimeMillis();
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
			if(ref.toLowerCase().endsWith(".mp3"))
				audio = new Mp3InputStream(ResourceLoader.getResourceAsStream(ref));
			else
				audio = new OggInputStream(ResourceLoader.getResourceAsStream(ref));
		}
		
		this.audio = audio;
		sampleRate = audio.getRate();
		if (audio.getChannels() > 1) {
			sampleSize = 4; // AL10.AL_FORMAT_STEREO16
		} else {
			sampleSize = 2; // AL10.AL_FORMAT_MONO16
		}
		//positionOffset = 0;
		streamPos = 0;
		
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
	private void removeBuffers() {
		IntBuffer buffer = BufferUtils.createIntBuffer(1);
		//int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
		
		/*while (queued > 0)
		{
			AL10.alSourceUnqueueBuffers(source, buffer);
			queued--;
		}/*/
		
		while (AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) > 0)
		{
			AL10.alSourceUnqueueBuffers(source, buffer);
			buffer.clear();
		}//*/
		
	}
	
	/**
	 * Start this stream playing
	 * 
	 * @param loop True if the stream should loop 
	 * @throws IOException Indicates a failure to read from the stream
	 */
	public void play(boolean loop) throws IOException {
		//System.out.println("play "+loop);
		this.loop = loop;
		initStreams();
		
		done = false;

		AL10.alSourceStop(source);
		//removeBuffers();
		
		startPlayback();
		//AL10.alSourcePlay(source);
	}
	
	/**
	 * Setup the playback properties
	 * 
	 * @param pitch The pitch to play back at
	 */
	public void setup(float pitch) {
		this.pitch = pitch;
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
	public void update() {
		if (done) {
			return;
		}

		int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
		while (processed > 0) {
			unqueued.clear();
			AL10.alSourceUnqueueBuffers(source, unqueued);
			
			int bufferIndex = unqueued.get(0);
			
			//float bufferLength = (AL10.alGetBufferi(bufferIndex, AL10.AL_SIZE) / sampleSize) / sampleRate;
			int bufferLength = AL10.alGetBufferi(bufferIndex, AL10.AL_SIZE);
			//positionOffset += bufferLength;
			playedPos += bufferLength;
			if(musicLength>0 && playedPos>musicLength)
				playedPos -= musicLength;
			
			unqueued.clear();
			unqueued.put(bufferIndex);
			unqueued.flip();
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
	public boolean stream(int bufferId) {
		//Thread.dumpStack();
		try {
			int count = audio.read(buffer);
			if (count != -1) {
				lastUpdateTime = System.currentTimeMillis();
				streamPos += count;
				//bufferData = BufferUtils.createByteBuffer(sectionSize);
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
			e.printStackTrace();
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
	public boolean setPosition(float position) {
		try {
			//int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
			//AL10.alSourceStop(source);
			
			long samplePos = (long) (position*sampleRate)*sampleSize;
			
			if(streamPos > samplePos){//(getPosition() > position) {
				initStreams();
			}
			//if(audio instanceof Mp3InputStream){
				long skiped = audio.skip(samplePos - streamPos);
				if(skiped>=0)
					streamPos+=skiped;
				else{
					System.out.println("Failed to skip?");
				}
			//}
			while(streamPos+buffer.length < samplePos){
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
			/*while(streamPos%sampleSize!=0){
				audio.read();
				streamPos++;
			}*/
			playedPos = streamPos;
			
			startPlayback(); 
			//if (state != AL10.AL_PLAYING) {
		   // 	AL10.alSourcePlay(source);
		    //}

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
	public float getPosition() {
		float time = ((float)playedPos/(float)sampleSize)/(float)sampleRate;
		float timePosition = time + (System.currentTimeMillis()-lastUpdateTime)/1000f;
		//System.out.println(playedPos +" "+streamPos+" "+AL10.alGetSourcef(source, AL11.AL_SAMPLE_OFFSET)+" "+System.currentTimeMillis()+" "+time+" "+sampleRate+" "+sampleSize+" "+timePosition);
		return timePosition;//AL10.alGetSourcef(source, AL11.AL_SEC_OFFSET);
	}

	public void pausing() {
		//System.out.println("Pasuing ");
		
	}

	public void resuming() {
		//System.out.println("Resuming  ");
		lastUpdateTime = System.currentTimeMillis();
	}
}

