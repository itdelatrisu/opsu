package org.newdawn.slick;

/**
 * Describes classes capable of responding to key presses
 * 
 * @author kevin
 */
public interface KeyListener extends ControlledInputReciever {
	/**
	 * Notification that a key was pressed
	 * 
	 * @param key The key code that was pressed (@see org.newdawn.slick.Input)
	 * @param c The character of the key that was pressed
	 */
	public abstract void keyPressed(int key, char c);

	/**
	 * Notification that a key was released
	 * 
	 * @param key The key code that was released (@see org.newdawn.slick.Input)
	 * @param c The character of the key that was released
	 */
	public abstract void keyReleased(int key, char c);

}