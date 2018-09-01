package itdelatrisu.opsu.storyboard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;

/**
 * Storyboard Object
 * 
 * @author fluddokt
 *
 */
public class SBObject implements Comparable<SBObject>{

	Image im;
	float x, y; //position
	float sx = 1, sy = 1; //scale
	float scaleAll = 1;
	
	Color color = new Color(Color.white);
	Color tempColor = new Color(Color.white);
	float rotation;
	float opacity;
	int lineNumber;
	
	boolean hFliped, vFliped, additiveBlend;
	
	//Initial Values
	float ix, iy;
	float iOpacity = 1;
	float iRotation;
	float isx = 1, isy = 1;
	float iscaleAll = 1;
	Color icolor = new Color(Color.white);
	boolean ihFliped, ivFliped, iadditiveBlend;
	
	//Time for current most initial value
	int ixTime = Integer.MAX_VALUE, iyTime = Integer.MAX_VALUE;
	int iOpacityTime = Integer.MAX_VALUE;
	int iRotationTime = Integer.MAX_VALUE;
	int iscaleTime = Integer.MAX_VALUE;
	int iscaleSPTime = Integer.MAX_VALUE;
	int iColorTime = Integer.MAX_VALUE;
	
	int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;
	
	public enum SBAlignH{
		Left, Centre, Right
	}
	public enum SBAlignV{
		Top, Centre, Bottom
	}
	//SBAlign align;
	SBAlignH alignH;
	SBAlignV alignV;
	ArrayList<SBCommand> commands = new ArrayList<SBCommand>();
	
	SBComEventRunner events = new SBComEventRunner();

	Set<SBCommand> activeCommands = new HashSet<>();
	Set<SBCommandTrigger> activeTriggers = new HashSet<>();
	
	public TreeSet<SBObject> layer;
	
	int index;
	
	int rand = (int) (Math.random() * 100);
	public boolean isActive = false;
	
	public SBObject(int line, int index, TreeSet<SBObject> layerTS, String align, Image im, float x, float y) {
		this.lineNumber = line;
		this.index = index;
		this.layer = layerTS;
		setAlign(align);
		this.im = im;
		this.ix = x;
		this.iy = y;
		
	}

	public void reset() {
		x = ix;
		y = iy;
		sx = isx;
		sy = isy;
		scaleAll = iscaleAll;
		rotation = iRotation;
		opacity = iOpacity;
		color.r = icolor.r;
		color.g = icolor.g;
		color.b = icolor.b;
		additiveBlend = iadditiveBlend;
		hFliped = ihFliped;
		vFliped = ivFliped;
		isActive = false;
		activeCommands.clear();
		events.reset();
	}

	protected static SGL GL = Renderer.get();
	
	public boolean render(Graphics g, float dimLevel) {
		if (opacity <= 0 || sx == 0 || sy == 0 || scaleAll == 0)
			return false;
		
		if (sx < 0)
			sx = -sx;
		if (sy < 0)
			sy = -sy;
		float width = sx * scaleAll * im.getWidth()*Storyboard.getYMultiplier();
		float height = sy * scaleAll * im.getHeight()*Storyboard.getYMultiplier();
		float orginx = 0;
		float orginy = 0;
		
		switch(alignV){
			case Top: break;
			case Centre: orginy = height/2; break;
			case Bottom: orginy = height;   break;
				
		}
		
		switch(alignH){
			case Left: break;
			case Centre: orginx = width/2; break;
			case Right:  orginx = width;   break;
		}
		im.setAlpha(opacity);
		if (additiveBlend)
			g.setDrawMode(Graphics.MODE_NORMAL_ADDITIVE_BLEND);
		else
			g.setDrawMode(Graphics.MODE_NORMAL);
		
		if (hFliped || vFliped) {
			im.setFlipped(hFliped, vFliped);
		}
		im.setCenterOfRotation(orginx, orginy);
		im.setRotation((float) (rotation * 180 / Math.PI));
		
		if (dimLevel == 0)
			im.drawCenterRot(x, y, width, height, color);
		else {
			tempColor.r = color.r * dimLevel;
			tempColor.g = color.g * dimLevel;
			tempColor.b = color.b * dimLevel;
			tempColor.a = color.a;
			
			im.drawCenterRot(x, y, width, height, tempColor);
		}
		if (hFliped || vFliped)
			im.setFlipped(false, false);
		/*
		//x+= lineNumber;
		g.setFont(itdelatrisu.opsu.ui.Fonts.LARGE);
		g.setColor(Color.black);
		//g.drawRect(x-orginx, y-orginy, width, height);
		String txt = lineNumber+" "+activeCommands.size()+" "+events.eventsIndex+" "+opacity;
		
		
		g.drawString(txt, x+rand*10-1, y+rand*10-1);
		//g.drawString(txt, x+rand+1, y+rand+1);
		g.setColor(Color.white);
		g.drawString(txt, x+rand, y+rand);
		//*/
		//g.setColor(opacity <= 0? Color.red : Color.white);
		//g.drawRect(x, y, 3, 3);
		
		return true;
	}
	
