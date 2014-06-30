package org.newdawn.slick;

/**
 * A generic exception thrown by everything in the library
 *
 * @author kevin
 */
public class SlickException extends Exception {
	/**
	 * Create a new exception with a detail message
	 * 
	 * @param message The message describing the cause of this exception
	 */
	public SlickException(String message) {
		super(message);
	}

	/**
	 * Create a new exception with a detail message
	 * 
	 * @param message The message describing the cause of this exception
	 * @param e The exception causing this exception to be thrown
	 */
	public SlickException(String message, Throwable e) {
		super(message, e);
	}
}
