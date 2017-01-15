package fluddokt.opsu.fake;

import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

public class Graphics {

	static SpriteBatch batch;
	static ShapeRenderer shapeRender;
	static UnicodeFont curFont;
	public static OrthographicCamera camera;
	final static int NONE = 0;
	final static int SPRITE = 3;
	final static int SHAPELINE = 5;
	final static int SHAPEFILLED = 6;
	public static final int MODE_NORMAL = 1;
	public static final int MODE_ALPHA_MAP = 2;
	public static final int MODE_ALPHA_BLEND = 3;

	static int mode = 0;
	static int width, height;
	public static Color bgcolor = Color.black;
	static Color fgcolor = Color.white;
	static float lineWidth = 1;

	public static void init() {
		Image.texmap.clear();
		batch = new SpriteBatch();
		shapeRender = new ShapeRenderer();
		shapeRender.setAutoShapeType(true);
		mode = 0;
	}

	public static void resize(int wid, int hei) {
		width = wid;
		height = hei;
		camera= new OrthographicCamera(wid, hei);
		camera.setToOrtho(true, wid, hei);
		
		//camera.translate(wid / 2, hei / 2);
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		shapeRender.setProjectionMatrix(camera.combined);
	}

	public void setBackground(Color ncolor) {
		bgcolor = ncolor;
	}

	public void setFont(UnicodeFont nfont) {
		curFont = nfont;
	}

	public void drawString(String str, float x, float y) {
		drawString(curFont, str, x, y);
	}

	public void drawString(Font font, String str, float x, float y) {
		if (str == null)
			return;
		checkMode(SPRITE);
		font.dynFont.draw(batch, str, x, y);
}

	public void setColor(Color ncolor) {
		fgcolor = ncolor;
		batch.setColor(clamp(ncolor.r, 0, 1), clamp(ncolor.g, 0, 1),
				clamp(ncolor.b, 0, 1), clamp(ncolor.a, 0, 1));
		shapeRender.setColor(clamp(ncolor.r, 0, 1), clamp(ncolor.g, 0, 1),
				clamp(ncolor.b, 0, 1), clamp(ncolor.a, 0, 1));
	}
	
	public void setColorAlpha(Color ncolor, float alpha) {
		fgcolor = ncolor;
		batch.setColor(clamp(ncolor.r, 0, 1), clamp(ncolor.g, 0, 1),
				clamp(ncolor.b, 0, 1), clamp(ncolor.a * alpha, 0, 1));
		shapeRender.setColor(clamp(ncolor.r, 0, 1), clamp(ncolor.g, 0, 1),
				clamp(ncolor.b, 0, 1), clamp(ncolor.a * alpha, 0, 1));
	}

	private static float clamp(float n, float min, float max) {
		if (n > max)
			return max;
		if (n < min)
			return min;
		return n;
	}

	public void setAntiAlias(boolean b) {
		// TODO Auto-generated method stub
	}

	public void setLineWidth(float f) {
		checkMode(NONE);
		lineWidth = f;
	}
	public void resetLineWidth() {
		checkMode(NONE);
		lineWidth = 1;
	}

	public void fillRect(float x, float y, float w, float h) {
		checkMode(SHAPEFILLED);
		shapeRender.rect(x, y, w, h);
	}

	public void drawRect(float x, float y, float w, float h) {
		checkMode(SHAPELINE);
		shapeRender.rect(x, y, w, h);

	}

	public void drawOval(float x, float y, float w, float h) {
		checkMode(SHAPELINE);
		shapeRender.ellipse(x, y, w, h);

	}

	public void fillArc(float x, float y, float w, float h, float start,
			float end) {
		checkMode(SHAPEFILLED);
		if (w != h)
			throw new Error("fillArc Not implemented for w!=h");
		while (start< 0)
			start += 360;
		start %= 360;
		end %= 360;
		while (end < start)
			end += 360;
		shapeRender.arc(x + w / 2, y + h / 2, w / 2, start, end-start);// 36);
		

	}

	public void fillRoundRect(float x, float y, float w, float h, float m) {
		// TODO Auto-generated method stub
		checkMode(SHAPEFILLED);
		shapeRender.rect(x, y, w, h);
	}

	public void drawRoundRect(float x, float y, int w, int h, int m) {
		// TODO Auto-generated method stub
		checkMode(SHAPELINE);
		shapeRender.rect(x, y, w, h);
		
	}

	public void drawLine(float x1, float y1, float x2, float y2) {
		checkMode(SHAPELINE);
		shapeRender.line(x1, y1, x2, y2);
	}

	public static void checkMode(int nmode) {
		if (mode != nmode) {
			Gdx.gl20.glLineWidth(lineWidth);
			//end
			switch (mode) {
				case SPRITE:
					batch.end();
					break;
				case SHAPEFILLED: 
				case SHAPELINE:
					shapeRender.end();
					break;
			}
			//begin
			switch (nmode) {
				case SPRITE:
					batch.begin();
					break;
				case SHAPEFILLED: 
					Gdx.gl.glEnable(GL20.GL_BLEND);
					shapeRender.begin(ShapeType.Filled);
					break;
				case SHAPELINE:
					Gdx.gl.glEnable(GL20.GL_BLEND);
					shapeRender.begin(ShapeType.Line);
					break;
			}
			mode = nmode;
		}
	}

