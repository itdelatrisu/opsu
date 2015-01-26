package itdelatrisu.opsu.audio;

import java.io.File;
import java.io.IOException;

import org.newdawn.slick.SlickException;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Glide;
import net.beadsproject.beads.ugens.GranularSamplePlayer;
import net.beadsproject.beads.ugens.SamplePlayer.EnvelopeType;

/**
 * Music player using the Beads Project's GranularSamplePlayer class.
 * Used for playing MP3s.
 * @author https://github.com/LudziE12/josu (originally)
 */
public class BeadsPlayer extends MusicPlayer {
	/** The audio context. */
	private static AudioContext context;
	static {
		context = new AudioContext(2048);
		context.start();
	}

	/** The music player. */
	private GranularSamplePlayer player;

	/** The sound sample. */
	private Sample sample;

	/** Glides. */
	private Glide volumeGlide, pitchGlide, speedGlide;

	/** Whether the track is playing. */
	private boolean playing;

	/** The current track volume. */
	private float volume = 1f;

	/**
	 * Constructor.
	 * @param file the audio file
	 * @throws SlickException failure to load file
	 */
	public BeadsPlayer(File file) throws IOException {
		super(file);

		player = new GranularSamplePlayer(context, sample = new Sample(file.getAbsolutePath(), false));

		player.setRate(speedGlide = new Glide(context, 1.0f, 0));
		player.setPitch(pitchGlide = new Glide(context, 1.0f, 0));
		player.setEnvelopeType(EnvelopeType.FINE);

		Gain gain = new Gain(context, 1, volumeGlide = new Glide(context, 1.0f, 0));			
		gain.addInput(player);
		context.out.addInput(gain);
	}

	@Override
	public void loop() {
		// TODO
		play();
	}

	@Override
	public void play() {
		if (player.isDeleted())
			player = new GranularSamplePlayer(context, sample);
		if (!playing)
			resume();
	}

	@Override
	public void setPosition(int time) {
		play();
		player.setPosition(time);
	}

	@Override
	public int getPosition() { return (int) player.getPosition(); }

	@Override
	public boolean isPlaying() { return playing; }

	@Override
	public void stop() {
		playing = false;
		player.reset();
	}

	@Override
	public void pause() {
		playing = false;
		player.pause(true);
	}

	@Override
	public void resume() {
		setVolume(volume);
		if (!player.isPaused()) {
			if (!playing)
				player.start(0);
		} else
			player.pause(false);
		playing = true;
	}

	@Override
	public void fadeOut(int duration) {
		float vol = volume;
		glideVolume(0f, duration);
		this.volume = vol;
	}

	@Override
	public void setVolume(float volume) {
		volumeGlide.setValueImmediately(volume);
		this.volume = volume;
	}

	@Override
	public void close() { player.kill(); }

	/**
	 * Returns the track length.
	 */
	public int getLength() { return (int) sample.getLength(); }

	/**
	 * Returns whether or not the track is deleted.
	 */
	public boolean hasEnded() { return player.isDeleted(); }
	
	/**
	 * Glides the track volume to a level.
	 */
	public void glideVolume(float volume, float time){
		volumeGlide.setGlideTime(time);
		volumeGlide.setValue(volume);
		this.volume = volume;
	}

	/**
	 * Glides the track speed to a level.
	 */
	public void glideSpeed(float speed, float time){
		speedGlide.setGlideTime(time);
		speedGlide.setValue(speed);
	}

	/**
	 * Sets the track speed.
	 */
	public void setSpeed(float speed) { speedGlide.setValueImmediately(speed); }

	/**
	 * Glides the track pitch to a level.
	 */
	public void glidePitch(float pitch, float time){
		pitchGlide.setGlideTime(time);
		pitchGlide.setValue(pitch);
	}

	/**
	 * Sets the track pitch.
	 */
	public void setPitch(float pitch){ pitchGlide.setValue(pitch); }
}
