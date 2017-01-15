package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.utils.TimeUtils;

import javazoom2.jl.decoder.*;

public class AudioDevicePlayer3 extends AudioDevicePlayer{

	private final AudioDevicePlayer thisAudioDevicePlayer = this;
	boolean setNextPosition = false;
	float nextPosition;
	float volume = 0.1f;
	
	PlayThread playThread;
	Bitstream bitstream;
	Decoder decoder;
	Header header;
	SampleBuffer buf;
	InputStreamFactory inputStreamFactory;
	String name;
	
	AudioDeviceListener adl;

	boolean started = false;
	boolean paused = true;
	boolean stop = false;
	
	boolean toLoop;


	float position;// in ms
	long posUpdateTime;
	
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
				//hread.currentThread().setPriority(Thread.MAX_PRIORITY);
				try {
					while (!toStop) {
					header = bitstream.readFrame();
					if (setNextPosition) {
						System.out.println("PlayThread Next Positioning: " + position + " " + nextPosition);
						if (position > nextPosition) {
							resetStream();
							header = bitstream.readFrame();
						}
						while (position < nextPosition) {
							bitstream.closeFrame();
							header = bitstream.readFrame();
							position += header.ms_per_frame();
						}
						setNextPosition = false;
					}
					if (header == null) {
						System.out.println("Header is null");
						if (toLoop) {
							bitstream.closeFrame();
							resetStream();
							if (header == null) {
								System.out.println("PlayThread Header is null still"
												+ bitstream.header_pos());
								break;
							}
						} else {
							stop = true;
							toStop = true;
							
							break;
						}
					} else {
						
						if (!initData) {
							sampleRate = header.frequency();
							channels = header.mode() == Header.SINGLE_CHANNEL ? 1: 2;
							
							initData = true;
							buf = new SampleBuffer(sampleRate, channels);
							decoder = new Decoder();
							decoder.setOutputBuffer(buf);
							buf.clear_buffer();
							System.out.println("PlayThread initData");

						}

							
						if (header != null) {//???
							decoder.decodeFrame(header, bitstream);
							int len = buf.getBufferLength();
							buf.clear_buffer();
							if (!initedAD) {
								initedAD = true;
								System.out.println("PlayThread Music Init");
								if (ad != null && audioUsed) {
									ad.dispose();
								}
								audioUsed = false;
								ad = Gdx.audio.newAudioDevice(sampleRate,
										channels == 1);
								// write 0's to start with to get rid of garbage
								//ad.writeSamples(new short[200], 0, 200);
								//audioUsed = true;
								
								System.out
										.println("Latency: " + ad.getLatency()
												+ " sr:" + sampleRate);
								latency = ad.getLatency() / sampleRate;
							}
							ad.setVolume(volume);
							posUpdateTime = TimeUtils.millis();
							if (len > 0) {
								ad.writeSamples(buf.getBuffer(), 0, len);// buf.channelPointer2[0]);
								audioUsed = true;
							}

							position += 1000f * len / channels / sampleRate;// header.frequency();//header.ms_per_frame();
							bitstream.closeFrame();
							/*
							int prefSleep = Math.max(1000*len/channels/sampleRate/2 -worstSleepAccuracy,0);
							long time = System.currentTimeMillis();
							Thread.sleep(Math.max(prefSleep,0));
							long deltaTime = System.currentTimeMillis() - time + 2;
							if(deltaTime - prefSleep > worstSleepAccuracy){
								worstSleepAccuracy = (int)(deltaTime -prefSleep);
								System.out.println("worstSleepAccuracy"+worstSleepAccuracy+" "+prefSleep+" "+deltaTime);
							}*/
							
							//Thread.sleep(1);
						}
					}
				}
			//} catch(InterruptedException e){
			//	System.out.println("Interrupted: "+file.path());
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
				if (bitstream != null) {
					try {
						bitstream.close();
						bitstream = null;
					} catch (BitstreamException e) {
						e.printStackTrace();
					}
				}
				if(adl != null)
					adl.complete(thisAudioDevicePlayer);
			}
			decrementThreadCount();
			System.out.println("Thread stoped " + name+" "+threadCount);
		}

	}
	private void resetStream(){
		if (bitstream != null) {
			try {
				bitstream.close();
				bitstream = null;
			} catch (BitstreamException e) {
				e.printStackTrace();
			}
		}
		decoder = new Decoder();
		bitstream = new Bitstream(inputStreamFactory.getNewInputStream());
		decoder.setOutputBuffer(buf);
		position = 0;
		stop = false;
	}

	public AudioDevicePlayer3(InputStreamFactory in, String name) {
		System.out.println("New Song " + name);
		this.inputStreamFactory = in;
		this.name = name;
	}


	//TODO pre decode some data before starting the thread?
	private synchronized void startThread() {
		stopThread();
		if(bitstream == null)
			resetStream();
		if (playThread == null){
			playThread = new PlayThread();
			playThread.start();
		}
	}
	private synchronized void stopThread() {
		if (playThread != null) {
			try {
				playThread.toStop = true;
				playThread.interrupt();
//				playThread.join();
				playThread = null;
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean setPosition(float f) {
		// System.out.println("setPosition "+f);
		nextPosition = f * 1000;
		setNextPosition = true;
		return true;
	}

	public float getPosition() {
		if (setNextPosition) {
			return -1000000000;//nextPosition / 1000f;
		}
		return (position) / 1000f - latency;
	}

	public boolean playing() {
		return playThread!=null && !playThread.toStop;
	}

	public void setVolume(float nvolume) {
		volume = nvolume;
	}


	public void play() {
		System.out.println("PlayThread Play "+name);
		stopThread();
		resetStream();
		setLoop(false);
		startThread();
	}
	public void loop() {
		System.out.println("PlayThread Loop "+name);
		stopThread();
		resetStream();
		setLoop(true);
		startThread();
	}

	
	public synchronized void stop() {
		System.out.println("PlayThread stop");
		stop = true;
		stopThread();
	}

	public void pause() {
		System.out.println("PlayThread pause");
		paused = true;
		stopThread();
	}

	public void resume() {
		System.out.println("PlayThread resume");
		paused = false;
		startThread();
	}

	public String getName() {
		return name;
	}

	public void fade(int duration, float f, boolean b) {
		//TODO
		pause();
	}

	public void dispose() {
		System.out.println("PlayThread dispose " + name);
		stop();
	}
	
	private void setLoop(boolean loop) {
		toLoop = loop;
	}

	public void setAudioDeviceListener(AudioDeviceListener audioDeviceListener){
		adl = audioDeviceListener;
	}
	public void setPitch(float pitch) {
		// TODO Auto-generated method stub
		
	}
}
