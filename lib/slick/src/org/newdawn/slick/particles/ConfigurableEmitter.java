package org.newdawn.slick.particles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;
import org.newdawn.slick.util.FastTrig;
import org.newdawn.slick.util.Log;

/**
 * An emitter than can be externally configured. This configuration can also be
 * saved/loaded using the ParticleIO class.
 * 
 * @see ParticleIO
 * 
 * @author kevin
 */
public class ConfigurableEmitter implements ParticleEmitter {
	/** The path from which the images should be loaded */
	private static String relativePath = "";

	/**
	 * Set the path from which images should be loaded
	 * 
	 * @param path
	 *            The path from which images should be loaded
	 */
	public static void setRelativePath(String path) {
		if (!path.endsWith("/")) {
			path += "/";
		}
		relativePath = path;
	}

	/** The spawn interval range property - how often spawn happens */
	public Range spawnInterval = new Range(100, 100);
	/** The spawn count property - how many particles are spawned each time */
	public Range spawnCount = new Range(5, 5);
	/** The initial life of the new pixels */
	public Range initialLife = new Range(1000, 1000);
	/** The initial size of the new pixels */
	public Range initialSize = new Range(10, 10);
	/** The offset from the x position */
	public Range xOffset = new Range(0, 0);
	/** The offset from the y position */
	public Range yOffset = new Range(0, 0);
	/** The spread of the particles */
	public RandomValue spread = new RandomValue(360);
	/** The angular offset */
	public SimpleValue angularOffset = new SimpleValue(0);
	/** The initial distance of the particles */
	public Range initialDistance = new Range(0, 0);
	/** The speed particles fly out */
	public Range speed = new Range(50, 50);
	/** The growth factor on the particles */
	public SimpleValue growthFactor = new SimpleValue(0);
	/** The factor of gravity to apply */
	public SimpleValue gravityFactor = new SimpleValue(0);
	/** The factor of wind to apply */
	public SimpleValue windFactor = new SimpleValue(0);
	/** The length of the effect */
	public Range length = new Range(1000, 1000);
	/**
	 * The color range
	 * 
	 * @see ColorRecord
	 */
	public ArrayList colors = new ArrayList();
	/** The starting alpha value */
	public SimpleValue startAlpha = new SimpleValue(255);
	/** The ending alpha value */
	public SimpleValue endAlpha = new SimpleValue(0);

	/** Whiskas - Interpolated value for alpha */
	public LinearInterpolator alpha;
	/** Whiskas - Interpolated value for size */
	public LinearInterpolator size;
	/** Whiskas - Interpolated value for velocity */
	public LinearInterpolator velocity;
	/** Whiskas - Interpolated value for y axis scaling */
	public LinearInterpolator scaleY;

	/** The number of particles that will be emitted */
	public Range emitCount = new Range(1000, 1000);
	/** The points indicate */
	public int usePoints = Particle.INHERIT_POINTS;

	/** True if the quads should be orieted based on velocity */
	public boolean useOriented = false;
	/**
	 * True if the additivie blending mode should be used for particles owned by
	 * this emitter
	 */
	public boolean useAdditive = false;

	/** The name attribute */
	public String name;
	/** The name of the image in use */
	public String imageName = "";
	/** The image being used for the particles */
	private Image image;
	/** True if the image needs updating */
	private boolean updateImage;

	/** True if the emitter is enabled */
	private boolean enabled = true;
	/** The x coordinate of the position of this emitter */
	private float x;
	/** The y coordinate of the position of this emitter */
	private float y;
	/** The time in milliseconds til the next spawn */
	private int nextSpawn = 0;

	/** The timeout counting down to spawn */
	private int timeout;
	/** The number of particles in use by this emitter */
	private int particleCount;
	/** The system this emitter is being updated to */
	private ParticleSystem engine;
	/** The number of particles that are left ot emit */
	private int leftToEmit;

	/** True if we're wrapping up */
	protected boolean wrapUp = false;
	/** True if the system has completed due to a wrap up */
	protected boolean completed = false;
	/** True if we need to adjust particles for movement */
	protected boolean adjust;
	/** The amount to adjust on the x axis */
	protected float adjustx;
	/** The amount to adjust on the y axis */
	protected float adjusty;
	
