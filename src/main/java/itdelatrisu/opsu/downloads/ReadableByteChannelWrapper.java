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

package itdelatrisu.opsu.downloads;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Wrapper for a ReadableByteChannel that stores the number of bytes read.
 * @author par (http://stackoverflow.com/a/11068356)
 */
public class ReadableByteChannelWrapper implements ReadableByteChannel {
	/** The wrapped ReadableByteChannel. */
	private final ReadableByteChannel rbc;

	/** The number of bytes read. */
	private long bytesRead;

	/**
	 * Constructor.
	 * @param rbc the ReadableByteChannel to wrap
	 */
	public ReadableByteChannelWrapper(ReadableByteChannel rbc) {
		this.rbc = rbc;
	}

	@Override
	public void close() throws IOException { rbc.close(); }

	@Override
	public boolean isOpen() { return rbc.isOpen(); }

	@Override
	public int read(ByteBuffer bb) throws IOException {
		int bytes;
		if ((bytes = rbc.read(bb)) > 0)
			bytesRead += bytes;
		return bytes;
	}

	/**
	 * Returns the number of bytes read so far.
	 */
	public long getReadSoFar() { return bytesRead; }
}
