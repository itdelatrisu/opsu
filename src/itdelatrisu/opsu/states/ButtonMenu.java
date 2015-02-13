/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.states;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.MenuButton;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OsuGroupList;
import itdelatrisu.opsu.OsuGroupNode;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;

import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.EmptyTransition;
import org.newdawn.slick.state.transition.FadeInTransition;

/**
 * Generic button menu state.
 */
public class ButtonMenu extends BasicGameState {
	/** Menu states. */
	public enum MenuState {
		EXIT (new Button[] { Button.YES, Button.NO }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				return new String[] { "Are you sure you want to exit opsu!?" };
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.NO.click(container, game);
			}
		},
		BEATMAP (new Button[] { Button.CLEAR_SCORES, Button.DELETE, Button.CANCEL }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				OsuGroupNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				String osuString = (node != null) ? OsuGroupList.get().getBaseNode(node.index).toString() : "";
				return new String[] { osuString, "What do you want to do with this beatmap?" };
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.CANCEL.click(container, game);
			}
		},
		BEATMAP_DELETE_SELECT (new Button[] { Button.DELETE_GROUP, Button.DELETE_SONG, Button.CANCEL_DELETE }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				OsuGroupNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				String osuString = (node != null) ? node.toString() : "";
				return new String[] { String.format("Are you sure you wish to delete '%s' from disk?", osuString) };
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.CANCEL_DELETE.click(container, game);
			}
		},
		BEATMAP_DELETE_CONFIRM (new Button[] { Button.DELETE_CONFIRM, Button.CANCEL_DELETE }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				return BEATMAP_DELETE_SELECT.getTitle(container, game);
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.CANCEL_DELETE.click(container, game);
			}
		},
		RELOAD (new Button[] { Button.RELOAD_CONFIRM, Button.RELOAD_CANCEL }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				return new String[] {
						"You have requested a full process of your beatmaps.",
						"This could take a few minutes.",
						"Are you sure you wish to continue?"
				};
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.RELOAD_CANCEL.click(container, game);
			}
		};

		/** The buttons in the state. */
		private Button[] buttons;

		/** The associated MenuButton objects. */
		private MenuButton[] menuButtons;

		/** Initial x coordinate offsets left/right of center (for shifting animation), times width. (TODO) */
		private static final float OFFSET_WIDTH_RATIO = 1 / 18f;

		/**
		 * Constructor.
		 * @param buttons the ordered list of buttons in the state
		 */
		MenuState(Button[] buttons) {
			this.buttons = buttons;
		}

		/**
		 * Initializes the menu state.
		 * @param container the game container
		 * @param game the game
		 * @param button the center button image
		 * @param buttonL the left button image
		 * @param buttonR the right button image
		 */
		public void init(GameContainer container, StateBasedGame game, Image button, Image buttonL, Image buttonR) {
			float center = container.getWidth() / 2f;
			float centerOffset = container.getWidth() * OFFSET_WIDTH_RATIO;
			float baseY = container.getHeight() * 0.2f;
			baseY += ((getTitle(container, game).length - 1) * Utils.FONT_LARGE.getLineHeight());
			float offsetY = button.getHeight() * 1.25f;

			menuButtons = new MenuButton[buttons.length];
			for (int i = 0; i < buttons.length; i++) {
				MenuButton b = new MenuButton(button, buttonL, buttonR,
						center + ((i % 2 == 0) ? centerOffset * -1 : centerOffset),
						baseY + (i * offsetY));
				b.setText(String.format("%d. %s", i + 1, buttons[i].getText()),
						Utils.FONT_XLARGE, Color.white);
				b.setHoverFade();
				menuButtons[i] = b;
			}
		}

		/**
		 * Draws the title and buttons to the graphics context.
		 * @param container the game container
		 * @param game the game
		 * @param g the graphics context
		 */
		public void draw(GameContainer container, StateBasedGame game, Graphics g) {
			// draw title
			String[] title = getTitle(container, game);
			float c = container.getWidth() * 0.02f;
			int maxLineWidth = container.getWidth() - (int) (c * 2);
			int lineHeight = Utils.FONT_LARGE.getLineHeight();
			for (int i = 0, j = 0; i < title.length; i++, j++) {
				// wrap text if too long
				if (Utils.FONT_LARGE.getWidth(title[i]) > maxLineWidth) {
					List<String> list = Utils.wrap(title[i], Utils.FONT_LARGE, maxLineWidth);
					for (String str : list)
						Utils.FONT_LARGE.drawString(c, c + (j++ * lineHeight), str, Color.white);
				} else
					Utils.FONT_LARGE.drawString(c, c + (j * lineHeight), title[i], Color.white);
			}

			// draw buttons
			for (int i = 0; i < buttons.length; i++)
				menuButtons[i].draw(buttons[i].getColor());
		}

		/**
		 * Updates the menu state.
		 * @param container the game container
		 * @param delta the delta interval
		 * @param mouseX the mouse x coordinate
		 * @param mouseY the mouse y coordinate
		 */
		public void update(GameContainer container, int delta, int mouseX, int mouseY) {
			float center = container.getWidth() / 2f;
			for (int i = 0; i < buttons.length; i++) {
				menuButtons[i].hoverUpdate(delta, mouseX, mouseY);

				// move button to center
				float x = menuButtons[i].getX();
				if (i % 2 == 0) {
					if (x < center)
						menuButtons[i].setX(Math.min(x + (delta / 5f), center));
				} else {
					if (x > center)
						menuButtons[i].setX(Math.max(x - (delta / 5f), center));
				}
			}
		}

		/**
		 * Processes a mouse click action.
		 * @param container the game container
		 * @param game the game
		 * @param cx the x coordinate
		 * @param cy the y coordinate
		 */
		public void click(GameContainer container, StateBasedGame game, int cx, int cy) {
			for (int i = 0; i < buttons.length; i++) {
				if (menuButtons[i].contains(cx, cy)) {
					buttons[i].click(container, game);
					break;
				}
			}
		}

		/**
		 * Processes a key press action (numeric digits only).
		 * @param container the game container
		 * @param game the game
		 * @param digit the digit pressed
		 */
		public void keyPress(GameContainer container, StateBasedGame game, int digit) {
			int index = digit - 1;
			if (index >= 0 && index < buttons.length)
				buttons[index].click(container, game);
		}

		/**
		 * Retrieves the title strings for the menu state (via override).
		 * @param container the game container
		 * @param game the game
		 */
		public String[] getTitle(GameContainer container, StateBasedGame game) { return new String[0]; }

		/**
		 * Processes a state enter request.
		 * @param container the game container
		 * @param game the game
		 */
		public void enter(GameContainer container, StateBasedGame game) {
			float center = container.getWidth() / 2f;
			float centerOffset = container.getWidth() * OFFSET_WIDTH_RATIO;
			for (int i = 0; i < buttons.length; i++) {
				menuButtons[i].setX(center + ((i % 2 == 0) ? centerOffset * -1 : centerOffset));
				menuButtons[i].resetHover();
			}
		}

		/**
		 * Processes a state exit request (via override).
		 * @param container the game container
		 * @param game the game
		 */
		public void leave(GameContainer container, StateBasedGame game) {}
	};

	/** Button types. */
	private enum Button {
		YES ("Yes", Color.green) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				container.exit();
			}
		},
		NO ("No", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUBACK);
				game.enterState(Opsu.STATE_MAINMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			}
		},
		CLEAR_SCORES ("Clear local scores", Color.magenta) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				OsuGroupNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).doStateActionOnLoad(MenuState.BEATMAP, node);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			}
		},
		DELETE ("Delete...", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				OsuGroupNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				MenuState ms = (node.osuFileIndex == -1 || node.osuFiles.size() == 1) ?
						MenuState.BEATMAP_DELETE_CONFIRM : MenuState.BEATMAP_DELETE_SELECT;
				((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(ms, node);
				game.enterState(Opsu.STATE_BUTTONMENU);
			}
		},
		CANCEL ("Cancel", Color.gray) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUBACK);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			}
		},
		DELETE_CONFIRM ("Yes, delete this beatmap!", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				OsuGroupNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).doStateActionOnLoad(MenuState.BEATMAP_DELETE_CONFIRM, node);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			}
		},
		DELETE_GROUP ("Yes, delete all difficulties!", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				DELETE_CONFIRM.click(container, game);
			}
		},
		DELETE_SONG ("Yes, but only this difficulty", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				OsuGroupNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).doStateActionOnLoad(MenuState.BEATMAP_DELETE_SELECT, node);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			}
		},
		CANCEL_DELETE ("Nooooo! I didn't mean to!", Color.gray) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				CANCEL.click(container, game);
			}
		},
		RELOAD_CONFIRM ("Let's do it!", Color.green) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).doStateActionOnLoad(MenuState.RELOAD);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			}
		},
		RELOAD_CANCEL ("Cancel", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				CANCEL.click(container, game);
			}
		};

		/** The text to show on the button. */
		private String text;

		/** The button color. */
		private Color color;

		/**
		 * Constructor.
		 * @param text the text to show on the button
		 * @param color the button color
		 */
		Button(String text, Color color) {
			this.text = text;
			this.color = color;
		}

		/**
		 * Returns the button text.
		 */
		public String getText() { return text; }

		/**
		 * Returns the button color.
		 */
		public Color getColor() { return color; }

		/**
		 * Processes a mouse click action (via override).
		 * @param container the game container
		 * @param game the game
		 */
		public void click(GameContainer container, StateBasedGame game) {}
	}

	/** The current menu state. */
	private MenuState menuState;

	/** The song node to process in the state. */
	private OsuGroupNode node;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private int state;

	public ButtonMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		this.input = container.getInput();

		// initialize buttons
		Image button = GameImage.MENU_BUTTON_MID.getImage();
		button = button.getScaledCopy(container.getWidth() / 2, button.getHeight());
		Image buttonL = GameImage.MENU_BUTTON_LEFT.getImage();
		Image buttonR = GameImage.MENU_BUTTON_RIGHT.getImage();
		for (MenuState ms : MenuState.values())
			ms.init(container, game, button, buttonL, buttonR);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Color.black);
		if (menuState != null)
			menuState.draw(container, game, g);
		Utils.drawVolume(g);
		Utils.drawFPS();
		Utils.drawCursor();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		Utils.updateCursor(delta);
		Utils.updateVolumeDisplay(delta);
		if (menuState != null)
			menuState.update(container, delta, input.getMouseX(), input.getMouseY());
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button
		if (button != Input.MOUSE_LEFT_BUTTON)
			return;

		if (menuState != null)
			menuState.click(container, game, x, y);
	}

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
			if (menuState != null)
				menuState.leave(container, game);
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		default:
			if (menuState != null)
				menuState.keyPress(container, game, Character.getNumericValue(c));
			break;
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		if (menuState != null)
			menuState.enter(container, game);
	}

	/**
	 * Changes the menu state.
	 * @param menuState the new menu state
	 */
	public void setMenuState(MenuState menuState) { setMenuState(menuState, null); }

	/**
	 * Changes the menu state.
	 * @param menuState the new menu state
	 * @param node the song node to process in the state
	 */
	public void setMenuState(MenuState menuState, OsuGroupNode node) {
		this.menuState = menuState;
		this.node = node;
	}

	/**
	 * Returns the song node being processed, or null if none.
	 */
	public OsuGroupNode getNode() { return node; }
}
