package org.newdawn.slick.tests;
	
import java.util.ArrayList;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Log;

/**
 * A test for input
 * 
 * @author kevin
 */
public class InputTest extends BasicGame {
	/** The message to be displayed */
	private String message = "Press any key, mouse button, or drag the mouse";
	/** The lines to be drawn on the screen */
	private ArrayList lines = new ArrayList();
	/** True if the mouse button is down */
	private boolean buttonDown;
	/** The x position of our controlled stuff */
	private float x;
	/** The y position of our controlled stuff */
	private float y;
	/** The colors */
	private Color[] cols = new Color[] {Color.red, Color.green, Color.blue, Color.white, Color.magenta, Color.cyan};
	/** The current color index */
	private int index;
	/** The input syste being polled */
	private Input input;
	/** The scroll box */
	private int ypos;
	/** The container holding this test */
	private AppGameContainer app;
	
	/** True if space is down */
	private boolean space;
	/** True if left shift is down */
	private boolean lshift;
	/** True if right shift is down */
	private boolean rshift;
	
	/**
	 * Create a new input test
	 */
	public InputTest() {
		super("Input Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		if (container instanceof AppGameContainer) {
			app = (AppGameContainer) container;
		}
		
		input = container.getInput();
		x = 300;
		y = 300;
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
        g.drawString("left shift down: "+lshift, 100, 240);
        g.drawString("right shift down: "+rshift, 100, 260);
        g.drawString("space down: "+space, 100, 280); 
        
		g.setColor(Color.white);
		g.drawString(message, 10, 50);
		g.drawString(""+container.getInput().getMouseY(), 10, 400);
		g.drawString("Use the primary gamepad to control the blob, and hit a gamepad button to change the color", 10, 90);

		for (int i=0;i<lines.size();i++) {
			Line line = (Line) lines.get(i);
			line.draw(g);
		}
		
		g.setColor(cols[index]);
		g.fillOval((int) x, (int) y, 50, 50);
		g.setColor(Color.yellow);
		g.fillRect(50,200+ypos,40,40);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) {
        lshift = container.getInput().isKeyDown(Input.KEY_LSHIFT);
        rshift = container.getInput().isKeyDown(Input.KEY_RSHIFT);
        space = container.getInput().isKeyDown(Input.KEY_SPACE); 
        
		if (controllerLeft[0]) {
			x -= delta * 0.1f;
		}
		if (controllerRight[0]) {
			x += delta * 0.1f;
		}
		if (controllerUp[0]) {
			y -= delta * 0.1f;
		}
		if (controllerDown[0]) {
			y += delta * 0.1f;
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
		if (key == Input.KEY_ESCAPE) {
			System.exit(0);
		}
		if (key == Input.KEY_F1) {
			if (app != null) {
				try {
					app.setDisplayMode(600, 600, false);
					app.reinit();
				} catch (Exception e) { Log.error(e); }
			}
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#keyReleased(int, char)
	 */
	public void keyReleased(int key, char c) {
		message = "You pressed key code "+key+" (character = "+c+")";
	}

	/**
	 * @see org.newdawn.slick.BasicGame#mousePressed(int, int, int)
	 */
	public void mousePressed(int button, int x, int y) {
		if (button == 0) {
			buttonDown = true;
		}
		
		message = "Mouse pressed "+button+" "+x+","+y;
	}

	/**
	 * @see org.newdawn.slick.BasicGame#mouseReleased(int, int, int)
	 */
	public void mouseReleased(int button, int x, int y) {
		if (button == 0) {
			buttonDown = false;
		}
		
		message = "Mouse released "+button+" "+x+","+y;
	}

	/**
	 * @see org.newdawn.slick.BasicGame#mouseClicked(int, int, int, int)
	 */
	public void mouseClicked(int button, int x, int y, int clickCount) {
		System.out.println("CLICKED:"+x+","+y+" "+clickCount);
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#mouseWheelMoved(int)
	 */
	public void mouseWheelMoved(int change) {
		message = "Mouse wheel moved: "+change;
		
		if (change < 0) {
			ypos -= 10;
		} 
		if (change > 0) {
			ypos += 10;
		} 
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#mouseMoved(int, int, int, int)
	 */
	public void mouseMoved(int oldx, int oldy, int newx, int newy) {
		if (buttonDown) {
			lines.add(new Line(oldx,oldy,newx,newy));
		}
	}
	
	/**
	 * A line that has been drawn by the user
	 *
	 * @author kevin
	 */
	private class Line {
		/** The start x position */
		private int oldx;
		/** The start y position */
		private int oldy;
		/** The end x position */
		private int newx;
		/** The end y position */
		private int newy;
		
		/**
		 * Create a new line
		 * 
		 * @param oldx The start x position
		 * @param oldy The start y position
		 * @param newx The end x position
		 * @param newy The end y position
		 */
		public Line(int oldx, int oldy,int newx,int newy) {
			this.oldx = oldx;
			this.oldy = oldy;
			this.newx = newx;
			this.newy = newy;
		}
		
		/**
		 * Draw the line to the provided graphics context
		 * 
		 * @param g The graphics context on which to draw the line
		 */
		public void draw(Graphics g) {
			g.drawLine(oldx, oldy, newx, newy);
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#controllerButtonPressed(int, int)
	 */
	public void controllerButtonPressed(int controller, int button) {
		super.controllerButtonPressed(controller, button);
		
		index ++;
		index %= cols.length;
	}
	
	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments passed into our test
 	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new InputTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
