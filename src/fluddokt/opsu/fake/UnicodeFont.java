package fluddokt.opsu.fake;

public class UnicodeFont extends Font {

	public UnicodeFont(Font font) {
		super(font.name, font.style, font.size, font.file);
	}

	public void addGlyphs(char c, char c2) {
		// TODO Auto-generated method stub
		
	}

	public void addBackupFont(UnicodeFont backup) {
		dynFont.addBackupFace(backup);
	}


}
