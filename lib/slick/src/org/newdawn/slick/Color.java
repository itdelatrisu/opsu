package org.newdawn.slick;

import java.io.Serializable;
import java.nio.FloatBuffer;

import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;

/**
 * A simple wrapper round the values required for a colour
 * 
 * @author Kevin Glass
 */
public class Color implements Serializable {
	/** The version ID for this class  */
	private static final long serialVersionUID = 1393939L;
	
	/** The renderer to use for all GL operations */
	protected transient SGL GL = Renderer.get();
	
	/** The fixed color transparent */
    public static final Color transparent = new Color(0.0f,0.0f,0.0f,0.0f);
	/** The fixed colour white */
	public static final Color white = new Color(1.0f,1.0f,1.0f,1.0f);
	/** The fixed colour yellow */
	public static final Color yellow = new Color(1.0f,1.0f,0,1.0f);
	/** The fixed colour red */
	public static final Color red = new Color(1.0f,0,0,1.0f);
	/** The fixed colour blue */
	public static final Color blue = new Color(0,0,1.0f,1.0f);
	/** The fixed colour green */
	public static final Color green = new Color(0,1.0f,0,1.0f);
	/** The fixed colour black */
	public static final Color black = new Color(0,0,0,1.0f);
	/** The fixed colour gray */
	public static final Color gray = new Color(0.5f,0.5f,0.5f,1.0f);
	/** The fixed colour cyan */
	public static final Color cyan = new Color(0,1.0f,1.0f,1.0f);
	/** The fixed colour dark gray */
	public static final Color darkGray = new Color(0.3f,0.3f,0.3f,1.0f);
	/** The fixed colour light gray */
	public static final Color lightGray = new Color(0.7f,0.7f,0.7f,1.0f);
	/** The fixed colour dark pink */
    public final static Color pink      = new Color(255, 175, 175, 255);
	/** The fixed colour dark orange */
    public final static Color orange 	= new Color(255, 200, 0, 255);
	/** The fixed colour dark magenta */
    public final static Color magenta	= new Color(255, 0, 255, 255);
    
	/** The red component of the colour */
	public float r;
	/** The green component of the colour */
	public float g;
	/** The blue component of the colour */
	public float b;
	/** The alpha component of the colour */
	public float a = 1.0f;
	
	/**
	 * Copy constructor
	 * 
	 * @param color The color to copy into the new instance
	 */
	public Color(Color color) {
		r = color.r;
		g = color.g;
		b = color.b;
		a = color.a;
	}

	/**
	 * Create a component based on the first 4 elements of a float buffer
	 * 
	 * @param buffer The buffer to read the color from
	 */
	public Color(FloatBuffer buffer) {
		this.r = buffer.get();
		this.g = buffer.get();
		this.b = buffer.get();
		this.a = buffer.get();
	}
	
