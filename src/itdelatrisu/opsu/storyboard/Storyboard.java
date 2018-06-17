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
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.ui.Fonts;

public class Storyboard {

	/*
	https://osu.ppy.sh/forum/viewtopic.php?p=12468#p12468
	
	O ShortHands {Sequential Vals Repeats} {startVal/endVal}{startTime/EndTime} 
	O 	repeat commands shorthand[multiple same command listed as one command]
	O Params Command HVA
	O Loop
	? Stacked compound statement(Loops in loops?)
	
	
	Pass Fail layer
	Trigger
>>>	  triggers should finish normally if triggered even if it is pass its end time?
	
	O Animation
	animation starts on first active command
	better image memory management[unload/load images as necessary somehow?]
	
	.osu + .osb storyboard append
	Storyboard Variables?
	
	bg / video / storyboard[remove bg when first used at that time ?]
	wide screen storyboard
	no breaks bars
	osu seems to have a white tint?
	
	loop commands dont get reseted so may not display correctly while seeking
	trigger commands doesn't make object auto visible?
	trigger at trackPos instead of object time?
	initial values on loops and trigger...
	trigger command overrides other commands
	obj is not Active until a command is active, a triggerCommand is not active until it gets trigger
	
	
	objects are active from the lowest start time to the highest end time.
	initial values based on first time stamped command [better way?]
	objects keeps the value even after the command finishes
	command time collision: shorter command first, last command for commands with the exact same time
	the first commands that runs finishes before the next one can start
	S and V time collides with each other but uses different scales...?
	shorthand time S uses a different scale vs V
	
	probably implemented by a track for each type of command and only one of each type can be on.
	
	artifacting on 1px img[something something pow2 edge-bleed?]
	trigger doesnt have priority?
	animation start time?
	
	dimming
	cleanup/comment
	*/
	
