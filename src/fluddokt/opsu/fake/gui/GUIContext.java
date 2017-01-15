package fluddokt.opsu.fake.gui;

import com.badlogic.gdx.utils.TimeUtils;

import fluddokt.opsu.fake.Input;

public abstract class GUIContext {
	Input input = new Input();
	public Input getInput() {
		return input;
	}
	
	public abstract void addInputListener(GInputListener listener);
	public abstract void removeInputListener(GInputListener listener);

	public long getTime() {
		return TimeUtils.millis();
	}

}
