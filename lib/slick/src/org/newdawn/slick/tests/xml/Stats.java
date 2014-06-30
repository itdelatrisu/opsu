package org.newdawn.slick.tests.xml;

/**
 * A test example of some object data that can be configured via XML
 * 
 * @author kevin
 */
public class Stats {
	/** hit points */
	private int hp;
	/** magic points */
	private int mp;
	/** age in years */
	private float age;
	/** experience points */
	private int exp;

	/**
	 * Dump this object to sysout
	 * 
	 * @param prefix The prefix to apply to all lines
	 */
	public void dump(String prefix) {
		System.out.println(prefix+"Stats "+hp+","+mp+","+age+","+exp);
	}
}
