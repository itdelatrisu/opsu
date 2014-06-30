package org.newdawn.slick;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.newdawn.slick.util.Log;

/**
 * A wrapped for all keyboard, mouse and controller input
 *
 * @author kevin
 */
public class Input {
	/** The controller index to pass to check all controllers */
	public static final int ANY_CONTROLLER = -1;
	
	/** The maximum number of buttons on controllers */
	private static final int MAX_BUTTONS = 100;
	
	/** */
	public static final int KEY_ESCAPE          = 0x01;
	/** */
	public static final int KEY_1               = 0x02;
	/** */
	public static final int KEY_2               = 0x03;
	/** */
	public static final int KEY_3               = 0x04;
	/** */
	public static final int KEY_4               = 0x05;
	/** */
	public static final int KEY_5               = 0x06;
	/** */
	public static final int KEY_6               = 0x07;
	/** */
	public static final int KEY_7               = 0x08;
	/** */
	public static final int KEY_8               = 0x09;
	/** */
	public static final int KEY_9               = 0x0A;
	/** */
	public static final int KEY_0               = 0x0B;
	/** */
	public static final int KEY_MINUS           = 0x0C; /* - on main keyboard */
	/** */
	public static final int KEY_EQUALS          = 0x0D;
	/** */
	public static final int KEY_BACK            = 0x0E; /* backspace */
	/** */
	public static final int KEY_TAB             = 0x0F;
	/** */
	public static final int KEY_Q               = 0x10;
	/** */
	public static final int KEY_W               = 0x11;
	/** */
	public static final int KEY_E               = 0x12;
	/** */
	public static final int KEY_R               = 0x13;
	/** */
	public static final int KEY_T               = 0x14;
	/** */
	public static final int KEY_Y               = 0x15;
	/** */
	public static final int KEY_U               = 0x16;
	/** */
	public static final int KEY_I               = 0x17;
	/** */
	public static final int KEY_O               = 0x18;
	/** */
	public static final int KEY_P               = 0x19;
	/** */
	public static final int KEY_LBRACKET        = 0x1A;
	/** */
	public static final int KEY_RBRACKET        = 0x1B;
	/** */
	public static final int KEY_RETURN          = 0x1C; /* Enter on main keyboard */
	/** */
	public static final int KEY_ENTER           = 0x1C; /* Enter on main keyboard */
	/** */
	public static final int KEY_LCONTROL        = 0x1D;
	/** */
	public static final int KEY_A               = 0x1E;
	/** */
	public static final int KEY_S               = 0x1F;
	/** */
	public static final int KEY_D               = 0x20;
	/** */
	public static final int KEY_F               = 0x21;
	/** */
	public static final int KEY_G               = 0x22;
	/** */
	public static final int KEY_H               = 0x23;
	/** */
	public static final int KEY_J               = 0x24;
	/** */
	public static final int KEY_K               = 0x25;
	/** */
	public static final int KEY_L               = 0x26;
	/** */
	public static final int KEY_SEMICOLON       = 0x27;
	/** */
	public static final int KEY_APOSTROPHE      = 0x28;
	/** */
	public static final int KEY_GRAVE           = 0x29; /* accent grave */
	/** */
	public static final int KEY_LSHIFT          = 0x2A;
	/** */
	public static final int KEY_BACKSLASH       = 0x2B;
	/** */
	public static final int KEY_Z               = 0x2C;
	/** */
	public static final int KEY_X               = 0x2D;
	/** */
	public static final int KEY_C               = 0x2E;
	/** */
	public static final int KEY_V               = 0x2F;
	/** */
	public static final int KEY_B               = 0x30;
	/** */
	public static final int KEY_N               = 0x31;
	/** */
	public static final int KEY_M               = 0x32;
	/** */
	public static final int KEY_COMMA           = 0x33;
	/** */
	public static final int KEY_PERIOD          = 0x34; /* . on main keyboard */
	/** */
	public static final int KEY_SLASH           = 0x35; /* / on main keyboard */
	/** */
	public static final int KEY_RSHIFT          = 0x36;
	/** */
	public static final int KEY_MULTIPLY        = 0x37; /* * on numeric keypad */
	/** */
	public static final int KEY_LMENU           = 0x38; /* left Alt */
	/** */
	public static final int KEY_SPACE           = 0x39;
	/** */
	public static final int KEY_CAPITAL         = 0x3A;
	/** */
	public static final int KEY_F1              = 0x3B;
	/** */
	public static final int KEY_F2              = 0x3C;
	/** */
	public static final int KEY_F3              = 0x3D;
	/** */
	public static final int KEY_F4              = 0x3E;
	/** */
	public static final int KEY_F5              = 0x3F;
	/** */
	public static final int KEY_F6              = 0x40;
	/** */
	public static final int KEY_F7              = 0x41;
	/** */
	public static final int KEY_F8              = 0x42;
	/** */
	public static final int KEY_F9              = 0x43;
	/** */
	public static final int KEY_F10             = 0x44;
	/** */
	public static final int KEY_NUMLOCK         = 0x45;
	/** */
	public static final int KEY_SCROLL          = 0x46; /* Scroll Lock */
	/** */
	public static final int KEY_NUMPAD7         = 0x47;
	/** */
	public static final int KEY_NUMPAD8         = 0x48;
	/** */
	public static final int KEY_NUMPAD9         = 0x49;
	/** */
	public static final int KEY_SUBTRACT        = 0x4A; /* - on numeric keypad */
	/** */
	public static final int KEY_NUMPAD4         = 0x4B;
	/** */
	public static final int KEY_NUMPAD5         = 0x4C;
	/** */
	public static final int KEY_NUMPAD6         = 0x4D;
	/** */
	public static final int KEY_ADD             = 0x4E; /* + on numeric keypad */
	/** */
	public static final int KEY_NUMPAD1         = 0x4F;
	/** */
	public static final int KEY_NUMPAD2         = 0x50;
	/** */
	public static final int KEY_NUMPAD3         = 0x51;
	/** */
	public static final int KEY_NUMPAD0         = 0x52;
	/** */
	public static final int KEY_DECIMAL         = 0x53; /* . on numeric keypad */
	/** */
	public static final int KEY_F11             = 0x57;
	/** */
	public static final int KEY_F12             = 0x58;
	/** */
	public static final int KEY_F13             = 0x64; /*                     (NEC PC98) */
	/** */
	public static final int KEY_F14             = 0x65; /*                     (NEC PC98) */
	/** */
	public static final int KEY_F15             = 0x66; /*                     (NEC PC98) */
	/** */
	public static final int KEY_KANA            = 0x70; /* (Japanese keyboard)            */
	/** */
	public static final int KEY_CONVERT         = 0x79; /* (Japanese keyboard)            */
	/** */
	public static final int KEY_NOCONVERT       = 0x7B; /* (Japanese keyboard)            */
	/** */
	public static final int KEY_YEN             = 0x7D; /* (Japanese keyboard)            */
	/** */
	public static final int KEY_NUMPADEQUALS    = 0x8D; /* = on numeric keypad (NEC PC98) */
	/** */
	public static final int KEY_CIRCUMFLEX      = 0x90; /* (Japanese keyboard)            */
	/** */
	public static final int KEY_AT              = 0x91; /*                     (NEC PC98) */
	/** */
	public static final int KEY_COLON           = 0x92; /*                     (NEC PC98) */
	/** */
	public static final int KEY_UNDERLINE       = 0x93; /*                     (NEC PC98) */
	/** */
	public static final int KEY_KANJI           = 0x94; /* (Japanese keyboard)            */
	/** */
	public static final int KEY_STOP            = 0x95; /*                     (NEC PC98) */
	/** */
	public static final int KEY_AX              = 0x96; /*                     (Japan AX) */
	/** */
	public static final int KEY_UNLABELED       = 0x97; /*                        (J3100) */
	/** */
	public static final int KEY_NUMPADENTER     = 0x9C; /* Enter on numeric keypad */
	/** */
	public static final int KEY_RCONTROL        = 0x9D;
	/** */
	public static final int KEY_NUMPADCOMMA     = 0xB3; /* , on numeric keypad (NEC PC98) */
	/** */
	public static final int KEY_DIVIDE          = 0xB5; /* / on numeric keypad */
	/** */
	public static final int KEY_SYSRQ           = 0xB7;
	/** */
	public static final int KEY_RMENU           = 0xB8; /* right Alt */
	/** */
	public static final int KEY_PAUSE           = 0xC5; /* Pause */
	/** */
	public static final int KEY_HOME            = 0xC7; /* Home on arrow keypad */
	/** */
	public static final int KEY_UP              = 0xC8; /* UpArrow on arrow keypad */
	/** */
	public static final int KEY_PRIOR           = 0xC9; /* PgUp on arrow keypad */
	/** */
	public static final int KEY_LEFT            = 0xCB; /* LeftArrow on arrow keypad */
	/** */
	public static final int KEY_RIGHT           = 0xCD; /* RightArrow on arrow keypad */
	/** */
	public static final int KEY_END             = 0xCF; /* End on arrow keypad */
	/** */
	public static final int KEY_DOWN            = 0xD0; /* DownArrow on arrow keypad */
	/** */
	public static final int KEY_NEXT            = 0xD1; /* PgDn on arrow keypad */
	/** */
	public static final int KEY_INSERT          = 0xD2; /* Insert on arrow keypad */
	/** */
	public static final int KEY_DELETE          = 0xD3; /* Delete on arrow keypad */
	/** */
	public static final int KEY_LWIN            = 0xDB; /* Left Windows key */
	/** */
	public static final int KEY_RWIN            = 0xDC; /* Right Windows key */
	/** */
	public static final int KEY_APPS            = 0xDD; /* AppMenu key */
	/** */
	public static final int KEY_POWER           = 0xDE;
	/** */
	public static final int KEY_SLEEP           = 0xDF;
	
