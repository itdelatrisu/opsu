package fluddokt.opsu.fake;

import java.util.HashSet;
import java.util.LinkedList;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.Texture;

public class UnicodeFont {

	LinkedList<Effect> colorEffects = new LinkedList<Effect>();
	public BitmapFont bitmap;
	int padbottom=0, padtop=0;
	Font font;
	StringBuilder chars = new StringBuilder();
	HashSet<Character> set = new HashSet<Character>();
	boolean glythsAdded = false;
	
	public UnicodeFont(Font font) {
		System.out.println(font+" "+font.name);
		this.font = font;
		
		String initialList = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890:!@#$%^&";
		
		chars.append(initialList);
		for(int i=0;i<initialList.length();i++){
			set.add(initialList.charAt(i));
		}
		
		regenBitmap();
	}

	public void addAsciiGlyphs() {
		// TODO Auto-generated method stub

	}
	public void drawString(float x, float y, String string) {
		//checkString(string);
		Graphics.getGraphics().drawString(this, string, x, y+padtop);
	}
	public void drawString(float x, float y, String string, Color textColor) {
		Graphics.getGraphics().setColor(textColor);
		drawString(x, y, string);

	}

	public LinkedList<Effect> getEffects() {
		return colorEffects;
	}

	public int getHeight(String str) {
		return (int) bitmap.getBounds(str).height;
	}

	public int getLineHeight() {
		return (int)bitmap.getLineHeight();
	}

	public int getWidth(String str) {
		return (int) bitmap.getBounds(str).width;
	}
	public void addGlyphs(String string) {
		checkString(string);
	}
	private void checkString(String string) {
		for(int i=0 ;i<string.length(); i++){
			char c = string.charAt(i);
			if(!set.contains(c)){
				set.add(c);
				chars.append(c);
				glythsAdded = true;
			}
		}
	}
	public void loadGlyphs() throws SlickException {
		if(glythsAdded)
			regenBitmap();
	}
	private void regenBitmap() {
		//System.out.println("Regen Bitmap "+font.name+" "+chars.toString());
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ResourceLoader.getFileHandle(font.name));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = (int) font.size;
		//sizes smaller than 28 produces garbage so scale it up
		while(parameter.size<28)
			parameter.size*=2;
			
		parameter.kerning=true;
		parameter.minFilter = Texture.TextureFilter.Linear;
		parameter.magFilter = Texture.TextureFilter.Linear;
		//parameter.characters = ";
		
		
		parameter.characters = chars.toString();
		generator.scaleForPixelHeight(parameter.size);
		bitmap = generator.generateFont(parameter); // font size 12 pixels
		bitmap.setUseIntegerPositions(false);
		bitmap.setScale(font.size/parameter.size);
		generator.dispose(); // don't forget to dispose to avoid memory leaks!
		
	}

	public void setPaddingBottom(int padding) {
		padbottom = padding;
	}

	public void setPaddingTop(int padding) {
		padtop = padding;
	}

}
