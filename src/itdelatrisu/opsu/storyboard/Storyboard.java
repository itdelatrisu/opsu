package itdelatrisu.opsu.storyboard;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;

/**
 * Main Storyboard class based on
 * https://osu.ppy.sh/forum/viewtopic.php?p=12468#p12468
 * https://osu.ppy.sh/help/wiki/Storyboard_Scripting/Commands
 * 
 * Currently works by making a timeline of when to add/remove objects 
 * each object also make its own timeline for commands
 * 
 * probably buggy
 * known bugs:
 *  textures bleeding especially single pixel
 * Triggers / visibility
 *   only visible if gets triggered unless already visible....
 *   Things auto visible unless contains trigger??
 * @author fluddokt
 *
 */
public class Storyboard {

	

	private static float xMultiplier;
	private static float yMultiplier;
	private static int xOffset;
	private static int yOffset;
	public static void init(int width, int height) {
		int swidth = width;
		int sheight = height;
		if (swidth * 3 > sheight * 4)
			swidth = sheight * 4 / 3;
		else
			sheight = swidth * 3 / 4;
		xMultiplier = swidth / 640f;
		yMultiplier = sheight / 480f;
		xOffset = (int) (width - swidth) / 2;
		yOffset = (int) (height - sheight) / 2;
	}
	
	ArrayList<SBObject> objs = new ArrayList<>();
	
	TreeSet<SBObject> background = new TreeSet<>();
	TreeSet<SBObject> foreground = new TreeSet<>();
	TreeSet<SBObject> fail = new TreeSet<>();
	TreeSet<SBObject> pass = new TreeSet<>();

	SBEventRunner events = new SBEventRunner();
	
	HashSet<TriggerListener> trigListenerSet = new HashSet<>();
	HashMap<TriggerEvent, HashSet<TriggerListener>> trigLis = new HashMap<>();
	HashSet<SBCommandTrigger> activeTriggers = new HashSet<>();
	public File file;
	String bgPath = null;
	private boolean usesBG = false;
	/** The renderer to use for all GL operations */
	protected static SGL GL = Renderer.get();
	int objCnt = 0;
	int inactiveCnt;
	boolean isFailing = false;
	boolean additiveBlending = false;
	
	public Storyboard(File file){
		this.file = file;
		
		for (TriggerEvent trigEv : TriggerEvent.values()){
			trigLis.put(trigEv, new HashSet<TriggerListener>());
		}
		
	}
	public void load(File file, String bgPath2) throws IOException  {
		try (
			InputStream bis = new BufferedInputStream(new FileInputStream(file));
			LineNumberReader in = new LineNumberReader(new InputStreamReader(bis, "UTF-8"));
		) {
			try {
				String line = in.readLine();
				String tokens[] = null;
				while (line != null) {
					line = line.trim();
					if (!isValidLine(line)) {
						line = in.readLine();
						continue;
					}
					switch (line) {
						case "[Events]":
							SBObject lastObject = null;//new SBObject();
							line = in.readLine();
							while (line != null) {
								line = line.trim();
								if (!isValidLine(line)) { //actually stops accepting new commands to the current object 
									line = in.readLine();
									continue;
								}
								if (line.charAt(0) == '[')
									break;
								tokens = line.split(",");
								switch (tokens[0]) {
									case "0":  // background image
										bgPath = tokens[2].replaceAll("^\"|\"$", "");
										
									case "1":
									case "Video":  // background video
									case "2":  // break periods
										line = in.readLine();
										break;
									default:
										System.err.println("SB: unknown token:"+tokens[0]+" at line:"+in.getLineNumber()+" "+file);
										line = in.readLine();
										break;
									case "Sprite":{
										Image im = null;
										String align = tokens[2];
										String path = tokens[3].replaceAll("^\"|\"$", "");
										im = getImage(file.getParentFile(), path);
										float x = Float.parseFloat(tokens[4]);
										float y = Float.parseFloat(tokens[5]);
										String layer = tokens[1];
										//System.out.println(bgPath +" "+path);
										if ("Background".equals(layer) && bgPath!=null && bgPath.equalsIgnoreCase(path)) {
											usesBG  = true;
										}
										TreeSet<SBObject> layerTS = getLayer(layer);
										lastObject = new SBObject(in.getLineNumber(),objCnt++, layerTS, align, im, scaleX(x), scaleY(y));
										objs.add(lastObject);
										line = parseCommands(in, 0, lastObject, lastObject.commands);
									}break;
									case "Animation":{
										
										String align = tokens[2];
										float x = Float.parseFloat(tokens[4]);
										float y = Float.parseFloat(tokens[5]);
										int frameCount = Integer.parseInt(tokens[6]);
										int frameDelay= Integer.parseInt(tokens[7]);
										String loopType = tokens.length>8 ? tokens[8].trim() : "";
										
										Image[] im = new Image[frameCount];
										String path = tokens[3].replaceAll("^\"|\"$", "");
										
										String endPath = getExt(path);
										String startPath = path.substring(0, path.length()-endPath.length());
										for(int i=0; i<frameCount; i++) {
											im[i] = getImage(file.getParentFile(), startPath+i+endPath);
										}
										String layer = tokens[1];
										TreeSet<SBObject> layerTS = getLayer(layer);
										lastObject = new SBAnimObject(in.getLineNumber(),objCnt++, layerTS, align, im, scaleX(x), scaleY(y), frameCount, frameDelay, loopType);
										objs.add(lastObject);
										line = parseCommands(in, 0, lastObject, lastObject.commands);
									}break;
								}
							}
							break;
						default:
							line = in.readLine();
							break;
					}
					
				}
			} catch (Exception e) {
				System.err.println("Error loading Storyboard '"+file+"' on line "+in.getLineNumber());
				e.printStackTrace();
				throw e;
			}
		}
	}
	
