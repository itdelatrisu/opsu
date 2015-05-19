package fluddokt.opsu.fake;

import com.badlogic.gdx.files.FileHandle;

public class MusicJL3 extends AbsMusic {

	private FileHandle file;
	AudioDevicePlayer player;
	final MusicJL3 thisMusicJL = this;

	public MusicJL3(String path, final AbsMusicCompleteListener lis) {
		super(lis);
		file = ResourceLoader.getFileHandle(path);
		player = new AudioDevicePlayer(new FileHandleInputStreamFactory(file), path);
		player.setAudioDeviceListener(new AudioDeviceListener() {
			
			@Override
			public void complete(AudioDevicePlayer thisAudioDevicePlayer) {
				lis.complete(thisMusicJL);
			}
		});
	}

	@Override
	public boolean setPosition(float f) {
		return player.setPosition(f);
	}

	@Override
	public void loop() {
		player.loop();
	}

	@Override
	public void play() {
		player.play();
	}

	@Override
	public boolean playing() {
		return player.playing();
	}

	@Override
	public void pause() {
		player.pause();
	}

	@Override
	public void resume() {
		player.resume();
	}

	@Override
	public void setVolume(float volume) {
		player.setVolume(volume);
	}

	@Override
	public void stop() {
		player.stop();
	}

	@Override
	public float getPosition() {
		return player.getPosition();
	}

	@Override
	public void dispose() {
		player.dispose();
	}

	@Override
	public String getName() {
		return player.getName();
	}

	@Override
	public void setPitch(float pitch) {
		// TODO Auto-generated method stub
		player.setPitch(pitch);
	}

}
