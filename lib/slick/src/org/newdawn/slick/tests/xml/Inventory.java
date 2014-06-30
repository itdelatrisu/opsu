package org.newdawn.slick.tests.xml;

import java.util.ArrayList;

/**
 * A test example of some object data that can be configured via XML
 * 
 * @author kevin
 */
public class Inventory {
	/** The items held in the inventory */
	private ArrayList items = new ArrayList();

	/**
	 * Called by XML parser to add a configured item to the entity 
	 * 
	 * @param item The item to be added 
	 */
	private void add(Item item) {
		items.add(item);
	}

	/**
	 * Dump this object to sysout
	 * 
	 * @param prefix The prefix to apply to all lines
	 */
	public void dump(String prefix) {
		System.out.println(prefix+"Inventory");
		for (int i=0;i<items.size();i++) {
			((Item) items.get(i)).dump(prefix+"\t");
		}
	}
}
