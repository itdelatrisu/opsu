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

package itdelatrisu.opsu.io;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Wrapper for an InputStream that computes the MD5 hash while reading the stream.
 */
public class MD5InputStreamWrapper extends InputStream {
	/** The input stream. */
	private InputStream in;

	/** Whether the end of stream has been reached. */
	private boolean eof = false;

	/** A MessageDigest object that implements the MD5 digest algorithm. */
	private MessageDigest md;

	/** The computed MD5 hash. */
	private String md5;

	/**
	 * Constructor.
	 * @param in the input stream
	 * @throws NoSuchAlgorithmException if no Provider supports a MessageDigestSpi implementation for the MD5 algorithm
	 */
	public MD5InputStreamWrapper(InputStream in) throws NoSuchAlgorithmException {
		this.in = in;
		this.md = MessageDigest.getInstance("MD5");
	}

	@Override
	public int read() throws IOException {
		int bytesRead = in.read();
		if (bytesRead >= 0)
			md.update((byte) bytesRead);
		else
			eof = true;
		return bytesRead;
	}

	@Override
	public int available() throws IOException { return in.available(); }

	@Override
	public void close() throws IOException { in.close(); }

	@Override
	public synchronized void mark(int readlimit) { in.mark(readlimit); }

	@Override
	public boolean markSupported() { return in.markSupported(); }

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int bytesRead = in.read(b, off, len);
		if (bytesRead >= 0)
			md.update(b, off, bytesRead);
		else
			eof = true;
		return bytesRead;
	}

	@Override
	public int read(byte[] b) throws IOException { return read(b, 0, b.length); }

	@Override
	public synchronized void reset() throws IOException {
		throw new RuntimeException("The reset() method is not implemented.");
	}

	@Override
	public long skip(long n) throws IOException {
		throw new RuntimeException("The skip() method is not implemented.");
	}

	/**
	 * Returns the MD5 hash of the input stream.
	 * @throws IOException if the end of stream has not yet been reached and a call to {@link #read(byte[])} fails
	 */
	public String getMD5() throws IOException {
		if (md5 != null)
			return md5;

		if (!eof) {  // read the rest of the stream
			byte[] buf = new byte[0x1000];
			while (!eof)
				read(buf);
		}

		byte[] md5byte = md.digest();
		StringBuilder result = new StringBuilder();
		for (byte b : md5byte)
			result.append(String.format("%02x", b));
		md5 = result.toString();
		return md5;
	}
}
