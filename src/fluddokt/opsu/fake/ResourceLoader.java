package fluddokt.opsu.fake;

import java.io.InputStream;
import java.net.URL;
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
		System.out.print("ResourceLoader: getFileHandle:" + file + " ");
		FileHandle fh = Gdx.files.absolute(file);
		if (fh.exists()) {
			System.out.println("QFOUNDED");
			return fh;
		}
		for (FileSystemLocation t : loc) {
			FileHandle child;

			child = Gdx.files.internal(t.f.getPath()).child(file);
			if (child.exists()) {
				System.out.println("FOUNDED internal " + t.f);
				return child;
			}

			child = Gdx.files.local(t.f.getPath()).child(file);
			if (child.exists()) {
				System.out.println("FOUNDED local " + t.f);
				return child;
			}

			child = Gdx.files.external(t.f.getPath()).child(file);
			if (child.exists()) {
				System.out.println("FOUNDED external " + t.f);
				return child;
			}
		}
		System.out.println("CNF");
		return null;
	}

	public static void addResourceLocation(FileSystemLocation fileSystemLocation) {
		loc.add(fileSystemLocation);
	}

}
