package itdelatrisu.opsu.audio;

import itdelatrisu.opsu.ErrorHandler;

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
	byte[] buffer;
	
	/** The name given to this clip */
	String name;
	
	/** Constructor 
	 * @param name 
	 * @throws LineUnavailableException */
	public MultiClip(String name, AudioInputStream audioIn) throws IOException, LineUnavailableException {
		this.name = name;
		if(audioIn != null){
			buffer = new byte[audioIn.available()];
			int readed= 0;
			while(readed < buffer.length) {
				int read = audioIn.read(buffer, readed, buffer.length-readed);
				if(read < 0 )
					break;
				readed += read;
			}
			format = audioIn.getFormat();
		} else {
			System.out.println("Null multiclip");
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
			t.open(format, buffer, 0, buffer.length);
		clips.add(t);
		return t;
	}
}
