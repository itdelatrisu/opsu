package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.TimeUtils;

import javazoom.jl.decoder.*;

public class MusicJL2 extends AbsMusic {

	boolean setNextPosition = false;
	float nextPosition;
	float volume = 0.1f;
	
	PlayThread playThread;
	Bitstream bitstream;
	Decoder decoder;
	Header header;
	SampleBuffer buf;
	FileHandle file;

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
			super("MusicJLThread "+file.path());
		}
		public void run() {
			
				incrementThreadCount();
				System.out.println("MusicJL Running Thread " + file.path()+" "+threadCount);
				
				try {
					while (!toStop) {
					header = bitstream.readFrame();
					if (setNextPosition) {
						System.out.println("Next Positioning: " + position + " " + nextPosition);
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
						if (toLoop) {
							bitstream.closeFrame();
							resetStream();
							if (header == null) {
								System.out.println("MusicJL Header is null still"
												+ bitstream.header_pos());
								break;
							}
						} else {
							stop = true;
							toStop = true;
							
							break;
						}
					} else {
						if (!initedAD) {
							initedAD = true;
							System.out.println("MusicJL Music Init");
							if (ad != null && audioUsed) {
								ad.dispose();
							}
							audioUsed = false;

							if (!initData) {
								sampleRate = header.frequency();
								channels = header.mode() == Header.SINGLE_CHANNEL ? 1: 2;
								
								initData = true;
								buf = new SampleBuffer(sampleRate, channels);
								decoder = new Decoder();
								decoder.setOutputBuffer(buf);
								buf.clear_buffer();
								System.out.println("MusicJL initData");

							}

							ad = Gdx.audio.newAudioDevice(sampleRate,
									channels == 1);

							// write 0's to start with to get rid of garbage
							ad.writeSamples(new short[4096], 0, 4096);
							audioUsed = true;
							
							System.out
									.println("Latency: " + ad.getLatency()
											+ " sr:" + sampleRate);
							latency = ad.getLatency() / sampleRate;
						}
						if (header != null && initedAD) {
							decoder.decodeFrame(header, bitstream);
							int len = buf.getBufferLength();
							buf.clear_buffer();

							ad.setVolume(volume);
							posUpdateTime = TimeUtils.millis();
							if (len > 0) {
								ad.writeSamples(buf.getBuffer(), 0, len);// buf.channelPointer2[0]);
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
				System.out.println(e+" "+file.path());
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
				fireMusicEnded();
			}
			decrementThreadCount();
			System.out.println("Thread stoped " + file.path()+" "+threadCount);
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
		bitstream = new Bitstream(file.read());
		decoder.setOutputBuffer(buf);
		position = 0;
		stop = false;
	}

	public MusicJL2(String path, AbsMusicCompleteListener lis) {
		super(lis);
		System.out.println("New Song " + path);
		file = ResourceLoader.getFileHandle(path);
	}


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
				playThread.join();
				Thread.sleep(1);
				playThread = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public boolean setPosition(float f) {
		// System.out.println("setPosition "+f);
		nextPosition = f * 1000;
		setNextPosition = true;
		return true;
	}

	@Override
	public float getPosition() {
		if (setNextPosition) {
			return nextPosition / 1000f;
		}
		return (position) / 1000f - latency;
	}

	@Override
	public boolean playing() {
		return playThread!=null && !playThread.toStop;
	}

	@Override
	public void setVolume(float nvolume) {
		volume = nvolume;
	}


	@Override
	public void play() {
		System.out.println("MusicJL Play "+file.path());
		stopThread();
		resetStream();
		setLoop(false);
		startThread();
	}
	@Override
	public void loop() {
		System.out.println("MusicJL Loop "+file.path());
		stopThread();
		resetStream();
		setLoop(true);
		startThread();
	}

	
	@Override
	public synchronized void stop() {
		System.out.println("MusicJL stop");
		stop = true;
		stopThread();
	}

	@Override
	public void pause() {
		System.out.println("MusicJL pause");
		paused = true;
		stopThread();
	}

	@Override
	public void resume() {
		System.out.println("MusicJL resume");
		paused = false;
		startThread();
	}



	@Override
	public String getName() {
		return file != null ? file.path() : "NULL";
	}

	@Override
	public void fade(int duration, float f, boolean b) {
		super.fade(duration, f, b);
		pause();
	}

	@Override
	public void dispose() {
		System.out.println("dispose " + (file != null ? file.path() : "null"));
		stop();
	}
	

	private void setLoop(boolean loop) {
		toLoop = loop;
	}

}
