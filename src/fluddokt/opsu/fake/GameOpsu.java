package fluddokt.opsu.fake;

import itdelatrisu.opsu.Opsu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;

public class GameOpsu  extends com.badlogic.gdx.Game {

	//GameContainer gc = new GameContainer();
	StateBasedGame sbg;
	
	private void addState(BasicGameState gs) throws SlickException {
		sbg.addState(gs);
		
	}


	@Override
	public void setScreen(Screen screen) {
		// TODO Auto-generated method stub
		super.setScreen(screen);
	}

	@Override
	public Screen getScreen() {
		// TODO Auto-generated method stub
		return super.getScreen();
	}
	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void render() {
		super.render();
		Color bgcolor = Graphics.bgcolor;
		if(bgcolor!=null)
			Gdx.gl.glClearColor(bgcolor.r, bgcolor.g, bgcolor.b, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		try {
			sbg.render();
		} catch (SlickException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Graphics.checkMode(0);
	}

	@Override
	public void resize(int width, int height) {
		// TODO Auto-generated method stub
		super.resize(width, height);
		Graphics.resize(width, height);
		sbg.gc.width = width;
		sbg.gc.height = height;
		
	}

	@Override
	public void create() {
		
		FileHandle aedsf;
		aedsf = Gdx.files.local("./res");
		System.out.println(" local "+aedsf+" "+aedsf.exists()+" "+aedsf.length()+" "+aedsf.lastModified());
		aedsf = Gdx.files.internal("./res");
		System.out.println(" internal "+aedsf+" "+aedsf.exists()+" ");
		
		Gdx.input.setCatchBackKey(true);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);  
		Graphics.init();
		
		Opsu.main(new String[0]);
		sbg = new Opsu("fkopsu!");
		try {
			sbg.init();
		} catch (SlickException e) {
			e.printStackTrace();
		}
		Gdx.input.setInputProcessor(sbg);
	}
	
}
