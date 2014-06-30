package org.newdawn.slick.state;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

/**
 * A simple state used an adapter so we don't have to implement all the event methods
 * every time.
 *
 * @author kevin
 */
public abstract class BasicGameState implements GameState {
	/**
	 * @see org.newdawn.slick.ControlledInputReciever#inputStarted()
	 */
	public void inputStarted() {
		
	}
	
	/**
	 * @see org.newdawn.slick.InputListener#isAcceptingInput()
	 */
	public boolean isAcceptingInput() {
		return true;
	}
	
	/**
	 * @see org.newdawn.slick.InputListener#setInput(org.newdawn.slick.Input)
	 */
	public void setInput(Input input) {
	}
	
	/**
	 * @see org.newdawn.slick.InputListener#inputEnded()
	 */
	public void inputEnded() {
	}
	
	/**
	 * @see org.newdawn.slick.state.GameState#getID()
	 */
	public abstract int getID();

	/**
	 * @see org.newdawn.slick.InputListener#controllerButtonPressed(int, int)
	 */
	public void controllerButtonPressed(int controller, int button) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#controllerButtonReleased(int, int)
	 */
	public void controllerButtonReleased(int controller, int button) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#controllerDownPressed(int)
	 */
	public void controllerDownPressed(int controller) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#controllerDownReleased(int)
	 */
	public void controllerDownReleased(int controller) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#controllerLeftPressed(int)
	 */
	public void controllerLeftPressed(int controller) {
		
	}

	/**
	 * @see org.newdawn.slick.InputListener#controllerLeftReleased(int)
	 */
	public void controllerLeftReleased(int controller) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#controllerRightPressed(int)
	 */
	public void controllerRightPressed(int controller) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#controllerRightReleased(int)
	 */
	public void controllerRightReleased(int controller) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#controllerUpPressed(int)
	 */
	public void controllerUpPressed(int controller) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#controllerUpReleased(int)
	 */
	public void controllerUpReleased(int controller) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#keyReleased(int, char)
	 */
	public void keyReleased(int key, char c) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#mouseMoved(int, int, int, int)
	 */
	public void mouseMoved(int oldx, int oldy, int newx, int newy) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#mouseDragged(int, int, int, int)
	 */
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#mouseClicked(int, int, int, int)
	 */
	public void mouseClicked(int button, int x, int y, int clickCount) {
	}
	
	/**
	 * @see org.newdawn.slick.InputListener#mousePressed(int, int, int)
	 */
	public void mousePressed(int button, int x, int y) {
	}

	/**
	 * @see org.newdawn.slick.InputListener#mouseReleased(int, int, int)
	 */
	public void mouseReleased(int button, int x, int y) {
	}

	/**
	 * @see org.newdawn.slick.state.GameState#enter(org.newdawn.slick.GameContainer, org.newdawn.slick.state.StateBasedGame)
	 */
	public void enter(GameContainer container, StateBasedGame game) throws SlickException {
	}

	/**
	 * @see org.newdawn.slick.state.GameState#leave(org.newdawn.slick.GameContainer, org.newdawn.slick.state.StateBasedGame)
	 */
	public void leave(GameContainer container, StateBasedGame game) throws SlickException {
	}

	/**
	 * @see org.newdawn.slick.InputListener#mouseWheelMoved(int)
	 */
	public void mouseWheelMoved(int newValue) {
	}

}
