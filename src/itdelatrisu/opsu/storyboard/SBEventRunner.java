package itdelatrisu.opsu.storyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Storyboard Event Runner
 * 
 * @author fluddokt
 *
 */
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