package fluddokt.opsu.fake.gl;

import java.nio.ByteBuffer;

import com.badlogic.gdx.Gdx;
import fluddokt.opsu.fake.Log;

public class GL11 {
	public static final int GL_TEXTURE_BINDING_2D = com.badlogic.gdx.graphics.GL20.GL_TEXTURE_BINDING_2D;
	public static final int GL_TEXTURE_2D = com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D;
	public static final int GL_TEXTURE_1D = GL_TEXTURE_2D;//GL20.GL_TEXTURE_1D;
	
	public static final int GL_COLOR_BUFFER_BIT = com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
	public static final int GL_DEPTH_BUFFER_BIT = com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT;
	
	//public static final int GL_QUADS = 0x10;//GL20.GL_QUADS;
	public static final int GL_TRIANGLE_FAN = com.badlogic.gdx.graphics.GL20.GL_TRIANGLE_FAN;
	public static final int GL_TRIANGLES =  com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;
	public static final int GL_TRIANGLE_STRIP =  com.badlogic.gdx.graphics.GL20.GL_TRIANGLE_STRIP;

	public static final int GL_POLYGON_SMOOTH = 0;//GL20.GL_POLYGON_SMOOTH;
	public static final int GL_BLEND = com.badlogic.gdx.graphics.GL20.GL_BLEND;
	public static final int GL_ONE_MINUS_SRC_ALPHA = com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA;
	public static final int GL_SRC_ALPHA = com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA;
	
	public static final int GL_DEPTH_TEST = com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST;
	public static final int GL_DEPTH_WRITEMASK = com.badlogic.gdx.graphics.GL20.GL_DEPTH_WRITEMASK;
	
	public static final int GL_TEXTURE_MIN_FILTER = com.badlogic.gdx.graphics.GL20.GL_TEXTURE_MIN_FILTER;
	public static final int GL_TEXTURE_MAG_FILTER = com.badlogic.gdx.graphics.GL20.GL_TEXTURE_MAG_FILTER;
	
	public static final int GL_LINEAR_MIPMAP_LINEAR = com.badlogic.gdx.graphics.GL20.GL_LINEAR_MIPMAP_LINEAR;
	public static final int GL_LINEAR = com.badlogic.gdx.graphics.GL20.GL_LINEAR;
	public static final int GL_NEAREST = com.badlogic.gdx.graphics.GL20.GL_NEAREST;

	public static final int GL_TEXTURE_WRAP_S = com.badlogic.gdx.graphics.GL20.GL_TEXTURE_WRAP_S;
	public static final int GL_TEXTURE_WRAP_T = com.badlogic.gdx.graphics.GL20.GL_TEXTURE_WRAP_T;
	
	//public static final int GL_CLAMP = GL20.GL_CLAMP;
	public static final int GL_CLAMP_TO_EDGE = com.badlogic.gdx.graphics.GL20.GL_CLAMP_TO_EDGE;
	
	//public static final int GL_PROJECTION = 0x11;//GL20.GL_PROJECTION;
	//public static final int GL_MODELVIEW = 0x12;//GL20.GL_MODELVIEW;
	
	public static final int GL_TRUE = com.badlogic.gdx.graphics.GL20.GL_TRUE;
	public static final int GL_FALSE = com.badlogic.gdx.graphics.GL20.GL_FALSE;
	
	public static final int GL_RGBA = com.badlogic.gdx.graphics.GL20.GL_RGBA;
	public static final int GL_DEPTH_COMPONENT = com.badlogic.gdx.graphics.GL20.GL_DEPTH_COMPONENT;
	public static final int GL_DEPTH_COMPONENT16 = com.badlogic.gdx.graphics.GL20.GL_DEPTH_COMPONENT16;
	
	public static final int GL_UNSIGNED_BYTE = com.badlogic.gdx.graphics.GL20.GL_UNSIGNED_BYTE;
	public static final int GL_UNSIGNED_INT = com.badlogic.gdx.graphics.GL20.GL_UNSIGNED_INT;
	public static final int GL_FLOAT = com.badlogic.gdx.graphics.GL20.GL_FLOAT;
	public static final int GL_EQUAL = com.badlogic.gdx.graphics.GL20.GL_EQUAL;
	public static final int GL_LESS = com.badlogic.gdx.graphics.GL20.GL_LESS;
	public static final int GL_VERSION = com.badlogic.gdx.graphics.GL20.GL_VERSION;
	public static final int GL_VENDOR = com.badlogic.gdx.graphics.GL20.GL_VENDOR;
	public static final int GL_ALWAYS = com.badlogic.gdx.graphics.GL20.GL_ALWAYS;
	public static final int GL_LINES = com.badlogic.gdx.graphics.GL20.GL_LINES;
	public static final int GL_LEQUAL = com.badlogic.gdx.graphics.GL20.GL_LEQUAL;
	
