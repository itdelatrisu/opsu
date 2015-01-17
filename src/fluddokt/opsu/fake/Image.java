package fluddokt.opsu.fake;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Image {

	TextureRegion tex;
	float width,height;
	private float alpha=1f, rotation=0;
	String name;
	public Image(String filename) throws SlickException{
		Texture ttex = new Texture(ResourceLoader.getFileHandle(filename));
		ttex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		tex = new TextureRegion(ttex);
		width = tex.getRegionWidth();
		height = tex.getRegionHeight();
		name=filename;
	}
	public Image(Image copy, float nscalex, float nscaley) {
		tex = copy.tex;
		name = copy.name+" s "+nscalex+" "+nscaley;
		width = nscalex;
		height = nscaley;
	}
	public Image(Image copy, int x, int y,int wid,int hei) {
		tex = new TextureRegion(copy.tex, x, y, wid, hei);
		width=wid;
		height=hei;
		name = copy.name+" r "+x+" "+y+" "+wid+" "+hei;
	}
	public Image() {
	}
	public Image(int width, int height) {
		this.width = width;
		this.height = height;
		name = "null image";
	}

	public int getHeight() {
		return (int)height;
	}
	public int getWidth() {
		return (int)width;
	}
	public Image getScaledCopy(float w, float h) {
		return new Image(this,w,h);
	}
	
	public Image getScaledCopy(float f) {
		return new Image(this,width*f,height*f);
		//return new Image(this,tex.getWidth()*f,tex.getHeight()*f);
		
	}

	public void setAlpha(float f) {
		//System.out.println("setAlpha: "+this+" "+name+" "+getAlpha()+" "+f);
		this.alpha = f;
		
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	boolean destroyed = false;
	public void destroy() throws SlickException {
		destroyed=true;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public void draw() {
		Graphics.getGraphics().setColor(Color.white.multAlpha(alpha));
		//Graphics.setColor(Color.white);
		
		Graphics.getGraphics().drawTexture(getTextureRegion(), 0, 0, getWidth(),getHeight(),rotation);
	}
	public void draw(float x, float y) {
		//System.out.println("imgalpha: "+this+" "+name+" "+alpha);
		Graphics.getGraphics().setColor(Color.white.multAlpha(alpha));
		//Graphics.getGraphics().setColor(Color.white);
		
		Graphics.getGraphics().drawTexture(getTextureRegion(),x,y, getWidth(),getHeight(),rotation);
	}

	public void draw(float x, float y, Color filter) {
		Graphics.getGraphics().setColor(filter.multAlpha(alpha));
		//Graphics.getGraphics().setColor(Color.white);
		Graphics.getGraphics().drawTexture(getTextureRegion(), x, y, getWidth(),getHeight(),rotation);
	}
	public void drawCentered(float x, float y) {
		Graphics.getGraphics().setColor(Color.white.multAlpha(alpha));
		//Graphics.getGraphics().setColor(Color.white);
		Graphics.getGraphics().drawTexture(getTextureRegion(), x-getWidth()/2, y-getHeight()/2,getWidth(),getHeight(),rotation);
	}
	public TextureRegion getTextureRegion(){
		return tex;
	}
	

	public Image getSubImage(int x, int y, int w, int h) {
		return new Image(this,x,y,w,h);
	}

	public float getRotation() {
		return rotation;
	}

	public void rotate(float f) {
		rotation+=f;
		
	}

	

}
