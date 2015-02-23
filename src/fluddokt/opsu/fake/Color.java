package fluddokt.opsu.fake;

public class Color {

	public static final Color black = new Color(0f, 0f, 0f);
	public static final Color white = new Color(1f, 1f, 1f);
	public static final Color transparent = new Color(0f, 0f, 0f, 0.5f);
	public static final Color lightGray = new Color(0.8f, 0.8f, 0.8f, 0.7f);
	public static final Color green = new Color(0f, 1f, 0f);
	public static final Color red = new Color(1f, 0f, 0f);
	public static final Color orange = new Color(1f, 0.5f, 0f);
	public static final Color blue = new Color(0f, 0f, 1f);
	public static final Color lightgreen = new Color(0.5f, 1f, 0.5f);
	public static final Color lightred = new Color(1f, 0.5f, 0.5f);
	public static final Color lightorange = new Color(1f, 0.75f, 0.5f);
	public static final Color lightblue = new Color(0.5f, 0.5f, 1f);
	public static final Color gray = new Color(0.5f, 0.5f, 0.5f);
	public static final Color magenta = new Color(1f, 0f, 1f);
	public static final Color cyan = new Color(0f, 1f, 1f);
	public float r, g, b, a;

	public Color(float r, float g, float b) {
		this(r, g, b, 1f);
	}

	public Color(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	public Color(int r, int g, int b, float a) {
		this.r = r / 255f;
		this.g = g / 255f;
		this.b = b / 255f;
		this.a = a;
	}

	public Color(int r, int g, int b, int a) {
		this.r = r / 255f;
		this.g = g / 255f;
		this.b = b / 255f;
		this.a = a / 255f;
	}

	public Color(int r, int g, int b) {
		this(r, g, b, 255);
	}

	public Color(Color color) {
		r = color.r;
		g = color.g;
		b = color.b;
		a = color.a;
	}

	protected Color multAlpha(float na) {
		return new Color(r, g, b, a * na);
	}

}
