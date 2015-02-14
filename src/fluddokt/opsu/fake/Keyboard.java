package fluddokt.opsu.fake;

public class Keyboard {

	public static final int KEY_NONE = 0;

	public static boolean isRepeatEvent() {
		// TODO Auto-generated method stub
		return false;
	}

	public static String getKeyName(int keycode) {
		return com.badlogic.gdx.Input.Keys.toString(keycode);
	}

	public static int getKeyIndex(String value) {
		return com.badlogic.gdx.Input.Keys.valueOf(value);
	}

}
