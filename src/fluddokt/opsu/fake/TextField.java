package fluddokt.opsu.fake;

import java.awt.event.KeyEvent;

import com.badlogic.gdx.Gdx;

public class TextField implements InputListener{

	UnicodeFont font;
	int x,y,w,h;
	StringBuilder str = new StringBuilder();
	Color bgColor=Color.green,textColor=Color.blue, borderColor=Color.red;
	GameContainer container;
	boolean hasFocus;
	
	public TextField(GameContainer container, UnicodeFont font, int x,
			int y, int w, int h) {
		this.x=x;
		this.y=y;
		this.w=w;
		this.h=h;
		this.font=font;
		this.container = container;
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
		// TODO Auto-generated method stub
		
	}

	public void setMaxLength(int i) {
		// TODO Auto-generated method stub
		
	}

	public float getY() {
		return y;
	}

	public float getX() {
		return x;
	}

	public void render(GameContainer container, Graphics g) {
		g.setColor(bgColor);
		g.fillRect(x, y, w, h);
		g.setColor(borderColor);
		g.drawRect(x, y, w, h);
		g.setColor(textColor);
		g.drawString(font, str.toString(), x, y);
	}
	public void setFocus(boolean b) {
		//if(b!=hasFocus){
			if(b){
				container.addInputListener(this);
			}else
				container.removeInputListener(this);
		//}
	}

	public String getText() {
		return str.toString();
	}

	public void setText(String string) {
		str = new StringBuilder(string);
	}

	public float getWidth() {
		return w;
	}

	public float getHeight() {
		return h;
	}

	@Override
	public void keyDown(int keycode) {
	}

	@Override
	public void keyUp(int keycode) {
	}

	@Override
	public void keyType(char character) {
		if(character == KeyEvent.VK_BACK_SPACE)
			str.setLength(Math.max(str.length() - 1, 0));
		else if(!Character.isISOControl(character))
			str.append(character);
	}

	@Override
	public void touchDown(int screenX, int screenY, int pointer, int button) {
		if(!(screenX<x || screenX>x+w || screenY<y || screenY > y+h)){
			Gdx.input.setOnscreenKeyboardVisible(true);
		} else {
			Gdx.input.setOnscreenKeyboardVisible(false);
		}
	}

	@Override
	public void touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		
	}

}
