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

import java.io.File;

/**
 * Music player interface.
 */
public abstract class MusicPlayer {
	/** The audio file. */
	protected File file;

	/**
	 * Constructor.
	 * @param file the audio file
	 */
	protected MusicPlayer(File file) {
		this.file = file;
	}

	/**
	 * Loops the track.
	 */
	public abstract void loop();

	/**
	 * Plays the track.
	 */
	public abstract void play();

	/**
	 * Sets the track position.
	 * @param time the track time, in ms
	 */
	public abstract void setPosition(int time);

	/**
	 * Gets the current track position.
	 * @return the track time, in ms
	 */
	public abstract int getPosition();

	/**
	 * Returns whether or not the track is currently playing.
	 * @return true if playing
	 */
	public abstract boolean isPlaying();

	/**
	 * Stops the track.
	 */
	public abstract void stop();

	/**
	 * Pauses the track.
	 */
	public abstract void pause();

	/**
	 * Resumes a paused track.
	 */
	public abstract void resume();

	/**
	 * Fades out the track.
	 * @param duration the fade time, in ms
	 */
	public abstract void fadeOut(int duration);

	/**
	 * Sets the track volume.
	 * @param volume the volume [0, 1]
	 */
	public abstract void setVolume(float volume);

	/**
	 * Closes and destroys the track.
	 */
	public abstract void close();
}
