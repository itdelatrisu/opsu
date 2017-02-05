package fluddokt.opsu.fake;

public class InternalTextureLoader {

	public static InternalTextureLoader get() {
		return new InternalTextureLoader();
	}

	public void clear() {
		Image.clearAll();
	}

}
