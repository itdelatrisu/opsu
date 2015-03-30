package fluddokt.opsu.fake;

import java.io.Reader;
import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class ResourceLoader {

	static LinkedList<FileSystemLocation> loc = new LinkedList<FileSystemLocation>();

	public static void removeAllResourceLocations() {
		loc.clear();
	}

	public static FileHandle getFileHandle(String file) {
		if (file == null)
			throw new Error("null file name");
		//System.out.print("ResourceLoader: getFileHandle:" + file + " ");
		FileHandle fh = Gdx.files.absolute(file);
		if (fh.exists()) {
			//System.out.println("QFOUNDED");
			return fh;
		}
		for (FileSystemLocation t : loc) {
			FileHandle child;
			if (t.localExist){
				child = t.local.child(file);
				if (child.exists()) {
					//System.out.println("FOUNDED local " + t.f);
					return child;
				}
			}

			if (t.externalExist){
				child = t.external.child(file);
				if (child.exists()) {
					//System.out.println("FOUNDED external " + t.f);
					return child;
				}
			}

			if (t.internalExist){
				try {
					child = t.internal.child(file);
					if (child.exists()) {
						//System.out.println("FOUNDED internal " + t.f);
						return child;
					}
				} catch (Exception e) {
					System.out.println("ResourceLoader Internal Fail: "+t.f+" "+file+" "+e);
					//e.printStackTrace();
				}
			}
			
		}
		//System.out.println("CNF");
		return null;
	}

	public static void addResourceLocation(FileSystemLocation fileSystemLocation) {
		loc.add(fileSystemLocation);
	}

	public static boolean resourceExists(String string) {
		return getFileHandle(string) != null;
	}

	public static Reader getResourceAsStream(String file) {
		return getFileHandle(file).reader();
	}

}
