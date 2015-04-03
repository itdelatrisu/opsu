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
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuParser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.newdawn.slick.Music;
import org.newdawn.slick.MusicListener;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;
import org.tritonus.share.sampled.file.TAudioFileFormat;

/**
 * Controller for all music.
 */
public class MusicController {
	/** The current music track. */
	private static Music player;

	/** The last OsuFile passed to play(). */
	private static OsuFile lastOsu;

	/** The track duration. */
	private static int duration = 0;

	/** Thread for loading tracks. */
	private static Thread trackLoader;

	/** Whether or not the current track has ended. */
	private static boolean trackEnded;

	/** Whether the theme song is currently playing. */
	private static boolean themePlaying = false;

	/** Track pause time. */
	private static float pauseTime = 0f;

	/** Whether the current track volume is dimmed. */
	private static boolean trackDimmed = false;

	/** The track dim level, if dimmed. */
	private static float dimLevel = 1f;

	// This class should not be instantiated.
	private MusicController() {}

	/**
	 * Plays an audio file at the preview position.
	 * If the audio file is already playing, then nothing will happen.
	 * @param osu the OsuFile to play
	 * @param loop whether or not to loop the track
	 * @param preview whether to start at the preview time (true) or beginning (false)
	 */
	public static void play(final OsuFile osu, final boolean loop, final boolean preview) {
		// new track: load and play
		if (lastOsu == null || !osu.audioFilename.equals(lastOsu.audioFilename)) {
			reset();
			System.gc();

			switch (OsuParser.getExtension(osu.audioFilename.getName())) {
			case "ogg":
			case "mp3":
				trackLoader = new Thread() {
					@Override
					public void run() {
						loadTrack(osu.audioFilename, (preview) ? osu.previewTime : 0, loop);
					}
				};
				trackLoader.start();
				break;
			default:
				break;
			}
		}

		// new track position: play at position
		else if (osu.previewTime != lastOsu.previewTime)
			playAt(osu.previewTime, loop);

		lastOsu = osu;
	}

	/**
	 * Loads a track and plays it.
	 * @param file the audio file
	 * @param position the track position (in ms)
	 * @param loop whether or not to loop the track
	 */
	private static void loadTrack(File file, int position, boolean loop) {
		try {
			player = new Music(file.getPath(), true);
			player.addListener(new MusicListener() {
				@Override
				public void musicEnded(Music music) {
					if (music == player)  // don't fire if music swapped
						trackEnded = true;
				}

				@Override
				public void musicSwapped(Music music, Music newMusic) {}
			});
			playAt(position, loop);
		} catch (Exception e) {
			ErrorHandler.error(String.format("Could not play track '%s'.", file.getName()), e, false);
		}
	}

	/**
	 * Plays the current track at the given position.
	 * @param position the track position (in ms)
	 * @param loop whether or not to loop the track
	 */
	public static void playAt(final int position, final boolean loop) {
		if (trackExists()) {
			setVolume(Options.getMusicVolume() * Options.getMasterVolume());
			trackEnded = false;
			pauseTime = 0f;
			if (loop)
				player.loop();
			else
				player.play();
			if (position >= 0)
				player.setPosition(position / 1000f);
		}
	}

	/**
	 * Returns true if a track is being loaded.
	 */
	public static boolean isTrackLoading() {
		return (trackLoader != null && trackLoader.isAlive());
	}

	/**
	 * Returns true if a track is loaded.
	 */
	public static boolean trackExists() { return (player != null); }

	/**
	 * Returns the OsuFile associated with the current track.
	 */
	public static OsuFile getOsuFile() { return lastOsu; }

	/**
	 * Returns true if the current track is playing.
	 */
	public static boolean isPlaying() {
		return (trackExists() && player.playing());
	}

	/**
	 * Returns true if the current track is paused.
	 */
	public static boolean isPaused() {
		return (trackExists() && pauseTime > 0f);
	}

	/**
	 * Pauses the current track.
	 */
	public static void pause() {
		if (isPlaying()) {
			pauseTime = player.getPosition();
			player.pause();
		}
	}

	/**
	 * Resumes the current track.
	 */
	public static void resume() {
		if (trackExists()) {
			pauseTime = 0f;
			player.resume();
			player.setVolume(1.0f);
		}
	}

	/**
	 * Stops the current track.
	 */
	public static void stop() {
		if (isPlaying())
			player.stop();
		if (trackExists())
			pauseTime = 0f;
	}

	/**
	 * Fades out the track.
	 */
	public static void fadeOut(int duration) {
		if (isPlaying())
			player.fade(duration, 0f, true);
	}

