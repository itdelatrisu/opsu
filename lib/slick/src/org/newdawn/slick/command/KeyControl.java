package org.newdawn.slick.command;

/**
 * A control relating to a command indicate that it should be fired when a specific key is pressed
 * or released.
 * 
 * @author joverton
 */
public class KeyControl implements Control {
	/** The key code that needs to be pressed */
    private int keycode;
    
    /**
     * Create a new control that caused an command to be fired on a key pressed/released
     * 
     * @param keycode The code of the key that causes the command
     */
    public KeyControl(int keycode) {     
        this.keycode = keycode;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o instanceof KeyControl) {
        	return ((KeyControl)o).keycode == keycode;
        }
        
        return false;
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return keycode;
    }
}
