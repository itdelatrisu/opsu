package fluddokt.opsu.fake;

import java.util.HashMap;
import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.TimeUtils;

import fluddokt.newdawn.slick.state.transition.Transition;
import fluddokt.opsu.fake.gui.GInputListener;

public abstract class StateBasedGame extends Game2 implements InputProcessor {

	public GameContainer gc;
	final static BasicGameState EMPTY_STATE = new BasicGameState(){};
	BasicGameState currentState = EMPTY_STATE;
	BasicGameState nextState = null;
	BasicGameState oldState = null;
	HashMap<Integer, BasicGameState> bgs = new HashMap<Integer, BasicGameState>();
	LinkedList<BasicGameState> orderedbgs = new LinkedList<BasicGameState>();
	String title;
	LinkedList<GInputListener> inputListener = new LinkedList<GInputListener>();
	boolean rightIsPressed;
	int touchX = 0;
	int touchY = 0;
	long touchTime;
	
	Transition enterT, leaveT;
	
	public StateBasedGame(String name) {
		this.title = name;
		Display.setTitle(name);
	}

	public BasicGameState getState(int state) {
		return bgs.get(state);
	}

	public void enterState(int newState) {
		enterState(newState, null, null);

	}

	public void enterState(int newState, Transition leaveT, Transition enterT) {
		this.enterT = enterT;
		this.leaveT = leaveT;
		oldState = currentState;
		currentState = EMPTY_STATE;
		nextState = bgs.get(newState);
		
	}

	private boolean enterNextState() throws SlickException {
		if (nextState != null) {
			if (gc == null) {
				throw new Error("");
			}
			oldState.leave(gc, this);
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
				lastEnteredState--;
			}
			else
				lastEnteredState = 0;
		}
		
		if (leaveT == null)
			enterNextState();
		{
			if (leaveT != null) {
				if (leaveT.isComplete()) {
					leaveT = null;
				} else {
					leaveT.update(this, gc, deltaTime);
					//oldState.update(gc, this, deltaTime);
				}
			} else{
				if (enterT != null) {
					if (enterT.isComplete()) {
						enterT = null;
					} else {
						enterT.update(this, gc, deltaTime);
					}
				}
				if (currentState != null && lastEnteredState == 0)
					currentState.update(gc, this, deltaTime);
			}
			Graphics g = Graphics.getGraphics();
			if (leaveT != null) {
				leaveT.preRender(this, gc, g);
				oldState.render(gc, this, g);
				leaveT.postRender(this, gc, g);
			} else if (enterT != null) {
				enterT.preRender(this, gc, g);
				currentState.render(gc, this, g);
				enterT.postRender(this, gc, g);
			} else {
				currentState.render(gc, this, g);
			}
		}
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
		for (GInputListener keylis : inputListener) {
			keylis.consumeEvent = false;
			keylis.keyPressed(keycode, (char)0);
			if (keylis.consumeEvent)
				return true;
		}
		currentState.keyPressed(keycode, (char)0);
				//com.badlogic.gdx.Input.Keys.toString(keycode).charAt(0));

		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		for (GInputListener keylis : inputListener) {
			keylis.consumeEvent = false;
			keylis.keyReleased(keycode, (char)0);
			if (keylis.consumeEvent)
				return true;
		}
		currentState.keyReleased(keycode, (char)0);
				//com.badlogic.gdx.Input.Keys.toString(keycode).charAt(0));

		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		for (GInputListener keylis : inputListener) {
			keylis.consumeEvent = false;
			keylis.keyType(character);
			if (keylis.consumeEvent)
				return true;
		}
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		try {
			if (pointer > 0) {
				if(rightIsPressed){
					mouseReleased(Input.MOUSE_RIGHT_BUTTON, oldx, oldy);
				}
				mousePressed(Input.MOUSE_RIGHT_BUTTON, oldx, oldy );
				gc.getInput().setMouseRighButtontDown(true);
				rightIsPressed = true;
				touchX = oldx;
				touchY = oldy;
				touchTime = TimeUtils.millis();
			} else {
				mousePressed(button, screenX, screenY);
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
	private void mousePressed(int button, int x, int y) {
		Input.x = x;
		Input.y = y;
		for (GInputListener keylis : inputListener) {
			keylis.consumeEvent = false;
			keylis.mousePressed(button, x, y);
			if (keylis.consumeEvent)
				return;
		}
		currentState.mousePressed(button, x, y);
	}
	private void mouseReleased(int button, int x, int y) {
		for (GInputListener keylis : inputListener) {
			keylis.consumeEvent = false;
			keylis.mouseReleased(button, x, y);
			if (keylis.consumeEvent)
				return;
		}
		currentState.mouseReleased(button, x, y);
		
	}
	private void mouseClicked(int button, int x, int y, int clickCount) {
		for (GInputListener keylis : inputListener) {
			keylis.consumeEvent = false;
			keylis.mouseClicked(button, x, y, clickCount);
			if (keylis.consumeEvent)
				return;
		}
		currentState.mouseClicked(button, x, y, clickCount);
		
	}
	private void mouseDragged(int oldx, int oldy, int newx, int newy) {
		for (GInputListener keylis : inputListener) {
			keylis.consumeEvent = false;
			keylis.mouseDragged(oldx, oldy, newx, newy);
			if (keylis.consumeEvent)
				return;
		}
		currentState.mouseDragged(oldx, oldy, newx, newy);
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (pointer > 0){
			int dx = oldx - touchX;
			int dy = oldy - touchY;
			if( TimeUtils.timeSinceMillis(touchTime) < 500 && dx*dx + dy*dy < 10 * 10) {
				mouseClicked(Input.MOUSE_RIGHT_BUTTON, oldx, oldy, 1);
			}
			mouseReleased(Input.MOUSE_RIGHT_BUTTON, oldx, oldy);
			
			gc.getInput().setMouseRighButtontDown(false);
			rightIsPressed = false;
		} else {
			int dx = screenX - touchX;
			int dy = screenY - touchY;
			if( TimeUtils.timeSinceMillis(touchTime) < 500 && dx*dx + dy*dy < 10 * 10) {
				mouseClicked(button, screenX, screenY, 1);
			}
			mouseReleased(button, screenX, screenY);
			
			oldx = screenX;
			oldy = screenY;
		}
		

		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		if (pointer == 0) {
			Input.x = screenX;
			Input.y = screenY;
			mouseDragged(oldx, oldy, screenX, screenY);
			oldx = screenX;
			oldy = screenY;
		}
		return false;
	}

	int oldx, oldy;

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		Input.x = screenX;
		Input.y = screenY;
		currentState.mouseMoved(oldx, oldy, screenX, screenX);
		oldx = screenX;
		oldy = screenY;
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		for (GInputListener keylis : inputListener) {
			keylis.consumeEvent = false;
			keylis.mouseWheelMoved(-amount*120);
			if (keylis.consumeEvent)
				return true;
		}
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

	public void addInputListener(GInputListener listener) {
		if (!inputListener.contains(listener))
			inputListener.addFirst(listener);
	}

	public void removeInputListener(GInputListener listener) {
		inputListener.remove(listener);
	}
}
