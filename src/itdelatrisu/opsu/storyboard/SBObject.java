package itdelatrisu.opsu.storyboard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;

import itdelatrisu.opsu.ui.Fonts;

public class SBObject implements Comparable<SBObject>{

	Image im;
	float x, y; //position
	float sx = 1, sy = 1; //scale
	float scaleAll = 1;
	
	Color color = new Color(Color.white);
	Color tempColor = new Color(0xffffff);
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
	
	int ixTime = Integer.MAX_VALUE, iyTime = Integer.MAX_VALUE;
	int iOpacityTime = Integer.MAX_VALUE;
	int iRotationTime = Integer.MAX_VALUE;
	int iscaleTime = Integer.MAX_VALUE;
	int iscaleSPTime = Integer.MAX_VALUE;
	int iColorTime = Integer.MAX_VALUE;
	
	
	int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;
	
	/*public enum SBAlign{
		TopLeft, TopCentre, TopRight,
		CentreLeft, Centre, CentreRight,
		BottomLeft, BottomCentre, BottomRight,
	}*/
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

	HashSet<SBCommand> activeCommands = new HashSet<>();
	HashSet<SBCommandTrigger> activeTriggers = new HashSet<>();
	
	public TreeSet<SBObject> layer;
	
	int index;
	
	int rand = (int) (Math.random() * 100);
	public boolean isActive = false;
	
	public SBObject(int line, int index, TreeSet<SBObject> layerTS, String align, Image im, float x, float y) {
		this.lineNumber = line;
		this.index = index;
		this.layer = layerTS;
		//this.align = getAlign(align);
		setAlign(align);
		this.im = im;
		this.ix = x;
		this.iy = y;
		
	}

	public void reset() {
		x = ix;
		y = iy;
		sx = isy;
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
		if (!isActive || opacity <= 0 || sx == 0 || sy == 0 || scaleAll == 0)
			return false;
		/*
		//x+= lineNumber;
		g.setFont(Fonts.LARGE);
		g.setColor(Color.black);
		//g.drawRect(x-orginx, y-orginy, width, height);
		String txt = lineNumber+" "+activeCommands.size()+" "+events.eventsIndex+" "+opacity;
		
		
		g.drawString(txt, x+rand-1, y+rand-1);
		//g.drawString(txt, x+rand+1, y+rand+1);
		g.setColor(Color.white);
		g.drawString(txt, x+rand, y+rand);
		//*/
		//g.setColor(opacity <= 0? Color.red : Color.white);
		//g.drawRect(x, y, 3, 3);
		if (sx < 0)
			sx = -sx;
		if (sy < 0)
			sy = -sy;
		float width = sx * scaleAll * im.getWidth()*Storyboard.getYMultiplier();
		float height = sy * scaleAll* im.getHeight()*Storyboard.getYMultiplier();
		float orginx = 0;
		float orginy = 0;
	/*	switch(align){
			case TopLeft:
			case TopCentre:
			case TopRight:
				break;
			case CentreLeft:
			case Centre:
			case CentreRight:
				orginy = height/2;
				break;
			case BottomLeft:
			case BottomCentre:
			case BottomRight:
				orginy = height;
				break;
				
		}
		
		switch(align){
			case TopLeft:
			case CentreLeft:
			case BottomLeft:
				break;
			case TopCentre:
			case Centre:
			case BottomCentre:
				orginx = width/2;
				break;
			case TopRight:
			case CentreRight:
			case BottomRight:
				orginx = width;
					break;
		}*/
		
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
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
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
		if (additiveBlend)
			g.setDrawMode(Graphics.MODE_NORMAL);
		
		g.setLineWidth(1f);
		
		//System.out.println("Render:"+im+" "+x+" "+y+" "+opacity+" "+orginx+" "+orginy+" "+sx);
		return true;
	}
	/*public static SBAlign getAlign(String align){
		switch(align){
		default:
		case "TopLeft": return SBAlign.TopLeft;
		case "TopCentre": return SBAlign.TopCentre;
		case "TopRight": return SBAlign.TopRight;
		case "CentreLeft": return SBAlign.CentreLeft;
		case "Centre": return SBAlign.Centre;
		case "CentreRight": return SBAlign.CentreRight;
		case "BottomLeft": return SBAlign.BottomLeft;
		case "BottomCentre": return SBAlign.BottomCentre;
		case "BottomRight": return SBAlign.BottomRight;
		}
	}*/
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

	List<SBCommandTrigger> trigClearList = new ArrayList<>();
	public void update(int trackPosition) {
		//if (opacity <= 0)
		//	return;
		//visible = trackPosition>start && trackPosition<end;
		events.update(trackPosition);
		//if (! visible || opacity <= 0)
		//	return;
		for(SBCommand c : activeCommands)
			c.update(trackPosition);
		
		for(SBCommandTrigger c : activeTriggers) {
			c.update(trackPosition);
			if (!c.isRunning)
				trigClearList.add(c);
		}
		activeTriggers.removeAll(trigClearList);
		trigClearList.clear();
		
	}

	public void init() {
		//initial values are based on the first(startTime?) commands of those types.
		//even if that startTime didn't pass yet ............ -_-"
		//not sure if this includes loops/triggers 
		//includes loops/......
		
		//Optimization: find when opacity is 0 and add events to remove/add?
		events.addCommands(commands, activeCommands);
		events.ready();
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
		return "SBObject line:"+lineNumber+" "+x+" "+y+" "+sx+" "+sy+" "+opacity+" "+rotation+" "+color+" "+t;
	}

	public void addActiveTrigger(SBCommandTrigger sbCommandTrigger) {
		activeTriggers.add(sbCommandTrigger);
	}
	
}
class SBAnimObject extends SBObject {

	//based on initial start time
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
