package fluddokt.opsu.fake;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UrlInputStreamFactory implements InputStreamFactory {

	URL url;
	public UrlInputStreamFactory(URL url) {
		this.url = url;
	}

	@Override
	public InputStream getNewInputStream() {
		try {
			return new BufferedInputStream(url.openStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
