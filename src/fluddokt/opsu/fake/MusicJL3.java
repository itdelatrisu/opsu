package fluddokt.opsu.fake;

import java.io.IOException;

import com.badlogic.gdx.files.FileHandle;

import fluddokt.opsu.fake.openal.MP3InputStreamFactory;
import fluddokt.opsu.fake.openal.OggInputStreamFactory;

public class MusicJL3 extends AbsMusic implements AudioDeviceListener{

	private FileHandle file;
	AudioDevicePlayer player;
	final MusicJL3 thisMusicJL = this;

	public MusicJL3(String path, final AbsMusicCompleteListener lis) throws IOException {
		super(lis);
		file = ResourceLoader.getFileHandle(path);
		path = path.toLowerCase();
		//*
		if(path.endsWith(".mp3")){
			try {
				player = new AudioDevicePlayer2(
					new MP3InputStreamFactory(
						new FileHandleInputStreamFactory(file)
					), path);
			} catch (IOException e) {
				e.printStackTrace();
				try {
					player = new AudioDevicePlayer2(
						new OggInputStreamFactory(
							new FileHandleInputStreamFactory(file)
						), path);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		} else if(path.endsWith(".ogg")){
			try {
				player = new AudioDevicePlayer2(
					new OggInputStreamFactory(
						new FileHandleInputStreamFactory(file)
					), path);
			} catch (IOException e) {
				e.printStackTrace();
				try {
					player = new AudioDevicePlayer2(
						new MP3InputStreamFactory(
							new FileHandleInputStreamFactory(file)
						), path);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		/*/
		player = new AudioDevicePlayer3(
				new FileHandleInputStreamFactory(file)
				, path
				);
		//*/
		if(player == null)
			throw new IOException("Could not find player to play "+file);
		player.setAudioDeviceListener(this);
	}

	@Override //AudioDeviceListener
	public void complete(AudioDevicePlayer thisAudioDevicePlayer) {
		lis.complete(thisMusicJL);
	}
	

	@Override //AudioDeviceListener
	public void requestSync(AudioDevicePlayer thisAudioDevicePlayer) {
		lis.requestSync(thisMusicJL);
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