	/** A helper for left ALT */
	public static final int KEY_LALT = KEY_LMENU;
	/** A helper for right ALT */
	public static final int KEY_RALT = KEY_RMENU;
	
	/** Control index */
	private static final int LEFT = 0;
	/** Control index */
	private static final int RIGHT = 1;
	/** Control index */
	private static final int UP = 2;
	/** Control index */
	private static final int DOWN = 3;
	/** Control index */
	private static final int BUTTON1 = 4;
	/** Control index */
	private static final int BUTTON2 = 5;
	/** Control index */
	private static final int BUTTON3 = 6;
	/** Control index */
	private static final int BUTTON4 = 7;
	/** Control index */
	private static final int BUTTON5 = 8;
	/** Control index */
	private static final int BUTTON6 = 9;
	/** Control index */
	private static final int BUTTON7 = 10;
	/** Control index */
	private static final int BUTTON8 = 11;
	/** Control index */
	private static final int BUTTON9 = 12;
	/** Control index */
	private static final int BUTTON10 = 13;
	
	/** The left mouse button indicator */
	public static final int MOUSE_LEFT_BUTTON = 0;
	/** The right mouse button indicator */
	public static final int MOUSE_RIGHT_BUTTON = 1;
	/** The middle mouse button indicator */
	public static final int MOUSE_MIDDLE_BUTTON = 2;
	
