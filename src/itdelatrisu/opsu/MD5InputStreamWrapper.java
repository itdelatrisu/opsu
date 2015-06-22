package itdelatrisu.opsu;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5InputStreamWrapper extends InputStream {

	InputStream in;
	private boolean eof; // End Of File
	MessageDigest md;
	public MD5InputStreamWrapper(InputStream in) throws NoSuchAlgorithmException {
		this.in = in;
		md = MessageDigest.getInstance("MD5");
	}

	@Override
	public int read() throws IOException {
		int readed = in.read();
		if(readed>=0)
			md.update((byte) readed);
		else
			eof=true;
		return readed;
	}

	@Override
	public int available() throws IOException {
		return in.available();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int readed = in.read(b, off, len);
		if(readed>=0)
			md.update(b, off, readed);
		else
			eof=true;
		
		return readed;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0 ,b.length);
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new RuntimeException("MD5 stream not resetable");
	}

	@Override
	public long skip(long n) throws IOException {
		throw new RuntimeException("MD5 stream not skipable");
	}

	public String getMD5() throws IOException {
		byte[] buf = null;
		if(!eof)
			buf = new byte[0x1000];
		while(!eof){
			read(buf);
		}
		
		byte[] md5byte = md.digest();
		StringBuilder result = new StringBuilder();
		for (byte b : md5byte)
			result.append(String.format("%02x", b));
		//System.out.println("MD5 stream md5 " + result.toString());
		return result.toString();
		
	}

}