	enum Mod {Fade, ScaleX, scaleY, scaleA}
	public void ready() {
		for(SBObject o : objs) {
			o.init();
		}
		
		//Creates attach and remove events for objects when visibility changes
		
		//objects are not visible as long as fade<=0 or sx/sy == 0 
		//or objects is out of bounds?
		//as long as not used in loop or trigger can remove objects?
		//if starts or ends with != 0 then visible
		//currently only does fade and is pretty buggy.
		class TEvent {
			int time;
			int state;
			int mod;
			final static int Fade = 1
			/*,ScaleX = 2
			,ScaleY = 3
			,ScaleA = 4*/
			;
			final static int 
					Update = 1
					,NoUpdate = 2
					,BeginMulti = 3
					,EndMulti = 4;
			public TEvent(int time, int state, int mod) {
				super();
				this.time = time;
				this.state = state;
				this.mod = mod;
			}
			@Override
			public String toString() {
				return "TEvent "+time+" "+state+" "+mod;
			}
			
		}
		for(SBObject o : objs) {
			//*
			List<TEvent> list = new ArrayList<>();
			for(SBCommand c : o.commands) {
				if (c instanceof SBCommandMod) {
					SBCommandMod c2 = (SBCommandMod)c;
					if (c2.mod == SBModify.SBFadeModify) {
						//s0 e0 - no update s
						//s0 e1 - update s
						//s1 e0 - update s noupdate e
						//s1 e1 - update s
						if (c2.endVals[0] == 0) {
							if (c2.startVals[0] != 0) {
								list.add(new TEvent(c2.startTime, TEvent.Update, TEvent.Fade));
								list.add(new TEvent(c2.endTime, TEvent.NoUpdate, TEvent.Fade));
							} else {
								list.add(new TEvent(c2.startTime, TEvent.NoUpdate, TEvent.Fade));
							}
						} else {
							list.add(new TEvent(c2.startTime, TEvent.Update, TEvent.Fade));
						}
					}
						
				}
				if (c instanceof SBCommandMult) {
					SBCommandMult c2 = (SBCommandMult)c;
					boolean usesFade = false;
					for (SBCommand c3: c2.commands) {
						if(c3 instanceof SBCommandMod) {
							SBCommandMod c4 = (SBCommandMod)c3;
							if (c4.mod == SBModify.SBFadeModify) {
								usesFade = true;
							}
						}
					}
					if (usesFade) {
						list.add(new TEvent(c2.startTime, TEvent.BeginMulti, TEvent.Fade));
						list.add(new TEvent(c2.endTime, TEvent.EndMulti, TEvent.Fade));
					}
				}
			}
			Collections.sort(list, new Comparator<TEvent>() {

				@Override
				public int compare(TEvent o1, TEvent o2) {
					return o1.time - o2.time;
				}
			});
			boolean fadeVis = false;
			int fadeMultiCnt = 0;
			
			boolean isUpdating = o.iOpacity != 0;
			boolean addAtStart = isUpdating;
			boolean removeAtEnd = true;
			for (TEvent e : list) {
				
				switch (e.state) {
				case TEvent.NoUpdate:
					fadeVis = false;
					break;
				case TEvent.Update:
					fadeVis = true;
					break;
				case TEvent.BeginMulti:
					fadeMultiCnt++;
					break;
				case TEvent.EndMulti:
					fadeMultiCnt--;
					fadeVis = true;
					break;
				}
				boolean needsToUpdate = (fadeMultiCnt > 0 || fadeVis);
				if (isUpdating != needsToUpdate) {
					isUpdating = !isUpdating;
					if(isUpdating) {
						events.add(new AttachObjectSBEvent(e.time, this, o));
						removeAtEnd = true;
					}
					else {
						if(e.time == o.start)
							addAtStart = false;
						//else if (e.time == o.end)
						//	;
						else
							events.add(new RemoveObjectSBEvent(e.time, this, o));
						removeAtEnd = false;
						
					}
					}
			}
			if (addAtStart)
				events.add(new AttachObjectSBEvent(o.start, this, o));
			if (removeAtEnd)
				events.add(new RemoveObjectSBEvent(o.end, this, o));
			/*/
				events.add(new AttachObjectSBEvent(o.start, this, o));
				events.add(new RemoveObjectSBEvent(o.end, this, o));
			//*/
		}
		events.ready();
		events.setListener(new SBEventRunnerListener() {
			@Override
			public void reseted() {
				reset();
			}
		});
	}
	private String getExt(String fileName) {
		String extension = "";

		int i = fileName.lastIndexOf('.');
		int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

		if (i > p) {
			extension = fileName.substring(i);
		}
		return extension;
	}

