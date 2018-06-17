package itdelatrisu.opsu.storyboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
public abstract class SBCommand{
	SBObject obj;
	int easing;
	int startTime;
	int endTime;
	
	float duration;
	
	boolean isVarying = false;
	public boolean isSameInitalEndValue;
	public SBCommand(SBObject obj, int startTime, int endTime) {
		this.obj = obj;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	public void update(int trackPosition) {
	}
	public void start() {
	}
	public void end() {
	}
	public SBCommand cloneOffset() {
		return null;
	}
	public abstract void setIntialVal(int startTime);
	
}
class SBCommandMod extends SBCommand {
	
	SBObject obj;
	int easing;
	
	float duration;
	
	float[] startVals;
	float[] endVals;
	float[] tempVals;
	
	SBModify mod;
	
	public SBCommandMod(SBObject obj, SBModify command, int easing, int startTime, int endTime, float[] startVals, float[] endVals) {
		super(obj, startTime, endTime);
		this.obj = obj;
		this.mod = command;
		this.easing = easing;
		this.startVals = startVals;
		this.endVals = endVals;
		tempVals = new float[startVals.length];
		duration = endTime - startTime;
		isVarying = true;
		if (easing > 2) {
			System.err.println("Unknown Easing :"+easing+" for:"+obj.lineNumber);
		}
	}

	@Override
	final public void update(int trackPosition) {
		//if (trackPosition < startTime || trackPosition > endTime)
		//	return;
		if (duration>0)
			updateT(Utils.clamp((trackPosition - startTime) / duration ,0,1));
	}
	@Override
	public void start() {
		obj.isActive = true;
		updateT(0);
	}

	@Override
	public void end() {
		updateT(1);
	}

	//@Override
	public void updateT(float t) {
		for (int i=0; i<tempVals.length; i++) {
			tempVals[i] = easing(t, startVals[i], endVals[i]);
		}
		mod.set(obj, tempVals);
	}
	public void setIntialVal(int startTime) {
		mod.setI(obj, startVals, startTime);
	}
	final public float easing(float t, float start, float end) {
		
		switch(easing) 
		{
			case 1: // ease out
				//t = 1 - (1-t)*(1-t);
				//t = 1 - 1 + 2t - t*t
				//t = 2*t - t*t;
				//t = (2-t)*t;
				t = AnimationEquation.OUT_QUAD.calc(t);
				break;
				//return (1-t)*start + t*end;
			case 2: // ease in
				//t = t*t;
				t = AnimationEquation.IN_QUAD.calc(t);
				//return (1-t)*start + t*end;
				break;
				
			case 3: t = AnimationEquation.IN_QUAD.calc(t); break;
			case 4: t = AnimationEquation.OUT_QUAD.calc(t); break;
			case 5: t = AnimationEquation.IN_OUT_QUAD.calc(t); break;
			case 6: t = AnimationEquation.IN_CUBIC.calc(t); break;
			case 7: t = AnimationEquation.OUT_CUBIC.calc(t); break;
			case 8: t = AnimationEquation.IN_OUT_CUBIC.calc(t); break;
			case 9: t = AnimationEquation.IN_QUART.calc(t); break;
			case 10: t = AnimationEquation.OUT_QUART.calc(t); break;
			case 11: t = AnimationEquation.IN_OUT_QUART.calc(t); break;
			case 12: t = AnimationEquation.IN_QUINT.calc(t); break;
			case 13: t = AnimationEquation.OUT_QUINT.calc(t); break;
			case 14: t = AnimationEquation.IN_OUT_QUINT.calc(t); break;
			
			case 15: t = AnimationEquation.IN_SINE.calc(t); break;
			case 16: t = AnimationEquation.OUT_SINE.calc(t); break;
			case 17: t = AnimationEquation.IN_OUT_SINE.calc(t); break;
			case 18: t = AnimationEquation.IN_EXPO.calc(t); break;
			case 19: t = AnimationEquation.OUT_EXPO.calc(t); break;
			case 20: t = AnimationEquation.IN_OUT_EXPO.calc(t); break;
			case 21: t = AnimationEquation.IN_CIRC.calc(t); break;
			case 22: t = AnimationEquation.OUT_CIRC.calc(t); break;
			case 23: t = AnimationEquation.IN_OUT_CIRC.calc(t); break;
			
			case 24: t = AnimationEquation.IN_ELASTIC.calc(t); break;
			case 25: t = AnimationEquation.OUT_ELASTIC.calc(t); break;
			case 26: t = AnimationEquation.OUT_ELASTIC_HALF.calc(t); break;
			case 27: t = AnimationEquation.OUT_ELASTIC_QUARTER.calc(t); break;
			case 28: t = AnimationEquation.IN_OUT_ELASTIC.calc(t); break;
			
			case 29: t = AnimationEquation.IN_BACK.calc(t); break;
			case 30: t = AnimationEquation.OUT_BACK.calc(t); break;
			case 31: t = AnimationEquation.IN_OUT_BACK.calc(t); break;
			case 32: t = AnimationEquation.IN_BOUNCE.calc(t); break;
			case 33: t = AnimationEquation.OUT_BOUNCE.calc(t); break;
			case 34: t = AnimationEquation.IN_OUT_BOUNCE.calc(t); break;
			
			
			case 0:
			default:
				//t = t;
				//t = AnimationEquation.LINEAR.calc(t);
		}
		return (1-t)*start + t*end;
		
	}

