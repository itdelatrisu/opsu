package org.newdawn.slick.util;

/**
 * Plugin in interface for the logging of Slick
 * 
 * @author kevin
 */
public interface LogSystem {

	/**
	 * Log an error
	 * 
	 * @param message The message describing the error
	 * @param e The exception causing the error
	 */
	public void error(String message, Throwable e);

	/**
	 * Log an error
	 * 
	 * @param e The exception causing the error
	 */
	public void error(Throwable e);

	/**
	 * Log an error
	 * 
	 * @param message The message describing the error
	 */
	public void error(String message);
	
	/**
	 * Log a warning
	 * 
	 * @param message The message describing the warning
	 */
	public void warn(String message);

	/**
	 * Log a warning
	 * 
	 * @param message The message describing the warning
	 * @param e The cause of the warning
	 */
	public void warn(String message, Throwable e);
	
	/**
	 * Log an information message
	 * 
	 * @param message The message describing the infomation
	 */
	public void info(String message);

	/**
	 * Log a debug message
	 * 
	 * @param message The message describing the debug
	 */
	public void debug(String message);
}
