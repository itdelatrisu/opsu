package org.newdawn.slick.tests;

import java.awt.Font;
import java.util.ArrayList;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.TrueTypeFont;

/**
 * A test of the font rendering capabilities
 * 
 * @author James Chambers (Jimmy)
 * @author Kevin Glass (kevglass)
 */
public class TrueTypeFontPerformanceTest extends BasicGame {
	/** The java.awt font we're going to test */
	private java.awt.Font awtFont;
	/** The True Type font we're going to use to render */
	private TrueTypeFont font;

	/** The test text */
	private String text = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Proin bibendum. Aliquam ac sapien a elit congue iaculis. Quisque et justo quis mi mattis euismod. Donec elementum, mi quis aliquet varius, nisi leo volutpat magna, quis ultricies eros augue at risus. Integer non magna at lorem sodales molestie. Integer diam nulla, ornare sit amet, mattis quis, euismod et, mauris. Proin eget tellus non nisl mattis laoreet. Nunc at nunc id elit pretium tempor. Duis vulputate, nibh eget rhoncus eleifend, tellus lectus sollicitudin mi, rhoncus tincidunt nisi massa vitae ipsum. Praesent tellus diam, luctus ut, eleifend nec, auctor et, orci. Praesent eu elit. Pellentesque ante orci, volutpat placerat, ornare eget, cursus sit amet, eros. Duis pede sapien, euismod a, volutpat pellentesque, convallis eu, mauris. Nunc eros. Ut eu risus et felis laoreet viverra. Curabitur a metus.";
	/** The text broken into lines */
	private ArrayList lines = new ArrayList();
	/** True if the text is visible */
	private boolean visible = true;

	/**
	 * Create a new test for font rendering
	 */
	public TrueTypeFontPerformanceTest() {
		super("Font Performance Test");
	}

	/**
	 * @see org.newdawn.slick.Game#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		awtFont = new java.awt.Font("Verdana", Font.PLAIN, 16);
		font = new TrueTypeFont(awtFont, false);

		for (int j = 0; j < 2; j++) {
			int lineLen = 90;
			for (int i = 0; i < text.length(); i += lineLen) {
				if (i + lineLen > text.length()) {
					lineLen = text.length() - i;
				}

				lines.add(text.substring(i, i + lineLen));
			}
			lines.add("");
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer,
	 *      org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		g.setFont(font);

		if (visible) {
			for (int i = 0; i < lines.size(); i++) {
				font.drawString(10, 50 + (i * 20), (String) lines.get(i),
						i > 10 ? Color.red : Color.green);
			}
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer,
	 *      int)
	 */
	public void update(GameContainer container, int delta)
			throws SlickException {
	}

	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
		if (key == Input.KEY_ESCAPE) {
			System.exit(0);
		}
		if (key == Input.KEY_SPACE) {
			visible = !visible;
		}
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv
	 *            The arguments passed in the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(
					new TrueTypeFontPerformanceTest());
			container.setDisplayMode(800, 600, false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
