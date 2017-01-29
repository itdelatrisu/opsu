/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2017 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu;

import itdelatrisu.opsu.video.FFmpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Native loader, based on the JarSplice launcher.
 *
 * @author http://ninjacave.com
 */
public class NativeLoader {
	/** The directory to unpack natives to. */
	private final File nativeDir;

	/**
	 * Constructor.
	 * @param dir the directory to unpack natives to
	 */
	public NativeLoader(File dir) {
		nativeDir = dir;
	}

	/**
	 * Unpacks natives for the current operating system to the natives directory.
	 * @throws IOException if an I/O exception occurs
	 */
	public void loadNatives() throws IOException {
		if (!nativeDir.exists())
			nativeDir.mkdir();

		JarFile jarFile = Utils.getJarFile();
		if (jarFile == null)
			return;

		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry e = entries.nextElement();
			if (e == null)
				break;

			File f = new File(nativeDir, e.getName());
			if (isNativeFile(e.getName()) && !e.isDirectory() && e.getName().indexOf('/') == -1 && !f.exists()) {
				InputStream in = jarFile.getInputStream(jarFile.getEntry(e.getName()));
				OutputStream out = new FileOutputStream(f);

				byte[] buffer = new byte[65536];
				int bufferSize;
				while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1)
					out.write(buffer, 0, bufferSize);

				in.close();
				out.close();
			}
		}

		jarFile.close();
	}

	/**
	 * Returns whether the given file name is a native file for the current operating system.
	 * @param entryName the file name
	 * @return true if the file is a native that should be loaded, false otherwise
	 */
	private boolean isNativeFile(String entryName) {
		String osName = System.getProperty("os.name");
		String name = entryName.toLowerCase();

		if (osName.startsWith("Win")) {
			if (name.endsWith(".dll"))
				return true;
		} else if (osName.startsWith("Linux")) {
			if (name.endsWith(".so"))
				return true;
		} else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
			if (name.endsWith(".dylib") || name.endsWith(".jnilib"))
				return true;
		}

		if (name.equals(FFmpeg.DEFAULT_NATIVE_FILENAME.toLowerCase()))
			return true;

		return false;
	}
}