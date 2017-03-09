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

package itdelatrisu.opsu.render;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.objects.curves.Vec2f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.util.Log;

/**
 * Hold the temporary render state that needs to be restored again after the new
 * style curves are drawn.
 *
 * @author Bigpet {@literal <dravorek (at) gmail.com>}
 */
public class LegacyCurveRenderState {
	/** The width and height of the display container this curve gets drawn into. */
	protected static int containerWidth, containerHeight;

	/** Thickness of the curve. */
	protected static int scale;

	/** Static state that's needed to draw the new style curves. */
	private static final NewCurveStyleState staticState = new NewCurveStyleState();

	/** Cached drawn slider, only used if new style sliders are activated. */
	public Rendertarget fbo;

	/** The HitObject associated with the curve to be drawn. */
	protected HitObject hitObject;

	/** The points along the curve to be drawn. */
	protected Vec2f[] curve;

	/** The point to which the curve has last been rendered into the texture (as an index into {@code curve}). */
	private int lastPointDrawn;

	/** The point from which the curve has last been rendered into the texture (as an index into {@code curve}). */
	private int firstPointDrawn;

	/** Index of the curve array that indicates the starting position of the slider part that should not be drawn. */
	private int spliceFrom;

	/** Index of the curve array that indicates the end position of the slider part that should not be drawn. */
	private int spliceTo;

	/** List holding pairs of 2 numbers in a sequential order, indicating the start and end index of the curve array to render. */
	protected List<Integer> pointsToRender;

	/**
	 * Set the width and height of the container that Curves get drawn into.
	 * Should be called before any curves are drawn.
	 * @param width the container width
	 * @param height the container height
	 * @param circleDiameter the circle diameter
	 */
	public static void init(int width, int height, float circleDiameter) {
		containerWidth = width;
		containerHeight = height;

		// equivalent to what happens in Slider.init()
		scale = (int) (circleDiameter * HitObject.getXMultiplier());  // convert from Osupixels (640x480)
		//scale = scale * 118 / 128; //for curves exactly as big as the sliderball
		FrameBufferCache.init(width, height);
		NewCurveStyleState.initUnitCone();
	}

	/**
	 * Undo the static state. Static state setup caused by calls to
	 * {@link #draw(org.newdawn.slick.Color, org.newdawn.slick.Color, int, int)}
	 * are undone.
	 */
	public static void shutdown() {
		staticState.shutdown();
		FrameBufferCache.shutdown();
	}

	/**
	 * Creates an object to hold the render state that's necessary to draw a curve.
	 * @param hitObject the HitObject that represents this curve, just used as a unique ID
	 * @param curve the points along the curve to be drawn
	 */
	public LegacyCurveRenderState(HitObject hitObject, Vec2f[] curve) {
		this.hitObject = hitObject;
		this.curve = curve;
		initFBO();
	}

	/** Initializes the FBO. */
	private void initFBO() {
		FrameBufferCache cache = FrameBufferCache.getInstance();
		Rendertarget mapping = cache.get(hitObject);
		if (mapping == null)
			mapping = cache.insert(hitObject);
		fbo = mapping;
		createVertexBuffer(fbo.getVbo());
		// write impossible value to make sure the fbo is cleared
		lastPointDrawn = -1;
		spliceFrom = spliceTo = -1;
	}

	/**
	 * Splices the curve.
	 * @param from the start index to splice
	 * @param to the end index to splice
	 */
	public void splice(int from, int to) {
		spliceFrom = from * 2;
		spliceTo = to * 2;
		firstPointDrawn = -1;  // force redraw
		lastPointDrawn = -1;   // force redraw
	}

	/**
	 * Draw a curve to the screen that's tinted with `color`. The first time
	 * this is called this caches the image result of the curve and on subsequent
	 * runs it just draws the cached copy to the screen.
	 * @param color tint of the curve
	 * @param borderColor the curve border color
	 * @param pointsToRender the points to render (pairs of indices: from, to)
	 */
	public void draw(Color color, Color borderColor, List<Integer> pointsToRender) {
		lastPointDrawn = -1;
		firstPointDrawn = -1;
		this.pointsToRender = pointsToRender;
		draw(color, borderColor, 0, curve.length);
		this.pointsToRender = null;
	}

