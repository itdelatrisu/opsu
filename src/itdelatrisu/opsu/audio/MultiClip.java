package itdelatrisu.opsu.audio;

import java.io.IOException;
import java.util.LinkedList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;

//http://stackoverflow.com/questions/1854616/in-java-how-can-i-play-the-same-audio-clip-multiple-times-simultaneously
public class MultiClip {
	/** A list of clips used for this audio sample */
	LinkedList<Clip> clips = new LinkedList<Clip>();
	
	/** The format of this audio sample */
	AudioFormat format;
	
	/** The data for this audio sample */
	byte[] audioData;
	
	/** The name given to this clip */
	String name;
	
	/** Size of a single buffer */
	final int BUFFER_SIZE = 0x1000;
	
	/** Constructor  */
	public MultiClip(String name, AudioInputStream audioIn) throws IOException, LineUnavailableException {
		this.name = name;
		if(audioIn != null){
			format = audioIn.getFormat();
			
			LinkedList<byte[]> allBufs = new LinkedList<byte[]>();
			
			int readed = 0;
			boolean hasData = true;
			while (hasData) {
				readed = 0;
				byte[] tbuf = new byte[BUFFER_SIZE];
				while (readed < tbuf.length) {
					int read = audioIn.read(tbuf, readed, tbuf.length - readed);
					if (read < 0) {
						hasData = false;
						break;
					}
					readed += read;
				}
				allBufs.add(tbuf);
			}
			
			audioData = new byte[(allBufs.size() - 1) * BUFFER_SIZE + readed];
			
			int cnt = 0;
			for (byte[] tbuf : allBufs) {
				int size = BUFFER_SIZE;
				if (cnt == allBufs.size() - 1) {
					size = readed;
				}
				System.arraycopy(tbuf, 0, audioData, BUFFER_SIZE * cnt, size);
				cnt++;
			}
		}
		getClip();
	}
	
	/**
	 * Returns the name of the clip
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Plays the clip with the specified volume.
	 * @param volume the volume the play at
	 * @throws IOException 
	 * @throws LineUnavailableException 
	 */
	public void start(float volume) throws LineUnavailableException, IOException {
		Clip clip = getClip();
		
		// PulseAudio does not support Master Gain
		if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
			// set volume
			FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
			gainControl.setValue(dB);
		}
		
		clip.setFramePosition(0);
		clip.start();
	}
	/**
	 * Returns a Clip that is not playing from the list
	 * if one is not available a new one is created
	 * @return the Clip
	 */
	private Clip getClip() throws LineUnavailableException, IOException{
		for(Clip c : clips){
			if(!c.isRunning()){
				return c;
			}
		}
		Clip t = AudioSystem.getClip();
		if (format != null)
			t.open(format, audioData, 0, audioData.length);
		clips.add(t);
		return t;
	}
}