	/**
	 * Returns the position in the current track, in ms.
	 * If no track is loaded, 0 will be returned.
	 */
	public static int getPosition() {
		if (isPlaying())
			return (int) (player.getPosition() * 1000 + Options.getMusicOffset());
		else if (isPaused())
			return Math.max((int) (pauseTime * 1000 + Options.getMusicOffset()), 0);
		else
			return 0;
	}

	/**
	 * Seeks to a position in the current track.
	 */
	public static boolean setPosition(int position) {
		return (trackExists() && position >= 0 && player.setPosition(position / 1000f));
	}

	/**
	 * Returns the duration of the current track, in milliseconds.
	 * Currently only works for MP3s.
	 * @return the duration, or -1 if no track exists, else the {@code endTime}
	 *         field of the OsuFile loaded
	 * @author Tom Brito (http://stackoverflow.com/a/3056161)
	 */
	public static int getDuration() {
		if (!trackExists() || lastOsu == null)
			return -1;

		if (duration == 0) {
			if (lastOsu.audioFilename.getName().endsWith(".mp3")) {
				try {
					AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(lastOsu.audioFilename);
					if (fileFormat instanceof TAudioFileFormat) {
						Map<?, ?> properties = ((TAudioFileFormat) fileFormat).properties();
						Long microseconds = (Long) properties.get("duration");
						duration = (int) (microseconds / 1000);
						return duration;
					}
				} catch (UnsupportedAudioFileException | IOException e) {}
			}
			duration = lastOsu.endTime;
		}
		return duration;
	}

	/**
	 * Plays the current track.
	 * @param loop whether or not to loop the track
	 */
	public static void play(boolean loop) {
		if (trackExists()) {
			trackEnded = false;
			if (loop)
				player.loop();
			else
				player.play();
		}
	}

	/**
	 * Sets the music volume.
	 * @param volume [0, 1]
	 */
	public static void setVolume(float volume) {
		SoundStore.get().setMusicVolume((isTrackDimmed()) ? volume * dimLevel : volume);
	}

	/**
	 * Sets the music pitch (and speed).
	 * @param pitch
	 */
	public static void setPitch(float pitch) {
		SoundStore.get().setMusicPitch(pitch);
	}

	/**
	 * Returns whether or not the current track has ended.
	 */
	public static boolean trackEnded() { return trackEnded; }

	/**
	 * Loops the current track if it has ended.
	 * @param preview whether to start at the preview time (true) or beginning (false)
	 */
	public static void loopTrackIfEnded(boolean preview) {
		if (trackEnded && trackExists())
			playAt((preview) ? lastOsu.previewTime : 0, false);
	}

	/**
	 * Plays the theme song.
	 */
	public static void playThemeSong() {
		OsuFile osu = Options.getOsuTheme();
		if (osu != null) {
			play(osu, true, false);
			themePlaying = true;
		}
	}

	/**
	 * Returns whether or not the theme song is playing.
	 */
	public static boolean isThemePlaying() { return themePlaying; }

	/**
	 * Returns whether or not the volume of the current track, if any,
	 * has been dimmed.
	 */
	public static boolean isTrackDimmed() { return trackDimmed; }

	/**
	 * Toggles the volume dim state of the current track.
	 * @param multiplier the volume multiplier when the track is dimmed
	 */
	public static void toggleTrackDimmed(float multiplier) {
		float volume = Options.getMusicVolume() * Options.getMasterVolume();
		dimLevel = (isTrackDimmed()) ? 1f : multiplier;
		trackDimmed = !trackDimmed;
		setVolume(volume);
	}

	/**
	 * Completely resets MusicController state.
	 * <p>
	 * Stops the current track, cancels track conversions, erases
	 * temporary files, releases OpenAL sources, and resets state.
	 */
	public static void reset() {
		stop();

		// interrupt the track loading
		// TODO: Not sure if the interrupt does anything, and the join kind of
		// defeats the purpose of threading it, but it is needed since bad things
		// likely happen when OpenALStreamPlayer source is released asynchronously.
		if (isTrackLoading()){
			trackLoader.interrupt();
			try {
				trackLoader.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		trackLoader = null;

		// reset state
		lastOsu = null;
		duration = 0;
		trackEnded = false;
		themePlaying = false;
		pauseTime = 0f;
		trackDimmed = false;

		// releases all sources from previous tracks
		destroyOpenAL();
	}

	/**
	 * Stops and releases all sources, clears each of the specified Audio
	 * buffers, destroys the OpenAL context, and resets SoundStore for future use.
	 *
	 * Calling SoundStore.get().init() will re-initialize the OpenAL context
	 * after a call to destroyOpenAL (Note: AudioLoader.getXXX calls init for you).
	 *
	 * @author davedes (http://slick.ninjacave.com/forum/viewtopic.php?t=3920)
	 */
	private static void destroyOpenAL() {
		if (!trackExists())
			return;
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
							+ ", err: "+exc);
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