package itdelatrisu.opsu.fake;

import java.io.File;

public class ClasspathLocation extends FileSystemLocation {

	public ClasspathLocation() {
		super(new File("."));
	}

}
