package fluddokt.opsu.fake;

import java.net.URL;

public class MultiClip extends Clip{

	public MultiClip(String path) {
		super(path);
	}

	public MultiClip(URL url, boolean isMP3, LineListener listener) {
		super(url, isMP3, listener);
	}

	public static void destroyExtraClips() {
		// TODO Auto-generated method stub
		
	}


}