	/**
	 * Create a new emitter configurable externally
	 * 
	 * @param name
	 *            The name of emitter
	 */
	public ConfigurableEmitter(String name) {
		this.name = name;
		leftToEmit = (int) emitCount.random();
		timeout = (int) (length.random());

		colors.add(new ColorRecord(0, Color.white));
		colors.add(new ColorRecord(1, Color.red));

		ArrayList curve = new ArrayList();
		curve.add(new Vector2f(0.0f, 0.0f));
		curve.add(new Vector2f(1.0f, 255.0f));
		alpha = new LinearInterpolator(curve, 0, 255);

		curve = new ArrayList();
		curve.add(new Vector2f(0.0f, 0.0f));
		curve.add(new Vector2f(1.0f, 255.0f));
		size = new LinearInterpolator(curve, 0, 255);

		curve = new ArrayList();
		curve.add(new Vector2f(0.0f, 0.0f));
		curve.add(new Vector2f(1.0f, 1.0f));
		velocity = new LinearInterpolator(curve, 0, 1);

		curve = new ArrayList();
		curve.add(new Vector2f(0.0f, 0.0f));
		curve.add(new Vector2f(1.0f, 1.0f));
		scaleY = new LinearInterpolator(curve, 0, 1);
	}

	/**
	 * Set the name of the image to use on a per particle basis. The complete
	 * reference to the image is required (based on the relative path)
	 * 
	 * @see #setRelativePath(String)
	 * 
	 * @param imageName
	 *            The name of the image to use on a per particle reference
	 */
	public void setImageName(String imageName) {
		if (imageName.length() == 0) {
			imageName = null;
		}

		this.imageName = imageName;
		if (imageName == null) {
			image = null;
		} else {
			updateImage = true;
		}
	}
	
	/**
	 * The name of the image to load
	 * 
	 * @return The name of the image to load
	 */
	public String getImageName() {
		return imageName;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[" + name + "]";
	}

	/**
	 * Set the position of this particle source
	 * 
	 * @param x
	 *            The x coodinate of that this emitter should spawn at
	 * @param y
	 *            The y coodinate of that this emitter should spawn at
	 */
	public void setPosition(float x, float y) {
		setPosition(x,y,true);
	}

	/**
	 * Set the position of this particle source
	 * 
	 * @param x
	 *            The x coodinate of that this emitter should spawn at
	 * @param y
	 *            The y coodinate of that this emitter should spawn at
	 * @param moveParticles
	 * 		      True if particles should be moved with the emitter
	 */
	public void setPosition(float x, float y, boolean moveParticles) {
		if (moveParticles) {
			adjust = true;
			adjustx -= this.x - x;
			adjusty -= this.y - y;
		}
		this.x = x;
		this.y = y;		
	}
	
	/**
	 * Get the base x coordiante for spawning particles
	 * 
	 * @return The x coordinate for spawning particles
	 */
	public float getX() {
		return x;
	}

	/**
	 * Get the base y coordiante for spawning particles
	 * 
	 * @return The y coordinate for spawning particles
	 */
	public float getY() {
		return y;
	}

