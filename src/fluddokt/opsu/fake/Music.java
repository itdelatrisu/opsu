package fluddokt.opsu.fake;

import java.util.ArrayList;

import com.badlogic.gdx.utils.TimeUtils;

public class Music implements AbsMusicCompleteListener{

	AbsMusic music;
	ArrayList<MusicListener> musicListenerList = new ArrayList<MusicListener>();

	public Music(String path, boolean b) {
		if (path.toLowerCase().endsWith(".mp3"))
			music = new MusicJL(path, this);
		else
			music = new MusicGdx(path, this);
		GameContainer.setMusic(this);
		SoundStore.get().setMusic(this);

	}

	public boolean setPosition(float f) {
		System.out.println("Music setPosition " + f);
		boolean b = music.setPosition(f);
		lastPosition = music.getPosition();
		lastTime = TimeUtils.millis() - (long) (lastPosition * 1000);
		return b;

	}

	public void loop() {
		System.out.println("Music loop " + music.getName());
		music.loop();
		lastPosition = music.getPosition();
		lastTime = TimeUtils.millis();
	}

	public void play() {
		System.out.println("Music play " + music.getName());
		music.play();
		lastPosition = music.getPosition();
		lastTime = TimeUtils.millis();
	}

	public boolean playing() {
		return music.playing();
	}

	public void pause() {
		System.out.println("Music pause " + music.getName());
		music.pause();
	}

	public void resume() {
		System.out.println("Music resume " + music.getName());
		music.resume();
	}

	public void setVolume(float volume) {
	}

	public void stop() {
		System.out.println("Music stop " + music.getName());
		music.stop();
	}

	public void fade(int duration, float f, boolean b) {
		System.out.println("Music fade " + music.getName());
		music.fade(duration, f, b);
	}

	float lastPosition = 0;// music.getPosition();
	float lastUpdatePosition = 0;
	long lastTime = TimeUtils.millis();
	long lastUpdateTime = TimeUtils.millis();
	float deltaTime = 0;
	float avgDiff;

	public float getPosition() {
		float thisPosition = music.getPosition();
		long thisTime = TimeUtils.millis();
		// float dxPosition = thisPosition - lastPosition;
		float dxPosition2 = thisPosition - lastUpdatePosition;

		float syncPosition = (thisPosition);// ;
		long dxTime = thisTime - lastTime;

		// Whenever the time changes check the difference between that and our
		// current time
		// sync our time to song time
		if (Math.abs(syncPosition - dxTime / 1000f) > 1 / 2f) {
			System.out.println("Time HARD Reset" + " " + syncPosition + " "
					+ (dxTime / 1000f) + " "
					+ (int) (syncPosition * 1000 - (dxTime)) + " "
					+ (int) (syncPosition * 1000 - (thisTime - lastTime)));
			lastTime = thisTime - ((long) (syncPosition * 1000));
			dxTime = thisTime - lastTime;
			avgDiff = 0;
		}
		if ((int) (dxPosition2 * 1000) != 0) {// && thisTime-lastUpdateTime>8
			float diff = thisPosition * 1000 - (dxTime);
			avgDiff = (diff + avgDiff * 9) / 10;
			lastTime -= (int) (avgDiff / 4);
			// if((int)(avgDiff/4)>=1)
			// System.out.println("getPosition: mpos:"+thisPosition+"\t "+(dxTime/1000f)+"\t "+(int)(thisPosition*1000-(dxTime))+"\t "+(int)avgDiff+"\t "+lastTime);
			dxTime = thisTime - lastTime;
			lastUpdatePosition = thisPosition;
			lastUpdateTime = thisTime;
		}

		return dxTime / 1000f;
	}

	public void dispose() {
		System.out.println("Music dispose " + music.getName());
		music.dispose();
	}

	public void setMusicVolume(float musvolume) {
		music.setVolume(musvolume);
	}

	public void addListener(MusicListener musicListener) {
		musicListenerList.add(musicListener);
		
	}

	@Override
	public void complete(AbsMusic mus) {
		for (MusicListener musLis : musicListenerList){
			musLis.musicEnded(this);
		}
	}
}
