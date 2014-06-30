package org.newdawn.slick.tests.states;

import org.newdawn.slick.AngelCodeFont;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

/**
 * A simple test state to display an image and rotate it
 *
 * @author kevin
 */
public class TestState2 extends BasicGameState {
	/** The ID given to this state */
	public static final int ID = 2;
	/** The font to write the message with */
	private Font font;
	/** The image to be display */
	private Image image;
	/** The angle we'll rotate by */
	private float ang;
	/** The game holding this state */
	private StateBasedGame game;
	
	/**
	 * @see org.newdawn.slick.state.BasicGameState#getID()
	 */
	public int getID() {
		return ID;
	}

	/**
	 * @see org.newdawn.slick.state.BasicGameState#init(org.newdawn.slick.GameContainer, org.newdawn.slick.state.StateBasedGame)
	 */
	public void init(GameContainer container, StateBasedGame game) throws SlickException {
		this.game = game;
		font = new AngelCodeFont("testdata/demo2.fnt","testdata/demo2_00.tga");
		image = new Image("testdata/logo.tga");
	}

	/**
	 * @see org.newdawn.slick.state.BasicGameState#render(org.newdawn.slick.GameContainer, org.newdawn.slick.state.StateBasedGame, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, StateBasedGame game, Graphics g) {
		g.setFont(font);
		g.setColor(Color.green);
		g.drawString("This is State 2", 200, 50);
		
		g.rotate(400,300,ang);
		g.drawImage(image,400-(image.getWidth()/2),300-(image.getHeight()/2));
	}

	/**
	 * @see org.newdawn.slick.state.BasicGameState#update(org.newdawn.slick.GameContainer, org.newdawn.slick.state.StateBasedGame, int)
	 */
	public void update(GameContainer container, StateBasedGame game, int delta) {
		ang += delta * 0.1f;
	}
	
	/**
	 * @see org.newdawn.slick.state.BasicGameState#keyReleased(int, char)
	 */
	public void keyReleased(int key, char c) {
		if (key == Input.KEY_1) {
			game.enterState(TestState1.ID, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		}
		if (key == Input.KEY_3) {
			game.enterState(TestState3.ID, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		}
	}
}
