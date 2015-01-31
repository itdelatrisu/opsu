package fluddokt.opsu.fake;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Animation extends Image{

	ArrayList<ImageDura> imgs = new ArrayList<ImageDura>();
	class ImageDura{
		Image img;
		int duration;
		public ImageDura(Image img, int duration){
			this.img = img;
			this.duration = duration;
			
		}
	}
	int curFrame;

	public Animation() {
	}
	
	public Animation(String filename) throws SlickException {
		super(filename);
		throw new Error("Not IMplemented");
	}

	public Animation(SpriteSheet spr, int i) {
		//TODO FIX ME
		for(int y=0;y<spr.image.getHeight();y+=spr.height){
			for(int x=0; x<spr.image.getWidth();x+=spr.width){
				addFrame(new Image(spr.image,x,y,spr.width,spr.height),i);
			}
		}
	}

	public Animation(Image[] imgs, int dura) {
		for(int i=0; i<imgs.length; i++){
			addFrame(imgs[i], dura);
		}
	}


	@Override
	public int getHeight() {
		return imgs.get(0).img.getHeight();
	}

	@Override
	public int getWidth() {
		return imgs.get(0).img.getHeight();
	}
	public Image getScaledCopy(float w, float h) {
		throw new Error("Not IMplemented");
	}
	
	public Image getScaledCopy(float f) {
		throw new Error("Not IMplemented");
		
	}



	public int getFrameCount() {
		return imgs.size();
	}

	public Image getImage(int i) {
		ImageDura im = imgs.get(i);
		return im.img;
	}

	public void addFrame(Image img, int dura) {
		imgs.add(new ImageDura(
				img//.getScaledCopy(1f)
				, dura));
	}

	long lastUpdate = System.currentTimeMillis();
	@Override
	public TextureRegion getTextureRegion() {
		while(System.currentTimeMillis()-lastUpdate>imgs.get(curFrame).duration){
			lastUpdate=System.currentTimeMillis();
			curFrame = (curFrame+1)%getFrameCount();
		}
		//System.out.println("curFrame Anim:"+curFrame);
		return getImage(curFrame).tex;
	}

	public Image getCurrentFrame() {
		return getImage(curFrame);
	}
	

}
