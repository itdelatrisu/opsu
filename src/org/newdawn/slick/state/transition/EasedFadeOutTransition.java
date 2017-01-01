/*
 * Copyright (c) 2013, Slick2D
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Slick2D nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.newdawn.slick.state.transition;

import itdelatrisu.opsu.ui.animations.AnimationEquation;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.state.GameState;
import org.newdawn.slick.state.StateBasedGame;

/**
 * A transition to fade out to a given colour using an easing function.
 *
 * @author kevin (base)
 */
public class EasedFadeOutTransition implements Transition {
	/** The color to fade to */
	private final Color color;
	/** The time it takes the fade to happen */
	private final int fadeTime;
	/** The easing function */
	private final AnimationEquation eq;
	/** The transition progress */
	private float t = 0f;

	/**
	 * Create a new eased fade out transition.
	 */
	public EasedFadeOutTransition() { this(Color.black, 500, AnimationEquation.OUT_QUART); }

	/**
	 * Create a new eased fade out transition.
	 *
	 * @param color The color we're going to fade out to
	 */
	public EasedFadeOutTransition(Color color) { this(color, 500, AnimationEquation.OUT_QUART); }

	/**
	 * Create a new eased fade out transition.
	 *
	 * @param color The color we're going to fade out to
	 * @param fadeTime The time it takes the fade to occur
	 */
	public EasedFadeOutTransition(Color color, int fadeTime) { this(color, fadeTime, AnimationEquation.OUT_QUART); }

	/**
	 * Create a new eased fade out transition.
	 *
	 * @param color The color we're going to fade out to
	 * @param fadeTime The time it takes the fade to occur
	 * @param eq The easing function to use
	 */
	public EasedFadeOutTransition(Color color, int fadeTime, AnimationEquation eq) {
		this.color = new Color(color);
		this.color.a = 0;
		this.fadeTime = fadeTime;
		this.eq = eq;
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
		t += delta * (1.0f / fadeTime);
		float alpha = t > 1f ? 1f : eq.calc(t);
		color.a = alpha;
	}

	@Override
	public void preRender(StateBasedGame game, GameContainer container, Graphics g) {}

	@Override
	public void init(GameState firstState, GameState secondState) {}
}
