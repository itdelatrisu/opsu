package org.newdawn.slick.tests;

import java.io.IOException;

import org.newdawn.slick.AngelCodeFont;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Music;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;
import org.newdawn.slick.loading.DeferredResource;
import org.newdawn.slick.loading.LoadingList;

/**
 * A test for deferred loading. Each of the resources is requested then the loading list
 * is cycled to actual perform the resource allowing the rendering to be performed in
 * between 
 *
 * @author kevin
 */
public class DeferredLoadingTest extends BasicGame {
	/** The music that will be played on load completion */
	private Music music;
	/** The sound that will be played on load completion */
	private Sound sound;
	/** The image that will be shown on load completion */
	private Image image;
	/** The font that will be rendered on load completion */
	private Font font;
	/** The next resource to load */
	private DeferredResource nextResource;
	/** True if we've loaded all the resources and started rendereing */
	private boolean started;
	
	/**
	 * Create a new image rendering test
	 */
	public DeferredLoadingTest() {
		super("Deferred Loading Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		LoadingList.setDeferredLoading(true);
		
		new Sound("testdata/cbrown01.wav");
		new Sound("testdata/engine.wav");
		sound = new Sound("testdata/restart.ogg");
		new Music("testdata/testloop.ogg");
		music = new Music("testdata/SMB-X.XM");
		
		new Image("testdata/cursor.png");
		new Image("testdata/cursor.tga");
		new Image("testdata/cursor.png");
		new Image("testdata/cursor.png");
		new Image("testdata/dungeontiles.gif");
		new Image("testdata/logo.gif");
		image = new Image("testdata/logo.tga");
		new Image("testdata/logo.png");
		new Image("testdata/rocket.png");
		new Image("testdata/testpack.png");
		
		font = new AngelCodeFont("testdata/demo.fnt", "testdata/demo_00.tga");
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		if (nextResource != null) {
			g.drawString("Loading: "+nextResource.getDescription(), 100, 100);
		}
		
		int total = LoadingList.get().getTotalResources();
		int loaded = LoadingList.get().getTotalResources() - LoadingList.get().getRemainingResources();
		
		float bar = loaded / (float) total;
		g.fillRect(100,150,loaded*40,20);
		g.drawRect(100,150,total*40,20);
		
		if (started) {
			image.draw(100,200);
			font.drawString(100,500,"LOADING COMPLETE");
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) throws SlickException {
		if (nextResource != null) {
			try {
				nextResource.load();
				// slow down loading for example purposes
				try { Thread.sleep(50); } catch (Exception e) {}
			} catch (IOException e) {
				throw new SlickException("Failed to load: "+nextResource.getDescription(), e);
			}
			
			nextResource = null;
		}
		
		if (LoadingList.get().getRemainingResources() > 0) {
			nextResource = LoadingList.get().getNext();
		} else {
			if (!started) {
				started = true;
				music.loop();
				sound.play();
			}
		}
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new DeferredLoadingTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
	}
}
