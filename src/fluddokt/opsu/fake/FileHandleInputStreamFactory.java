package fluddokt.opsu.fake;

import java.io.InputStream;

import com.badlogic.gdx.files.FileHandle;

public class FileHandleInputStreamFactory implements InputStreamFactory {

	private FileHandle file;

	public FileHandleInputStreamFactory(FileHandle file) {
		this.file = file;
	}

	@Override
	public InputStream getNewInputStream() {
		return file.read();
	}

}
