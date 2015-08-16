package fluddokt.opsu.fake;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;

import fluddokt.opsu.fake.openal.AudioInputStream2;
import fluddokt.opsu.fake.openal.AudioInputStreamFactory;

public class AudioDevicePlayer2 extends AudioDevicePlayer {

	private final AudioDevicePlayer2 thisAudioDevicePlayer = this;
	private boolean setNextPosition = false;
	private float nextPosition;
	private float volume = 0.1f;
	
	private PlayThread playThread;
	private AudioInputStreamFactory inputStreamFactory;
	private AudioInputStream2 currentStream;
	private Object currentStreamLock = new Object();
	private String name;
	
	private AudioDeviceListener adl;

	private boolean paused = true;
	private boolean stop = true;
	
	private boolean toLoop;

	private float position;// in ms
	private int samplePos;
	
	private float pitch = 1f;
	
	float latency;
	int sampleRate = -1;
	int channels;
	
	static int threadCount = 0;
	static Object threadCountLock = new Object();
	static int worstSleepAccuracy = 0;
	
	public void incrementThreadCount(){
		synchronized (threadCountLock) {
			threadCount++;
		}
	}
	public void decrementThreadCount(){
		synchronized (threadCountLock) {
			threadCount--;
		}
	}
	boolean initData = false;

	class PlayThread extends Thread {
		boolean toStop = false;
		boolean initedAD = false;
		boolean audioUsed = false;

