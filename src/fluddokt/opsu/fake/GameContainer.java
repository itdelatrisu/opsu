package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;

public class GameContainer {
	public static StateBasedGame sbg;
	
	public int width = 800;
	public int height = 600;
	public boolean hasFocus = true;
	Input input = new Input();
	
	protected boolean running;
	protected boolean forceExit;
	
	public GameContainer(StateBasedGame game) {
		sbg =(StateBasedGame)game;
		sbg.setContainer(this);
	}
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
		close_sub();
		if(music != null){
			music.dispose();
		}
		Gdx.app.exit();
	}

	protected void close_sub() {
		// TODO Auto-generated method stub
		
	}
	public Input getInput() {
		return input;
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
			music.setMusicVolume(musvolume);
	}

	public void setShowFPS(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void setAlwaysRender(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public int getFPS() {
		return Gdx.graphics.getFramesPerSecond();
	}

	public Graphics getGraphics() {
		return Graphics.getGraphics();
	}

	public int getScreenWidth() {
		return Gdx.graphics.getDesktopDisplayMode().width;
	}

	public int getScreenHeight() {
		return Gdx.graphics.getDesktopDisplayMode().height;
	}

	public void setVSync(boolean b) {
		Gdx.graphics.setVSync(b);
	}

	public void start() throws SlickException {
		// TODO Auto-generated method stub
		
	}

	protected void updateAndRender(int delta) throws SlickException {
		// TODO Auto-generated method stub
		
	}
	
	static Music music;
	public static void setMusic(Music imusic) {
		if(music!=null)
			music.dispose();
		music = imusic;
		music.setVolume(musvolume);
	}
	boolean musicWasPlaying = false;
	public void loseFocus() {
		hasFocus = false;
		
	}
	public void lostFocus() {
		if(music!=null){
			musicWasPlaying = music.playing();
			if(Gdx.app.getType() == ApplicationType.Android){
				music.pause();
			}
		}
	}
	public void focus() {
		hasFocus = true;
		if(music!=null && musicWasPlaying){
			if(Gdx.app.getType() == ApplicationType.Android){
				music.resume();
			}
		}
	}
	public void setForceExit(boolean b) {
		// TODO Auto-generated method stub
		
	}
	public void addInputListener(InputListener listener) {
		sbg.addKeyListener(listener);
	}
	public void removeInputListener(InputListener listener) {
		sbg.removeKeyListener(listener);
		
	}

}
