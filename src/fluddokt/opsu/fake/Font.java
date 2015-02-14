package fluddokt.opsu.fake;

import java.util.HashSet;
import java.util.LinkedList;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class Font {

	public static final String PLAIN = "PLAIN";
	public static final String BOLD = "BOLD";
	String name;
	String style;
	float size;
	FileHandle file;

	public Font(String fontName) {
		this(fontName, PLAIN, 12);
	}

	public Font(String nfiName, String nstyle, float nsize) {
		this(nfiName, nstyle, nsize, ResourceLoader.getFileHandle(nfiName));
	}

	public Font(String nfiName, String nstyle, float nsize, FileHandle nfile) {
		name = nfiName;
		style = nstyle;
		size = nsize;
		file = nfile;
		dynFont = new DynamicFreeType(file, this);
		// System.out.println(font+" "+font.name);

	}

	public Font deriveFont(String nstyle, float nsize) {
		return new Font(name, nstyle, nsize, file);
	}

	public Font deriveFont(String nstyle) {
		return new Font(name, nstyle, size, file);
	}

	public Font deriveFont(float nsize) {
		return new Font(name, style, nsize, file);
	}

	LinkedList<Effect> colorEffects = new LinkedList<Effect>();
	public BitmapFont bitmap;
	int padbottom = 0, padtop = 0;
	StringBuilder chars = new StringBuilder();
	HashSet<Character> set = new HashSet<Character>();
	boolean glythsAdded = false;

	DynamicFreeType dynFont;

	// Font font;
	// public UnicodeFont(Font font) {
	// this.font = font;

	// String initialList =
	// "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890:!@#$%^&";

	// chars.append(initialList);
	// for(int i=0;i<initialList.length();i++){
	// set.add(initialList.charAt(i));
	// }
	// for(int i=0;i<255;i++){
	// addGlyphs((char)i+"");
	// }

	// regenBitmap();
	// }

	public void addAsciiGlyphs() {
		// TODO Auto-generated method stub

	}

	public void drawString(float x, float y, String string) {
		checkString(string);
		Graphics.getGraphics().drawString(this, string, x, y + padtop);
	}

	public void drawString(float x, float y, String string, Color textColor) {
		Graphics.getGraphics().setColor(textColor);
		drawString(x, y, string);

	}

	public LinkedList<Effect> getEffects() {
		return colorEffects;
	}

	public int getHeight(String str) {
		return dynFont.getHeight(str) + padtop + padbottom;
		// return (int) bitmap.getBounds(str).height+padtop+padbottom;
	}

	public int getLineHeight() {
		return dynFont.getLineHeight() + padtop + padbottom;
		// return (int)bitmap.getLineHeight()+padtop+padbottom;
	}

	public int getWidth(String str) {
		return dynFont.getWidth(str);
		// return (int) bitmap.getBounds(str).width;
	}

	public void addGlyphs(String string) {
		checkString(string);
	}

	private void checkString(String string) {
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (!set.contains(c)) {
				set.add(c);
				chars.append(c);
				glythsAdded = true;
			}
		}
	}

	public void loadGlyphs() throws SlickException {
		// if(glythsAdded)
		// regenBitmap();
	}

	/*
	 * private void regenBitmap() {
	 * //System.out.println("Regen Bitmap "+font.name+" "+chars.toString());
	 * FreeTypeFontGenerator generator = new FreeTypeFontGenerator(font.file);
	 * FreeTypeFontParameter parameter = new FreeTypeFontParameter();
	 * parameter.size = (int) font.size; //sizes smaller than 28 produces
	 * garbage so scale it up while(parameter.size<22) parameter.size*=2;
	 * 
	 * parameter.kerning=true; parameter.minFilter =
	 * Texture.TextureFilter.Linear; parameter.magFilter =
	 * Texture.TextureFilter.Linear; //parameter.characters = ";
	 * 
	 * 
	 * parameter.characters = chars.toString();
	 * generator.scaleForPixelHeight(parameter.size); bitmap =
	 * generator.generateFont(parameter); // font size 12 pixels
	 * bitmap.setUseIntegerPositions(false);
	 * bitmap.setScale(font.size/parameter.size); generator.dispose(); // don't
	 * forget to dispose to avoid memory leaks!
	 * 
	 * } //
	 */

	public void setPaddingBottom(int padding) {
		padbottom = padding;
	}

	public void setPaddingTop(int padding) {
		padtop = padding;
	}

}
