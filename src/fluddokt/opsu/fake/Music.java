package fluddokt.opsu.fake;

import java.io.IOException;
import java.util.ArrayList;

import com.badlogic.gdx.utils.TimeUtils;

public class Music implements AbsMusicCompleteListener{

	AbsMusic music;
	ArrayList<MusicListener> musicListenerList = new ArrayList<MusicListener>();
	float pitch = 1;

	public Music(String path, boolean b) {
		if(music != null){
			music.dispose();
		}
		//if (path.toLowerCase().endsWith(".mp3"))
			try {
				music = new MusicJL3(path, this);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//else
		//	music = new MusicGdx(path, this);
		GameContainer.setMusic(this);
		SoundStore.get().setMusic(this);

	}

	public boolean setPosition(float f) {
		//System.out.println("Music setPosition " + f);
		boolean b = music.setPosition(f);
		syncPosition();
		return b;

	}

	public void loop() {
		//System.out.println("Music loop " + music.getName());
		music.loop();
		syncPosition();
	}

	public void play() {
		//System.out.println("Music play " + music.getName());
		music.play();
		syncPosition();
	}

	public boolean playing() {
		return music.playing();
	}

	public void pause() {
		//System.out.println("Music pause " + music.getName());
		music.pause();
	}

	public void resume() {
		//System.out.println("Music resume " + music.getName());
		
		music.resume();
		syncPosition();
	}

	public void setVolume(float volume) {
	}

	public void stop() {
		//System.out.println("Music stop " + music.getName());
		music.stop();
	}

	public void fade(int duration, float f, boolean b) {
		//System.out.println("Music fade " + music.getName());
		music.fade(duration, f, b);
	}

	float lastPosition = 0;// music.getPosition();
	float lastUpdatePosition = 0;
	long lastTime = TimeUtils.millis(); //assumed start time
	long lastUpdateTime = TimeUtils.millis();
	float deltaTime = 0;
	float avgDiff;
	int diffCnt = 0;

	private void syncPosition(){
		lastPosition = music.getPosition();
		lastTime = TimeUtils.millis() - (long) (lastPosition * 1000 / pitch);
		System.out.println("sync"+" "+lastPosition+" "+lastTime);
		avgDiff = 0;
		diffCnt = 0;
	}
	public float getPosition() {
		if(!music.playing())
			return music.getPosition();
		float thisPosition = music.getPosition();
		long thisTime = TimeUtils.millis();
		// float dxPosition = thisPosition - lastPosition;
		float dxPosition2 = thisPosition - lastUpdatePosition;

		float syncPosition = (thisPosition);// ;
		float dxTime = ((thisTime - lastTime))*pitch;

		// Whenever the time changes check the difference between that and our
		// current time
		// sync our time to song time
		if (Math.abs(syncPosition - dxTime / 1000f) > 0.2f) {
			System.out.println("Time HARD Reset" + " " + syncPosition + " "
					+ (dxTime / 1000f) + " "
					+ (int) (syncPosition * 1000 - (dxTime)) + " "
					+ (int) (syncPosition * 1000 - (thisTime - lastTime)));
			syncPosition();
			dxTime = ((thisTime - lastTime)*pitch);
			avgDiff = 0;
		}
		float diff = syncPosition * 1000 - (dxTime);
		if ((int) (dxPosition2 * 1000) != 0) {// && thisTime-lastUpdateTime>8
			if(diffCnt<100)
				diffCnt++;
			
			avgDiff = ((diff + (avgDiff * (diffCnt-1))) / diffCnt);
			float minDiff = avgDiff/1;
			if(Math.abs(minDiff)>=1){
				//System.out.println("getPosition: mpos:"+thisPosition+"\t "+(dxTime/1000f)+"\t diff:"+(int)(diff)+"\t avg:"+avgDiff+"\t "+lastTime+" "+diffCnt);
				lastTime -= (int)(minDiff);
				avgDiff -=  (int)(minDiff);
				dxTime = ((thisTime - lastTime)*pitch);
				lastUpdatePosition = thisPosition;
				lastUpdateTime = thisTime;
			}
		} 

		lastGetPos = dxTime / 1000f;
		//System.out.println("getPos:"+dxTime/1000f);
		return dxTime / 1000f;
	}
	float lastGetPos;
	

	public void dispose() {
		//System.out.println("Music dispose " + music.getName());
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

	public void setMusicPitch(float pitch) {
		music.setPitch(pitch);
		this.pitch= pitch;
		syncPosition();
		System.out.println("Pitch :"+pitch);
	}

	@Override
	public void requestSync(AbsMusic mus) {
		System.out.println("Music Request Sync");
		syncPosition();
	}
}
