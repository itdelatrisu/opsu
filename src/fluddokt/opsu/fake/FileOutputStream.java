package fluddokt.opsu.fake;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class FileOutputStream extends OutputStream {

	java.io.OutputStream out;

	public FileOutputStream(File file, boolean b) throws FileNotFoundException {
		out = file.fh.write(b);
	}

	public FileOutputStream(File file) throws FileNotFoundException {
		this(file, false);
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}
	
	@Override
	public void close() throws IOException {
		out.close();
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		out.write(b);
	}

	

}
