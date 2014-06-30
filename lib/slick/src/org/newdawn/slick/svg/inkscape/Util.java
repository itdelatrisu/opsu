package org.newdawn.slick.svg.inkscape;

import java.util.StringTokenizer;

import org.newdawn.slick.geom.Transform;
import org.newdawn.slick.svg.NonGeometricData;
import org.newdawn.slick.svg.ParsingException;
import org.w3c.dom.Element;

/**
 * A set of utility for processing the SVG documents produced by Inkscape
 *
 * @author kevin
 */
public class Util {
	/** The namespace for inkscape */
	public static final String INKSCAPE = "http://www.inkscape.org/namespaces/inkscape";
	/** The namespace for sodipodi */
	public static final String SODIPODI = "http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd";
	/** The namespace for xlink */
	public static final String XLINK = "http://www.w3.org/1999/xlink";

	/**
	 * Get the non-geometric data information from an XML element
	 * 
	 * @param element The element to be processed
	 * @return The non-geometric data (i.e. stroke, fill, etc)
	 */
	static NonGeometricData getNonGeometricData(Element element) {
		String meta = getMetaData(element);
		
		NonGeometricData data = new InkscapeNonGeometricData(meta, element);
		data.addAttribute(NonGeometricData.ID, element.getAttribute("id"));
		data.addAttribute(NonGeometricData.FILL, getStyle(element, NonGeometricData.FILL));
		data.addAttribute(NonGeometricData.STROKE, getStyle(element, NonGeometricData.STROKE));
		data.addAttribute(NonGeometricData.OPACITY, getStyle(element, NonGeometricData.OPACITY));
		data.addAttribute(NonGeometricData.STROKE_DASHARRAY, getStyle(element, NonGeometricData.STROKE_DASHARRAY));
		data.addAttribute(NonGeometricData.STROKE_DASHOFFSET, getStyle(element, NonGeometricData.STROKE_DASHOFFSET));
		data.addAttribute(NonGeometricData.STROKE_MITERLIMIT, getStyle(element, NonGeometricData.STROKE_MITERLIMIT));
		data.addAttribute(NonGeometricData.STROKE_OPACITY, getStyle(element, NonGeometricData.STROKE_OPACITY));
		data.addAttribute(NonGeometricData.STROKE_WIDTH, getStyle(element, NonGeometricData.STROKE_WIDTH));
		
		return data;
	}
	
	/**
	 * Get the meta data store within an element either in the label or
	 * id atributes
	 * 
	 * @param element The element to be processed
	 * @return The meta data stored
	 */
	static String getMetaData(Element element) {
		String label = element.getAttributeNS(INKSCAPE, "label");
		if ((label != null) && (!label.equals(""))) {
			return label;
		}
		
		return element.getAttribute("id");
	}
	
	/**
	 * Get the style attribute setting for a given style information element (i.e. fill, stroke)
	 * 
	 * @param element The element to be processed
	 * @param styleName The name of the attribute to retrieve
	 * @return The style value
	 */
	static String getStyle(Element element, String styleName) {
		String value = element.getAttribute(styleName);
		
		if ((value != null) && (value.length() > 0)) {
			return value;
		}
		
		String style = element.getAttribute("style");
		return extractStyle(style, styleName);
	}
	
	/**
	 * Extract the style value from a Inkscape encoded string
	 * 
	 * @param style The style string to be decoded 
	 * @param attribute The style attribute to retrieve 
	 * @return The value for the given attribute
	 */
	static String extractStyle(String style, String attribute) {
		if (style == null) {
			return "";
		}
		
		StringTokenizer tokens = new StringTokenizer(style,";");
		
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken();
			String key = token.substring(0,token.indexOf(':'));
			if (key.equals(attribute)) {
				return token.substring(token.indexOf(':')+1);
			}
		}
		
		return "";
	}

	/**
	 * Get a transform defined in the XML
	 * 
	 * @param element The element from which the transform should be read
	 * @return The transform to be applied
	 */
	static Transform getTransform(Element element) {
		return getTransform(element, "transform");
	}
	
	/**
	 * Get a transform defined in the XML
	 * 
	 * @param element The element from which the transform should be read
	 * @param attribute The name of the attribute holding the transform
	 * @return The transform to be applied
	 */
	static Transform getTransform(Element element, String attribute) {
		String str = element.getAttribute(attribute);
		if (str == null) {
			return new Transform();
		}
		
		if (str.equals("")) {
			return new Transform();
		} else if (str.startsWith("translate")) {
			str = str.substring(0, str.length()-1);
			str = str.substring("translate(".length());
			StringTokenizer tokens = new StringTokenizer(str, ", ");
			float x = Float.parseFloat(tokens.nextToken());
			float y = Float.parseFloat(tokens.nextToken());
			
			return Transform.createTranslateTransform(x,y);
		} else if (str.startsWith("matrix")) {
			float[] pose = new float[6];
			str = str.substring(0, str.length()-1);
			str = str.substring("matrix(".length());
			StringTokenizer tokens = new StringTokenizer(str, ", ");
			float[] tr = new float[6];
			for (int j=0;j<tr.length;j++) {
				tr[j] = Float.parseFloat(tokens.nextToken());
			}
			
			pose[0] = tr[0];
			pose[1] = tr[2];
			pose[2] = tr[4];
			pose[3] = tr[1];
			pose[4] = tr[3];
			pose[5] = tr[5];
			
			return new Transform(pose);
		}
		
		return new Transform();
	}
	
	/**
	 * Get a floating point attribute that may appear in either the default or
	 * SODIPODI namespace
	 * 
	 * @param element The element from which the attribute should be read
	 * @param attr The attribute to be read
	 * @return The value from the given attribute
	 * @throws ParsingException Indicates the value in the attribute was not a float
	 */
	static float getFloatAttribute(Element element, String attr) throws ParsingException {
		String cx = element.getAttribute(attr);
		if ((cx == null) || (cx.equals(""))) {
			cx = element.getAttributeNS(SODIPODI, attr);
		}
		
		try {
			return Float.parseFloat(cx);
		} catch (NumberFormatException e) {
			throw new ParsingException(element, "Invalid value for: "+attr, e);
		}
	}
	
	/**
	 * Get the attribute value as a reference to another entity
	 * 
	 * @param value The value to treat as reference
	 * @return The reference part of the attribute value
	 */
	public static String getAsReference(String value) {
		if (value.length() < 2) {
			return "";
		}
		
		value = value.substring(1, value.length());
		
		return value;
	}
}
