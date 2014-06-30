package org.newdawn.slick.openal;

/** 
 * The description of of audio data loaded by the AudioLoader
 * 
 * @author kevin
 * @author Nathan Sweet <misc@n4te.com>
 */
public interface Audio {

	/**
	 * Stop the sound effect
	 */
	public void stop();

	/**
	 * Get the ID of the OpenAL buffer holding this data (if any). This method
	 * is not valid with streaming resources.
	 * 
	 * @return The ID of the OpenAL buffer holding this data 
	 */
	public int getBufferID();
	
	/**
	 * Check if the sound is playing as sound fx
	 * 
	 * @return True if the sound is playing
	 */
	public boolean isPlaying();

	/**
	 * Play this sound as a sound effect
	 * 
	 * @param pitch The pitch of the play back
	 * @param gain The gain of the play back
	 * @param loop True if we should loop
	 * @return The ID of the source playing the sound
	 */
	public int playAsSoundEffect(float pitch, float gain, boolean loop);

	/**
	 * Play this sound as a sound effect
	 * 
	 * @param pitch The pitch of the play back
	 * @param gain The gain of the play back
	 * @param loop True if we should loop
	 * @param x The x position of the sound
	 * @param y The y position of the sound
	 * @param z The z position of the sound
	 * @return The ID of the source playing the sound
	 */
	public int playAsSoundEffect(float pitch, float gain, boolean loop,
			float x, float y, float z);

	/**
	 * Play this sound as music
	 * 
	 * @param pitch The pitch of the play back
	 * @param gain The gain of the play back
	 * @param loop True if we should loop
	 * @return The ID of the source playing the sound
	 */
	public int playAsMusic(float pitch, float gain, boolean loop);
	
	/**
	 * Seeks to a position in the music.
	 * 
	 * @param position Position in seconds.
	 * @return True if the setting of the position was successful
	 */
	public boolean setPosition(float position);

	/**
	 * Return the current playing position in the sound
	 * 
	 * @return The current position in seconds.
	 */
	public float getPosition();
}
