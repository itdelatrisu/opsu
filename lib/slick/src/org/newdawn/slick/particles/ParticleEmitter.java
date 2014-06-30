package org.newdawn.slick.particles;

import org.newdawn.slick.Image;

/**
 * An emitter is responsible for producing the particles and controlling them during
 * their life. An implementation of this interface can be considered a particle
 * effect.
 *
 * @author kevin
 */
public interface ParticleEmitter {
	/**
	 * Update the emitter, produce any particles required by requesting
	 * them from the particle system provided.
	 * 
	 * @param system The particle system used to create particles
	 * @param delta The amount of time in milliseconds since last emitter update
	 */
	public void update(ParticleSystem system, int delta);

	/**
	 * Check if this emitter has completed it's cycle
	 * 
	 * @return True if the emitter has completed it's cycle
	 */
	public boolean completed();
	
	/**
	 * Wrap up the particle emitter. This means the emitter will no longer produce
	 * particles and will be marked as completed once the particles have expired
	 */
	public void wrapUp();
	
	/**
	 * Update a single particle that this emitter produced
	 * 
	 * @param particle The particle to be updated
	 * @param delta The amount of time in millisecond since last particle update
	 */
	public void updateParticle(Particle particle, int delta);
	
	/**
	 * Check if the emitter is enabled 
	 * 
	 * @return True if the emitter is enabled
	 */
	public boolean isEnabled();
	
	/**
	 * Indicate whether the emitter should be enabled
	 * 
	 * @param enabled True if the emitter should be enabled
	 */
	public void setEnabled(boolean enabled);
	
	/**
	 * Check if this emitter should use additive blending
	 * 
	 * @return True if the emitter should use the right blending
	 */
	public boolean useAdditive();
	
	/**
	 * Get the image to draw for each particle
	 * 
	 * @return The image to draw for each particle
	 */
	public Image getImage();

	/**
	 * Check if the particles produced should maintain orientation
	 * 
	 * @return True if the particles produced should maintain orientation
	 */
	public boolean isOriented();
	
	/**
	 * Check if this emitter should use points based on it's own settings 
	 * and those of the particle system
	 * 
	 * @param system The particle system to cross check agianst
	 * @return True if we should use points
	 */
	public boolean usePoints(ParticleSystem system);
	
	/**
	 * Clear the state of emitter back to default
	 */
	public void resetState();
}
