package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class FileSystemLocation {

	File f;

	FileHandle internal,external,local;
	boolean internalExist,externalExist,localExist;
	public FileSystemLocation(File dir) {
		f = dir;
		internal = Gdx.files.internal(f.getPath());
		external = Gdx.files.external(f.getPath());
		local = Gdx.files.local(f.getPath());
		internalExist = internal.exists();
		externalExist = external.exists();
		localExist = local.exists();
		
		
	}
}