	/**
	 * Create a 3 component colour
	 * 
	 * @param r The red component of the colour (0.0 -> 1.0)
	 * @param g The green component of the colour (0.0 -> 1.0)
	 * @param b The blue component of the colour (0.0 -> 1.0)
	 */
	public Color(float r,float g,float b) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = 1;
	}

	/**
	 * Create a 4 component colour
	 * 
	 * @param r The red component of the colour (0.0 -> 1.0)
	 * @param g The green component of the colour (0.0 -> 1.0)
	 * @param b The blue component of the colour (0.0 -> 1.0)
	 * @param a The alpha component of the colour (0.0 -> 1.0)
	 */
	public Color(float r,float g,float b,float a) {
		this.r = Math.min(r, 1);
		this.g = Math.min(g, 1);
		this.b = Math.min(b, 1);
		this.a = Math.min(a, 1);
	}

	/**
	 * Create a 3 component colour
	 * 
	 * @param r The red component of the colour (0 -> 255)
	 * @param g The green component of the colour (0 -> 255)
	 * @param b The blue component of the colour (0 -> 255)
	 */
	public Color(int r,int g,int b) {
		this.r = r / 255.0f;
		this.g = g / 255.0f;
		this.b = b / 255.0f;
		this.a = 1;
	}

	/**
	 * Create a 4 component colour
	 * 
	 * @param r The red component of the colour (0 -> 255)
	 * @param g The green component of the colour (0 -> 255)
	 * @param b The blue component of the colour (0 -> 255)
	 * @param a The alpha component of the colour (0 -> 255)
	 */
	public Color(int r,int g,int b,int a) {
		this.r = r / 255.0f;
		this.g = g / 255.0f;
		this.b = b / 255.0f;
		this.a = a / 255.0f;
	}
	
	/**
	 * Create a colour from an evil integer packed 0xAARRGGBB. If AA 
	 * is specified as zero then it will be interpreted as unspecified
	 * and hence a value of 255 will be recorded.
	 * 
	 * @param value The value to interpret for the colour
	 */
	public Color(int value) {
		int r = (value & 0x00FF0000) >> 16;
		int g = (value & 0x0000FF00) >> 8;
		int b =	(value & 0x000000FF);
		int a = (value & 0xFF000000) >> 24;
				
		if (a < 0) {
			a += 256;
		}
		if (a == 0) {
			a = 255;
		}
		
		this.r = r / 255.0f;
		this.g = g / 255.0f;
		this.b = b / 255.0f;
		this.a = a / 255.0f;
	}
	
	/**
	 * Decode a number in a string and process it as a colour
	 * reference.
	 * 
	 * @param nm The number string to decode
	 * @return The color generated from the number read
	 */
	public static Color decode(String nm) {
		return new Color(Integer.decode(nm).intValue());
	}
	
	/**
	 * Bind this colour to the GL context
	 */
	public void bind() {
		GL.glColor4f(r,g,b,a);
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return ((int) (r+g+b+a)*255);
	}
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (other instanceof Color) {
			Color o = (Color) other;
			return ((o.r == r) && (o.g == g) && (o.b == b) && (o.a == a));
		}
		
		return false;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Color ("+r+","+g+","+b+","+a+")";
	}

	/**
	 * Make a darker instance of this colour
	 * 
	 * @return The darker version of this colour
	 */
	public Color darker() {
		return darker(0.5f);
	}
	
	/**
	 * Make a darker instance of this colour
	 * 
	 * @param scale The scale down of RGB (i.e. if you supply 0.03 the colour will be darkened by 3%)
	 * @return The darker version of this colour
	 */
	public Color darker(float scale) {
        scale = 1 - scale;
		Color temp = new Color(r * scale,g * scale,b * scale,a);
		
		return temp;
	}

	/**
	 * Make a brighter instance of this colour
	 * 
	 * @return The brighter version of this colour
	 */
	public Color brighter() {
		return brighter(0.2f);
	}

	/**
	 * Get the red byte component of this colour
	 * 
	 * @return The red component (range 0-255)
	 */
	public int getRed() {
		return (int) (r * 255);
	}

	/**
	 * Get the green byte component of this colour
	 * 
	 * @return The green component (range 0-255)
	 */
	public int getGreen() {
		return (int) (g * 255);
	}

	/**
	 * Get the blue byte component of this colour
	 * 
	 * @return The blue component (range 0-255)
	 */
	public int getBlue() {
		return (int) (b * 255);
	}

	/**
	 * Get the alpha byte component of this colour
	 * 
	 * @return The alpha component (range 0-255)
	 */
	public int getAlpha() {
		return (int) (a * 255);
	}
	
	/**
	 * Get the red byte component of this colour
	 * 
	 * @return The red component (range 0-255)
	 */
	public int getRedByte() {
		return (int) (r * 255);
	}

	/**
	 * Get the green byte component of this colour
	 * 
	 * @return The green component (range 0-255)
	 */
	public int getGreenByte() {
		return (int) (g * 255);
	}

	/**
	 * Get the blue byte component of this colour
	 * 
	 * @return The blue component (range 0-255)
	 */
	public int getBlueByte() {
		return (int) (b * 255);
	}

	/**
	 * Get the alpha byte component of this colour
	 * 
	 * @return The alpha component (range 0-255)
	 */
	public int getAlphaByte() {
		return (int) (a * 255);
	}
	
	/**
	 * Make a brighter instance of this colour
	 * 
	 * @param scale The scale up of RGB (i.e. if you supply 0.03 the colour will be brightened by 3%)
	 * @return The brighter version of this colour
	 */
	public Color brighter(float scale) {
        scale += 1;
		Color temp = new Color(r * scale,g * scale,b * scale,a);
		
		return temp;
	}
	
	/**
	 * Multiply this color by another
	 *
	 * @param c the other color
	 * @return product of the two colors
	 */
	public Color multiply(Color c) {
		return new Color(r * c.r, g * c.g, b * c.b, a * c.a);
	}

	/**
	 * Add another colour to this one
	 * 
	 * @param c The colour to add 
	 */
	public void add(Color c) {
		r += c.r;
		g += c.g;
		b += c.b;
		a += c.a;
	}
	
	/**
	 * Scale the components of the colour by the given value
	 * 
	 * @param value The value to scale by
	 */
	public void scale(float value) {
		r *= value;
		g *= value;
		b *= value;
		a *= value;
	}
	
	/**
	 * Add another colour to this one
	 * 
	 * @param c The colour to add 
	 * @return The copy which has had the color added to it
	 */
	public Color addToCopy(Color c) {
		Color copy = new Color(r,g,b,a);
		copy.r += c.r;
		copy.g += c.g;
		copy.b += c.b;
		copy.a += c.a;
		
		return copy;
	}
	
	/**
	 * Scale the components of the colour by the given value
	 * 
	 * @param value The value to scale by
	 * @return The copy which has been scaled
	 */
	public Color scaleCopy(float value) {
		Color copy = new Color(r,g,b,a);
		copy.r *= value;
		copy.g *= value;
		copy.b *= value;
		copy.a *= value;
		
		return copy;
	}
}