	@Override
	public String toString() {
		return obj.lineNumber+" "+mod;
	}
	
}

abstract class SBModify {
	public abstract int vars();
	public abstract void set(SBObject o, float[] vals);
	public abstract void setI(SBObject o, float[] vals, int startTime);
	public abstract void get(SBObject o, float[] vals);
	public void init(float[] vals){}
}
class SBFadeModify extends SBModify {
	@Override
	public int vars() {
		return 1;
	}
	@Override
	public void set(SBObject o, float[] vals) {
		o.opacity = vals[0];
	}
	@Override
	public void setI(SBObject o, float[] vals, int startTime) {
		if(startTime < o.iOpacityTime) {
			o.iOpacityTime = startTime;
			o.iOpacity = vals[0];
		}
	}
	@Override
	public void get(SBObject o, float[] vals) {
		vals[0] = o.opacity;
	}
	public String toString(){return "F";}
}
class SBMoveModify extends SBModify {
	@Override
	public int vars() {
		return 2;
	}
	@Override
	public void set(SBObject o, float[] vals) {
		o.x = vals[0];
		o.y = vals[1];
	}
	@Override
	public void setI(SBObject o, float[] vals, int startTime) {
		if(startTime < o.ixTime || startTime < o.iyTime) {
			o.ixTime = startTime;
			o.iyTime = startTime;
			o.ix = vals[0];
			o.iy = vals[1];
		}
	}
	@Override
	public void get(SBObject o, float[] vals) {
		vals[0] = o.x;
		vals[1] = o.y;
	}
	@Override
	public void init(float[] vals) {
		vals[0] = Storyboard.scaleX(vals[0]);
		vals[1] = Storyboard.scaleY(vals[1]);
	}

}
class SBMovexModify extends SBModify {
	@Override
	public int vars() {
		return 1;
	}
	@Override
	public void set(SBObject o, float[] vals) {
		o.x = vals[0];
	}
	@Override
	public void setI(SBObject o, float[] vals, int startTime) {
		if(startTime < o.ixTime) {
			o.ixTime = startTime;
			o.ix = vals[0];
		}
	}
	@Override
	public void get(SBObject o, float[] vals) {
		vals[0] = o.x;
	}
	@Override
	public void init(float[] vals) {
		vals[0] = Storyboard.scaleX(vals[0]);
	}
}
class SBMoveyModify extends SBModify {
	@Override
	public int vars() {
		return 1;
	}
	@Override
	public void set(SBObject o, float[] vals) {
		o.y = vals[0];
	}
	@Override
	public void setI(SBObject o, float[] vals, int startTime) {
		if(startTime < o.iyTime) {
			o.iyTime = startTime;
			o.iy = vals[0];
		}
	}
	@Override
	public void get(SBObject o, float[] vals) {
		vals[0] = o.y;
	}
	@Override
	public void init(float[] vals) {
		vals[0] = Storyboard.scaleY(vals[0]);
	}	
}
class SBScaleModify extends SBModify {
	@Override
	public int vars() {
		return 1;
	}
	@Override
	public void set(SBObject o, float[] vals) {
		o.sx = o.sy = vals[0];
		o.scaleAll = 1;
	}
	@Override
	public void setI(SBObject o, float[] vals, int startTime) {
		if(startTime < o.iscaleTime || startTime < o.iscaleSPTime) {
			o.iscaleTime = o.iscaleSPTime = startTime;
			o.isx = o.isy = vals[0];
			o.iscaleAll = 1;
		}
	}
	@Override
	public void get(SBObject o, float[] vals) {
		vals[0] = o.sx;
	}
}
class SBSpecialScaleModify extends SBModify {
	@Override
	public int vars() {
		return 1;
	}
	@Override
	public void set(SBObject o, float[] vals) {
		o.sx = o.sy = 1;
		o.scaleAll = vals[0];
	}
	@Override
	public void setI(SBObject o, float[] vals, int startTime) {
		if(startTime < o.iscaleTime || startTime < o.iscaleSPTime) {
			o.iscaleTime = o.iscaleSPTime = startTime;
			o.isx = o.isy = 1;
			o.iscaleAll = vals[0];
		}
	}
	@Override
	public void get(SBObject o, float[] vals) {
		vals[0] = o.sx;
	}
}
class SBVecScaleModify extends SBModify {
	@Override
	public int vars() {
		return 2;
	}
	@Override
	public void set(SBObject o, float[] vals) {
		o.sx = vals[0];
		o.sy = vals[1];
	}
	@Override
	public void setI(SBObject o, float[] vals, int startTime) {
		if(startTime < o.iscaleTime) {
			o.iscaleTime = startTime;
			o.isx = vals[0];
			o.isy = vals[1];
		}
	}
	@Override
	public void get(SBObject o, float[] vals) {
		vals[0] = o.sx;
		vals[1] = o.sy;
	}
}
class SBRotateModify extends SBModify {
	@Override
	public int vars() {
		return 1;
	}
	@Override
	public void set(SBObject o, float[] vals) {
		o.rotation = vals[0];
	}
	@Override
	public void setI(SBObject o, float[] vals, int startTime) {
		if(startTime < o.iRotationTime) {
			o.iRotationTime = startTime;
			o.iRotation = vals[0];
		}
	}
	@Override
	public void get(SBObject o, float[] vals) {
		vals[0] = o.rotation;
	}
}
class SBColorModify extends SBModify {
	@Override
	public int vars() {
		return 3;
	}
	@Override
	public void set(SBObject o, float[] vals) {
		o.color.r = vals[0];
		o.color.g = vals[1];
		o.color.b = vals[2];
	}
	@Override
	public void setI(SBObject o, float[] vals, int startTime) {
		if(startTime < o.iColorTime) {
			o.iColorTime = startTime;
			o.icolor.r = vals[0];
			o.icolor.g = vals[1];
			o.icolor.b = vals[2];
		}
		
	}
	@Override
	public void get(SBObject o, float[] vals) {
		vals[0] = o.color.r;
		vals[1] = o.color.g;
		vals[2] = o.color.b;
	}
	@Override
	public void init(float[] vals) {
		vals[0]/=255;
		vals[1]/=255;
		vals[2]/=255;
	}
}

abstract class SBCommandMult extends SBCommand{
	ArrayList<SBCommand> commands;
	//ArrayList<SBEvent> events = new ArrayList<>();
	HashSet<SBCommand> activeCommands = new HashSet<>();
	//int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;
	int eventsDuration;
	SBComEventRunner events = new SBComEventRunner();
	public SBCommandMult(SBObject obj, int startTime, int endTime, ArrayList<SBCommand> commands) {
		super(obj, startTime, endTime);
		this.commands = commands;
		
		/*for(SBCommand c : commands) {
			start = Math.min(start, c.startTime);
			end = Math.max(end, c.endTime);
			if(c.startTime == c.endTime) {
				events.add(new EndCommandSBEvent(c.startTime, c));
			}
			events.add(new AttachCommandSBEvent(c.startTime, activeCommands, c));
			events.add(new RemoveCommandSBEvent(c.endTime, activeCommands, c));
		}*/
		events.addCommands(commands, activeCommands);
		eventsDuration = events.end - events.start;
		//events.start = 0;
		events.ready();
		/*events.sort(new Comparator<SBEvent>() {
			@Override
			public int compare(SBEvent o1, SBEvent o2) {
				return Integer.compare(o1.getTime(), o2.getTime());
			}
		});*/
	}
	@Override
	public void start() {
		super.start();
		events.reset();
		activeCommands.clear();
	}

}
class SBCommandLoop extends SBCommandMult implements SBEventRunnerListener{
	int currentloop = -1;
	int maxLoop;
	public SBCommandLoop(SBObject obj, int startTime, int loopCnt, ArrayList<SBCommand> commands) {
		super(obj, startTime, 0, commands);
		this.isVarying = true;
		this.startTime += events.start;
		this.endTime = startTime + eventsDuration * loopCnt + events.start;
		maxLoop = loopCnt;
		events.setListener(this);
	}
	
