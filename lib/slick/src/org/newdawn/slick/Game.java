package org.newdawn.slick;

/**
 * The main game interface that should be implemented by any game being developed
 * using the container system. There will be some utility type sub-classes as development
 * continues.
 * 
 * @see org.newdawn.slick.BasicGame
 *
 * @author kevin
 */
public interface Game {
	/**
	 * Initialise the game. This can be used to load static resources. It's called
	 * before the game loop starts
	 * 
	 * @param container The container holding the game
	 * @throws SlickException Throw to indicate an internal error
	 */
	public void init(GameContainer container) throws SlickException;
	
	/**
	 * Update the game logic here. No rendering should take place in this method
	 * though it won't do any harm. 
	 * 
	 * @param container The container holing this game
	 * @param delta The amount of time thats passed since last update in milliseconds
	 * @throws SlickException Throw to indicate an internal error
	 */
	public void update(GameContainer container, int delta) throws SlickException;
	
	/**
	 * Render the game's screen here. 
	 * 
	 * @param container The container holing this game
	 * @param g The graphics context that can be used to render. However, normal rendering
	 * routines can also be used.
 	 * @throws SlickException Throw to indicate a internal error
	 */
	public void render(GameContainer container, Graphics g) throws SlickException;
	
	/**
	 * Notification that a game close has been requested
	 * 
	 * @return True if the game should close
	 */
	public boolean closeRequested();
	
	/**
	 * Get the title of this game 
	 * 
	 * @return The title of the game
	 */
	public String getTitle();
}
