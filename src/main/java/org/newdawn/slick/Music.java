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

package org.newdawn.slick;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.AudioImpl;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.util.Log;

/**
 * A piece of music loaded and playable within the game. Only one piece of music can
 * play at any given time and a channel is reserved so music will always play. 
 *
 * @author kevin
 * @author Nathan Sweet {@literal <misc@n4te.com>}
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Music {
	/** The music currently being played or null if none */
	private static Music currentMusic;
	
	/** The lock object for synchronized modification to Music*/
	private static Object musicLock = new Object();
	
	/**
	 * Poll the state of the current music. This causes streaming music
	 * to stream and checks listeners. Note that if you're using a game container
	 * this will be auto-magically called for you.
	 * 
	 * @param delta The amount of time since last poll
	 */
	public static void poll(int delta) {
		synchronized (musicLock) {
			if (currentMusic != null) {
				SoundStore.get().poll(delta);
				if (!SoundStore.get().isMusicPlaying()) {
					if (!currentMusic.positioning) {
						Music oldMusic = currentMusic;
						currentMusic = null;
						oldMusic.fireMusicEnded();
					}
				} else {
					currentMusic.update(delta);
				}
			}
		}
	}
	
	/** The sound from FECK representing this music */
	private Audio sound;
	/** True if the music is playing */
	private boolean playing;
	/** The list of listeners waiting for notification that the music ended */
	private ArrayList listeners = new ArrayList();
	/** The volume of this music */
	private float volume = 1.0f;
	/** Start gain for fading in/out */
	private float fadeStartGain;
	/** End gain for fading in/out */
	private float fadeEndGain;
	/** Countdown for fading in/out */
	private int fadeTime;
	/** Duration for fading in/out */
	private int fadeDuration;
	/** True if music should be stopped after fading in/out */
	private boolean stopAfterFade;
	/** True if the music is being repositioned and it is therefore normal that it's not playing */
	private boolean positioning; 
	/** The position that was requested */
	private float requiredPosition = -1;

	/** The pitch of this music */
	private float pitch = 1.0f;
	/** Start pitch for fading pitch */
	private float pitchStart;
	/** End pitch for fading pitch */
	private float pitchEnd;
	/** Countdown for fading pitch */
	private int pitchTime;
	/** Duration for fading pitch */
	private int pitchDuration;


	/**
	 * Create and load a piece of music (either OGG or MOD/XM)
	 * 
	 * @param ref The location of the music
	 * @throws SlickException
	 */
	public Music(String ref) throws SlickException {
		this(ref, false);
	}

	/**
	 * Create and load a piece of music (either OGG or MOD/XM)
	 * 
	 * @param ref The location of the music
	 * @throws SlickException
	 */
	public Music(URL ref) throws SlickException {
		this(ref, false);
	}

	/**
	 * Create and load a piece of music (either OGG or MOD/XM)
	 * @param in The stream to read the music from 
	 * @param ref  The symbolic name of this music 
	 * @throws SlickException Indicates a failure to read the music from the stream
	 */
	public Music(InputStream in, String ref) throws SlickException {
		SoundStore.get().init();
		
		try {
			if (ref.toLowerCase().endsWith(".ogg")) {
				sound = SoundStore.get().getOgg(in);
			} else if (ref.toLowerCase().endsWith(".wav")) {
				sound = SoundStore.get().getWAV(in);
			} else if (ref.toLowerCase().endsWith(".xm") || ref.toLowerCase().endsWith(".mod")) {
				sound = SoundStore.get().getMOD(in);
			} else if (ref.toLowerCase().endsWith(".aif") || ref.toLowerCase().endsWith(".aiff")) {
				sound = SoundStore.get().getAIF(in);
			} else {
				throw new SlickException("Only .xm, .mod, .ogg, and .aif/f are currently supported.");
			}
		} catch (Exception e) {
			Log.error(e);
			throw new SlickException("Failed to load music: "+ref);
		}
	}
	
	/**
	 * Create and load a piece of music (either OGG or MOD/XM)
	 * 
	 * @param url The location of the music
	 * @param streamingHint A hint to indicate whether streaming should be used if possible
	 * @throws SlickException
	 */
	public Music(URL url, boolean streamingHint) throws SlickException {
		SoundStore.get().init();
		String ref = url.getFile();
		
		try {
			if (ref.toLowerCase().endsWith(".ogg") || ref.toLowerCase().endsWith(".mp3")) {
				if (streamingHint) {
					synchronized (musicLock) {
						sound = SoundStore.get().getOggStream(url);
					}
				} else {
					sound = SoundStore.get().getOgg(url.openStream());
				}
			} else if (ref.toLowerCase().endsWith(".wav")) {
				sound = SoundStore.get().getWAV(url.openStream());
			} else if (ref.toLowerCase().endsWith(".xm") || ref.toLowerCase().endsWith(".mod")) {
				sound = SoundStore.get().getMOD(url.openStream());
			} else if (ref.toLowerCase().endsWith(".aif") || ref.toLowerCase().endsWith(".aiff")) {
				sound = SoundStore.get().getAIF(url.openStream());
			} else {
				throw new SlickException("Only .xm, .mod, .ogg, and .aif/f are currently supported.");
			}
		} catch (Exception e) {
			Log.error(e);
			throw new SlickException("Failed to load sound: "+url);
		}
	}
	
	/**
	 * Create and load a piece of music (either OGG or MOD/XM)
	 * 
	 * @param ref The location of the music
	 * @param streamingHint A hint to indicate whether streaming should be used if possible
	 * @throws SlickException
	 */
	public Music(String ref, boolean streamingHint) throws SlickException {
		SoundStore.get().init();
		
		try {
			if (ref.toLowerCase().endsWith(".ogg") || ref.toLowerCase().endsWith(".mp3")) {
				if (streamingHint) {
					synchronized (musicLock) {
						//getting a stream ends the current stream....
						//which may cause a MusicEnded instead of of MusicSwap
						//Not that it really matters for MusicController use
						sound = SoundStore.get().getOggStream(ref);
					}
				} else {
					sound = SoundStore.get().getOgg(ref);
				}
			} else if (ref.toLowerCase().endsWith(".wav")) {
				sound = SoundStore.get().getWAV(ref);
			} else if (ref.toLowerCase().endsWith(".xm") || ref.toLowerCase().endsWith(".mod")) {
				sound = SoundStore.get().getMOD(ref);
			} else if (ref.toLowerCase().endsWith(".aif") || ref.toLowerCase().endsWith(".aiff")) {
				sound = SoundStore.get().getAIF(ref);
			} else {
				throw new SlickException("Only .xm, .mod, .ogg, and .aif/f are currently supported.");
			}
		} catch (Exception e) {
			Log.error(e);
			throw new SlickException("Failed to load sound: "+ref);
		}
	}

	/**
	 * Add a listener to this music
	 * 
	 * @param listener The listener to add
	 */
	public void addListener(MusicListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a listener from this music
	 * 
	 * @param listener The listener to remove
	 */
	public void removeListener(MusicListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Fire notifications that this music ended
	 */
	private void fireMusicEnded() {
		playing = false;
		for (int i=0;i<listeners.size();i++) {
			((MusicListener) listeners.get(i)).musicEnded(this);
		}
	}

	/**
	 * Fire notifications that this music was swapped out
	 * 
	 * @param newMusic The new music that will be played
	 */
	private void fireMusicSwapped(Music newMusic) {
		playing = false;
		for (int i=0;i<listeners.size();i++) {
			((MusicListener) listeners.get(i)).musicSwapped(this, newMusic);
		}
	}
	/**
	 * Loop the music
	 */
	public void loop() {
		loop(1.0f, 1.0f); 
	}
	
	/**
	 * Play the music
	 */
	public void play() {
		play(1.0f, 1.0f); 
	}

	/**
	 * Play the music at a given pitch and volume
	 * 
	 * @param pitch The pitch to play the music at (1.0 = default)
	 * @param volume The volume to play the music at (1.0 = default)
	 */
	public void play(float pitch, float volume) {
		startMusic(pitch, volume, false);
	}

	/**
	 * Loop the music at a given pitch and volume
	 * 
	 * @param pitch The pitch to play the music at (1.0 = default)
	 * @param volume The volume to play the music at (1.0 = default)
	 */
	public void loop(float pitch, float volume) {
		startMusic(pitch, volume, true);
	}
	
	/**
	 * play or loop the music at a given pitch and volume
	 * @param pitch The pitch to play the music at (1.0 = default)
	 * @param volume The volume to play the music at (1.0 = default)
	 * @param loop if false the music is played once, the music is looped otherwise
	 */
	private void startMusic(float pitch, float volume, boolean loop) {
		synchronized (musicLock) {
			if (currentMusic != null) {
				currentMusic.stop();
				currentMusic.fireMusicSwapped(this);
			}
			
			if (volume < 0.0f)
				volume = 0.0f;
			if (volume > 1.0f)
				volume = 1.0f;
	
			playing = true;
			currentMusic = this;
			sound.playAsMusic(pitch, volume, loop);
			setVolume(volume);
			setPitch(pitch);
			if (requiredPosition != -1) {
				setPosition(requiredPosition);
			}
		}
	}
	
	/**
	 * Pause the music playback
	 */
	public void pause() {
		playing = false;
		AudioImpl.pauseMusic();
	}
	
	/**
	 * Stop the music playing
	 */
	public void stop() {
		synchronized (musicLock) {
			playing = false;
			sound.stop();
		}
	}
	
	/**
	 * Resume the music playback
	 */
	public void resume() {
		playing = true;
		AudioImpl.restartMusic();
	}
	
	/**
	 * Check if the music is being played
	 * 
	 * @return True if the music is being played
	 */
	public boolean playing() {
		return (currentMusic == this) && (playing);
	}
	
	/**
	 * Set the volume of the music as a factor of the global volume setting
	 * 
	 * @param volume The volume to play music at. 0 - 1, 1 is Max
	 */
	public void setVolume(float volume) {
		// Bounds check
		if(volume > 1) {
			volume = 1;
		} else if(volume < 0) {
			volume = 0;
		}
		
		this.volume = volume;
		// This sound is being played as music
		if (currentMusic == this) {
			SoundStore.get().setCurrentMusicVolume(volume);
		}
	}
	/**
	 * Set the pitch of the music as a factor of it's normal pitch
	 *
	 * @param pitch The pitch to play music at.
	 */
	public void setPitch(float pitch) {
		this.pitch = pitch;
		if (currentMusic == this) {
			SoundStore.get().setMusicPitch(pitch);
		}
	}

	/**
	 * Get the individual volume of the music
	 * @return The volume of this music, still effected by global SoundStore volume. 0 - 1, 1 is Max
	 */
	public float getVolume() {
		return volume;
	}

	/**
	 * Fade this music to the volume specified
	 * 
	 * @param duration Fade time in milliseconds.
	 * @param endVolume The target volume
	 * @param stopAfterFade True if music should be stopped after fading in/out
	 */
	public void fade (int duration, float endVolume, boolean stopAfterFade) {
		this.stopAfterFade = stopAfterFade;
		fadeStartGain = volume;
		fadeEndGain = endVolume;
		fadeDuration = duration;
		fadeTime = duration;
	}

	/**
	 * Fade the pitch and speed of this music to the pitch specified
	 *
	 * @param duration Pitch fade time in milliseconds
	 * @param endPitch The target pitch (and speed)
	 */
	public void pitchFade (int duration, float endPitch) {
		pitchStart = pitch;
		pitchEnd = endPitch;
		pitchDuration = duration;
		pitchTime = duration;
	}

	/**
	 * Update the current music applying any effects that need to updated per 
	 * tick.
	 * 
	 * @param delta The amount of time in milliseconds thats passed since last update
	 */
	void update(int delta) {
		if (!playing) {
			return;
		}
       
		if (pitchTime > 0) {
			pitchTime -= delta;
			if (pitchTime < 0) {
				pitchTime = 0;
			}
			float offset = (pitchEnd - pitchStart) * (1 - (pitchTime / (float)pitchDuration));
			setPitch(pitchStart + offset);
		}
		if (fadeTime > 0) {
			fadeTime -= delta;
			if (fadeTime <= 0) {
				fadeTime = 0;
				if (stopAfterFade) {
					stop();
					return;
				}
			}
			
			float offset = (fadeEndGain - fadeStartGain) * (1 - (fadeTime / (float)fadeDuration));
			setVolume(fadeStartGain + offset);
		}
	}

	/**
	 * Seeks to a position in the music. For streaming music, seeking before the current position causes 
	 * the stream to be reloaded.
	 * 
	 * @param position Position in seconds.
	 * @return True if the seek was successful
	 */
	public boolean setPosition(float position) {
		synchronized (musicLock) {
			if (playing) {
				requiredPosition = -1;
				
				positioning = true;
				playing = false;
				boolean result = sound.setPosition(position);
				playing = true;
				positioning = false;
	
				return result;
			} else {
				requiredPosition = position;
				return false;
			}
		}
	}

	/**
	 * The position into the sound thats being played
	 * 
	 * @return The current position in seconds.
	 */
	public float getPosition () {
		return sound.getPosition();
	}
}
