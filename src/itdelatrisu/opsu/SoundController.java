/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014 Jeffrey Han
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

package itdelatrisu.opsu;

import itdelatrisu.opsu.states.Options.OpsuOptions;

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

import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * Controller for all sound effects.
 * Note: Uses Java Sound because OpenAL lags too much for accurate hit sounds.
 */
public class SoundController {
	/**
	 * Sound effect constants.
	 */
	public final int
		SOUND_APPLAUSE     = 0,
		SOUND_COMBOBREAK   = 1,
//		SOUND_COUNT        = ,  // ?
		SOUND_COUNT1       = 2,
		SOUND_COUNT2       = 3,
		SOUND_COUNT3       = 4,
		SOUND_FAIL         = 5,
		SOUND_GO           = 6,
		SOUND_MENUBACK     = 7,
		SOUND_MENUCLICK    = 8,
		SOUND_MENUHIT      = 9,
		SOUND_READY        = 10,
		SOUND_SECTIONFAIL  = 11,
		SOUND_SECTIONPASS  = 12,
		SOUND_SHUTTER      = 13,
		SOUND_SPINNERBONUS = 14,
		SOUND_SPINNEROSU   = 15,
		SOUND_SPINNERSPIN  = 16,
		SOUND_MAX          = 17;  // not a sound

	/**
	 * Sound effect names (indexed by SOUND_* constants).
	 */
	private final String[] soundNames = {
		"applause",
		"combobreak",
//		"count",  // ?
		"count1s",
		"count2s",
		"count3s",
		"failsound",
		"gos",
		"menuback",
		"menuclick",
		"menuhit",
		"readys",
		"sectionfail",
		"sectionpass",
		"shutter",
		"spinnerbonus",
		"spinner-osu",
		"spinnerspin",
	};

	/**
	 * Sound effects (indexed by SOUND_* constants).
	 */
	private Clip[] sounds = new Clip[SOUND_MAX];

	/**
	 * Sound sample sets.
	 */
	private final String[] sampleSets = {
		"normal",
		"soft",
		"drum",
//		"taiko"
	};

	/**
	 * Current sample set (index in sampleSet[] array).
	 */
	private int sampleSetIndex = -1;

	/**
	 * Hit sound effects.
	 */
	public final int
		HIT_CLAP          = 0,
		HIT_FINISH        = 1,
		HIT_NORMAL        = 2,
		HIT_WHISTLE       = 3,
		HIT_SLIDERSLIDE   = 4,
		HIT_SLIDERTICK    = 5,
		HIT_SLIDERWHISTLE = 6,
		HIT_MAX           = 7;  // not a sound

	/**
	 * Hit sound effect names (indexed by HIT_* constants).
	 */
	private final String[] hitSoundNames = {
		"hitclap",
		"hitfinish",
		"hitnormal",
		"hitwhistle",
		"sliderslide",
		"slidertick",
		"sliderwhistle"
	};

	/**
	 * Hit sound effects (indexed by sampleSets[], HIT_* constants).
	 */
	private Clip[][] hitSounds = new Clip[sampleSets.length][HIT_MAX];

	/**
	 * Sample volume multiplier, from timing points [0, 1].
	 */
	private float sampleVolumeMultiplier = 1f;

	/**
	 * The name of the current sound file being loaded.
	 */
	private String currentFileName;

	/**
	 * The number of the current sound file being loaded.
	 */
	private int currentFileIndex = -1;

