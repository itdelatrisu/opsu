package org.newdawn.slick.gui;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

/**
 * Renamed to provide backwards compatibility
 *
 * @author kevin
 * @deprecated
 */
public abstract class BasicComponent extends AbstractComponent {
	/** The x position of the component */
	protected int x;
	/** The y position of the component */
	protected int y;
	/** The width of the component */
	protected int width;
	/** The height of the component */
	protected int height;

	/**
	 * Create a new component
	 * 
	 * @param container
	 *            The container displaying this component
	 */
	public BasicComponent(GUIContext container) {
		super(container);
	}
	
	/**
	 * @see org.newdawn.slick.gui.AbstractComponent#getHeight()
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @see org.newdawn.slick.gui.AbstractComponent#getWidth()
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @see org.newdawn.slick.gui.AbstractComponent#getX()
	 */
	public int getX() {
		return x;
	}

	/**
	 * @see org.newdawn.slick.gui.AbstractComponent#getY()
	 */
	public int getY() {
		return y;
	}

	/**
	 * Allow the sub-component to render
	 * 
	 * @param container The container holding the GUI
	 * @param g The graphics context into which we should render
	 */
	public abstract void renderImpl(GUIContext container, Graphics g);
	
	/**
	 * @see org.newdawn.slick.gui.AbstractComponent#render(org.newdawn.slick.gui.GUIContext, org.newdawn.slick.Graphics)
	 */
	public void render(GUIContext container, Graphics g) throws SlickException {
		renderImpl(container,g);
	}

	/**
	 * @see org.newdawn.slick.gui.AbstractComponent#setLocation(int, int)
	 */
	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

}
