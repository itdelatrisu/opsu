package itdelatrisu.opsu.ui;

/**
 * Drag to scroll based on:
 * http://ariya.ofilabs.com/2013/11/javascript-kinetic-scrolling-part-2.html
 * 
 * @author fluddokt (https://github.com/fluddokt)
 */
public class KineticScrolling {

	/** The moving averaging constant */
	final static private float AVG_CONST = 0.2f;
	final static private float ONE_MINUS_AVG_CONST = 1 - AVG_CONST;

	/** The constant used to determine how fast the target position will be reach. */
	final static private int TIME_CONST = 200;
	
	/** The constant used to determine how much of the velocity will be used to launch to the target. */
	final static private float AMPLITUDE_CONST = 0.25f;
	
	/** The current position. */
	float position;

	/** The offset to scroll to the target position. */
	float amplitude;
	
	/** The current target to scroll to. */
	float target;
	
	/** The total amount of time since the mouse button was released. */
	float totalDelta;

	/** The maximum and minimum value the position can reach. */
	float max = Float.MAX_VALUE;
	float min = -Float.MAX_VALUE;

	/** Whether the mouse is currently pressed or not */
	boolean pressed = false;

	/** The change in position since the last update */
	float deltaPosition;
	
	/** The moving average of the velocity. */
	float avgVelocity;
	
	/**
	 * Returns the current Position.
	 * @return the position.
	 */
	public float getPosition() {
		return position;
	}
	
	/**
	 * Updates the scrolling.
	 * @param delta the elapsed time since the last update
	 */
	public void update(float delta) {
		if (!pressed) {
			totalDelta += delta;
			position = target + (float) (-amplitude * Math.exp(-totalDelta / TIME_CONST));
		} else {
			avgVelocity = (ONE_MINUS_AVG_CONST * avgVelocity + AVG_CONST * (deltaPosition * 1000f / delta));
			
			position += deltaPosition;
			target = position ;
			deltaPosition = 0;
		}
		if (position > max) {
			amplitude = 0;
			target = position = max;
		}
		if (position < min) {
			amplitude = 0;
			target = position = min;
		}
	}

	/**
	 * Scrolls to the position.
	 * @param newPosition the position to scroll to.
	 */
	public void scrollToPosition(float newPosition) {
		amplitude = newPosition - position;
		target = newPosition;
		totalDelta = 0;
	}

	/**
	 * Scrolls to an offset from target.
	 * @param offset the offset from the target to scroll to.
	 */
	public void scrollOffset(float offset) {
		scrollToPosition(target + offset);
	}

	/**
	 * Sets the position.
	 * @param newPosition the position to be set
	 */
	public void setPosition(float newPosition) {
		pressed();
		release();
		target = newPosition;
		position = target;
	}

	/**
	 * Set the position relative to an offset.
	 * @param offset the offset from the position.
	 */
	public void addOffset(float offset) {
		setPosition(position + offset);
	}

	/**
	 * Call this when the mouse button has been pressed.
	 */
	public void pressed() {
		if (pressed)
			return;
		pressed = true;
		avgVelocity = 0;
	}

	/**
	 * Call this when the mouse button has been released.
	 */
	public void release() {
		if (!pressed)
			return;
		pressed = false;
		amplitude = AMPLITUDE_CONST * avgVelocity;
		target = Math.round(target + amplitude);
		totalDelta = 0;
	}

	/**
	 * Call this when the mouse has been dragged.
	 * @param distance the amount that the mouse has been dragged
	 */
	public void dragged(float distance) {
		deltaPosition += distance;
	}

	/**
	 * Set the minimum and maximum bound.
	 * @param min the minimum bound
	 * @param max the maximum bound
	 */
	public void setMinMax(float min, float max) {
		this.min = min;
		this.max = max;
	}
}
