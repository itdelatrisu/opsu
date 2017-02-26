package itdelatrisu.opsu.audio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineEvent.Type;

/**
 * This is a LineListener that fixes a problem with the OpenJDK icedtea-sound
 * implementation for PulseAudio. When underflow of a stream happens (like
 * reaching the end of it), the PulseAudioClip fires a STOP event, but it
 * doesn't update its internal state so subsequent calls to isRunning() or
 * isActive() will return true. Calling stop explicitly fixes this and will not
 * generate any other STOP events.
 * 
 */
final class PulseAudioFixerListener implements LineListener {
	private final Clip clip;
	private static final ExecutorService executor = Executors.newCachedThreadPool();

	PulseAudioFixerListener(Clip c) {
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