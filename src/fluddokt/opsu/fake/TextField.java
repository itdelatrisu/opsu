package fluddokt.opsu.fake;

public class TextField {

	UnicodeFont font;
	int x,y,w,h;
	String str="";
	Color bgColor=Color.green,textColor=Color.blue, borderColor=Color.red;
	GameContainer container;
	
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
		g.drawString(font, str, x, y);
	}

	public void setFocus(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public String getText() {
		return str;
	}

	public void setText(String string) {
		str = string;
	}

	public float getWidth() {
		return w;
	}

	public float getHeight() {
		return h;
	}

}
