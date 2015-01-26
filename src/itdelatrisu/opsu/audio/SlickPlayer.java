package itdelatrisu.opsu.audio;

import itdelatrisu.opsu.ErrorHandler;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.newdawn.slick.Music;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;

/**
 * Music player using Slick2D's Music class.
 * Used for playing OGGs.
 */
public class SlickPlayer extends MusicPlayer {
	/** The music player. */
	private Music player;

	/**
	 * Constructor.
	 * @param file the audio file
	 * @throws SlickException failure to load file
	 */
	public SlickPlayer(File file) throws SlickException {
		super(file);
		this.player = new Music(file.getPath());
	}

	@Override
	public void loop() { player.loop(); }

	@Override
	public void play() { player.play(); }

	@Override
	public void setPosition(int time) { player.setPosition(time / 1000f); }

	@Override
	public int getPosition() { return (int) (player.getPosition() * 1000f); }

	@Override
	public boolean isPlaying() { return player.playing(); }

	@Override
	public void stop() { player.stop(); }

	@Override
	public void pause() { player.pause(); }

	@Override
	public void resume() { player.resume(); player.setVolume(1f); }

	@Override
	public void fadeOut(int duration) { player.fade(duration, 0f, false); }

	@Override
	public void setVolume(float volume) { SoundStore.get().setMusicVolume(volume); }

	@Override
	public void close() { destroyOpenAL(); }

	/**
	 * Stops and releases all sources, clears each of the specified Audio
	 * buffers, destroys the OpenAL context, and resets SoundStore for future use.
	 *
	 * Calling SoundStore.get().init() will re-initialize the OpenAL context
	 * after a call to destroyOpenAL (Note: AudioLoader.getXXX calls init for you).
	 *
	 * @author davedes (http://slick.ninjacave.com/forum/viewtopic.php?t=3920)
	 */
	private void destroyOpenAL() {
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
							+ ", err: "+exc);
				}
			}

			// clear OpenAL
			AL.destroy();

			// reset SoundStore so that next time we create a Sound/Music, it will reinit
			SoundStore.get().clear();
		} catch (Exception e) {
			ErrorHandler.error("Failed to destroy OpenAL.", e, true);
		}
	}
}
