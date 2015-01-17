package fluddokt.opsu.fake;

import java.util.LinkedList;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.Texture;

public class UnicodeFont {

	LinkedList<ColorEffect> colorEffects = new LinkedList<ColorEffect>();
	public BitmapFont bitmap;
	int padbottom=0, padtop=0;
	public UnicodeFont(Font font) {
		System.out.println(font+" "+font.name);
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ResourceLoader.getFileHandle(font.name));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = (int) font.size;
		while(parameter.size<28)
			parameter.size*=2;
			
		parameter.kerning=true;
		parameter.minFilter = Texture.TextureFilter.Nearest;
		parameter.magFilter = Texture.TextureFilter.Linear;
		//parameter.characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456790:!@#$%^&";
		generator.scaleForPixelHeight(parameter.size);
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
		Graphics.getGraphics().drawString(this, string, x, y+padtop);
	}

	public void drawString(float x, float y, String string, Color textColor) {
		Graphics.getGraphics().setColor(textColor);
		drawString(x, y, string);

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

	public void loadGlyphs() throws SlickException {
		// TODO Auto-generated method stub

	}

	public void setPaddingBottom(int padding) {
		padbottom = padding;
	}

	public void setPaddingTop(int padding) {
		padtop = padding;
	}

}
