package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.Music;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;
import org.newdawn.slick.util.ResourceLoader;

/**
 * A test for the sound system of the library
 * 
 * @author kevin
 * @author aaron
 */
public class SoundURLTest extends BasicGame {
	/** The sound to be played */
	private Sound sound;
	/** The sound to be played */
	private Sound charlie;
	/** The sound to be played */
	private Sound burp;
	/** The music to be played */
	private Music music;
	/** The music to be played */
	private Music musica;
	/** The music to be played */
	private Music musicb;
	/** The sound to be played */
	private Sound engine;
	/** The Volume of the playing music */
	private int volume = 1;
	
	/**
	 * Create a new test for sounds
	 */
	public SoundURLTest() {
		super("Sound URL Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		sound = new Sound(ResourceLoader.getResource("testdata/restart.ogg"));
		charlie = new Sound(ResourceLoader.getResource("testdata/cbrown01.wav"));
		engine = new Sound(ResourceLoader.getResource("testdata/engine.wav"));
		//music = musica = new Music("testdata/SMB-X.XM");
		music = musica = new Music(ResourceLoader.getResource("testdata/restart.ogg"), false);
		musicb = new Music(ResourceLoader.getResource("testdata/kirby.ogg"), false);
		burp = new Sound(ResourceLoader.getResource("testdata/burp.aif"));
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
		g.drawString("Press + or - to change volume of music", 100, 270);
		g.setColor(Color.blue);
		g.drawString("Music Volume Level: " + volume / 10.0f, 150, 300);
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
		if (key == Input.KEY_E) {
			if (engine.playing()) {
				engine.stop();
			} else {
				engine.loop();
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

	}
	
	/**
	 *  Convenience routine to set volume of current music 
	 */
	private void setVolume() {
		// Do bounds checking
		if(volume > 10) {
			volume = 10;
		} else if(volume < 0) {
			volume = 0;
		}
		
		music.setVolume(volume / 10.0f);
	}
	
	/**
	 * Entry point to the sound test
	 * 
	 * @param argv The arguments provided to the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new SoundURLTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
