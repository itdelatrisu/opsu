/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

/**
 * Reader for osu! file types.
 *
 * @author Markus Jarderot (http://stackoverflow.com/questions/28788616)
 */
public class OsuReader {
	/** Input stream reader. */
	private DataInputStream reader;

	/**
	 * Constructor.
	 * @param file the file to read from
	 * @throws IOException
	 */
	public OsuReader(File file) throws IOException {
		this(new FileInputStream(file));
	}

	/**
	 * Constructor.
	 * @param source the input stream to read from
	 */
	public OsuReader(InputStream source) {
		this.reader = new DataInputStream(new BufferedInputStream(source));
	}

	/**
	 * Returns the input stream in use.
	 */
	public InputStream getInputStream() { return reader; }

	/**
	 * Closes the input stream.
	 * @throws IOException if an I/O error occurs
	 */
	public void close() throws IOException { reader.close(); }

	/**
	 * Reads a 1-byte value.
	 */
	public byte readByte() throws IOException {
		return this.reader.readByte();
	}

	/**
	 * Reads a 2-byte little endian value.
	 */
	public short readShort() throws IOException {
		byte[] bytes = new byte[2];
		this.reader.readFully(bytes);
		ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		return bb.getShort();
	}

	/**
	 * Reads a 4-byte little endian value.
	 */
	public int readInt() throws IOException {
		byte[] bytes = new byte[4];
		this.reader.readFully(bytes);
		ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
	}

	/**
	 * Reads an 8-byte little endian value.
	 */
	public long readLong() throws IOException {
		byte[] bytes = new byte[8];
		this.reader.readFully(bytes);
		ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		return bb.getLong();
	}

	/**
	 * Reads a 4-byte little endian float.
	 */
	public float readSingle() throws IOException {
		byte[] bytes = new byte[4];
		this.reader.readFully(bytes);
		ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		return bb.getFloat();
	}

	/**
	 * Reads an 8-byte little endian double.
	 */
	public double readDouble() throws IOException {
		byte[] bytes = new byte[8];
		this.reader.readFully(bytes);
		ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		return bb.getDouble();
	}

	/**
	 * Reads a 1-byte value as a boolean.
	 */
	public boolean readBoolean() throws IOException {
		return this.reader.readBoolean();
	}

	/**
	 * Reads an unsigned variable length integer (ULEB128).
	 */
	public int readULEB128() throws IOException {
		int value = 0;
		for (int shift = 0; shift < 32; shift += 7) {
			byte b = this.reader.readByte();
			value |= (b & 0x7F) << shift;
			if (b >= 0)
				return value;  // MSB is zero. End of value.
		}
		throw new IOException("ULEB128 too large");
	}

	/**
	 * Reads a variable-length string of 1-byte characters.
	 */
	public String readString() throws IOException {
		// 00 = empty string
		// 0B <length> <char>* = normal string
		// <length> is encoded as an LEB, and is the byte length of the rest.
		// <char>* is encoded as UTF8, and is the string content.
		byte kind = this.reader.readByte();
		if (kind == 0)
			return "";
		if (kind != 0x0B)
			throw new IOException(String.format("String format error: Expected 0x0B or 0x00, found 0x%02X", kind & 0xFF));
		int length = readULEB128();
		if (length == 0)
			return "";
		byte[] utf8bytes = new byte[length];
		this.reader.readFully(utf8bytes);
		return new String(utf8bytes, "UTF-8");
	}

	/**
	 * Reads an 8-byte date in Windows ticks.
	 */
	public Date readDate() throws IOException {
		long ticks = readLong();
		final long TICKS_AT_EPOCH = 621355968000000000L;
		final long TICKS_PER_MILLISECOND = 10000;

		return new Date((ticks - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND);
	}
}
