package org.newdawn.slick.tests.xml;


/**
 * A test example of some object data that can be configured via XML
 * 
 * @author kevin
 */
public class Item {
	/** The name injected by the XML parser */
	protected String name;
	/** The condition value injected by the XML parser */
	protected int condition; 

	/**
	 * Dump this object to sysout
	 * 
	 * @param prefix The prefix to apply to all lines
	 */
	public void dump(String prefix) {
		System.out.println(prefix+"Item "+name+","+condition);
	}
}
