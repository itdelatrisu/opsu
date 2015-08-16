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
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import itdelatrisu.opsu.beatmap.BeatmapParser;
import org.tritonus.share.sampled.file.TAudioFileFormat;

/**
 * Controller for all music.
 */
public class MusicController {
	/** The current music player. */
	public static MusicPlayer player;

	/** The last beatmap passed to play(). */
	private static Beatmap lastBeatmap;

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
	 * @param beatmap the beatmap to play
	 * @param loop whether or not to loop the track
	 * @param preview whether to start at the preview time (true) or beginning (false)
	 */
	public static void play(final Beatmap beatmap, final boolean loop, final boolean preview) {
		// new track: load and play
		if (lastBeatmap == null || !beatmap.audioFilename.equals(lastBeatmap.audioFilename)) {
			reset();
			System.gc();

			switch (BeatmapParser.getExtension(beatmap.audioFilename.getName())) {
				case "ogg":
				case "mp3":
					trackLoader = new Thread() {
						@Override
						public void run() {
							loadTrack(beatmap.audioFilename, (preview) ? beatmap.previewTime : 0, loop);
						}
					};
					trackLoader.start();
					break;
				default:
					break;
			}
		}

		// new track position: play at position
		else if (beatmap.previewTime != lastBeatmap.previewTime)
			playAt(beatmap.previewTime, loop);

		lastBeatmap = beatmap;
	}

	/**
	 * Loads a track and plays it.
	 * @param file the audio file
	 * @param position the track position (in ms)
	 * @param loop whether or not to loop the track
	 */
	private static void loadTrack(File file, int position, boolean loop) {
		try {
			player.load(file);
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
			if (position >= 0)
				player.setPosition(position);
			if (loop)
				player.loop();
			else
				player.play();
		}
	}

	/**
	 * Update tick triggered by container.
	 * @delta time in ms since the last update.
	 */
	public static void update(int delta) {
		player.update(delta);
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
	 * Returns the beatmap associated with the current track.
	 */
	public static Beatmap getBeatmap() { return lastBeatmap; }

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
			player.fade(duration, 0f, true);
	}

	/**
	 * Returns the position in the current track, in milliseconds.
	 * If no track is loaded, 0 will be returned.
	 */
	public static int getPosition() {
		if (isPlaying())
			return (int) (player.getPosition() + Options.getMusicOffset());
		else if (isPaused())
			return Math.max((int) (pauseTime + Options.getMusicOffset()), 0);
		else
			return 0;
	}

	/**
	 * Seeks to a position in the current track.
	 * @param position the new track position (in ms)
	 */
	public static void setPosition(int position) {
		if (trackExists() && position >= 0)
			player.setPosition(position);
	}

	/**
	 * Returns the duration of the current track, in milliseconds.
	 * Currently only works for MP3s.
	 * @return the duration, or -1 if no track exists, else the {@code endTime}
	 *         field of the beatmap loaded
	 * @author Tom Brito (http://stackoverflow.com/a/3056161)
	 */
	public static int getDuration() {
		if (!trackExists() || lastBeatmap == null)
			return -1;

		if (duration == 0) {
			// TAudioFileFormat method only works for MP3s
			if (lastBeatmap.audioFilename.getName().endsWith(".mp3")) {
				try {
					AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(lastBeatmap.audioFilename);
					if (fileFormat instanceof TAudioFileFormat) {
						Map<?, ?> properties = ((TAudioFileFormat) fileFormat).properties();
						Long microseconds = (Long) properties.get("duration");
						duration = (int) (microseconds / 1000);
						return duration;
					}
				} catch (UnsupportedAudioFileException | IOException e) {}
			}

			// fallback: use beatmap end time (often not the track duration)
			duration = lastBeatmap.endTime;
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
	 * @param volume the new volume [0, 1]
	 */
	public static void setVolume(float volume) {
		player.setVolume(volume);
	}

	/**
	 * Sets the music speed.
	 * @param speed the new speed
	 */
	public static void setSpeed(float speed) {
		player.setSpeed(speed);
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
			playAt((preview) ? lastBeatmap.previewTime : 0, false);
	}

	/**
	 * Plays the theme song.
	 */
	public static void playThemeSong() {
		Beatmap beatmap = Options.getThemeBeatmap();
		if (beatmap != null) {
			play(beatmap, true, false);
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

		trackLoader = null;
		if (player != null) player.close();
		// reset state
		lastBeatmap = null;
		duration = 0;
		trackEnded = false;
		themePlaying = false;
		pauseTime = 0f;
		trackDimmed = false;
	}
}