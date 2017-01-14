package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;

public class Display {

	public static void setTitle(String title) {
		Gdx.graphics.setTitle(title);
	}

	static Resolution deskRes;
	public static Resolution getDesktopDisplayMode() {
		if(deskRes == null) {
			DisplayMode d = Gdx.graphics.getDesktopDisplayMode();
			deskRes = new Resolution(d.width, d.height);
		}
		return deskRes;

	}

}