	/**
	 * Draw a curve to the screen that's tinted with `color`. The first time
	 * this is called this caches the image result of the curve and on subsequent
	 * runs it just draws the cached copy to the screen.
	 * @param color tint of the curve
	 * @param borderColor the curve border color
	 * @param from index to draw from
	 * @param to index to draw to (exclusive)
	 */
	public void draw(Color color, Color borderColor, int from, int to) {
		float alpha = color.a;

		if (fbo == null)
			initFBO();

		if (lastPointDrawn != to || firstPointDrawn != from) {
			int oldFb = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
			int oldTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			//glGetInteger requires a buffer of size 16, even though just 4
			//values are returned in this specific case
			IntBuffer oldViewport = BufferUtils.createIntBuffer(16);
			GL11.glGetInteger(GL11.GL_VIEWPORT, oldViewport);
			EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fbo.getID());
			GL11.glViewport(0, 0, fbo.width, fbo.height);
			GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			this.renderCurve(color, borderColor, from, to, firstPointDrawn != from);
			lastPointDrawn = to;
			firstPointDrawn = from;
			color.a = 1f;

			GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTex);
			EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldFb);
			GL11.glViewport(oldViewport.get(0), oldViewport.get(1), oldViewport.get(2), oldViewport.get(3));
		}

		// draw a fullscreen quad with the texture that contains the curve
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_TEXTURE_1D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo.getTextureID());
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glColor4f(1.0f, 1.0f, 1.0f, alpha);
		GL11.glTexCoord2f(1.0f, 1.0f);
		GL11.glVertex2i(fbo.width, 0);
		GL11.glTexCoord2f(0.0f, 1.0f);
		GL11.glVertex2i(0, 0);
		GL11.glTexCoord2f(0.0f, 0.0f);
		GL11.glVertex2i(0, fbo.height);
		GL11.glTexCoord2f(1.0f, 0.0f);
		GL11.glVertex2i(fbo.width, fbo.height);
		GL11.glEnd();
	}

	/**
	 * Discard the cache mapping for this curve object.
	 */
	public void discardGeometry() {
		fbo = null;
		FrameBufferCache.getInstance().freeMappingFor(hitObject);
	}

	/**
	 * A structure to hold all the important OpenGL state that needs to be
	 * changed to draw the curve. This is used to backup and restore the state
	 * so that the code outside of this (mainly Slick2D) doesn't break.
	 */
	private class RenderState {
		boolean smoothedPoly;
		boolean blendEnabled;
		boolean depthEnabled;
		boolean depthWriteEnabled;
		boolean texEnabled;
		int texUnit;
		int oldProgram;
		int oldArrayBuffer;
	}

	/**
	 * Backup the current state of the relevant OpenGL state and change it to
	 * what's needed to draw the curve.
	 */
	private RenderState saveRenderState() {
		RenderState state = new RenderState();
		state.smoothedPoly = GL11.glGetBoolean(GL11.GL_POLYGON_SMOOTH);
		state.blendEnabled = GL11.glGetBoolean(GL11.GL_BLEND);
		state.depthEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
		state.depthWriteEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		state.texEnabled = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
		state.texUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		state.oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		state.oldArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_TEXTURE_1D);
		GL11.glBindTexture(GL11.GL_TEXTURE_1D, staticState.gradientTexture);
		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);

		GL20.glUseProgram(0);

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();

		return state;
	}

	/**
	 * Restore the old OpenGL state that's backed up in {@code state}.
	 * @param state the old state to restore
	 */
	private void restoreRenderState(RenderState state) {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_BLEND);
		GL20.glUseProgram(state.oldProgram);
		GL13.glActiveTexture(state.texUnit);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, state.oldArrayBuffer);
		if (!state.depthWriteEnabled)
			GL11.glDepthMask(false);
		if (!state.depthEnabled)
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		if (state.texEnabled)
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		if (state.smoothedPoly)
			GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
		if (!state.blendEnabled)
			GL11.glDisable(GL11.GL_BLEND);
	}

	/**
	 * Write the vertices and (with position and texture coordinates) for the full
	 * curve into the OpenGL buffer with the ID specified by {@code bufferID}
	 * @param bufferID the buffer ID for the OpenGL buffer the vertices should be written into
	 */
	private void createVertexBuffer(int bufferID) {
		int arrayBufferBinding = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		FloatBuffer buff = BufferUtils.createByteBuffer(4 * (4 + 2) * (2 * curve.length - 1) * (NewCurveStyleState.DIVIDES + 2)).asFloatBuffer();
		if (curve.length > 0)
			fillCone(buff, curve[0].x, curve[0].y);
		for (int i = 1; i < curve.length; ++i) {
			float x = curve[i].x;
			float y = curve[i].y;
			fillCone(buff, x, y);
			float last_x = curve[i - 1].x;
			float last_y = curve[i - 1].y;
			double diff_x = x - last_x;
			double diff_y = y - last_y;
			float dist = Utils.distance(x, y, last_x, last_y);
			if (dist < scale / 8f) {
				x = (float) (x - diff_x / 2);
				y = (float) (y - diff_y / 2);
			} else {
				// don't mind me
				x = -100f;
				y = -100f;
			}
			fillCone(buff, x, y);
		}
		buff.flip();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buff, GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBufferBinding);
	}

	/**
	 * Do the actual drawing of the curve into the currently bound framebuffer.
	 * @param color the color of the curve
	 * @param borderColor the curve border color
	 */
	private void renderCurve(Color color, Color borderColor, int from, int to, boolean clearFirst) {
		staticState.initGradient();
		RenderState state = saveRenderState();
		staticState.initShaderProgram();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fbo.getVbo());
		GL20.glUseProgram(staticState.program);
		GL20.glEnableVertexAttribArray(staticState.attribLoc);
		GL20.glEnableVertexAttribArray(staticState.texCoordLoc);
		GL20.glUniform1i(staticState.texLoc, 0);
		GL20.glUniform3f(staticState.colLoc, color.r, color.g, color.b);
		GL20.glUniform4f(staticState.colBorderLoc, borderColor.r, borderColor.g, borderColor.b, borderColor.a);
		//stride is 6*4 for the floats (4 bytes) (u,v)(x,y,z,w)
		//2*4 is for skipping the first 2 floats (u,v)
		GL20.glVertexAttribPointer(staticState.attribLoc, 4, GL11.GL_FLOAT, false, 6 * 4, 2 * 4);
		GL20.glVertexAttribPointer(staticState.texCoordLoc, 2, GL11.GL_FLOAT, false, 6 * 4, 0);
		if (clearFirst)
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		if (pointsToRender == null) {
			for (int i = from * 2; i < to * 2 - 1; ++i) {
				if (spliceFrom <= i && i <= spliceTo)
					continue;
				GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, i * (NewCurveStyleState.DIVIDES + 2), NewCurveStyleState.DIVIDES + 2);
			}
		} else {
			Iterator<Integer> iter = pointsToRender.iterator();
			while (iter.hasNext()) {
				for (int i = iter.next() * 2, end = iter.next() * 2 - 1; i < end; ++i)
					GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, i * (NewCurveStyleState.DIVIDES + 2), NewCurveStyleState.DIVIDES + 2);
			}
		}
		GL11.glFlush();
		GL20.glDisableVertexAttribArray(staticState.texCoordLoc);
		GL20.glDisableVertexAttribArray(staticState.attribLoc);
		restoreRenderState(state);
	}

	/**
	 * Fill {@code buff} with the texture coordinates and positions for a cone
	 * that has its center at the coordinates {@code (x1,y1)}.
	 * @param buff the buffer to be filled
	 * @param x1 x-coordinate of the cone
	 * @param y1 y-coordinate of the cone
	 */
	protected void fillCone(FloatBuffer buff, float x1, float y1) {
		float divx = containerWidth / 2.0f;
		float divy = containerHeight / 2.0f;
		float offx = -1.0f;
		float offy = 1.0f;
		float radius = scale / 2;

		for (int i = 0; i < NewCurveStyleState.unitCone.length / 6; ++i) {
			buff.put(NewCurveStyleState.unitCone[i * 6 + 0]);
			buff.put(NewCurveStyleState.unitCone[i * 6 + 1]);
			buff.put(offx + (x1 + radius * NewCurveStyleState.unitCone[i * 6 + 2]) / divx);
			buff.put(offy - (y1 + radius * NewCurveStyleState.unitCone[i * 6 + 3]) / divy);
			buff.put(NewCurveStyleState.unitCone[i * 6 + 4]);
			buff.put(NewCurveStyleState.unitCone[i * 6 + 5]);
		}
	}

	/**
	 * Contains all the necessary state that needs to be tracked to draw curves
	 * in the new style and not re-create the shader each time.
	 *
	 * @author Bigpet {@literal <dravorek (at) gmail.com>}
	 */
	private static class NewCurveStyleState {
		/**
		 * Used for new style Slider rendering, defines how many vertices the
		 * base of the cone has that is used to draw the curve.
		 */
		protected static final int DIVIDES = 30;

		/**
		 * Array to hold the dummy vertex data (texture coordinates and position)
		 * of a cone with DIVIDES vertices at its base, that is centered around
		 * (0,0) and has a radius of 1 (so that it can be translated and scaled easily).
		 */
		protected static float[] unitCone = new float[(DIVIDES + 2) * 6];

		/** OpenGL shader program ID used to draw and recolor the curve. */
		protected int program = 0;

		/** OpenGL shader attribute location of the vertex position attribute. */
		protected int attribLoc = 0;

		/** OpenGL shader attribute location of the texture coordinate attribute. */
		protected int texCoordLoc = 0;

		/** OpenGL shader uniform location of the color attribute. */
		protected int colLoc = 0;

		/** OpenGL shader uniform location of the border color attribute. */
		protected int colBorderLoc = 0;

		/** OpenGL shader uniform location of the texture sampler attribute. */
		protected int texLoc = 0;

		/** OpenGL texture id for the gradient texture for the curve. */
		protected int gradientTexture = 0;

		/**
		 * Reads the first row of the slider gradient texture and upload it as
		 * a 1D texture to OpenGL if it hasn't already been done.
		 */
		public void initGradient() {
			if (gradientTexture == 0) {
				Image slider = GameImage.SLIDER_GRADIENT_EXPERIMENTAL.getImage().getScaledCopy(1.0f / GameImage.getUIscale());
				staticState.gradientTexture = GL11.glGenTextures();
				ByteBuffer buff = BufferUtils.createByteBuffer(slider.getWidth() * 4);
				for (int i = 0; i < slider.getWidth(); ++i) {
					Color col = slider.getColor(i, 0);
					buff.put((byte) (255 * col.r));
					buff.put((byte) (255 * col.g));
					buff.put((byte) (255 * col.b));
					buff.put((byte) (255 * col.a));
				}
				buff.flip();
				GL11.glBindTexture(GL11.GL_TEXTURE_1D, gradientTexture);
				GL11.glTexImage1D(GL11.GL_TEXTURE_1D, 0, GL11.GL_RGBA, slider.getWidth(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buff);
				EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_1D);
			}
		}

		/**
		 * Write the data into {@code unitCone} if it hasn't already been initialized.
		 */
		public static void initUnitCone() {
			int index = 0;
			//check if initialization has already happened
			if (unitCone[0] == 0.0f) {
				//tip of the cone
				//vec2 texture coordinates
				unitCone[index++] = 1.0f;
				unitCone[index++] = 0.5f;

				//vec4 position
				unitCone[index++] = 0.0f;
				unitCone[index++] = 0.0f;
				unitCone[index++] = 0.0f;
				unitCone[index++] = 1.0f;
				for (int j = 0; j < NewCurveStyleState.DIVIDES; ++j) {
					double phase = j * (float) Math.PI * 2 / NewCurveStyleState.DIVIDES;
					//vec2 texture coordinates
					unitCone[index++] = 0.0f;
					unitCone[index++] = 0.5f;
					//vec4 positon
					unitCone[index++] = (float) Math.sin(phase);
					unitCone[index++] = (float) Math.cos(phase);
					unitCone[index++] = 1.0f;
					unitCone[index++] = 1.0f;
				}
				//vec2 texture coordinates
				unitCone[index++] = 0.0f;
				unitCone[index++] = 0.5f;
				//vec4 positon
				unitCone[index++] = (float) Math.sin(0.0f);
				unitCone[index++] = (float) Math.cos(0.0f);
				unitCone[index++] = 1.0f;
				unitCone[index++] = 1.0f;
			}
		}

		/**
		 * Compiles and links the shader program for the new style curve objects
		 * if it hasn't already been compiled and linked.
		 */
		public void initShaderProgram() {
			if (program == 0) {
				program = GL20.glCreateProgram();
				int vtxShdr = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
				int frgShdr = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
				GL20.glShaderSource(vtxShdr, "#version 110\n"
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
				GL20.glShaderSource(frgShdr, "#version 110\n"
						+ "\n"
						+ "uniform sampler1D tex;\n"
						+ "uniform vec2 tex_size;\n"
						+ "uniform vec3 col_tint;\n"
						+ "uniform vec4 col_border;\n"
						+ "\n"
						+ "varying vec2 tex_coord;\n"
						+ "\n"
						+ "void main()\n"
						+ "{\n"
						+ "    vec4 in_color = texture1D(tex, tex_coord.x);\n"
						+ "    float blend_factor = in_color.r-in_color.b;\n"
						+ "    vec4 new_color = vec4(mix(in_color.xyz*col_border.xyz,col_tint,blend_factor),in_color.w);\n"
						+ "    gl_FragColor = new_color;\n"
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
				res = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS);
				if (res != GL11.GL_TRUE) {
					String error = GL20.glGetProgramInfoLog(program, 1024);
					Log.error("Program linking failed.", new Exception(error));
				}
				GL20.glDeleteShader(vtxShdr);
				GL20.glDeleteShader(frgShdr);
				attribLoc = GL20.glGetAttribLocation(program, "in_position");
				texCoordLoc = GL20.glGetAttribLocation(program, "in_tex_coord");
				texLoc = GL20.glGetUniformLocation(program, "tex");
				colLoc = GL20.glGetUniformLocation(program, "col_tint");
				colBorderLoc = GL20.glGetUniformLocation(program, "col_border");
			}
		}

		/**
		 * Cleanup any OpenGL objects that may have been initialized.
		 */
		private void shutdown() {
			if (gradientTexture != 0) {
				GL11.glDeleteTextures(gradientTexture);
				gradientTexture = 0;
			}

			if (program != 0) {
				GL20.glDeleteProgram(program);
				program = 0;
				attribLoc = 0;
				texCoordLoc = 0;
				colLoc = 0;
				colBorderLoc = 0;
				texLoc = 0;
			}
		}
	}
}
