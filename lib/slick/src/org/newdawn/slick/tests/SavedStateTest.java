package org.newdawn.slick.tests;
	
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SavedState;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.gui.AbstractComponent;
import org.newdawn.slick.gui.ComponentListener;
import org.newdawn.slick.gui.TextField;

/**
 * A test of the the local storage utilities
 *
 * @author kevin
 */
public class SavedStateTest extends BasicGame implements ComponentListener {
	/** The field taking the name */
	private TextField name;
	/** The field taking the age */
	private TextField age;
	/** The name value */
	private String nameValue = "none";
	/** The age value */
	private int ageValue = 0;
	/** The saved state */
	private SavedState state;
	/** The status message to display */
	private String message = "Enter a name and age to store";
	
	/**
	 * Create a new test for font rendering
	 */
	public SavedStateTest() {
		super("Saved State Test");
	}
	
	/**
	 * @see org.newdawn.slick.Game#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		state = new SavedState("testdata");
		nameValue = state.getString("name","DefaultName");
		ageValue = (int) state.getNumber("age",64);
		
		name = new TextField(container,container.getDefaultFont(),100,100,300,20,this);
		age = new TextField(container,container.getDefaultFont(),100,150,201,20,this);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		name.render(container, g);
		age.render(container, g);
		
		container.getDefaultFont().drawString(100, 300, "Stored Name: "+nameValue);
		container.getDefaultFont().drawString(100, 350, "Stored Age: "+ageValue);
		container.getDefaultFont().drawString(200, 500, message);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) throws SlickException {
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
		if (key == Input.KEY_ESCAPE) {
			System.exit(0);
		}
	}
	
	/** The container we're using */
	private static AppGameContainer container;
	
	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments passed in the test
	 */
	public static void main(String[] argv) {
		try {
			container = new AppGameContainer(new SavedStateTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see org.newdawn.slick.gui.ComponentListener#componentActivated(org.newdawn.slick.gui.AbstractComponent)
	 */
	public void componentActivated(AbstractComponent source) {
		if (source == name) {
			nameValue = name.getText();
			state.setString("name", nameValue);
		}
		if (source == age) {
			try {
				ageValue = Integer.parseInt(age.getText());
				state.setNumber("age", ageValue);
			} catch (NumberFormatException e) {
				// ignone
			}
		}

		try {
			state.save();
		} catch (Exception e) {
			message = System.currentTimeMillis() + " : Failed to save state";
		}
	}
}
