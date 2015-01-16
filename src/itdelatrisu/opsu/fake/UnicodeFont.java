package itdelatrisu.opsu.fake;

import java.util.LinkedList;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.Texture;

public class UnicodeFont {

	LinkedList<ColorEffect> colorEffects = new LinkedList<ColorEffect>();
	public BitmapFont bitmap;
	public UnicodeFont(Font font) {
		System.out.println(font+" "+font.name);
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ResourceLoader.getFileHandle(font.name));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = (int) font.size;
		while(parameter.size<28)
			parameter.size*=3;
			
		parameter.kerning=true;
		parameter.minFilter = Texture.TextureFilter.Linear;
		parameter.magFilter = Texture.TextureFilter.Linear;
		//parameter.characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456790:!@#$%^&";
		generator.scaleForPixelHeight(parameter.size+2);
		bitmap = generator.generateFont(parameter); // font size 12 pixels
		bitmap.setUseIntegerPositions(false);
		bitmap.setScale(font.size/parameter.size);
		generator.dispose(); // don't forget to dispose to avoid memory leaks!
		
	
	}

	public void addAsciiGlyphs() {
		// TODO Auto-generated method stub

	}

	public void addGlyphs(String titleUnicode) {
		// TODO Auto-generated method stub

	}

	public void drawString(float x, float y, String string) {
		Graphics.drawString(this, string, x, y);
	}

	public void drawString(float x, float y, String string, Color textColor) {
		Graphics.setColor(textColor);
		Graphics.drawString(this, string, x, y);

	}

	public LinkedList<ColorEffect> getEffects() {
		return colorEffects;
	}

	public int getHeight(String str) {
		return (int) bitmap.getBounds(str).height;
	}

	public int getLineHeight() {
		return (int)bitmap.getLineHeight()+6;
	}

	public int getWidth(String str) {
		return (int) bitmap.getBounds(str).width;
	}

	public void loadGlyphs() {
		// TODO Auto-generated method stub

	}

	public void setPaddingBottom(int padding) {
		// TODO Auto-generated method stub

	}

	public void setPaddingTop(int padding) {
		// TODO Auto-generated method stub

	}

}
