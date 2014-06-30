package org.newdawn.slick.command;

/**
 * A control indicating that a gamepad/joystick button must be pressed
 * or released to invoke an command.
 *
 * @author kevin
 */
public class ControllerButtonControl extends ControllerControl {

	/**
	 * Create a new control based on a controller input
	 * 
	 * @param controllerIndex The index of the controller to listen to
	 * @param button The index of the button that causes the command
	 */
	public ControllerButtonControl(int controllerIndex, int button) {
		super(controllerIndex, BUTTON_EVENT, button);
	}
}
