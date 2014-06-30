package org.newdawn.slick.particles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.util.Log;

/**
 * A particle syste responsible for maintaining a set of data about individual 
 * particles which are created and controlled by assigned emitters. This pseudo 
 * chaotic nature hopes to give more organic looking effects
 *
 * @author kevin
 */
public class ParticleSystem {
	/** The renderer to use for all GL operations */
	protected SGL GL = Renderer.get();
	
	/** The blending mode for the glowy style */
	public static final int BLEND_ADDITIVE = 1;
	/** The blending mode for the normal style */
	public static final int BLEND_COMBINE = 2;
	
	/** The default number of particles in the system */
	private static final int DEFAULT_PARTICLES = 100;

	/** List of emitters to be removed */
	private ArrayList removeMe = new ArrayList();
	
	/**
	 * Set the path from which images should be loaded
	 * 
	 * @param path
	 *            The path from which images should be loaded
	 */
	public static void setRelativePath(String path) {
		ConfigurableEmitter.setRelativePath(path);
	}
	
	/**
	 * A pool of particles being used by a specific emitter
	 * 
	 * @author void
	 */
	private class ParticlePool
	{
		/** The particles being rendered and maintained */
		public Particle[] particles;
		/** The list of particles left to be used, if this size() == 0 then the particle engine was too small for the effect */
		public ArrayList available;
		
		/**
		 * Create a new particle pool contiaining a set of particles
		 * 
		 * @param system The system that owns the particles over all
		 * @param maxParticles The maximum number of particles in the pool
		 */
		public ParticlePool( ParticleSystem system, int maxParticles )
		{
			particles = new Particle[ maxParticles ];
			available = new ArrayList();
			
			for( int i=0; i<particles.length; i++ )
			{
				particles[i] = createParticle( system );
			}
			
			reset(system);
		}
		
		/**
		 * Rest the list of particles
		 * 
		 * @param system The system in which the particle belong
		 */
		public void reset(ParticleSystem system) {
			available.clear();
			
			for( int i=0; i<particles.length; i++ )
			{
				available.add(particles[i]);
			}
		}
	}
	
	/**
	 * A map from emitter to a the particle pool holding the particles it uses
	 * void: this is now sorted by emitters to allow emitter specfic state to be set for
	 * each emitter. actually this is used to allow setting an individual blend mode for
	 * each emitter
	 */
	protected HashMap particlesByEmitter = new HashMap();
	/** The maximum number of particles allows per emitter */
	protected int maxParticlesPerEmitter;
	
	/** The list of emittered producing and controlling particles */
	protected ArrayList emitters = new ArrayList();
	
	/** The dummy particle to return should no more particles be available */
	protected Particle dummy;
	/** The blending mode */
	private int blendingMode = BLEND_COMBINE;
	/** The number of particles in use */
	private int pCount;
	/** True if we're going to use points to render the particles */
	private boolean usePoints;
	/** The x coordinate at which this system should be rendered */
	private float x;
	/** The x coordinate at which this system should be rendered */
	private float y;
	/** True if we should remove completed emitters */
	private boolean removeCompletedEmitters = true;

	/** The default image for the particles */
	private Image sprite;
	/** True if the particle system is visible */
	private boolean visible = true;
	/** The name of the default image */
	private String defaultImageName;
	/** The mask used to make the particle image background transparent if any */
	private Color mask;
	
	/**
	 * Create a new particle system
	 * 
	 * @param defaultSprite The sprite to render for each particle
	 */
	public ParticleSystem(Image defaultSprite) {
		this(defaultSprite, DEFAULT_PARTICLES);
	}
	
	/**
	 * Create a new particle system
	 * 
	 * @param defaultSpriteRef The sprite to render for each particle
	 */
	public ParticleSystem(String defaultSpriteRef) {
		this(defaultSpriteRef, DEFAULT_PARTICLES);
	}
	
	/**
	 * Reset the state of the system
	 */
	public void reset() {
		Iterator pools = particlesByEmitter.values().iterator();
		while (pools.hasNext()) {
			ParticlePool pool = (ParticlePool) pools.next();
			pool.reset(this);
		}
		
		for (int i=0;i<emitters.size();i++) {
			ParticleEmitter emitter = (ParticleEmitter) emitters.get(i);
			emitter.resetState();
		}
	}
	
