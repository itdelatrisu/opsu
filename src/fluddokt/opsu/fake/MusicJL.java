package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.TimeUtils;

import javazoom.jl.decoder.*;

public class MusicJL extends AbsMusic {

	boolean setNextPosition = false;
	float nextPosition;
	float volume = 0.1f;
	boolean audioUsed = false;
	
	class PlayThread extends Thread{
		boolean started = false;
		boolean paused = true;
		boolean toStop = false;
		boolean toLoop;
		boolean inited = false;
		
		float position;//in ms
		long posUpdateTime;
		float latency;
		int sampleRate=44100;
		int channels;
		boolean initData = false;
		
		public void setPosition(float f){
			//System.out.println("setPosition "+f);
			nextPosition = f*1000;
			setNextPosition = true;
			
		}
		public float getPosition(){
			if(setNextPosition){
				return nextPosition/1000f;
			}
			//return (position+(TimeUtils.millis()-posUpdateTime))/1000f;
			return (position)/1000f-latency;
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
			inited = false;
			
			toStop = true;
			paused = true;
			
		}
		public void pause(){
			//System.out.println("MusicJL pause");
			inited = false;
		
			paused = true;
		}
		public void resumePlaying(){
			//System.out.println("MusicJL resumePlaying");
				paused = false;
		}
		public void setLoop(boolean loop){
			//System.out.println("MusicJL loop "+loop);
			toLoop = loop;
		}
		public void play(){
			//System.out.println("MusicJL play");
				paused = false;
				toStop = false;
		}
		public void run(){
			try {
				System.out.println("MusicJL Running Thread play "+file.path());
				bitstream = new Bitstream(
						//new FileInputStream(path));
					//new java.io.BufferedInputStream(
							file.read()
						//	)
				);
				position = 0;
				started=true;
				while(!toStop){
					if(paused){
						if(ad!=null){
							ad.dispose();
							audioUsed = false;
							ad=null;
						}
						inited=false;
						Thread.sleep(16);
						continue;
					}else{
						
						header = bitstream.readFrame();
						if(setNextPosition){
							System.out.println("Next Positioning: "+position+" "+nextPosition);
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
							if(toLoop){
								System.out.println("Header is null "+bitstream.header_pos());
								bitstream.closeFrame();
								bitstream.close();
								bitstream = new Bitstream(file.read());
								header = bitstream.readFrame();
								position=0;
								buf = new OutputBuffer(header.mode()==Header.SINGLE_CHANNEL?1:2, false);
								decoder = new MP3Decoder();
								decoder.setOutputBuffer(buf);
								if(header == null){
									System.out.println("MusicJL Header is null still"+bitstream.header_pos());
									break;
								}
							}else{
								toStop = true;
							}
						}else{
							if(!inited){
								inited = true;
								System.out.println("MusicJL Music Init");
								if(ad!=null && audioUsed){
									ad.dispose();
								}
								audioUsed = false;
								
								if(!initData){
									sampleRate = header.frequency();
									channels = header.mode()==Header.SINGLE_CHANNEL?1:2;
									initData=true;
									buf = new OutputBuffer(channels, false);
									decoder = new MP3Decoder();
									decoder.setOutputBuffer(buf);
									buf.reset();
									System.out.println("MusicJL initData");
									
								}
								
								
								ad = Gdx.audio.newAudioDevice(sampleRate, channels==1);
								
								//write 0's to start with to get rid of garbage
								ad.writeSamples(new short[4096], 0, 4096);
								System.out.println("Latency: "+ad.getLatency()+" sr:"+sampleRate);
								latency = ad.getLatency()/sampleRate;
							}
							if(header!=null && inited){
								decoder.decodeFrame(header, bitstream);
								int len = buf.reset()/2;
								
								ad.setVolume(volume);
								//header.
								posUpdateTime = TimeUtils.millis();
								//System.out.println("Info:"+header+" "+" "+len+" "+ad.getLatency()+" "+volume+" "+TimeUtils.millis());
								System.out.print(""); //magic: fixes audio in vm linux 
								//Thread.sleep(1);
								if(len>0)
									ad.writeSamples(buf.buffer2, 0, len);//buf.channelPointer2[0]);
								audioUsed = true;
								
								position+= 1000f*len/channels/sampleRate;// header.frequency();//header.ms_per_frame();
								bitstream.closeFrame();
								//Thread.sleep(16);//Math.max(1000*len/channels/sampleRate,1));
								
							}
						}
					}
				}
			}catch (Exception e){
				e.printStackTrace();
			}
			if(ad!=null && audioUsed){
				ad.dispose();
				audioUsed = false;
			}
			toStop=true;
			paused = true;
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
		playThread.setLoop(true);
		start();
	}

	@Override
	public void play() {
		//System.out.println("Play "+file.path());
		playThread.setLoop(false);
		start();
	}
	private void start() {
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
	public String getName() {
		return file.path();
	}
	@Override
	public void pause() {
		playThread.pause();
	}

	@Override
	public void resume() {
		playThread.resumePlaying();
		start();
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

	/*float lastPosition = 0;//music.getPosition();
	float lastUpdatePosition = 0;
	long lastTime = TimeUtils.millis();
	long lastUpdateTime = TimeUtils.millis();
	float deltaTime=0;
	float avgDiff;*/
	@Override
	public float getPosition() {
		/*float thisPosition = playThread.getPosition();
		long thisTime = TimeUtils.millis();
		//float dxPosition =  thisPosition - lastPosition;
		float dxPosition2 =  thisPosition - lastUpdatePosition;
		
		float syncPosition = (thisPosition);//;
		long dxTime = thisTime - lastTime;
		
		//Whenever the time changes check the difference between that and our current time
		//sync our time to song time
		if(Math.abs(syncPosition - dxTime/1000f)>1/10f){
			lastTime = thisTime - ((long)(syncPosition*1000));
			dxTime = thisTime - lastTime;
			System.out.println("Time HARD Reset"+" "+syncPosition+" "+(dxTime/1000f) 
					+" " +(int)(syncPosition*1000-(dxTime)) 
					+" " +(int)(syncPosition*1000-(thisTime - lastTime)) 
				);
		}
		if((int)(dxPosition2*1000)!=0){// && thisTime-lastUpdateTime>8
			float diff = thisPosition*1000-(dxTime);
			avgDiff = (diff+avgDiff*9)/10;
			lastTime-=(int)(avgDiff/4);
			if((int)(avgDiff/4)>1)
				System.out.println("getPosition: mpos:"+thisPosition+"\t "+(dxTime/1000f)+"\t "+(int)(thisPosition*1000-(dxTime))+"\t "+(int)avgDiff+"\t "+lastTime);
			dxTime = thisTime - lastTime;
			lastUpdatePosition = thisPosition;
			lastUpdateTime = thisTime;
		}
		
		return dxTime/1000f;*/
		return playThread.getPosition();
	}

	@Override
	public void dispose() {
		
		System.out.println("dispose "+(file!=null?file.path():"null"));
		try {
			stop();
			playThread.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			if(ad!=null){
				if(audioUsed)
					ad.dispose();
				
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