	public static int glGetInteger(int pname) {
		Gdx.gl20.glGetIntegerv(pname, UtilBuff.prepare());
		return UtilBuff.get();
	}

	public static void glClearColor(float red, float green, float blue, float alpha) {
		Gdx.gl20.glClearColor(red, green, blue, alpha);
	}

	public static void glViewport(int x, int y, int width, int height) {
		Gdx.gl20.glViewport(x, y, width, height);
	}

	public static void glBindTexture(int target, int texture) {
		Gdx.gl20.glBindTexture(target, texture);
	}

	public static void glClear(int mask) {
		Gdx.gl20.glClear(mask);
	}

	public static void glEnable(int cap) {
		Gdx.gl20.glEnable(cap);
	}

	public static void glDisable(int cap) {
		Gdx.gl20.glDisable(cap);
	}

	public static boolean glGetBoolean(int pname) {
		Gdx.gl20.glGetBooleanv(pname, UtilBuff.prepareByte());
		return UtilBuff.getByte() != 0;
	}

	public static void glDepthMask(boolean flag) {
		Gdx.gl20.glDepthMask(flag);
	}

	public static void glDrawArrays(int mode, int first, int count) {
		Gdx.gl20.glDrawArrays(mode, first, count);
	}

	public static int glGenTextures() {
		return Gdx.gl20.glGenTexture();
	}

	public static void glTexImage1D(int target, int level, int internalformat,
			int width, int border, int format, int type, ByteBuffer pixels) {
		Gdx.gl20.glTexImage2D(target, level, internalformat, width, 1, border, format, type, pixels);
	}

	public static void glBlendFunc(int sfactor, int dfactor) {
		Gdx.gl20.glBlendFunc(sfactor, dfactor);
	}

	public static void glTexParameteri(int target, int pname, int param) {
		Gdx.gl20.glTexParameteri(target, pname, param);
	}

	public static void glDeleteTextures(int texId) {
		Gdx.gl20.glDeleteTextures(1, UtilBuff.prepare(texId));
	}
	
	public static void glEnd() {
		GL20.glDisableVertexAttribArray(texCoordLoc);
		GL20.glDisableVertexAttribArray(vertLoc);
		setRenderState(oldState);
	}
	
	public static void glBegin(){
		if(!inited) {
			init();
			inited = true;
		}
		oldState = getRenderState();
		GL20.glUseProgram(program);
		GL20.glEnableVertexAttribArray(vertLoc);
		GL20.glEnableVertexAttribArray(texCoordLoc);
		
		//GL11.glBindTexture(GL11.GL_TEXTURE_1D, 0);
	}
	static boolean inited = false;
	static RenderState oldState;
	/**
	 * A structure to hold all the important OpenGL state that needs to be
	 * changed to draw the curve. This is used to backup and restore the state
	 * so that the code outside of this (mainly Slick2D) doesn't break.
	 */
	private static class RenderState {
		boolean smoothedPoly;
		boolean blendEnabled;
		boolean depthEnabled;
		boolean depthWriteEnabled;
		boolean texEnabled;
		int texUnit;
		int oldProgram;
		int oldArrayBuffer;
	}
	
	private static RenderState getRenderState() {
		RenderState state = new RenderState();
		state.smoothedPoly = GL11.glGetBoolean(GL11.GL_POLYGON_SMOOTH);
		state.blendEnabled = GL11.glGetBoolean(GL11.GL_BLEND);
		state.depthEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
		state.depthWriteEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		state.texEnabled = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
		state.texUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		state.oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		state.oldArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		return state;
	}
	
