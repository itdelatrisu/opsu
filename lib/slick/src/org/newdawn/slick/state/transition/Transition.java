package org.newdawn.slick.state.transition;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.GameState;
import org.newdawn.slick.state.StateBasedGame;

/**
 * A transition between two game states
 *
 * @author kevin
 */
public interface Transition {
	/** 
	 * Update the transition. Cause what ever happens in the transition to happen
	 * 
	 * @param game The game this transition is being rendered as part of
	 * @param container The container holding the game
	 * @param delta The amount of time passed since last update
	 * @throws SlickException Indicates a failure occured during the update 
	 */
	public void update(StateBasedGame game, GameContainer container, int delta) throws SlickException;

	/**
	 * Render the transition before the existing state rendering
	 * 
	 * @param game The game this transition is being rendered as part of
	 * @param container The container holding the game
	 * @param g The graphics context to use when rendering the transiton
	 * @throws SlickException Indicates a failure occured during the render 
	 */
	public void preRender(StateBasedGame game, GameContainer container, Graphics g) throws SlickException;
	
	/**
	 * Render the transition over the existing state rendering
	 * 
	 * @param game The game this transition is being rendered as part of
	 * @param container The container holding the game
	 * @param g The graphics context to use when rendering the transiton
	 * @throws SlickException Indicates a failure occured during the render 
	 */
	public void postRender(StateBasedGame game, GameContainer container, Graphics g) throws SlickException;
	
	/**
	 * Check if this transtion has been completed
	 * 
	 * @return True if the transition has been completed
	 */
	public boolean isComplete();
	
	/**
	 * Initialise the transition
	 * 
	 * @param firstState The first state we're rendering (this will be rendered by the framework)
	 * @param secondState The second stat we're transitioning to or from (this one won't be rendered)
	 */
	public void init(GameState firstState, GameState secondState);
}
