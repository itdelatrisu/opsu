package fluddokt.opsu.fake.gui;

import fluddokt.opsu.fake.Graphics;
import fluddokt.opsu.fake.SlickException;

public abstract class AbstractComponent extends GInputAdapter{
	public AbstractComponent(GUIContext container) {
		container.addInputListener(this);
	}

	public abstract int getWidth();

	public abstract int getHeight();

	public abstract int getY();

	public abstract int getX();

	

	public abstract void render(GUIContext container, Graphics g) throws SlickException;

	public abstract void setFocus(boolean focus);

	public abstract void setLocation(int x, int y);

	

}
