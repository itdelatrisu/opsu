package fluddokt.opsu.fake.gui;

import com.badlogic.gdx.utils.TimeUtils;

import fluddokt.opsu.fake.Input;
import fluddokt.opsu.fake.InputListener;

public abstract class GUIContext {
	Input input = new Input();
	public Input getInput() {
		return input;
	}
	
	public abstract void addInputListener(InputListener listener);
	public abstract void removeInputListener(InputListener listener);

	public long getTime() {
		return TimeUtils.millis();
	}

}
