package fluddokt.opsu.fake;

public class Music {

	AbsMusic music;
	public Music(String path){
		//music = new MusicGdx(path);
		music = new MusicJL(path);
		GameContainer.setMusic(this);
	}

	public boolean setPosition(float f){
		return music.setPosition(f);}

	public void loop(){music.loop();}

	public void play(){music.play();}

	public boolean playing(){
		return music.playing();}

	public void pause(){music.pause();}

	public void resume(){music.resume();}

	public void setVolume(float volume){music.setVolume(volume);}

	public void stop(){music.stop();}

	public void fade(int duration, float f, boolean b){music.fade(duration, f, b);}

	public float getPosition(){
		return music.getPosition();}
	

	public void dispose(){music.dispose();}

}
