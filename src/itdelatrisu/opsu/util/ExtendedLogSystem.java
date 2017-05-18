/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2017 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */
package itdelatrisu.opsu.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.newdawn.slick.util.LogSystem;

/**
 * An extension to the logging system used by Slick.
 * <br><br>
 * This implementation logs messages to both the console and file
 * for easier access to logs, useful for people who often launch opsu!
 * in scripts.
 * 
 * @author Lyonlancer5
 */
public class ExtendedLogSystem implements LogSystem {
	
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("HH:mm:ss");
	private final String name;
	
	private final PrintStream console, file;
	
	public ExtendedLogSystem(String name){
		this.name = name;
		this.console = System.out;
		File logFile = new File(".opsu.log");
		
		PrintStream f = null;
		
		try {
			f = new PrintStream(logFile);
		} catch (IOException e) {
			warn("Could not log-to-file", e);
		}
		
		this.file = f;
		if(this.file != null)
			this.file.println("# Log-to-file : " + new Date());
	}
	
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
		console.println(formatMessage(e.getMessage(), "ERROR"));
		e.printStackTrace(console);
		if(file != null){
			file.println(formatMessage(e.getMessage(), "ERROR"));
			e.printStackTrace(file);
		}
	}

	/**
	 * Log an error
	 * 
	 * @param message The message describing the error
	 */
	public void error(String message) {
		console.println(formatMessage(message, "ERROR"));
		if(file != null)file.println(formatMessage(message, "ERROR"));
	}

	/**
	 * Log a warning
	 * 
	 * @param message The message describing the warning
	 */
	public void warn(String message) {
		console.println(formatMessage(message, "WARN"));
		if(file != null)file.println(formatMessage(message, "WARN"));
	}

	/**
	 * Log an information message
	 * 
	 * @param message The message describing the infomation
	 */
	public  void info(String message) {
		console.println(formatMessage(message, "INFO"));
		if(file != null)file.println(formatMessage(message, "INFO"));
	}

	/**
	 * Log a debug message
	 * 
	 * @param message The message describing the debug
	 */
	public void debug(String message) {
		console.println(formatMessage(message, "DEBUG"));
		if(file != null)file.println(formatMessage(message, "DEBUG"));
	}

	/**
	 * Log a warning with an exception that caused it
	 * 
	 * @param message The message describing the warning
	 * @param e The cause of the warning
	 */
	public void warn(String message, Throwable e) {
		warn(message);
		e.printStackTrace(console);
		if(file != null)e.printStackTrace(file);
	}
	
	
	/**
	 * Format the message given
	 * 
	 * @param message The message to format
	 */
	private String formatMessage(String message, String level){
		StringBuilder builder = new StringBuilder();
		//time
		builder.append("[").append(FORMAT.format(new Date())).append("] ");
		//thread info
		builder.append("[").append(Thread.currentThread().getName()).append("/").append(level).append("] ");
		//name
		builder.append("[").append(this.name).append("]: ");
		//message
		return builder.append(message).toString();
	}
}
