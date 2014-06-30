package org.newdawn.slick.util.xml;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.ResourceLoader;
import org.w3c.dom.Document;

/**
 * A simple utility wrapper around the Java DOM implementation to hopefully
 * make XML parsing that bit easier without requiring YAL. 
 * 
 * @author kevin
 */
public class XMLParser {
	/** The factory used to to create document builders that parse XML into the DOM */
	private static DocumentBuilderFactory factory;
	
	/**
	 * Create a new parser
	 */
	public XMLParser() {
	}
	
	/**
	 * Parse the XML document located by the slick resource loader using the
	 * reference given.
	 * 
	 * @param ref The reference to the XML document
	 * @return The root element of the newly parse document
	 * @throws SlickException Indicates a failure to parse the XML, most likely the 
	 * XML is malformed in some way.
	 */
	public XMLElement parse(String ref) throws SlickException {
		return parse(ref, ResourceLoader.getResourceAsStream(ref));
	}
	
	/**
	 * Parse the XML document that can be read from the given input stream
	 * 
	 * @param name The name of the document
	 * @param in The input stream from which the document can be read
	 * @return The root element of the newly parse document
	 * @throws SlickXMLException Indicates a failure to parse the XML, most likely the 
	 * XML is malformed in some way.
	 */
	public XMLElement parse(String name, InputStream in) throws SlickXMLException {
		try {
			if (factory == null) {
				factory = DocumentBuilderFactory.newInstance();
			}
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(in);
			
			return new XMLElement(doc.getDocumentElement());
		} catch (Exception e) {
			throw new SlickXMLException("Failed to parse document: "+name, e);
		}
	}
}
