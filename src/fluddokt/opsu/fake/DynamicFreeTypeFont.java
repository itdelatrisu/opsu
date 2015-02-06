package fluddokt.opsu.fake;

import java.util.ArrayList;
import java.util.HashMap;

import sun.font.TrueTypeFont;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
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
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Size;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.SizeMetrics;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sun.corba.se.impl.ior.ByteBuffer;

class DynamicFreeType{
	FileHandle handle;
	private Face face;
	int thiscnt=0;
	static int cnt = 0;
	Font fontParam;
	int ascent,descent,height;
	public DynamicFreeType(FileHandle font, Font fontParam) {
		this.fontParam = fontParam;
		String filePath = font.pathWithoutExtension();
		Library library = FreeType.initFreeType();
		if (library == null) throw new GdxRuntimeException("Couldn't initialize FreeType");
		face = FreeType.newFace(library, font, 0);
		//face.
		if (face == null) throw new GdxRuntimeException("Couldn't create face for font '" + font + "'");
		
		
		if(!FreeType.setPixelSizes(face, 0, (int)fontParam.size)){
			throw new GdxRuntimeException("Couldn't set size for font '" + font + "'");
		}
		
		SizeMetrics sizes = face.getSize().getMetrics();
		ascent = FreeType.toInt(sizes.getAscender());
		descent = FreeType.toInt(sizes.getDescender());
		height = FreeType.toInt(sizes.getHeight());
		//System.out.println("Face flag "+face.getFaceFlags());
		
		//if (checkForBitmapFont()) {
		//return;
		//}
		//if (!face.setPixelSizes(0, 15)) throw new GdxRuntimeException("Couldn't set size for font '" + font + "'");
		thiscnt = cnt++;
	}

	//ArrayList<Texture> pages = new ArrayList<Texture>();
	HashMap<Character, CharInfo> charmap = new HashMap<Character, CharInfo>();
	Pixmap curPixmap;
	Texture curTexture;
	class CharInfo{
		TextureRegion region;
		public float horadvance;
		public float xbear;
		public float ybear;
		public float height;
		public int bitmaptop;
		public int yoffset;
		public int xoffset;
	}
	public void draw(SpriteBatch batch, String str, float x, float y) {
		// TODO Auto-generated method stub
		//if(curTexture!=null)
		//	batch.draw(curTexture,0,thiscnt*64);
		char prevchr = 0;
		for(int i=0; i<str.length(); i++){
			char thischr = str.charAt(i);
			CharInfo ci = getCharInfo(str.charAt(i));
			TextureRegion tr = ci.region;
			batch.draw(tr, x +ci.xbear//xoffset
					, y - ascent + ci.ybear -ci.height);//ci.yoffset);//- tr.getRegionHeight() + ci.ybear );//-
			if(FreeType.hasKerning(face)){
				System.out.println("KERNING");
				x+=FreeType.getKerning(face, prevchr, thischr, FreeType.FT_KERNING_DEFAULT);
			}else{
				/*
				if(thischr == ' ')
					x+=ci.horadvance;
				else{
					x+=tr.getRegionWidth()+2;
				}
				/*/
				x+= ci.horadvance;
				//*/
			}
			prevchr = thischr;
			
		}
	}
	private float to26p6float(int n){
		//System.out.println(n+" "+(1<<6));
		return n/(float)(1<<6);
	}
	private CharInfo getCharInfo(char charAt) {
		CharInfo ci = charmap.get(charAt);
		if(ci == null){
			ci = addChar(charAt);
			charmap.put(charAt, ci);
		}
		return ci;
	}

