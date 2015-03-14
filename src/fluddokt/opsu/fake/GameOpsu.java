package fluddokt.opsu.fake;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Options;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class GameOpsu extends com.badlogic.gdx.Game {

	final String VERSION = "OpsuAnd0.7.0a";
	public StateBasedGame sbg;
	
	Stage stage;
	Table table;
	Skin skin;
	static GameOpsu gameOpsu;
	
	boolean inited = false;

	private boolean pause;
	public GameOpsu() {
		gameOpsu = this;
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
		sbg.gc.close_sub();
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
		if(!pause){
		try{
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
					error("SlickErrorInit", e);
				}
				File dataDir = Options.DATA_DIR;
				System.out.println("dataDir :"+dataDir+" "+dataDir.isExternal()+" "+dataDir.exists());
				if(dataDir.isExternal()){
					File nomediafile = new File(dataDir, ".nomedia");
					if(!nomediafile.exists())
						new FileOutputStream(nomediafile.getIOFile()).close();
				}
				Gdx.input.setInputProcessor(new InputMultiplexer(stage, sbg));
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
				error("SlickErrorRender", e);
			}
			Graphics.checkMode(0);
		}
		}catch(Throwable e){
			e.printStackTrace();
			error("RenderError", e);
		}
		}else{
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		}
		
		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		System.out.println("Game resize" + width + " " + height);

		super.resize(width, height);
		Graphics.resize(width, height);
		if(!inited)
			return;
		sbg.gc.width = width;
		sbg.gc.height = height;
		stage.getViewport().update(width, height);
	}

	@Override
	public void create() {

		stage = new Stage();
		Gdx.input.setInputProcessor(stage);
		table = new Table();
		table.setFillParent(true);
		stage.addActor(table);
		
		skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				ErrorHandler.error("** Uncaught Exception! A **", e, true);
			}
		});
		
		if(!Gdx.files.isExternalStorageAvailable()){
			if(!Gdx.files.isLocalStorageAvailable()){
				error("No storage is available ... ????", null);
			}else{
				error("External Storage is not available. \n"
						+"Using Local Storage instead." , null);
			}
		}
		Gdx.graphics.setVSync(false);
		Gdx.input.setCatchBackKey(true);
		
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		
		
		Graphics.init();
		
	}

	public static void error(String string, Throwable e) {
		gameOpsu.errorDialog(string, e);
	}

	private void errorDialog(final String string, final Throwable e) {
		pause = true;
		String tbodyString = "X";
		if(e != null){
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			tbodyString = sw.toString();
		}
		final String bodyString = tbodyString;
		Dialog dialog = new Dialog("ERROR", skin){
			final String title = string;
			final String body = bodyString;
			@Override
			protected void result(Object object) {
				System.out.println(object);
				if("CloseOpsu".equals(object)){
					System.exit(0);
				}
				
				if("R".equals(object)){
					try {
						System.out.println("Reporting");
						Desktop.getDesktop().browse(
								new URI("https://github.com/fluddokt/opsu/issues/new?"
									+"title="+java.net.URLEncoder.encode(title, "UTF-8")
									+"&body="+java.net.URLEncoder.encode(title+"\n"+VERSION+"\n"+body ,"UTF-8")
								)
						);
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				pause = false;
				if("S".equals(object)){
					
				}
				
			}
			
		}.button("Close this Dialog Box and Continue","S").button("Report on github","R").button("Close Opsu", "CloseOpsu");;
		dialog.getContentTable().row();
		Label tex =new Label(string+"\n"+bodyString, skin);
		
		dialog.getContentTable().add(new ScrollPane(tex)).width(Gdx.graphics.getWidth());
		dialog.pack();
		table.addActor(dialog);
		table.validate();
		
		
	}

}
