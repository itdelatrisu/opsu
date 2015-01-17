package itdelatrisu.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
public class Input {

	public static final int MOUSE_LEFT_BUTTON = Buttons.LEFT;
	public static final int MOUSE_RIGHT_BUTTON = Buttons.RIGHT;
	public static final int MOUSE_MIDDLE_BUTTON = Buttons.MIDDLE;
	
	//TODO KEYS1!!
	public static final int KEY_ESCAPE = Keys.ESCAPE;
	
	public static final int KEY_SPACE = Keys.SPACE;
	public static final int KEY_R = Keys.R;
	public static final int KEY_RCONTROL = Keys.CONTROL_RIGHT;
	public static final int KEY_LCONTROL = Keys.CONTROL_LEFT;
	public static final int KEY_S = Keys.S;
	public static final int KEY_L = Keys.L;
	public static final int KEY_F12 = Keys.F12;
	public static final int KEY_F1 = Keys.F1;
	public static final int KEY_F2 = Keys.F2;
	public static final int KEY_ENTER = Keys.ENTER;
	public static final int KEY_DOWN = Keys.DOWN;
	public static final int KEY_UP = Keys.UP;
	public static final int KEY_RIGHT = Keys.RIGHT;
	public static final int KEY_LEFT = Keys.LEFT;
	public static final int KEY_NEXT = Keys.PAGE_DOWN;
	public static final int KEY_PRIOR = Keys.PAGE_UP;
	public static final int KEY_BACK = Keys.BACK;
	public static final int KEY_Q = Keys.Q;
	public static final int KEY_P = Keys.P;
	public static final int KEY_1 = Keys.NUM_1;
	public static final int KEY_2 = Keys.NUM_2;
	public static final int KEY_TAB = Keys.TAB;
	public static final int KEY_LSHIFT = Keys.SHIFT_LEFT;
	public static final int KEY_RSHIFT = Keys.SHIFT_RIGHT;
	public static final int KEY_Z = Keys.Z;
	public static final int KEY_X = Keys.X;
	public static final int KEY_A = Keys.A;
	public static final int KEY_B = Keys.B;
	public static final int KEY_V = Keys.V;
	public static final int KEY_W = Keys.W;
	
	public static final int ANDROID_BACK = Keys.BACK;

	public int getMouseY() {
		return Gdx.input.getY();
	}

	public boolean isKeyDown(int key) {
		// TODO Auto-generated method stub
		return Gdx.input.isKeyPressed(key);
	}

	public int getMouseX() {
		return Gdx.input.getX();
	}

	public void enableKeyRepeat() {
		// TODO Auto-generated method stub
		
	}

	public boolean isMouseButtonDown(int button) {
		// TODO Auto-generated method stub
		return Gdx.input.isButtonPressed(button);
	}

}
