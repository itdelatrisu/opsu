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
import itdelatrisu.opsu.Utils;

/**
 * Controller for all music.
 */
public class MusicController {
	/** The current music track. */
	private static MusicPlayer player;

	/** The last OsuFile passed to play(). */
	private static OsuFile lastOsu;

	/** Thread for loading tracks. */
	private static Thread trackLoader;

	/** Whether the theme song is currently playing. */
	private static boolean themePlaying = false;

	/** Track pause time. */
	private static float pauseTime = 0f;

	/** Whether the current track volume is dimmed. */
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

			trackLoader = new Thread() {
				@Override
				public void run() {
					try {
						switch (Utils.getExtension(osu.audioFilename.getName())) {
						case "ogg":
							player = new SlickPlayer(osu.audioFilename);
							break;
						case "mp3":
							player = new BeadsPlayer(osu.audioFilename);
							break;
						default:
							throw new RuntimeException("Unknown music type!");
						}
						playAt((osu.previewTime > 0) ? osu.previewTime : 0, loop);
					} catch (Exception e) {
						ErrorHandler.error(String.format("Failed to load track '%s'.", osu.getFile().getPath()), e, true);
					} finally {
						trackLoader = null;
					}
				}
			};
			trackLoader.start();
		}
		lastOsu = osu;
	}

	/**
	 * Plays the current track at the given position.
	 */
	public static void playAt(final int position, final boolean loop) {
		if (trackExists()) {
			setVolume(Options.getMusicVolume() * Options.getMasterVolume());
			setPosition(position);
			pauseTime = 0f;
			if (loop)
				player.loop();
			else
				player.play();
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
		return (trackExists() && player.isPlaying());
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
			player.fadeOut(duration);
	}

	/**
	 * Returns the position in the current track, in ms.
	 * If no track is loaded, 0 will be returned.
	 */
	public static int getPosition() {
		if (isPlaying())
			return Math.max((int) (player.getPosition() + Options.getMusicOffset()), 0);
		else if (isPaused())
			return Math.max((int) (pauseTime + Options.getMusicOffset()), 0);
		else
			return 0;
	}

	/**
	 * Seeks to a position in the current track.
	 */
	public static void setPosition(int position) {
		if (trackExists())
			player.setPosition(position);
	}

	/**
	 * Sets the music volume.
	 * @param volume [0, 1]
	 */
	public static void setVolume(float volume) {
		if (trackExists())
			player.setVolume(volume);
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

		if (trackExists()) {
			player.close();
			player = null;
		}

		// reset state
		lastOsu = null;
		themePlaying = false;
		pauseTime = 0f;
		trackDimmed = false;
	}
}