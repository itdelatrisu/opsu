package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;

public class GameContainer {

	public int width = 800;
	public int height = 600;
	public int getWidth() {
		// TODO Auto-generated method stub
		return width;
	}

	public int getHeight() {
		// TODO Auto-generated method stub
		return height;
	}

	public void exit() {
		Gdx.app.exit();
		// TODO Auto-generated method stub
		
	}

	public Input getInput() {
		// TODO Auto-generated method stub
		return new Input();
	}

	public boolean hasFocus() {
		// TODO Auto-generated method stub
		return true;
	}

	public void setTargetFrameRate(int targetFPS) {
		// TODO Auto-generated method stub
		
	}

	public void setMusicVolume(float musicVolume) {
		// TODO Auto-generated method stub
		
	}

	public void setShowFPS(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void setAlwaysRender(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public Object getFPS() {
		// TODO Auto-generated method stub
		return null;
	}

	public Graphics getGraphics() {
		return Graphics.getGraphics();
	}

	public int getScreenWidth() {
		// TODO Auto-generated method stub
		return width;
	}

	public int getScreenHeight() {
		// TODO Auto-generated method stub
		return height;
	}

	public void setVSync(boolean b) {
		// TODO Auto-generated method stub
		
	}

}
