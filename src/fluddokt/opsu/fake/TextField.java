package fluddokt.opsu.fake;

import java.awt.event.KeyEvent;

import com.badlogic.gdx.Gdx;

import fluddokt.opsu.fake.gui.GInputAdapter;
import fluddokt.opsu.fake.gui.GUIContext;

public class TextField extends GInputAdapter {

	UnicodeFont font;
	int x, y, w, h;
	StringBuilder str = new StringBuilder();
	Color bgColor = Color.green, textColor = Color.blue,
			borderColor = Color.red;
	GameContainer container;
	boolean hasFocus;

	public TextField(GameContainer container, UnicodeFont font, int x, int y,
			int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.font = font;
		this.container = container;
		container.addInputListener(this);
	}

	public void setBackgroundColor(Color color) {
		bgColor = color;
	}

	public void setBorderColor(Color color) {
		borderColor = color;
	}

	public void setTextColor(Color color) {
		textColor = color;
	}

	public void setConsumeEvents(boolean b) {
	
	}

	public void setMaxLength(int i) {
	
	}

	public int getY() {
		return y;
	}

	public int getX() {
		return x;
	}

	public void render(GUIContext container2, Graphics g) {
		g.setColor(bgColor);
		g.fillRect(x, y, w, h);
		g.setColor(borderColor);
		g.drawRect(x, y, w, h);
		g.setColor(textColor);
		g.drawString(font, str.toString(), x, y);
	}

	public void setFocus(boolean b) {
		hasFocus = b;
	}

	public String getText() {
		return str.toString();
	}

	public void setText(String string) {
		str = new StringBuilder(string);
	}

	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}

	@Override
	public void keyType(char character) {
		if (hasFocus) {
			if (character == KeyEvent.VK_BACK_SPACE)
				str.setLength(Math.max(str.length() - 1, 0));
			else if (!Character.isISOControl(character))
				str.append(character);
			consumeEvent();
		}
	}

	@Override
	public void mousePressed(int button, int screenX, int screenY) {
		if (hasFocus) {
			if (!(screenX < x || screenX > x + w || screenY < y || screenY > y + h)) {
				Gdx.input.setOnscreenKeyboardVisible(true);
				consumeEvent();
			} else {
				Gdx.input.setOnscreenKeyboardVisible(false);
			}
		}
	}

	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public void setBound(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}


}
