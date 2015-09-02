package fluddokt.opsu.fake;

import java.util.HashMap;
import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.OrderedSet;
import com.badlogic.gdx.utils.TimeUtils;

public abstract class StateBasedGame extends Game2 implements InputProcessor {

	public GameContainer gc;
	BasicGameState currentState = new BasicGameState() {
	};
	BasicGameState nextState = null;
	HashMap<Integer, BasicGameState> bgs = new HashMap<Integer, BasicGameState>();
	LinkedList<BasicGameState> orderedbgs = new LinkedList<BasicGameState>();
	String title;
	OrderedSet<InputListener> keyListeners = new OrderedSet<InputListener>();
	boolean rightIsPressed;
	int touchX = 0;
	int touchY = 0;
	long touchTime;
	
	public StateBasedGame(String name) {
		this.title = name;
		Display.setTitle(name);
		// gc.width = Gdx.graphics.getWidth();
		// gc.height = Gdx.graphics.getHeight();
	}

	public BasicGameState getState(int state) {
		return bgs.get(state);
	}

	public void enterState(int newState) {
		enterState(newState, null, null);

	}

	public void enterState(int newState, STransition STransition,
			FadeInTransition fadeInTransition) {
		System.out.println("Enter State Transition " + newState);
		nextState = bgs.get(newState);
	}

	private boolean enterNextState() throws SlickException {
		if (nextState != null) {
			if (gc == null) {
				throw new Error("");
			}
			currentState.leave(gc, this);
			currentState = nextState;
			nextState = null;
			touchX = 0;
			touchY = 0;
			
			if (!currentState.inited) {
				currentState.init(gc, this);
				currentState.inited = true;
			}
			currentState.enter(gc, this);
			return true;
		}
		return false;
	}

	public int getCurrentStateID() {
		return currentState.getID();
	}

	public String getTitle() {
		return title;
	}

	public void addState(BasicGameState gs) throws SlickException {
		bgs.put(gs.getID(), gs);
		orderedbgs.add(gs);
		if (gs.getID() == 0)
			enterState(0);
		// gs.init(gc, this);
	}

	int lastEnteredState = 0;
	public void render() throws SlickException {
		int deltaTime = (int) (Gdx.graphics.getDeltaTime() * 1000);
		
		if(lastEnteredState > 0){
			if(deltaTime > 32) {
				deltaTime = 0;
				lastEnteredState--;
			}
			else
				lastEnteredState = 0;
		}
			
		if(enterNextState())
			lastEnteredState = 20;
		
		if (currentState != null) {
			currentState.update(gc, this, deltaTime);
			currentState.render(gc, this, Graphics.getGraphics());
			/*
			if(Graphics.curFont!=null){
				if(GameContainer.music!=null)
					Graphics.getGraphics().drawString(""
					+" "+(GameContainer.music.lastTime&0xffff)
					+" "+pad(""+(int)((GameContainer.music.music.getPosition()-GameContainer.music.lastGetPos)*1000))
					+" avg:"+pad(""+(int)(GameContainer.music.avgDiff*1000)/1000f)
					//+" "+GameContainer.music.music.getPosition()
					//+" "+GameContainer.music.lastGPos
					, 10, 100);
			}
			*/
		}
	}

	private String pad(String s) {
		while(s.length()<6){
			s=' '+s;
		}
		return s;
	}

	public void init() throws SlickException {
		initStatesList(gc);
		for (BasicGameState tgs : orderedbgs) {
			if (!tgs.inited) {
				tgs.init(gc, this);
				tgs.inited = true;
			}
		}
	}

	@Override
	public boolean keyDown(int keycode) {
		// System.out.println("Key:"+keycode);
		for (InputListener keylis : keyListeners) {
			keylis.keyDown(keycode);
		}
		currentState.keyPressed(keycode, (char)0);
				//com.badlogic.gdx.Input.Keys.toString(keycode).charAt(0));

		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		for (InputListener keylis : keyListeners) {
			keylis.keyUp(keycode);
		}
		currentState.keyReleased(keycode, (char)0);
				//com.badlogic.gdx.Input.Keys.toString(keycode).charAt(0));

		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		for (InputListener keylis : keyListeners) {
			keylis.keyType(character);
		}
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		try {
			for (InputListener keylis : keyListeners) {
				keylis.touchDown(screenX, screenY, pointer, button);
			}
			if (pointer > 0) {
				if(rightIsPressed){
					currentState.mouseReleased(Input.MOUSE_RIGHT_BUTTON, oldx, oldy);
				}
				currentState.mousePressed(Input.MOUSE_RIGHT_BUTTON, oldx, oldy );
				gc.getInput().setMouseRighButtontDown(true);
				rightIsPressed = true;
				touchX = oldx;
				touchY = oldy;
				touchTime = TimeUtils.millis();
			} else {
				currentState.mousePressed(button, screenX, screenY);
				oldx = screenX;
				oldy = screenY;
				touchX = screenX;
				touchY = screenY;
				touchTime = TimeUtils.millis();
			}
		} catch (Throwable e) {
			e.printStackTrace();
			GameOpsu.error("touchDown", e);
		}
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		for (InputListener keylis : keyListeners) {
			keylis.touchUp(screenX, screenY, pointer, button);
		}
		if (pointer > 0){
			currentState.mouseReleased(Input.MOUSE_RIGHT_BUTTON, oldx, oldy);
			int dx = oldx - touchX;
			int dy = oldy - touchY;
			if( TimeUtils.timeSinceMillis(touchTime) < 500 && dx*dx + dy*dy < 10 * 10)
				currentState.mouseClicked(Input.MOUSE_RIGHT_BUTTON, oldx, oldy, 1);
			
			gc.getInput().setMouseRighButtontDown(false);
			rightIsPressed = false;
		} else {
			currentState.mouseReleased(button, screenX, screenY);
			int dx = screenX - touchX;
			int dy = screenY - touchY;
			if( TimeUtils.timeSinceMillis(touchTime) < 500 && dx*dx + dy*dy < 10 * 10)
				currentState.mouseClicked(button, screenX, screenY, 1);
			
			oldx = screenX;
			oldy = screenY;
		}
		

		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		for (InputListener keylis : keyListeners) {
			keylis.touchDragged(screenX, screenY, pointer);
		}
		if (pointer == 0) {
			currentState.mouseDragged(oldx, oldy, screenX, screenY);
			oldx = screenX;
			oldy = screenY;
		}
		return false;
	}

	int oldx, oldy;

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

	public boolean closeRequested() {
		return false;
	}

	public abstract void initStatesList(GameContainer container)
			throws SlickException;

	public GameContainer getContainer() {
		return gc;
	}

	public void setContainer(GameContainer gameContainer) {
		gc = gameContainer;
	}

	public void addKeyListener(InputListener listener) {
		keyListeners.add(listener);

	}

	public void removeKeyListener(InputListener listener) {
		keyListeners.remove(listener);
	}
}
