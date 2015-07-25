package fluddokt.opsu.fake.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class GL13 {

	public static final int GL_ACTIVE_TEXTURE = GL20.GL_ACTIVE_TEXTURE;

	public static void glActiveTexture(int texture) {
		Gdx.gl20.glActiveTexture(texture);
	}

}