	/** True if the controllers system has been initialised */
	private static boolean controllersInited = false;
	/** The list of controllers */
	private static ArrayList controllers = new ArrayList();

	/** The last recorded mouse x position */
	private int lastMouseX;
	/** The last recorded mouse y position */
	private int lastMouseY;
	/** THe state of the mouse buttons */
	protected boolean[] mousePressed = new boolean[10];
	/** THe state of the controller buttons */
	private boolean[][] controllerPressed = new boolean[100][MAX_BUTTONS];
	
	/** The character values representing the pressed keys */
	protected char[] keys = new char[1024];
	/** True if the key has been pressed since last queries */
	protected boolean[] pressed = new boolean[1024];
	/** The time since the next key repeat to be fired for the key */
	protected long[] nextRepeat = new long[1024];
	
	/** The control states from the controllers */
	private boolean[][] controls = new boolean[10][MAX_BUTTONS+10];
	/** True if the event has been consumed */
	protected boolean consumed = false;
	/** A list of listeners to be notified of input events */
	protected HashSet allListeners = new HashSet();
	/** The listeners to notify of key events */
	protected ArrayList keyListeners = new ArrayList();
	/** The listener to add */
	protected ArrayList keyListenersToAdd = new ArrayList();
	/** The listeners to notify of mouse events */
	protected ArrayList mouseListeners = new ArrayList();
	/** The listener to add */
	protected ArrayList mouseListenersToAdd = new ArrayList();
	/** The listener to nofiy of controller events */
	protected ArrayList controllerListeners = new ArrayList();
	/** The current value of the wheel */
	private int wheel;
	/** The height of the display */
	private int height;
	
	/** True if the display is active */
	private boolean displayActive = true;
	
	/** True if key repeat is enabled */
	private boolean keyRepeat;
	/** The initial delay for key repeat starts */
	private int keyRepeatInitial;
	/** The interval of key repeat */
	private int keyRepeatInterval;
	
	/** True if the input is currently paused */
	private boolean paused;
	/** The scale to apply to screen coordinates */
	private float scaleX = 1;
	/** The scale to apply to screen coordinates */
	private float scaleY = 1;
	/** The offset to apply to screen coordinates */
	private float xoffset = 0;
	/** The offset to apply to screen coordinates */
	private float yoffset = 0;
	
	/** The delay before determining a single or double click */
	private int doubleClickDelay = 250;
	/** The timer running out for a single click */
	private long doubleClickTimeout = 0;
	
	/** The clicked x position */
	private int clickX;
	/** The clicked y position */
	private int clickY;
	/** The clicked button */
	private int clickButton;

	/** The x position location the mouse was pressed */
	private int pressedX = -1;
	
	/** The x position location the mouse was pressed */
	private int pressedY = -1;
	
	/** The pixel distance the mouse can move to accept a mouse click */
	private int mouseClickTolerance = 5;

	/**
	 * Disables support for controllers. This means the jinput JAR and native libs 
	 * are not required.
	 */
	public static void disableControllers() {
	   controllersInited = true;
	}
	
	/**
	 * Create a new input with the height of the screen
	 * 
	 * @param height The height of the screen
	 */
	public Input(int height) {
		init(height);
	}
	
	/**
	 * Set the double click interval, the time between the first
	 * and second clicks that should be interpreted as a 
	 * double click.
	 * 
	 * @param delay The delay between clicks
	 */
	public void setDoubleClickInterval(int delay) {
		doubleClickDelay = delay;
	}

	/**
	 * Set the pixel distance the mouse can move to accept a mouse click. 
	 * Default is 5.
	 * 
	 * @param mouseClickTolerance The number of pixels.
	 */
	public void setMouseClickTolerance (int mouseClickTolerance) {
		this.mouseClickTolerance = mouseClickTolerance;
	}

	/**
	 * Set the scaling to apply to screen coordinates
	 * 
	 * @param scaleX The scaling to apply to the horizontal axis
	 * @param scaleY The scaling to apply to the vertical axis
	 */
	public void setScale(float scaleX, float scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}
	
	/**
	 * Set the offset to apply to the screen coodinates
	 * 
	 * @param xoffset The offset on the x-axis
	 * @param yoffset The offset on the y-axis
	 */
	public void setOffset(float xoffset, float yoffset) {
		this.xoffset = xoffset;
		this.yoffset = yoffset;
	}
	
	/**
	 * Reset the transformation being applied to the input to the default
	 */
	public void resetInputTransform() {
	    setOffset(0, 0);
	    setScale(1, 1);
	}
	
	/**
	 * Add a listener to be notified of input events
	 * 
	 * @param listener The listener to be notified
	 */
	public void addListener(InputListener listener) {
		addKeyListener(listener);
		addMouseListener(listener);
		addControllerListener(listener);
	}

	/**
	 * Add a key listener to be notified of key input events
	 * 
	 * @param listener The listener to be notified
	 */
	public void addKeyListener(KeyListener listener) {
		keyListenersToAdd.add(listener);
	}
	
	/**
	 * Add a key listener to be notified of key input events
	 * 
	 * @param listener The listener to be notified
	 */
	private void addKeyListenerImpl(KeyListener listener) {
		if (keyListeners.contains(listener)) {
			return;
		}
		keyListeners.add(listener);
		allListeners.add(listener);
	}

