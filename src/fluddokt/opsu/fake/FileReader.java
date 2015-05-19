package fluddokt.opsu.fake;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

public class FileReader extends Reader {

	java.io.FileReader fr;
	public FileReader(File file) throws FileNotFoundException {
		fr = new java.io.FileReader(file.getIOFile());
	}

	@Override
	public void close() throws IOException {
		fr.close();
	}

	@Override
	public int read(char[] cbuf, int offset, int length) throws IOException {
		return fr.read(cbuf, offset, length);
	}

}
