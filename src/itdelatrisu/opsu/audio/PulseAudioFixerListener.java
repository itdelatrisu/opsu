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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;

/**
 * This is a LineListener that fixes a problem with the OpenJDK icedtea-sound
 * implementation for PulseAudio. When underflow of a stream happens (like
 * reaching the end of it), the PulseAudioClip fires a STOP event, but it
 * doesn't update its internal state so subsequent calls to isRunning() or
 * isActive() will return true. Calling stop explicitly fixes this and will not
 * generate any other STOP events.
 *
 * @author chanceVermilion (https://github.com/itdelatrisu/opsu/pull/252)
 */
public class PulseAudioFixerListener implements LineListener {
	/** The thread pool. */
	private static final ExecutorService executor = Executors.newCachedThreadPool();

	/** The associated clip. */
	private final Clip clip;

	/** Constructor. */
	public PulseAudioFixerListener(Clip c) {
		this.clip = c;
	}

	@Override
	public void update(LineEvent event) {
		if (event.getType().equals(Type.STOP)) {
			// Stop must be called in a separate thread in order for the
			// underflow callback to complete and not deadlock.
			executor.execute(new Runnable() {
				@Override
				public void run() {
					clip.stop();
				}
			});
		}
	}
}
