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

	PlayThread playThread;
	Bitstream bitstream;
	Decoder decoder;
	Header header;
	AudioDevice ad;
	SampleBuffer buf;
	FileHandle file;

	class PlayThread extends Thread {
		boolean started = false;
		boolean paused = true;
		boolean toStop = false;
		boolean toLoop;
		boolean inited = false;

		float position;// in ms
		long posUpdateTime;
		float latency;
		int sampleRate = 44100;
		int channels;
		boolean initData = false;

		public void setPosition(float f) {
			// System.out.println("setPosition "+f);
			nextPosition = f * 1000;
			setNextPosition = true;

		}

		public float getPosition() {
			if (setNextPosition) {
				return nextPosition / 1000f;
			}
			// return (position+(TimeUtils.millis()-posUpdateTime))/1000f;
			return (position) / 1000f - latency;
		}

		public boolean isPlaying() {
			return !paused;
		}

		public void setVolume(float nvolume) {
			volume = nvolume;
		}

		public void stopPlaying() {
			inited = false;

			toStop = true;
			paused = true;

		}

		public void pause() {
			inited = false;
			paused = true;
		}

		public void resumePlaying() {
			// System.out.println("MusicJL resumePlaying");
			paused = false;
		}

		public void setLoop(boolean loop) {
			// System.out.println("MusicJL loop "+loop);
			toLoop = loop;
		}

		public void play() {
			// System.out.println("MusicJL play");
			paused = false;
			toStop = false;
		}

		public void run() {
			try {
				System.out
						.println("MusicJL Running Thread play " + file.path());
				bitstream = new Bitstream(file.read()
				// )
				);
				position = 0;
				started = true;
				while (!toStop) {
					if (paused) {
						if (ad != null) {
							ad.dispose();
							audioUsed = false;
							ad = null;
						}
						inited = false;
						Thread.sleep(16);
						continue;
					} else {

						header = bitstream.readFrame();
						if (setNextPosition) {
							System.out.println("Next Positioning: " + position
									+ " " + nextPosition);
							if (position > nextPosition) {
								decoder = new Decoder();
								bitstream = new Bitstream(file.read());
								decoder.setOutputBuffer(buf);
								position = 0;
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
								System.out.println("Header is null "
										+ bitstream.header_pos());
								bitstream.closeFrame();
								bitstream.close();
								bitstream = new Bitstream(file.read());
								header = bitstream.readFrame();
								position = 0;
								buf = new SampleBuffer(sampleRate, channels);
								decoder = new Decoder();
								decoder.setOutputBuffer(buf);
								if (header == null) {
									System.out
											.println("MusicJL Header is null still"
													+ bitstream.header_pos());
									break;
								}
							} else {
								toStop = true;
							}
						} else {
							if (!inited) {
								inited = true;
								System.out.println("MusicJL Music Init");
								if (ad != null && audioUsed) {
									ad.dispose();
								}
								audioUsed = false;

								if (!initData) {
									sampleRate = header.frequency();
									channels = header.mode() == Header.SINGLE_CHANNEL ? 1
											: 2;
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
								System.out
										.println("Latency: " + ad.getLatency()
												+ " sr:" + sampleRate);
								latency = ad.getLatency() / sampleRate;
							}
							if (header != null && inited) {
								decoder.decodeFrame(header, bitstream);
								int len = buf.getBufferLength();
								buf.clear_buffer();

								ad.setVolume(volume);
								posUpdateTime = TimeUtils.millis();
								if (len > 0) {
									ad.writeSamples(buf.getBuffer(), 0, len);// buf.channelPointer2[0]);
									audioUsed = true;
								}

								position += 1000f * len / channels / sampleRate;// header.frequency();//header.ms_per_frame();
								bitstream.closeFrame();
								// Thread.sleep(16);//Math.max(1000*len/channels/sampleRate,1));

							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (ad != null && audioUsed) {
				ad.dispose();
				audioUsed = false;
			}
			toStop = true;
			paused = true;
			System.out.println("Done " + file.path());
		}

	}

	public MusicJL(String path) {
		System.out.println("New Song " + path);
		file = ResourceLoader.getFileHandle(path);

		if (playThread == null || playThread.toStop)
			playThread = new PlayThread();

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
		// System.out.println("Play "+file.path());
		playThread.setLoop(false);
		start();
	}

	private void start() {
		if (playThread.toStop)
			playThread = new PlayThread();
		playThread.play();
		if (!playThread.started)
			playThread.start();

	}

	@Override
	public boolean playing() {
		return playThread.isPlaying();
	}

	@Override
	public String getName() {
		return file != null ? file.path() : "NULL";
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

	@Override
	public float getPosition() {

		return playThread.getPosition();
	}

	@Override
	public void dispose() {

		System.out.println("dispose " + (file != null ? file.path() : "null"));
		try {
			stop();
			playThread.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (ad != null) {
			if (audioUsed)
				ad.dispose();

		}

		if (bitstream != null) {
			try {
				bitstream.close();
			} catch (BitstreamException e) {
				e.printStackTrace();
			}
		}
	}

}
