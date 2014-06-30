package org.newdawn.slick.state.transition;

import java.util.ArrayList;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.GameState;
import org.newdawn.slick.state.StateBasedGame;

/**
 * A transition thats built of a set of other transitions which are chained
 * together to build the overall effect.
 *
 * @author kevin
 */
public class CombinedTransition implements Transition {
	/** The list of transitions to be combined */
    private ArrayList transitions = new ArrayList();

    /**
     * Create an empty transition
     */
    public CombinedTransition() {
    }

    /**
     * Add a transition to the list that will be combined to form
     * the final transition
     * 
     * @param t The transition to add
     */
    public void addTransition(Transition t) {
          transitions.add(t);
    }

	/**
	 * @see org.newdawn.slick.state.transition.Transition#isComplete()
	 */
	public boolean isComplete() {
        for (int i=0;i<transitions.size();i++) {
            if (!((Transition) transitions.get(i)).isComplete()) {
            	return false;
            }
        }
        
        return true;
	}

	/**
	 * @see org.newdawn.slick.state.transition.Transition#postRender(org.newdawn.slick.state.StateBasedGame, org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void postRender(StateBasedGame game, GameContainer container, Graphics g) throws SlickException {
        for (int i=transitions.size()-1;i>=0;i--) {
            ((Transition) transitions.get(i)).postRender(game, container, g);
        }
	}

	/**
	 * @see org.newdawn.slick.state.transition.Transition#preRender(org.newdawn.slick.state.StateBasedGame, org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void preRender(StateBasedGame game, GameContainer container, Graphics g) throws SlickException {
        for (int i=0;i<transitions.size();i++) {
            ((Transition) transitions.get(i)).postRender(game, container, g);
        }
	}

	/**
	 * @see org.newdawn.slick.state.transition.Transition#update(org.newdawn.slick.state.StateBasedGame, org.newdawn.slick.GameContainer, int)
	 */
	public void update(StateBasedGame game, GameContainer container, int delta) throws SlickException {
        for (int i=0;i<transitions.size();i++) {
        	Transition t = (Transition) transitions.get(i);
        	
        	if (!t.isComplete()) {
        		t.update(game, container, delta);
        	}
        }
	}

	public void init(GameState firstState, GameState secondState) {
	   for (int i = transitions.size() - 1; i >= 0; i--) {
	      ((Transition)transitions.get(i)).init(firstState, secondState);
	   }
	}
}
