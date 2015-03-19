package itdelatrisu.opsu.audio;

import itdelatrisu.opsu.ErrorHandler;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;

/**
 * Extension of Clip that allows playing multiple copies of a Clip simultaneously.
 * http://stackoverflow.com/questions/1854616/
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public class MultiClip {
	/** Maximum number of extra clips that can be created at one time. */
	private static final int MAX_CLIPS = 20;

	/** A list of all created MultiClips. */
	private static final LinkedList<MultiClip> ALL_MULTICLIPS = new LinkedList<MultiClip>();

	/** Size of a single buffer. */
	private static final int BUFFER_SIZE = 0x1000;

	/** Current number of extra clips created. */
	private static int extraClips = 0;

	/** Current number of clip-closing threads in execution. */
	private static int closingThreads = 0;

	/** A list of clips used for this audio sample. */
	private LinkedList<Clip> clips = new LinkedList<Clip>();

	/** The audio input stream. */
	private AudioInputStream audioIn;

	/** The format of this audio sample. */
	private AudioFormat format;

	/** The data for this audio sample. */
	private byte[] audioData;

	/** The name given to this clip. */
	private String name;

	/**
	 * Constructor.
	 * @param name the clip name
	 * @param audioIn the associated AudioInputStream
	 */
	public MultiClip(String name, AudioInputStream audioIn) throws IOException, LineUnavailableException {
		this.name = name;
		this.audioIn = audioIn;
		if (audioIn != null) {
			format = audioIn.getFormat();

			LinkedList<byte[]> allBufs = new LinkedList<byte[]>();

			int totalRead = 0;
			boolean hasData = true;
			while (hasData) {
				totalRead = 0;
				byte[] tbuf = new byte[BUFFER_SIZE];
				while (totalRead < tbuf.length) {
					int read = audioIn.read(tbuf, totalRead, tbuf.length - totalRead);
					if (read < 0) {
						hasData = false;
						break;
					}
					totalRead += read;
				}
				allBufs.add(tbuf);
			}

			audioData = new byte[(allBufs.size() - 1) * BUFFER_SIZE + totalRead];

			int cnt = 0;
			for (byte[] tbuf : allBufs) {
				int size = BUFFER_SIZE;
				if (cnt == allBufs.size() - 1)
					size = totalRead;
				System.arraycopy(tbuf, 0, audioData, BUFFER_SIZE * cnt, size);
				cnt++;
			}
		}
		getClip();
		ALL_MULTICLIPS.add(this);
	}

	/**
	 * Returns the name of the clip.
	 * @return the name
	 */
	public String getName() { return name; }

	/**
	 * Plays the clip with the specified volume.
	 * @param volume the volume the play at
	 * @param listener the line listener
	 */
	public void start(float volume, LineListener listener) throws LineUnavailableException {
		Clip clip = getClip();
		if (clip == null)
			return;

		// PulseAudio does not support Master Gain
		if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
			// set volume
			FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
			gainControl.setValue(dB);
		}

		if (listener != null)
			clip.addLineListener(listener);
		clip.setFramePosition(0);
		clip.start();
	}

	/**
	 * Returns a Clip that is not playing from the list.
	 * If no clip is available, then a new one is created if under MAX_CLIPS.
	 * Otherwise, an existing clip will be returned.
	 * @return the Clip to play
	 */
	private Clip getClip() throws LineUnavailableException {
		// TODO:
		// Occasionally, even when clips are being closed in a separate thread,
		// playing any clip will cause the game to hang until all clips are
		// closed.  Why?
		if (closingThreads > 0)
			return null;

		// search for existing stopped clips
		for (Iterator<Clip> iter = clips.iterator(); iter.hasNext();) {
			Clip c = iter.next();
			if (!c.isRunning()) {
				iter.remove();
				clips.add(c);
				return c;
			}
		}

		Clip c = null;
		if (extraClips >= MAX_CLIPS) {
			// use an existing clip
			if (clips.isEmpty())
				return null;
			c = clips.removeFirst();
			c.stop();
			clips.add(c);
		} else {
			// create a new clip
			c = AudioSystem.getClip();
			if (format != null)
				c.open(format, audioData, 0, audioData.length);
			clips.add(c);
			if (clips.size() != 1)
				extraClips++;
		}
		return c;
	}

	/**
	 * Destroys the MultiClip and releases all resources.
	 */
	public void destroy() {
		if (clips.size() > 0) {
			for (Clip c : clips) {
				c.stop();
				c.flush();
				c.close();
			}
			extraClips -= clips.size() - 1;
			clips = new LinkedList<Clip>();
		}
		audioData = null;
		if (audioIn != null) {
			try {
				audioIn.close();
			} catch (IOException e) {
				ErrorHandler.error(String.format("Could not close AudioInputStream for MultiClip %s.", name), e, true);
			}
		}
	}

	/**
	 * Destroys all extra clips.
	 */
	public static void destroyExtraClips() {
		if (extraClips == 0)
			return;

		// find all extra clips
		final LinkedList<Clip> clipsToClose = new LinkedList<Clip>();
		for (MultiClip mc : MultiClip.ALL_MULTICLIPS) {
			for (Iterator<Clip> iter = mc.clips.iterator(); iter.hasNext();) {
				Clip c = iter.next();
				if (mc.clips.size() > 1) {  // retain last Clip in list
					iter.remove();
					clipsToClose.add(c);
				}
			}
		}

		// close clips in a new thread
		new Thread() {
			@Override
			public void run() {
				closingThreads++;
				for (Clip c : clipsToClose) {
					c.stop();
					c.flush();
					c.close();
				}
				closingThreads--;
			}
		}.start();

		// reset extra clip count
		extraClips = 0;
	}
}
