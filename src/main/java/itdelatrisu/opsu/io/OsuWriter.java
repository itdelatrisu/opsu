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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Writer for osu! file types.
 */
public class OsuWriter {
	/** Output stream writer. */
	private DataOutputStream writer;

	/**
	 * Constructor.
	 * @param file the file to write to
	 * @throws FileNotFoundException
	 */
	public OsuWriter(File file) throws FileNotFoundException {
		this(new FileOutputStream(file));
	}

	/**
	 * Constructor.
	 * @param dest the output stream to write to
	 */
	public OsuWriter(OutputStream dest) {
		this.writer = new DataOutputStream(new BufferedOutputStream(dest));
	}

	/**
	 * Returns the output stream in use.
	 */
	public OutputStream getOutputStream() { return writer; }

	/**
	 * Closes the output stream.
	 * @throws IOException if an I/O error occurs
	 */
	public void close() throws IOException { writer.close(); }

	/**
	 * Writes a 1-byte value.
	 */
	public void write(byte v) throws IOException { writer.writeByte(v); }

	/**
	 * Writes a 2-byte value.
	 */
	public void write(short v) throws IOException {
		byte[] bytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array();
		writer.write(bytes);
	}

	/**
	 * Writes a 4-byte value.
	 */
	public void write(int v) throws IOException {
		byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array();
		writer.write(bytes);
	}

	/**
	 * Writes an 8-byte value.
	 */
	public void write(long v) throws IOException {
		byte[] bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(v).array();
		writer.write(bytes);
	}

	/**
	 * Writes a 4-byte float.
	 */
	public void write(float v) throws IOException { writer.writeFloat(v); }

	/**
	 * Writes an 8-byte double.
	 */
	public void write(double v) throws IOException { writer.writeDouble(v); }

	/**
	 * Writes a boolean as a 1-byte value.
	 */
	public void write(boolean v) throws IOException { writer.writeBoolean(v); }

	/**
	 * Writes an unsigned variable length integer (ULEB128).
	 */
	public void writeULEB128(int i) throws IOException {
		int value = i;
		do {
			byte b = (byte) (value & 0x7F);
			value >>= 7;
			if (value != 0)
				b |= (1 << 7);
			writer.writeByte(b);
		} while (value != 0);
	}

	/**
	 * Writes a variable-length string of 1-byte characters.
	 */
	public void write(String s) throws IOException {
		// 00 = empty string
		// 0B <length> <char>* = normal string
		// <length> is encoded as an LEB, and is the byte length of the rest.
		// <char>* is encoded as UTF8, and is the string content.
		if (s == null || s.length() == 0)
			writer.writeByte(0x00);
		else {
			writer.writeByte(0x0B);
			writeULEB128(s.length());
			writer.writeBytes(s);
		}
	}

	/**
	 * Writes a date in Windows ticks (8 bytes).
	 */
	public void write(Date date) throws IOException {
		final long TICKS_AT_EPOCH = 621355968000000000L;
		final long TICKS_PER_MILLISECOND = 10000;

		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTime(date);
		long ticks = TICKS_AT_EPOCH + calendar.getTimeInMillis() * TICKS_PER_MILLISECOND;
		write(ticks);
	}

	/**
	 * Writes an array of bytes.
	 */
	public void write(byte[] b) throws IOException {
		writer.write(b);
	}
}
