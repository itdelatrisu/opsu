package fluddokt.opsu.fake;

import java.io.IOException;
import java.net.URI;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;

public class Desktop {

	public static boolean isDesktopSupported() {
		if(Gdx.app.getType() == ApplicationType.Desktop){
			return java.awt.Desktop.isDesktopSupported();
		}
		
		return false;
	}

	static Desktop single;
	public static Desktop getDesktop() {
		if(single == null)
			single = new Desktop();
		return single;
	}

	public void browse(URI rEPOSITORY_URI) throws IOException {
		if(Gdx.app.getType() == ApplicationType.Desktop){
			java.awt.Desktop.getDesktop().browse(rEPOSITORY_URI);
		}
	}

}
