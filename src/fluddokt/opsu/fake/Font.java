package fluddokt.opsu.fake;

import java.util.LinkedList;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class Font {

	public static final int PLAIN = 0;
	public static final int BOLD = 1;
	String name;
	int style;
	float size;
	FileHandle file;

	public Font(String fontName) {
		this(fontName, PLAIN, 12);
	}

	public Font(String nfiName, int nstyle, float nsize) {
		this(nfiName, nstyle, nsize, ResourceLoader.getFileHandle(nfiName));
	}

	public Font(String nfiName, int nstyle, float nsize, FileHandle nfile) {
		name = nfiName;
		style = nstyle;
		size = nsize;
		file = nfile;
		dynFont = new DynamicFreeTypeFont(file, this);
		// System.out.println(font+" "+font.name);

	}

	public Font deriveFont(int nstyle, float nsize) {
		return new Font(name, nstyle, nsize, file);
	}

	public Font deriveFont(int nstyle) {
		return new Font(name, nstyle, size, file);
	}

	public Font deriveFont(float nsize) {
		return new Font(name, style, nsize, file);
	}

	LinkedList<Effect> colorEffects = new LinkedList<Effect>();
	public BitmapFont bitmap;
	int padbottom = 0, padtop = 0;
	//StringBuilder chars = new StringBuilder();
	//HashSet<Character> set = new HashSet<Character>();
	//boolean glythsAdded = false;

	DynamicFreeTypeFont dynFont;

	public void addAsciiGlyphs() {
	}

	public void drawString(float x, float y, String string) {
		//checkString(string);
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
	}

	public int getLineHeight() {
		return dynFont.getLineHeight() + padtop + padbottom;
	}

	public int getWidth(String str) {
		return dynFont.getWidth(str);
	}

	public void addGlyphs(String string) {
		//checkString(string);
	}

	/*
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
	*/

	public void loadGlyphs() throws SlickException {
	}

	public void setPaddingBottom(int padding) {
		padbottom = padding;
	}

	public void setPaddingTop(int padding) {
		padtop = padding;
	}
	
	public int getPaddingTop() {
		return padtop;
	}

	public int getPaddingBottom() {
		return padbottom;
	}

}
