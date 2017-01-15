package fluddokt.opsu.fake;

import java.util.HashMap;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Image {

	TextureRegion tex;
	float width, height;
	private float alpha = 1f, rotation = 0;
	//String name;
	String filename;
	TextureInfo texinfo;
	static HashMap<String, TextureInfo> texmap = new HashMap<String, TextureInfo>();
	Image parentImage;
	FrameBuffer fb;
	FBGraphics fbg;
	Pixmap pixmap;
	
	private class FBGraphics extends Graphics{
		FrameBuffer fb;
		public FBGraphics(FrameBuffer fb) {
			this.fb = fb;
		}

		@Override
		protected void bind() {
			fb.bind();
		}

		@Override
		protected void unbind() {
			FrameBuffer.unbind();
		}
		
	}
	
	private class TextureInfo {
		Texture tex;
		int pw, ph;
		// HashSet<Image> set = new HashSet<Image>();
		int refcnt = 0;
		String name;

		public TextureInfo(String name, Texture tex, int pw, int ph) {
			this.name = name;
			this.tex = tex;
			this.pw = pw;
			this.ph = ph;
		}

		public void add(Image img) {
			// set.add(img);
			refcnt++;
		}

		public void remove(Image img) {
			refcnt--;
			// if(set.size()<=0){
			System.out.println("RefCnt :"+name+" "+refcnt);
			if (refcnt <= 0) {
				System.out.println("Remove TextureInfo :" + name);
				tex.dispose();
				texmap.remove(img.filename);
			}
		}
	}

	public Image(String filename) throws SlickException {
		this.filename = filename;
		Texture texture = null;
		// *
		texinfo = texmap.get(filename);
		int pw, ph;
		if (texinfo == null) {
			//*
			SqPixmapTextureData td = new SqPixmapTextureData(
					ResourceLoader.getFileHandle(filename));
			texture = new Texture(td);
			pw = td.getImgWidth();
			ph = td.getImgHeight();
			/*/
			texture = new Texture(ResourceLoader.getFileHandle(filename));
			pw = texture.getWidth();
			ph = texture.getHeight();
			//*/
			texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
			texinfo = new TextureInfo(filename, texture, pw, ph);
			texmap.put(filename, texinfo);
		}

		pw = texinfo.pw;
		ph = texinfo.ph;
		texture = texinfo.tex;
		texinfo.add(this);
		tex = new TextureRegion(texture, pw, ph);
		tex.flip(false, true);
		width = tex.getRegionWidth();
		height = tex.getRegionHeight();
		//name = filename;
	}

	class SqPixmapTextureData implements TextureData {
		FileHandle file;
		Pixmap p;
		boolean inited;
		int pw, ph, npw, nph;
		Format pformat;

		public SqPixmapTextureData(FileHandle file) {
			this.file = file;
		}

		@Override
		public TextureDataType getType() {
			return TextureDataType.Pixmap;

		}

		private void loadPixmap() {
			try {
				p = new Pixmap(file);
			} catch (GdxRuntimeException e) {
				// TODO Fails to load some pngs. Try javapng / pngj?
				e.printStackTrace();
				p = new Pixmap(32, 32, Format.RGBA8888);
			}
			pw = p.getWidth();
			ph = p.getHeight();
			//int pw4 = gpow2(pw);
			//int ph4 = gpow2(ph);
			int pw4 = nextmultipleof4(pw);
			int ph4 = nextmultipleof4(ph);
			if ((pw != pw4 || ph != ph4)
			// &&false
			) {
				// System.out.println("Creating Image align 4 "+pw+" "+ph+" "+pw4+" "+ph4);
				Pixmap p2 = new Pixmap(pw4, ph4, Format.RGBA8888);
				Pixmap.setBlending(Pixmap.Blending.None);
				p2.drawPixmap(p, 0, 0);
				p.dispose();
				p = p2;
			} else {

			}
			npw = p.getWidth();
			nph = p.getHeight();
			pformat = p.getFormat();
			inited = true;
			
		}

		@Override
		public boolean isPrepared() {
			return true;
		}

		@Override
		public void prepare() {
			throw new GdxRuntimeException(
					"This TextureData implementation does not upload data itself");
		}

		@Override
		public Pixmap consumePixmap() {
			if (p == null)
				loadPixmap();
			Pixmap t = p;
			p = null;
			return t;
		}

		@Override
		public boolean disposePixmap() {
			return true;
		}

		@Override
		public void consumeCustomData(int target) {
			throw new GdxRuntimeException(
					"prepare() must not be called on a PixmapTextureData instance as it is already prepared.");

		}

		public int getImgWidth() {
			if (!inited)
				loadPixmap();
			return pw;
		}

		public int getImgHeight() {
			if (!inited)
				loadPixmap();
			return ph;
		}

		@Override
		public int getWidth() {
			if (!inited)
				loadPixmap();
			return npw;
		}

		@Override
		public int getHeight() {
			if (!inited)
				loadPixmap();
			return nph;
		}

		@Override
		public Format getFormat() {
			if (!inited)
				loadPixmap();
			return pformat;
		}

		@Override
		public boolean useMipMaps() {
			return false;
		}

		@Override
		public boolean isManaged() {
			return true;
		}

	}

	private int gpow2(int n) {
		int pow2 = 1;
		while (pow2 < n) {
			pow2 <<= 1;// *=2
		}
		return pow2;
	}

	private int nextmultipleof4(int n) {
		return ((n + 3) / 4) * 4;
	}
	
	public Image(Image copy) {
		//texinfo = copy.texinfo;
		//texinfo.add(this);
		parentImage = copy;

		tex = copy.tex;
		width = copy.width;
		height = copy.height;
		filename = copy.filename;
		//name = copy.name+"[c]";
	}
	public Image(Image copy, float wid, float hei) {
		//texinfo = copy.texinfo;
		//texinfo.add(this);
		parentImage = copy;

		tex = copy.tex;
		width = wid;
		height = hei;
		filename = copy.filename;
		//name = copy.name + " s " + wid + " " + hei;
	}

	public Image(Image copy, int x, int y, int wid, int hei) {
		//texinfo = copy.texinfo;
		//texinfo.add(this);
		parentImage = copy;

		float dx = copy.tex.getRegionWidth() / (float) copy.width;
		float dy = copy.tex.getRegionHeight() / (float) copy.height;
		tex = new TextureRegion(copy.tex, 
				Math.round(x * dy),
				Math.round((hei+y) * dy)-copy.tex.getRegionHeight(),
				Math.round(wid * dx), 
				-Math.round(hei * dy));
		//tex.flip(false, true);
		width = (tex.getRegionWidth() / dx);
		height = (tex.getRegionHeight() / dy);
		filename = copy.filename;
		//name = copy.name + " r " + x + " " + y + " " + wid + " " + hei;
	}

	public Image() {
	}

	public Image(int width, int height) {
		this.width = width;
		this.height = height;
		fb = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
		fbg = new FBGraphics(fb);
		tex = new TextureRegion(fb.getColorBufferTexture());
		//name = "FrameBuffer image";
	}

	public int getHeight() {
		return (int) height;
	}

	public int getWidth() {
		return (int) width;
	}

	public Image getScaledCopy(float w, float h) {
		return new Image(this, w, h);
	}

	public Image getScaledCopy(float f) {
		return new Image(this, width * f, height * f);
	}

	public void setAlpha(float f) {
		this.alpha = f;
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	boolean destroyed = false;

	public void destroy() throws SlickException {
		// System.out.println("Destroy :"+name);
		if (!destroyed){
			if(parentImage != null)
				parentImage.destroy();
			if(texinfo != null)
				texinfo.remove(this);
			
			if(fb != null){
				fb.dispose();
			}
		}
		
		destroyed = true;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	static Color tempColor = new Color();
	public void draw() {
		draw(0, 0, Color.white);
	}

	public void draw(float x, float y) {
		draw(x, y, Color.white);
	}

	public void draw(float x, float y, Color color) {
		Graphics.getGraphics().setColorAlpha(color, alpha);
		Graphics.getGraphics().drawTexture(getTextureRegion(), x, y,
				getWidth(), getHeight(), rotation);
	}


	public void drawCentered(float x, float y, Color color) {
		draw(x - getWidth() / 2, y - getHeight() / 2, color);
	}

	public void draw(float x, float y, float w, float h) {
		Graphics.getGraphics().setColorAlpha(Color.white, alpha);
		Graphics.getGraphics().drawTexture(getTextureRegion(), x, y,
				w, h, rotation);
	}
	
	public void drawCentered(float x, float y) {
		drawCentered(x, y, Color.white);
	}

	public TextureRegion getTextureRegion() {
		return tex;
	}

	public Image getSubImage(int x, int y, int w, int h) {
		Image img = new Image(this, x, y, w, h);
		
		return img;
	}

	public float getRotation() {
		return rotation;
	}

	public void rotate(float f) {
		rotation += f;

	}

	public String getResourceReference() {
		return filename;
	}

	public float getAlphaAt(int x, int y) {
		if(pixmap == null)
			pixmap = new Pixmap(ResourceLoader.getFileHandle(filename));
		return pixmap.getPixel((int)(x *pixmap.getWidth() / width), (int) (y * pixmap.getHeight() / height))&0xff;
	}

	public Graphics getGraphics() {
		if(fb != null && fbg != null){
			return fbg;
		}else{
			throw new Error("Getting graphics for non framebuffer image");
		}
	}

	//Color singGetColor = new Color();
	public Color getColor(int x, int y) {
		if(pixmap == null)
			pixmap = new Pixmap(ResourceLoader.getFileHandle(filename));
		return new Color(pixmap.getPixel((int)(x *pixmap.getWidth() / width), (int) (y * pixmap.getHeight() / height)));
	}

	public Image copy() {
		return new Image(this);
	}

	public void startUse() {
		// TODO Auto-generated method stub
		
	}
	public void endUse() {
		// TODO Auto-generated method stub
		
	}

	Color imageColor = new Color(0xffffffff);
	public void setImageColor(float r, float g, float b, float a) {
		imageColor.init(r, g, b, a);
	}

	public void drawEmbedded(float x, float y, int w, int h, float r) {
		Graphics.getGraphics().setColor(imageColor);
		Graphics.getGraphics().drawTexture(getTextureRegion(), x, y,
				w, h, r);
		
	}

	public void drawEmbedded(float x, float y, float w, float h, int angle) {
		Graphics.getGraphics().setColor(imageColor);
		Graphics.getGraphics().drawTexture(getTextureRegion(), x, y,
				w, h, angle);
	}

	public void setFlipped(boolean x, boolean y) {
		/*    isFlipped false true
		flip? false     false true
		      true      true  false
		*/
		tex.flip(x ^ tex.isFlipX(), y ^ !tex.isFlipY());
	}

	
}
