package org.newdawn.slick;

/**
 * Description of anything that can be drawn
 * 
 * @author kevin
 */
public interface Renderable {

	/**
	 * Draw this artefact at the given location
	 * 
	 * @param x The x coordinate to draw the artefact at
	 * @param y The y coordinate to draw the artefact at 
	 */
	public void draw(float x, float y);
	
}