	/**
	 * Check if this system is currently visible, i.e. it's actually
	 * rendered
	 * 
	 * @return True if the particle system is rendered
	 */
	public boolean isVisible() {
		return visible;
	}
	
	/**
	 * Indicate whether the particle system should be visible, i.e. whether
	 * it'll actually render
	 * 
	 * @param visible True if the particle system should render
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	/**
	 * Indicate if completed emitters should be removed
	 * 
	 * @param remove True if completed emitters should be removed
	 */
	public void setRemoveCompletedEmitters(boolean remove) {
		removeCompletedEmitters = remove;
	}
	
	/**
	 * Indicate if this engine should use points to render the particles
	 * 
	 * @param usePoints True if points should be used to render the particles
	 */
	public void setUsePoints(boolean usePoints) {
		this.usePoints = usePoints;
	}
	
	/**
	 * Check if this engine should use points to render the particles
	 * 
	 * @return True if the engine should use points to render the particles
	 */
	public boolean usePoints() {
		return usePoints;
	}

	/**
	 * Create a new particle system
	 * 
	 * @param defaultSpriteRef The sprite to render for each particle
	 * @param maxParticles The number of particles available in the system
	 */
	public ParticleSystem(String defaultSpriteRef, int maxParticles) {
		this(defaultSpriteRef, maxParticles, null);
	}
	
	/**
	 * Create a new particle system
	 * 
	 * @param defaultSpriteRef The sprite to render for each particle
	 * @param maxParticles The number of particles available in the system
	 * @param mask The mask used to make the sprite image transparent
	 */
	public ParticleSystem(String defaultSpriteRef, int maxParticles, Color mask) {
		this.maxParticlesPerEmitter= maxParticles;
		this.mask = mask;
		
		setDefaultImageName(defaultSpriteRef);
		dummy = createParticle(this);
	}

	/**
	 * Create a new particle system
	 * 
	 * @param defaultSprite The sprite to render for each particle
	 * @param maxParticles The number of particles available in the system
	 */
	public ParticleSystem(Image defaultSprite, int maxParticles) {
		this.maxParticlesPerEmitter= maxParticles;
	
		sprite = defaultSprite;
		dummy = createParticle(this);
	}
	
	/**
	 * Set the default image name 
	 * 
	 * @param ref The default image name
	 */
	public void setDefaultImageName(String ref) {
		defaultImageName = ref;
		sprite = null;
	}
	
	/**
	 * Get the blending mode in use
	 * 
	 * @see #BLEND_COMBINE
	 * @see #BLEND_ADDITIVE
	 * @return The blending mode in use
	 */
	public int getBlendingMode() {
		return blendingMode;
	}
	
	/**
	 * Create a particle specific to this system, override for your own implementations. 
	 * These particles will be cached and reused within this system.
	 * 
	 * @param system The system owning this particle
	 * @return The newly created particle.
	 */
	protected Particle createParticle(ParticleSystem system) {
		return new Particle(system);
	}
	
	/**
	 * Set the blending mode for the particles
	 * 
	 * @param mode The mode for blending particles together
	 */
	public void setBlendingMode(int mode) {
		this.blendingMode = mode;
	}
	
	/**
	 * Get the number of emitters applied to the system
	 * 
	 * @return The number of emitters applied to the system
	 */
	public int getEmitterCount() {
		return emitters.size();
	}
	
	/**
	 * Get an emitter a specified index int he list contained within this system
	 * 
	 * @param index The index of the emitter to retrieve
	 * @return The particle emitter 
	 */
	public ParticleEmitter getEmitter(int index) {
		return (ParticleEmitter) emitters.get(index);
	}
	
	/**
	 * Add a particle emitter to be used on this system
	 * 
	 * @param emitter The emitter to be added
	 */
	public void addEmitter(ParticleEmitter emitter) {
		emitters.add(emitter);
		
		ParticlePool pool= new ParticlePool( this, maxParticlesPerEmitter );
		particlesByEmitter.put( emitter, pool );
	}
	
	/**
	 * Remove a particle emitter that is currently used in the system
	 * 
	 * @param emitter The emitter to be removed
	 */
	public void removeEmitter(ParticleEmitter emitter) {
		emitters.remove(emitter);
		particlesByEmitter.remove(emitter);
	}
	
