package fluddokt.opsu.fake;

public abstract class AbsMusic {
	
	AbsMusicCompleteListener lis;
	public AbsMusic(AbsMusicCompleteListener lis) {
		this.lis = lis;
	}

	public abstract boolean setPosition(float f);

	public abstract void loop() ;

	public abstract void play() ;

	public abstract boolean playing() ;

	public abstract void pause() ;

	public abstract void resume();

	public abstract void setVolume(float volume) ;

	public abstract void stop() ;

	public void volFade(int duration, float f, boolean b){
		pause();
	}

	public abstract float getPosition() ;

	public abstract void dispose() ;

	public abstract String getName();
	
	protected void fireMusicEnded(){
		lis.complete(this);
	}

	public abstract void setPitch(float pitch);

	public void pitchFade(int duration, float endPitch) {	}

}
