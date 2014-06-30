package org.newdawn.slick.svg;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A diagram read from SVG containing multiple figures
 *
 * @author kevin
 */
public class Diagram {
	/** The figures in the diagram */
	private ArrayList figures = new ArrayList();
	/** The pattern definitions */
	private HashMap patterns = new HashMap();
	/** The linear gradients defined within the diagram */
	private HashMap gradients = new HashMap();
	/** The figures mapping */
	private HashMap figureMap = new HashMap();
	
	/** The width of the diagram */
	private float width;
	/** The height of the diagram */
	private float height;
	
	/**
	 * Create a new empty diagram
	 * 
	 * @param width The width of the diagram
	 * @param height The height of the diagram
	 */
	public Diagram(float width, float height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * Get the width of the diagram
	 * 
	 * @return The width of the diagram
	 */
	public float getWidth() {
		return width;
	}
	
	/**
	 * Get the height of the diagram
	 * 
	 * @return The height of the diagram
	 */
	public float getHeight() {
		return height;
	}
	
	/**
	 * Add a pattern definition basd on a image
	 * 
	 * @param name The name of the pattern
	 * @param href The href to the image specified in the doc
	 */
	public void addPatternDef(String name, String href) {
		patterns.put(name, href);
	}
	
	/**
	 * Add gradient to the diagram 
	 * 
	 * @param name The name of the gradient
	 * @param gradient The gradient to be added
	 */
	public void addGradient(String name, Gradient gradient) {
		gradients.put(name, gradient);
	}
	
	/**
	 * Get a pattern definition from the diagram
	 * 
	 * @param name The name of the pattern
	 * @return The href to the image that was specified for the given pattern
	 */
	public String getPatternDef(String name) {
		return (String) patterns.get(name);
	}
	
	/**
	 * Get the gradient defined in this document
	 * 
	 * @param name The name of the gradient
	 * @return The gradient definition
	 */
	public Gradient getGradient(String name) {
		return (Gradient) gradients.get(name);
	}
	
	/**
	 * Get the names of the patterns defined 
	 * 
	 * @return The names of the pattern
	 */
	public String[] getPatternDefNames() {
		return (String[]) patterns.keySet().toArray(new String[0]);		
	}
	
	/**
	 * Get a figure by a given ID
	 * 
	 * @param id The ID of the figure
	 * @return The figure with the given ID
	 */
	public Figure getFigureByID(String id) {
		return (Figure) figureMap.get(id);
	}
	
	/**
	 * Add a figure to the diagram
	 * 
	 * @param figure The figure to add
	 */
	public void addFigure(Figure figure) {
		figures.add(figure);
		figureMap.put(figure.getData().getAttribute(NonGeometricData.ID), figure);
		
		String fillRef = figure.getData().getAsReference(NonGeometricData.FILL);
		Gradient gradient = getGradient(fillRef);
		if (gradient != null) {
			if (gradient.isRadial()) {
				for (int i=0;i<InkscapeLoader.RADIAL_TRIANGULATION_LEVEL;i++) {
					figure.getShape().increaseTriangulation();
				}
			}
		}
	}
	
	/**
	 * Get the number of figures in the diagram
	 * 
	 * @return The number of figures in the diagram
	 */
	public int getFigureCount() {
		return figures.size();
	}
	
	/**
	 * Get the figure at a given index
	 * 
	 * @param index The index of the figure to retrieve
	 * @return The figure at the given index
	 */
	public Figure getFigure(int index) {
		return (Figure) figures.get(index);
	}
	
	/**
	 * Remove a figure from the diagram
	 * 
	 * @param figure The figure to be removed
	 */
	public void removeFigure(Figure figure) {
		figures.remove(figure);
		figureMap.remove(figure.getData().getAttribute("id"));
	}
}
