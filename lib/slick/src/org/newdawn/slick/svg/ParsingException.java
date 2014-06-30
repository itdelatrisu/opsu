package org.newdawn.slick.svg;

import org.newdawn.slick.SlickException;
import org.w3c.dom.Element;

/**
 * Exception indicating a failure to parse XML, giving element information
 *
 * @author kevin
 */
public class ParsingException extends SlickException {

	/**
	 * Create a new exception
	 * 
	 * @param nodeID The ID of the node that failed validation 
	 * @param message The description of the failure
	 * @param cause The exception causing this one
	 */
	public ParsingException(String nodeID, String message, Throwable cause) {
		super("("+nodeID+") "+message, cause);
	}

	/**
	 * Create a new exception
	 * 
	 * @param element The element that failed validation 
	 * @param message The description of the failure
	 * @param cause The exception causing this one
	 */
	public ParsingException(Element element, String message, Throwable cause) {
		super("("+element.getAttribute("id")+") "+message, cause);
	}

	/**
	 * Create a new exception
	 * 
	 * @param nodeID The ID of the node that failed validation 
	 * @param message The description of the failure
	 */
	public ParsingException(String nodeID, String message) {
		super("("+nodeID+") "+message);
	}

	/**
	 * Create a new exception
	 * 
	 * @param element The element that failed validation 
	 * @param message The description of the failure
	 */
	public ParsingException(Element element, String message) {
		super("("+element.getAttribute("id")+") "+message);
	}
}