		AudioDevice ad;
		public PlayThread(){
			super("PlayThread "+name);
		}
		public void run() {
			incrementThreadCount();
			System.out.println("PlayThread Running Thread " + name +" "+threadCount);
			//Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			short[] rbuf = new short[1024];
			short[] obuf = new short[rbuf.length * 2];
			short prevL = 0, prevR =0;
			short thisL = 0;
			short thisR = 0;
			
			int lastIndex = 0;
			//float prevAt = 0;
			int prevAtFP = 0; //16.16fixed point
			int pitchFP; //16.16fixed point
			adl.requestSync(thisAudioDevicePlayer);
			synchronized (currentStreamLock) {
			try {
				while (!toStop) {
						
					if (setNextPosition) {
						System.out.println("PlayThread Next Positioning: " + position + " " + nextPosition);
						int nextSamplePos = (int) (nextPosition * sampleRate) * channels;
						if(samplePos > nextSamplePos){
							resetStream();
						}
						if(nextSamplePos >= 0) {
							int skipby = nextSamplePos - samplePos;
							int actualSkip = (int) currentStream.skip(skipby);
							System.out.println("Skipby: "+skipby+" actual:"+actualSkip);
							samplePos += actualSkip;
						} else {
							samplePos = nextSamplePos;
						}
						setNextPosition = false;
						adl.requestSync(thisAudioDevicePlayer);
					}
					int len = 0;
					
					if (samplePos < 0){
						len = Math.min(rbuf.length, -samplePos);
						for (int i=0; i<len; i++)
							rbuf[i] = 0;
						System.out.println("Len :" + len);
					} else
						len = currentStream.read(rbuf);
					if (!initedAD) {
						initedAD = true;
						System.out.println("PlayThread Music Init AD "+sampleRate+" "+channels);
						if (ad != null && audioUsed) {
							ad.dispose();
						}
						audioUsed = false;
						//ad = Gdx.audio.newAudioDevice((int) (sampleRate * pitch), channels == 1);
						ad = Gdx.audio.newAudioDevice(sampleRate, channels == 1);
						//// write 0's to start with to get rid of garbage
						//ad.writeSamples(new short[0x4000], 0, 0x4000);
						//audioUsed = true;
						
						System.out.println("Latency: " + ad.getLatency() + " sr:" + sampleRate);
						latency = ad.getLatency() / sampleRate;
						adl.requestSync(thisAudioDevicePlayer);
					}
					ad.setVolume(volume);
					pitchFP = (int) (pitch*0x10000);
					if (len > 0) {
						int olen = len/channels;
						if(pitch == 1){
							ad.writeSamples(rbuf, 0, len);
						}else if(channels == 2){
							
							/*if(pitch == 1){
								useBuf = buf;
							}else if(pitch == 0.5f){
								
								for(int i=0,j=0; i<len; i+=2,j+=4){
									buf2[j] = buf[i];
									buf2[j+2] = buf[i];
									
									buf2[j+1] = buf[i+1];
									buf2[j+3] = buf[i+1];
								}
								len *=2;
								useBuf = buf2;
								
							}else if(pitch == 2f){
								len /=2;
								for(int i=0,j=0; i<len; i+=2,j+=4){
									buf2[i] = buf[j];
									buf2[i+1] = buf[j+1];
								}
								useBuf = buf2;
							}else{*/
								int olenFP = olen<<16;
								while(prevAtFP < olenFP){
									int thisIndex = -1;
									int newLen = 0;
									while(prevAtFP < olenFP & newLen<obuf.length){
										thisIndex = prevAtFP>>16;//(int)prevAt;
										int mult = prevAtFP&0xffff;//prevAt-thisIndex;
										int oneMinMult = 0x10000 - mult;//1 - mult;
										
										while(lastIndex!=thisIndex){
											prevL = thisL;
											prevR = thisR;
											thisL = rbuf[thisIndex*2];
											thisR = rbuf[thisIndex*2+1];
											lastIndex ++;
										}
										obuf[newLen]   = (short) ((prevL*oneMinMult + thisL*mult)>>16); //(short) (prevL*oneMinMult + thisL*mult)
										obuf[newLen+1] = (short) ((prevR*oneMinMult + thisR*mult)>>16); //(short) (prevr*oneMinMult + thisR*mult)
										newLen+=2;
										
										prevAtFP+=pitchFP;
									}
									ad.writeSamples(obuf, 0, newLen);
									
								}
								lastIndex-=olen;
								prevAtFP-=olenFP;
								
							//}
						}else{
								int olenFP = olen<<16;
								while(prevAtFP < olenFP){
									int thisIndex = -1;
									int newLen = 0;
									while(prevAtFP < olenFP & newLen<obuf.length){
										thisIndex = prevAtFP>>16;//(int)prevAt;
										int mult = prevAtFP&0xffff;//prevAt-thisIndex;
										int oneMinMult = 0x10000 - mult;//1 - mult;
										
										while(lastIndex!=thisIndex){
											prevL = thisL;
											thisL = rbuf[thisIndex];
											lastIndex ++;
										}
										obuf[newLen]   = (short) ((prevL*oneMinMult + thisL*mult)>>16); //(short) (prevL*oneMinMult + thisL*mult)
										newLen+=1;
										
										prevAtFP+=pitchFP;
									}
									ad.writeSamples(obuf, 0, newLen);
								}
								lastIndex-=olen;
								prevAtFP-=olenFP;
						}
						
						samplePos+=len;
						
						if(!audioUsed){
							adl.requestSync(thisAudioDevicePlayer);
							audioUsed = true;
						}
					} else if (len > 0) {
						System.out.println("len < 0 :(");
					} else if (len < 0){
						if (toLoop)
							resetStream();
						else {
							stop = true;
							toStop = true;
						}
					}
					
				}
			
			
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(e+" "+name);
			}
			if (ad != null && audioUsed) {
				ad.dispose();
				audioUsed = false;
				initedAD = false;
			}
			if(stop){
				closeStream();
				System.out.println("Track  Complete");
				adl.complete(thisAudioDevicePlayer);
			}
		}
			decrementThreadCount();
			System.out.println("PlayThread stoped " + name+" "+threadCount);
		}

	}
	private void resetStream() throws IOException{
		//*
		synchronized (currentStreamLock) {
			closeStream();
			currentStream = inputStreamFactory.getNewAudioInputStream();
			
			if(currentStream.atEnd())
				throw new IOException("End of stream for new Stream");
			//*/
			System.out.println("ASDF reset stream "+ name+" "+Thread.currentThread());
			//currentStream = null;
			sampleRate = currentStream.getRate();
			channels = currentStream.getChannels();
			
			position = 0;
			samplePos = 0;
		}
		stop = false;
	}
	private void closeStream(){
		System.out.println("ADP Close Stream "+currentStream+" "+Thread.currentThread());
		synchronized (currentStreamLock) {
			if(currentStream != null)
				try {
					currentStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			currentStream = null;
		}
	}

	public AudioDevicePlayer2(AudioInputStreamFactory in, String name) throws IOException {
		System.out.println("ADP New Song " + name+" "+Thread.currentThread());
		this.inputStreamFactory = in;
		this.name = name;

		resetStream();
	}

	private void startThread() {
		System.out.println("ADP startThread "+ name+" "+Thread.currentThread());
		//synchronized (playThreadLock) {
			stopThread();
			if(currentStream == null)
				try {
					resetStream();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (playThread == null){
				paused = false;
				stop = false;
				playThread = new PlayThread();
				playThread.start();
			}
		//}
	}
	private void stopThread() {
		System.out.println("ADP stopThread "+ name+" "+Thread.currentThread());
		//*
		//synchronized (playThreadLock) {
			if (playThread != null) {
				try {
					playThread.toStop = true;
					playThread.interrupt();
					//playThread.join();
					playThread = null;
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		//}//*/
		
	}
	
	public boolean setPosition(float f) {
		System.out.println("setPosition "+f);
		nextPosition = f;
		setNextPosition = true;
		return true;
	}

	public float getPosition() {
		if (setNextPosition) {
			return nextPosition;
		}
		return (float)samplePos/sampleRate/channels - latency;
		//return (position) / 1000f - latency;
	}

	public boolean playing() {
		return !paused && !stop;
	}

	public void setVolume(float nvolume) {
		System.out.println("setVolume Play " + name+" "+Thread.currentThread());
		volume = nvolume;
	}


	public void play() {
		System.out.println("ADP Play "+ name+" "+Thread.currentThread());
		stop = false;
		setLoop(false);
		startThread();
	}
	public void loop() {
		System.out.println("ADP Loop "+ name+" "+Thread.currentThread());
		stop = false;
		setLoop(true);
		startThread();
	}

	
	public void stop() {
		System.out.println("ADP stop "+ name+" "+Thread.currentThread());
		stop = true;
		stopThread();
		//closeStream();
	}

	public void pause() {
		System.out.println("ADP pause "+ name+" "+Thread.currentThread());
		paused = true;
		stopThread();
	}

	public void resume() {
		System.out.println("ADP resume "+ name+" "+Thread.currentThread());
		if(paused || playThread==null || playThread.toStop==true){
			paused = false;
			startThread();
		}
	}
	public void dispose() {
		System.out.println("ADP dispose " + name+" "+Thread.currentThread());
		stop();
		/*
		closeStream();
		inputStreamFactory = null;
		*/
	
	}
	public String getName() {
		return name;
	}

	public void fade(int duration, float f, boolean b) {
		//TODO
	//	pause();
	}
	
	private void setLoop(boolean loop) {
		toLoop = loop;
	}

	public void setAudioDeviceListener(AudioDeviceListener audioDeviceListener){
		adl = audioDeviceListener;
	}
	public void setPitch(float pitch) {
		this.pitch = pitch;
		//if(playThread!=null)
		//	playThread.initedAD=false;
	}
}
