package fluddokt.opsu.fake.openal;

import java.io.IOException;


public interface AudioInputStreamFactory {
	public AudioInputStream2 getNewAudioInputStream() throws IOException;
}
