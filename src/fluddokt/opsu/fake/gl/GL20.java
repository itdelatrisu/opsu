package fluddokt.opsu.fake.gl;

import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.graphics.GL20;

public class GL20 {

	public static final int GL_CURRENT_PROGRAM = com.badlogic.gdx.graphics.GL20.GL_CURRENT_PROGRAM;
	public static final int GL_VERTEX_SHADER = com.badlogic.gdx.graphics.GL20.GL_VERTEX_SHADER;
	public static final int GL_FRAGMENT_SHADER = com.badlogic.gdx.graphics.GL20.GL_FRAGMENT_SHADER;
	public static final int GL_COMPILE_STATUS = com.badlogic.gdx.graphics.GL20.GL_COMPILE_STATUS;
	public static final int GL_LINK_STATUS = com.badlogic.gdx.graphics.GL20.GL_LINK_STATUS;
	public static final int GL_FRAMEBUFFER = com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER;
	public static final int GL_FRAMEBUFFER_COMPLETE =  com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER_COMPLETE;
	public static final int GL_FRAMEBUFFER_UNSUPPORTED =  com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER_UNSUPPORTED;

	public static void glUseProgram(int program) {
		//if(program == 0)
		//	program = GL11.program;
		Gdx.gl20.glUseProgram(program);
	}

	public static void glEnableVertexAttribArray(int index) {
		Gdx.gl20.glEnableVertexAttribArray(index);
	}

	public static void glUniform1i(int location, int x) {
		Gdx.gl20.glUniform1i(location, x);
	}

	public static void glUniform3f(int location, float x, float y, float z) {
		Gdx.gl20.glUniform3f(location, x, y, z);
	}

	public static void glUniform4f(int location, float x, float y, float z, float w) {
		Gdx.gl20.glUniform4f(location, x, y, z, w);
	}

	public static void glVertexAttribPointer(int indx, int size,
			int type, boolean normalized, int stride, int ptr) {
		Gdx.gl20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
	}

	public static void glDisableVertexAttribArray(int index) {
		Gdx.gl20.glDisableVertexAttribArray(index);
	}

	public static int glCreateProgram() {
		return Gdx.gl20.glCreateProgram();
	}

	public static int glCreateShader(int type) {
		return Gdx.gl20.glCreateShader(type);
	}

	public static void glShaderSource(int shader, String string) {
		Gdx.gl20.glShaderSource(shader, string);
	}

	public static int glGetShaderi(int shader, int pname) {
		Gdx.gl20.glGetShaderiv(shader, pname, UtilBuff.prepare());
		return UtilBuff.get();
	}

	public static String glGetShaderInfoLog(int shader, int i) {
		return Gdx.gl20.glGetShaderInfoLog(shader);
	}

	public static void glAttachShader(int program, int shader) {
		Gdx.gl20.glAttachShader(program, shader);
	}

	public static void glLinkProgram(int program) {
		Gdx.gl20.glLinkProgram(program);
	}

	public static int glGetProgrami(int program, int pname) {
		Gdx.gl20.glGetProgramiv(program, pname, UtilBuff.prepare());
		return UtilBuff.get();
	}

	public static String glGetProgramInfoLog(int program, int i) {
		return Gdx.gl20.glGetProgramInfoLog(program);
	}

	public static void glDeleteShader(int shader) {
		Gdx.gl20.glDeleteShader(shader);
	}

	public static int glGetAttribLocation(int program, String name) {
		return Gdx.gl20.glGetAttribLocation(program, name);
	}

	public static int glGetUniformLocation(int program, String name) {
		return Gdx.gl20.glGetUniformLocation(program, name);
	}

	public static void glDeleteProgram(int program) {
		 Gdx.gl20.glDeleteProgram(program);
	}

	public static void glCompileShader(int shader) {
		 Gdx.gl20.glCompileShader(shader);
		
	}

	public static int glCheckFramebufferStatus(int target) {
		return Gdx.gl20.glCheckFramebufferStatus(target);
	}

}
