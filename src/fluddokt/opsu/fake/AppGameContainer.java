package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;

public class AppGameContainer extends GameContainer {

	public static int containerWidth, containerHeight;
	
	public AppGameContainer(Game2 game) {
		super((StateBasedGame) game);
	}

	public AppGameContainer(Game2 game, int width, int height, boolean fullscreen) {
		super((StateBasedGame) game);
	}

	public void setDisplayMode(int containerWidth, int containerHeight,
			boolean b) throws SlickException {
		System.out.println("setDisplayMode :" + containerWidth + " "
				+ containerHeight);
		AppGameContainer.containerWidth = containerWidth;
		AppGameContainer.containerHeight = containerHeight;

		Gdx.graphics.setDisplayMode(containerWidth, containerHeight, b);
		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
	}

	public void setIcons(String[] icons) {
		// TODO Auto-generated method stub
	}

	public void destroy() {
		// TODO Auto-generated method stub
	}

}
