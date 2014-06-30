package org.newdawn.slick.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.newdawn.slick.Input;
import org.newdawn.slick.util.InputAdapter;

/**
 * The central provider that maps real device input into abstract commands
 * defined by the developer. Registering a control against an command with this
 * class will cause the provider to produce an event for the command when the
 * input is pressed and released.
 * 
 * @author joverton
 */
public class InputProvider {
	/** The commands that have been defined */
	private HashMap commands;

	/** The list of listeners that may be listening */
	private ArrayList listeners = new ArrayList();

	/** The input context we're responding to */
	private Input input;

	/** The command input states */
	private HashMap commandState = new HashMap();

	/** True if this provider is actively sending events */
	private boolean active = true;

	/**
	 * Create a new input proider which will provide abstract input descriptions
	 * based on the input from the supplied context.
	 * 
	 * @param input
	 *            The input from which this provider will receive events
	 */
	public InputProvider(Input input) {
		this.input = input;

		input.addListener(new InputListenerImpl());
		commands = new HashMap();
	}

	/**
	 * Get the list of commands that have been registered with the provider,
	 * i.e. the commands that can be issued to the listeners
	 * 
	 * @return The list of commands (@see Command) that can be issued from this
	 *         provider
	 */
	public List getUniqueCommands() {
		List uniqueCommands = new ArrayList();

		for (Iterator it = commands.values().iterator(); it.hasNext();) {
			Command command = (Command) it.next();

			if (!uniqueCommands.contains(command)) {
				uniqueCommands.add(command);
			}
		}

		return uniqueCommands;
	}

	/**
	 * Get a list of the registered controls (@see Control) that can cause a
	 * particular command to be invoked
	 * 
	 * @param command
	 *            The command to be invoked
	 * @return The list of controls that can cause the command (@see Control)
	 */
	public List getControlsFor(Command command) {
		List controlsForCommand = new ArrayList();

		for (Iterator it = commands.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			Control key = (Control) entry.getKey();
			Command value = (Command) entry.getValue();

			if (value == command) {
				controlsForCommand.add(key);
			}
		}
		return controlsForCommand;
	}