	/**
	 * Add a mouse listener to be notified of mouse input events
	 * 
	 * @param listener The listener to be notified
	 */
	public void addMouseListener(MouseListener listener) {
		mouseListenersToAdd.add(listener);
	}
	
	/**
	 * Add a mouse listener to be notified of mouse input events
	 * 
	 * @param listener The listener to be notified
	 */
	private void addMouseListenerImpl(MouseListener listener) {
		if (mouseListeners.contains(listener)) {
			return;
		}
		mouseListeners.add(listener);
		allListeners.add(listener);
	}
	
	/**
	 * Add a controller listener to be notified of controller input events
	 * 
	 * @param listener The listener to be notified
	 */
	public void addControllerListener(ControllerListener listener) {
		if (controllerListeners.contains(listener)) {
			return;
		}
		controllerListeners.add(listener);
		allListeners.add(listener);
	}
	
	/**
	 * Remove all the listeners from this input
	 */
	public void removeAllListeners() {
		removeAllKeyListeners();
		removeAllMouseListeners();
		removeAllControllerListeners();
	}

	/**
	 * Remove all the key listeners from this input
	 */
	public void removeAllKeyListeners() {
		allListeners.removeAll(keyListeners);
		keyListeners.clear();
	}

	/**
	 * Remove all the mouse listeners from this input
	 */
	public void removeAllMouseListeners() {
		allListeners.removeAll(mouseListeners);
		mouseListeners.clear();
	}

	/**
	 * Remove all the controller listeners from this input
	 */
	public void removeAllControllerListeners() {
		allListeners.removeAll(controllerListeners);
		controllerListeners.clear();
	}
	
	/**
	 * Add a listener to be notified of input events. This listener
	 * will get events before others that are currently registered
	 * 
	 * @param listener The listener to be notified
	 */
	public void addPrimaryListener(InputListener listener) {
		removeListener(listener);
		
		keyListeners.add(0, listener);
		mouseListeners.add(0, listener);
		controllerListeners.add(0, listener);
		
		allListeners.add(listener);
	}
	
	/**
	 * Remove a listener that will no longer be notified
	 * 
	 * @param listener The listen to be removed
	 */
	public void removeListener(InputListener listener) {
		removeKeyListener(listener);
		removeMouseListener(listener);
		removeControllerListener(listener);
	}

	/**
	 * Remove a key listener that will no longer be notified
	 * 
	 * @param listener The listen to be removed
	 */
	public void removeKeyListener(KeyListener listener) {
		keyListeners.remove(listener);
		
		if (!mouseListeners.contains(listener) && !controllerListeners.contains(listener)) {
			allListeners.remove(listener);
		}
	}

	/**
	 * Remove a controller listener that will no longer be notified
	 * 
	 * @param listener The listen to be removed
	 */
	public void removeControllerListener(ControllerListener listener) {
		controllerListeners.remove(listener);
		
		if (!mouseListeners.contains(listener) && !keyListeners.contains(listener)) {
			allListeners.remove(listener);
		}
	}

	/**
	 * Remove a mouse listener that will no longer be notified
	 * 
	 * @param listener The listen to be removed
	 */
	public void removeMouseListener(MouseListener listener) {
		mouseListeners.remove(listener);
		
		if (!controllerListeners.contains(listener) && !keyListeners.contains(listener)) {
			allListeners.remove(listener);
		}
	}
	
	/**
	 * Initialise the input system
	 * 
	 * @param height The height of the window
	 */
	void init(int height) {
		this.height = height;
		lastMouseX = getMouseX();
		lastMouseY = getMouseY();
	}
	
	/**
	 * Get the character representation of the key identified by the specified code
	 * 
	 * @param code The key code of the key to retrieve the name of
	 * @return The name or character representation of the key requested
	 */
	public static String getKeyName(int code) {
		return Keyboard.getKeyName(code);
	}
	
