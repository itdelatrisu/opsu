package itdelatrisu.opsu.storyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public abstract class SBEvent {
	
	SBObject obj;
	int time;
	public SBEvent(int time, SBObject o) {
		this.time = time;
		this.obj = o;
	}

	public abstract void execute();
	public int getTime() {
		return time;
	}

}
class AttachObjectSBEvent extends SBEvent{

	Storyboard storyboard;
	public AttachObjectSBEvent(int time, Storyboard storyboard, SBObject o) {
		super(time, o);
		this.storyboard = storyboard;
	}

	@Override
	public void execute() {
		if(!obj.layer.add(obj)) {
			System.err.println("Fail to attach Object to storyboard "+obj);
		}
	}
	
}
class RemoveObjectSBEvent extends SBEvent{

	Storyboard storyboard;
	public RemoveObjectSBEvent(int time, Storyboard storyboard,SBObject o) {
		super(time, o);
		this.storyboard = storyboard;
	}

	@Override
	public void execute() {
		if (!obj.layer.remove(obj))
			System.err.println("Fail to remove Object to storyboard "+obj);
		
	}
	
}
abstract class CommandSBEvent extends SBEvent{
	SBCommand c;
	public CommandSBEvent(int time, SBCommand c) {
		super(time, c.obj);
		this.c = c;
		
	}
}
class AttachCommandSBEvent extends CommandSBEvent{
	HashSet<SBCommand> activeCommands;
	public AttachCommandSBEvent(int time, HashSet<SBCommand> activeCommands, SBCommand c) {
		super(time, c);
		this.activeCommands = activeCommands;
	}
	
	@Override
	public void execute() {
		c.start();
		activeCommands.add(c);
	}
}
class RemoveCommandSBEvent extends CommandSBEvent{
	HashSet<SBCommand> activeCommands;
	public RemoveCommandSBEvent(int time, HashSet<SBCommand> activeCommands, SBCommand c) {
		super(time, c);
		this.activeCommands = activeCommands;
	}

	@Override
	public void execute() {
		activeCommands.remove(c);
		c.end();
	}

}
class StartCommandSBEvent extends CommandSBEvent{
	public StartCommandSBEvent(int time, SBCommand c) {
		super(time, c);
	}
	
	@Override
	public void execute() {
		c.start();
	}
}
class EndCommandSBEvent extends CommandSBEvent{

	public EndCommandSBEvent(int time, SBCommand c) {
		super(time, c);
	}

	@Override
	public void execute() {
		c.end();
	}
}
class SBEventRunner {
	ArrayList<SBEvent> events = new ArrayList<>();
	int eventsIndex = 0;
	SBEventRunnerListener listener = new SBEventRunnerListener() {
		@Override
		public void reseted() {}
	};
	public void ready() {
		Collections.sort(events, new Comparator<SBEvent>() {
			@Override
			public int compare(SBEvent o1, SBEvent o2) {
				return Integer.compare(o1.getTime(), o2.getTime());
			}
		});
	}
	public void update(int trackPosition) {
		if (eventsIndex-1 >= 0 && events.get(eventsIndex-1).getTime() > trackPosition)
			reset();
		
		while (eventsIndex<events.size() && events.get(eventsIndex).getTime() <= trackPosition) {
			//System.out.println("SB Execute:"+events.get(eventsIndex)+" "+this);
			events.get(eventsIndex).execute();
			eventsIndex++;
		}
	}
	public void reset() {
		eventsIndex = 0;
		listener.reseted();
	}
	public void add(SBEvent e) {
		events.add(e);
	}
	public void setListener(SBEventRunnerListener listenener) {
		this.listener = listenener;
	}
}
interface SBEventRunnerListener {
	public void reseted();
}
class SBComEventRunner extends SBEventRunner{
	public int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;

	public void addCommands(List<SBCommand> commands, HashSet<SBCommand> activeCommands) {
		for(SBCommand c : commands) {
			start = Math.min(start, c.startTime);
			end = Math.max(end, c.endTime);
			if(c.startTime == c.endTime || c.isSameInitalEndValue) {
				if(c.isVarying)
					events.add(new EndCommandSBEvent(c.startTime, c));
				else
					events.add(new StartCommandSBEvent(c.startTime, c));
					
			} else if(c.isVarying){
				events.add(new AttachCommandSBEvent(c.startTime, activeCommands, c));
				events.add(new RemoveCommandSBEvent(c.endTime, activeCommands, c));
			} else {
				events.add(new StartCommandSBEvent(c.startTime, c));
				events.add(new EndCommandSBEvent(c.endTime, c));
			}
			//System.out.println("C:"+c);
		}
	}
	
}