package org.newdawn.slick.tests;

import org.newdawn.slick.Animation;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.PackedSpriteSheet;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;

/**
 * A test for packed sprite sheets
 *
 * @author kevin
 */
public class PackedSheetTest extends BasicGame {
	/** The sheet loaded */
	private PackedSpriteSheet sheet;
	/** The container holding this game */
	private GameContainer container;
	/** The position of the rocket */
	private float r = -500;
	/** The rocket's image */
	private Image rocket;
	/** The animation for the runner */
	private Animation runner;
	/** The angle of roatation */
	private float ang;
	
	/**
	 * Create a new image rendering test
	 */
	public PackedSheetTest() {
		super("Packed Sprite Sheet Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		this.container = container;
		
		sheet = new PackedSpriteSheet("testdata/testpack.def", Image.FILTER_NEAREST);
		rocket = sheet.getSprite("rocket");
		
		SpriteSheet anim = sheet.getSpriteSheet("runner");
		runner = new Animation();
		
		for (int y=0;y<2;y++) {
			for (int x=0;x<6;x++) {
				runner.addFrame(anim.getSprite(x,y), 50);
			}
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		rocket.draw((int) r,100);
		runner.draw(250,250);
		g.scale(1.2f,1.2f);
		runner.draw(250,250);
		g.scale(1.2f,1.2f);
		runner.draw(250,250);
		g.resetTransform();
		
		g.rotate(670, 470, ang);
		sheet.getSprite("floppy").draw(600,400);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) {
		r += delta * 0.4f;
		if (r > 900) {
			r = -500;
		}
		
		ang += delta * 0.1f;
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new PackedSheetTest());
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
		if (key == Input.KEY_ESCAPE) {
			container.exit();
		}
	}
}