	public void drawTexture(TextureRegion tex, float x, float y, float wid,
			float hei, float rotation) {
		checkMode(SPRITE);
		if (tex == null)
			throw new Error("Texture is null");
		// draw(TextureRegion region, float x, float y, float originX, float
		// originY, float width, float height, float scaleX, float scaleY, float
		// rotation, boolean clockwise)
		
		batch.draw(tex, x, y, wid / 2, hei / 2, wid, hei, 1, 1,
				rotation);
	}
	public void drawTexture(Texture tex, float x, float y, float wid,
			float hei) {
		checkMode(SPRITE);
		if (tex == null)
			throw new Error("Texture is null");
		batch.draw(tex, x, y, wid, hei);
	}

	static Graphics g = new Graphics();
	static Graphics current = g;

	public static Graphics getGraphics() {
		return g;
	}

	Rectangle scissor;
	public void setClip(int x, int y, int w, int h) {
		clearClip();
		scissor = new Rectangle();
		Rectangle clip = new Rectangle(x, y, w, h);
		ScissorStack.calculateScissors(camera, batch.getTransformMatrix(), clip, scissor);
		if (!ScissorStack.pushScissors(scissor))
			scissor = null;
	}

	public void clearClip() {
		checkMode(0);
		if (scissor != null){
			scissor = null;
			if( ScissorStack.peekScissors() != null)
				ScissorStack.popScissors();
		}
	}

	public static void setCurrent(Graphics g2) {
		if(current != g2){
			checkMode(0);
			current.unbind();
			current = g2;
			current.bind();
		}
	}

	public void flush() {
		checkMode(0);
		Gdx.gl.glFlush();
	}

	public void drawImage(Image image, float x, float y) {
		image.draw(x,y);
	}

	public void clearAlphaMap() {
		// TODO Auto-generated method stub
		//Gdx.gl.glColorMask(false, false, false, true);
		//fillRect(0, 0, width, height);
		//Gdx.gl.glColorMask(true, true, true, true);
		
		//clear();
	}
	
	protected void bind(){
		
	}
	protected void unbind(){
		
	}

	public void clear() {
		if( current != this)
			bind();
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		if( current != this)
			unbind();
		
	}

	public void setDrawMode(int mode) {
		//TODO
		checkMode(0);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		batch.enableBlending();
		//Gdx.gl.glEnable(GL20.GL_BLEND);
		if (mode == MODE_NORMAL) {
			Gdx.gl.glColorMask(true, true, true, true);
			//Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			//Gdx.gl.glBlendFunc(GL20.GL_ZERO, GL20.GL_ZERO);
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		} else if (mode == MODE_ALPHA_MAP) {
			//Gdx.gl.glDisable(GL20.GL_BLEND);
			//batch.disableBlending();
			Gdx.gl.glColorMask(false, false, true, true);
			//Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ZERO);
			Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ZERO);
			batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO);
		} else if (mode == MODE_ALPHA_BLEND) {
			Gdx.gl.glColorMask(true, true, false, false);
			//Gdx.gl.glBlendFunc(GL20.GL_DST_ALPHA, GL20.GL_ONE_MINUS_DST_ALPHA);
			//Gdx.gl.glBlendFunc(GL20.GL_ZERO, GL20.GL_ONE);
			batch.setBlendFunction(GL20.GL_DST_ALPHA, GL20.GL_ONE_MINUS_DST_ALPHA);
		} else {
			throw new Error("Unknown Draw Mode");
		}
		//batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO);
		//batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_ZERO);
		//Gdx.gl.glBlendFunc(GL20.GL_ZERO, GL20.GL_ONE);
	}

	public Color getColor() {
		return fgcolor;
	}

	public float getLineWidth() {
		return 1;
	}

	public void pushTransform() {
		checkMode(NONE);
		TransformState state = getNewTranformState();
		state.position.set(camera.position);
		state.direction.set(camera.direction);
		state.up.set(camera.up);
		transformStack.addLast(state);
	}
	
	final Vector3 Z_AXIS = new Vector3(0, 0, 1);
	final Vector3 temp = new Vector3();
	public void rotate(float x, float y, float a) {
		checkMode(NONE);
		temp.set(x, y, 0);
		camera.rotateAround(temp, Z_AXIS, a);
		updateCamera();
	}

	public void translate(float x, float y) {
		checkMode(NONE);
		camera.translate(-x, -y);
		updateCamera();
	}

	public void popTransform() {
		checkMode(NONE);
		TransformState state = transformStack.removeLast();
		camera.position.set(state.position);
		camera.direction.set(state.direction);
		camera.up.set(state.up);
		updateCamera();
		transformPool.addFirst(state);
		
	}
	public void updateCamera() {
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		shapeRender.setProjectionMatrix(camera.combined);
	}
	private class TransformState {
		Vector3 direction = new Vector3();
		Vector3 position = new Vector3();
		Vector3 up = new Vector3();
	}
	private TransformState getNewTranformState() {
		if (transformPool.size() > 0)
			return transformPool.removeFirst();
		return new TransformState();
	}
	private LinkedList<TransformState> transformStack = new LinkedList<TransformState>();
	private LinkedList<TransformState> transformPool = new LinkedList<TransformState>();
}
