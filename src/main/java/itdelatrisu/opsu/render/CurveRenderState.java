/*
 *  opsu! - an open-source osu! client
 *  Copyright (C) 2014, 2015 Jeffrey Han
 *
 *  opsu! is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  opsu! is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */
package itdelatrisu.opsu.render;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.objects.curves.Vec2f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.util.Log;

/**
 * Hold the temporary render state that needs to be restored again after the new
 * style curves are drawn.
 *
 * @author Bigpet {@literal <dravorek (at) gmail.com>}
 */
public class CurveRenderState {
	/** The width and height of the display container this curve gets drawn into. */
	protected static int containerWidth, containerHeight;

	/** Thickness of the curve. */
	protected static int scale;

	/** Static state that's needed to draw the new style curves. */
	private static final NewCurveStyleState staticState = new NewCurveStyleState();

	/** The vertex buffer used for the curve's vertices. */
	private int vboID;

	/** The HitObject associated with the curve to be drawn. */
	protected HitObject hitObject;

	/** The points along the curve to be drawn. */
	protected Vec2f[] curve;

	/** The indices of the points. */
	protected int[] pointIndices;

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
	}

	/**
	 * Undo the static state. Static state setup caused by calls to
	 * {@link #draw(org.newdawn.slick.Color, org.newdawn.slick.Color, float)}
	 * are undone.
	 */
	public static void shutdown() {
		staticState.shutdown();
	}

	/**
	 * Creates an object to hold the render state that's necessary to draw a curve.
	 * @param hitObject the HitObject that represents this curve, just used as a unique ID
	 * @param curve the points along the curve to be drawn
	 */
	public CurveRenderState(HitObject hitObject, Vec2f[] curve) {
		this.hitObject = hitObject;
		this.curve = curve;
		this.pointIndices = new int[curve.length];
		this.vboID = -1;
	}

	/**
	 * Draw a curve to the screen that's tinted with `color`. The first time
	 * this is called this caches the image result of the curve and on subsequent
	 * runs it just draws the cached copy to the screen.
	 * @param color tint of the curve
	 * @param borderColor the curve border color
	 * @param t the point up to which the curve should be drawn (in the interval [0, 1])
	 */
	public void draw(Color color, Color borderColor, float t) {
		t = Utils.clamp(t, 0.0f, 1.0f);

		// create curve geometry and store it on the GPU
		if (vboID == -1) {
			vboID = GL15.glGenBuffers();
			createVertexBuffer(vboID);
		}

		int drawUpTo = (int) (t * (curve.length - 1));
		this.renderCurve(color, borderColor, drawUpTo);
	}

	/**
	 * Discard the geometry for this curve object.
	 */
	public void discardGeometry() {
		GL15.glDeleteBuffers(vboID);
		vboID = -1;
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
		state.oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		state.oldArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL14.glBlendEquation(GL14.GL_FUNC_ADD);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_TEXTURE_1D);
		GL11.glBindTexture(GL11.GL_TEXTURE_1D, staticState.gradientTexture);
		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);

		GL20.glUseProgram(0);

		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

		return state;
	}

	/**
	 * Restore the old OpenGL state that's backed up in {@code state}.
	 * @param state the old state to restore
	 */
	private void restoreRenderState(RenderState state) {
		GL11.glEnable(GL11.GL_BLEND);
		GL20.glUseProgram(state.oldProgram);
		GL11.glDisable(GL11.GL_TEXTURE_1D);
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
		float radius = scale / 2;
		int triangle_count = NewCurveStyleState.DIVIDES; // for curve caps
		float last_dx=0, last_dy=0;
		float last_alpha = 0;
		for (int i = 0; i < curve.length; ++i) { // compute number of triangles
			float x = curve[i].x;
			float y = curve[i].y;
			if (i > 0) {
				float last_x = curve[i - 1].x;
				float last_y = curve[i - 1].y;
				float diff_x = x - last_x;
				float diff_y = y - last_y;

				float alpha = (float)Math.atan2(diff_y, diff_x);

				if (i > 1) {
					float theta = alpha - last_alpha;
					if (theta > Math.PI) theta -= 2*Math.PI;
					if (theta < -Math.PI) theta += 2*Math.PI;

					if (Math.abs(theta) < 2*Math.PI / NewCurveStyleState.DIVIDES) {
						triangle_count++;
					} else {
						int divs = (int)(Math.ceil(NewCurveStyleState.DIVIDES * Math.abs(theta) / (2*Math.PI)));
						triangle_count += divs;
					}
				}
				triangle_count += 4;

				last_dx = diff_x;
				last_dy = diff_y;
				last_alpha = alpha;
			}
		}

		int arrayBufferBinding = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		FloatBuffer buff = BufferUtils.createByteBuffer(4 * (4 + 2) * 3 * (triangle_count)).asFloatBuffer();

		last_dx=0; last_dy=0;
		float last_ox=0, last_oy=0;
		for (int i = 0; i < curve.length; ++i) {
			float x = curve[i].x;
			float y = curve[i].y;
			if (i > 0) {
				/*
				Render this shape:
				 ___ ___
				|A /|C /|
				| /B| /D|
				|/__|/__|

				 */
				float last_x = curve[i - 1].x;
				float last_y = curve[i - 1].y;
				float diff_x = x - last_x;
				float diff_y = y - last_y;
				float length = (float)Math.hypot(diff_x, diff_y);
				float offs_x = radius * diff_y / length;
				float offs_y = radius * -diff_x / length;

				float alpha = (float)Math.atan2(diff_y, diff_x);

				if (i > 1) {
					float cross = last_dx * diff_y - last_dy * diff_x;
					float theta = alpha - last_alpha;
					if (theta > Math.PI) theta -= 2*Math.PI;
					if (theta < -Math.PI) theta += 2*Math.PI;

					if (Math.abs(theta) < 2*Math.PI / NewCurveStyleState.DIVIDES) { // small angle, just render single triangle
						if (cross > 0) { // going counterclockwise
							buff.put(1.0f);            buff.put(0.5f);
							buff.put(last_x);          buff.put(last_y);
							buff.put(0.0f);            buff.put(1.0f);
							buff.put(0.0f);            buff.put(0.5f);
							buff.put(last_x + last_ox);buff.put(last_y + last_oy);
							buff.put(1.0f);            buff.put(1.0f);
							buff.put(0.0f);            buff.put(0.5f);
							buff.put(last_x + offs_x); buff.put(last_y + offs_y);
							buff.put(1.0f);            buff.put(1.0f);
						} else if (cross < 0) {
							buff.put(1.0f);            buff.put(0.5f);
							buff.put(last_x);          buff.put(last_y);
							buff.put(0.0f);            buff.put(1.0f);
							buff.put(0.0f);            buff.put(0.5f);
							buff.put(last_x - offs_x); buff.put(last_y - offs_y);
							buff.put(1.0f);            buff.put(1.0f);
							buff.put(0.0f);            buff.put(0.5f);
							buff.put(last_x - last_ox);buff.put(last_y - last_oy);
							buff.put(1.0f);            buff.put(1.0f);
						} else {
							// straight line, very unlikely
						}
					} else {
						int divs = (int)(Math.ceil(NewCurveStyleState.DIVIDES * Math.abs(theta) / (2*Math.PI)));
						float phi = Math.abs(theta) / divs;
						float sinphi = (float)Math.sin(phi);
						float cosphi = (float)Math.cos(phi);

						float prev_ox = last_ox;
						float prev_oy = last_oy;
						if (cross < 0) {
							prev_ox = -offs_x;
							prev_oy = -offs_y;
						}
						for (int j = 0; j < divs; j++) {
							/*
							 * Ratation matrix:
							 * [ cos -sin ]
							 * [ sin  cos ]
							 */
							float ox = cosphi*prev_ox - sinphi*prev_oy;
							float oy = sinphi*prev_ox + cosphi*prev_oy;

							buff.put(1.0f);            buff.put(0.5f);
							buff.put(last_x);          buff.put(last_y);
							buff.put(0.0f);            buff.put(1.0f);
							buff.put(0.0f);            buff.put(0.5f);
							buff.put(last_x + prev_ox);buff.put(last_y + prev_oy);
							buff.put(1.0f);            buff.put(1.0f);
							buff.put(0.0f);            buff.put(0.5f);
							buff.put(last_x + ox);     buff.put(last_y + oy);
							buff.put(1.0f);            buff.put(1.0f);

							prev_ox = ox; prev_oy = oy;
						}
					}
				} else {
					int divs = NewCurveStyleState.DIVIDES / 2;

					float phi = (float)(Math.PI / divs);
					float sinphi = (float)Math.sin(phi);
					float cosphi = (float)Math.cos(phi);
					float prev_ox = 0;
					float prev_oy = -radius;

					for (int j = 0; j < divs; j++) {
						float ox = cosphi*prev_ox - sinphi*prev_oy;
						float oy = sinphi*prev_ox + cosphi*prev_oy;

						buff.put(1.0f);            buff.put(0.5f);
						buff.put(0);               buff.put(0);
						buff.put(0.0f);            buff.put(1.0f);
						buff.put(0.0f);            buff.put(0.5f);
						buff.put(0 + prev_ox);     buff.put(0 + prev_oy);
						buff.put(1.0f);            buff.put(1.0f);
						buff.put(0.0f);            buff.put(0.5f);
						buff.put(0 + ox);          buff.put(0 + oy);
						buff.put(1.0f);            buff.put(1.0f);

						prev_ox = ox; prev_oy = oy;
					}

					prev_ox = -offs_x;
					prev_oy = -offs_y;

					for (int j = 0; j < divs; j++) {
						float ox = cosphi*prev_ox - sinphi*prev_oy;
						float oy = sinphi*prev_ox + cosphi*prev_oy;

						buff.put(1.0f);            buff.put(0.5f);
						buff.put(last_x);          buff.put(last_y);
						buff.put(0.0f);            buff.put(1.0f);
						buff.put(0.0f);            buff.put(0.5f);
						buff.put(last_x + prev_ox);buff.put(last_y + prev_oy);
						buff.put(1.0f);            buff.put(1.0f);
						buff.put(0.0f);            buff.put(0.5f);
						buff.put(last_x + ox);     buff.put(last_y + oy);
						buff.put(1.0f);            buff.put(1.0f);

						prev_ox = ox; prev_oy = oy;
					}
				}

				buff.put(0.0f);            buff.put(0.5f);
				buff.put(last_x - offs_x); buff.put(last_y - offs_y);
				buff.put(1.0f);            buff.put(1.0f);
				buff.put(1.0f);            buff.put(0.5f);
				buff.put(x);               buff.put(y);
				buff.put(0.0f);            buff.put(1.0f);
				buff.put(0.0f);            buff.put(0.5f);
				buff.put(x - offs_x);      buff.put(y - offs_y);
				buff.put(1.0f);            buff.put(1.0f);

				buff.put(0.0f);            buff.put(0.5f);
				buff.put(last_x - offs_x); buff.put(last_y - offs_y);
				buff.put(1.0f);            buff.put(1.0f);
				buff.put(1.0f);            buff.put(0.5f);
				buff.put(last_x);          buff.put(last_y);
				buff.put(0.0f);            buff.put(1.0f);
				buff.put(1.0f);            buff.put(0.5f);
				buff.put(x);               buff.put(y);
				buff.put(0.0f);            buff.put(1.0f);

				buff.put(1.0f);            buff.put(0.5f);
				buff.put(last_x);          buff.put(last_y);
				buff.put(0.0f);            buff.put(1.0f);
				buff.put(0.0f);            buff.put(0.5f);
				buff.put(x + offs_x);      buff.put(y + offs_y);
				buff.put(1.0f);            buff.put(1.0f);
				buff.put(1.0f);            buff.put(0.5f);
				buff.put(x);               buff.put(y);
				buff.put(0.0f);            buff.put(1.0f);

				buff.put(1.0f);            buff.put(0.5f);
				buff.put(last_x);          buff.put(last_y);
				buff.put(0.0f);            buff.put(1.0f);
				buff.put(0.0f);            buff.put(0.5f);
				buff.put(last_x + offs_x); buff.put(last_y + offs_y);
				buff.put(1.0f);            buff.put(1.0f);
				buff.put(0.0f);            buff.put(0.5f);
				buff.put(x + offs_x);      buff.put(y + offs_y);
				buff.put(1.0f);            buff.put(1.0f);

				last_dx = diff_x;
				last_dy = diff_y;
				last_ox = offs_x;
				last_oy = offs_y;
				last_alpha = alpha;
			}

			pointIndices[i] = buff.position() / 6; // 6 elements per vertex
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
	private void renderCurve(Color color, Color borderColor, int to) {
		staticState.initGradient();
		RenderState state = saveRenderState();
		staticState.initShaderProgram();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
		GL20.glUseProgram(staticState.program);
		GL20.glEnableVertexAttribArray(staticState.attribLoc);
		GL20.glEnableVertexAttribArray(staticState.texCoordLoc);
		GL20.glUniform1i(staticState.texLoc, 0);
		GL20.glUniform4f(staticState.colLoc, color.r, color.g, color.b, color.a);
		GL20.glUniform4f(staticState.colBorderLoc, borderColor.r, borderColor.g, borderColor.b, borderColor.a);

		float lastSegmentX = to == 0 ? curve[1].x - curve[0].x : curve[to].x - curve[to-1].x;
		float lastSegmentY = to == 0 ? curve[1].y - curve[0].y : curve[to].y - curve[to-1].y;
		float lastSegmentInvLen = 1.f/(float)Math.hypot(lastSegmentX, lastSegmentY);
		GL20.glUniform4f(staticState.endPointLoc, curve[to].x, curve[to].y, lastSegmentX * lastSegmentInvLen, lastSegmentY * lastSegmentInvLen);
		//stride is 6*4 for the floats (4 bytes) (u,v)(x,y,z,w)
		//2*4 is for skipping the first 2 floats (u,v)
		GL20.glVertexAttribPointer(staticState.attribLoc, 4, GL11.GL_FLOAT, false, 6 * 4, 2 * 4);
		GL20.glVertexAttribPointer(staticState.texCoordLoc, 2, GL11.GL_FLOAT, false, 6 * 4, 0);

		GL11.glColorMask(false,false,false,false);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, pointIndices[to]);
		GL11.glColorMask(true,true,true,true);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDepthFunc(GL11.GL_EQUAL);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, pointIndices[to]);
		GL11.glDepthFunc(GL11.GL_LESS);

		GL11.glFlush();
		GL20.glDisableVertexAttribArray(staticState.texCoordLoc);
		GL20.glDisableVertexAttribArray(staticState.attribLoc);
		restoreRenderState(state);
	}

	/**
	 * Contains all the necessary state that needs to be tracked to draw curves
	 * in the new style and not re-create the shader each time.
	 *
	 * @author Bigpet {@literal <dravorek (at) gmail.com>}
	 */
	private static class NewCurveStyleState {
		/**
		 * Used for new style Slider rendering, defines how many vertices there
		 * are in a circle. Must be even.
		 */
		protected static final int DIVIDES = 30;

		/** OpenGL shader program ID used to draw and recolor the curve. */
		protected int program = 0;

		/** OpenGL shader attribute location of the vertex position attribute. */
		protected int attribLoc = 0;

		/** OpenGL shader attribute location of the texture coordinate attribute. */
		protected int texCoordLoc = 0;

		/** OpenGL shader uniform location of the end point attribute. */
		protected int endPointLoc = 0;

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
				Image slider = GameImage.SLIDER_GRADIENT.getImage().getScaledCopy(1.0f / GameImage.getUIscale());
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
				ContextCapabilities capabilities = GLContext.getCapabilities();
				if (capabilities.OpenGL30) {
					GL30.glGenerateMipmap(GL11.GL_TEXTURE_1D);
				} else if (capabilities.GL_EXT_framebuffer_object) {
					EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_1D);
				} else {
					GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
				}
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
				GL20.glShaderSource(vtxShdr, "#version 130\n"
						+ "\n"
						+ "uniform vec4 endPoint;\n"
						+ "\n"
						+ "attribute vec4 in_position;\n"
						+ "attribute vec2 in_tex_coord;\n"
						+ "\n"
						+ "varying vec2 tex_coord;\n"
						+ "void main()\n"
						+ "{\n"
						+ "    vec4 pos = in_position;\n"
						+ "    if (gl_VertexID < " + 3 * DIVIDES / 2 + ") {\n"
						+ "        mat2 rot = mat2(endPoint.zw, vec2(-1.0,1.0)*endPoint.wz);\n"
						+ "        pos.xy = endPoint.xy + rot * in_position.xy;\n"
						+ "    }\n"
						+ "    gl_Position = gl_ModelViewProjectionMatrix * pos;\n"
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
						+ "uniform vec4 col_tint;\n"
						+ "uniform vec4 col_border;\n"
						+ "\n"
						+ "varying vec2 tex_coord;\n"
						+ "\n"
						+ "void main()\n"
						+ "{\n"
						+ "    vec4 in_color = texture1D(tex, tex_coord.x);\n"
						+ "    float blend_factor = in_color.r-in_color.b;\n"
						+ "    vec4 new_color = vec4(mix(in_color.xyz*col_border.xyz,col_tint.xyz,blend_factor),in_color.w*col_tint.w);\n"
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
				endPointLoc = GL20.glGetUniformLocation(program, "endPoint");
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