	public String parseCommands(LineNumberReader in, int indent, SBObject o, ArrayList<SBCommand> commands) throws NumberFormatException, IOException {
	
		//System.out.println("ParseCommand:"+indent);
		String line;
		String[] tokens;
		
		line = in.readLine();
		while (line != null) {
			if (!isValidLine(line))
				return line;
			if (line.length() <= indent + 1 || line.charAt(indent) != ' ' || line.charAt(indent+1) == ' ')
				return line;
			SBModify command = null;
			line = line.trim();
			tokens = line.split(",");
			switch (tokens[0]) {
				default:
					System.err.println("parseCommands] unknown token:"+tokens[0]+" at line:"+in.getLineNumber());
					return line;
					
				case "0":  // background image
				case "1":
				case "Video":  // background video
				case "2":  // break periods
				case "Sprite":
					return line;
					
					
				case "F":
					command = SBModify.SBFadeModify;
					break;
				case "M":
					command = SBModify.SBMoveModify;
					break;
				case "MX":
					command = SBModify.SBMovexModify;
					break;
				case "MY":
					command = SBModify.SBMoveyModify;
					break;
				case "S":
					command = SBModify.SBScaleModify;
					break;
				case "V":
					command = SBModify.SBVecScaleModify;
					break;
				case "R":
					command = SBModify.SBRotateModify;
					break;
				case "C":
					command = SBModify.SBColorModify;
					break;
				case "P": {
					int tstartTime = Integer.parseInt(tokens[2]);
					int tendTime = tokens[3].length()>0?Integer.parseInt(tokens[3]):tstartTime;
					switch(tokens[4])  {
						case "H": commands.add(new SBCommandEventFlipH(o, tstartTime, tendTime)); break;
						case "V": commands.add(new SBCommandEventFlipV(o, tstartTime, tendTime)); break;
						case "A": commands.add(new SBCommandEventAddBlend(o, tstartTime, tendTime)); break;
						default: System.err.println(" P command not implemented: "+tokens[4]);
					}
				} break;
				case "L": {
					int tstartTime = Integer.parseInt(tokens[1]);
					int loopCnt = Integer.parseInt(tokens[2]);
					ArrayList<SBCommand> commands2 = new ArrayList<SBCommand>();
					line = parseCommands(in, indent+1, o, commands2);
					commands.add(new SBCommandLoop(o, tstartTime, loopCnt, commands2));
				} continue; //don't read next line
				case "T": {
					String triggerName = tokens[1];
					int tstartTime = Integer.parseInt(tokens[2]);
					int tendTime = Integer.parseInt(tokens[3]);
					
					ArrayList<SBCommand> commands2 = new ArrayList<SBCommand>();
					line = parseCommands(in, indent+1, o, commands2);
					commands.add(new SBCommandTrigger(o, triggerName, tstartTime, tendTime, commands2, this));
				} continue; //don't read next line
			}
			if (command != null) {
				int easing = Integer.parseInt(tokens[1]);
				int startTime = Integer.parseInt(tokens[2]);
				int endTime = tokens[3].length()>0?Integer.parseInt(tokens[3]):startTime;
				int duration = endTime - startTime;
				if (command == SBModify.SBScaleModify && startTime == endTime)
					command = SBModify.SBSpecialScaleModify;
				int nvars = command.vars();
				int repeats = (tokens.length - 4 - nvars)/(nvars);
				int tokensAt = 4;
				if (repeats > 1)
					System.out.println("Command with repeats:"+command+" "+repeats);
				if (repeats == 0)
					repeats = 1; //shorthand startVal == endVal
				float[] startVals = new float[nvars];
				for(int j=0; j<nvars; j++){
					startVals[j] = Float.parseFloat(tokens[tokensAt++]);
				}
				command.init(startVals);
				for (int i=0; i< repeats; i++) {
					float[] endVals;
					
					if (tokensAt + nvars <= tokens.length) {
						endVals = new float[nvars];
						for(int j=0; j<nvars; j++){
							endVals[j] = Float.parseFloat(tokens[tokensAt++]);
					//		System.out.println("E:"+endVals[j]);
						}
						command.init(endVals);
					} else {
						endVals = startVals;
					}
					
				
					commands.add(
							new SBCommandMod(o,
							command, easing, 
							startTime + duration * i, 
							endTime + duration * i, 
							startVals, endVals)
					);
					startVals = endVals;
				}
			}
			line = in.readLine();
		}
		return line;
		
	}
	
