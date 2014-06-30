package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.Music;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.openal.SoundStore;

/**
 * A test for the sound system (positioning) of the library
 * 
 * @author kevin
 */
public class SoundPositionTest extends BasicGame {
	/** the GameContainer instance for this game/testcase */
	private GameContainer myContainer;
	/** The music to be played */
	private Music music;
	
	/** The IDs of the sources used for each engine noise */
	private int[] engines = new int[3];
	
	/**
	 * Create a new test for sounds
	 */
	public SoundPositionTest() {
		super("Music Position Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		SoundStore.get().setMaxSources(32);
		
		myContainer = container;
		music = new Music("testdata/kirby.ogg", true);
		music.play();
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		g.setColor(Color.white);
		g.drawString("Position: "+music.getPosition(), 100,100);
		g.drawString("Space - Pause/Resume", 100,130);
		g.drawString("Right Arrow - Advance 5 seconds", 100, 145);
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
		if (key == Input.KEY_SPACE) {
			if (music.playing()) {
				music.pause();
			} else {
				music.resume();
			}
		}
		if (key == Input.KEY_RIGHT) {
			music.setPosition(music.getPosition()+5);
		}
	}
	
	/**
	 * Entry point to the sound test
	 * 
	 * @param argv The arguments provided to the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new SoundPositionTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