	/**
	 * Loads and returns a Clip from a resource.
	 * @param ref the resource name
	 * @return the loaded and opened clip
	 */
	private Clip loadClip(String ref) {
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
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			Log.error(String.format("Failed to load file '%s'.", ref), e);
		}
		return null;
	}

	/**
	 * Loads all sound files.
	 */
	public void init() {
		if (options.isSoundDisabled())
			return;

		// TODO: support MP3 sounds?
		currentFileIndex = 0;

		// menu and game sounds
		for (int i = 0; i < SOUND_MAX; i++, currentFileIndex++) {
			currentFileName = String.format("%s.wav", soundNames[i]);
			sounds[i] = loadClip(currentFileName);
		}

		// hit sounds
		for (int i = 0; i < sampleSets.length; i++) {
			for (int j = 0; j < HIT_MAX; j++, currentFileIndex++) {
				currentFileName = String.format("%s-%s.wav", sampleSets[i], hitSoundNames[j]);
				hitSounds[i][j] = loadClip(currentFileName);
			}
		}

		currentFileName = null;
		currentFileIndex = -1;
	}

	/**
	 * Sets the sample set to use when playing hit sounds.
	 * @param sampleSet the sample set ("None", "Normal", "Soft", "Drum")
	 */
	public void setSampleSet(String sampleSet) {
		sampleSetIndex = -1;
		for (int i = 0; i < sampleSets.length; i++) {
			if (sampleSet.equalsIgnoreCase(sampleSets[i])) {
				sampleSetIndex = i;
				return;
			}
		}
	}

	/**
	 * Sets the sample set to use when playing hit sounds.
	 * @param sampleType the sample set (0:none, 1:normal, 2:soft, 3:drum)
	 */
	public void setSampleSet(byte sampleType) {
		if (sampleType >= 0 && sampleType <= 3)
			sampleSetIndex = sampleType - 1;
	}
	
	/**
	 * Sets the sample volume (modifies the global sample volume).
	 * @param volume the sample volume [0, 1]
	 */
	public void setSampleVolume(float volume) {
		if (volume >= 0f && volume <= 1f)
			sampleVolumeMultiplier = volume;
	}

	/**
	 * Plays a sound clip.
	 * @param clip the Clip to play
	 * @param volume the volume [0, 1]
	 */
	private void playClip(Clip clip, float volume) {
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
	 * @param sound the sound (SOUND_* constant)
	 */
	public void playSound(int sound) {
		if (sound < 0 || sound >= SOUND_MAX)
			return;

		playClip(sounds[sound], options.getEffectVolume());
	}

	/**
	 * Plays hit sound(s) using an OsuHitObject bitmask.
	 * @param hitSound the sound (bitmask)
	 */
	public void playHitSound(byte hitSound) {
		if (sampleSetIndex < 0 || hitSound < 0)
			return;

		float volume = options.getHitSoundVolume() * sampleVolumeMultiplier;
		if (volume == 0f)
			return;

		// play all sounds
		if (hitSound == OsuHitObject.SOUND_NORMAL)
			playClip(hitSounds[sampleSetIndex][HIT_NORMAL], volume);
		else {
			if ((hitSound & OsuHitObject.SOUND_WHISTLE) > 0)
				playClip(hitSounds[sampleSetIndex][HIT_WHISTLE], volume);
			if ((hitSound & OsuHitObject.SOUND_FINISH) > 0)
				playClip(hitSounds[sampleSetIndex][HIT_FINISH], volume);
			if ((hitSound & OsuHitObject.SOUND_CLAP) > 0)
				playClip(hitSounds[sampleSetIndex][HIT_CLAP], volume);
		}
	}

	/**
	 * Plays a hit sound.
	 * @param sound (HIT_* constant)
	 */
	public void playHitSound(int sound) {
		if (sampleSetIndex < 0 || sound < 0 || sound > HIT_MAX)
			return;

		playClip(hitSounds[sampleSetIndex][sound],
				options.getHitSoundVolume() * sampleVolumeMultiplier);
	}

	/**
	 * Returns the name of the current file being loaded, or null if none.
	 */
	public String getCurrentFileName() {
		return (currentFileName != null) ? currentFileName : null;
	}

	/**
	 * Returns the progress of sound loading, or -1 if not loading.
	 * @return the completion percent [0, 100] or -1
	 */
	public int getLoadingProgress() {
		if (currentFileIndex == -1)
			return -1;

		return currentFileIndex * 100 / (SOUND_MAX + (sampleSets.length * HIT_MAX));
	}
	
	private OpsuOptions options;

	public SoundController(OpsuOptions options) {
		super();
		this.options = options;
	}
}
