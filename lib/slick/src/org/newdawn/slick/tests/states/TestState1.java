package org.newdawn.slick.tests.states;

import org.newdawn.slick.AngelCodeFont;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.GameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.CrossStateTransition;
import org.newdawn.slick.state.transition.EmptyTransition;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

/**
 * A simple test state to display a message describing the test 
 *
 * @author kevin
 */
public class TestState1 extends BasicGameState {
	/** The ID given to this state */
	public static final int ID = 1;
	/** The font to write the message with */
	private Font font;
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
	}

	/**
	 * @see org.newdawn.slick.state.BasicGameState#render(org.newdawn.slick.GameContainer, org.newdawn.slick.state.StateBasedGame, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, StateBasedGame game, Graphics g) {
		g.setFont(font);
		g.setColor(Color.white);
		g.drawString("State Based Game Test", 100, 100);
		g.drawString("Numbers 1-3 will switch between states.", 150, 300);
		g.setColor(Color.red);
		g.drawString("This is State 1", 200, 50);
	}

	/**
	 * @see org.newdawn.slick.state.BasicGameState#update(org.newdawn.slick.GameContainer, org.newdawn.slick.state.StateBasedGame, int)
	 */
	public void update(GameContainer container, StateBasedGame game, int delta) {
	}

	/**
	 * @see org.newdawn.slick.state.BasicGameState#keyReleased(int, char)
	 */
	public void keyReleased(int key, char c) {
		
		if (key == Input.KEY_2) {
			GameState target = game.getState(TestState2.ID);
			
			final long start = System.currentTimeMillis();
			CrossStateTransition t = new CrossStateTransition(target) {				
				public boolean isComplete() {
					return (System.currentTimeMillis() - start) > 2000;
				}

				public void init(GameState firstState, GameState secondState) {
				}
			};
			
			game.enterState(TestState2.ID, t, new EmptyTransition());
		}
		if (key == Input.KEY_3) {
			game.enterState(TestState3.ID, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		}
	}
}
