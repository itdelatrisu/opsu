package org.newdawn.slick.util;

/**
 * Utility to handle Java's odd trig performance issues
 *
 * @author JeffK
 */
public class FastTrig {
   /**
	 * Fast Trig functions for x86.  
	 * This forces the trig functiosn to stay within the safe area on the x86 processor (-45 degrees to +45 degrees)
	 * The results may be very slightly off from what the Math and StrictMath trig functions give due to
	 * rounding in the angle reduction but it will be very very close.
	 * 
	 * @param radians The original angle
	 * @return The reduced Sin angle
	 */
	private static double reduceSinAngle(double radians) {
		double orig = radians;
		radians %= Math.PI * 2.0; // put us in -2PI to +2PI space
		if (Math.abs(radians) > Math.PI) { // put us in -PI to +PI space
			radians = radians - (Math.PI * 2.0);
		}
		if (Math.abs(radians) > Math.PI / 2) {// put us in -PI/2 to +PI/2 space
			radians = Math.PI - radians;
		}

		return radians;
	}

	/**
	 * Get the sine of an angle
	 * 
	 * @param radians The angle
	 * @return The sine of the angle
	 */
	public static double sin(double radians) {
		radians = reduceSinAngle(radians); // limits angle to between -PI/2 and +PI/2
		if (Math.abs(radians) <= Math.PI / 4) {
			return Math.sin(radians);
		} else {
			return Math.cos(Math.PI / 2 - radians);
		}
	}

	/**
	 * Get the cosine of an angle
	 * 
	 * @param radians The angle
	 * @return The cosine of the angle
	 */
	public static double cos(double radians) {
		return sin(radians + Math.PI / 2);
	}

}
