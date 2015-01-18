package fluddokt.opsu.fake;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class FileOutputStream extends OutputStream {

	java.io.OutputStream out;
	public FileOutputStream(File file, boolean b) throws FileNotFoundException{
		out = file.fh.write(b);
	}
	@Override
	public void write(int arg0) throws IOException {
		out.write(arg0);
	}

}
