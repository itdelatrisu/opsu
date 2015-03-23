package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class ClipGDXSound extends ClipImplementation {
	Sound sound;
	long id;
	
	public ClipGDXSound(String path) {
		try {
			sound = Gdx.audio.newSound(ResourceLoader.getFileHandle(path));
		} catch (GdxRuntimeException e) {
			Log.warn("error loading sound " + path + " ", e);
		}
	}

	@Override
	public void stop() {
		if(sound != null)
			sound.stop();
	}

	@Override
	public int play(float volume, LineListener listener) {
		if(sound != null)
			return (int)sound.play(volume);
		return 0;
	}

	@Override
	public void destroy() {
		if(sound != null)
			sound.dispose();
	}
}
