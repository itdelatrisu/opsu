package org.newdawn.slick;

import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.newdawn.slick.util.ResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A sprite sheet based on an XML descriptor generated from the simple slick tool
 * 
 * @author kevin
 */
public class XMLPackedSheet {
	/** The full sheet image */
	private Image image;
	/** The sprites stored on the image */
	private HashMap sprites = new HashMap();
	
	/**
	 * Create a new XML packed sheet from the XML output by the slick tool
	 * 
	 * @param imageRef The reference to the image
	 * @param xmlRef The reference to the XML
	 * @throws SlickException Indicates a failure to parse the XML or read the image
	 */
	public XMLPackedSheet(String imageRef, String xmlRef) throws SlickException
	{
		image = new Image(imageRef, false, Image.FILTER_NEAREST);
	
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(ResourceLoader.getResourceAsStream(xmlRef));
			
			NodeList list = doc.getElementsByTagName("sprite");
			for (int i=0;i<list.getLength();i++) {
				Element element = (Element) list.item(i);
				
				String name = element.getAttribute("name");
				int x = Integer.parseInt(element.getAttribute("x"));
				int y = Integer.parseInt(element.getAttribute("y"));
				int width = Integer.parseInt(element.getAttribute("width"));
				int height = Integer.parseInt(element.getAttribute("height"));
				
				sprites.put(name, image.getSubImage(x,y,width,height));
			}
		} catch (Exception e) {
			throw new SlickException("Failed to parse sprite sheet XML", e);
		}
	}
	
	/**
	 * Get a sprite by it's given name
	 * 
	 * @param name The name of the sprite to retrieve
	 * @return The sprite from the sheet or null if the name isn't used in this sheet
	 */
	public Image getSprite(String name) {
		return (Image) sprites.get(name);
	}
}