	int x,y,maxHeight;
	public CharInfo addChar(char c){
		FreeType.loadChar(face, c, 
				FreeType.
				//FT_LOAD_RENDER
				//FT_LOAD_TARGET_LIGHT
				FT_LOAD_DEFAULT
				);// FT_LOAD_MONOCHROME FT_RENDER_MODE_LIGHT
		GlyphSlot slot = face.getGlyph();
		FreeType.renderGlyph(slot, FreeType.FT_RENDER_MODE_LIGHT);
		Bitmap bitmap = slot.getBitmap();
		//java.nio.ByteBuffer b = bitmap.getBuffer();
		
		//System.out.println("Pixel Mode "+bitmap.getPixelMode());
		Pixmap pixmap;
		if(bitmap.getPixelMode() == FreeType.FT_PIXEL_MODE_GRAY){
			pixmap = bitmap.getPixmap(Format.RGBA8888);
			/*pixmap = new Pixmap(bitmap.getWidth(),bitmap.getRows(),Format.RGBA8888);
			java.nio.ByteBuffer rbuf = bitmap.getBuffer();
			java.nio.ByteBuffer wbuf = pixmap.getPixels();
			
			for(int y=0; y<pixmap.getHeight(); y++){
				for(int x=0; x<pixmap.getWidth(); x++){
					byte curbyte = rbuf.get();
					wbuf.putInt((curbyte&0xff) | 0xffffff00);
				}
			}*/
			
		}else if(bitmap.getPixelMode() == FreeType.FT_PIXEL_MODE_MONO){
			pixmap = new Pixmap(bitmap.getWidth(),bitmap.getRows(),Format.RGBA8888);
			java.nio.ByteBuffer rbuf = bitmap.getBuffer();
			java.nio.ByteBuffer wbuf = pixmap.getPixels();
			
			byte curbyte = rbuf.get();
			int bitAt = 0;
			for(int y=0; y<pixmap.getHeight(); y++){
				for(int x=0; x<pixmap.getWidth(); x++){
					if(((curbyte>>(7-bitAt))&1) > 0){
						wbuf.putInt(0xffffffff);
					}else{
						wbuf.putInt(0x00000000);
					}
					bitAt++;
					if(bitAt>=8){
						bitAt=0;
						if(rbuf.hasRemaining())
							curbyte = rbuf.get();
					}
				}
				if(bitAt>0){
					bitAt=0;
					if(rbuf.hasRemaining())
						curbyte = rbuf.get();
				}
			}
		}else{
			throw new GdxRuntimeException("Unknown Freetype pixel mode :"+bitmap.getPixelMode());
		}
		//create a new page
		if(curPixmap == null || y+pixmap.getHeight() > curPixmap.getHeight()){
			x=0;
			y=0;
			maxHeight=0;
			curPixmap = new Pixmap(512, 512, Format.RGBA8888);
			curTexture = new Texture(new PixmapTextureData(curPixmap, null, false, false, true));
		}
		//cant fit width, go to next line
		if(x+pixmap.getWidth() > curPixmap.getWidth()){
			x=0;
			y+=maxHeight;
			maxHeight=0;
		}
		//find the max Height of the this line
		if(pixmap.getHeight()>maxHeight){
			maxHeight = pixmap.getHeight();
		}
		
		
		curPixmap.drawPixmap(pixmap, x, y);
		
		curTexture.load(new PixmapTextureData(curPixmap, null, false, false, true));
		
		TextureRegion tr = new TextureRegion(curTexture, x,y,pixmap.getWidth(), pixmap.getHeight());
		x+=pixmap.getWidth();
		
		GlyphMetrics metrics = slot.getMetrics();
		CharInfo ci = new CharInfo();
		ci.region = tr;
		ci.horadvance = to26p6float(metrics.getHoriAdvance());//slot.getLinearHoriAdvance()>>16;
		ci.xbear = to26p6float(metrics.getHoriBearingX());
		ci.ybear = to26p6float(metrics.getHoriBearingY());
		ci.height = to26p6float(metrics.getHeight());
		ci.bitmaptop = slot.getBitmapTop();
		ci.yoffset = slot.getBitmapTop() - pixmap.getHeight();
		ci.xoffset = slot.getBitmapLeft();
		
		/*System.out.println(
				c
				+" fs:"+fontParam.size
				+" fs:"+fontParam.style
				+" hadv:"+ci.horadvance
				+" xbear:"+ci.xbear
				+" ybear:"+ci.ybear
				+" height:"+ci.height
				+" bitmaptop:"+ci.bitmaptop
				+" yoffset:"+ci.yoffset
				+" xoffset:"+ci.xoffset
				+" pixWid:"+pixmap.getWidth()
				+" pixtHei:"+pixmap.getHeight()
				);*/
		return ci;
	}
	public int getHeight(String str) {
		float max = 0;
		for(int i=0; i<str.length(); i++){
			float t = getCharInfo(str.charAt(i)).height;
			if(t > max)
				max = t;
		}
		return (int) max;//getLineHeight();//FreeType.toInt(face.getMaxAdvanceHeight());
	}
	public int getWidth(String str) {
		float len = 0;
		for(int i=0; i<str.length(); i++){
			float t = getCharInfo(str.charAt(i)).horadvance;
					//FreeType.;
			len += t;
		}
		return (int) len;//FreeType.toInt(face.getMaxAdvanceWidth())*str.length();
	}
	public int getLineHeight() {
		return ascent;
			
	}
	
}