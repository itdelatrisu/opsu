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

import itdelatrisu.opsu.Resources.Origin;
import itdelatrisu.opsu.Resources.SoundResource;
import itdelatrisu.opsu.states.Options.OpsuOptions;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;
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
	public enum BasicSounds implements SoundResource {
		SOUND_APPLAUSE("applause"),
		SOUND_COMBOBREAK("combobreak"),
		SOUND_COUNT1("count1s"),
		SOUND_COUNT2("count2s"),
		SOUND_COUNT3("count3s"),
		SOUND_FAIL("failsound"),
		SOUND_GO("gos"),
		SOUND_MENUBACK("menuback"),
		SOUND_MENUCLICK("menuclick"),
		SOUND_MENUHIT("menuhit"),
		SOUND_READY("readys"),
		SOUND_SECTIONFAIL("sectionfail"),
		SOUND_SECTIONPASS("sectionpass"),
		SOUND_SHUTTER("shutter"),
		SOUND_SPINNERBONUS("spinnerbonus"),
		SOUND_SPINNEROSU("spinner-osu"),
		SOUND_SPINNERSPIN("spinnerspin");
		
		final String name;
		
		private BasicSounds(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public List<String> getExtensions() {
			return Collections.singletonList(".wav");
		}

		@Override
		public List<Origin> getOrigins() {
			// TODO where should these be able to be loaded from?
			return Arrays.asList(Origin.BEATMAP, Origin.SKIN, Origin.GAME);
		}

		@Override
		public void unload(Clip data) {
			// TODO not sure if something needs to be done here
		}
	}
	
	/**
	 * Sound sample sets.
	 */
	private static final String[] sampleSets = {
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
	public static enum HitSounds {
		HIT_CLAP("hitclap"),
		HIT_FINISH("hitfinish"),
		HIT_NORMAL("hitnormal"),
		HIT_WHISTLE("hitwhistle"),
		HIT_SLIDERSLIDE("sliderslide"),
		HIT_SLIDERTICK("slidertick"),
		HIT_SLIDERWHISTLE("sliderwhistle");
		
		final String name;

		private HitSounds(String name) {
			this.name = name;
		}
	}

	/**
	 * Hit sound effects (indexed by sampleSets[], HIT_* constants).
	 */
	private static SoundResource[][] hitSounds = new SoundResource[sampleSets.length][HitSounds.values().length];
	
	static {
		for (int i = 0; i < sampleSets.length; i++) {
			for (HitSounds sound : HitSounds.values()) {
				final String fileName = String.format("%s-%s", sampleSets[i], sound.name);
				hitSounds[i][sound.ordinal()] = new SoundResource() {
					@Override
					public List<Origin> getOrigins() {
						return Arrays.asList(Origin.BEATMAP, Origin.SKIN, Origin.GAME);
					}
					
					@Override
					public String getName() {
						return fileName;
					}
					
					@Override
					public List<String> getExtensions() {
						return Collections.singletonList(".wav");
					}
					
					@Override
					public void unload(Clip data) {
						// TODO not sure if something needs to be done here
					}
				};
			}
		}
	}

	/**
	 * Sample volume multiplier, from timing points [0, 1].
	 */
	private float sampleVolumeMultiplier = 1f;

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
	 * load and play the requested sound
	 * @param resource not null
	 */
	public void playSound(SoundResource resource) {
		Clip clip = resources.getResource(resource);
		
		playClip(clip, options.getEffectVolume());
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

		SoundResource resource = null;
		
		// play all sounds
		if (hitSound == OsuHitObject.SOUND_NORMAL)
			resource = hitSounds[sampleSetIndex][HitSounds.HIT_NORMAL.ordinal()];
		else {
			if ((hitSound & OsuHitObject.SOUND_WHISTLE) > 0)
				resource = hitSounds[sampleSetIndex][HitSounds.HIT_WHISTLE.ordinal()];
			if ((hitSound & OsuHitObject.SOUND_FINISH) > 0)
				resource = hitSounds[sampleSetIndex][HitSounds.HIT_FINISH.ordinal()];
			if ((hitSound & OsuHitObject.SOUND_CLAP) > 0)
				resource = hitSounds[sampleSetIndex][HitSounds.HIT_CLAP.ordinal()];
		}
		
		if(resource != null) {
			Clip clip = resources.getResource(resource);
			if(clip != null) {
				playClip(clip, volume);
			}
		}
	}

	/**
	 * Plays a hit sound.
	 * @param sound (HIT_* constant)
	 */
	public void playHitSound(HitSounds sound) {
		if (sampleSetIndex < 0)
			return;

		SoundResource resource = hitSounds[sampleSetIndex][sound.ordinal()];
		if(resource != null) {
			Clip clip = resources.getResource(resource);
			if(clip != null) {
				playClip(clip, options.getHitSoundVolume() * sampleVolumeMultiplier);
			}
		}
	}

	private OpsuOptions options;

	private Resources resources;

	public SoundController(OpsuOptions options, Resources resources) {
		super();
		this.options = options;
		this.resources = resources;
	}
}
