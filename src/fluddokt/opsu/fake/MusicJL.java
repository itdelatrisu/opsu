package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.TimeUtils;

import javazoom.jl.decoder.*;

public class MusicJL extends AbsMusic {

	boolean setNextPosition = false;
	float nextPosition;
	float volume = 1f;
	static Object ALLock = new Object();
	
	class PlayThread extends Thread{
		boolean started = false;
		boolean paused = true;
		boolean toStop = false;
		boolean toLoop;
		boolean inited = false;
		
		float position;//in ms
		
		public void setPosition(float f){
			System.out.println("setPosition "+f);
			nextPosition = f*1000;
			setNextPosition = true;
			
		}
		public float getPosition(){
			if(setNextPosition){
				return nextPosition/1000f;
			}
			return position/1000f;
		}
		public boolean isPlaying() {
			return !paused;
		}
		public void setVolume(float nvolume) {
			volume = nvolume;
			//if(ad!=null)
			//	ad.setVolume(volume);
			
		}
		public void stopPlaying(){
			System.out.println("stopPlaying");
			inited = false;
			
			toStop = true;
			paused = true;
			
		}
		public void pause(){
			System.out.println("pause");
			inited = false;
		
			paused = true;
		}
		public void resumePlaying(){
			System.out.println("resumePlaying");
				paused = false;
		}
		public void loop(){
			System.out.println("loop");
				toLoop = true;
		}
		public void play(){
			System.out.println("play");
				paused = false;
				toStop = false;
		}
		public void run(){
			try {
				System.out.println("Running Thread play "+file.path());
				bitstream = new Bitstream(
						//new FileInputStream(path));
					file.read()
				);
				position = 0;
				started=true;
				while(!toStop){
					if(paused){
						if(ad!=null){
							ad.dispose();
							ad=null;
						}
						inited=false;
						Thread.sleep(16);
						continue;
					}
						
					header = bitstream.readFrame();
					if(setNextPosition){
						//System.out.println("Next Positioning: "+position+" "+nextPosition);
						if(position > nextPosition){
							decoder = new MP3Decoder();
							bitstream = new Bitstream(
								file.read()
							);
							decoder.setOutputBuffer(buf);
							position = 0;
							header = bitstream.readFrame();
						}
						while(position < nextPosition){
							//System.out.println("Next Positioning2: "+position+" "+nextPosition);
							bitstream.closeFrame();
							header = bitstream.readFrame();
							position+=header.ms_per_frame();
							
						}
						setNextPosition = false;
					}
					if(header == null){
						System.out.println("Header is null "+bitstream.header_pos());
						bitstream.closeFrame();
						bitstream.close();
						bitstream = new Bitstream(file.read());
						header = bitstream.readFrame();
						if(header == null){
							System.out.println("Header is null still"+bitstream.header_pos());
							break;
						}
						
					}else{
						if(!inited){
							System.out.println("Music Init");
							if(ad!=null){
								ad.dispose();
							}
							ad = Gdx.audio.newAudioDevice(header.frequency(), header.mode()==Header.SINGLE_CHANNEL);
							
								//ad.setVolume(volume);
							buf = new OutputBuffer(header.mode()==Header.SINGLE_CHANNEL?1:2, false);
							decoder = new MP3Decoder();
							decoder.setOutputBuffer(buf);
							inited = true;
						}
						if(header!=null && inited){
							decoder.decodeFrame(header, bitstream);
							int len = buf.reset()/2;
							//ad.setVolume(volume+(float)(Math.random()*0.001));
							ad.writeSamples(buf.buffer2, 0, len);//buf.channelPointer2[0]);
							position+=header.ms_per_frame();
							bitstream.closeFrame();
						}
					}
				}
			}catch (Exception e){
				e.printStackTrace();
				System.out.println("ASDF");
			}
			toStop=true;
			System.out.println("Done "+file.path());
		}
		
	}
	PlayThread playThread;
	Bitstream bitstream;
	MP3Decoder decoder;
	Header header;
	AudioDevice ad;
	OutputBuffer buf;
	FileHandle file;
	public MusicJL(String path) {
		System.out.println("New Song "+path);
		file = ResourceLoader.getFileHandle(path);
		
		if(playThread==null || playThread.toStop)
			playThread = new PlayThread();
		//if(!playThread.started)
		//	playThread.start();
		
		
	}
	@Override
	public boolean setPosition(float f) {
		
		playThread.setPosition(f);
		return true;
	}

	@Override
	public void loop() {
		// TODO Auto-generated method stub
		playThread.loop();
		play();
	}

	@Override
	public void play() {
		System.out.println("Play "+file.path());
		if(playThread.toStop)
			playThread = new PlayThread();
		playThread.play();
		if(!playThread.started)
			playThread.start();
	}

	@Override
	public boolean playing() {
		return playThread.isPlaying();
	}

	@Override
	public void pause() {
		playThread.pause();
	}

	@Override
	public void resume() {
		playThread.resumePlaying();
	}

	@Override
	public void setVolume(float volume) {
		playThread.setVolume(volume);
	}

	@Override
	public void stop() {
		playThread.stopPlaying();
	}

	@Override
	public void fade(int duration, float f, boolean b) {
		// TODO Auto-generated method stub
		super.fade(duration, f, b);
		playThread.stopPlaying();
	}

	float lastPosition = 0;//music.getPosition();
	float lastUpdatePosition = 0;
	long lastTime = TimeUtils.millis();
	float deltaTime=0;
	@Override
	public float getPosition() {
		float thisPosition = playThread.getPosition(); // 1/8
		long thisTime = TimeUtils.millis();
		//float dxPosition =  thisPosition - lastPosition;
		float dxPosition2 =  thisPosition - lastUpdatePosition;
		
		float syncPosition = (thisPosition);//;
		long dxTime = thisTime - lastTime;
		
		//Whenever the time changes check the difference between that and our current time
		//sync our time to song time
		if(Math.abs(syncPosition - dxTime/1000f)>0.1){
			lastTime = thisTime - ((long)(syncPosition*1000));
		}
		if((int)(dxPosition2*1000)!=0 && Math.abs(syncPosition - dxTime/1000f)>0){
			//System.out.println("Time Reset"+" "+syncPosition+" "+(dxTime/1000f) +" " +(syncPosition-(dxTime/1000f)) );
			
			lastTime = thisTime - ((long)(syncPosition*1000)+dxTime)/2;
			
			/*System.out.println( "Synced:"+(syncPosition*1000)
					+" dx:"+(syncPosition-lastTime)
					+" "
			);*/
			
			lastPosition = thisPosition;
			
		}
		dxTime = thisTime - lastTime;
		lastUpdatePosition = thisPosition;
		//System.out.println("getPosition: mpos:"+thisPosition+" "+(dxTime/1000f));
		
		return dxTime/1000f;
		//return playThread.getPosition();
	}

	@Override
	public void dispose() {
		System.out.println("dispose "+file.path());
		try {
			stop();
			playThread.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(ad!=null){
			if(playThread.inited)
				ad.dispose();
		}
		if(bitstream!=null){
			try {
				bitstream.close();
			} catch (BitstreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
