package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Clip {

	Sound sound;
	public Clip(String path) {
		try {
			sound = Gdx.audio.newSound(ResourceLoader.getFileHandle(path));
		} catch (GdxRuntimeException e) {
			Log.warn("error loading sound "+path+" "+e);
			//e.printStackTrace();
		}
	}

	public void stop() {
		if(sound != null)
			sound.stop();
		// TODO Auto-generated method stub
		
	}

	public void start(float volume) {
		// TODO Auto-generated method stub
		if(sound != null)
			sound.play(volume);
	}

	public void setFramePosition(int i) {
		//sound.s
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