	public void setAlign(String align){
		switch(align){
		default:
		case "TopLeft":
			alignV = SBAlignV.Top; alignH = SBAlignH.Left;
			return;
		case "TopCentre":
			alignV = SBAlignV.Top; alignH = SBAlignH.Centre;
			return;
		case "TopRight":
			alignV = SBAlignV.Top; alignH = SBAlignH.Right;
			return;
		case "CentreLeft":
			alignV = SBAlignV.Centre; alignH = SBAlignH.Left;
			return;
		case "Centre":
			alignV = SBAlignV.Centre; alignH = SBAlignH.Centre;
			return;
		case "CentreRight":
			alignV = SBAlignV.Centre; alignH = SBAlignH.Right;
			return;
		case "BottomLeft":
			alignV = SBAlignV.Bottom; alignH = SBAlignH.Left;
			return;
		case "BottomCentre":
			alignV = SBAlignV.Bottom; alignH = SBAlignH.Centre;
			return;
		case "BottomRight":
			alignV = SBAlignV.Bottom; alignH = SBAlignH.Right;
			return;
		}
	}

	public void addEvent(SBCommand event) {
		commands.add(event);
	}

	public void update(int trackPosition) {
		events.update(trackPosition);

		for(SBCommand c : activeCommands)
			c.update(trackPosition);
		
		Iterator<SBCommandTrigger> ita = activeTriggers.iterator();
		while (ita.hasNext()) {
			SBCommandTrigger c = ita.next();
			c.update(trackPosition);
			if (!c.isRunning)
				ita.remove();
		}
	}

	public void init() {
		events.addCommands(commands, activeCommands);
		events.ready();
		for(SBCommand c2 : commands) {
			if (c2 instanceof SBCommandTrigger)
				iOpacity = 0;
		}
		for(SBCommand c2 : commands) {
			start = Math.min(start, c2.startTime);
			end = Math.max(end, c2.endTime);
			c2.setIntialVal(c2.startTime);
		}
		reset();
	}

	@Override
	public int compareTo(SBObject o) {
		return index - o.index;
	}

	@Override
	public String toString() {
		String t = "";
		//for(SBCommand s : activeCommands)
		//	t+=" "+s;
		return "SBObject line:"+lineNumber+" "+x+" "+y+" s:"+sx+" "+sy+" sa:"+scaleAll+" op:"+opacity+" "+rotation+" "+color+" "+t+" "+isx+" "+iscaleAll;
	}

	public void addActiveTrigger(SBCommandTrigger sbCommandTrigger) {
		activeTriggers.add(sbCommandTrigger);
	}
	
}
class SBAnimObject extends SBObject {

	private int frameDelay;
	private Image[] imgs;
	
	static int FOREVER = 0;
	static int ONCE = 1;
	
	private int loopType = FOREVER;

	public SBAnimObject(int line, int index, TreeSet<SBObject> layerTS, String align, Image[] im, float x, float y, int fc, int fDelay, String loopType) {
		super(line, index, layerTS, align, null, x, y);
		this.frameDelay = fDelay;
		this.imgs = im;
		this.im = imgs[0];
		if ("LoopOnce".equals(loopType)) {
			this.loopType = ONCE;
		}
	}

	@Override
	public void update(int trackPosition) {
		//0th frame based on initial start time
		
		int index = (trackPosition - start)/frameDelay;
		if(index >= imgs.length) {
			if (loopType == FOREVER) {
				index -= (index / imgs.length) * imgs.length;
			} else {
				index = imgs.length - 1;
			}
		}
		if(index >= 0) {
			im = imgs[index];
		}
		super.update(trackPosition);
	}
	
}
