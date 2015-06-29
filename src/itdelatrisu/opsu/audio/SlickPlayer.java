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

package itdelatrisu.opsu.audio;

import itdelatrisu.opsu.ErrorHandler;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.newdawn.slick.Music;
import org.newdawn.slick.MusicListener;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.IntBuffer;

public class SlickPlayer extends MusicPlayer {
	private static final int OFFSET = 75;

	private Music player;

	@Override
	public void load(File file) {
		try {
			player = new Music(file.getPath(), true);
		} catch (SlickException e) {
			ErrorHandler.error(String.format("Could not play track '%s'.", file.getName()), e, false);
		}

		setVolume(volume);
	}

	@Override
	public void loop() {
		player.loop();
	}

	@Override
	public void play() {
		player.play();
	}

	@Override
	public void setPosition(int time) {
		player.setPosition((time) / 1000f);
	}

	@Override
	public int getPosition() {
		return (int) (player.getPosition() * 1000) - OFFSET;
	}

	@Override
	public boolean isPlaying() {
		return player != null && player.playing();
	}

	@Override
	public void stop() {
		player.stop();
	}

	@Override
	public void pause() {
		player.pause();
	}

	@Override
	public void resume() {
		player.resume();
	}

	@Override
	public void update(int delta) {
		Music.poll(delta);
	}

	@Override
	public void fade(int duration, float endVolume, boolean stopAfterFade) {
		player.fade(duration, endVolume, stopAfterFade);
	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;
		player.setVolume(volume);
	}

	@Override
	public void setSpeed(float speed) {
		SoundStore.get().setMusicPitch(speed);
	}

	@Override
	public void setPitch(float pitch) {
		// Not implemented
	}

	@Override
	public void close() {
		if (player != null) destroyOpenAL();
	}

	/**
	 * Stops and releases all sources, clears each of the specified Audio
	 * buffers, destroys the OpenAL context, and resets SoundStore for future use.
	 * <p/>
	 * Calling SoundStore.get().init() will re-initialize the OpenAL context
	 * after a call to destroyOpenAL (Note: AudioLoader.getXXX calls init for you).
	 *
	 * @author davedes (http://slick.ninjacave.com/forum/viewtopic.php?t=3920)
	 */
	private void destroyOpenAL() {
		stop();

		try {
			// get Music object's (private) Audio object reference
			Field sound = player.getClass().getDeclaredField("sound");
			sound.setAccessible(true);
			Audio audio = (Audio) (sound.get(player));

			// first clear the sources allocated by SoundStore
			int max = SoundStore.get().getSourceCount();
			IntBuffer buf = BufferUtils.createIntBuffer(max);
			for (int i = 0; i < max; i++) {
				int source = SoundStore.get().getSource(i);
				buf.put(source);

				// stop and detach any buffers at this source
				AL10.alSourceStop(source);
				AL10.alSourcei(source, AL10.AL_BUFFER, 0);
			}
			buf.flip();
			AL10.alDeleteSources(buf);
			int exc = AL10.alGetError();
			if (exc != AL10.AL_NO_ERROR) {
				throw new SlickException(
						"Could not clear SoundStore sources, err: " + exc);
			}

			// delete any buffer data stored in memory, too...
			if (audio != null && audio.getBufferID() != 0) {
				buf = BufferUtils.createIntBuffer(1).put(audio.getBufferID());
				buf.flip();
				AL10.alDeleteBuffers(buf);
				exc = AL10.alGetError();
				if (exc != AL10.AL_NO_ERROR) {
					throw new SlickException("Could not clear buffer "
							+ audio.getBufferID()
							+ ", err: " + exc);
				}
			}

			// clear OpenAL
			AL.destroy();

			// reset SoundStore so that next time we create a Sound/Music, it will reinit
			SoundStore.get().clear();

			player = null;
		} catch (Exception e) {
			ErrorHandler.error("Failed to destroy OpenAL.", e, true);
		}
	}
}
