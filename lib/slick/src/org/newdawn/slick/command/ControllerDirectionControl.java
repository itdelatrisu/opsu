package org.newdawn.slick.command;

/**
 * A control indicating that a particular direction must be pressed or released
 * on a controller to cause the command to fire
 *
 * @author kevin
 */
public class ControllerDirectionControl extends ControllerControl {
	/** The direction indicating we're waiting for the user to press left */
	public static final Direction LEFT = new Direction(LEFT_EVENT);
	/** The direction indicating we're waiting for the user to press up */
	public static final Direction UP = new Direction(UP_EVENT);
	/** The direction indicating we're waiting for the user to press down */
	public static final Direction DOWN = new Direction(DOWN_EVENT);
	/** The direction indicating we're waiting for the user to press right */
	public static final Direction RIGHT = new Direction(RIGHT_EVENT);
	
	/**
	 * Create a new input that indicates a direcitonal control must be pressed
	 * 
	 * @param controllerIndex The index of the controller to listen to
	 * @param dir The direction to wait for 
	 */
	public ControllerDirectionControl(int controllerIndex, Direction dir) {
		super(controllerIndex, dir.event, 0);
	}
	
	/**
	 * Enum pretender
	 * 
	 * @author kevin
	 */
	private static class Direction {
		/** The event to be fired for this direction */
		private int event;
		
		/**
		 * Create a new direction indicator/enum value
		 * 
		 * @param event The event to fire when this direction is used
		 */
		public Direction(int event) {
			this.event = event;
		}
	}
}
