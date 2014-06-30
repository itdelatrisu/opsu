package org.newdawn.slick.svg;

import java.util.Properties;

import org.newdawn.slick.Color;

/**
 * A set of data about a shape that doesn't fit into it's geometric 
 * configuration.
 *
 * @author kevin
 */
public class NonGeometricData {
	/** The ID of the figure */
	public static final String ID = "id";
	/** The fill type */
	public static final String FILL = "fill";
	/** The stroke type */
	public static final String STROKE = "stroke";
	/** The alpha value for filling */
	public static final String OPACITY = "opacity";
	/** The width of the line to draw */
	public static final String STROKE_WIDTH = "stroke-width";
	/** The mitre of the line to draw */
	public static final String STROKE_MITERLIMIT = "stroke-miterlimit";
	/** The dash definition of the line to draw */
	public static final String STROKE_DASHARRAY = "stroke-dasharray";
	/** The offset into the dash definition of the line to draw */
	public static final String STROKE_DASHOFFSET = "stroke-dashoffset";
	/** The alpha value for drawing */
	public static final String STROKE_OPACITY = "stroke-opacity";
	
	/** Value indicating that no settings has been specified */
	public static final String NONE = "none";
	
	/** The meta data stored for the figure */
	private String metaData = "";
	/** The attributes stored for the figure */
	private Properties props = new Properties();
	
	/**
	 * Create a set of non-geometric data for a figure
	 * 
	 * @param metaData The meta data (either label or id) for the figure
	 */
	public NonGeometricData(String metaData) {
		this.metaData = metaData;
		addAttribute(STROKE_WIDTH, "1");
	}
	
	/**
	 * Morph the color from a string
	 * 
	 * @param str The string to morph
	 * @return The new color string
	 */
	private String morphColor(String str) {
		if (str.equals("")) {
			return "#000000";
		}
		if (str.equals("white")) {
			return "#ffffff";
		}
		if (str.equals("black")) {
			return "#000000";
		}
		
		return str;
	}
	
	/**
	 * Add a configured style attribute into the data set
	 * 
	 * @param attribute The attribute to add  
	 * @param value The value to assign
	 */
	public void addAttribute(String attribute, String value) {
		if (value == null) {
			value = "";
		}
		
		if (attribute.equals(FILL) ) {
			value = morphColor(value);
		}
		if (attribute.equals(STROKE_OPACITY)) {
			if (value.equals("0")) {
				props.setProperty(STROKE, "none");
			}
		}
		if (attribute.equals(STROKE_WIDTH)) {
			if (value.equals("")) {
				value = "1";
			}
			if (value.endsWith("px")) {
				value = value.substring(0,value.length()-2);
			}
		}
		if (attribute.equals(STROKE)) {
			if ("none".equals(props.getProperty(STROKE))) {
				return;
			}
			if ("".equals(props.getProperty(STROKE))) {
				return;
			}
			value = morphColor(value);
		}
		
		props.setProperty(attribute, value);
	}

	/**
	 * Check if a given attribute is in colour format
	 * 
	 * @param attribute The attribute to check
	 * @return True if the attirbute value is in colour format
	 */
	public boolean isColor(String attribute) {
		return getAttribute(attribute).startsWith("#");
	}
	
	/**
	 * Get the meta data assigned to the figure. Either the label or
	 * the id value.
	 * 
	 * @return The meta data assigned to the figure
	 */
	public String getMetaData() {
		return metaData;
	}
	
	/**
	 * Get the attribtue value for a given attribute
	 * 
	 * @param attribute The attribute whose value should be obtained
	 * @return The value for the given attribute
	 */
	public String getAttribute(String attribute) {
		return props.getProperty(attribute);
	}
	
	/**
	 * Get an attribute value converted to a color. isColor should first be checked
	 * 
	 * @param attribute The attribute whose value should be interpreted as a color
	 * @return The color based on the attribute
	 */
	public Color getAsColor(String attribute) {
		if (!isColor(attribute)) {
			throw new RuntimeException("Attribute "+attribute+" is not specified as a color:"+getAttribute(attribute));
		}
		
		int col = Integer.parseInt(getAttribute(attribute).substring(1), 16);
		
		return new Color(col);
	}
	
	/**
	 * Get the attribute value as a reference to another entity
	 * 
	 * @param attribute The name of the attribute to retrieve
	 * @return The reference part of the attribute value
	 */
	public String getAsReference(String attribute) {
		String value = getAttribute(attribute);
		if (value.length() < 7) {
			return "";
		}
		
		value = value.substring(5, value.length()-1);
		
		return value;
	}
	
	/**
	 * Get an attribute converted to a float value
	 * 
	 * @param attribute The attribute to retrieve
	 * @return The float value derived from the attribute
	 */
	public float getAsFloat(String attribute) {
		String value = getAttribute(attribute);
		if (value == null) {
			return 0;
		}
		
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Attribute "+attribute+" is not specified as a float:"+getAttribute(attribute));			
		}
	}

	/**
	 * True if the shape is meant to be filled
	 * 
	 * @return True if the shape is meant to be filled
	 */
	public boolean isFilled() {
		return isColor(NonGeometricData.FILL);
	}
	
	/**
	 * True if the shape is meant to be outlined
	 * 
	 * @return True if the shape is meant to be outlined
	 */
	public boolean isStroked() {
		return isColor(NonGeometricData.STROKE) && (getAsFloat(NonGeometricData.STROKE_WIDTH) > 0);
	}
}