	private static int containerHeight;
	private static float xMultiplier;
	private static float yMultiplier;
	private static int xOffset;
	private static int yOffset;
	//have a list of objects draw layers 
	//have a list of events (add/remove to draw list |  add/remove commands)
	//have a list of commands
	public static void init(int width, int height) {
		containerHeight = height;
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
	
	//HashSet<SBCommand> activeCommands = new HashSet<>();

	SBEventRunner events = new SBEventRunner();
	
	SBFadeModify fadeM = new SBFadeModify();
	SBMoveModify moveM = new SBMoveModify();
	SBMovexModify movexM = new SBMovexModify();
	SBMoveyModify moveyM = new SBMoveyModify();
	SBScaleModify scaleM = new SBScaleModify();
	SBSpecialScaleModify specialScaleM = new SBSpecialScaleModify();
	SBVecScaleModify vecScaleM = new SBVecScaleModify();
	SBRotateModify rotateM = new SBRotateModify();
	SBColorModify colorM = new SBColorModify();
	
	HashSet<TriggerListener> trigListenerSet = new HashSet<>();
	HashMap<TriggerEvent, HashSet<TriggerListener>> trigLis = new HashMap<>();
	HashSet<SBCommandTrigger> activeTriggers = new HashSet<>();
	public File file;
	String bgPath = null;
	private boolean usesBG = false;
	/** The renderer to use for all GL operations */
	protected static SGL GL = Renderer.get();
	int objCnt = 0;
	
	boolean isFailing = false;
	
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
		//objects are not visible as long as fade<=0 or sx/sy == 0 
		//or objects is out of bounds?
		//as long as not used in loop or trigger can remove objects?
		//if starts or ends with != 0 then visible
		class TEvent {
			int time;
			int state;
			int mod;
			final static int Fade = 1
			,ScaleX = 2
			,ScaleY = 3
			,ScaleA = 4;
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
					if (c2.mod == fadeM) {
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
							if (c4.mod == fadeM) {
								usesFade = true;
							}
						}
					}
					if (usesFade) {
						list.add(new TEvent(c2.startTime, TEvent.BeginMulti, TEvent.Fade));
						list.add(new TEvent(c2.endTime, TEvent.EndMulti, TEvent.Fade));
					}
				}
				//c.isSameInitalEndValue;
			}
			list.sort(new Comparator<TEvent>() {

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
					break;
				}
				boolean needsToUpdate = (fadeMultiCnt > 0 || fadeVis);
				if (fadeMultiCnt > 0)
					System.out.println("Fade MultiCnt > 0");
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
					System.out.println(e+" "+isUpdating+" "+o);
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
		System.out.println("events Size:"+events.events.size());
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
		
		String indentStr = "";
		for (int i=0; i<indent; i++)
			indentStr+=" ";
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
					command = fadeM;
					break;
				case "M":
					command = moveM;
					break;
				case "MX":
					command = movexM;
					break;
				case "MY":
					command = moveyM;
					break;
				case "S":
					command = scaleM;
					break;
				case "V":
					command = vecScaleM;
					break;
				case "R":
					command = rotateM;
					break;
				case "C":
					command = colorM;
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
					//loops in loops / loops in triggers / triggers in loops ???
					//System.err.println(" L command not implemented L:"+" "+in.getLineNumber()+" ST:"+tstartTime+" LOOP:"+loopCnt);
					ArrayList<SBCommand> commands2 = new ArrayList<SBCommand>();
					line = parseCommands(in, indent+1, o, commands2);
					//commands.addAll(unrollLoop(tstartTime, loopCnt, commands2));
					commands.add(new SBCommandLoop(o, tstartTime, loopCnt, commands2));
					//for (SBCommand c : commands2) {
					//	commands.add(c.clone());
					//}
					//o.addEvent(new SBCommandLoop(o, tstartTime, loopCnt, commands2, this));
				} continue; //don't read next line
				case "T": {
					//System.err.println(" T command not implemented");
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
				if (command == scaleM && startTime == endTime)
					command = specialScaleM;
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
	private List<SBCommand> unrollLoop(int tstartTime, int loopCnt, ArrayList<SBCommand> commands) {
		LinkedList<SBCommand> list = new LinkedList<>(); 
		int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;

		//find start
		//find end
		for(SBCommand c : commands) {
			start = Math.min(start, c.startTime);
			end = Math.max(end, c.endTime);
		}
		for (int i=0; i<loopCnt; i++) {
			for(SBCommand c : commands) {
				list.add(c.cloneOffset());
			}
		}
		return null;
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
		//System.out.println("Get: "+path);
		if (im == null) {
			try {
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
				if (!new File(parent, path).isFile()) {
					System.err.println("File Doesn't exist:"+path);
					im = new Image(32, 32);
				}else
					im = new Image(parent+"/"+path);//, false, 0);
				//im = im.getScaledCopy(yMultiplier);
				imageCache.put(path, im);
				System.out.println("Load: "+path);
			} catch (Exception e) {
				System.out.println("Error loading "+path);
				e.printStackTrace();
			}
		}
		return im;
	}

	public void render(Graphics g, float dimLevel) {
		int inactiveCnt = 0;
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
		//*
		//System.out.println("Active Objs cnt:"+background.size()+" "+foreground.size());
		String t = "objCnt: total:"+objs.size()+" f:"+foreground.size()+" b:"+background.size()+" inactive:"+inactiveCnt+" tp:"+trkPos;
		g.setColor(Color.black);
		g.drawString(t, 125, 50);
	
		g.setColor(Color.white);
		g.drawString(t, 124, 50);
		g.setColor(Color.black);
		//*/
		int y = 50+12;
		int dy = Fonts.SMALL.getLineHeight()*7/8;
		/*
		for(SBObject o :foreground) {
			if (o.opacity > 0)
				Fonts.SMALL.drawString(125, y += dy, o.toString());
			if (y > containerHeight)
				break;
		}
		y += dy;
		for(SBObject o :pass) {
			if (o.opacity > 0)
				Fonts.SMALL.drawString(125, y += dy, o.toString());
			if (y > containerHeight)
				break;
		}
		
		y += dy;
		for(SBObject o :fail) {
			if (o.opacity > 0)
				Fonts.SMALL.drawString(125, y += dy, o.toString());
			if (y > containerHeight)
				break;
		}
		y += dy;
		for(SBObject o :background) {
			if (o.opacity > 0)
				Fonts.SMALL.drawString(125, y += dy, o.toString());
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
		/*
		for(SBObject o : objs) {
			o.update(trackPosition);
		}
		/*/
		/*
		for(SBCommand c : activeCommands) {
			c.update(trackPosition);
		}
		//*/
		//System.out.println("Active Commands cnt:"+activeCommands.size());
	}
	public void reset() {
		background.clear();
		pass.clear();
		fail.clear();
		foreground.clear();
		isFailing = false;
		//activeCommands.clear();
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
				System.out.println("nobjs:"+cnt+" "+(hasL?"L":"")+(hasT?"T":""));
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
		System.out.println("trigger nowPassing "+trackPosition);
		for(TriggerListener l : trigLis.get(TriggerEvent.Passing)) {
			l.trigger(trackPosition);
		}
		isFailing = false;
	}
	public void nowFailing(int trackPosition) {
		System.out.println("trigger nowFailing "+trackPosition);
		for(TriggerListener l : trigLis.get(TriggerEvent.Failing)) {
			l.trigger(trackPosition);
		}
		isFailing = true;

	}
	public void nowHitSoundClap(int trackPosition) {
		//System.out.println("trigger nowHitSoundClap "+trackPosition);
		for(TriggerListener l : trigLis.get(TriggerEvent.HitSoundClap)) {
			l.trigger(trackPosition);
		}
	}
	public void nowHitSoundFinish(int trackPosition) {
		//System.out.println("trigger nowHitSoundFinish "+trackPosition);
		for(TriggerListener l : trigLis.get(TriggerEvent.HitSoundFinish)) {
			l.trigger(trackPosition);
		}
	}
	public void nowHitSoundWhistle(int trackPosition) {
		//System.out.println("trigger nowHitSoundWhistle "+trackPosition);
		for(TriggerListener l : trigLis.get(TriggerEvent.HitSoundWhistle)) {
			l.trigger(trackPosition);
		}
	}
	public void addActiveTrigger(SBCommandTrigger sbCommandTrigger) {
		activeTriggers.add(sbCommandTrigger);
		
	}
}
