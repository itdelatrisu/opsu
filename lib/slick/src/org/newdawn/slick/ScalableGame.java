package org.newdawn.slick;

import org.newdawn.slick.opengl.SlickCallable;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;

/**
 * A wrapper to allow any game to be scalable. This relies on knowing the 
 * normal width/height of the game - i.e. the dimensions that the game is
 * expecting to be run at. The wrapper then takes the size of the container
 * and scales rendering and input based on the ratio.
 *
 * Note: Using OpenGL directly within a ScalableGame can break it
 * 
 * @author kevin
 */
public class ScalableGame implements Game {
	/** The renderer to use for all GL operations */
	private static SGL GL = Renderer.get();
	
	/** The normal or native width of the game */
	private float normalWidth;
	/** The normal or native height of the game */
	private float normalHeight;
	/** The game that is being wrapped */
	private Game held;
	/** True if we should maintain the aspect ratio */
	private boolean maintainAspect;
	/** The target width */
	private int targetWidth;
	/** The target height */
	private int targetHeight;
	/** The game container wrapped */
	private GameContainer container;
	
	/** 
	 * Create a new scalable game wrapper
	 * 
	 * @param held The game to be wrapper and displayed at a different resolution
	 * @param normalWidth The normal width of the game
	 * @param normalHeight The noral height of the game
	 */
	public ScalableGame(Game held, int normalWidth, int normalHeight) {
		this(held, normalWidth, normalHeight, false);
	}
	
	/** 
	 * Create a new scalable game wrapper
	 * 
	 * @param held The game to be wrapper and displayed at a different resolution
	 * @param normalWidth The normal width of the game
	 * @param normalHeight The noral height of the game
	 * @param maintainAspect True if we should maintain the aspect ratio
	 */
	public ScalableGame(Game held, int normalWidth, int normalHeight, boolean maintainAspect) {
		this.held = held;
		this.normalWidth = normalWidth;
		this.normalHeight = normalHeight;
		this.maintainAspect = maintainAspect;
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		this.container = container;
		
		recalculateScale();
		held.init(container);
	}
	
	/**
	 * Recalculate the scale of the game
	 * 
	 * @throws SlickException Indicates a failure to reinit the game
	 */
	public void recalculateScale() throws SlickException {
		targetWidth = container.getWidth();
		targetHeight = container.getHeight();
		
		if (maintainAspect) {
			boolean normalIsWide = (normalWidth / normalHeight > 1.6 ? true : false);
			boolean containerIsWide = ((float) targetWidth / (float) targetHeight > 1.6 ? true : false);
			float wScale = targetWidth / normalWidth;
			float hScale = targetHeight / normalHeight;

			if (normalIsWide & containerIsWide) {
				float scale = (wScale < hScale ? wScale : hScale);
				targetWidth = (int) (normalWidth * scale);
				targetHeight = (int) (normalHeight * scale);
			} else if (normalIsWide & !containerIsWide) {
				targetWidth = (int) (normalWidth * wScale);
				targetHeight = (int) (normalHeight * wScale);
			} else if (!normalIsWide & containerIsWide) {
				targetWidth = (int) (normalWidth * hScale);
				targetHeight = (int) (normalHeight * hScale);
			} else {
				float scale = (wScale < hScale ? wScale : hScale);
				targetWidth = (int) (normalWidth * scale);
				targetHeight = (int) (normalHeight * scale);
			}

		} 
		
		if (held instanceof InputListener) {
			container.getInput().addListener((InputListener) held);
		}
		container.getInput().setScale(normalWidth / targetWidth,
									  normalHeight / targetHeight);
		

		int yoffset = 0;
		int xoffset = 0;
		
		if (targetHeight < container.getHeight()) {
			yoffset = (container.getHeight() - targetHeight) / 2;
		}
		if (targetWidth < container.getWidth()) {
			xoffset = (container.getWidth() - targetWidth) / 2;
		}
		container.getInput().setOffset(-xoffset / (targetWidth / normalWidth), 
									   -yoffset / (targetHeight / normalHeight));
		
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) throws SlickException {
		if ((targetHeight != container.getHeight()) ||
		    (targetWidth != container.getWidth())) {
			recalculateScale();
		}
		
		held.update(container, delta);
	}

	/**
	 * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public final void render(GameContainer container, Graphics g)
			throws SlickException {
		int yoffset = 0;
		int xoffset = 0;
		
		if (targetHeight < container.getHeight()) {
			yoffset = (container.getHeight() - targetHeight) / 2;
		}
		if (targetWidth < container.getWidth()) {
			xoffset = (container.getWidth() - targetWidth) / 2;
		}
		
		SlickCallable.enterSafeBlock();
		g.setClip(xoffset, yoffset, targetWidth, targetHeight);
		GL.glTranslatef(xoffset, yoffset, 0);
		g.scale(targetWidth / normalWidth, targetHeight / normalHeight);
		GL.glPushMatrix();
		held.render(container, g);
		GL.glPopMatrix();
		g.clearClip();
		SlickCallable.leaveSafeBlock();
		
		renderOverlay(container, g);
	}

	/**
	 * Render the overlay that will sit over the scaled screen
	 * 
	 * @param container The container holding the game being render
	 * @param g Graphics context on which to render
	 */
	protected void renderOverlay(GameContainer container, Graphics g) {
	}
	
	/**
	 * @see org.newdawn.slick.Game#closeRequested()
	 */
	public boolean closeRequested() {
		return held.closeRequested();
	}

	/**
	 * @see org.newdawn.slick.Game#getTitle()
	 */
	public String getTitle() {
		return held.getTitle();
	}
}
