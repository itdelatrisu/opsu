package fluddokt.opsu.fake;

public class Font {

	public static final String PLAIN = "PLAIN";
	public static final String BOLD = null;
	String name;
	String style;
	float size;
	

	public Font(String fontName) {
		this(fontName,PLAIN,12);
	}
	public Font(String nfiName, String nstyle, float nsize) {
		name = nfiName;
		style = nstyle;
		size = nsize;
	}

	public Font deriveFont(String nstyle, float nsize) {
		return new Font(name,nstyle,nsize);
	}

	public Font deriveFont(String nstyle) {
		return new Font(name,nstyle,size);
	}

	public Font deriveFont(float nsize) {
		return new Font(name,style,nsize);
	}

}