	/**
	 * Returns false if the line is too short or commented.
	 */
	private static boolean isValidLine(String line) {
		return (line.length() > 1 && !line.startsWith("//"));
	}
	
	HashMap<String, Image> imageCache = new HashMap<>();
	public Image getImage(File parent, String path) {
		Image im = imageCache.get(path);
		if (im == null) {
			try {
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
				if (!new File(parent, path).isFile()) {
					System.err.println("File Doesn't exist:"+path);
					im = new Image(32, 32);
				}else
					im = new Image(parent+"/"+path);//, false, 0);
				imageCache.put(path, im);
			} catch (Exception e) {
				System.out.println("Error loading "+path);
				e.printStackTrace();
			}
		}
		return im;
	}

	public void render(Graphics g, float dimLevel) {
		additiveBlending = false;
		inactiveCnt = 0;
		for(SBObject o : background) {
			if (!o.render(g, dimLevel))
				inactiveCnt ++;
		}
		for(SBObject o : !isFailing ? pass : fail) {
			if (!o.render(g, dimLevel))
				inactiveCnt ++;
		}
		
		for(SBObject o : foreground) {
			if (!o.render(g, dimLevel))
				inactiveCnt ++;
		}
		g.setDrawMode(Graphics.MODE_NORMAL);
		/*
		//System.out.println("Active Objs cnt:"+background.size()+" "+foreground.size());
		String t = "objCnt: total:"+objs.size()+" f:"+foreground.size()+" b:"+background.size()+" inactive:"+inactiveCnt+" tp:"+trkPos;
		g.setColor(Color.black);
		g.drawString(t, 125, 50);
	
		g.setColor(Color.white);
		g.drawString(t, 124, 50);
		g.setColor(Color.black);
		
		int y = 50+12;
		int dy = itdelatrisu.opsu.ui.Fonts.SMALL.getLineHeight()*7/8;
		int containerHeight = 600;
		
		for(SBObject o :foreground) {
			if (o.opacity > 0)
				itdelatrisu.opsu.ui.Fonts.SMALL.drawString(125, y += dy, o.toString());
			if (y > containerHeight )
				break;
		}
		y += dy;
		for(SBObject o :pass) {
			if (o.opacity > 0)
				itdelatrisu.opsu.ui.Fonts.SMALL.drawString(125, y += dy, o.toString());
			if (y > containerHeight)
				break;
		}
		
		y += dy;
		for(SBObject o :fail) {
			if (o.opacity > 0)
				itdelatrisu.opsu.ui.Fonts.SMALL.drawString(125, y += dy, o.toString());
			if (y > containerHeight)
				break;
		}
		y += dy;
		for(SBObject o :background) {
			if (o.opacity > 0)
				itdelatrisu.opsu.ui.Fonts.SMALL.drawString(125, y += dy, o.toString());
			if (y > containerHeight)
				break;
		}
		//*/
		//*/
		/*for(SBCommand o :activeCommands) {
			//g.drawString(o.toString(),125, y += 12);
			if (y > containerHeight)
				break;
		}*/
	}
	int trkPos;
	public void update(int trackPosition) {
		trkPos = trackPosition;
		events.update(trackPosition);
		for(SBObject o: background) {
			o.update(trackPosition);
		}
		for(SBObject o: pass) {
			o.update(trackPosition);
		}
		for(SBObject o: fail) {
			o.update(trackPosition);
		}
		for(SBObject o: foreground) {
			o.update(trackPosition);
		}
	}
	public void reset() {
		background.clear();
		pass.clear();
		fail.clear();
		foreground.clear();
		isFailing = false;
		for (SBObject o : objs) {
			o.reset();
		}
		
	}
	public static float scaleX(float x) {
		return x * xMultiplier + xOffset;
	}
	public static float scaleY(float y) {
		return y * yMultiplier + yOffset;
	}
	/**
	 * Returns the X multiplier for coordinates.
	 */
	public static float getXMultiplier() { return xMultiplier; }

