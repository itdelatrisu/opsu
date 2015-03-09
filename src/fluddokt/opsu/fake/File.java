package fluddokt.opsu.fake;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileFilter;
import java.io.FilenameFilter;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;

public class File {

	public static final String separator = "/";
	FileHandle fh;

	public File(String name) {
		if (Gdx.app.getType() == ApplicationType.Desktop)
			fh = Gdx.files.local(name);
		else
			fh = Gdx.files.external(name);
		if (fh.exists()) {
			System.out.println("new file:"+info());
			return;
		}
		
		if(Gdx.files.isExternalStorageAvailable()){
			fh = Gdx.files.external(name);
		} else if(Gdx.files.isLocalStorageAvailable()){
			fh = Gdx.files.local(name);
		} else {
			//no storage available ...
		}
		if (fh.exists()) {
			System.out.println("new file:"+info());
			return;
		}
		
		fh = Gdx.files.absolute(name);
		if (fh.exists()) {
			System.out.println("new file:"+info());
			return;
		}
		
		fh = Gdx.files.internal(name);
		if (fh.exists()) {
			System.out.println("new file:"+info());
			return;
		}

		if(Gdx.files.isExternalStorageAvailable()){
			fh = Gdx.files.external(name);
		} else if(Gdx.files.isLocalStorageAvailable()){
			fh = Gdx.files.local(name);
		} else {
			//no storage available ...
		}
		//fh = Gdx.files.external(name);
		System.out.println("new nonexist file:"+info());
		
	}

	private File(FileHandle nfh) {
		this.fh = nfh;
	}

	public File(File parent, String child) {
		System.out.println("new child file:"+parent.info()+" "+child);
		fh = parent.fh.child(child);
	}

	private String info() {
		return fh.path()+"["+fh.type();
	}

	public File(String parent, String child) {
		this(new File(parent), child);
	}

	public boolean isFile() {
		return fh.exists() && !fh.isDirectory();
	}

	public String getAbsolutePath() {
		return getFullPath();
	}

	public String getFullPath() {
		String par;
		if (fh.type() == FileType.Local)
			par = Gdx.files.getLocalStoragePath();
		else if (fh.type() == FileType.External)
			par = Gdx.files.getExternalStoragePath();
		else
			par = "";
		return par + fh.path();
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

	private File[] consturctList(FileHandle[] list) {
		if (list == null)
			return null;
		File[] newlist = new File[list.length];
		for (int i = 0; i < list.length; i++)
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

	public void delete() {
		fh.delete();
	}

	public String getPath() {
		return fh.path();
	}

	public boolean mkdir() {
		fh.mkdirs();
		return true;
	}

	public boolean exists() {
		return fh.exists();
	}

	public java.io.File getIOFile() {
		return fh.file();
	}

	public String toString() {
		return getPath();
	}

	public BufferedWriter writer(String charset) {
		return new BufferedWriter(fh.writer(false, charset));
	}

	public boolean equals(File f) {
		return this.fh.path().equals(f.fh.path());
	}

	public long lastModified() {
		return fh.lastModified();
	}

	public String[] list() {
		return consturctStringList(fh.list());
	}
	private String[] consturctStringList(FileHandle[] list) {
		if (list == null)
			return null;
		String[] newlist = new String[list.length];
		for (int i = 0; i < list.length; i++)
			newlist[i] = list[i].name();
		return newlist;
	}

	public boolean isExternal() {
		return fh.type() == FileType.External|| 
				(
					fh.type() == FileType.Absolute 
					&& fh.path().contains(Gdx.files.getExternalStoragePath())
				);
	}

}
