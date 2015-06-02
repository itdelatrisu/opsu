package fluddokt.opsu.fake;

import java.net.URL;

public class Clip {

	ClipImplementation sound;
	int id;
	public Clip(String path) {
		sound = new ClipGDXSound(path);
	}

	public Clip(URL url, boolean isMP3, LineListener listener) {
		sound = new ClipGDXAudioDev(url, isMP3, listener);
	}

	/*
	public void stop() {
		if (sound != null)
			sound.stop();
	}
	*/

	public void start(float volume, LineListener listener) {
		if (sound != null)
			id = sound.play(volume, listener);
	}

	public void destroy() {
		sound.destroy();
		
	}

}