	private static void setRenderState(RenderState state) {
		GL11.glDepthMask(state.depthWriteEnabled);
		setGLState(GL11.GL_DEPTH_TEST, state.depthEnabled);
		setGLState(GL11.GL_TEXTURE_2D, state.texEnabled);
		setGLState(GL11.GL_POLYGON_SMOOTH, state.smoothedPoly);
		setGLState(GL11.GL_BLEND, state.blendEnabled);
		GL20.glUseProgram(state.oldProgram);
		GL13.glActiveTexture(state.texUnit);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, state.oldArrayBuffer);
	}
	
	private static void setGLState(int cap, boolean state){
		if(state)
			GL11.glEnable(cap);
		else
			GL11.glDisable(cap);
	}
	public static void init(){
		if(program != 0)
			System.out.println("Program != 0");
		
		program = GL20.glCreateProgram();
		int vtxShdr = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		int frgShdr = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
		GL20.glShaderSource(vtxShdr, 
				 "#version 100\n"
				+ "\n"
				+ "attribute vec4 in_position;\n"
				+ "attribute vec2 in_tex_coord;\n"
				+ "\n"
				+ "varying vec2 tex_coord;\n"
				+ "void main()\n"
				+ "{\n"
				+ "    gl_Position = in_position;\n"
				+ "    tex_coord = in_tex_coord;\n"
				+ "}");
		GL20.glCompileShader(vtxShdr);
		int res = GL20.glGetShaderi(vtxShdr, GL20.GL_COMPILE_STATUS);
		if (res != GL11.GL_TRUE) {
			String error = GL20.glGetShaderInfoLog(vtxShdr, 1024);
			Log.error("Vertex Shader compilation failed.", new Exception(error));
		}
		GL20.glShaderSource(frgShdr, 
				"#version 100\n"
				+ "#ifdef GL_ES\n"
				+ "precision mediump float;\n"
				+ "#endif\n"
				+ "\n"
				+ "uniform sampler2D tex;\n"
				+ "uniform vec2 tex_size;\n"
				+ "uniform vec3 col_tint;\n"
				+ "uniform vec4 col_border;\n"
				+ "\n"
				+ "varying vec2 tex_coord;\n"
				+ "\n"
				+ "void main()\n"
				+ "{\n"
				+ "    vec4 in_color = texture2D(tex, tex_coord.xy) * col_border;\n"
				//+ "    float blend_factor = in_color.r-in_color.b;\n"
				//+ "    vec4 new_color = vec4(mix(in_color.xyz*col_border.xyz,col_tint,blend_factor),in_color.w);\n"
				+ "    gl_FragColor = in_color;\n"//new_color;\n"
				+ "}");
		GL20.glCompileShader(frgShdr);
		res = GL20.glGetShaderi(frgShdr, GL20.GL_COMPILE_STATUS);
		if (res != GL11.GL_TRUE) {
			String error = GL20.glGetShaderInfoLog(frgShdr, 1024);
			Log.error("Fragment Shader compilation failed.", new Exception(error));
		}
		GL20.glAttachShader(program, vtxShdr);
		GL20.glAttachShader(program, frgShdr);
		GL20.glLinkProgram(program);
		System.out.println("INFO: "+GL20.glGetProgramInfoLog(program, 1024));
		
		res = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS);
		System.out.println("INFO2: "+GL20.glGetProgramInfoLog(program, 1024));
		if (res != GL11.GL_TRUE) {
			String error = GL20.glGetProgramInfoLog(program, 1024);
			Log.error("Program linking failed. [" + error + "]", new Exception(error));
			System.out.println("Program linking failed. [" + error + "]");
		}
		GL20.glDeleteShader(vtxShdr);
		GL20.glDeleteShader(frgShdr);
		vertLoc = GL20.glGetAttribLocation(program, "in_position");
		texCoordLoc = GL20.glGetAttribLocation(program, "in_tex_coord");
		texLoc = GL20.glGetUniformLocation(program, "tex");
		colLoc = GL20.glGetUniformLocation(program, "col_tint");
		colBorderLoc = GL20.glGetUniformLocation(program, "col_border");
	}
	public static int program;
	public static int vertLoc;
	public static int texCoordLoc;
	public static int texLoc;
	public static int colLoc;
	public static int colBorderLoc;

	public static void glTexImage2D(int target, int level, int internalformat, int width,
			int height, int border, int format, int type, ByteBuffer pixels) {
		Gdx.gl20.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
	}

	public static void glFlush() {
		Gdx.gl20.glFlush();
	}

	public static void glDepthFunc(int func) {
		Gdx.gl20.glDepthFunc(func);
	}

	public static void glColorMask(boolean r, boolean g, boolean b, boolean a) {
		Gdx.gl20.glColorMask(r,g,b,a);
	}

	public static String glGetString(int name) {
		return Gdx.gl20.glGetString(name);
	}

}