	/**
	 * Remove all the emitters from the system
	 */
	public void removeAllEmitters() {
		for (int i=0;i<emitters.size();i++) {
			removeEmitter((ParticleEmitter) emitters.get(i));
			i--;
		}
	}
	
	/**
	 * Get the x coordiante of the position of the system
	 * 
	 * @return The x coordinate of the position of the system
	 */
	public float getPositionX() {
		return x;
	}
	
	/**
	 * Get the y coordiante of the position of the system
	 * 
	 * @return The y coordinate of the position of the system
	 */
	public float getPositionY() {
		return y;
	}
	
	/**
	 * Set the position at which this system should render relative to the current
	 * graphics context setup
	 * 
	 * @param x The x coordinate at which this system should be centered
 	 * @param y The y coordinate at which this system should be centered
	 */
	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Render the particles in the system
	 */
	public void render() {
		render(x,y);
	}
	
	/**
	 * Render the particles in the system
	 * 
	 * @param x The x coordinate to render the particle system at (in the current coordinate space)
	 * @param y The y coordinate to render the particle system at (in the current coordiante space)
	 */
	public void render(float x, float y) {
		if ((sprite == null) && (defaultImageName != null)) {
			loadSystemParticleImage();
		}
		
		if (!visible) {
			return;
		}
		
		GL.glTranslatef(x,y,0);
		
		if (blendingMode == BLEND_ADDITIVE) {
			GL.glBlendFunc(SGL.GL_SRC_ALPHA, SGL.GL_ONE);
		}
		if (usePoints()) {
			GL.glEnable( SGL.GL_POINT_SMOOTH ); 
			TextureImpl.bindNone();
		}
		
		// iterate over all emitters
		for( int emitterIdx=0; emitterIdx<emitters.size(); emitterIdx++ )
		{
			// get emitter
			ParticleEmitter emitter = (ParticleEmitter) emitters.get(emitterIdx);
			
			if (!emitter.isEnabled()) {
				continue;
			}
			
			// check for additive override and enable when set
			if (emitter.useAdditive()) {
				GL.glBlendFunc(SGL.GL_SRC_ALPHA, SGL.GL_ONE);
			}
			
			// now get the particle pool for this emitter and render all particles that are in use
			ParticlePool pool = (ParticlePool) particlesByEmitter.get(emitter);
			Image image = emitter.getImage();
			if (image == null) {
				image = this.sprite;
			}
			
			if (!emitter.isOriented() && !emitter.usePoints(this)) {
				image.startUse();
			}
			
			for (int i = 0; i < pool.particles.length; i++)
			{
				if (pool.particles[i].inUse())
					pool.particles[i].render();
			} 
			
			if (!emitter.isOriented() && !emitter.usePoints(this)) {
				image.endUse();
			}

			// reset additive blend mode
			if (emitter.useAdditive()) {
				GL.glBlendFunc(SGL.GL_SRC_ALPHA, SGL.GL_ONE_MINUS_SRC_ALPHA);
			}
		}

		if (usePoints()) {
			GL.glDisable( SGL.GL_POINT_SMOOTH ); 
		}
		if (blendingMode == BLEND_ADDITIVE) {
			GL.glBlendFunc(SGL.GL_SRC_ALPHA, SGL.GL_ONE_MINUS_SRC_ALPHA);
		}
		
		Color.white.bind();
		GL.glTranslatef(-x,-y,0);
	}
	
