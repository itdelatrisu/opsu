package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;

public class Log {

	public static void error(String string) {
		Gdx.app.error("Error", string);
		if (DefaultLogSystem.out != null) {
			DefaultLogSystem.out.println("Error: " + string + " ");
		}
		GameOpsu.error(string, null);
	}

	public static void error(Throwable e) {
		error(":", e);

	}

	public static void error(String string, Throwable e) {
		if(e != null)
			Gdx.app.error("Error", string, e);
		else{
			Gdx.app.error("Error", string+" null");
		}
		if (DefaultLogSystem.out != null) {
			DefaultLogSystem.out.println("Error: " + string + " ");
			if(e != null)
				e.printStackTrace(DefaultLogSystem.out);
		}
		GameOpsu.error(string, e);

	}

	public static void setVerbose(boolean b) {
		// TODO Auto-generated method stub

	}

	public static void warn(String string, Throwable e) {
		Gdx.app.log("warn", string, e);
		if (DefaultLogSystem.out != null) {
			DefaultLogSystem.out.println("Warn: " + string + " ");
			e.printStackTrace(DefaultLogSystem.out);
		}

	}

	public static void warn(String string) {
		Gdx.app.log("warn", string);
		if (DefaultLogSystem.out != null) {
			DefaultLogSystem.out.println("Warn: " + string + " ");
		}

	}

	public static void debug(String string) {
		Gdx.app.debug("debug", string);
		if (DefaultLogSystem.out != null) {
			DefaultLogSystem.out.println("Debug: " + string + " ");
		}
	}


}
