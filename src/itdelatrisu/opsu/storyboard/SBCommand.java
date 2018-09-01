package itdelatrisu.opsu.storyboard;

import java.util.ArrayList;
import java.util.HashSet;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

/**
 * Storyboard commands
 * 
 * @author fluddokt
 *
 */
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
	}

	@Override
	final public void update(int trackPosition) {
		if (duration>0)
			updateT(Utils.clamp((trackPosition - startTime) / duration ,0,1));
	}
	@Override
	public void start() {
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
				t = AnimationEquation.OUT_QUAD.calc(t); break;
			case 2: // ease in
				t = AnimationEquation.IN_QUAD.calc(t); break;
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
		//start + t*(end-start)
		
	}

	@Override
	public String toString() {
		return obj.lineNumber+" "+mod;
	}
	
}

enum SBModify {
	SBFadeModify(){
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
	},
	SBMoveModify(){
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
	},
	SBMovexModify {
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
	},
	SBMoveyModify{
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
	},
	SBScaleModify{
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
	},
	SBSpecialScaleModify{
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
	},
	SBVecScaleModify{
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
	},
	SBRotateModify{
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
	},
	SBColorModify{
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
	},
	;
	public int vars() {return 0;}
	public void set(SBObject o, float[] vals) {}
	public void setI(SBObject o, float[] vals, int startTime) {}
	public void get(SBObject o, float[] vals) {}
	public void init(float[] vals){}
}

abstract class SBCommandMult extends SBCommand{
	ArrayList<SBCommand> commands;
	HashSet<SBCommand> activeCommands = new HashSet<>();
	int eventsDuration;
	SBComEventRunner events = new SBComEventRunner();
	public SBCommandMult(SBObject obj, int startTime, int endTime, ArrayList<SBCommand> commands) {
		super(obj, startTime, endTime);
		this.commands = commands;
		
		events.addCommands(commands, activeCommands);
		eventsDuration = events.end - events.start;
		events.ready();
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
		trackPosition -= startTime;
		
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
		events.update(trackPosition);
		for(SBCommand c : activeCommands) {
			c.update(trackPosition);
		}
	}

	@Override
	public void start() {
		super.start();
		sb.addTriggerListener(this, triggerName);
	}

	@Override
	public void end() {
		sb.removeTriggerListener(this, triggerName);
		super.end();
	}

	@Override
	public void trigger(int trackPosition) {
		obj.addActiveTrigger(this);
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

