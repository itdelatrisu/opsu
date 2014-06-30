package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.BlobbyTransition;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;
import org.newdawn.slick.state.transition.HorizontalSplitTransition;
import org.newdawn.slick.state.transition.RotateTransition;
import org.newdawn.slick.state.transition.SelectTransition;
import org.newdawn.slick.state.transition.Transition;
import org.newdawn.slick.state.transition.VerticalSplitTransition;
import org.newdawn.slick.util.Log;

/**
 * A test to view the different transitions that are currently implemented
 * 
 * @author kevin
 */
public class TransitionTest extends StateBasedGame {
	/** The transitions under test */
	private Class[][] transitions = new Class[][] {
			{null, VerticalSplitTransition.class},
			{FadeOutTransition.class, FadeInTransition.class},
			{null, RotateTransition.class},
			{null, HorizontalSplitTransition.class},
			{null, BlobbyTransition.class},
			{null, SelectTransition.class},
	};
	/** The index of the next transition to use */
	private int index;
	
	/**
	 * Test the transitions implemented
	 */
	public TransitionTest() {
		super("Transition Test - Hit Space To Transition");
	}
	
	/**
	 * @see org.newdawn.slick.state.StateBasedGame#initStatesList(org.newdawn.slick.GameContainer)
	 */
	public void initStatesList(GameContainer container) throws SlickException {
		addState(new ImageState(0, "testdata/wallpaper/paper1.png", 1));
		addState(new ImageState(1, "testdata/wallpaper/paper2.png", 2));
		addState(new ImageState(2, "testdata/bigimage.tga", 0));
	}

	/**
	 * Get the next transition pair that we'lluse
	 * 
	 * @return The pair of transitions used to enter and leave the next state
	 */
	public Transition[] getNextTransitionPair() {
		Transition[] pair = new Transition[2];
		
		try {
			if (transitions[index][0] != null) {
				pair[0] = (Transition) transitions[index][0].newInstance();
			}
			if (transitions[index][1] != null) {
				pair[1] = (Transition) transitions[index][1].newInstance();
			}
		} catch (Throwable e) {
			Log.error(e);
		}
		
		index++;
		if (index >= transitions.length) {
			index = 0;
		}
		
		return pair;
	}
	
	/**
	 * A test state that just displayed one image full scren
	 * 
	 * @author kevin
	 */
	private class ImageState extends BasicGameState {
		/** The id of this state */
		private int id;
		/** The next state we'll move to */
		private int next;
		/** The reference to the image to be displayed */
		private String ref;
		/** The loaded image */
		private Image image;
		
		/**
		 * Create a new image state
		 * 
		 * @param id The id of the this state
		 * @param ref The reference to the image to display
		 * @param next The next state we'll mvoe to
		 */
		public ImageState(int id, String ref, int next) {
			this.ref = ref;
			this.id = id;
			this.next = next;
		}

		/**
		 * @see org.newdawn.slick.state.BasicGameState#getID()
		 */
		public int getID() {
			return id;
		}

		/**
		 * @see org.newdawn.slick.state.GameState#init(org.newdawn.slick.GameContainer, org.newdawn.slick.state.StateBasedGame)
		 */
		public void init(GameContainer container, StateBasedGame game) throws SlickException {
			image = new Image(ref);
		}

		/**
		 * @see org.newdawn.slick.state.GameState#render(org.newdawn.slick.GameContainer, org.newdawn.slick.state.StateBasedGame, org.newdawn.slick.Graphics)
		 */
		public void render(GameContainer container, StateBasedGame game, Graphics g) throws SlickException {
			image.draw(0,0,800,600);
			g.setColor(Color.red);
			g.fillRect(-50,200,50,50);
		}

		/**
		 * @see org.newdawn.slick.state.GameState#update(org.newdawn.slick.GameContainer, org.newdawn.slick.state.StateBasedGame, int)
		 */
		public void update(GameContainer container, StateBasedGame game, int delta) throws SlickException {
			if (container.getInput().isKeyPressed(Input.KEY_SPACE)) {
				Transition[] pair = getNextTransitionPair();
				game.enterState(next, pair[0], pair[1]);
			}
		}
	}
	
	/**
	 * Entry point to our test
	 * 
	 * @param argv
	 *            The arguments passed to the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(
					new TransitionTest());
			container.setDisplayMode(800, 600, false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
