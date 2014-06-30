package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.Music;
import org.newdawn.slick.MusicListener;
import org.newdawn.slick.SlickException;

/**
 * A test for music listeners which notify you when the music has eneded
 *
 * @author kevin
 */
public class MusicListenerTest extends BasicGame implements MusicListener {
	/** True if we should display the music ended message */
	private boolean musicEnded = false;
	/** True if we should display the music swapped message */
	private boolean musicSwapped = false;
	/** The music to be played */
	private Music music;
	/** The music to be streamed */
	private Music stream;
	
	/**
	 * Create a new test
	 */
	public MusicListenerTest() {
		super("Music Listener Test");
	}

	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		music = new Music("testdata/restart.ogg", false);
		stream = new Music("testdata/restart.ogg", false);
		
		music.addListener(this);
		stream.addListener(this);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) throws SlickException {
	}

	/**
	 * @see org.newdawn.slick.MusicListener#musicEnded(org.newdawn.slick.Music)
	 */
	public void musicEnded(Music music) {
		musicEnded = true;
	}

	/**
	 * @see org.newdawn.slick.MusicListener#musicSwapped(org.newdawn.slick.Music, org.newdawn.slick.Music)
	 */
	public void musicSwapped(Music music, Music newMusic) {
		musicSwapped = true;
	}
	
	/**
	 * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) throws SlickException {
		g.drawString("Press M to play music", 100, 100);
		g.drawString("Press S to stream music", 100, 150);
		if (musicEnded) {
			g.drawString("Music Ended", 100, 200);
		}
		if (musicSwapped) {
			g.drawString("Music Swapped", 100, 250);
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
		if (key == Input.KEY_M) {
			musicEnded = false;
			musicSwapped = false;
			music.play();
		}
		if (key == Input.KEY_S) {
			musicEnded = false;
			musicSwapped = false;
			stream.play();
		}
	}
	
	/**
	 * Entry point to the sound test
	 * 
	 * @param argv The arguments provided to the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new MusicListenerTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
