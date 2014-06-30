package org.newdawn.slick.openal;

import ibxm.Module;
import ibxm.OpenALMODPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

/**
 * A sound as a MOD file - can only be played as music
 * 
 * @author Kevin Glass
 */
public class MODSound extends AudioImpl {
	/** The MOD play back system */
	private static OpenALMODPlayer player = new OpenALMODPlayer();
	
	/** The module to play back */
	private Module module;
	/** The sound store this belongs to */
	private SoundStore store;
	
	/**
	 * Create a mod sound to be played back 
	 * 
	 * @param store The store this sound belongs to 
	 * @param in The input stream to read the data from
	 * @throws IOException Indicates a failure to load a sound
	 */
	public MODSound(SoundStore store, InputStream in) throws IOException {
		this.store = store;
		module = OpenALMODPlayer.loadModule(in);
	}
	
	/**
	 * @see org.newdawn.slick.openal.AudioImpl#playAsMusic(float, float, boolean)
	 */
	public int playAsMusic(float pitch, float gain, boolean loop) {
		cleanUpSource();

		player.play(module, store.getSource(0), loop, SoundStore.get().isMusicOn());
		player.setup(pitch, 1.0f);
		store.setCurrentMusicVolume(gain);
		
		store.setMOD(this);
		
		return store.getSource(0);
	}

	/**
	 * Clean up the buffers applied to the sound source
	 */
	private void cleanUpSource() {
		AL10.alSourceStop(store.getSource(0));
		IntBuffer buffer = BufferUtils.createIntBuffer(1);
		int queued = AL10.alGetSourcei(store.getSource(0), AL10.AL_BUFFERS_QUEUED);
		
		while (queued > 0)
		{
			AL10.alSourceUnqueueBuffers(store.getSource(0), buffer);
			queued--;
		}
		
		AL10.alSourcei(store.getSource(0), AL10.AL_BUFFER, 0);
	}
	
	/**
	 * Poll the streaming on the MOD
	 */
	public void poll() {
		player.update();
	}
	
	/**
	 * @see org.newdawn.slick.openal.AudioImpl#playAsSoundEffect(float, float, boolean)
	 */
	public int playAsSoundEffect(float pitch, float gain, boolean loop) {
		return -1;
	}

	/**
	 * @see org.newdawn.slick.openal.AudioImpl#stop()
	 */
	public void stop() {
		store.setMOD(null);
	}

	/**
	 * @see org.newdawn.slick.openal.AudioImpl#getPosition()
	 */
	public float getPosition() {
		throw new RuntimeException("Positioning on modules is not currently supported");
	}

	/**
	 * @see org.newdawn.slick.openal.AudioImpl#setPosition(float)
	 */
	public boolean setPosition(float position) {
		throw new RuntimeException("Positioning on modules is not currently supported");
	}
}
