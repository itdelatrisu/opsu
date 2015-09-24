package fluddokt.opsu.fake;

import java.io.IOException;
import java.net.URI;

import com.badlogic.gdx.Gdx;

public class Desktop {

	public enum Action {
		BROWSE
	}
	
	public static boolean isDesktopSupported() {
		// if(Gdx.app.getType() == ApplicationType.Desktop){
		// return java.awt.Desktop.isDesktopSupported();
		// }
		// else
		return true;
	}

	static Desktop single;

	public static Desktop getDesktop() {
		if (single == null)
			single = new Desktop();
		return single;
	}

	public void browse(URI uri) throws IOException {
		// if(Gdx.app.getType() == ApplicationType.Desktop){
		// java.awt.Desktop.getDesktop().browse(uri);
		// } else {
		Gdx.net.openURI(uri.toString());
		// }
	}

	public boolean isSupported(Action browse) {
		return true;
	}

}
