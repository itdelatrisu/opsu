package itdelatrisu.opsu.storyboard;

import java.util.Set;

/**
 * Storyboard events
 * @author fluddokt
 *
 */
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
//attaches an object to the storyboard
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
//Remove an object from the storyboard
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
//events based on command
abstract class CommandSBEvent extends SBEvent{
	SBCommand c;
	public CommandSBEvent(int time, SBCommand c) {
		super(time, c.obj);
		this.c = c;
		
	}
}
//adds the command to the activeCommands and starts it
class AttachCommandSBEvent extends CommandSBEvent{
	Set<SBCommand> activeCommands;
	public AttachCommandSBEvent(int time, Set<SBCommand> activeCommands, SBCommand c) {
		super(time, c);
		this.activeCommands = activeCommands;
	}
	
	@Override
	public void execute() {
		c.start();
		activeCommands.add(c);
	}
}
//Removes the command from the activeCommands and ends it
class RemoveCommandSBEvent extends CommandSBEvent{
	Set<SBCommand> activeCommands;
	public RemoveCommandSBEvent(int time, Set<SBCommand> activeCommands, SBCommand c) {
		super(time, c);
		this.activeCommands = activeCommands;
	}

	@Override
	public void execute() {
		activeCommands.remove(c);
		c.end();
	}

}
//Runs a command as if it was Starting
class StartCommandSBEvent extends CommandSBEvent{
	public StartCommandSBEvent(int time, SBCommand c) {
		super(time, c);
	}
	
	@Override
	public void execute() {
		c.start();
	}
}
//Runs a command as if it was Ending
class EndCommandSBEvent extends CommandSBEvent{
	public EndCommandSBEvent(int time, SBCommand c) {
		super(time, c);
	}

	@Override
	public void execute() {
		c.end();
	}
}