	/**
	 * @see org.newdawn.slick.particles.ParticleEmitter#isEnabled()
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @see org.newdawn.slick.particles.ParticleEmitter#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @see org.newdawn.slick.particles.ParticleEmitter#update(org.newdawn.slick.particles.ParticleSystem,
	 *      int)
	 */
	public void update(ParticleSystem system, int delta) {
		this.engine = system;

		if (!adjust) {
			adjustx = 0;
			adjusty = 0;
		} else {
			adjust = false;
		}
		
		if (updateImage) {
			updateImage = false;
			try {
				image = new Image(relativePath + imageName);
			} catch (SlickException e) {
				image = null;
				Log.error(e);
			}
		}

		if ((wrapUp) || 
		    ((length.isEnabled()) && (timeout < 0)) ||
		    ((emitCount.isEnabled() && (leftToEmit <= 0)))) {
			if (particleCount == 0) {
				completed = true;
			}
		}
		particleCount = 0;
		
		if (wrapUp) {
			return;
		}
		
		if (length.isEnabled()) {
			if (timeout < 0) {
				return;
			}
			timeout -= delta;
		}
		if (emitCount.isEnabled()) {
			if (leftToEmit <= 0) {
				return;
			}
		}

		nextSpawn -= delta;
		if (nextSpawn < 0) {
			nextSpawn = (int) spawnInterval.random();
			int count = (int) spawnCount.random();

			for (int i = 0; i < count; i++) {
				Particle p = system.getNewParticle(this, initialLife.random());
				p.setSize(initialSize.random());
				p.setPosition(x + xOffset.random(), y + yOffset.random());
				p.setVelocity(0, 0, 0);

				float dist = initialDistance.random();
				float power = speed.random();
				if ((dist != 0) || (power != 0)) {
					float s = spread.getValue(0);
					float ang = (s + angularOffset.getValue(0) - (spread
							.getValue() / 2)) - 90;
					float xa = (float) FastTrig.cos(Math.toRadians(ang)) * dist;
					float ya = (float) FastTrig.sin(Math.toRadians(ang)) * dist;
					p.adjustPosition(xa, ya);

					float xv = (float) FastTrig.cos(Math.toRadians(ang));
					float yv = (float) FastTrig.sin(Math.toRadians(ang));
					p.setVelocity(xv, yv, power * 0.001f);
				}

				if (image != null) {
					p.setImage(image);
				}

				ColorRecord start = (ColorRecord) colors.get(0);
				p.setColor(start.col.r, start.col.g, start.col.b, startAlpha
						.getValue(0) / 255.0f);
				p.setUsePoint(usePoints);
				p.setOriented(useOriented);

				if (emitCount.isEnabled()) {
					leftToEmit--;
					if (leftToEmit <= 0) {
						break;
					}
				}
			}
		}
	}

	/**
	 * @see org.newdawn.slick.particles.ParticleEmitter#updateParticle(org.newdawn.slick.particles.Particle,
	 *      int)
	 */
	public void updateParticle(Particle particle, int delta) {
		particleCount++;
		
		// adjust the particles if required
		particle.x += adjustx;
		particle.y += adjusty;

		particle.adjustVelocity(windFactor.getValue(0) * 0.00005f * delta, gravityFactor
				.getValue(0) * 0.00005f * delta);
		
		float offset = particle.getLife() / particle.getOriginalLife();
		float inv = 1 - offset;
		float colOffset = 0;
		float colInv = 1;

		Color startColor = null;
		Color endColor = null;
		for (int i = 0; i < colors.size() - 1; i++) {
			ColorRecord rec1 = (ColorRecord) colors.get(i);
			ColorRecord rec2 = (ColorRecord) colors.get(i + 1);

			if ((inv >= rec1.pos) && (inv <= rec2.pos)) {
				startColor = rec1.col;
				endColor = rec2.col;

				float step = rec2.pos - rec1.pos;
				colOffset = inv - rec1.pos;
				colOffset /= step;
				colOffset = 1 - colOffset;
				colInv = 1 - colOffset;
			}
		}

		if (startColor != null) {
			float r = (startColor.r * colOffset) + (endColor.r * colInv);
			float g = (startColor.g * colOffset) + (endColor.g * colInv);
			float b = (startColor.b * colOffset) + (endColor.b * colInv);

			float a;
			if (alpha.isActive()) {
				a = alpha.getValue(inv) / 255.0f;
			} else {
				a = ((startAlpha.getValue(0) / 255.0f) * offset)
						+ ((endAlpha.getValue(0) / 255.0f) * inv);
			}
			particle.setColor(r, g, b, a);
		}

		if (size.isActive()) {
			float s = size.getValue(inv);
			particle.setSize(s);
		} else {
			particle.adjustSize(delta * growthFactor.getValue(0) * 0.001f);
		}

		if (velocity.isActive()) {
			particle.setSpeed(velocity.getValue(inv));
		}

		if (scaleY.isActive()) {
			particle.setScaleY(scaleY.getValue(inv));
		}
	}

	/**
	 * Check if this emitter has completed it's cycle
	 * 
	 * @return True if the emitter has completed it's cycle
	 */
	public boolean completed() {
		if (engine == null) {
			return false;
		}

		if (length.isEnabled()) {
			if (timeout > 0) {
				return false;
			}
			return completed;
		}
		if (emitCount.isEnabled()) {
			if (leftToEmit > 0) {
				return false;
			}
			return completed;
		}

		if (wrapUp) {
			return completed;
		}
		
		return false;
	}

	/**
	 * Cause the emitter to replay it's circle
	 */
	public void replay() {
		reset();
		nextSpawn = 0;
		leftToEmit = (int) emitCount.random();
		timeout = (int) (length.random());
	}

