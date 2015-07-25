package fluddokt.opsu.fake.gl;

import java.nio.FloatBuffer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class GL15 {

	public static final int GL_ARRAY_BUFFER_BINDING = GL20.GL_ARRAY_BUFFER_BINDING;
	public static final int GL_ARRAY_BUFFER = GL20.GL_ARRAY_BUFFER;
	public static final int GL_STATIC_DRAW = GL20.GL_STATIC_DRAW;

	public static void glBindBuffer(int target, int buffer) {
		Gdx.gl20.glBindBuffer(target, buffer);
	}
	public static int glGenBuffers() {
		return Gdx.gl20.glGenBuffer();
	}
	public static void glBufferData(int target, FloatBuffer buff, int usage) {
		Gdx.gl20.glBufferData(target, buff.limit()*4, buff, usage);
	}
	public static void glDeleteBuffers(int buffer) {
		Gdx.gl20.glDeleteBuffer(buffer);
	}

}