	/**
	 * Indicate whether this provider should be sending events
	 * 
	 * @param active
	 *            True if this provider should be sending events
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * Check if this provider should be sending events
	 * 
	 * @return True if this provider should be sending events
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Add a listener to the provider. This listener will be notified of
	 * commands detected from the input.
	 * 
	 * @param listener
	 *            The listener to be added
	 */
	public void addListener(InputProviderListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a listener from this provider. The listener will no longer be
	 * provided with notification of commands performe.
	 * 
	 * @param listener
	 *            The listener to be removed
	 */
	public void removeListener(InputProviderListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Bind an command to a control.
	 * 
	 * @param command
	 *            The command to bind to
	 * @param control
	 *            The control that is pressed/released to represent the command
	 */
	public void bindCommand(Control control, Command command) {
		commands.put(control, command);

		if (commandState.get(command) == null) {
			commandState.put(command, new CommandState());
		}
	}

	/**
	 * Clear all the controls that have been configured for a given command
	 * 
	 * @param command The command whose controls should be unbound
	 */
	public void clearCommand(Command command) {
		List controls = getControlsFor(command);
		
		for (int i=0;i<controls.size();i++) {
	    	unbindCommand((Control) controls.get(i));
	    }
	}
	
	/**
	 * Unbinds the command associated with this control
	 * 
	 * @param control
	 *            The control to remove
	 */
	public void unbindCommand(Control control) {
		Command command = (Command) commands.remove(control);
		if (command != null) {
			if (!commands.keySet().contains(command)) {
				commandState.remove(command);
			}
		}
	}

	/**
	 * Get the recorded state for a given command
	 * 
	 * @param command
	 *            The command to get the state for
	 * @return The given command state
	 */
	private CommandState getState(Command command) {
		return (CommandState) commandState.get(command);
	}

	/**
	 * Check if the last control event we recieved related to the given command
	 * indicated that a control was down
	 * 
	 * @param command
	 *            The command to check
	 * @return True if the last event indicated a button down
	 */
	public boolean isCommandControlDown(Command command) {
		return getState(command).isDown();
	}

	/**
	 * Check if one of the controls related to the command specified has been
	 * pressed since we last called this method
	 * 
	 * @param command
	 *            The command to check
	 * @return True if one of the controls has been pressed
	 */
	public boolean isCommandControlPressed(Command command) {
		return getState(command).isPressed();
	}

	/**
	 * Fire notification to any interested listeners that a control has been
	 * pressed indication an particular command
	 * 
	 * @param command
	 *            The command that has been pressed
	 */
	protected void firePressed(Command command) {
		getState(command).down = true;
		getState(command).pressed = true;

		if (!isActive()) {
			return;
		}

		for (int i = 0; i < listeners.size(); i++) {
			((InputProviderListener) listeners.get(i)).controlPressed(command);
		}
	}

	/**
	 * Fire notification to any interested listeners that a control has been
	 * released indication an particular command should be stopped
	 * 
	 * @param command
	 *            The command that has been pressed
	 */
	protected void fireReleased(Command command) {
		getState(command).down = false;

		if (!isActive()) {
			return;
		}

		for (int i = 0; i < listeners.size(); i++) {
			((InputProviderListener) listeners.get(i)).controlReleased(command);
		}
	}

	/**
	 * A token representing the state of all the controls causing an command to
	 * be invoked
	 * 
	 * @author kevin
	 */
	private class CommandState {
		/** True if one of the controls for this command is down */
		private boolean down;

		/** True if one of the controls for this command is pressed */
		private boolean pressed;

		/**
		 * Check if a control for the command has been pressed since last call.
		 * 
		 * @return True if the command has been pressed
		 */
		public boolean isPressed() {
			if (pressed) {
				pressed = false;
				return true;
			}

			return false;
		}

		/**
		 * Check if the last event we had indicated the control was pressed
		 * 
		 * @return True if the control was pressed
		 */
		public boolean isDown() {
			return down;
		}
	}

	/**
	 * A simple listener to respond to input and look up any required commands
	 * 
	 * @author kevin
	 */
	private class InputListenerImpl extends InputAdapter {
		/**
		 * @see org.newdawn.slick.util.InputAdapter#isAcceptingInput()
		 */
		public boolean isAcceptingInput() {
			return true;
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#keyPressed(int, char)
		 */
		public void keyPressed(int key, char c) {
			Command command = (Command) commands.get(new KeyControl(key));
			if (command != null) {
				firePressed(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#keyReleased(int, char)
		 */
		public void keyReleased(int key, char c) {
			Command command = (Command) commands.get(new KeyControl(key));
			if (command != null) {
				fireReleased(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#mousePressed(int, int, int)
		 */
		public void mousePressed(int button, int x, int y) {
			Command command = (Command) commands.get(new MouseButtonControl(
					button));
			if (command != null) {
				firePressed(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#mouseReleased(int, int, int)
		 */
		public void mouseReleased(int button, int x, int y) {
			Command command = (Command) commands.get(new MouseButtonControl(
					button));
			if (command != null) {
				fireReleased(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#controllerLeftPressed(int)
		 */
		public void controllerLeftPressed(int controller) {
			Command command = (Command) commands
					.get(new ControllerDirectionControl(controller,
							ControllerDirectionControl.LEFT));
			if (command != null) {
				firePressed(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#controllerLeftReleased(int)
		 */
		public void controllerLeftReleased(int controller) {
			Command command = (Command) commands
					.get(new ControllerDirectionControl(controller,
							ControllerDirectionControl.LEFT));
			if (command != null) {
				fireReleased(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#controllerRightPressed(int)
		 */
		public void controllerRightPressed(int controller) {
			Command command = (Command) commands
					.get(new ControllerDirectionControl(controller,
							ControllerDirectionControl.RIGHT));
			if (command != null) {
				firePressed(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#controllerRightReleased(int)
		 */
		public void controllerRightReleased(int controller) {
			Command command = (Command) commands
					.get(new ControllerDirectionControl(controller,
							ControllerDirectionControl.RIGHT));
			if (command != null) {
				fireReleased(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#controllerUpPressed(int)
		 */
		public void controllerUpPressed(int controller) {
			Command command = (Command) commands
					.get(new ControllerDirectionControl(controller,
							ControllerDirectionControl.UP));
			if (command != null)
				firePressed(command);
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#controllerUpReleased(int)
		 */
		public void controllerUpReleased(int controller) {
			Command command = (Command) commands
					.get(new ControllerDirectionControl(controller,
							ControllerDirectionControl.UP));
			if (command != null) {
				fireReleased(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#controllerDownPressed(int)
		 */
		public void controllerDownPressed(int controller) {
			Command command = (Command) commands
					.get(new ControllerDirectionControl(controller,
							ControllerDirectionControl.DOWN));
			if (command != null) {
				firePressed(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#controllerDownReleased(int)
		 */
		public void controllerDownReleased(int controller) {
			Command command = (Command) commands
					.get(new ControllerDirectionControl(controller,
							ControllerDirectionControl.DOWN));
			if (command != null) {
				fireReleased(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#controllerButtonPressed(int,
		 *      int)
		 */
		public void controllerButtonPressed(int controller, int button) {
			Command command = (Command) commands
					.get(new ControllerButtonControl(controller, button));
			if (command != null) {
				firePressed(command);
			}
		}

		/**
		 * @see org.newdawn.slick.util.InputAdapter#controllerButtonReleased(int,
		 *      int)
		 */
		public void controllerButtonReleased(int controller, int button) {
			Command command = (Command) commands
					.get(new ControllerButtonControl(controller, button));
			if (command != null) {
				fireReleased(command);
			}
		}
	};
}
