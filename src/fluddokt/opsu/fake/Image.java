package fluddokt.opsu.fake;

import java.util.HashMap;
import java.util.HashSet;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Image {

	TextureRegion tex;
	float width,height;
	private float alpha=1f, rotation=0;
	String name;
	String filename;
	TextureInfo texinfo;
	static HashMap<String, TextureInfo> texmap = new HashMap<String, TextureInfo>();
	private class TextureInfo{
		Texture tex;
		int pw,ph;
		//HashSet<Image> set = new HashSet<Image>();
		int refcnt = 0;
		String name;
		public TextureInfo(String name, Texture tex, int pw, int ph) {
			this.name = name;
			this.tex = tex;
			this.pw = pw;
			this.ph = ph;
		}
		public void add(Image img){
//			set.add(img);
			refcnt++;
		}
		public void remove(Image img){
			refcnt--;
			//if(set.size()<=0){
			if(refcnt<=0){
				System.out.println("Remove TextureInfo :"+name);
				tex.dispose();
				texmap.remove(img.filename);
			}
		}
	}
	public Image(String filename) throws SlickException{
		this.filename = filename;
		Texture texture = null;
		//*
		texinfo = texmap.get(filename);
		int pw,ph;
		if(texinfo == null){
			SqPixmapTextureData td = new SqPixmapTextureData(ResourceLoader.getFileHandle(filename));
			texture = new Texture(td);
			pw = td.getImgWidth();
			ph = td.getImgHeight();
			texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
			texinfo = new TextureInfo(filename, texture, pw, ph);
			texmap.put(filename,texinfo);
		}
		
		pw = texinfo.pw;
		ph = texinfo.ph;
		texture = texinfo.tex;
		texinfo.add(this);
		//p.dispose();
		/*/
		texture = new Texture(ResourceLoader.getFileHandle(filename));
		
		int pw = texture.getWidth();
		int ph = texture.getHeight();
		//*/
//		System.out.println("TexMang :"
//				+Texture.getManagedStatus()+" "
//				+Texture.getNumManagedTextures());
		tex = new TextureRegion(texture, pw, ph);
		//ttex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		//tex = new TextureRegion(ttex);
		width = tex.getRegionWidth();
		height = tex.getRegionHeight();
		name=filename;
	}
	class SqPixmapTextureData implements TextureData
	{
		FileHandle file;
		Pixmap p;
		boolean inited;
		int pw,ph, npw,nph;
		Format pformat;
		
		public SqPixmapTextureData(FileHandle file) {
			this.file = file;
		}

		@Override
		public TextureDataType getType() {
			//System.out.println("getType "+file.path());
			return TextureDataType.Pixmap;

		}
		private void loadPixmap(){
			p = new Pixmap(file);
			pw = p.getWidth();
			ph = p.getHeight();
			int pw4 = nextmultipleof4(pw);
			int ph4 = nextmultipleof4(ph);
			if((pw != pw4 || ph != ph4) 
					//&&false
					){
				//System.out.println("Creating Image align 4 "+pw+" "+ph+" "+pw4+" "+ph4);
				Pixmap p2 = new Pixmap(pw4, ph4, Format.RGBA8888);
				Pixmap.setBlending(Pixmap.Blending.None);
				p2.drawPixmap(p, 0, 0);
				p.dispose();
				p = p2;
			}else{
				
			}
			npw = p.getWidth();
			nph = p.getHeight();
			pformat = p.getFormat();
			inited = true;
		}

		@Override
		public boolean isPrepared() {
			//System.out.println("isPrepared "+file.path());
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public void prepare() {
			 throw new GdxRuntimeException("This TextureData implementation does not upload data itself");
		}

		@Override
		public Pixmap consumePixmap() {
			//System.out.println("consumePixmap "+file.path());
			if(p==null)
				loadPixmap();
			Pixmap t = p;
			p = null;
			return t;
		}

		@Override
		public boolean disposePixmap() {
			//System.out.println("disposePixmap "+file.path());
			return true;
		}

		@Override
		public void consumeCustomData(int target) {
			throw new GdxRuntimeException("prepare() must not be called on a PixmapTextureData instance as it is already prepared.");
	
		}

		public int getImgWidth() {
			//System.out.println("getWidth");
			if(!inited)
				loadPixmap();
			return pw;
		}

		public int getImgHeight() {
			//System.out.println("getHeight");
			if(!inited)
				loadPixmap();
			return ph;
		}
		@Override
		public int getWidth() {
			//System.out.println("getWidth");
			if(!inited)
				loadPixmap();
			return npw;
		}

		@Override
		public int getHeight() {
			//System.out.println("getHeight");
			if(!inited)
				loadPixmap();
			return nph;
		}

		@Override
		public Format getFormat() {
			//System.out.println("getFormat "+file.path());
			if(!inited)
				loadPixmap();
			return pformat;
		}

		@Override
		public boolean useMipMaps() {
			//System.out.println("useMipMaps "+file.path());
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isManaged() {
			//System.out.println("isManaged "+file.path());
			// TODO Auto-generated method stub
			return true;
		}
		
	}
	private int gpow2(int n){
		int pow2 = 1;
		while( pow2<n){
			pow2<<=1;// *=2
		}
		return pow2;
	}
	private int nextmultipleof4(int n){
		return ((n+3)/4)*4;
	}
	public Image(Image copy, float wid, float hei) {
		texinfo = copy.texinfo;
		texinfo.add(this);
		
		tex = copy.tex;
		width = wid;
		height = hei;
		filename = copy.filename;
		name = copy.name+" s "+wid+" "+hei;
	}
	public Image(Image copy, int x, int y,int wid,int hei) {
		texinfo = copy.texinfo;
		texinfo.add(this);
		
		float dx = copy.tex.getRegionWidth()/(float)copy.width;
		float dy = copy.tex.getRegionHeight()/(float)copy.height;
		tex = new TextureRegion(copy.tex, Math.round(x*dy), Math.round(y*dy), Math.round(wid*dx), Math.round(hei*dy));
		width=(tex.getRegionWidth()/dx);
		height=(tex.getRegionHeight()/dy);
		filename = copy.filename;
		name = copy.name+" r "+x+" "+y+" "+wid+" "+hei;
	}
	public Image() {
	}
	public Image(int width, int height) {
		this.width = width;
		this.height = height;
		name = "null image";
	}

	public int getHeight() {
		return (int)height;
	}
	public int getWidth() {
		return (int)width;
	}
	public Image getScaledCopy(float w, float h) {
		return new Image(this,w,h);
	}
	
	public Image getScaledCopy(float f) {
		return new Image(this,width*f,height*f);
	}

	public void setAlpha(float f) {
		this.alpha = f;
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	boolean destroyed = false;
	public void destroy() throws SlickException {
		//System.out.println("Destroy :"+name);
		if(!destroyed)
			texinfo.remove(this);
		destroyed=true;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public void draw() {
		Graphics.getGraphics().setColor(Color.white.multAlpha(alpha));
		Graphics.getGraphics().drawTexture(getTextureRegion(), 0, 0, getWidth(),getHeight(),rotation);
	}
	public void draw(float x, float y) {
		Graphics.getGraphics().setColor(Color.white.multAlpha(alpha));
		Graphics.getGraphics().drawTexture(getTextureRegion(),x,y, getWidth(),getHeight(),rotation);
	}

	public void draw(float x, float y, Color filter) {
		Graphics.getGraphics().setColor(filter.multAlpha(alpha));
		Graphics.getGraphics().drawTexture(getTextureRegion(), x, y, getWidth(),getHeight(),rotation);
	}
	public void drawCentered(float x, float y) {
		Graphics.getGraphics().setColor(Color.white.multAlpha(alpha));
		Graphics.getGraphics().drawTexture(getTextureRegion(), x-getWidth()/2, y-getHeight()/2,getWidth(),getHeight(),rotation);
	}
	public TextureRegion getTextureRegion(){
		return tex;
	}
	

	public Image getSubImage(int x, int y, int w, int h) {
		Image img = new Image(this,x,y,w,h);
		
		return img;
	}

	public float getRotation() {
		return rotation;
	}

	public void rotate(float f) {
		rotation+=f;
		
	}
	public String getResourceReference() {
		return filename;
	}

	

}
