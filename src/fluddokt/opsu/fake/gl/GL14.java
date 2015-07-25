package fluddokt.opsu.fake.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class GL14 {

	public static final int GL_FUNC_ADD = GL20.GL_FUNC_ADD;

	public static void glBlendEquation(int mode) {
		Gdx.gl20.glBlendEquation(mode);
		
	}

}
