package fluddokt.opsu.fake.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class GL30 {

	public static final int GL_FRAMEBUFFER_BINDING = GL20.GL_FRAMEBUFFER_BINDING;
	//public static final int GL_DRAW_FRAMEBUFFER = com.badlogic.gdx.graphics.GL30.GL_DRAW_FRAMEBUFFER;
	public static final int GL_DRAW_FRAMEBUFFER = com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER;
	public static final int GL_READ_FRAMEBUFFER_BINDING = com.badlogic.gdx.graphics.GL30.GL_READ_FRAMEBUFFER_BINDING;
	public static final int GL_RENDERBUFFER = GL20.GL_RENDERBUFFER;
	public static final int GL_DEPTH_ATTACHMENT = GL20.GL_DEPTH_ATTACHMENT;
	public static final int GL_FRAMEBUFFER = GL20.GL_FRAMEBUFFER;
	public static final int GL_COLOR_ATTACHMENT0 = GL20.GL_COLOR_ATTACHMENT0;

	public static void glBindFramebuffer(int target, int framebuffer) {
		Gdx.gl20.glBindFramebuffer(target, framebuffer);
	}
	public static void glGenerateMipmap(int target) {
		Gdx.gl20.glGenerateMipmap(target);
	}
	public static void glBindRenderbuffer(int target, int renderbuffer) {
		Gdx.gl20.glBindRenderbuffer(target, renderbuffer);
	}
	public static void glRenderbufferStorage(int target,
			int internalformat, int width, int height) {
		Gdx.gl20.glRenderbufferStorage(target, internalformat, width, height);
	}
	public static void glFramebufferRenderbuffer(int target,
			int attachment, int renderbuffertarget, int renderbuffer) {
		Gdx.gl20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
	}
	public static void glDeleteFramebuffers(int framebuffer) {
		Gdx.gl20.glDeleteFramebuffer(framebuffer);
	}
	public static void glDeleteRenderbuffers(int renderbuffer) {
		Gdx.gl20.glDeleteRenderbuffer(renderbuffer);
	}
	public static int glGenFramebuffers() {
		return Gdx.gl20.glGenFramebuffer();
	}
	public static int glGenRenderbuffers() {
		return Gdx.gl20.glGenRenderbuffer();
	}

}
