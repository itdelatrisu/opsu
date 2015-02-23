package fluddokt.opsu.fake;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class GameOpsu extends com.badlogic.gdx.Game {

	StateBasedGame sbg;

	boolean inited = false;
	public GameOpsu() {
	}

	@Override
	public void pause() {
		System.out.println("Game pause");
		if(!inited)
			return;
		super.pause();
		sbg.gc.loseFocus();
		try {
			sbg.render();
		} catch (SlickException e) {
			e.printStackTrace();
		}
		sbg.gc.lostFocus();
	}

	@Override
	public void resume() {
		System.out.println("Game resume");
		if(!inited)
			return;
		super.resume();
		sbg.gc.focus();
	}

	@Override
	public void dispose() {
		System.out.println("Game Dispose");
		if(!inited)
			return;
		super.dispose();
		for (GameImage img : GameImage.values()) {
			try {
				img.dispose();
			} catch (SlickException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void render() {
		super.render();
		
		if(!inited){
			if(Gdx.graphics.getWidth() > Gdx.graphics.getHeight()){
				Opsu.main(new String[0]);
				sbg = Opsu.opsu;
				sbg.gc.width = Gdx.graphics.getWidth();
				sbg.gc.height = Gdx.graphics.getHeight();
				try {
					sbg.init();
				} catch (SlickException e) {
					e.printStackTrace();
				}
				Gdx.input.setInputProcessor(sbg);
				inited = true;
			}
		} else {
			Color bgcolor = Graphics.bgcolor;
			if (bgcolor != null)
				Gdx.gl.glClearColor(bgcolor.r, bgcolor.g, bgcolor.b, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			try {
				sbg.render();
			} catch (SlickException e) {
				e.printStackTrace();
			}
			Graphics.checkMode(0);
		}
	}

	@Override
	public void resize(int width, int height) {
		System.out.println("Game resize" + width + " " + height);

		super.resize(width, height);
		Graphics.resize(width, height);
		//int owid = sbg.gc.width;
		//int ohei = sbg.gc.height;
		if(!inited)
			return;
		sbg.gc.width = width;
		sbg.gc.height = height;

		/*try {
			if(owid!=width || ohei!=height){
				for (GameImage img : GameImage.values()) {
					try {
						img.dispose();
					} catch (SlickException e) {
						e.printStackTrace();
					}
				}
				Utils.init(sbg.gc,sbg);
			}
		} catch (SlickException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	@Override
	public void create() {
		Gdx.graphics.setVSync(false);
		Gdx.input.setCatchBackKey(true);
		
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		
		Graphics.init();
		
	}

}
