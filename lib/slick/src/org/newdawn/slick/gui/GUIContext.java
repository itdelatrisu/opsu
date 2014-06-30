package org.newdawn.slick.gui;

import org.lwjgl.input.Cursor;
import org.newdawn.slick.Font;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.ImageData;

/**
 * The context in which GUI components are created and rendered
 *
 * @author kevin
 */
public interface GUIContext {

	/**
	 * Get the input system
	 * 
	 * @return The input system available to this game container
	 */
	public Input getInput();
	
	/**
	 * Get the accurate system time
	 * 
	 * @return The system time in milliseconds
	 */
	public long getTime();
	
	/**
	 * Get the width of the standard screen resolution
	 * 
	 * @return The screen width
	 */
	public abstract int getScreenWidth();
	
	/**
	 * Get the height of the standard screen resolution
	 * 
	 * @return The screen height
	 */
	public abstract int getScreenHeight();
	
	/**
	 * Get the width of the game canvas
	 * 
	 * @return The width of the game canvas
	 */
	public int getWidth();
	
	/**
	 * Get the height of the game canvas
	 * 
	 * @return The height of the game canvas
	 */
	public int getHeight();
	
	/**
	 * Get the default system font
	 * 
	 * @return The default system font
	 */
	public Font getDefaultFont();
	
	/**
	 * Set the mouse cursor to be displayed - this is a hardware cursor and hence
	 * shouldn't have any impact on FPS.
	 * 
	 * @param ref The location of the image to be loaded for the cursor
	 * @param hotSpotX The x coordinate of the hotspot within the cursor image
	 * @param hotSpotY The y coordinate of the hotspot within the cursor image
	 * @throws SlickException Indicates a failure to load the cursor image or create the hardware cursor
	 */
	public abstract void setMouseCursor(String ref, int hotSpotX, int hotSpotY) throws SlickException;

	/**
	 * Set the mouse cursor to be displayed - this is a hardware cursor and hence
	 * shouldn't have any impact on FPS.
	 * 
	 * @param data The image data from which the cursor can be construted
	 * @param hotSpotX The x coordinate of the hotspot within the cursor image
	 * @param hotSpotY The y coordinate of the hotspot within the cursor image
	 * @throws SlickException Indicates a failure to load the cursor image or create the hardware cursor
	 */
	public abstract void setMouseCursor(ImageData data, int hotSpotX, int hotSpotY) throws SlickException;

	/**
	 * Set the mouse cursor to be displayed - this is a hardware cursor and hence
	 * shouldn't have any impact on FPS.
	 * 
	 * @param cursor The cursor to use
	 * @param hotSpotX The x coordinate of the hotspot within the cursor image
	 * @param hotSpotY The y coordinate of the hotspot within the cursor image
	 * @throws SlickException Indicates a failure to load the cursor image or create the hardware cursor
	 */
	public abstract void setMouseCursor(Cursor cursor, int hotSpotX, int hotSpotY) throws SlickException;
	
	/**
	 * Set the default mouse cursor - i.e. the original cursor before any native 
	 * cursor was set
	 */
	public abstract void setDefaultMouseCursor();
}
