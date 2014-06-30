package org.newdawn.slick;

/**
 * Description of classes capable of responding to controller events
 * 
 * @author kevin
 */
public interface ControllerListener extends ControlledInputReciever {

	/**
	 * Notification that the left control has been pressed on 
	 * the controller.
	 * 
	 * @param controller The index of the controller on which the control
	 * was pressed.
	 */
	public abstract void controllerLeftPressed(int controller);

	/**
	 * Notification that the left control has been released on 
	 * the controller.
	 * 
	 * @param controller The index of the controller on which the control
	 * was released.
	 */
	public abstract void controllerLeftReleased(int controller);

	/**
	 * Notification that the right control has been pressed on 
	 * the controller.
	 * 
	 * @param controller The index of the controller on which the control
	 * was pressed.
	 */
	public abstract void controllerRightPressed(int controller);

	/**
	 * Notification that the right control has been released on 
	 * the controller.
	 * 
	 * @param controller The index of the controller on which the control
	 * was released.
	 */
	public abstract void controllerRightReleased(int controller);

	/**
	 * Notification that the up control has been pressed on 
	 * the controller.
	 * 
	 * @param controller The index of the controller on which the control
	 * was pressed.
	 */
	public abstract void controllerUpPressed(int controller);

	/**
	 * Notification that the up control has been released on 
	 * the controller.
	 * 
	 * @param controller The index of the controller on which the control
	 * was released.
	 */
	public abstract void controllerUpReleased(int controller);

	/**
	 * Notification that the down control has been pressed on 
	 * the controller.
	 * 
	 * @param controller The index of the controller on which the control
	 * was pressed.
	 */
	public abstract void controllerDownPressed(int controller);

	/**
	 * Notification that the down control has been released on 
	 * the controller.
	 * 
	 * @param controller The index of the controller on which the control
	 * was released.
	 */
	public abstract void controllerDownReleased(int controller);

	/**
	 * Notification that a button control has been pressed on 
	 * the controller.
	 * 
	 * @param controller The index of the controller on which the control
	 * was pressed.
	 * @param button The index of the button pressed (starting at 1)
	 */
	public abstract void controllerButtonPressed(int controller, int button);

	/**
	 * Notification that a button control has been released on 
	 * the controller.
	 * 
	 * @param controller The index of the controller on which the control
	 * was released.
	 * @param button The index of the button released (starting at 1)
	 */
	public abstract void controllerButtonReleased(int controller, int button);

}