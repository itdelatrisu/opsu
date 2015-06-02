package fluddokt.opsu.fake;

import java.util.HashMap;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Bitmap;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Face;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.GlyphMetrics;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.GlyphSlot;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Library;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.SizeMetrics;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class DynamicFreeTypeFont {
	FileHandle handle;
	private Face face;
	int thiscnt = 0;
	static int cnt = 0;
	Font fontParam;
	int ascent, descent, height;
	boolean useKerning = false;

	public DynamicFreeTypeFont(FileHandle font, Font fontParam) {
		this.fontParam = fontParam;
		Library library = FreeType.initFreeType();
		if (library == null)
			throw new GdxRuntimeException("Couldn't initialize FreeType");
		face = FreeType.newFace(library, font, 0);

		if (face == null)
			throw new GdxRuntimeException("Couldn't create face for font '"
					+ font + "'");

		if (!FreeType.setPixelSizes(face, 0, (int) fontParam.size)) {
			throw new GdxRuntimeException("Couldn't set size for font '" + font
					+ "'");
		}

		SizeMetrics sizes = face.getSize().getMetrics();
		ascent = FreeType.toInt(sizes.getAscender());
		descent = FreeType.toInt(sizes.getDescender());
		height = FreeType.toInt(sizes.getHeight());
		// System.out.println("Face flag "+face.getFaceFlags());
		useKerning = FreeType.hasKerning(face);

		thiscnt = cnt++;
	}

	// ArrayList<Texture> pages = new ArrayList<Texture>();
	HashMap<Character, CharInfo> charmap = new HashMap<Character, CharInfo>();
	Pixmap curPixmap;
	Texture curTexture;

	class CharInfo {
		TextureRegion region;
		public float horadvance;
		public float xbear;
		public float ybear;
		public float height;
		public int sBitmapTop;
		public int yoffset;
		public int sBitmapLeft;
		public int bitmapPitch;
		public int bitmapRows;
		public int bitmapWidth;
	}

	public void draw(SpriteBatch batch, String str, float x, float y) {
		x = (int)x;
		y = (int)y;
		int prevchrIndex = 0;
		CharInfo prevCharInfo = null;
		float ox = x;
		for (int i = 0; i < str.length(); i++) {
			char thischr = str.charAt(i);
			if (thischr == '\n') {
				y += getLineHeight();
				x = ox;
				continue;
			}
				
			if (useKerning) {
				int thisChrIndex = FreeType.getCharIndex(face, thischr);
				float spacing = 0;//to26p6float(FreeType.getKerning(face, prevchrIndex, thisChrIndex,;
						//FreeType.FT_KERNING_DEFAULT));
				prevchrIndex = thisChrIndex;
				
				//OpenType kerning via the 'GPOS' table is not supported! You need a higher-level library like HarfBuzz, Pango, or ICU, 
				//System.out.println(spacing+" "+thischr);
				if(spacing==0 && prevCharInfo != null)
					spacing += prevCharInfo.horadvance;
				x += spacing;
			}
			
			CharInfo ci = getCharInfo(str.charAt(i));
			TextureRegion tr = ci.region;
			batch.draw(tr, x + ci.sBitmapLeft// xoffset
			, y - ci.sBitmapTop + ascent);// ci.yoffset);//-
													// tr.getRegionHeight() +
													// ci.ybear );//-
			if (!useKerning) {
				x += ci.horadvance;
			}
			prevCharInfo = ci;
		}
	}

	private float to26p6float(int n) {
		return n / (float) (1 << 6);
	}

	private CharInfo getCharInfo(char charAt) {
		CharInfo ci = charmap.get(charAt);
		if (ci == null) {
			ci = addChar(charAt);
			charmap.put(charAt, ci);
		}
		return ci;
	}

	int x, y, maxHeight;

	public CharInfo addChar(char c) {
		FreeType.loadChar(face, c,

		// FreeType.FT_LOAD_RENDER
		// FreeType.FT_LOAD_DEFAULT
		// FreeType.FT_LOAD_RENDER
				fontParam.size < 16 ? FreeType.FT_LOAD_DEFAULT : 
							FreeType.FT_LOAD_NO_HINTING
		 |FreeType.FT_LOAD_NO_BITMAP
		// FreeType.FT_LOAD_NO_AUTOHINT
		);// FT_LOAD_MONOCHROME FT_RENDER_MODE_LIGHT
		GlyphSlot slot = face.getGlyph();
		FreeType.renderGlyph(slot, FreeType.FT_RENDER_MODE_LIGHT);
		Bitmap bitmap = slot.getBitmap();

		// System.out.println("Pixel Mode "+bitmap.getPixelMode());
		Pixmap pixmap;
		if (bitmap.getPixelMode() == FreeType.FT_PIXEL_MODE_GRAY) {
			// pixmap = bitmap.getPixmap(Format.RGBA8888);
			// *
			pixmap = new Pixmap(bitmap.getWidth(), bitmap.getRows(),
					Format.RGBA8888);
			java.nio.ByteBuffer rbuf = bitmap.getBuffer();
			java.nio.ByteBuffer wbuf = pixmap.getPixels();

			for (int y = 0; y < pixmap.getHeight(); y++) {
				for (int x = 0; x < pixmap.getWidth(); x++) {
					byte curbyte = rbuf.get();
					wbuf.putInt((curbyte & 0xff) | 0xffffff00);
				}
			}// */

		} else if (bitmap.getPixelMode() == FreeType.FT_PIXEL_MODE_MONO) {
			pixmap = new Pixmap(bitmap.getWidth(), bitmap.getRows(),
					Format.RGBA8888);
			java.nio.ByteBuffer rbuf = bitmap.getBuffer();
			java.nio.ByteBuffer wbuf = pixmap.getPixels();

			byte curbyte = rbuf.get();
			int bitAt = 0;
			for (int y = 0; y < pixmap.getHeight(); y++) {
				for (int x = 0; x < pixmap.getWidth(); x++) {
					if (((curbyte >> (7 - bitAt)) & 1) > 0) {
						wbuf.putInt(0xffffffff);
					} else {
						wbuf.putInt(0x00000000);
					}
					bitAt++;
					if (bitAt >= 8) {
						bitAt = 0;
						if (rbuf.hasRemaining())
							curbyte = rbuf.get();
					}
				}
				if (bitAt > 0) {
					bitAt = 0;
					if (rbuf.hasRemaining())
						curbyte = rbuf.get();
				}
			}
		} else {
			throw new GdxRuntimeException("Unknown Freetype pixel mode :"
					+ bitmap.getPixelMode());
		}
		
		int pixMapWidth = pixmap.getWidth();
		if((fontParam.style&Font.BOLD) > 0){
		//	pixMapWidth+=1;
		}
		
		
		// create a new page
		if (curPixmap == null || y + pixmap.getHeight() > curPixmap.getHeight()) {
			x = 0;
			y = 0;
			maxHeight = 0;
			curPixmap = new Pixmap(512, 512, Format.RGBA8888);
			curTexture = new Texture(new PixmapTextureData(curPixmap, null,
					false, false, true));
			curPixmap.setColor(0);
			curPixmap.fill();
		}
		
		// cant fit width, go to next line
		if (x + pixMapWidth > curPixmap.getWidth()) {
			x = 0;
			y += maxHeight;
			maxHeight = 0;
		}
		// find the max Height of the this line
		if (pixmap.getHeight() > maxHeight) {
			maxHeight = pixmap.getHeight();
		}

		Pixmap.setBlending(Blending.None);
		curPixmap.drawPixmap(pixmap, x, y);
		if((fontParam.style&Font.BOLD) > 0){
			Pixmap.setBlending(Blending.SourceOver);
			curPixmap.drawPixmap(pixmap, x, y);
			curPixmap.drawPixmap(pixmap, x, y);
			Pixmap.setBlending(Blending.None);
		}
		

		curTexture.load(new PixmapTextureData(curPixmap, null, false, false,
				true));

		TextureRegion tr = new TextureRegion(curTexture, x, y,
				pixMapWidth, pixmap.getHeight());
		tr.flip(false, true);
		x += pixMapWidth;

		GlyphMetrics metrics = slot.getMetrics();
		CharInfo ci = new CharInfo();
		ci.region = tr;
		ci.horadvance = to26p6float(metrics.getHoriAdvance());// slot.getLinearHoriAdvance()>>16;
		ci.xbear = to26p6float(metrics.getHoriBearingX());
		ci.ybear = to26p6float(metrics.getHoriBearingY());
		ci.height = to26p6float(metrics.getHeight());
		ci.sBitmapTop = slot.getBitmapTop();
		ci.sBitmapLeft = slot.getBitmapLeft();
		ci.yoffset = slot.getBitmapTop() - pixmap.getHeight();
		ci.bitmapPitch = bitmap.getPitch();
		ci.bitmapRows = bitmap.getRows();
		ci.bitmapWidth = bitmap.getWidth();
		
		/*
		System.out.println("char: "+c+"hradv:"+ci.horadvance+" xbear:"+ci.xbear+" ybear"+ci.ybear+" height"+ci.height+
				" sBitmapTop"+ci.sBitmapTop+ " sBitmapLeft"+ci.sBitmapLeft
				+" bitmapPitch"+ci.bitmapPitch+" bitmapRows"+ci.bitmapRows+" bitmapWidth"+ci.bitmapWidth
				);*/
		pixmap.dispose();
		
		 
		return ci;
	}

	public int getHeight(String str) {
		/*float max = 0;
		for (int i = 0; i < str.length(); i++) {
			float t = getCharInfo(str.charAt(i)).height;
			if (t > max)
				max = t;
		}
		return (int) max;*/
		return getLineHeight();
	}

	public int getWidth(String str) {
		float len = 0;
		for (int i = 0; i < str.length(); i++) {
			float t = getCharInfo(str.charAt(i)).horadvance;
			len += t;
		}
		return (int) len;
	}

	public int getLineHeight() {
		return ascent - descent ;

	}

}