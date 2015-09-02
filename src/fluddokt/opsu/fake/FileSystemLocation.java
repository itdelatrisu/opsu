package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class FileSystemLocation {

	File f;

	FileHandle internal,external,local;
	boolean internalExist,externalExist,localExist;
	public FileSystemLocation(File dir) {
		this(dir, false);
	}
	public FileSystemLocation(File dir, boolean tryInternal) {
		if (dir == null)
			return;
		f = dir;
		String path = f.getPath()+"/";
		internal = Gdx.files.internal(path);
		internalExist = tryInternal; //internal.exists() always returns false for dirs..
		
		external = Gdx.files.external(path);
		externalExist = external.exists();
		
		local = Gdx.files.local(path);
		localExist = local.exists();
		
		System.out.println("New FileSystemLocation: "+path);
		System.out.println("Internal: "+internal+" "+internalExist);
		System.out.println("External: "+external+" "+externalExist);
		System.out.println("Local: "+local+" "+localExist);
		
		
		
		
	}
}
