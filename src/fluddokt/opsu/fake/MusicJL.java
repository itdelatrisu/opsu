package fluddokt.opsu.fake;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;

import javazoom.jl.decoder.*;

public class MusicJL extends AbsMusic {

	class PlayThread extends Thread{
		public void run(){
			try {
				header = bitstream.readFrame();
			
				ad = Gdx.audio.newAudioDevice(header.frequency(), header.mode()==Header.SINGLE_CHANNEL);
				ad.setVolume(1);
				buf = new OutputBuffer(header.mode()==Header.SINGLE_CHANNEL?1:2, false);
				short[] sbuf = new short[OutputBuffer.BUFFERSIZE];
				decoder = new MP3Decoder();
				decoder.setOutputBuffer(buf);
				int cnt = 0;
				while(header!=null){
					if(cnt++ > 500)
						break;
					//*
					System.out.println(header+" "+header.frequency()
							+" mode:"+header.mode_string()
							+" mode:"+header.mode()
							+" modee:"+header.mode_extension()
							+" A:"+Header.STEREO+" "+Header.DUAL_CHANNEL+" "+Header.JOINT_STEREO+" "+Header.SINGLE_CHANNEL
							+" SFreq:"+header.sample_frequency()
							+" MSPf:"+header.ms_per_frame()
							+" FrSz:"+header.framesize
							+" CFrFz:"+header.calculate_framesize()
							+" SyncHd:"+header.getSyncHeader()
							+" Layer:"+header.layer_string()
							+" nslots:"+header.nSlots
							+" bitrate"+header.bitrate()
							+" isb"+header.bitrate_instant()
							);//*/
					
					//decoder.setOutputBuffer(new SampleBuffer(header.sample_frequency(), 2));
					
					
					decoder.decodeFrame(header, bitstream);
					//byte[] bbuf = buf.getBuffer();
					//for(int i=0,j=0;i<header.framesize*2;i++,j+=2){
					//	sbuf[i] = (short)(bbuf[j+1]<<8 | bbuf[j]);
					//}
					//Thread.sleep(1000);
					//bitstream.closeFrame();
					//if(buf!=null)
					//System.out.println(buf.getBufferLength()+" "+buf.getChannelCount()+" "+buf.getSampleFrequency());
					int len = buf.reset();
						ad.writeSamples(buf.buffer2, 0, len);//buf.channelPointer2[0]);
					
					bitstream.closeFrame();
					header = bitstream.readFrame();
					if(Thread.interrupted()){
						System.out.println("Interrupted");
						return;
					}
				}
			} catch (BitstreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ArrayIndexOutOfBoundsException e){
				e.printStackTrace();
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//while(header!=null);
			System.out.println("Done");
		}
	}
	Thread playThread;
	Bitstream bitstream;
	MP3Decoder decoder;
	Header header;
	AudioDevice ad;
	OutputBuffer buf;
	public MusicJL(String path) {
		bitstream = new Bitstream(
					//new FileInputStream(path));
		ResourceLoader.getFileHandle(path).read()
		);
				
		
	}
	@Override
	public boolean setPosition(float f) {
		// TODO Auto-generated method stub
		return super.setPosition(f);
	}

	@Override
	public void loop() {
		// TODO Auto-generated method stub
		play();
	}

	@Override
	public void play() {
		System.out.println("Play");
		if(playThread!=null){
			playThread.stopPlay();
			try {
				playThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		playThread = new PlayThread();
		playThread.start();
	}

	@Override
	public boolean playing() {
		// TODO Auto-generated method stub
		return super.playing();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		super.pause();
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		super.resume();
	}

	@Override
	public void setVolume(float volume) {
		// TODO Auto-generated method stub
		super.setVolume(volume);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		super.stop();
	}

	@Override
	public void fade(int duration, float f, boolean b) {
		// TODO Auto-generated method stub
		super.fade(duration, f, b);
	}

	@Override
	public float getPosition() {
		// TODO Auto-generated method stub
		return super.getPosition();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		super.dispose();
	}

}