	/**
	 * Check if a particular key has been pressed since this method 
	 * was last called for the specified key
	 * 
	 * @param code The key code of the key to check
	 * @return True if the key has been pressed
	 */
	public boolean isKeyPressed(int code) {
		if (pressed[code]) {
			pressed[code] = false;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check if a mouse button has been pressed since last call
	 * 
	 * @param button The button to check
	 * @return True if the button has been pressed since last call
	 */
	public boolean isMousePressed(int button) {
		if (mousePressed[button]) {
			mousePressed[button] = false;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check if a controller button has been pressed since last 
	 * time
	 * 
	 * @param button The button to check for (note that this includes directional controls first)
	 * @return True if the button has been pressed since last time
	 */
	public boolean isControlPressed(int button) {
		return isControlPressed(button, 0);
	}

	/**
	 * Check if a controller button has been pressed since last 
	 * time
	 * 
	 * @param controller The index of the controller to check
	 * @param button The button to check for (note that this includes directional controls first)
	 * @return True if the button has been pressed since last time
	 */
	public boolean isControlPressed(int button, int controller) {
		if (controllerPressed[controller][button]) {
			controllerPressed[controller][button] = false;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Clear the state for isControlPressed method. This will reset all
	 * controls to not pressed
	 */
	public void clearControlPressedRecord() {
		for (int i=0;i<controllers.size();i++) {
			Arrays.fill(controllerPressed[i], false);
		}
	}
	
	/**
	 * Clear the state for the <code>isKeyPressed</code> method. This will
	 * resort in all keys returning that they haven't been pressed, until
	 * they are pressed again
	 */
	public void clearKeyPressedRecord() {
		Arrays.fill(pressed, false);
	}

	/**
	 * Clear the state for the <code>isMousePressed</code> method. This will
	 * resort in all mouse buttons returning that they haven't been pressed, until
	 * they are pressed again
	 */
	public void clearMousePressedRecord() {
		Arrays.fill(mousePressed, false);
	}
	
	/**
	 * Check if a particular key is down
	 * 
	 * @param code The key code of the key to check
	 * @return True if the key is down
	 */
	public boolean isKeyDown(int code) {
		return Keyboard.isKeyDown(code);
	}

	/**
	 * Get the absolute x position of the mouse cursor within the container
	 * 
	 * @return The absolute x position of the mouse cursor
	 */
	public int getAbsoluteMouseX() {
		return Mouse.getX();
	}

	/**
	 * Get the absolute y position of the mouse cursor within the container
	 * 
	 * @return The absolute y position of the mouse cursor
	 */
	public int getAbsoluteMouseY() {
		return height - Mouse.getY();
	}
	   
	/**
	 * Get the x position of the mouse cursor
	 * 
	 * @return The x position of the mouse cursor
	 */
	public int getMouseX() {
		return (int) ((Mouse.getX() * scaleX)+xoffset);
	}
	
	/**
	 * Get the y position of the mouse cursor
	 * 
	 * @return The y position of the mouse cursor
	 */
	public int getMouseY() {
		return (int) (((height-Mouse.getY()) * scaleY)+yoffset);
	}
	
	/**
	 * Check if a given mouse button is down
	 * 
	 * @param button The index of the button to check (starting at 0)
	 * @return True if the mouse button is down
	 */
	public boolean isMouseButtonDown(int button) {
		return Mouse.isButtonDown(button);
	}
	
	/**
	 * Check if any mouse button is down
	 * 
	 * @return True if any mouse button is down
	 */
	private boolean anyMouseDown() {
		for (int i=0;i<3;i++) {
			if (Mouse.isButtonDown(i)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Get a count of the number of controlles available
	 * 
	 * @return The number of controllers available
	 */
	public int getControllerCount() {
		try {
			initControllers();
		} catch (SlickException e) {
			throw new RuntimeException("Failed to initialise controllers");
		}
		
		return controllers.size();
	}
	
	/**
	 * Get the number of axis that are avaiable on a given controller
	 * 
	 * @param controller The index of the controller to check
	 * @return The number of axis available on the controller
	 */
	public int getAxisCount(int controller) {
		return ((Controller) controllers.get(controller)).getAxisCount();
	}
	
	/**
	 * Get the value of the axis with the given index
	 *  
	 * @param controller The index of the controller to check
	 * @param axis The index of the axis to read
	 * @return The axis value at time of reading
	 */ 
	public float getAxisValue(int controller, int axis) {
		return ((Controller) controllers.get(controller)).getAxisValue(axis);
	}

	/**
	 * Get the name of the axis with the given index
	 *  
	 * @param controller The index of the controller to check
	 * @param axis The index of the axis to read
	 * @return The name of the specified axis
	 */ 
	public String getAxisName(int controller, int axis) {
		return ((Controller) controllers.get(controller)).getAxisName(axis);
	}
	
	/**
	 * Check if the controller has the left direction pressed
	 * 
	 * @param controller The index of the controller to check
	 * @return True if the controller is pressed to the left
	 */
	public boolean isControllerLeft(int controller) {
		if (controller >= getControllerCount()) {
			return false;
		}
		
		if (controller == ANY_CONTROLLER) {
			for (int i=0;i<controllers.size();i++) {
				if (isControllerLeft(i)) {
					return true;
				}
			}
			
			return false;
		}
		
		return ((Controller) controllers.get(controller)).getXAxisValue() < -0.5f
				|| ((Controller) controllers.get(controller)).getPovX() < -0.5f;
	}

	/**
	 * Check if the controller has the right direction pressed
	 * 
	 * @param controller The index of the controller to check
	 * @return True if the controller is pressed to the right
	 */
	public boolean isControllerRight(int controller) {
		if (controller >= getControllerCount()) {
			return false;
		}

		if (controller == ANY_CONTROLLER) {
			for (int i=0;i<controllers.size();i++) {
				if (isControllerRight(i)) {
					return true;
				}
			}
			
			return false;
		}
		
		return ((Controller) controllers.get(controller)).getXAxisValue() > 0.5f
   				|| ((Controller) controllers.get(controller)).getPovX() > 0.5f;
	}

	/**
	 * Check if the controller has the up direction pressed
	 * 
	 * @param controller The index of the controller to check
	 * @return True if the controller is pressed to the up
	 */
	public boolean isControllerUp(int controller) {
		if (controller >= getControllerCount()) {
			return false;
		}

		if (controller == ANY_CONTROLLER) {
			for (int i=0;i<controllers.size();i++) {
				if (isControllerUp(i)) {
					return true;
				}
			}
			
			return false;
		}
		return ((Controller) controllers.get(controller)).getYAxisValue() < -0.5f
		   		|| ((Controller) controllers.get(controller)).getPovY() < -0.5f;
	}

	/**
	 * Check if the controller has the down direction pressed
	 * 
	 * @param controller The index of the controller to check
	 * @return True if the controller is pressed to the down
	 */
	public boolean isControllerDown(int controller) {
		if (controller >= getControllerCount()) {
			return false;
		}

		if (controller == ANY_CONTROLLER) {
			for (int i=0;i<controllers.size();i++) {
				if (isControllerDown(i)) {
					return true;
				}
			}
			
			return false;
		}
		
		return ((Controller) controllers.get(controller)).getYAxisValue() > 0.5f
			   || ((Controller) controllers.get(controller)).getPovY() > 0.5f;
	       
	}

	/**
	 * Check if controller button is pressed
	 * 
	 * @param controller The index of the controller to check
	 * @param index The index of the button to check
	 * @return True if the button is pressed
	 */
	public boolean isButtonPressed(int index, int controller) {
		if (controller >= getControllerCount()) {
			return false;
		}

		if (controller == ANY_CONTROLLER) {
			for (int i=0;i<controllers.size();i++) {
				if (isButtonPressed(index, i)) {
					return true;
				}
			}
			
			return false;
		}
		
		return ((Controller) controllers.get(controller)).isButtonPressed(index);
	}
	
	/**
	 * Check if button 1 is pressed
	 * 
	 * @param controller The index of the controller to check
	 * @return True if the button is pressed
	 */
	public boolean isButton1Pressed(int controller) {
		return isButtonPressed(0, controller);
	}

	/**
	 * Check if button 2 is pressed
	 * 
	 * @param controller The index of the controller to check
	 * @return True if the button is pressed
	 */
	public boolean isButton2Pressed(int controller) {
		return isButtonPressed(1, controller);
	}

	/**
	 * Check if button 3 is pressed
	 * 
	 * @param controller The index of the controller to check
	 * @return True if the button is pressed
	 */
	public boolean isButton3Pressed(int controller) {
		return isButtonPressed(2, controller);
	}
	
	/**
	 * Initialise the controllers system
	 * 
	 * @throws SlickException Indicates a failure to use the hardware
	 */
	public void initControllers() throws SlickException {
		if (controllersInited) {
			return;
		}
		
		controllersInited = true;
		try {
			Controllers.create();
			int count = Controllers.getControllerCount();
			
			for (int i = 0; i < count; i++) {
				Controller controller = Controllers.getController(i);

				if ((controller.getButtonCount() >= 3) && (controller.getButtonCount() < MAX_BUTTONS)) {
					controllers.add(controller);
				}
			}
			
			Log.info("Found "+controllers.size()+" controllers");
			for (int i=0;i<controllers.size();i++) {
				Log.info(i+" : "+((Controller) controllers.get(i)).getName());
			}
		} catch (LWJGLException e) {
			if (e.getCause() instanceof ClassNotFoundException) {
				throw new SlickException("Unable to create controller - no jinput found - add jinput.jar to your classpath");
			}
			throw new SlickException("Unable to create controllers");
		} catch (NoClassDefFoundError e) {
			// forget it, no jinput availble
		}
	}
	
	/**
	 * Notification from an event handle that an event has been consumed
	 */
	public void consumeEvent() {
		consumed = true;
	}
	
	/**
	 * A null stream to clear out those horrid errors
	 *
	 * @author kevin
	 */
	private class NullOutputStream extends OutputStream {
		/**
		 * @see java.io.OutputStream#write(int)
		 */
		public void write(int b) throws IOException {
			// null implemetnation
		}
		
	}
	
	/**
	 * Hook to allow us to translate any key character into special key 
	 * codes for easier use.
	 * 
	 * @param key The original key code
	 * @param c The character that was fired
	 * @return The key code to fire
	 */
	private int resolveEventKey(int key, char c) {
		// BUG with LWJGL - equals comes back with keycode = 0
		// See: http://slick.javaunlimited.net/viewtopic.php?t=617
		if ((c == 61) || (key == 0)) {
			return KEY_EQUALS;
		}
		
		return key;
	}
	
	/**
	 * Notification that the mouse has been pressed and hence we
	 * should consider what we're doing with double clicking
	 * 
	 * @param button The button pressed/released
	 * @param x The location of the mouse
	 * @param y The location of the mouse
	 */
	public void considerDoubleClick(int button, int x, int y) {
		if (doubleClickTimeout == 0) {
			clickX = x;
			clickY = y;
			clickButton = button;
			doubleClickTimeout = System.currentTimeMillis() + doubleClickDelay;
			fireMouseClicked(button, x, y, 1);
		} else {
			if (clickButton == button) {
				if ((System.currentTimeMillis() < doubleClickTimeout)) {
					fireMouseClicked(button, x, y, 2);
					doubleClickTimeout = 0;
				}
			}
		}
	}
	
	/**
	 * Poll the state of the input
	 * 
	 * @param width The width of the game view
	 * @param height The height of the game view
	 */
	public void poll(int width, int height) {
		if (paused) {
			clearControlPressedRecord();
			clearKeyPressedRecord();
			clearMousePressedRecord();
			
			while (Keyboard.next()) {}
			while (Mouse.next()) {}
			return;
		}

		if (!Display.isActive()) {
			clearControlPressedRecord();
			clearKeyPressedRecord();
			clearMousePressedRecord();
		}
		
		// add any listeners requested since last time
		for (int i=0;i<keyListenersToAdd.size();i++) {
			addKeyListenerImpl((KeyListener) keyListenersToAdd.get(i));
		}
		keyListenersToAdd.clear();
		for (int i=0;i<mouseListenersToAdd.size();i++) {
			addMouseListenerImpl((MouseListener) mouseListenersToAdd.get(i));
		}
		mouseListenersToAdd.clear();
		
		if (doubleClickTimeout != 0) {
			if (System.currentTimeMillis() > doubleClickTimeout) {
				doubleClickTimeout = 0;
			}
		}
		
		this.height = height;

		Iterator allStarts = allListeners.iterator();
		while (allStarts.hasNext()) {
			ControlledInputReciever listener = (ControlledInputReciever) allStarts.next();
			listener.inputStarted();
		}
		
		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				int eventKey = resolveEventKey(Keyboard.getEventKey(), Keyboard.getEventCharacter());
				
				keys[eventKey] = Keyboard.getEventCharacter();
				pressed[eventKey] = true;
				nextRepeat[eventKey] = System.currentTimeMillis() + keyRepeatInitial;
				
				consumed = false;
				for (int i=0;i<keyListeners.size();i++) {
					KeyListener listener = (KeyListener) keyListeners.get(i);
					
					if (listener.isAcceptingInput()) {
						listener.keyPressed(eventKey, Keyboard.getEventCharacter());
						if (consumed) {
							break;
						}
					}
				}
			} else {
				int eventKey = resolveEventKey(Keyboard.getEventKey(), Keyboard.getEventCharacter());
				nextRepeat[eventKey] = 0;
				
				consumed = false;
				for (int i=0;i<keyListeners.size();i++) {
					KeyListener listener = (KeyListener) keyListeners.get(i);
					if (listener.isAcceptingInput()) {
						listener.keyReleased(eventKey, keys[eventKey]);
						if (consumed) {
							break;
						}
					}
				}
			}
		}
		
		while (Mouse.next()) {
			if (Mouse.getEventButton() >= 0) {
				if (Mouse.getEventButtonState()) {
					consumed = false;
					mousePressed[Mouse.getEventButton()] = true;

					pressedX = (int) (xoffset + (Mouse.getEventX() * scaleX));
					pressedY =  (int) (yoffset + ((height-Mouse.getEventY()) * scaleY));

					for (int i=0;i<mouseListeners.size();i++) {
						MouseListener listener = (MouseListener) mouseListeners.get(i);
						if (listener.isAcceptingInput()) {
							listener.mousePressed(Mouse.getEventButton(), pressedX, pressedY);
							if (consumed) {
								break;
							}
						}
					}
				} else {
					consumed = false;
					mousePressed[Mouse.getEventButton()] = false;
					
					int releasedX = (int) (xoffset + (Mouse.getEventX() * scaleX));
					int releasedY = (int) (yoffset + ((height-Mouse.getEventY()) * scaleY));
					if ((pressedX != -1) && 
					    (pressedY != -1) &&
						(Math.abs(pressedX - releasedX) < mouseClickTolerance) && 
						(Math.abs(pressedY - releasedY) < mouseClickTolerance)) {
						considerDoubleClick(Mouse.getEventButton(), releasedX, releasedY);
						pressedX = pressedY = -1;
					}

					for (int i=0;i<mouseListeners.size();i++) {
						MouseListener listener = (MouseListener) mouseListeners.get(i);
						if (listener.isAcceptingInput()) {
							listener.mouseReleased(Mouse.getEventButton(), releasedX, releasedY);
							if (consumed) {
								break;
							}
						}
					}
				}
			} else {
				if (Mouse.isGrabbed() && displayActive) {
					if ((Mouse.getEventDX() != 0) || (Mouse.getEventDY() != 0)) {
						consumed = false;
						for (int i=0;i<mouseListeners.size();i++) {
							MouseListener listener = (MouseListener) mouseListeners.get(i);
							if (listener.isAcceptingInput()) {
								if (anyMouseDown()) {
									listener.mouseDragged(0, 0, Mouse.getEventDX(), -Mouse.getEventDY());	
								} else {
									listener.mouseMoved(0, 0, Mouse.getEventDX(), -Mouse.getEventDY());
								}
								
								if (consumed) {
									break;
								}
							}
						}
					}
				}
				
				int dwheel = Mouse.getEventDWheel();
				wheel += dwheel;
				if (dwheel != 0) {
					consumed = false;
					for (int i=0;i<mouseListeners.size();i++) {
						MouseListener listener = (MouseListener) mouseListeners.get(i);
						if (listener.isAcceptingInput()) {
							listener.mouseWheelMoved(dwheel);
							if (consumed) {
								break;
							}
						}
					}
				}
			}
		}
		
		if (!displayActive || Mouse.isGrabbed()) {
			lastMouseX = getMouseX();
			lastMouseY = getMouseY();
		} else {
			if ((lastMouseX != getMouseX()) || (lastMouseY != getMouseY())) {
				consumed = false;
				for (int i=0;i<mouseListeners.size();i++) {
					MouseListener listener = (MouseListener) mouseListeners.get(i);
					if (listener.isAcceptingInput()) {
						if (anyMouseDown()) {
							listener.mouseDragged(lastMouseX ,  lastMouseY, getMouseX(), getMouseY());
						} else {
							listener.mouseMoved(lastMouseX ,  lastMouseY, getMouseX(), getMouseY());	
						}
						if (consumed) {
							break;
						}
					}
				}
				lastMouseX = getMouseX();
				lastMouseY = getMouseY();
			}
		}
		
		if (controllersInited) {
			for (int i=0;i<getControllerCount();i++) {
				int count = ((Controller) controllers.get(i)).getButtonCount()+3;
				count = Math.min(count, 24);
				for (int c=0;c<=count;c++) {
					if (controls[i][c] && !isControlDwn(c, i)) {
						controls[i][c] = false;
						fireControlRelease(c, i);
					} else if (!controls[i][c] && isControlDwn(c, i)) {
						controllerPressed[i][c] = true;
						controls[i][c] = true;
						fireControlPress(c, i);
					}
				}
			}
		}
		
		if (keyRepeat) {
			for (int i=0;i<1024;i++) {
				if (pressed[i] && (nextRepeat[i] != 0)) {
					if (System.currentTimeMillis() > nextRepeat[i]) {
						nextRepeat[i] = System.currentTimeMillis() + keyRepeatInterval;
						consumed = false;
						for (int j=0;j<keyListeners.size();j++) {
							KeyListener listener = (KeyListener) keyListeners.get(j);

							if (listener.isAcceptingInput()) {
								listener.keyPressed(i, keys[i]);
								if (consumed) {
									break;
								}
							}
						}
					}
				}
			}
		}

		
		Iterator all = allListeners.iterator();
		while (all.hasNext()) {
			ControlledInputReciever listener = (ControlledInputReciever) all.next();
			listener.inputEnded();
		}
		
		if (Display.isCreated()) {
			displayActive = Display.isActive();
		}
	}
	
	/**
	 * Enable key repeat for this input context. This will cause keyPressed to get called repeatedly
	 * at a set interval while the key is pressed
	 * 
	 * @param initial The interval before key repreating starts after a key press
	 * @param interval The interval between key repeats in ms
	 * @deprecated
	 */
	public void enableKeyRepeat(int initial, int interval) {
		Keyboard.enableRepeatEvents(true);
	}

	/**
	 * Enable key repeat for this input context. Uses the system settings for repeat
	 * interval configuration.
	 */
	public void enableKeyRepeat() {
		Keyboard.enableRepeatEvents(true);
	}
	
	/**
	 * Disable key repeat for this input context
	 */
	public void disableKeyRepeat() {
		Keyboard.enableRepeatEvents(false);
	}
	
	/**
	 * Check if key repeat is enabled
	 * 
	 * @return True if key repeat is enabled
	 */
	public boolean isKeyRepeatEnabled() {
		return Keyboard.areRepeatEventsEnabled();
	}
	
	/**
	 * Fire an event indicating that a control has been pressed
	 * 
	 * @param index The index of the control pressed
	 * @param controllerIndex The index of the controller on which the control was pressed
	 */
	private void fireControlPress(int index, int controllerIndex) {
		consumed = false;
		for (int i=0;i<controllerListeners.size();i++) {
			ControllerListener listener = (ControllerListener) controllerListeners.get(i);
			if (listener.isAcceptingInput()) {
				switch (index) {
				case LEFT:
					listener.controllerLeftPressed(controllerIndex);
					break;
				case RIGHT:
					listener.controllerRightPressed(controllerIndex);
					break;
				case UP:
					listener.controllerUpPressed(controllerIndex);
					break;
				case DOWN:
					listener.controllerDownPressed(controllerIndex);
					break;
				default:
					// assume button pressed
					listener.controllerButtonPressed(controllerIndex, (index - BUTTON1) + 1);
					break;
				}
				if (consumed) {
					break;
				}
			}
		}
	}

	/**
	 * Fire an event indicating that a control has been released
	 * 
	 * @param index The index of the control released
	 * @param controllerIndex The index of the controller on which the control was released
	 */
	private void fireControlRelease(int index, int controllerIndex) {
		consumed = false;
		for (int i=0;i<controllerListeners.size();i++) {
			ControllerListener listener = (ControllerListener) controllerListeners.get(i);
			if (listener.isAcceptingInput()) {
				switch (index) {
				case LEFT:
					listener.controllerLeftReleased(controllerIndex);
					break;
				case RIGHT:
					listener.controllerRightReleased(controllerIndex);
					break;
				case UP:
					listener.controllerUpReleased(controllerIndex);
					break;
				case DOWN:
					listener.controllerDownReleased(controllerIndex);
					break;
				default:
					// assume button release
					listener.controllerButtonReleased(controllerIndex, (index - BUTTON1) + 1);
					break;
				}
				if (consumed) {
					break;
				}
			}
		}
	}
	
	/**
	 * Check if a particular control is currently pressed
	 * 
	 * @param index The index of the control
	 * @param controllerIndex The index of the control to which the control belongs
	 * @return True if the control is pressed
	 */
	private boolean isControlDwn(int index, int controllerIndex) {
		switch (index) {
		case LEFT:
			return isControllerLeft(controllerIndex);
		case RIGHT:
			return isControllerRight(controllerIndex);
		case UP:
			return isControllerUp(controllerIndex);
		case DOWN:
			return isControllerDown(controllerIndex);
		}
		
		if (index >= BUTTON1) {
			return isButtonPressed((index-BUTTON1), controllerIndex);
		}

		throw new RuntimeException("Unknown control index");
	}
	

	/**
	 * Pauses the polling and sending of input events.
	 */
	public void pause() {
		paused = true;

		// Reset all polling arrays
		clearKeyPressedRecord();
		clearMousePressedRecord();
		clearControlPressedRecord();
	}

	/**
	 * Resumes the polling and sending of input events.
	 */
	public void resume() {
		paused = false;
	}
	
	/**
	 * Notify listeners that the mouse button has been clicked
	 * 
	 * @param button The button that has been clicked 
	 * @param x The location at which the button was clicked
	 * @param y The location at which the button was clicked
	 * @param clickCount The number of times the button was clicked (single or double click)
	 */
	private void fireMouseClicked(int button, int x, int y, int clickCount) {
		consumed = false;
		for (int i=0;i<mouseListeners.size();i++) {
			MouseListener listener = (MouseListener) mouseListeners.get(i);
			if (listener.isAcceptingInput()) {
				listener.mouseClicked(button, x, y, clickCount);
				if (consumed) {
					break;
				}
			}
		}
	}
}
