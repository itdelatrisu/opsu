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
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Options.GameOption;
import itdelatrisu.opsu.UI;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;

import java.util.Arrays;
import java.util.Collections;

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
 * "Game Options" state.
 */
public class OptionsMenu extends BasicGameState {
	/** Option tabs. */
	private enum OptionTab {
		DISPLAY ("Display", new GameOption[] {
			GameOption.SCREEN_RESOLUTION,
//			GameOption.FULLSCREEN,
			GameOption.TARGET_FPS,
			GameOption.SHOW_FPS,
			GameOption.SHOW_UNICODE,
			GameOption.SCREENSHOT_FORMAT,
			GameOption.NEW_CURSOR,
			GameOption.DYNAMIC_BACKGROUND,
			GameOption.LOAD_VERBOSE
		}),
		MUSIC ("Music", new GameOption[] {
			GameOption.MASTER_VOLUME,
			GameOption.MUSIC_VOLUME,
			GameOption.EFFECT_VOLUME,
			GameOption.HITSOUND_VOLUME,
			GameOption.MUSIC_OFFSET,
			GameOption.DISABLE_SOUNDS,
			GameOption.ENABLE_THEME_SONG
		}),
		GAMEPLAY ("Gameplay", new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.FORCE_DEFAULT_PLAYFIELD,
			GameOption.IGNORE_BEATMAP_SKINS,
			GameOption.SHOW_HIT_LIGHTING,
			GameOption.SHOW_COMBO_BURSTS,
			GameOption.SHOW_PERFECT_HIT,
			GameOption.SHOW_HIT_ERROR_BAR
		}),
		INPUT ("Input", new GameOption[] {
			GameOption.KEY_LEFT,
			GameOption.KEY_RIGHT,
			GameOption.DISABLE_MOUSE_WHEEL,
			GameOption.DISABLE_MOUSE_BUTTONS
		}),
		CUSTOM ("Custom", new GameOption[] {
			GameOption.FIXED_CS,
			GameOption.FIXED_HP,
			GameOption.FIXED_AR,
			GameOption.FIXED_OD,
			GameOption.CHECKPOINT
		});

		/** Total number of tabs. */
		public static final int SIZE = values().length;

		/** Array of OptionTab objects in reverse order. */
		public static final OptionTab[] VALUES_REVERSED;
		static {
			VALUES_REVERSED = values();
			Collections.reverse(Arrays.asList(VALUES_REVERSED));
		}

		/** Enum values. */
		private static OptionTab[] values = values();

		/** Tab name. */
		private String name;

		/** Options array. */
		public GameOption[] options;

		/** Associated tab button. */
		public MenuButton button;

		/**
		 * Constructor.
		 * @param name the tab name
		 * @param options the options to display under the tab
		 */
		OptionTab(String name, GameOption[] options) {
			this.name = name;
			this.options = options;
		}

		/**
		 * Returns the tab name.
		 */
		public String getName() { return name; }

		/**
		 * Returns the next tab.
		 */
		public OptionTab next() { return values[(this.ordinal() + 1) % values.length]; }

