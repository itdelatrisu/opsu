package org.newdawn.slick.gui;

/**
 * A descritpion of a class responding to events occuring on a GUI component
 *
 * @author kevin
 */
public interface ComponentListener {

	/**
	 * Notification that a component has been activated (button clicked,
	 * text field entered, etc)
	 * 
	 * @param source The source of the event
	 */
	public void componentActivated(AbstractComponent source);
}
