/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2017 Jeffrey Han
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
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.TimingPoint;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.ui.UI;

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
import org.newdawn.slick.Color;
import org.newdawn.slick.Music;
import org.newdawn.slick.MusicListener;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.util.ResourceLoader;
import org.tritonus.share.sampled.file.TAudioFileFormat;

/**
 * Controller for all music.
 */
public class MusicController {
	/** The current music track. */
	private static Music player;

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

	/** Current timing point index in the track, advanced by {@link #getBeatProgress()}. */
	private static int timingPointIndex;

	/** Last non-inherited timing point. */
	private static TimingPoint lastTimingPoint;

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
			final File audioFile = beatmap.audioFilename;
			if (!audioFile.isFile() && !ResourceLoader.resourceExists(audioFile.getPath())) {
				UI.getNotificationManager().sendBarNotification(String.format("Could not find track '%s'.", audioFile.getName()));
				return;
			}

			reset();
			Utils.gc(false);

			switch (BeatmapParser.getExtension(beatmap.audioFilename.getName())) {
			case "ogg":
			case "mp3":
				trackLoader = new Thread() {
					@Override
					public void run() {
						loadTrack(audioFile, (preview) ? beatmap.previewTime : 0, loop);
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
			// create the music player
			// NOTE: most errors from this call are suppressed, so check
			//       for SoundStore errors manually afterwards
			player = new Music(file.getPath(), true);
			if (!SoundStore.get().soundWorks()) {
				player = null;
				trackEnded = false;
				UI.getNotificationManager().sendNotification(
					"Looks like sound isn't working right now. Sorry!\n\n" +
					"Restarting the game will probably fix this.",
					Color.red
				);
				return;
			}
			player.addListener(new MusicListener() {
				@Override
				public void musicEnded(Music music) {
					if (music == player) {  // don't fire if music swapped
						trackEnded = true;
						resetTimingPoint();
					}
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
			resetTimingPoint();
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
	 * Returns the beatmap associated with the current track.
	 */
	public static Beatmap getBeatmap() { return lastBeatmap; }

	/**
	 * Gets the progress of the current beat.
	 * @return a beat progress value [0,1) where 0 marks the current beat and
	 *         1 marks the next beat, or {@code null} if no timing information
	 *         is available (e.g. music paused, no timing points)
	 */
	public static Float getBeatProgress() {
		if (!updateTimingPoint())
			return null;

		// calculate beat progress
		int trackPosition = Math.max(0, getPosition(false));
		double beatLength = lastTimingPoint.getBeatLength() * 100.0;
		int beatTime = lastTimingPoint.getTime();
		if (trackPosition < beatTime)
			trackPosition += (beatLength / 100.0) * (beatTime / lastTimingPoint.getBeatLength());
		return (float) ((((trackPosition - beatTime) * 100.0) % beatLength) / beatLength);
	}

	/**
	 * Gets the progress of the current measure.
	 * @return a measure progress value [0,1) where 0 marks the start of the measure and
	 *         1 marks the start of the next measure, or {@code null} if no timing information
	 *         is available (e.g. music paused, no timing points)
	 */
	public static Float getMeasureProgress() { return getMeasureProgress(1); }

	/**
	 * Gets the progress of the current measure.
	 * @param k the meter multiplier
	 * @return a measure progress value [0,1) where 0 marks the start of the measure and
	 *         1 marks the start of the next measure, or {@code null} if no timing information
	 *         is available (e.g. music paused, no timing points)
	 */
	public static Float getMeasureProgress(int k) {
		if (!updateTimingPoint())
			return null;

		// calculate measure progress
		int trackPosition = Math.max(0, getPosition(false));
		double measureLength = lastTimingPoint.getBeatLength() * lastTimingPoint.getMeter() * k * 100.0;
		int beatTime = lastTimingPoint.getTime();
		if (trackPosition < beatTime)
			trackPosition += (measureLength / 100.0) * (beatTime / lastTimingPoint.getBeatLength());
		return (float) ((((trackPosition - beatTime) * 100.0) % measureLength) / measureLength);
	}

	/**
	 * Updates the timing point information for the current track position.
	 * @return {@code false} if timing point information is not available, {@code true} otherwise
	 */
	private static boolean updateTimingPoint() {
		Beatmap map = getBeatmap();
		if (!isPlaying() || map == null || map.timingPoints == null || map.timingPoints.isEmpty())
			return false;

		// initialization
		if (timingPointIndex == 0 && lastTimingPoint == null && !map.timingPoints.isEmpty()) {
			TimingPoint timingPoint = map.timingPoints.get(0);
			if (!timingPoint.isInherited())
				lastTimingPoint = timingPoint;
		}

		// advance timing point index, record last non-inherited timing point
		int trackPosition = getPosition(false);
		for (int i = timingPointIndex + 1; i < map.timingPoints.size(); i++) {
			TimingPoint timingPoint = map.timingPoints.get(i);
			if (trackPosition < timingPoint.getTime())
				break;
			timingPointIndex = i;
			if (!timingPoint.isInherited() && timingPoint.getBeatLength() > 0)
				lastTimingPoint = timingPoint;
		}
		if (lastTimingPoint == null)
			return false;  // no timing info

		return true;
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
		if (trackExists()) {
			pauseTime = 0f;
			resetTimingPoint();
		}
	}

	/**
	 * Fades out the track.
	 * @param duration the fade time (in ms)
	 */
	public static void fadeOut(int duration) {
		if (isPlaying())
			player.fade(duration, 0f, true);
	}

	/**
	 * Fades out the pitch (and speed) of the track.
	 * @param duration the pitch fade time (in ms)
	 */
	public static void pitchFadeOut(int duration) {
		if (isPlaying())
			player.pitchFade(duration, 0f);
	}

	/**
	 * Returns the position in the current track, in milliseconds.
	 * If no track is loaded, 0 will be returned.
	 * @param useOffsets whether to apply offsets {@link Options#getMusicOffset()}
	 *                   and {@link Beatmap#localMusicOffset} to the position
	 */
	public static int getPosition(boolean useOffsets) {
		int offset = 0;
		if (useOffsets) {
			offset += Options.getMusicOffset();
			if (lastBeatmap != null)
				offset += lastBeatmap.localMusicOffset;
		}

		if (isPlaying())
			return (int) (player.getPosition() * 1000 + offset);
		else if (isPaused())
			return Math.max((int) (pauseTime * 1000 + offset), 0);
		else
			return 0;
	}

	/**
	 * Seeks to a position in the current track.
	 * @param position the new track position (in ms)
	 */
	public static boolean setPosition(int position) {
		if (!trackExists() || position < 0)
			return false;

		resetTimingPoint();
		return (player.setPosition(position / 1000f));
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
			resetTimingPoint();
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
		SoundStore.get().setMusicVolume((isTrackDimmed()) ? volume * dimLevel : volume);
	}

	/**
	 * Sets the music pitch (and speed).
	 * @param pitch the new pitch
	 */
	public static void setPitch(float pitch) {
		if (trackExists())
			player.setPitch(pitch);
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
			play(beatmap, false, false);
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
	 * Resets timing point information.
	 */
	private static void resetTimingPoint() {
		timingPointIndex = 0;
		lastTimingPoint = null;
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
				ErrorHandler.error(null, e, true);
			}
		}
		trackLoader = null;

		// reset state
		lastBeatmap = null;
		duration = 0;
		trackEnded = false;
		themePlaying = false;
		pauseTime = 0f;
		trackDimmed = false;
		resetTimingPoint();

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

		if (!AL.isCreated())
			return;

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
			ErrorHandler.error("Failed to destroy the OpenAL context.", e, true);
		}
	}
}
