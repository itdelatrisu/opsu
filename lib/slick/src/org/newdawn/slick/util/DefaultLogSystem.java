package org.newdawn.slick.util;

import java.io.PrintStream;
import java.util.Date;

/**
 * The default implementation that just spits the messages out to stdout
 * 
 * @author kevin
 */
public class DefaultLogSystem implements LogSystem {
	/** The output stream for dumping the log out on */
	public static PrintStream out = System.out;

	/**
	 * Log an error
	 * 
	 * @param message The message describing the error
	 * @param e The exception causing the error
	 */
	public void error(String message, Throwable e) {
		error(message);
		error(e);
	}

	/**
	 * Log an error
	 * 
	 * @param e The exception causing the error
	 */
	public void error(Throwable e) {
		out.println(new Date()+" ERROR:" +e.getMessage());
		e.printStackTrace(out);
	}

	/**
	 * Log an error
	 * 
	 * @param message The message describing the error
	 */
	public void error(String message) {
		out.println(new Date()+" ERROR:" +message);
	}

	/**
	 * Log a warning
	 * 
	 * @param message The message describing the warning
	 */
	public void warn(String message) {
		out.println(new Date()+" WARN:" +message);
	}

	/**
	 * Log an information message
	 * 
	 * @param message The message describing the infomation
	 */
	public  void info(String message) {
		out.println(new Date()+" INFO:" +message);
	}

	/**
	 * Log a debug message
	 * 
	 * @param message The message describing the debug
	 */
	public void debug(String message) {
		out.println(new Date()+" DEBUG:" +message);
	}

	/**
	 * Log a warning with an exception that caused it
	 * 
	 * @param message The message describing the warning
	 * @param e The cause of the warning
	 */
	public void warn(String message, Throwable e) {
		warn(message);
		e.printStackTrace(out);
	}
}
