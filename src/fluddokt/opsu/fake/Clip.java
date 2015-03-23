package fluddokt.opsu.fake;

import java.net.URL;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Clip {

	ClipImplementation sound;
	int id;
	public Clip(String path) {
		sound = new ClipGDXSound(path);
	}

	public Clip(URL url, boolean isMP3, LineListener listener) {
		sound = new ClipGDXAudioDev(url, isMP3, listener);
	}

	public void stop() {
		if (sound != null)
			sound.stop();
	}

	public void start(float volume, LineListener listener) {
		if (sound != null)
			id = sound.play(volume, listener);
	}

	public void setFramePosition(int i) {
		// TODO Auto-generated method stub

	}

	public boolean isRunning() {
		// TODO Auto-generated method stub
		return false;
	}

	public void flush() {
		// TODO Auto-generated method stub
	}

	public void destroy() {
		sound.destroy();
		
	}

}
