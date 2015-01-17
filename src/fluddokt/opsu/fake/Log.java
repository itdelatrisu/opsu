package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;

public class Log {

	/*public static void error(String string, Exception e) {
		Gdx.app.error("Error",string, e);
		if(DefaultLogSystem.out != null){
			DefaultLogSystem.out.println("Error: "+string+" ");
			e.printStackTrace(DefaultLogSystem.out);
			}
		
	}*/

	public static void error(String string) {
		Gdx.app.error("Error",string);
		if(DefaultLogSystem.out != null){
			DefaultLogSystem.out.println("Error: "+string+" ");
		}
		
	}

	public static void error(Exception e) {
		Gdx.app.error("Error","",e);
		e.printStackTrace();
		if(DefaultLogSystem.out != null){
			DefaultLogSystem.out.println("Error: ");
			e.printStackTrace(DefaultLogSystem.out);
		}
		
	}

	public static void error(String string, Throwable e) {
		Gdx.app.error("Error",string,e);
		if(DefaultLogSystem.out != null){
			DefaultLogSystem.out.println("Error: "+string+" ");
			e.printStackTrace(DefaultLogSystem.out);
			}
		
	}

	public static void setVerbose(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public static void warn(String string, Throwable e) {
		Gdx.app.log("warn",string,e);
		if(DefaultLogSystem.out != null){
			DefaultLogSystem.out.println("Error: "+string+" ");
			e.printStackTrace(DefaultLogSystem.out);
			}
		
	}

	public static void warn(String string) {
		Gdx.app.log("warn",string);
		if(DefaultLogSystem.out != null){
			DefaultLogSystem.out.println("Error: "+string+" ");
		}
		
	}

	public static void debug(String string) {
		Gdx.app.log("Error",string);
		if(DefaultLogSystem.out != null){
			DefaultLogSystem.out.println("Error: "+string+" ");
		}
	}

	public static void error(Throwable e) {
		error("Er:,e");
		
	}

}
