/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2017 Jeffrey Han
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

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OpsuConstants;
import itdelatrisu.opsu.ScoreData;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.beatmap.BeatmapSetNode;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
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
 * <p>
 * Displays a header and a set of defined options to the player.
 */
public class ButtonMenu extends BasicGameState {
	/** Menu states. */
	public enum MenuState {
		/** The exit confirmation screen. */
		EXIT (new Button[] { Button.YES, Button.NO }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				return new String[] {
					String.format("Are you sure you want to exit %s?", OpsuConstants.PROJECT_NAME)
				};
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.NO.click(container, game);
			}
		},
		/** The initial beatmap management screen (for a non-"favorite" beatmap). */
		BEATMAP (new Button[] { Button.CLEAR_SCORES, Button.FAVORITE_ADD, Button.DELETE, Button.CANCEL }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				BeatmapSetNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				String beatmapString = (node != null) ? BeatmapSetList.get().getBaseNode(node.index).toString() : "";
				return new String[] { beatmapString, "What do you want to do with this beatmap?" };
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.CANCEL.click(container, game);
			}
		},
		/** The initial beatmap management screen (for a "favorite" beatmap). */
		BEATMAP_FAVORITE (new Button[] { Button.CLEAR_SCORES, Button.FAVORITE_REMOVE, Button.DELETE, Button.CANCEL }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				return BEATMAP.getTitle(container, game);
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				BEATMAP.leave(container, game);
			}
		},
		/** The beatmap deletion screen for a beatmap set with multiple beatmaps. */
		BEATMAP_DELETE_SELECT (new Button[] { Button.DELETE_GROUP, Button.DELETE_SONG, Button.CANCEL_DELETE }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				BeatmapSetNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				String beatmapString = (node != null) ? node.toString() : "";
				return new String[] { String.format("Are you sure you wish to delete '%s' from disk?", beatmapString) };
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.CANCEL_DELETE.click(container, game);
			}
		},
		/** The beatmap deletion screen for a single beatmap. */
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
		/** The beatmap reloading confirmation screen. */
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
		},
		/** The score management screen. */
		SCORE (new Button[] { Button.DELETE_SCORE, Button.CLOSE }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				return new String[] { "Score Management" };
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.CLOSE.click(container, game);
			}
		},
		/** The game mod selection screen. */
		MODS (new Button[] { Button.RESET_MODS, Button.CLOSE }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				return new String[] {
					"Mods provide different ways to enjoy gameplay. Some have an effect on the score you can achieve during ranked play. Others are just for fun."
				};
			}

			@Override
			protected float getBaseY(GameContainer container, StateBasedGame game) {
				return container.getHeight() * 2f / 3;
			}

			@Override
			public void enter(GameContainer container, StateBasedGame game) {
				super.enter(container, game);
				for (GameMod mod : GameMod.values())
					mod.resetHover();
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.CLOSE.click(container, game);
			}

			@Override
			public void draw(GameContainer container, StateBasedGame game, Graphics g) {
				int width = container.getWidth();
				int height = container.getHeight();

				// score multiplier (TODO: fade in color changes)
				float mult = GameMod.getScoreMultiplier();
				String multString = String.format("Score Multiplier: %.2fx", mult);
				Color multColor = (mult == 1f) ? Color.white : (mult > 1f) ? Color.green : Color.red;
				float multY = Fonts.LARGE.getLineHeight() * 2 + height * 0.06f;
				Fonts.LARGE.drawString(
						(width - Fonts.LARGE.getWidth(multString)) / 2f,
						multY, multString, multColor);

				// category text
				for (GameMod.Category category : GameMod.Category.values()) {
					Fonts.LARGE.drawString(category.getX(),
							category.getY() - Fonts.LARGE.getLineHeight() / 2f,
							category.getName(), category.getColor());
				}

				// buttons
				for (GameMod mod : GameMod.values())
					mod.draw();

				super.draw(container, game, g);
			}

			@Override
			public void update(GameContainer container, int delta, int mouseX, int mouseY) {
				super.update(container, delta, mouseX, mouseY);
				GameMod hoverMod = null;
				for (GameMod mod : GameMod.values()) {
					mod.hoverUpdate(delta, mod.isActive());
					if (hoverMod == null && mod.contains(mouseX, mouseY))
						hoverMod = mod;
				}

				// tooltips
				if (hoverMod != null)
					UI.updateTooltip(delta, hoverMod.getDescription(), true);
			}

			@Override
			public void keyPress(GameContainer container, StateBasedGame game, int key, char c) {
				super.keyPress(container, game, key, c);
				for (GameMod mod : GameMod.values()) {
					if (key == mod.getKey()) {
						mod.toggle(true);
						break;
					}
				}
			}

			@Override
			public void click(GameContainer container, StateBasedGame game, int cx, int cy) {
				super.click(container, game, cx, cy);
				for (GameMod mod : GameMod.values()) {
					if (mod.contains(cx, cy)) {
						boolean prevState = mod.isActive();
						mod.toggle(true);
						if (mod.isActive() != prevState)
							SoundController.playSound(SoundEffect.MENUCLICK);
						return;
					}
				}
			}
		},
		/** The "About" screen. */
		ABOUT (new Button[] { Button.ABOUT_WEBSITE, Button.ABOUT_REPOSITORY, Button.ABOUT_REPORT, Button.ABOUT_ENV, Button.ABOUT_CREDITS, Button.ABOUT_CLOSE }) {
			@Override
			public String[] getTitle(GameContainer container, StateBasedGame game) {
				String version = Updater.get().getCurrentVersion();
				return new String[] {
					String.format(
						"%s %s by %s",
						OpsuConstants.PROJECT_NAME,
						(version == null) ? "(unknown version)" : "v" + version,
						OpsuConstants.PROJECT_AUTHOR
					),
					"Click an option below to learn more!"
				};
			}

			@Override
			public void leave(GameContainer container, StateBasedGame game) {
				Button.ABOUT_CLOSE.click(container, game);
			}
		};

		/** The buttons in the state. */
		private final Button[] buttons;

		/** The associated MenuButton objects. */
		private MenuButton[] menuButtons;

		/** The actual title string list, generated upon entering the state. */
		private List<String> actualTitle;

		/** The horizontal center offset, used for the initial button animation. */
		private AnimatedValue centerOffset;

		/** Initial x coordinate offsets left/right of center (for shifting animation), times width. (TODO) */
		private static final float OFFSET_WIDTH_RATIO = 1 / 25f;

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
			float baseY = getBaseY(container, game);
			float offsetY = button.getHeight() * 1.25f;

			menuButtons = new MenuButton[buttons.length];
			for (int i = 0; i < buttons.length; i++) {
				MenuButton b = new MenuButton(button, buttonL, buttonR, center, baseY + (i * offsetY));
				b.setText(String.format("%d. %s", i + 1, buttons[i].getText()), Fonts.XLARGE, Color.white);
				b.setHoverFade();
				menuButtons[i] = b;
			}
		}

		/**
		 * Returns the base Y coordinate for the buttons.
		 * @param container the game container
		 * @param game the game
		 */
		protected float getBaseY(GameContainer container, StateBasedGame game) {
			float baseY = container.getHeight() * 0.2f;
			baseY += ((getTitle(container, game).length - 1) * Fonts.LARGE.getLineHeight());
			return baseY;
		}

		/**
		 * Draws the title and buttons to the graphics context.
		 * @param container the game container
		 * @param game the game
		 * @param g the graphics context
		 */
		public void draw(GameContainer container, StateBasedGame game, Graphics g) {
			// draw title
			if (actualTitle != null) {
				float marginX = container.getWidth() * 0.015f, marginY = container.getHeight() * 0.01f;
				int lineHeight = Fonts.LARGE.getLineHeight();
				for (int i = 0, size = actualTitle.size(); i < size; i++)
					Fonts.LARGE.drawString(marginX, marginY + (i * lineHeight), actualTitle.get(i), Color.white);
			}

			// draw buttons
			for (int i = 0; i < buttons.length; i++)
				menuButtons[i].draw(buttons[i].getColor());

			UI.draw(g);
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
			boolean centerOffsetUpdated = centerOffset.update(delta);
			float centerOffsetX = centerOffset.getValue();
			for (int i = 0; i < buttons.length; i++) {
				menuButtons[i].hoverUpdate(delta, mouseX, mouseY);

				// move button to center
				if (centerOffsetUpdated)
					menuButtons[i].setX((i % 2 == 0) ? center + centerOffsetX : center - centerOffsetX);
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
		 * Processes a key press action.
		 * @param container the game container
		 * @param game the game
		 * @param key the key code that was pressed (see {@link org.newdawn.slick.Input})
		 * @param c the character of the key that was pressed
		 */
		public void keyPress(GameContainer container, StateBasedGame game, int key, char c) {
			int index = Character.getNumericValue(c) - 1;
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
			float centerOffsetX = container.getWidth() * OFFSET_WIDTH_RATIO;
			centerOffset = new AnimatedValue(700, centerOffsetX, 0, AnimationEquation.OUT_BOUNCE);
			for (int i = 0; i < buttons.length; i++) {
				menuButtons[i].setX(center + ((i % 2 == 0) ? centerOffsetX : centerOffsetX * -1));
				menuButtons[i].resetHover();
			}

			// create title string list
			actualTitle = new ArrayList<String>();
			String[] title = getTitle(container, game);
			int maxLineWidth = (int) (container.getWidth() * 0.96f);
			for (int i = 0; i < title.length; i++) {
				// wrap text if too long
				if (Fonts.LARGE.getWidth(title[i]) > maxLineWidth) {
					List<String> list = Fonts.wrap(Fonts.LARGE, title[i], maxLineWidth, false);
					actualTitle.addAll(list);
				} else
					actualTitle.add(title[i]);
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
				game.enterState(Opsu.STATE_MAINMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		CLEAR_SCORES ("Clear local scores", Color.magenta) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).doStateActionOnLoad(MenuState.BEATMAP, node);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		FAVORITE_ADD ("Add to Favorites", Color.blue) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				node.getBeatmapSet().setFavorite(true);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		FAVORITE_REMOVE ("Remove from Favorites", Color.blue) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				node.getBeatmapSet().setFavorite(false);
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).doStateActionOnLoad(MenuState.BEATMAP_FAVORITE);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		DELETE ("Delete...", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				MenuState ms = (node.beatmapIndex == -1 || node.getBeatmapSet().size() == 1) ?
						MenuState.BEATMAP_DELETE_CONFIRM : MenuState.BEATMAP_DELETE_SELECT;
				((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(ms, node);
				game.enterState(Opsu.STATE_BUTTONMENU);
			}
		},
		CANCEL ("Cancel", Color.gray) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUBACK);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		DELETE_CONFIRM ("Yes, delete this beatmap!", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).doStateActionOnLoad(MenuState.BEATMAP_DELETE_CONFIRM, node);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
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
				BeatmapSetNode node = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getNode();
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).doStateActionOnLoad(MenuState.BEATMAP_DELETE_SELECT, node);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
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
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		RELOAD_CANCEL ("Cancel", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				CANCEL.click(container, game);
			}
		},
		DELETE_SCORE ("Delete score", Color.green) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				ScoreData scoreData = ((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).getScoreData();
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).doStateActionOnLoad(MenuState.SCORE, scoreData);
				game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		CLOSE ("Close", Color.gray) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				CANCEL.click(container, game);
			}
		},
		RESET_MODS ("Reset All Mods", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUCLICK);
				for (GameMod mod : GameMod.values()) {
					if (mod.isActive())
						mod.toggle(false);
				}
			}
		},
		ABOUT_WEBSITE ("Visit Website", Color.cyan) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				try {
					Desktop.getDesktop().browse(OpsuConstants.WEBSITE_URI);
				} catch (Exception e) {
					UI.getNotificationManager().sendNotification("The web page could not be opened.", Color.red);
				}
				game.enterState(Opsu.STATE_MAINMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		ABOUT_REPOSITORY ("Browse Source Code", Color.green) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				try {
					Desktop.getDesktop().browse(OpsuConstants.REPOSITORY_URI);
				} catch (Exception e) {
					UI.getNotificationManager().sendNotification("The web page could not be opened.", Color.red);
				}
				game.enterState(Opsu.STATE_MAINMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		ABOUT_REPORT ("Report an Issue", Color.red) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				StringBuilder sb = new StringBuilder();
				sb.append("[Type your description here. Feel free to delete the info below if it's not relevant.]\n\n");
				sb.append("---\n");
				sb.append(ErrorHandler.getEnvironmentInfoForIssue());
				URI uri = ErrorHandler.getIssueURI("", sb.toString());
				try {
					Desktop.getDesktop().browse(uri);
				} catch (Exception e) {
					UI.getNotificationManager().sendNotification("The web page could not be opened.", Color.red);
				}
				game.enterState(Opsu.STATE_MAINMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		ABOUT_ENV ("Copy Debug Info", Color.orange) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				Utils.copyToClipboard(ErrorHandler.getEnvironmentInfoForIssue());
				UI.getNotificationManager().sendNotification("Debug info copied to clipboard.");
				game.enterState(Opsu.STATE_MAINMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		ABOUT_CREDITS ("View Credits", Color.magenta) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				SoundController.playSound(SoundEffect.MENUHIT);
				try {
					Desktop.getDesktop().browse(OpsuConstants.CREDITS_URI);
				} catch (Exception e) {
					UI.getNotificationManager().sendNotification("The web page could not be opened.", Color.red);
				}
				game.enterState(Opsu.STATE_MAINMENU, new EmptyTransition(), new FadeInTransition());
			}
		},
		ABOUT_CLOSE ("Close", Color.gray) {
			@Override
			public void click(GameContainer container, StateBasedGame game) {
				NO.click(container, game);
			}
		};

		/** The text to show on the button. */
		private final String text;

		/** The button color. */
		private final Color color;

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
	private BeatmapSetNode node;

	/** The score data to process in the state. */
	private ScoreData scoreData;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private final int state;

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
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		UI.update(delta);
		MusicController.loopTrackIfEnded(false);
		if (menuState != null)
			menuState.update(container, delta, input.getMouseX(), input.getMouseY());
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		if (menuState != null)
			menuState.click(container, game, x, y);
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		UI.globalMouseWheelMoved(newValue, true);
	}

	@Override
	public void keyPressed(int key, char c) {
		if (UI.globalKeyPressed(key))
			return;

		switch (key) {
		case Input.KEY_ESCAPE:
			if (menuState != null)
				menuState.leave(container, game);
			break;
		default:
			if (menuState != null)
				menuState.keyPress(container, game, key, c);
			break;
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		if (menuState != null)
			menuState.enter(container, game);
	}

	/**
	 * Changes the menu state.
	 * @param menuState the new menu state
	 */
	public void setMenuState(MenuState menuState) { setMenuState(menuState, null, null); }

	/**
	 * Changes the menu state.
	 * @param menuState the new menu state
	 * @param node the song node to process in the state
	 */
	public void setMenuState(MenuState menuState, BeatmapSetNode node) { setMenuState(menuState, node, null); }

	/**
	 * Changes the menu state.
	 * @param menuState the new menu state
	 * @param scoreData the score scoreData
	 */
	public void setMenuState(MenuState menuState, ScoreData scoreData) { setMenuState(menuState, null, scoreData); }

	/**
	 * Changes the menu state.
	 * @param menuState the new menu state
	 * @param node the song node to process in the state
	 * @param scoreData the score scoreData
	 */
	private void setMenuState(MenuState menuState, BeatmapSetNode node, ScoreData scoreData) {
		this.menuState = menuState;
		this.node = node;
		this.scoreData = scoreData;
	}

	/**
	 * Returns the song node being processed, or null if none.
	 */
	private BeatmapSetNode getNode() { return node; }

	/**
	 * Returns the score data being processed, or null if none.
	 */
	private ScoreData getScoreData() { return scoreData; }
}
