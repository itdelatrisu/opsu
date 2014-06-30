package org.newdawn.slick.tests.xml;

/**
 * A test example of some object data that can be configured via XML
 * 
 * @author kevin
 */
public class Entity {
	/** X position for the entity */
	private float x;
	/** Y position for the entity */
	private float y;
	/** items held */
	private Inventory invent;
	/** Entity statistics */
	private Stats stats;
	
	/**
	 * Called by XML parser to add a configured inventory to the entity 
	 * 
	 * @param inventory The inventory to be added 
	 */
	private void add(Inventory inventory) {
		this.invent = inventory;
	}

	/**
	 * Called by XML parser to add a configured statistics object to the entity 
	 * 
	 * @param stats The statistics to be added 
	 */
	private void add(Stats stats) {
		this.stats = stats;
	}
	
	/**
	 * Dump this object to sysout
	 * 
	 * @param prefix The prefix to apply to all lines
	 */
	public void dump(String prefix) {
		System.out.println(prefix+"Entity "+x+","+y);
		invent.dump(prefix+"\t");
		stats.dump(prefix+"\t");
	}
}
