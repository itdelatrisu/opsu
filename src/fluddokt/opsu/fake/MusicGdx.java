package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;



public class MusicGdx extends AbsMusic{

	com.badlogic.gdx.audio.Music music;
	public MusicGdx(String path) {
		music = Gdx.audio.newMusic(ResourceLoader.getFileHandle(path));
	}

	public boolean setPosition(float f) {
		music.setPosition(f);
		lastPosition = music.getPosition();
		lastTime = TimeUtils.millis()-(long)(lastPosition*1000);
		return true;
	}

	public void loop() {
		if(music.isPlaying())
			music.stop();
		music.setLooping(true);
		music.play();
		lastPosition = music.getPosition();
		lastTime = TimeUtils.millis();
		//music.
	}

	public void play() {
		if(music.isPlaying())
			music.stop();
		music.setLooping(false);
		music.play();
		lastPosition = music.getPosition();
		lastTime = TimeUtils.millis();
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

	public void fade(int duration, float f, boolean b) {
		// TODO Auto-generated method stub
		final int dura = duration;
		/*new Thread(){
			@Override
			public void run() {
				float mult=1;
				float sub = (1/60f)/(dura/1000f);
				float volume = music.getVolume();
				while(mult>=0){
					try {
						mult-= sub;
						//System.out.println("Fade:"+mult+" "+sub);
						music.setVolume(volume*mult);
						Thread.sleep(16);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				music.stop();
			}
		}.start();*/
		music.stop();
	}

	float lastPosition = 0;//music.getPosition();
	float lastUpdatePosition = 0;
	long lastTime = TimeUtils.millis();
	float deltaTime=0;
	public float getPosition() {
		float thisPosition = music.getPosition(); // 1/8
		//System.out.println("getPosition: mpos:"+thisPosition);
		long thisTime = TimeUtils.millis();
		//float dxPosition =  thisPosition - lastPosition;
		float dxPosition2 =  thisPosition - lastUpdatePosition;
		
		float syncPosition = (lastPosition);//;
		long dxTime = thisTime - lastTime;
		//if(dxTime>1)
		//	lastTime = thisTime;
		//Whenever the time changes check the difference between that and our current time
		//sync our time to song time
		if((int)(dxPosition2*1000)!=0 && Math.abs(syncPosition - dxTime/1000f)>0){
			System.out.println("Time Reset"+" "+syncPosition+" "+(dxTime/1000f) +" " +(syncPosition-(dxTime/1000f)) );
			//dxTime = (pos*1000+dxtime)/2
			//dxTime = (pos*1000+dxtime)/2
			//lastTime = thisTime - (pos*1000+dxtime)/2
			lastTime = thisTime - ((long)(syncPosition*1000)+dxTime)/2;
			System.out.println( "ASDF:"+(syncPosition*1000)+" "+(syncPosition-lastTime)+" "
			+((long)(syncPosition*1000)-(syncPosition-lastTime)));
			//lastTime = thisTime;
			lastPosition = thisPosition;
			//setPosition(thisPosition);
			//dxTime = 0;
		}
		lastUpdatePosition = thisPosition;
		//if(Gdx.)
		return dxTime/1000f;
		
		//long thisTime = TimeUtils.millis();
		//long dxTime = thisTime - lastTime;
		//return dxTime/1000f;
		//Gdx.graphics.getDeltaTime();
		//return music.getPosition();
		
	}

	public void dispose() {
		music.dispose();
	}

}
