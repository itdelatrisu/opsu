package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;

public class MusicGdx extends AbsMusic implements OnCompletionListener{

	com.badlogic.gdx.audio.Music music;
	String path;

	public MusicGdx(String path, AbsMusicCompleteListener lis) {
		super(lis);
		this.path = path;
		music = Gdx.audio.newMusic(ResourceLoader.getFileHandle(path));
	}

	public boolean setPosition(float f) {
		music.setPosition(f);
		return true;
	}

	public void loop() {
		if (music.isPlaying())
			music.stop();
		music.setLooping(true);
		music.play();

	}

	public void play() {
		if (music.isPlaying())
			music.stop();
		music.setLooping(false);
		music.play();
	}

	public boolean playing() {
		try {
			return music.isPlaying();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void pause() {
		music.pause();
	}

	public void resume() {
		music.play();
	}

	public void setVolume(float volume) {
		music.setVolume(volume);
	}

	public void stop() {
		music.stop();
	}

	@Override
	public float getPosition() {
		return music.getPosition();
	}

	public void dispose() {
		music.dispose();
	}

	@Override
	public void onCompletion(Music music) {
		fireMusicEnded();
	}

	@Override
	public String getName() {
		return path;
	}

	@Override
	public void setPitch(float pitch) {
	}

}
