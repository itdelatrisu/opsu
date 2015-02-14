package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Clip {

	Sound sound;
	long id;

	public Clip(String path) {
		try {
			sound = Gdx.audio.newSound(ResourceLoader.getFileHandle(path));
		} catch (GdxRuntimeException e) {
			Log.warn("error loading sound " + path + " " + e);
		}
	}

	public void stop() {
		if (sound != null)
			sound.stop();
	}

	public void start(float volume) {
		if (sound != null)
			id = sound.play(volume);
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

}
