package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;

public class Input {

	public static final int MOUSE_LEFT_BUTTON = Buttons.LEFT;
	public static final int MOUSE_RIGHT_BUTTON = Buttons.RIGHT;
	public static final int MOUSE_MIDDLE_BUTTON = Buttons.MIDDLE;

	public static final int KEY_F1 = Keys.F1;
	public static final int KEY_F2 = Keys.F2;
	public static final int KEY_F3 = Keys.F3;
	public static final int KEY_F4 = Keys.F4;
	public static final int KEY_F5 = Keys.F5;
	public static final int KEY_F6 = Keys.F6;
	public static final int KEY_F7 = Keys.F7;
	public static final int KEY_F8 = Keys.F8;
	public static final int KEY_F9 = Keys.F9;
	public static final int KEY_F10 = Keys.F10;
	public static final int KEY_F11 = Keys.F11;
	public static final int KEY_F12 = Keys.F12;

	public static final int KEY_DOWN = Keys.DOWN;
	public static final int KEY_UP = Keys.UP;
	public static final int KEY_RIGHT = Keys.RIGHT;
	public static final int KEY_LEFT = Keys.LEFT;
	public static final int KEY_NEXT = Keys.PAGE_DOWN;
	public static final int KEY_PRIOR = Keys.PAGE_UP;
	public static final int KEY_BACK = Keys.BACK;

	public static final int KEY_ESCAPE = Keys.ESCAPE;
	public static final int KEY_SPACE = Keys.SPACE;
	public static final int KEY_ENTER = Keys.ENTER;
	public static final int KEY_TAB = Keys.TAB;
	public static final int KEY_DELETE = Keys.DEL;

	public static final int KEY_RCONTROL = Keys.CONTROL_RIGHT;
	public static final int KEY_LCONTROL = Keys.CONTROL_LEFT;

	public static final int KEY_LSHIFT = Keys.SHIFT_LEFT;
	public static final int KEY_RSHIFT = Keys.SHIFT_RIGHT;

	public static final int KEY_A = Keys.A;
	public static final int KEY_B = Keys.B;
	public static final int KEY_C = Keys.C;
	public static final int KEY_D = Keys.D;
	public static final int KEY_E = Keys.E;
	public static final int KEY_F = Keys.F;
	public static final int KEY_G = Keys.G;
	public static final int KEY_H = Keys.H;
	public static final int KEY_I = Keys.I;
	public static final int KEY_J = Keys.J;
	public static final int KEY_K = Keys.K;
	public static final int KEY_L = Keys.L;
	public static final int KEY_M = Keys.M;
	public static final int KEY_N = Keys.N;
	public static final int KEY_O = Keys.O;
	public static final int KEY_P = Keys.P;
	public static final int KEY_Q = Keys.Q;
	public static final int KEY_R = Keys.R;
	public static final int KEY_S = Keys.S;
	public static final int KEY_T = Keys.T;
	public static final int KEY_U = Keys.U;
	public static final int KEY_V = Keys.V;
	public static final int KEY_W = Keys.W;
	public static final int KEY_X = Keys.X;
	public static final int KEY_Y = Keys.Y;
	public static final int KEY_Z = Keys.Z;
	public static final int KEY_1 = Keys.NUM_1;
	public static final int KEY_2 = Keys.NUM_2;
	public static final int KEY_3 = Keys.NUM_3;
	public static final int KEY_4 = Keys.NUM_4;
	public static final int KEY_5 = Keys.NUM_5;
	public static final int KEY_6 = Keys.NUM_6;
	public static final int KEY_7 = Keys.NUM_7;
	public static final int KEY_8 = Keys.NUM_8;
	public static final int KEY_9 = Keys.NUM_9;
	public static final int KEY_0 = Keys.NUM_0;

	public static final int ANDROID_BACK = Keys.BACK;
	public static final int ANDROID_MENU = Keys.MENU;


	public int getMouseY() {
		return Gdx.input.getY();
	}

	public int getMouseX() {
		return Gdx.input.getX();
	}

	public boolean isKeyDown(int key) {
		return Gdx.input.isKeyPressed(key);
	}

	public void enableKeyRepeat() {
		// TODO Auto-generated method stub

	}

	public boolean isMouseButtonDown(int button) {
		return Gdx.input.isButtonPressed(button);
	}

}
