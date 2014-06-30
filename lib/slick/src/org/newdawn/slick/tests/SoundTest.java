package org.newdawn.slick.tests;

import java.io.IOException;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.Music;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.AudioLoader;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.util.ResourceLoader;

/**
 * A test for the sound system of the library
 * 
 * @author kevin
 * @author aaron
 */
public class SoundTest extends BasicGame {
	/** the GameContainer instance for this game/testcase */
	private GameContainer myContainer;
	/** The sound to be played */
	private Sound sound;
	/** The sound to be played */
	private Sound charlie;
	/** The sound to be played */
	private Sound burp;
	/** The music to be played */
	private Music music;
	/** first music that can be played */
	private Music musica;
	/** second music that can be played */
	private Music musicb;
	/** The sound to be played */
	private Audio engine;
	/** The Volume of the playing music */
	private int volume = 10;
	
	/** The IDs of the sources used for each engine noise */
	private int[] engines = new int[3];
	
	/**
	 * Create a new test for sounds
	 */
	public SoundTest() {
		super("Sound And Music Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		SoundStore.get().setMaxSources(32);
		
		myContainer = container;
		sound = new Sound("testdata/restart.ogg");
		charlie = new Sound("testdata/cbrown01.wav");
		try {
			engine = AudioLoader.getAudio("WAV", ResourceLoader.getResourceAsStream("testdata/engine.wav"));
		} catch (IOException e) {
			throw new SlickException("Failed to load engine", e);
		}
		music = musica = new Music("testdata/SMB-X.XM");
		//music = musica = new Music("testdata/theme.ogg", true);
		musicb = new Music("testdata/kirby.ogg", true);
		burp = new Sound("testdata/burp.aif");
		
		music.play();
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		g.setColor(Color.white);
		g.drawString("The OGG loop is now streaming from the file, woot.",100,60);
		g.drawString("Press space for sound effect (OGG)",100,100);
		g.drawString("Press P to pause/resume music (XM)",100,130);
		g.drawString("Press E to pause/resume engine sound (WAV)",100,190);
		g.drawString("Press enter for charlie (WAV)",100,160);
		g.drawString("Press C to change music",100,210);
		g.drawString("Press B to burp (AIF)",100,240);
		g.drawString("Press + or - to change global volume of music", 100, 270);
		g.drawString("Press Y or X to change individual volume of music", 100, 300);
		g.drawString("Press N or M to change global volume of sound fx", 100, 330);
		g.setColor(Color.blue);
		g.drawString("Global Sound Volume Level: " + container.getSoundVolume(), 150, 390);
		g.drawString("Global Music Volume Level: " + container.getMusicVolume(), 150, 420);
		g.drawString("Current Music Volume Level: " + music.getVolume(), 150, 450);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) {
	}

	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
		if (key == Input.KEY_ESCAPE) {
			System.exit(0);
		}
		if (key == Input.KEY_SPACE) {
			sound.play();
		}
		if (key == Input.KEY_B) {
			burp.play();
		}
		if (key == Input.KEY_A) {
			sound.playAt(-1, 0, 0);
		}
		if (key == Input.KEY_L) {
			sound.playAt(1, 0, 0);
		}
		if (key == Input.KEY_RETURN) {
			charlie.play(1.0f,1.0f);
		}
		if (key == Input.KEY_P) {
			if (music.playing()) {
				music.pause();
			} else {
				music.resume();
			}
		}
		if (key == Input.KEY_C) {
			music.stop();
			if (music == musica) {
				music = musicb;
			} else {
				music = musica;
			}
			
			music.loop();
		}
		for (int i=0;i<3;i++) {
			if (key == Input.KEY_1+i) {
				if (engines[i] != 0) {
					System.out.println("Stop "+i);
					SoundStore.get().stopSoundEffect(engines[i]);
					engines[i] = 0;
				} else {
					System.out.println("Start "+i);
					engines[i] = engine.playAsSoundEffect(1, 1, true);
				}
			}
		}
		
		if (c == '+') {
			volume += 1;
			setVolume();
		}
		
		if (c == '-') {
			volume -= 1;
			setVolume();
		}
		
		if (key == Input.KEY_Y) {
			int vol = (int) (music.getVolume() * 10);
			vol --;
			if (vol < 0) vol = 0;
			// set individual volume of music
			music.setVolume(vol/10.0f);
		}
		if (key == Input.KEY_X) {
			int vol = (int) (music.getVolume() * 10);
			vol ++;
			if (vol > 10) vol = 10;
			// set individual volume of music
			music.setVolume(vol/10.0f);
		}
		if (key == Input.KEY_N) {
			int vol = (int) (myContainer.getSoundVolume() * 10);
			vol --;
			if (vol < 0) vol = 0;
			// set global volume of sound fx
			myContainer.setSoundVolume(vol/10.0f);
		}
		if (key == Input.KEY_M) {
			int vol = (int) (myContainer.getSoundVolume() * 10);
			vol ++;
			if (vol > 10) vol = 10;
			// set global volume of sound fx
			myContainer.setSoundVolume(vol/10.0f);
		}

	}
	
	/**
	 *  Convenience routine to set global volume of music 
	 */
	private void setVolume() {
		// Do bounds checking
		if(volume > 10) {
			volume = 10;
		} else if(volume < 0) {
			volume = 0;
		}
		
		myContainer.setMusicVolume(volume / 10.0f);
	}
	
	/**
	 * Entry point to the sound test
	 * 
	 * @param argv The arguments provided to the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new SoundTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
