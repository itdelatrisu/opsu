package fluddokt.opsu.fake;

import java.io.BufferedReader;



import java.io.BufferedWriter;
//import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;

public class File {

	public static final String separator = "/";
	FileHandle fh;

	public File(String name) {
		fh = Gdx.files.external(name);
		if(!fh.exists()){
			fh = Gdx.files.absolute(name);
			if(!fh.exists()){
				fh = Gdx.files.local(name);
				if(!fh.exists()){
					fh = Gdx.files.external(name);
				}
			}
		}
		//System.out.println("New File: "+name+" "+fh.type());
	}
	private File(FileHandle nfh) {
		this.fh = nfh;
	}

	public File(File parent, String child) {
		fh = parent.fh.child(child);
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
	public String getFullPath(){
		String par;
		if(fh.type() == FileType.Local)
			par = Gdx.files.getLocalStoragePath();
		else if(fh.type() == FileType.External)
			par = Gdx.files.getExternalStoragePath();
		//else if(fh.type() == FileType.Internal)
		//	par = Gdx.files.g
		else
			par = "";
		return par+fh.path();
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
		fh.delete();
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
		fh.mkdirs();
		return true;
	}

	public boolean exists() {
		return fh.exists();
	}

	public java.io.File getIOFile() {
		return fh.file();
	}

	public String toString(){
		return getPath();
	}
	public BufferedWriter writer(String charset) {
		return new BufferedWriter(fh.writer(false, charset));
	}

	public boolean equals(File f){
		return this.fh.path().equals(f.fh.path());
	}


}
