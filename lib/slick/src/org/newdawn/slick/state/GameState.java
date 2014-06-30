package org.newdawn.slick.state;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.InputListener;
import org.newdawn.slick.SlickException;

/**
 * A single state building up part of the game. The state include rendering, logic and input handling
 * for the state.
 *
 * @author kevin
 */
public interface GameState extends InputListener {
	/**
	 * Get the ID of this state
	 * 
	 * @return The game unique ID of this state
	 */
	public int getID();
	
	/**
	 * Initialise the state. It should load any resources it needs at this stage
	 * 
	 * @param container The container holding the game
	 * @param game The game holding this state
	 * @throws SlickException Indicates a failure to initialise a resource for this state
	 */
	public void init(GameContainer container, StateBasedGame game) throws SlickException;
	
	/**
	 * Render this state to the game's graphics context
	 * 
	 * @param container The container holding the game
	 * @param game The game holding this state
	 * @param g The graphics context to render to
	 * @throws SlickException Indicates a failure to render an artifact
	 */
	public void render(GameContainer container, StateBasedGame game, Graphics g) throws SlickException;
	
	/**
	 * Update the state's logic based on the amount of time thats passed
	 * 
	 * @param container The container holding the game
	 * @param game The game holding this state
	 * @param delta The amount of time thats passed in millisecond since last update
	 * @throws SlickException Indicates an internal error that will be reported through the
	 * standard framework mechanism
	 */
	public void update(GameContainer container, StateBasedGame game, int delta) throws SlickException ;
	
	/**
	 * Notification that we've entered this game state
	 * 
	 * @param container The container holding the game
	 * @param game The game holding this state
	 * @throws SlickException Indicates an internal error that will be reported through the
	 * standard framework mechanism
	 */
	public void enter(GameContainer container, StateBasedGame game) throws SlickException;

	/**
	 * Notification that we're leaving this game state
	 * 
	 * @param container The container holding the game
	 * @param game The game holding this state
	 * @throws SlickException Indicates an internal error that will be reported through the
	 * standard framework mechanism
	 */
	public void leave(GameContainer container, StateBasedGame game) throws SlickException;
}
