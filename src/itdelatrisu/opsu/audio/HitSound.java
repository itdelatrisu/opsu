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

import fluddokt.opsu.fake.MultiClip;

import java.util.HashMap;

/**
 * Hit sounds.
 */
public enum HitSound implements SoundController.SoundComponent {
	CLAP ("hitclap"),
	FINISH ("hitfinish"),
	NORMAL ("hitnormal"),
	WHISTLE ("hitwhistle"),
	SLIDERSLIDE ("sliderslide"),
	SLIDERTICK ("slidertick"),
	SLIDERWHISTLE ("sliderwhistle");

	/** Sound sample sets. */
	public enum SampleSet {
		NORMAL ("normal", 1),
		SOFT ("soft", 2),
		DRUM ("drum", 3);
//		TAIKO ("taiko", 4);

		/** The sample set name. */
		private final String name;

		/** The sample set index. */
		private final int index;

		/** Total number of sample sets. */
		public static final int SIZE = values().length;

		/**
		 * Constructor.
		 * @param name the sample set name
		 */
		SampleSet(String name, int index) {
			this.name = name;
			this.index = index;
		}

		/**
		 * Returns the sample set name.
		 * @return the name
		 */
		public String getName() { return name; }

		/**
		 * Returns the sample set index.
		 * @return the index
		 */
		public int getIndex() { return index; }
	}

	/** Current sample set. */
	private static SampleSet currentSampleSet;

	/** Current default sample set. */
	private static SampleSet currentDefaultSampleSet = SampleSet.NORMAL;

	/** The file name. */
	private final String filename;

	/** The Clip associated with the hit sound. */
	private HashMap<SampleSet, MultiClip> clips;

	/** Total number of hit sounds. */
	public static final int SIZE = values().length;

	/**
	 * Constructor.
	 * @param filename the sound file name
	 */
	HitSound(String filename) {
		this.filename = filename;
		this.clips = new HashMap<SampleSet, MultiClip>();
	}

	/**
	 * Returns the file name.
	 * @return the file name
	 */
	public String getFileName() { return filename; }

	@Override
	public MultiClip getClip() {
		return (currentSampleSet != null) ? clips.get(currentSampleSet) : null;
	}

	/**
	 * Returns the Clip associated with the given sample set.
	 * @param s the sample set
	 */
	public MultiClip getClip(SampleSet s) { return clips.get(s); }

	/**
	 * Sets the hit sound Clip for the sample type.
	 * @param s the sample set
	 * @param clip the Clip
	 */
	public void setClip(SampleSet s, MultiClip clip) {
		clips.put(s, clip);
	}

	/**
	 * Sets the default sample set to use when playing hit sounds.
	 * @param sampleSet the sample set ("auto", "Normal", "Soft", "Drum")
	 */
	public static void setDefaultSampleSet(String sampleSet) {
		currentDefaultSampleSet = SampleSet.NORMAL;
		for (SampleSet ss : SampleSet.values()) {
			if (sampleSet.equalsIgnoreCase(ss.getName())) {
				currentDefaultSampleSet = ss;
				return;
			}
		}
	}

	/**
	 * Sets the default sample set to use when playing hit sounds.
	 * @param sampleType the sample set (0:auto, 1:normal, 2:soft, 3:drum)
	 */
	public static void setDefaultSampleSet(byte sampleType) {
		currentDefaultSampleSet = SampleSet.NORMAL;
		for (SampleSet ss : SampleSet.values()) {
			if (sampleType == ss.getIndex()) {
				currentDefaultSampleSet = ss;
				return;
			}
		}
	}

	/**
	 * Sets the sample set to use when playing hit sounds.
	 * @param sampleType the sample set (0:auto, 1:normal, 2:soft, 3:drum)
	 */
	public static void setSampleSet(byte sampleType) {
		currentSampleSet = currentDefaultSampleSet;
		for (SampleSet ss : SampleSet.values()) {
			if (sampleType == ss.getIndex()) {
				currentSampleSet = ss;
				return;
			}
		}
	}
}