	/**
	 * Load the system particle image as the extension permissions
	 */
	private void loadSystemParticleImage() {
		AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
        		try {
        			if (mask != null) {
        				sprite = new Image(defaultImageName, mask);
        			} else {
        				sprite = new Image(defaultImageName);
        			}
        		} catch (SlickException e) {
        			Log.error(e);
        			defaultImageName = null;
        		}
                return null; // nothing to return
            }
        });
	}
	
	/**
	 * Update the system, request the assigned emitters update the particles
	 * 
	 * @param delta The amount of time thats passed since last update in milliseconds
	 */
	public void update(int delta) {
		if ((sprite == null) && (defaultImageName != null)) {
			loadSystemParticleImage();
		}
		
		removeMe.clear();
		ArrayList emitters = new ArrayList(this.emitters);
		for (int i=0;i<emitters.size();i++) {
			ParticleEmitter emitter = (ParticleEmitter) emitters.get(i);
			if (emitter.isEnabled()) {
				emitter.update(this, delta);
				if (removeCompletedEmitters) {
					if (emitter.completed()) {
						removeMe.add(emitter);
						particlesByEmitter.remove(emitter);
					}
				}
			}
		}
		this.emitters.removeAll(removeMe);
		
		pCount = 0;
		
		if (!particlesByEmitter.isEmpty())
		{
			Iterator it= particlesByEmitter.keySet().iterator();
			while (it.hasNext())
			{
				ParticleEmitter emitter = (ParticleEmitter) it.next();
				if (emitter.isEnabled()) {
					ParticlePool pool = (ParticlePool) particlesByEmitter.get(emitter);
					for (int i=0;i<pool.particles.length;i++) {
						if (pool.particles[i].life > 0) {
							pool.particles[i].update(delta);
							pCount++;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Get the number of particles in use in this system
	 * 
	 * @return The number of particles in use in this system
	 */
	public int getParticleCount() {
		return pCount;
	}
	
	/**
	 * Get a new particle from the system. This should be used by emitters to 
	 * request particles
	 * 
	 * @param emitter The emitter requesting the particle
	 * @param life The time the new particle should live for
	 * @return A particle from the system
	 */
	public Particle getNewParticle(ParticleEmitter emitter, float life)
	{
		ParticlePool pool = (ParticlePool) particlesByEmitter.get(emitter);
		ArrayList available = pool.available;
		if (available.size() > 0)
		{
			Particle p = (Particle) available.remove(available.size()-1);
			p.init(emitter, life);
			p.setImage(sprite);
			
			return p;
		}
		
		Log.warn("Ran out of particles (increase the limit)!");
		return dummy;
	}
	
	/**
	 * Release a particle back to the system once it has expired
	 * 
	 * @param particle The particle to be released
	 */
	public void release(Particle particle) {
		if (particle != dummy)
		{
			ParticlePool pool = (ParticlePool)particlesByEmitter.get( particle.getEmitter() );
			pool.available.add(particle);
		}
	}
	
	/**
	 * Release all the particles owned by the specified emitter
	 * 
	 * @param emitter The emitter owning the particles that should be released
	 */
	public void releaseAll(ParticleEmitter emitter) {
		if( !particlesByEmitter.isEmpty() )
		{
			Iterator it= particlesByEmitter.values().iterator();
			while( it.hasNext())
			{
				ParticlePool pool= (ParticlePool)it.next();
				for (int i=0;i<pool.particles.length;i++) {
					if (pool.particles[i].inUse()) {
						if (pool.particles[i].getEmitter() == emitter) {
							pool.particles[i].setLife(-1);
							release(pool.particles[i]);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Move all the particles owned by the specified emitter
	 * 
	 * @param emitter The emitter owning the particles that should be released
	 * @param x The amount on the x axis to move the particles
	 * @param y The amount on the y axis to move the particles
	 */
	public void moveAll(ParticleEmitter emitter, float x, float y) {
		ParticlePool pool = (ParticlePool) particlesByEmitter.get(emitter);
		for (int i=0;i<pool.particles.length;i++) {
			if (pool.particles[i].inUse()) {
				pool.particles[i].move(x, y);
			}
		}
	}
	
	/**
	 * Create a duplicate of this system. This would have been nicer as a different interface
	 * but may cause to much API change headache. Maybe next full version release it should be
	 * rethought.
	 * 
	 * TODO: Consider refactor at next point release
	 * 
	 * @return A copy of this particle system
	 * @throws SlickException Indicates a failure during copy or a invalid particle system to be duplicated
	 */
	public ParticleSystem duplicate() throws SlickException {
		for (int i=0;i<emitters.size();i++) {
			if (!(emitters.get(i) instanceof ConfigurableEmitter)) {
				throw new SlickException("Only systems contianing configurable emitters can be duplicated");
			}
		}
	
		ParticleSystem theCopy = null;
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ParticleIO.saveConfiguredSystem(bout, this);
			ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
			theCopy = ParticleIO.loadConfiguredSystem(bin);
		} catch (IOException e) {
			Log.error("Failed to duplicate particle system");
			throw new SlickException("Unable to duplicated particle system", e);
		}
		
		return theCopy;
	}
}