	/**
	 * Release all the particles held by this emitter
	 */
	public void reset() {
	    completed = false; 
		if (engine != null) {
			engine.releaseAll(this);
		}
	}

	/**
	 * Check if the replay has died out - used by the editor
	 */
	public void replayCheck() {
		if (completed()) {
			if (engine != null) {
				if (engine.getParticleCount() == 0) {
					replay();
				}
			}
		}
	}
	
	/**
	 * Create a duplicate of this emitter.
	 * The duplicate should be added to a ParticleSystem to be used.
	 * @return a copy if no IOException occurred, null otherwise
	 */
	public ConfigurableEmitter duplicate() {
		ConfigurableEmitter theCopy = null;
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ParticleIO.saveEmitter(bout, this);
			ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
			theCopy = ParticleIO.loadEmitter(bin);
		} catch (IOException e) {
			Log.error("Slick: ConfigurableEmitter.duplicate(): caught exception " + e.toString());
			return null;
		}
		return theCopy;
	}

	/**
	 * a general interface to provide a general value :]
	 * 
	 * @author void
	 */
	public interface Value {
		/**
		 * get the current value that might depend from the given time
		 * 
		 * @param time
		 * @return the current value
		 */
		public float getValue(float time);
	}

	/**
	 * A configurable simple single value
	 * 
	 * @author void
	 */
	public class SimpleValue implements Value {
		/** The value configured */
		private float value;
		/** The next value */
		private float next;

		/**
		 * Create a new configurable new value
		 * 
		 * @param value
		 *            The initial value
		 */
		private SimpleValue(float value) {
			this.value = value;
		}

		/**
		 * Get the currently configured value
		 * 
		 * @return The currently configured value
		 */
		public float getValue(float time) {
			return value;
		}

		/**
		 * Set the configured value
		 * 
		 * @param value
		 *            The configured value
		 */
		public void setValue(float value) {
			this.value = value;
		}
	}

	/**
	 * A configurable simple linear random value
	 * 
	 * @author void
	 */
	public class RandomValue implements Value {
		/** The value configured */
		private float value;

		/**
		 * Create a new configurable new value
		 * 
		 * @param value
		 *            The initial value
		 */
		private RandomValue(float value) {
			this.value = value;
		}

		/**
		 * Get the currently configured value
		 * 
		 * @return The currently configured value
		 */
		public float getValue(float time) {
			return (float) (Math.random() * value);
		}

		/**
		 * Set the configured value
		 * 
		 * @param value
		 *            The configured value
		 */
		public void setValue(float value) {
			this.value = value;
		}

		/**
		 * get the configured value
		 * 
		 * @return the configured value
		 */
		public float getValue() {
			return value;
		}
	}

	/**
	 * A value computed based on linear interpolation between a set of points
	 * 
	 * @author void
	 */
	public class LinearInterpolator implements Value {
		/** The list of points to interpolate between */
		private ArrayList curve;
		/** True if this interpolation value is active */
		private boolean active;
		/** The minimum value in the data set */
		private int min;
		/** The maximum value in the data set */
		private int max;

		/**
		 * Create a new interpolated value
		 * 
		 * @param curve The set of points to interpolate between
		 * @param min The minimum value in the dataset
		 * @param max The maximum value possible in the dataset
		 */
		public LinearInterpolator(ArrayList curve, int min, int max) {
			this.curve = curve;
			this.min = min;
			this.max = max;
			this.active = false;
		}

		/**
		 * Set the collection of data points to interpolate between
		 * 
		 * @param curve The list of data points to interpolate between
		 */
		public void setCurve(ArrayList curve) {
			this.curve = curve;
		}

		/**
		 * The list of data points to interpolate between
		 * 
		 * @return A list of Vector2f of the data points to interpolate between
		 */
		public ArrayList getCurve() {
			return curve;
		}

		/**
		 * Get the value to use at a given time value
		 * 
		 * @param t The time value (expecting t in [0,1])
		 * @return The value to use at the specified time
		 */
		public float getValue(float t) {
			// first: determine the segment we are in
			Vector2f p0 = (Vector2f) curve.get(0);
			for (int i = 1; i < curve.size(); i++) {
				Vector2f p1 = (Vector2f) curve.get(i);

				if (t >= p0.getX() && t <= p1.getX()) {
					// found the segment
					float st = (t - p0.getX())
							/ (p1.getX() - p0.getX());
					float r = p0.getY() + st
							* (p1.getY() - p0.getY());
					// System.out.println( "t: " + t + ", " + p0.x + ", " + p0.y
					// + " : " + p1.x + ", " + p1.y + " => " + r );

					return r;
				}

				p0 = p1;
			}
			return 0;
		}

		/**
		 * Check if this interpolated value should be used
		 * 
		 * @return True if this value is in use
		 */
		public boolean isActive() {
			return active;
		}

		/**
		 * Indicate if this interpoalte value should be used
		 * 
		 * @param active True if this value should be used
		 */
		public void setActive(boolean active) {
			this.active = active;
		}

		/**
		 * Get the maxmimum value possible in this data set
		 * 
		 * @return The maximum value possible in this data set
		 */
		public int getMax() {
			return max;
		}

		/**
		 * Set the maximum value possible in this data set
		 * 
		 * @param max The maximum value possible in this data set
		 */
		public void setMax(int max) {
			this.max = max;
		}

		/**
		 * Get the minimum value possible in this data set
		 * 
		 * @return The minimum value possible in this data set
		 */
		public int getMin() {
			return min;
		}

		/**
		 * Set the minimum value possible in this data set
		 * 
		 * @param min The minimum value possible in this data set
		 */
		public void setMin(int min) {
			this.min = min;
		}
	}

	/**
	 * A single element in the colour range of this emitter
	 * 
	 * @author kevin
	 */
	public class ColorRecord {
		/** The position in the life cycle */
		public float pos;
		/** The color at this position */
		public Color col;

		/**
		 * Create a new record
		 * 
		 * @param pos
		 *            The position in the life cycle (0 = start, 1 = end)
		 * @param col
		 *            The color applied at this position
		 */
		public ColorRecord(float pos, Color col) {
			this.pos = pos;
			this.col = col;
		}
	}

	/**
	 * Add a point in the colour cycle
	 * 
	 * @param pos
	 *            The position in the life cycle (0 = start, 1 = end)
	 * @param col
	 *            The color applied at this position
	 */
	public void addColorPoint(float pos, Color col) {
		colors.add(new ColorRecord(pos, col));
	}

	/**
	 * A simple bean describing a range of values
	 * 
	 * @author kevin
	 */
	public class Range {
		/** The maximum value in the range */
		private float max;
		/** The minimum value in the range */
		private float min;
		/** True if this range application is enabled */
		private boolean enabled = false;

		/**
		 * Create a new configurable range
		 * 
		 * @param min
		 *            The minimum value of the range
		 * @param max
		 *            The maximum value of the range
		 */
		private Range(float min, float max) {
			this.min = min;
			this.max = max;
		}

		/**
		 * Generate a random number in the range
		 * 
		 * @return The random number from the range
		 */
		public float random() {
			return (float) (min + (Math.random() * (max - min)));
		}

		/**
		 * Check if this configuration option is enabled
		 * 
		 * @return True if the range is enabled
		 */
		public boolean isEnabled() {
			return enabled;
		}

		/**
		 * Indicate if this option should be enabled
		 * 
		 * @param enabled
		 *            True if this option should be enabled
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
		 * Get the maximum value for this range
		 * 
		 * @return The maximum value for this range
		 */
		public float getMax() {
			return max;
		}

		/**
		 * Set the maxmium value for this range
		 * 
		 * @param max
		 *            The maximum value for this range
		 */
		public void setMax(float max) {
			this.max = max;
		}

		/**
		 * Get the minimum value for this range
		 * 
		 * @return The minimum value for this range
		 */
		public float getMin() {
			return min;
		}

		/**
		 * Set the minimum value for this range
		 * 
		 * @param min
		 *            The minimum value for this range
		 */
		public void setMin(float min) {
			this.min = min;
		}
	}

	public boolean useAdditive() {
		return useAdditive;
	}
	
	public boolean isOriented() {
		return this.useOriented;
	}
	
	public boolean usePoints(ParticleSystem system) {
		return (this.usePoints == Particle.INHERIT_POINTS) && (system.usePoints()) ||
			   (this.usePoints == Particle.USE_POINTS); 
	}

	public Image getImage() {
		return image;
	}

	public void wrapUp() {
		wrapUp = true;
	}

	public void resetState() {
		wrapUp = false;
		replay();
	}
}