	/**
	 * Returns the Y multiplier for coordinates.
	 */
	public static float getYMultiplier() { return yMultiplier; }
	public TreeSet<SBObject> getLayer(String layer) {
		switch(layer) {
			case "Background":
				return background;
			case "Foreground":
				return foreground;
			case "Pass":
				return pass;
			case "Fail":
				return fail;
			default:
				throw new RuntimeException("Could not find layer: "+layer);
		}
	}
	public boolean exists() {
		return objs.size()>0;
	}

	public boolean usesBG() {
		return usesBG;
	}

	public static boolean storyboardExist(File sbFile) throws IOException {
		boolean hasL = false;
		boolean hasT = false;
		try (
			InputStream bis = new BufferedInputStream(new FileInputStream(sbFile));
			LineNumberReader in = new LineNumberReader(new InputStreamReader(bis, "UTF-8"));
		) {
			int cnt = 0;
			String line = in.readLine();
			String tokens[] = null;
			while (line != null) {
				line = line.trim();
				if (!isValidLine(line)) {
					line = in.readLine();
					continue;
				}
				switch (line) {
					case "[Events]":
						while ((line = in.readLine()) != null) {
							line = line.trim();
							if (!isValidLine(line))
								continue;
							if (line.charAt(0) == '[')
								break;
							if (line.startsWith("  "))
								continue;
							tokens = line.split(",");
							switch (tokens[0]) {
								case "0":  // background image
								case "1":
								case "Video":  // background video
								case "2":  // break periods
									break;
								default:
									break;
								case "Sprite":
								case "Animation":
									cnt++;
									//return true;
									String layer = tokens[1];
									if (!"Foreground".equals(layer) &&  !"Background".equals(layer))
										System.out.println(layer);
									break;
								case "L":
									hasL = true;
									break;
								case "T":
									hasT = true;
									System.out.println(in.getLineNumber()+" "+line);
									break;
							}
							
						}
					default:
						line = in.readLine();
						break;
				}
			}
			if(cnt>0){
				System.out.println("nobjs:"+cnt+" "+(hasL?"L":"")+(hasT?"T":""));//TODO delete me
				return true;
			}
		}
		return false;
	}
	public void dispose() throws SlickException {
		for(Image im : imageCache.values()) {
			im.destroy();
		}
		imageCache.clear();
	}
	public void addTriggerListener(TriggerListener trigger, TriggerEvent triggerName) {
		trigListenerSet.add(trigger);
		trigLis.get(triggerName).add(trigger);
	}
	public void removeTriggerListener(TriggerListener trigger, TriggerEvent triggerName) {
		trigListenerSet.remove(trigger);
		trigLis.get(triggerName).remove(trigger);
	}
	
	public void nowPassing(int trackPosition) {
		for(TriggerListener l : trigLis.get(TriggerEvent.Passing)) {
			l.trigger(trackPosition);
		}
		isFailing = false;
	}
	public void nowFailing(int trackPosition) {
		for(TriggerListener l : trigLis.get(TriggerEvent.Failing)) {
			l.trigger(trackPosition);
		}
		isFailing = true;
	}
	public void nowHitSoundClap(int trackPosition) {
		for(TriggerListener l : trigLis.get(TriggerEvent.HitSoundClap)) {
			l.trigger(trackPosition);
		}
	}
	public void nowHitSoundFinish(int trackPosition) {
		for(TriggerListener l : trigLis.get(TriggerEvent.HitSoundFinish)) {
			l.trigger(trackPosition);
		}
	}
	public void nowHitSoundWhistle(int trackPosition) {
		for(TriggerListener l : trigLis.get(TriggerEvent.HitSoundWhistle)) {
			l.trigger(trackPosition);
		}
	}
	public void addActiveTrigger(SBCommandTrigger sbCommandTrigger) {
		activeTriggers.add(sbCommandTrigger);
	}
}
