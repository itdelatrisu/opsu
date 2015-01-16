package itdelatrisu.opsu.fake;

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;

import itdelatrisu.opsu.states.SongMenu;

public class StateBasedGame implements InputProcessor{

	public GameContainer gc = new GameContainer();
	BasicGameState currentState = new BasicGameState(){};
	BasicGameState nextState = null;
	HashMap<Integer,BasicGameState> bgs = new HashMap<Integer,BasicGameState>();
	
	public StateBasedGame(){
		gc.width = Gdx.graphics.getWidth();
		gc.height = Gdx.graphics.getHeight();
		
	}
	public BasicGameState getState(int stateSongmenu) {
		return bgs.get(stateSongmenu);
	}

	public void enterState(int newState) {
		enterState(newState,null,null);
		
	}

	public void enterState(int newState, STransition STransition,
			FadeInTransition fadeInTransition) {
		System.out.println("Enter State Transition "+newState);
		nextState = bgs.get(newState);
	}
	private void enterNextState(){
		if(nextState!=null){
			currentState = nextState;
			nextState = null;
			//currentState.init(gc, this);
			if(!currentState.inited){
				currentState.init(gc, this);
				currentState.inited=true;
			}
			currentState.enter(gc, this);
			
		}
	}

	public int getCurrentStateID() {
		return currentState.getID();
	}

	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	public void addState(BasicGameState gs) {
		// TODO Auto-generated method stub
		bgs.put(gs.getID(),gs);
		if(gs.getID()==0)
			currentState=gs;
		gs.init(gc, this);
	}
	public void render() throws SlickException{
		enterNextState();
		if(currentState != null){
			currentState.update(gc, this, 1000/60);
			currentState.render(gc, this, Graphics.getGraphics());
		}
	}

	public void init() throws SlickException {
		if(currentState != null){
			currentState.init(gc, this);
		}
	}
	@Override
	public boolean keyDown(int keycode) {
		System.out.println("Key:"+keycode);
		currentState.keyPressed(keycode, com.badlogic.gdx.Input.Keys.toString(keycode).charAt(0));
		
		return false;
	}
	@Override
	public boolean keyUp(int keycode) {
		currentState.keyReleased(keycode, com.badlogic.gdx.Input.Keys.toString(keycode).charAt(0));
		
		return false;
	}
	@Override
	public boolean keyTyped(char character) {
		return false;
	}
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		currentState.mousePressed(button, screenX,screenY);
		return false;
	}
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		currentState.mouseReleased(button, screenX, screenY);
		return false;
	}
	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		currentState.mouseDragged(oldx, oldy, screenX, screenY);
		oldx = screenX;
		oldy = screenY;
		return false;
	}
	int oldx,oldy;
	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		currentState.mouseMoved(oldx, oldy, screenX, screenX);
		oldx = screenX;
		oldy = screenY;
		return false;
	}
	@Override
	public boolean scrolled(int amount) {
		currentState.mouseWheelMoved(-amount);
		return false;
	}

}
