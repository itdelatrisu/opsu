package fluddokt.opsu.fake.gl;

import com.badlogic.gdx.Gdx;

public class GL32 {

	public static void glFramebufferTexture(int target,
			int attachment, int texture, int level) {
		Gdx.gl20.glFramebufferTexture2D(target, attachment, GL11.GL_TEXTURE_2D, texture, level);
		
	}

}
