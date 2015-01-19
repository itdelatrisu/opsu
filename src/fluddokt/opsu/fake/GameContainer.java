package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;

public class GameContainer {

	public int width = 800;
	public int height = 600;
	public boolean hasFocus = true;
	
	protected boolean running;
	protected boolean forceExit;
	
	protected void setup(){}
	protected void getDelta(){}
	
	protected boolean running(){return false;}
	protected void gameLoop(){}
	
	
	
	public int getWidth() {
			return width;
	}

	public int getHeight() {
		return height;
	}

	public void exit() {
		Gdx.app.exit();
	}

	public Input getInput() {
		// TODO Auto-generated method stub
		return new Input();
	}

	public boolean hasFocus() {
		return hasFocus;
	}

	public void setTargetFrameRate(int targetFPS) {
		// TODO Auto-generated method stub
		
	}

	static float musvolume;
	public void setMusicVolume(float musicVolume) {
		musvolume = musicVolume;
		if(music!=null)
			music.setVolume(musvolume);
	}

	public void setShowFPS(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void setAlwaysRender(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public int getFPS() {
		// TODO Auto-generated method stub
		return Gdx.graphics.getFramesPerSecond();
	}

	public Graphics getGraphics() {
		return Graphics.getGraphics();
	}

	public int getScreenWidth() {
		return width;
	}

	public int getScreenHeight() {
		return height;
	}

	public void setVSync(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void start() throws SlickException {
		// TODO Auto-generated method stub
		
	}

	protected void updateAndRender(int delta) throws SlickException {
		// TODO Auto-generated method stub
		
	}
	
	static Music music;
	public static void setMusic(Music imusic) {
		// TODO Auto-generated method stub
		if(music!=null)
			music.dispose();
		music = imusic;
		music.setVolume(musvolume);
	}

}
