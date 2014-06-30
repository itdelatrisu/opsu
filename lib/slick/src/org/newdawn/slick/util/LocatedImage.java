package org.newdawn.slick.util;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

/**
 * An image along with state information that allows it to be drawn without
 * specifing the state in which to render.
 * 
 * @author kevin
 */
public class LocatedImage {
	/** The image to be rendered - prefer aggregation */
	private Image image;
	/** The x coordinate at which the image should be rendered */
	private int x;
	/** The y coordinate at which the image should be rendered */
	private int y;
	/** The filter to apply across the image */
	private Color filter = Color.white;
	/** The width to render the image */
	private float width;
	/** The height to render the image */
	private float height;
	
	/**
	 * Create a new located image
	 * 
	 * @param image The image to be drawn
	 * @param x The x location at which the image should be drawn
	 * @param y The y location at which the image should be drawn
	 */
	public LocatedImage(Image image, int x, int y) {
		this.image = image;
		this.x = x;
		this.y = y;
		this.width = image.getWidth();
		this.height = image.getHeight();
	}
	
	/**
	 * Get the height the image will be drawn at
	 * 
	 * @return The height
	 */
	public float getHeight() {
		return height;
	}

	/**
	 * Get the width the image will be drawn at
	 * 
	 * @return The width
	 */
	public float getWidth() {
		return width;
	}
	
	/**
	 * Set the height the image should be drawn at
	 * 
	 * @param height The height the image should be drawn at
	 */
	public void setHeight(float height) {
		this.height = height;
	}

	/**
	 * Set the width the image should be drawn at
	 * 
	 * @param width The width the image should be drawn at
	 */
	public void setWidth(float width) {
		this.width = width;
	}
	
	/**
	 * Set the colour filter to apply to the image
	 * 
	 * @param c The color filter to apply to the image
	 */ 
	public void setColor(Color c) {
		this.filter = c;
	}
	
	/**
	 * Get the colour filter being applied
	 * 
	 * @return The color the being applied
	 */
	public Color getColor() {
		return filter;
	}
	
	/**
	 * Set the x position at which the image should be drawn
	 * 
	 * @param x The x coordinate of the position
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * Set the y position at which the image should be drawn
	 * 
	 * @param y The y coordinate of the position
	 */
	public void setY(int y) {
		this.y = y;
	}
	
	/**
	 * Get the x position at which the image will be drawn
	 * 
	 * @return The x position at which the image will be drawn
	 */
	public int getX() {
		return x;
	}

	/**
	 * Get the y position at which the image will be drawn
	 * 
	 * @return The y position at which the image will be drawn
	 */
	public int getY() {
		return y;
	}
	
	/**
	 * Draw the image based on the current configured state
	 */
	public void draw() {
		image.draw(x,y,width,height,filter);
	}
}
