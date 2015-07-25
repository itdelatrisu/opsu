package fluddokt.opsu.fake.gl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.badlogic.gdx.utils.BufferUtils;

public class UtilBuff {
	static IntBuffer ibuf = BufferUtils.newIntBuffer(16);
	static ByteBuffer bbuf = BufferUtils.newByteBuffer(16);
	
	public static IntBuffer prepare() {
		ibuf.clear();
		return ibuf;
	}
	public static int get() {
		
		//buf.flip();
		int t =  ibuf.get();
		//Thread.dumpStack();
		//System.out.println("Get: "+t);
		return t;
	}
	public static IntBuffer prepare(int n) {
		Thread.dumpStack();
		ibuf.clear();
		ibuf.put(n);
		ibuf.flip();
		return ibuf;
	}
	
	public static ByteBuffer prepareByte() {
		bbuf.clear();
		return bbuf;
	}
	public static int getByte() {
		
		int t =  bbuf.get();
		//Thread.dumpStack();
		//System.out.println("Get Byte: "+t);
		return t;
	}
	public static ByteBuffer prepareByte(byte n) {
		Thread.dumpStack();
		bbuf.clear();
		bbuf.put(n);
		bbuf.flip();
		return bbuf;
	}
}
