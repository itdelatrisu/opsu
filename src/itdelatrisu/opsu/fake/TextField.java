package itdelatrisu.opsu.fake;

public class TextField {

	UnicodeFont font;
	int x,y,w,h;
	String str="";
	public TextField(GameContainer container, UnicodeFont font, int x,
			int y, int w, int h) {
		this.x=x;
		this.y=y;
		this.w=w;
		this.h=h;
		this.font=font;
		// TODO Auto-generated constructor stub
	}

	public void setBackgroundColor(Color transparent) {
		// TODO Auto-generated method stub
		
	}

	public void setBorderColor(Color transparent) {
		// TODO Auto-generated method stub
		
	}

	public void setTextColor(Color white) {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		g.drawString(font, str, x, y);
		g.fillRect(x, y, w, h);
		
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

}
