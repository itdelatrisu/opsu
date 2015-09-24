package fluddokt.opsu.fake.gui;

import fluddokt.opsu.fake.Graphics;
import fluddokt.opsu.fake.InputListener;
import fluddokt.opsu.fake.SlickException;

public abstract class AbstractComponent {

	public AbstractComponent(GUIContext container) {
		container.addInputListener(new InputListener() {
			
			@Override
			public void touchUp(int screenX, int screenY, int pointer, int button) {
				mouseReleased(button, screenX, screenY);
			}
			
			@Override
			public void touchDragged(int screenX, int screenY, int pointer) {
			}
			
			@Override
			public void touchDown(int screenX, int screenY, int pointer, int button) {
				mousePressed(button, screenX, screenY);
			}
			
			@Override
			public void keyUp(int keycode) {
			}
			
			@Override
			public void keyType(char character) {
			}
			
			@Override
			public void keyDown(int keycode) {
			}
		});
	}

	public abstract int getWidth();

	public abstract int getHeight();

	public abstract int getY();

	public abstract int getX();

	public abstract void mousePressed(int button, int x, int y);

	public abstract void mouseReleased(int button, int x, int y);

	public abstract void render(GUIContext container, Graphics g) throws SlickException;

	public abstract void setFocus(boolean focus);

	public abstract void setLocation(int x, int y);

	public void consumeEvent(){
		
	}


}
