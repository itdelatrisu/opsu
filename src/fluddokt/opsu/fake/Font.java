package fluddokt.opsu.fake;

import com.badlogic.gdx.files.FileHandle;

public class Font {

	public static final String PLAIN = "PLAIN";
	public static final String BOLD = "BOLD";
	String name;
	String style;
	float size;
	FileHandle file;

	public Font(String fontName) {
		this(fontName,PLAIN,12);
	}
	public Font(String nfiName, String nstyle, float nsize) {
		name = nfiName;
		style = nstyle;
		size = nsize;
		file = ResourceLoader.getFileHandle(name);
	}
	public Font(String nfiName, String nstyle, float nsize, FileHandle nfile) {
		name = nfiName;
		style = nstyle;
		size = nsize;
		file = nfile;
	}

	public Font deriveFont(String nstyle, float nsize) {
		return new Font(name,nstyle,nsize,file);
	}

	public Font deriveFont(String nstyle) {
		return new Font(name,nstyle,size,file);
	}

	public Font deriveFont(float nsize) {
		return new Font(name,style,nsize,file);
	}

}
