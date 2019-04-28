package itdelatrisu.opsu.storyboard;

import java.util.List;
import java.util.Set;

/**
 * Storyboard Command Event Runner
 *  
 * @author fluddokt
 *
 */
class SBComEventRunner extends SBEventRunner{
	public int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;

	public void addCommands(List<SBCommand> commands, Set<SBCommand> activeCommands) {
		for(SBCommand c : commands) {
			start = Math.min(start, c.startTime);
			end = Math.max(end, c.endTime);
			if(c.startTime == c.endTime || c.isSameInitalEndValue) {
				if(c.isVarying)	//commands that vary just go to end value
					events.add(new EndCommandSBEvent(c.startTime, c));
				else	//commands that don't vary (flip/ blend) just sets it
					events.add(new StartCommandSBEvent(c.startTime, c));
					
			} else if(c.isVarying){
				events.add(new AttachCommandSBEvent(c.startTime, activeCommands, c));
				events.add(new RemoveCommandSBEvent(c.endTime, activeCommands, c));
			} else {
				events.add(new StartCommandSBEvent(c.startTime, c));
				events.add(new EndCommandSBEvent(c.endTime, c));
			}
		}
	}
}