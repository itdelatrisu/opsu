package fluddokt.opsu.fake;

public abstract class ClipImplementation {

	public abstract void stop();

	public abstract int play(float volume, LineListener listener);

	public abstract void destroy() ;

}
