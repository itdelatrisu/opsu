package fluddokt.opsu.fake;

public abstract class AudioDevicePlayer {

	public abstract void setAudioDeviceListener(AudioDeviceListener audioDeviceListener);

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

	public float getPosition() {
		return 0;
	}

	public void dispose() {
		
	}

	public String getName() {
		return null;
	}

	public void setPitch(float pitch) {
		
	}

}
