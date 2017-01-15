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

/**
 * Sound effects.
 */
public enum SoundEffect implements SoundController.SoundComponent {
	APPLAUSE ("applause"),
	COMBOBREAK ("combobreak"),
//	COUNT ("count"),  // ?
	COUNT1 ("count1s"),
	COUNT2 ("count2s"),
	COUNT3 ("count3s"),
	FAIL ("failsound"),
	GO ("gos"),
	MENUBACK ("menuback"),
	MENUCLICK ("menuclick"),
	MENUHIT ("menuhit"),
	READY ("readys"),
	SECTIONFAIL ("sectionfail"),
	SECTIONPASS ("sectionpass"),
	SHUTTER ("shutter"),
	SPINNERBONUS ("spinnerbonus"),
	SPINNEROSU ("spinner-osu"),
	SPINNERSPIN ("spinnerspin");

	/** The file name. */
	private final String filename;

	/** The Clip associated with the sound effect. */
	private MultiClip clip;

	/** Total number of sound effects. */
	public static final int SIZE = values().length;

	/**
	 * Constructor.
	 * @param filename the sound file name
	 */
	SoundEffect(String filename) {
		this.filename = filename;
	}

	/**
	 * Returns the file name.
	 * @return the file name
	 */
	public String getFileName() { return filename; }

	@Override
	public MultiClip getClip() { return clip; }

	/**
	 * Sets the Clip for the sound.
	 * @param clip the clip
	 */
	public void setClip(MultiClip clip) { this.clip = clip; }
}
