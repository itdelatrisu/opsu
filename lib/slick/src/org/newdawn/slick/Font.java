package org.newdawn.slick;


/**
 * The proprites of any font implementation
 * 
 * @author Kevin Glass
 */
public interface Font {
	/**
	 * Get the width of the given string
	 * 
	 * @param str The string to obtain the rendered with of
	 * @return The width of the given string
	 */
	public abstract int getWidth(String str);
	
	/**
	 * Get the height of the given string
	 * 
	 * @param str The string to obtain the rendered with of
	 * @return The width of the given string
	 */
	public abstract int getHeight(String str);
	
	/**
	 * Get the maximum height of any line drawn by this font
	 * 
	 * @return The maxium height of any line drawn by this font
	 */
	public int getLineHeight();
	
	/**
	 * Draw a string to the screen
	 * 
	 * @param x The x location at which to draw the string
	 * @param y The y location at which to draw the string
	 * @param text The text to be displayed
	 */
	public abstract void drawString(float x, float y, String text);

	/**
	 * Draw a string to the screen
	 * 
	 * @param x The x location at which to draw the string
	 * @param y The y location at which to draw the string
	 * @param text The text to be displayed
	 * @param col The colour to draw with
	 */
	public abstract void drawString(float x, float y, String text, Color col);


	/**
	 * Draw part of a string to the screen. Note that this will
	 * still position the text as though it's part of the bigger string.
	 * 
	 * @param x The x location at which to draw the string
	 * @param y The y location at which to draw the string
	 * @param text The text to be displayed
	 * @param col The colour to draw with
	 * @param startIndex The index of the first character to draw
	 * @param endIndex The index of the last character from the string to draw
	 */
	public abstract void drawString(float x, float y, String text, Color col, int startIndex, int endIndex);
}