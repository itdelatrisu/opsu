package fluddokt.opsu.fake;

public interface InputListener {

	void keyDown(int keycode);

	void keyUp(int keycode);

	void keyType(char character);

	void touchDown(int screenX, int screenY, int pointer, int button);

	void touchUp(int screenX, int screenY, int pointer, int button);

	void touchDragged(int screenX, int screenY, int pointer);

}
