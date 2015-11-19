package org.newdawn.slick.state.transition;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.state.GameState;
import org.newdawn.slick.state.StateBasedGame;

/**
 * A transition to fade out to a given colour after a delay.
 *
 * @author kevin (base)
 */
public class DelayedFadeOutTransition implements Transition {
	/** The color to fade to */
	private final Color color;
	/** The time it takes the fade to happen */
	private final int fadeTime;
	/** The time it takes before the fade starts */
	private final int delay;
	/** The elapsed time */
	private int elapsedTime = 0;

	/**
	 * Create a new delayed fade out transition.
	 */
	public DelayedFadeOutTransition() { this(Color.black, 500, 0); }

	/**
	 * Create a new delayed fade out transition.
	 * 
	 * @param color The color we're going to fade out to
	 */
	public DelayedFadeOutTransition(Color color) { this(color, 500, 0); }

	/**
	 * Create a new delayed fade out transition
	 * 
	 * @param color The color we're going to fade out to
	 * @param fadeTime The time it takes the fade to occur
	 * @param delay The time before the fade starts (must be less than {@code fadeTime})
	 */
	public DelayedFadeOutTransition(Color color, int fadeTime, int delay) {
		if (delay > fadeTime)
			throw new IllegalArgumentException();

		this.color = new Color(color);
		this.color.a = 0;
		this.fadeTime = fadeTime;
		this.delay = delay;
	}

	@Override
	public boolean isComplete() { return (color.a >= 1); }

	@Override
	public void postRender(StateBasedGame game, GameContainer container, Graphics g) {
		Color old = g.getColor();
		g.setColor(color);
		g.fillRect(0, 0, container.getWidth() * 2, container.getHeight() * 2);
		g.setColor(old);
	}

	@Override
	public void update(StateBasedGame game, GameContainer container, int delta) {
		if (elapsedTime < delay) {
			elapsedTime += delta;
			return;
		}

		color.a += delta * (1.0f / (fadeTime - delay));
		if (color.a > 1)
			color.a = 1;
	}

	@Override
	public void preRender(StateBasedGame game, GameContainer container, Graphics g) {}

	@Override
	public void init(GameState firstState, GameState secondState) {}
}