	@Override
	public void reseted() {
		currentloop = -1;
		activeCommands.clear();
	}
	
	@Override
	final public void update(int trackPosition) {
		//if (duration>0)
		//	updateT(Utils.clamp((trackPosition - startTime) / duration ,0,1));
		
		//for (SBCommand c : commands) {
		//	if c.startTime
		//	c.update(trackPosition - startTime);
		//}
		//construct event table
		trackPosition -= startTime;
		
		//if (trackPosition > eventsDuration + events.start) {
		//	trackPosition -= events.start;
			int loopAt = trackPosition / eventsDuration;
			if (currentloop != loopAt) {
				events.reset();
				currentloop = loopAt;
				if (loopAt >= maxLoop) {
					System.err.println("MAXED LOOP:"+loopAt+" "+maxLoop);
				}
			}
			trackPosition %= eventsDuration;
			trackPosition += events.start;
		//}
		//System.out.println("LOOP:"+obj.lineNumber+" "+events.eventsIndex+" tp:"+trackPosition+" l:"+currentloop+"/"+maxLoop+" "+events.start+" "+startTime+" "+endTime+" "+eventsDuration+" "+activeCommands.size()+" "+events.events.size());
		
		events.update(trackPosition);
		for(SBCommand c : activeCommands) {
			c.update(trackPosition);
		}
	}

