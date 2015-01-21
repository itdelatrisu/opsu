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
import java.lang.reflect.Field;
import java.nio.IntBuffer;

import javazoom.jl.converter.Converter;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.newdawn.slick.Music;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;

/**
 * Controller for all music.
 */
public class MusicController {
	/**
	 * The current music track.
	 */
	private static Music player;

	/**
	 * The last OsuFile passed to play().
	 */
	private static OsuFile lastOsu;

	/**
	 * Temporary WAV file for file conversions (to be deleted).
	 */
	private static File wavFile;

	/**
	 * Thread for loading tracks.
	 */
	private static Thread trackLoader;

	/**
	 * Whether the theme song is currently playing.
	 */
	private static boolean themePlaying = false;

	/**
	 * Track pause time.
	 */
	private static float pauseTime = 0f;

	/**
	 * Whether the current track volume is dimmed.
	 */
	private static boolean trackDimmed = false;

	// This class should not be instantiated.
	private MusicController() {}

	/**
	 * Plays an audio file at the preview position.
	 */
	public static void play(final OsuFile osu, final boolean loop) {
		if (lastOsu == null || !osu.audioFilename.equals(lastOsu.audioFilename)) {
			reset();
			System.gc();

			switch (OsuParser.getExtension(osu.audioFilename.getName())) {
			case "ogg":
				trackLoader = new Thread() {
					@Override
					public void run() {
						loadTrack(osu.audioFilename, osu.previewTime, loop);
					}
				};
				trackLoader.start();
				break;
			case "mp3":
				trackLoader = new Thread() {
					@Override
					public void run() {
						convertMp3(osu.audioFilename);
//						if (!Thread.currentThread().isInterrupted())
							loadTrack(wavFile, osu.previewTime, loop);
					}
				};
				trackLoader.start();
				break;
			default:
				break;
			}
		}
		lastOsu = osu;
	}

	/**
	 * Loads a track and plays it.
	 */
	private static void loadTrack(File file, int previewTime, boolean loop) {
		try {   // create a new player
			player = new Music(file.getPath());
			playAt((previewTime > 0) ? previewTime : 0, loop);
		} catch (Exception e) {
			ErrorHandler.error(String.format("Could not play track '%s'.", file.getName()), e, false);
		}
	}

	/**
	 * Plays the current track at the given position.
	 */
	public static void playAt(final int position, final boolean loop) {
		if (trackExists()) {
			setVolume(Options.getMusicVolume() * Options.getMasterVolume());
			player.setPosition(position / 1000f);
			pauseTime = 0f;
			if (loop)
				player.loop();
			else
				player.play();
		}
	}

	/**
	 * Converts an MP3 file to a temporary WAV file.
	 */
	private static File convertMp3(File file) {
		try {
			wavFile = File.createTempFile(".osu", ".wav", Options.TMP_DIR);
			wavFile.deleteOnExit();

			Converter converter = new Converter();
			converter.convert(file.getPath(), wavFile.getPath());
			return wavFile;
		} catch (Exception e) {
			ErrorHandler.error(String.format("Failed to play file '%s'.", file.getAbsolutePath()), e, false);
		}
		return wavFile;
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
	public static boolean trackExists() {
		return (player != null);
	}

	/**
	 * Returns the OsuFile associated with the current track.
	 */
	public static OsuFile getOsuFile() { return lastOsu; }

	/**
	 * Returns the name of the current track.
	 */
	public static String getTrackName() {
		if (!trackExists() || lastOsu == null)
			return null;
		return lastOsu.getTitle();
	}

	/**
	 * Returns the artist of the current track.
	 */
	public static String getArtistName() {
		if (!trackExists() || lastOsu == null)
			return null;
		return lastOsu.getArtist();
	}

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
			return Math.max((int) (player.getPosition() * 1000 + Options.getMusicOffset()), 0);
		else if (isPaused())
			return Math.max((int) (pauseTime * 1000 + Options.getMusicOffset()), 0);
		else
			return 0;
	}

	/**
	 * Seeks to a position in the current track.
	 */
	public static boolean setPosition(int position) {
		return (trackExists() && player.setPosition(position / 1000f));
	}

	/**
	 * Sets the music volume.
	 * @param volume [0, 1]
	 */
	public static void setVolume(float volume) {
		SoundStore.get().setMusicVolume(volume);
	}

	/**
	 * Plays the theme song.
	 */
	public static void playThemeSong() {
		OsuFile osu = Options.getOsuTheme();
		if (osu != null) {
			play(osu, true);
			themePlaying = true;
		}
	}

	/**
	 * Returns whether or not the current track, if any, is the theme song.
	 */
	public static boolean isThemePlaying() {
		return (themePlaying && trackExists());
	}

	/**
	 * Returns whether or not the volume of the current track, if any,
	 * has been dimmed.
	 */
	public static boolean isTrackDimmed() { return trackDimmed; }

	/**
	 * Toggles the volume dim state of the current track.
	 */
	public static void toggleTrackDimmed() {
		float volume = Options.getMusicVolume() * Options.getMasterVolume();
		setVolume((trackDimmed) ? volume : volume / 3f);
		trackDimmed = !trackDimmed;
	}

	/**
	 * Completely resets MusicController state.
	 * <p>
	 * Stops the current track, cancels track conversions, erases
	 * temporary files, releases OpenAL sources, and resets state.
	 */
	@SuppressWarnings("deprecation")
	public static void reset() {
		stop();

		// TODO: properly interrupt instead of using deprecated Thread.stop();
		// interrupt the conversion/track loading
		if (isTrackLoading())
//			trackLoader.interrupt();
			trackLoader.stop();
		trackLoader = null;

		// delete temporary WAV file
		if (wavFile != null) {
			wavFile.delete();
			wavFile = null;
		}

		// reset state
		lastOsu = null;
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