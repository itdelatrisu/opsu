package fluddokt.opsu.fake;

public class LineEvent {

	public static enum Type{
		STOP
		
	}

	Type type;
	public LineEvent(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

}
