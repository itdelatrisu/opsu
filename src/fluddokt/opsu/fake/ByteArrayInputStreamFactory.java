package fluddokt.opsu.fake;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ByteArrayInputStreamFactory implements InputStreamFactory {

	byte[] buf;
	public ByteArrayInputStreamFactory(byte[] buf) {
		this.buf = buf;
	}

	@Override
	public InputStream getNewInputStream() {
		return new ByteArrayInputStream(buf);
	}

}
