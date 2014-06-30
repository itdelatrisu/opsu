package org.newdawn.slick.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A simple central logging system
 * 
 * @author kevin
 */
public final class Log {
	/** True if we're doing verbose logging INFO and DEBUG */
	private static boolean verbose = true;
	/** true if activated by the system property "org.newdawn.slick.forceVerboseLog" */
	private static boolean forcedVerbose = false;
	
	/**
	 * The debug property which can be set via JNLP or startup parameter to switch
	 * logging mode to verbose for games that were released without verbose logging
	 * value must be "true"
	 */
	private static final String forceVerboseProperty = "org.newdawn.slick.forceVerboseLog";
	
	/**
	 * the verbose property must be set to "true" to switch on verbose logging
	 */
	private static final String forceVerbosePropertyOnValue = "true";
	
	/** The log system plugin in use */
	private static LogSystem logSystem = new DefaultLogSystem();
	
	/**
	 * The log is a simple static utility, no construction
	 */
	private Log() {
		
	}
	
	/**
	 * Set the log system that will have all of the log info 
	 * sent to it.
	 * 
	 * @param system The system to use for logging.
	 */
	public static void setLogSystem(LogSystem system) {
		logSystem = system;
	}
	
	/**
	 * Indicate that we want verbose logging.
	 * The call is ignored if verbose logging is forced by the system property
	 * "org.newdawn.slick.forceVerboseLog"
	 * 
	 * @param v True if we want verbose logging (INFO and DEBUG)
	 */
	public static void setVerbose(boolean v) {
		if (forcedVerbose)
			return;
		verbose = v;
	}

	/**
	 * Check if the system property org.newdawn.slick.verboseLog is set to true.
	 * If this is the case we activate the verbose logging mode
	 */
	public static void checkVerboseLogSetting() {
		try {
			AccessController.doPrivileged(new PrivilegedAction() {
	            public Object run() {
					String val = System.getProperty(Log.forceVerboseProperty);
					if ((val != null) && (val.equalsIgnoreCase(Log.forceVerbosePropertyOnValue))) {
						Log.setForcedVerboseOn();
					}
					
					return null;
	            }
			});
		} catch (Throwable e) {
			// ignore, security failure - probably an applet
		}
	}
	
	/**
	 * Indicate that we want verbose logging, even if switched off in game code.
	 * Only be called when system property "org.newdawn.slick.forceVerboseLog" is set to true.
	 * You must not call this method directly.
	 */
	public static void setForcedVerboseOn() {
		forcedVerbose = true;
		verbose = true;
	}
	
	/**
	 * Log an error
	 * 
	 * @param message The message describing the error
	 * @param e The exception causing the error
	 */
	public static void error(String message, Throwable e) {
		logSystem.error(message, e);
	}

	/**
	 * Log an error
	 * 
	 * @param e The exception causing the error
	 */
	public static void error(Throwable e) {
		logSystem.error(e);
	}

	/**
	 * Log an error
	 * 
	 * @param message The message describing the error
	 */
	public static void error(String message) {
		logSystem.error(message);
	}

	/**
	 * Log a warning
	 * 
	 * @param message The message describing the warning
	 */
	public static void warn(String message) {
		logSystem.warn(message);
	}
	
	/**
	 * Log a warning
	 * 
	 * @param message The message describing the warning
	 * @param e The issue causing the warning
	 */
	public static void warn(String message, Throwable e) {
		logSystem.warn(message, e);
	}

	/**
	 * Log an information message
	 * 
	 * @param message The message describing the infomation
	 */
	public static void info(String message) {
		if (verbose || forcedVerbose) {
			logSystem.info(message);
		}
	}

	/**
	 * Log a debug message
	 * 
	 * @param message The message describing the debug
	 */
	public static void debug(String message) {
		if (verbose || forcedVerbose) {
			logSystem.debug(message);
		}
	}
}
