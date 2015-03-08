package fluddokt.opsu.fake;

import java.util.ArrayList;

public class AbsMusic {
	
	AbsMusicCompleteListener lis;
	public AbsMusic(AbsMusicCompleteListener lis) {
		this.lis = lis;
	}

	public boolean setPosition(float f) {
		return false;
	}

	public void loop() {
	}

	public void play() {
	}

	public boolean playing() {
		return false;
	}

	public void pause() {
	}

	public void resume() {
	}

	public void setVolume(float volume) {
	}

	public void stop() {
	}

	public void fade(int duration, float f, boolean b) {
	}

	public float getPosition() {
		return 0;
	}

	public void dispose() {
	}

	public String getName() {
		return null;
	}
	protected void fireMusicEnded(){
		lis.complete(this);
	}

}
