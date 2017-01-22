package itdelatrius.opsu;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;
import org.newdawn.slick.util.ResourceLoader;

import itdelatrisu.opsu.audio.MultiClip;

public class MultiClipTest {

	@Test
	public void multiClipCanBeCreated() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		// test to catch issue with creating clips on linux.
		URL url = ResourceLoader.getResource("applause.wav");
		AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
		MultiClip clip = new MultiClip("test", audioIn);
		clip.destroy();
	}

}
