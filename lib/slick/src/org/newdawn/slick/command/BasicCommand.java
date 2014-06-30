package org.newdawn.slick.command;

/**
 * A simple named command
 * 
 * @author kevin
 */
public class BasicCommand implements Command {
	/** The name of the command */
	private String name;
	
	/** 
	 * Create a new basic command
	 * 
	 * @param name The name to give this command
	 */
	public BasicCommand(String name) {
		this.name = name;
	}
	
	/**
	 * Get the name given for this basic command
	 * 
	 * @return The name given for this basic command
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return name.hashCode();
	}
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (other instanceof BasicCommand) {
			return ((BasicCommand) other).name.equals(name);
		}
		
		return false;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[Command="+name+"]";
	}
}
