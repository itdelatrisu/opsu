package fluddokt.opsu.fake.gui;

public abstract class GInputListener {
	public boolean consumeEvent;
	
	public abstract void mousePressed(int button, int x, int y);

	public abstract void mouseReleased(int button, int x, int y);

	public abstract void mouseClicked(int button, int x, int y, int clickCount);

	public abstract void keyPressed(int key, char c);

	public abstract void mouseDragged(int oldx, int oldy, int newx, int newy);

	public abstract void mouseWheelMoved(int delta) ;

	public abstract void keyType(char character);

	public abstract void keyReleased(int keycode, char c);

	public void resetConsume() {
		consumeEvent = false;
	}
	
	public void consumeEvent() {
		consumeEvent = true;
	}

	public boolean isConsumed() {
		return consumeEvent;
	}
}
