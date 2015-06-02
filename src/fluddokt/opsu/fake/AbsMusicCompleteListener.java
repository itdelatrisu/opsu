package fluddokt.opsu.fake;

interface AbsMusicCompleteListener {

	public abstract void complete(AbsMusic mus);

	public void requestSync(AbsMusic mus);
	
}