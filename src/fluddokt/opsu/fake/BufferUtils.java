package fluddokt.opsu.fake;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class BufferUtils {

	public static IntBuffer createIntBuffer(int i) {
		return com.badlogic.gdx.utils.BufferUtils.newIntBuffer(i);
	}

	public static FloatBuffer createFloatBuffer(int i) {
		return com.badlogic.gdx.utils.BufferUtils.newFloatBuffer(i);
	}
	public static ByteBuffer createByteBuffer(int i) {
		return com.badlogic.gdx.utils.BufferUtils.newByteBuffer(i);
	}

	public static ShortBuffer createShortBuffer(int i) {
		return com.badlogic.gdx.utils.BufferUtils.newShortBuffer(i);
	}

}
