package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;



public class Music {

	com.badlogic.gdx.audio.Music music;
	public Music(String path) {
		music = Gdx.audio.newMusic(ResourceLoader.getFileHandle(path));
	}

	public boolean setPosition(float f) {
		music.setPosition(f);
		return true;
	}

	public void loop() {
		if(music.isPlaying())
			music.stop();
		music.play();
		//music.
		music.setLooping(true);
	}

	public void play() {
		music.play();
		music.setLooping(false);
	}

	public boolean playing() {
		return music.isPlaying();
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

	public void fade(int duration, float f, boolean b) {
		// TODO Auto-generated method stub
		final int dura = duration;
		new Thread(){
			@Override
			public void run() {
				float mult=1;
				float volume = music.getVolume();
				while(mult>=0){
					try {
						mult-= 0.01f;//1/60f/dura;
						music.setVolume(volume*mult);
						Thread.sleep(16);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				music.stop();
			}
		}.start();
		//music.stop();
	}

	public float getPosition() {
		return music.getPosition();
	}

}
