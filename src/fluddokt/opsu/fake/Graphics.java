package fluddokt.opsu.fake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

public class Graphics {

	static SpriteBatch batch;
	static ShapeRenderer shapeRender;
	static UnicodeFont curFont;
	final static int SPRITE = 3;
	final static int SHAPELINE = 5;
	final static int SHAPEFILLED = 6;

	static int mode = 0;
	static int width, height;
	public static Color bgcolor = Color.black;
	static Color fgcolor = Color.white;

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
		OrthographicCamera t = new OrthographicCamera(wid, hei);
		// t.setToOrtho(false,wid,hei);
		t.translate(wid / 2, hei / 2);
		t.update();
		batch.setProjectionMatrix(t.combined);
		shapeRender.setProjectionMatrix(t.combined);
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
		// font.bitmap.setColor(fgcolor.r, fgcolor.g, fgcolor.b, fgcolor.a);
		// font.bitmap.draw(batch, str, x, height-y);
		// batch.enableBlending();
		// setColor(Color.red);
		font.dynFont.draw(batch, str, x, height - y);

	}

	public void setColor(Color ncolor) {
		fgcolor = ncolor;
		batch.setColor(clamp(ncolor.r, 0, 1), clamp(ncolor.g, 0, 1),
				clamp(ncolor.b, 0, 1), clamp(ncolor.a, 0, 1));
		shapeRender.setColor(clamp(ncolor.r, 0, 1), clamp(ncolor.g, 0, 1),
				clamp(ncolor.b, 0, 1), clamp(ncolor.a, 0, 1));
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
		// TODO Auto-generated method stub

	}

	public void fillRect(float x, float y, float w, float h) {
		checkMode(SHAPEFILLED);
		shapeRender.rect(x, height - y - h, w, h);
	}

	public void drawRect(float x, float y, float w, float h) {
		checkMode(SHAPELINE);
		shapeRender.rect(x, height - y - h, w, h);

	}

	public void drawOval(float x, float y, float w, float h) {
		checkMode(SHAPELINE);
		shapeRender.ellipse(x, height - y - h, w, h);

	}

	public void fillArc(float x, float y, float w, float h, float start,
			float end) {
		checkMode(SHAPEFILLED);
		// System.out.println("Arc :"+(start+" "+end));
		if (w != h)
			throw new Error("fillArc Not implemented for w!=h");
		start = -(start - end);
		while (start < 0)
			start += 360;
		start %= 360;
		shapeRender.arc(x + w / 2, height - y - h + h / 2, w / 2, -end, start);// 36);

	}

	public void copyArea(Image screen, int i, int j) {
		// TODO Auto-generated method stub

	}

	public void fillRoundRect(float x, float y, float w, float h, float m) {
		// TODO Auto-generated method stub
		checkMode(SHAPEFILLED);
		shapeRender.rect(x, height - y - h, w, h);
	}

	public void drawLine(float x1, float y1, float x2, float y2) {
		checkMode(SHAPELINE);
		shapeRender.line(x1, height - y1, x2, height - y2);
	}

	public void resetLineWidth() {
		// TODO Auto-generated method stub

	}

	static void endMode() {
		if (mode == SPRITE) {
			batch.end();
		} else if (mode == SHAPEFILLED || mode == SHAPELINE) {
			shapeRender.end();
		}

		mode = 0;
	}

	static void beginMode(int nmode) {
		if (nmode == SPRITE) {
			batch.begin();
		} else if (nmode == SHAPEFILLED) {
			Gdx.gl.glEnable(GL20.GL_BLEND);
			shapeRender.begin(ShapeType.Filled);
		} else if (nmode == SHAPELINE) {
			shapeRender.begin(ShapeType.Line);
		}
		mode = nmode;
	}

	public static void checkMode(int nmode) {
		if (mode != nmode) {
			endMode();
			beginMode(nmode);
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
		batch.draw(tex, x, height - y - hei, wid / 2, hei / 2, wid, hei, 1, 1,
				-rotation);
	}

	static Graphics g;

	public static Graphics getGraphics() {
		if (g == null) {
			g = new Graphics();
		}
		return g;
	}

}
