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

package fluddokt.newdawn.slick.state.transition;

/*
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.GameState;
import org.newdawn.slick.state.StateBasedGame;
*/
import fluddokt.opsu.fake.*;

/**
 * A transition between two game states
 *
 * @author kevin
 */
public interface Transition {
	/** 
	 * Update the transition. Cause what ever happens in the transition to happen
	 * 
	 * @param game The game this transition is being rendered as part of
	 * @param container The container holding the game
	 * @param delta The amount of time passed since last update
	 * @throws SlickException Indicates a failure occured during the update 
	 */
	public void update(StateBasedGame game, GameContainer container, int delta) throws SlickException;

	/**
	 * Render the transition before the existing state rendering
	 * 
	 * @param game The game this transition is being rendered as part of
	 * @param container The container holding the game
	 * @param g The graphics context to use when rendering the transiton
	 * @throws SlickException Indicates a failure occured during the render 
	 */
	public void preRender(StateBasedGame game, GameContainer container, Graphics g) throws SlickException;
	
	/**
	 * Render the transition over the existing state rendering
	 * 
	 * @param game The game this transition is being rendered as part of
	 * @param container The container holding the game
	 * @param g The graphics context to use when rendering the transiton
	 * @throws SlickException Indicates a failure occured during the render 
	 */
	public void postRender(StateBasedGame game, GameContainer container, Graphics g) throws SlickException;
	
	/**
	 * Check if this transtion has been completed
	 * 
	 * @return True if the transition has been completed
	 */
	public boolean isComplete();
	
	/**
	 * Initialise the transition
	 * 
	 * @param firstState The first state we're rendering (this will be rendered by the framework)
	 * @param secondState The second stat we're transitioning to or from (this one won't be rendered)
	 */
	public void init(GameState firstState, GameState secondState);
}