		/**
		 * Returns the previous tab.
		 */
		public OptionTab prev() { return values[(this.ordinal() + (SIZE - 1)) % values.length]; }
	}

	/** Current tab. */
	private OptionTab currentTab;

	/** Max number of options displayed on one screen. */
	private int maxOptionsScreen = Math.max(
			Math.max(OptionTab.DISPLAY.options.length, OptionTab.MUSIC.options.length),
			Math.max(OptionTab.GAMEPLAY.options.length, OptionTab.CUSTOM.options.length));

	/** Key entry states. */
	private boolean keyEntryLeft = false, keyEntryRight = false;

	/** Game option coordinate modifiers (for drawing). */
	private int textY, offsetY;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private Graphics g;
	private int state;

	public OptionsMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		this.input = container.getInput();
		this.g = container.getGraphics();

		int width = container.getWidth();
		int height = container.getHeight();

		// option tabs
		Image tabImage = GameImage.MENU_TAB.getImage();
		float tabX = (width / 50) + (tabImage.getWidth() / 2f);
		float tabY = Utils.FONT_LARGE.getLineHeight() + Utils.FONT_DEFAULT.getLineHeight() +
				height * 0.03f + (tabImage.getHeight() / 2f);
		int tabOffset = Math.min(tabImage.getWidth(), width / OptionTab.SIZE);
		for (OptionTab tab : OptionTab.values())
			tab.button = new MenuButton(tabImage, tabX + (tab.ordinal() * tabOffset), tabY);

		// game option coordinate modifiers
		textY = (int) (tabY + tabImage.getHeight());
		int backHeight = GameImage.MENU_BACK.getAnimation(1).getHeight();
		offsetY = (height - textY - (backHeight * 4 / 5)) / maxOptionsScreen;
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Utils.COLOR_BLACK_ALPHA);

		int width = container.getWidth();
		int height = container.getHeight();
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();

		// title
		float c = container.getWidth() * 0.02f;
		Utils.FONT_LARGE.drawString(c, c, "Game Options", Color.white);
		Utils.FONT_DEFAULT.drawString(c, c + Utils.FONT_LARGE.getLineHeight() * 0.9f,
				"Click or drag an option to change it.", Color.white);

		// game options
		g.setLineWidth(1f);
		GameOption hoverOption = (keyEntryLeft)  ? GameOption.KEY_LEFT :
		                         (keyEntryRight) ? GameOption.KEY_RIGHT :
		                                           getOptionAt(mouseY);
		for (int i = 0; i < currentTab.options.length; i++) {
			GameOption option = currentTab.options[i];
			drawOption(option, i, hoverOption == option);
		}

		// option tabs
		OptionTab hoverTab = null;
		for (OptionTab tab : OptionTab.values()) {
			if (tab.button.contains(mouseX, mouseY)) {
				hoverTab = tab;
				break;
			}
		}
		for (OptionTab tab : OptionTab.VALUES_REVERSED) {
			if (tab != currentTab)
				UI.drawTab(tab.button.getX(), tab.button.getY(),
						tab.getName(), false, tab == hoverTab);
		}
		UI.drawTab(currentTab.button.getX(), currentTab.button.getY(),
				currentTab.getName(), true, false);
		g.setColor(Color.white);
		g.setLineWidth(2f);
		float lineY = OptionTab.DISPLAY.button.getY() + (GameImage.MENU_TAB.getImage().getHeight() / 2f);
		g.drawLine(0, lineY, width, lineY);
		g.resetLineWidth();

		UI.getBackButton().draw();

		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			g.setColor(Utils.COLOR_BLACK_ALPHA);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.white);
			Utils.FONT_LARGE.drawString(
					(width / 2) - (Utils.FONT_LARGE.getWidth("Please enter a letter or digit.") / 2),
					(height / 2) - Utils.FONT_LARGE.getLineHeight(), "Please enter a letter or digit."
			);
		}

		UI.draw(g);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		UI.update(delta);
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		UI.getBackButton().hoverUpdate(delta, mouseX, mouseY);
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mousePressed(int button, int x, int y) {
		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			keyEntryLeft = keyEntryRight = false;
			return;
		}

		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// back
		if (UI.getBackButton().contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			return;
		}

		// option tabs
		for (OptionTab tab : OptionTab.values()) {
			if (tab.button.contains(x, y)) {
				if (tab != currentTab) {
					currentTab = tab;
					SoundController.playSound(SoundEffect.MENUCLICK);
				}
				return;
			}
		}

		// options (click only)
		GameOption option = getOptionAt(y);
		if (option != GameOption.NULL)
			option.click(container);

		// special key entry states
		if (option == GameOption.KEY_LEFT) {
			keyEntryLeft = true;
			keyEntryRight = false;
		} else if (option == GameOption.KEY_RIGHT) {
			keyEntryLeft = false;
			keyEntryRight = true;
		}
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		// key entry state
		if (keyEntryLeft || keyEntryRight)
			return;

		// check mouse button (right click scrolls faster)
		int multiplier;
		if (input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON))
			multiplier = 4;
		else if (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON))
			multiplier = 1;
		else
			return;

		// get direction
		int diff = newx - oldx;
		if (diff == 0)
			return;
		diff = ((diff > 0) ? 1 : -1) * multiplier;

		// options (drag only)
		GameOption option = getOptionAt(oldy);
		if (option != GameOption.NULL)
			option.drag(container, diff);
	}

	@Override
	public void keyPressed(int key, char c) {
		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			if (Character.isLetterOrDigit(c)) {
				if (keyEntryLeft && Options.getGameKeyRight() != key)
					Options.setGameKeyLeft(key);
				else if (keyEntryRight && Options.getGameKeyLeft() != key)
					Options.setGameKeyRight(key);
			}
			keyEntryLeft = keyEntryRight = false;
			return;
		}

		switch (key) {
		case Input.KEY_ESCAPE:
			SoundController.playSound(SoundEffect.MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			break;
		case Input.KEY_F5:
			// restart application
			if ((input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) &&
				(input.isKeyDown(Input.KEY_RSHIFT) || input.isKeyDown(Input.KEY_LSHIFT))) {
				container.setForceExit(false);
				container.exit();
			}
			break;
		case Input.KEY_F7:
			Options.setNextFPS(container);
			break;
		case Input.KEY_F10:
			Options.toggleMouseDisabled();
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		case Input.KEY_TAB:
			// change tabs
			if (input.isKeyDown(Input.KEY_LSHIFT) || input.isKeyDown(Input.KEY_RSHIFT))
				currentTab = currentTab.prev();
			else
				currentTab = currentTab.next();
			SoundController.playSound(SoundEffect.MENUCLICK);
			break;
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		currentTab = OptionTab.DISPLAY;
	}

	/**
	 * Draws a game option.
	 * @param option the option
	 * @param pos the position to draw at
	 * @param focus whether the option is currently focused
	 */
	private void drawOption(GameOption option, int pos, boolean focus) {
		int width = container.getWidth();
		int textHeight = Utils.FONT_LARGE.getLineHeight();
		float y = textY + (pos * offsetY);
		Color color = (focus) ? Color.cyan : Color.white;

		Utils.FONT_LARGE.drawString(width / 30, y, option.getName(), color);
		Utils.FONT_LARGE.drawString(width / 2, y, option.getValueString(), color);
		Utils.FONT_SMALL.drawString(width / 30, y + textHeight, option.getDescription(), color);
		g.setColor(Utils.COLOR_WHITE_ALPHA);
		g.drawLine(0, y + textHeight, width, y + textHeight);
	}

	/**
	 * Returns the option at the given y coordinate.
	 * @param y the y coordinate
	 * @return the option, or GameOption.NULL if no such option exists
	 */
	private GameOption getOptionAt(int y) {
		GameOption option = GameOption.NULL;

		if (y < textY || y > textY + (offsetY * maxOptionsScreen))
			return option;

		int index = (y - textY + Utils.FONT_LARGE.getLineHeight()) / offsetY;
		if (index < currentTab.options.length)
			option = currentTab.options[index];
		return option;
	}
}
