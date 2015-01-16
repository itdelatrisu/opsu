package itdelatrisu.opsu.fake;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class ResourceLoader {

	static LinkedList<FileSystemLocation> loc = new LinkedList<FileSystemLocation>();
	public static URL getResource(String ref) {
		// TODO Auto-generated method stub
		return null;
	}

	public static InputStream getResourceAsStream(String fontName) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void removeAllResourceLocations() {
		// TODO Auto-generated method stub
		
	}

	public static FileHandle getFileHandle(String file) {
		if(file == null)
			throw new Error("null file name");
		System.out.print("getFileHandle:"+file+" ");
		FileHandle fh = Gdx.files.absolute(file);
		if(fh.exists()){
			System.out.println("QFOUNDED");
			return fh;
		}
		for(FileSystemLocation t: loc){
			//FileHandle aedsf = Gdx.files.local(t.f.getPath());
			//
			//System.out.println(t.fh+" local "+aedsf+" "+aedsf.exists()+" "+aedsf.length()+" "+aedsf.lastModified());
			//aedsf = Gdx.files.internal(t.f.getPath());
			//System.out.println(t.fh+" internal "+aedsf+" "+aedsf.exists()+" ");
			
			FileHandle child =  Gdx.files.local(t.f.getPath()).child(file);//t.child(file);
			//System.out.println(t.fh+" "+child+" "+child.exists()+" "+child.length()+" "+child.lastModified());
			if(child.exists()){
				System.out.println("FOUNDED");
				return child;
			}
			child =  Gdx.files.internal(t.f.getPath()).child(file);//t.child(file);
			//System.out.println(t.fh+" "+child+" "+child.exists());//+" "+child.length()+" "+child.lastModified());
			if(child.exists()){
				System.out.println("FOUNDED");
				return child;
			}
			
			child =  Gdx.files.external(t.f.getPath()).child(file);//t.child(file);
			//System.out.println(t.fh+" "+child+" "+child.exists());//+" "+child.length()+" "+child.lastModified());
			if(child.exists()){
				System.out.println("FOUNDED");
				return child;
			}
		}
		System.out.println("CNF");
		return null;
	}

	public static void addResourceLocation(FileSystemLocation fileSystemLocation) {
		// TODO Auto-generated method stub
		loc.add(fileSystemLocation);
		
	}

}
