package itdelatrisu.opsu.fake;

import java.io.BufferedReader;



import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class File {

	public static final String separator = "/";
	FileHandle fh;

	public File(String name) {
		fh = Gdx.files.external(name);
		// TODO Auto-generated constructor stub
	}
	private File() {
	// TODO Auto-generated constructor stub
	}

	public File(File parent, String child) {
		fh = parent.fh.child(child);
		// TODO Auto-generated constructor stub
	}

	public boolean isFile() {
		// TODO Auto-generated method stub
		return fh.exists() && !fh.isDirectory();
	}

	public String getAbsolutePath() {
		// TODO Auto-generated method stub
		return null;
	}

	public static File internal(String name) {
		File fi = new File();
		fi.fh = Gdx.files.internal(name);
		return fi;
	}
	public static File external(String name) {
		File fi = new File();
		fi.fh = Gdx.files.external(name);
		return fi;
	}
	public static File local(String name) {
		File fi = new File();
		fi.fh = Gdx.files.local(name);
		return fi;
	}

	public boolean isDirectory() {
		// TODO Auto-generated method stub
		return false;
	}

	public File[] listFiles() {
		// TODO Auto-generated method stub
		return null;
	}
	public File[] listFiles(FilenameFilter filenameFilter) {
		// TODO Auto-generated method stub
		return null;
	}

	public File getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	

	public BufferedReader reader(int i, String string) {
		// TODO Auto-generated method stub
		return null;
	}

	public BufferedReader reader(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	public static File createTempFile(String string, String string2,
			java.io.File tmpDir) {
		// TODO Auto-generated method stub
		return null;
	}

	public void delete() {
		// TODO Auto-generated method stub
		
	}

	public void deleteOnExit() {
		// TODO Auto-generated method stub
		
	}

	public File getParentFile() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPath() {
		return fh.path();
	}

	public boolean mkdir() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	public java.io.File getIOFile() {
		// TODO Auto-generated method stub
		return null;
	}

	

	

}