	@Override
	public void start() {
		events.update(0);
		super.start();
	}

	@Override
	public void end() {
		events.update(events.end);
		super.end();
	}

	@Override
	public void setIntialVal(int startTime) {
		for (SBCommand c : commands) {
			c.setIntialVal(startTime + this.startTime);
		}
	}
}

class SBCommandTrigger extends SBCommandMult implements SBEventRunnerListener, TriggerListener{
	boolean isRunning;
	int trackStartPos;
	Storyboard sb;
	TriggerEvent triggerName;
	public SBCommandTrigger(SBObject obj, String triggerName, int startTime, int endTime, ArrayList<SBCommand> commands, Storyboard sb) {
		super(obj, startTime, endTime, commands);
		this.isVarying = false;
		this.sb = sb;
		events.setListener(this);
		this.triggerName = TriggerEvent.toTriggerEvent(triggerName);
	}
	
	@Override
	public void reseted() {
		activeCommands.clear();
	}
	
	@Override
	final public void update(int trackPosition) {
		trackPosition -= trackStartPos;
		
		if (!isRunning || trackPosition < 0)
			return;
		
		if (trackPosition > events.end) {
			isRunning = false;
			trackPosition = events.end;
		}
		//if (!isRunning || trackPosition < 0 || trackPosition > events.end)
		//	return;
		
		//System.out.println("update "+triggerName+" "+trackPosition+" "+this+" "+obj+" ");
		events.update(trackPosition);
		for(SBCommand c : activeCommands) {
			c.update(trackPosition);
		}
	}

	@Override
	public void start() {
		//System.out.println("Starting Trigger "+this+" "+triggerName);
		super.start();
		sb.addTriggerListener(this, triggerName);
	}

	@Override
	public void end() {
		//System.out.println("Ending Trigger "+this);
		sb.removeTriggerListener(this, triggerName);
		super.end();
	}

	@Override
	public void trigger(int trackPosition) {
		obj.addActiveTrigger(this);
		//System.out.println("trigger "+triggerName+" "+trackPosition+" "+startTime+" "+endTime+" "+this+" "+obj+" ");
		
		trackStartPos = trackPosition;
		update(trackStartPos);
		events.reset();
		isRunning = true;
	}

	@Override
	public void setIntialVal(int startTime) {
		// TODO Auto-generated method stub
		
	}
	
	
}

class SBCommandEventFlipH extends SBCommand{
	public SBCommandEventFlipH(SBObject obj, int startTime, int endTime) {
		super(obj, startTime, endTime);
	}

	@Override
	public void start() {
		obj.hFliped = true;
	}
	
	@Override
	public void end() {
		obj.hFliped = false;
	}

	@Override
	public void setIntialVal(int startTime) {
		if (startTime == endTime) obj.ihFliped = true;
	}
}
class SBCommandEventFlipV extends SBCommand{
	public SBCommandEventFlipV(SBObject obj, int startTime, int endTime) {
		super(obj, startTime, endTime);
	}

	@Override
	public void start() {
		obj.vFliped = true;
	}
	
	@Override
	public void end() {
		obj.vFliped = false;
	}

	@Override
	public void setIntialVal(int startTime) {
		if (startTime == endTime) obj.ivFliped = true;
	}
}
class SBCommandEventAddBlend extends SBCommand{
	public SBCommandEventAddBlend(SBObject obj, int startTime, int endTime) {
		super(obj, startTime, endTime);
	}

	@Override
	public void start() {
		obj.additiveBlend = true;
	}
	
	@Override
	public void end() {
		obj.additiveBlend = false;
	}

	@Override
	public void setIntialVal(int startTime) {
		if (startTime == endTime) obj.iadditiveBlend = true;
	}
}

