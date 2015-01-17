package fluddokt.opsu.fake;

import java.io.BufferedReader;



import java.io.FileFilter;
import java.io.FilenameFilter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class File {

	public static final String separator = "/";
	FileHandle fh;

	public File(String name) {
		fh = Gdx.files.external(name);
	}
	private File(FileHandle nfh) {
		this.fh = nfh;
	}

	public File(File parent, String child) {
		fh = parent.fh.child(child);
	}

	public boolean isFile() {
		return fh.exists() && !fh.isDirectory();
	}

	public String getAbsolutePath() {
		// TODO Auto-generated method stub
		return null;
	}

	public static File internal(String name) {
		return new File(Gdx.files.internal(name));
	}
	public static File external(String name) {
		return new File(Gdx.files.external(name));
		}
	public static File local(String name) {
		return new File(Gdx.files.local(name));
		}

	public boolean isDirectory() {
		return fh.isDirectory();
	}

	public File[] listFiles() {
		return consturctList(fh.list());
	}
	public File[] listFiles(FilenameFilter filenameFilter) {
		return consturctList(fh.list(filenameFilter));
	}
	public File[] listFiles(FileFilter fileFilter) {
		return consturctList(fh.list(fileFilter));
	}
	private File[] consturctList(FileHandle[] list){
		if(list == null)
			return null;
		File[] newlist = new File[list.length];
		for(int i=0;i<list.length;i++)
			newlist[i] = new File(list[i]);
		return newlist;
	}

	public String getParent() {
		return new File(fh.parent()).getPath();
	}

	public File getParentFile() {
		return new File(fh.parent());
		}
	public String getName() {
		return fh.name();
	}

	

	public BufferedReader reader(int bufferSize, String charset) {
		return fh.reader(bufferSize, charset);
	}

	public BufferedReader reader(int bufferSize) {
		return fh.reader(bufferSize);
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


	public String getPath() {
		return fh.path();
	}

	public boolean mkdir() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean exists() {
		return fh.exists();
	}

	public java.io.File getIOFile() {
		return new java.io.File(fh.path());
	}

	public String toString(){
		return "File:"+getPath();
	}

	

}
