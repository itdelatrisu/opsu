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
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.audio.HitSound.SampleSet;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.newdawn.slick.util.ResourceLoader;

/**
 * Controller for all (non-music) sound components.
 * Note: Uses Java Sound because OpenAL lags too much for accurate hit sounds.
 */
public class SoundController {
	/**
	 * Interface for all (non-music) sound components.
	 */
	public interface SoundComponent {
		/**
		 * Returns the Clip associated with the sound component.
		 * @return the Clip
		 */
		public Clip getClip();
	}

	/**
	 * Sample volume multiplier, from timing points [0, 1].
	 */
	private static float sampleVolumeMultiplier = 1f;

	/**
	 * The name of the current sound file being loaded.
	 */
	private static String currentFileName;

	/**
	 * The number of the current sound file being loaded.
	 */
	private static int currentFileIndex = -1;

	// This class should not be instantiated.
	private SoundController() {}

	/**
	 * Loads and returns a Clip from a resource.
	 * @param ref the resource name
	 * @return the loaded and opened clip
	 */
	private static Clip loadClip(String ref) {
		try {
			URL url = ResourceLoader.getResource(ref);
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);

			// GNU/Linux workaround
//			Clip clip = AudioSystem.getClip();
			AudioFormat format = audioIn.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			Clip clip = (Clip) AudioSystem.getLine(info);

			clip.open(audioIn);
			return clip;
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException | RuntimeException e) {
			ErrorHandler.error(String.format("Failed to load file '%s'.", ref), e, true);
		}
		return null;
	}

	/**
	 * Loads all sound files.
	 */
	public static void init() {
		if (Options.isSoundDisabled())
			return;

		// TODO: support MP3 sounds?
		currentFileIndex = 0;

		// menu and game sounds
		for (SoundEffect s : SoundEffect.values()) {
			currentFileName = String.format("%s.wav", s.getFileName());
			s.setClip(loadClip(currentFileName));
			currentFileIndex++;
		}

		// hit sounds
		for (SampleSet ss : SampleSet.values()) {
			for (HitSound s : HitSound.values()) {
				currentFileName = String.format("%s-%s.wav", ss.getName(), s.getFileName());
				s.setClip(ss, loadClip(currentFileName));
				currentFileIndex++;
			}
		}

		currentFileName = null;
		currentFileIndex = -1;
	}

	/**
	 * Sets the sample volume (modifies the global sample volume).
	 * @param volume the sample volume [0, 1]
	 */
	public static void setSampleVolume(float volume) {
		if (volume >= 0f && volume <= 1f)
			sampleVolumeMultiplier = volume;
	}

	/**
	 * Plays a sound clip.
	 * @param clip the Clip to play
	 * @param volume the volume [0, 1]
	 */
	private static void playClip(Clip clip, float volume) {
		if (clip == null)  // clip failed to load properly
			return;

		if (volume > 0f) {
			// stop clip if running
			if (clip.isRunning()) {
				clip.stop();
				clip.flush();
			}

			// PulseAudio does not support Master Gain
			if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
				// set volume
				FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
				gainControl.setValue(dB);
			}

			// play clip
			clip.setFramePosition(0);
			clip.start();
		}
	}

	/**
	 * Plays a sound.
	 * @param s the sound effect
	 */
	public static void playSound(SoundComponent s) {
		playClip(s.getClip(), Options.getEffectVolume() * Options.getMasterVolume());
	}

	/**
	 * Plays hit sound(s) using an OsuHitObject bitmask.
	 * @param hitSound the hit sound (bitmask)
	 */
	public static void playHitSound(byte hitSound) {
		if (hitSound < 0)
			return;

		float volume = Options.getHitSoundVolume() * sampleVolumeMultiplier * Options.getMasterVolume();
		if (volume == 0f)
			return;

		// play all sounds
		if (hitSound == OsuHitObject.SOUND_NORMAL)
			playClip(HitSound.NORMAL.getClip(), volume);
		else {
			if ((hitSound & OsuHitObject.SOUND_WHISTLE) > 0)
				playClip(HitSound.WHISTLE.getClip(), volume);
			if ((hitSound & OsuHitObject.SOUND_FINISH) > 0)
				playClip(HitSound.FINISH.getClip(), volume);
			if ((hitSound & OsuHitObject.SOUND_CLAP) > 0)
				playClip(HitSound.CLAP.getClip(), volume);
		}
	}

	/**
	 * Plays a hit sound.
	 * @param s the hit sound
	 */
	public static void playHitSound(SoundComponent s) {
		playClip(s.getClip(), Options.getHitSoundVolume() * sampleVolumeMultiplier * Options.getMasterVolume());
	}

	/**
	 * Returns the name of the current file being loaded, or null if none.
	 */
	public static String getCurrentFileName() {
		return (currentFileName != null) ? currentFileName : null;
	}

	/**
	 * Returns the progress of sound loading, or -1 if not loading.
	 * @return the completion percent [0, 100] or -1
	 */
	public static int getLoadingProgress() {
		if (currentFileIndex == -1)
			return -1;

		return currentFileIndex * 100 / (SoundEffect.SIZE + (HitSound.SIZE * SampleSet.SIZE));
	}
}
