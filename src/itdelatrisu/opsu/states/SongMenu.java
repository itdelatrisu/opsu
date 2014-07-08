/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014 Jeffrey Han
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

import itdelatrisu.opsu.GUIMenuButton;
import itdelatrisu.opsu.MusicController;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuGroupList;
import itdelatrisu.opsu.OsuGroupNode;
import itdelatrisu.opsu.OsuParser;
import itdelatrisu.opsu.SoundController;
import itdelatrisu.opsu.Utils;

import org.lwjgl.opengl.Display;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.gui.TextField;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.EmptyTransition;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

/**
 * "Song Selection" state.
 * <ul>
 * <li>[Song] - start game (move to game state)
 * <li>[Back] - return to main menu
 * </ul>
 */
public class SongMenu extends BasicGameState {
	/**
	 * The number of buttons to be shown on each screen.
	 */
	private static final int MAX_BUTTONS = 6;

	/**
	 * Delay time, in milliseconds, between each search.
	 */
	private static final int SEARCH_DELAY = 300;

	/**
	 * Current start node (topmost menu entry).
	 */
	private OsuGroupNode startNode;

	/**
	 * Current focused (selected) node.
	 */
	private OsuGroupNode focusNode;

	/**
	 * Button coordinate values.
	 */
	private float
		buttonX, buttonY, buttonOffset,
		buttonWidth, buttonHeight;

	/**
	 * Sorting tab buttons (indexed by SORT_* constants).
	 */
	private GUIMenuButton[] sortTabs;

	/**
	 * The current sort order (SORT_* constant).
	 */
	private byte currentSort = OsuGroupList.SORT_TITLE;

	/**
	 * The options button (to enter the "Game Options" menu).
	 */
	private GUIMenuButton optionsButton;

	/**
	 * The search textfield.
	 */
	private TextField search;

	/**
	 * Delay timer, in milliseconds, before running another search.
	 * This is overridden by character entry (reset) and 'esc' (immediate search).
	 */
	private int searchTimer;

	/**
	 * Information text to display based on the search query.
	 */
	private String searchResultString;

	/**
	 * Search icon.
	 */
	private Image searchIcon;

	/**
	 * Music note icon.
	 */
	private Image musicNote;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private int state;

	public SongMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		this.input = container.getInput();

		int width = container.getWidth();
		int height = container.getHeight();

		// song button background & graphics context
		Image menuBackground = new Image("menu-button-background.png").getScaledCopy(width / 2, height / 6);
		OsuGroupNode.setBackground(menuBackground);

		// song button coordinates
		buttonX = width * 0.6f;
		buttonY = height * 0.16f;
		buttonWidth = menuBackground.getWidth();
		buttonHeight = menuBackground.getHeight();
		buttonOffset = (height * 0.8f) / MAX_BUTTONS;

		// sorting tabs
		sortTabs = new GUIMenuButton[OsuGroupList.SORT_MAX];
		Image tab = Utils.getTabImage();
		float tabX = buttonX + (tab.getWidth() / 2f);
		float tabY = (height * 0.15f) - (tab.getHeight() / 2f) - 2f;
		float tabOffset = (width - buttonX) / sortTabs.length;
		for (int i = 0; i < sortTabs.length; i++)
			sortTabs[i] = new GUIMenuButton(tab, tabX + (i * tabOffset), tabY);

		// search
		searchTimer = 0;
		searchResultString = "Type to search!";

		searchIcon = new Image("search.png");
		float iconScale = Utils.FONT_BOLD.getLineHeight() * 2f / searchIcon.getHeight();
		searchIcon = searchIcon.getScaledCopy(iconScale);

		search = new TextField(
				container, Utils.FONT_DEFAULT,
				(int) tabX + searchIcon.getWidth(), (int) ((height * 0.15f) - (tab.getHeight() * 5 / 2f)),
				(int) (buttonWidth / 2), Utils.FONT_DEFAULT.getHeight()
		);
		search.setBackgroundColor(Color.transparent);
		search.setBorderColor(Color.transparent);
		search.setTextColor(Color.white);
		search.setConsumeEvents(false);
		search.setMaxLength(60);

		// options button
		Image optionsIcon = new Image("options.png").getScaledCopy(iconScale);
		optionsButton = new GUIMenuButton(optionsIcon, search.getX() - (optionsIcon.getWidth() * 1.5f), search.getY());

		int musicNoteDim = (int) (Utils.FONT_LARGE.getHeight() * 0.75f + Utils.FONT_DEFAULT.getHeight());
		musicNote = new Image("music-note.png").getScaledCopy(musicNoteDim, musicNoteDim);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Color.black);

		int width = container.getWidth();
		int height = container.getHeight();

		// background
		if (focusNode != null)
			focusNode.osuFiles.get(focusNode.osuFileIndex).drawBG(width, height, 1.0f);

		// header setup
		float lowerBound = height * 0.15f;
		g.setColor(Utils.COLOR_BLACK_ALPHA);
		g.fillRect(0, 0, width, lowerBound);
		g.setColor(Utils.COLOR_BLUE_DIVIDER);
		g.setLineWidth(2f);
		g.drawLine(0, lowerBound, width, lowerBound);
		g.resetLineWidth();

		// header
		if (focusNode != null) {
			musicNote.draw();
			int musicNoteWidth = musicNote.getWidth();
			int musicNoteHeight = musicNote.getHeight();

			String[] info = focusNode.getInfo();
			g.setColor(Color.white);
			Utils.FONT_LARGE.drawString(musicNoteWidth + 5, -3, info[0]);
			Utils.FONT_DEFAULT.drawString(
					musicNoteWidth + 5, -3 + Utils.FONT_LARGE.getHeight() * 0.75f, info[1]);
			int headerY = musicNoteHeight - 3;
			Utils.FONT_BOLD.drawString(5, headerY, info[2]);
			headerY += Utils.FONT_BOLD.getLineHeight() - 6;
			Utils.FONT_DEFAULT.drawString(5, headerY, info[3]);
			headerY += Utils.FONT_DEFAULT.getLineHeight() - 4;
			Utils.FONT_SMALL.drawString(5, headerY, info[4]);
		}

		// song buttons
		OsuGroupNode node = startNode;
		for (int i = 0; i < MAX_BUTTONS && node != null; i++) {
			node.draw(buttonX, buttonY + (i*buttonOffset), (node == focusNode));
			node = node.next;
		}

		// options button
		optionsButton.draw();

		// sorting tabs
		float tabTextY = sortTabs[0].getY() - (sortTabs[0].getImage().getHeight() / 2f);
		for (int i = sortTabs.length - 1; i >= 0; i--) {
			sortTabs[i].getImage().setAlpha((i == currentSort) ? 1.0f : 0.7f);
			sortTabs[i].draw();
			float tabTextX = sortTabs[i].getX() - (Utils.FONT_MEDIUM.getWidth(OsuGroupList.SORT_NAMES[i]) / 2);
			Utils.FONT_MEDIUM.drawString(tabTextX, tabTextY, OsuGroupList.SORT_NAMES[i], Color.white);
		}

		// search
		Utils.FONT_BOLD.drawString(
				search.getX(), search.getY() - Utils.FONT_BOLD.getLineHeight(),
				searchResultString, Color.white
		);
		searchIcon.draw(search.getX() - searchIcon.getWidth(),
						search.getY() - Utils.FONT_DEFAULT.getLineHeight());
		g.setColor(Color.white);
		search.render(container, g);

		// scroll bar
		if (focusNode != null) {
			float scrollStartY = height * 0.16f;
			float scrollEndY = height * 0.82f;
			g.setColor(Utils.COLOR_BLACK_ALPHA);
			g.fillRoundRect(width - 10, scrollStartY, 5, scrollEndY, 4);
			g.setColor(Color.white);
			g.fillRoundRect(width - 10, scrollStartY + (scrollEndY * startNode.index / Opsu.groups.size()), 5, 20, 4);
		}

		// back button
		Utils.getBackButton().draw();

		Utils.drawFPS();
		Utils.drawCursor();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		Utils.updateCursor(delta);

		// search
		searchTimer += delta;
		if (searchTimer >= SEARCH_DELAY) {
			searchTimer = 0;

			// store the start/focus nodes
			OsuGroupNode oldFocusNode = null;
			int oldFileIndex = -1;
			if (focusNode != null) {
				oldFocusNode = Opsu.groups.getBaseNode(focusNode.index);
				oldFileIndex = focusNode.osuFileIndex;
			}

			if (Opsu.groups.search(search.getText())) {
				// empty search
				if (search.getText().isEmpty())
					searchResultString = "Type to search!";

				// search produced new list: re-initialize it
				startNode = focusNode = null;
				if (Opsu.groups.size() > 0) {
					Opsu.groups.init(currentSort);
					if (search.getText().isEmpty()) {  // cleared search
						// use previous start/focus if possible
						if (oldFocusNode != null)
							setFocus(oldFocusNode, oldFileIndex + 1, true);
						else
							setFocus(Opsu.groups.getRandomNode(), -1, true);
					} else {
						searchResultString = String.format("%d matches found!", Opsu.groups.size());
						setFocus(Opsu.groups.getRandomNode(), -1, true);
					}
				} else if (!search.getText().isEmpty())
					searchResultString = "No matches found. Hit 'esc' to reset.";
			}
		}

		// slide buttons
		int height = container.getHeight();
		float targetY = height * 0.16f;
		if (buttonY > targetY) {
			buttonY -= height * delta / 20000f;
			if (buttonY < targetY)
				buttonY = targetY;
		} else if (buttonY < targetY) {
			buttonY += height * delta / 20000f;
			if (buttonY > targetY)
				buttonY = targetY;
		}
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button 
		if (button != Input.MOUSE_LEFT_BUTTON)
			return;

		// back
		if (Utils.getBackButton().contains(x, y)) {
			SoundController.playSound(SoundController.SOUND_MENUBACK);
			game.enterState(Opsu.STATE_MAINMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			return;
		}

		// options
		if (optionsButton.contains(x, y)) {
			SoundController.playSound(SoundController.SOUND_MENUHIT);
			game.enterState(Opsu.STATE_OPTIONS, new EmptyTransition(), new FadeInTransition(Color.black));
			return;
		}

		if (focusNode == null)
			return;

		// sorting buttons
		for (byte i = 0; i < sortTabs.length; i++) {
			if (sortTabs[i].contains(x, y)) {
				if (i != currentSort) {
					currentSort = i;
					SoundController.playSound(SoundController.SOUND_MENUCLICK);
					OsuGroupNode oldFocusBase = Opsu.groups.getBaseNode(focusNode.index);
					int oldFocusFileIndex = focusNode.osuFileIndex;
					focusNode = null;
					Opsu.groups.init(i);
					setFocus(oldFocusBase, oldFocusFileIndex + 1, true);
				}
				return;
			}
		}

		for (int i = 0; i < MAX_BUTTONS; i++) {
			if ((x > buttonX && x < buttonX + buttonWidth) &&
				(y > buttonY + (i*buttonOffset) && y < buttonY + (i*buttonOffset) + buttonHeight)) {
				OsuGroupNode node = Opsu.groups.getNode(startNode, i);
				if (node == null)  // out of bounds
					break;

				int expandedIndex = Opsu.groups.getExpandedIndex();

				// clicked node is already expanded
				if (node.index == expandedIndex) {
					if (node.osuFileIndex == -1) {
						// check bounds
						int max = Math.max(Opsu.groups.size() - MAX_BUTTONS, 0);
						if (startNode.index > max)
							startNode = Opsu.groups.getBaseNode(max);

						// if group button clicked, undo expansion
						Opsu.groups.expand(node.index);

					} else if (node.osuFileIndex == focusNode.osuFileIndex) {
						// if already focused, load the beatmap
						startGame();

					} else {
						// focus the node
						setFocus(node, 0, false);
					}
					break;
				}

				// if current start node is expanded,
				// set it to the base node before undoing the expansion
				if (startNode.index == expandedIndex) {
					int max = Math.max(Opsu.groups.size() - MAX_BUTTONS, 0);
					if (startNode.index > max)  // check bounds
						startNode = Opsu.groups.getBaseNode(max);
					else
						startNode = Opsu.groups.getBaseNode(startNode.index);
				}
				setFocus(node, -1, false);

				break;
			}
		}
	}

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
			if (!search.getText().isEmpty()) {
				search.setText("");
				searchTimer = SEARCH_DELAY;
			} else {
				SoundController.playSound(SoundController.SOUND_MENUBACK);
				game.enterState(Opsu.STATE_MAINMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			}
			break;
		case Input.KEY_F1:
			game.enterState(Opsu.STATE_OPTIONS, new EmptyTransition(), new FadeInTransition(Color.black));
			break;
		case Input.KEY_F2:
			setFocus(Opsu.groups.getRandomNode(), -1, true);
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		case Input.KEY_ENTER:
			if (focusNode != null)
				startGame();
			break;
		case Input.KEY_DOWN:
			changeIndex(1);
			break;
		case Input.KEY_UP:
			changeIndex(-1);
			break;
		case Input.KEY_RIGHT:
			if (focusNode == null)
				break;
			OsuGroupNode next = focusNode.next;
			if (next != null) {
				setFocus(next, (next.index == focusNode.index) ? 0 : 1, false);
				changeIndex(1);
			}
			break;
		case Input.KEY_LEFT:
			if (focusNode == null)
				break;
			OsuGroupNode prev = focusNode.prev;
			if (prev != null) {
				if (prev.index == focusNode.index && prev.osuFileIndex < 0) {
					// skip the group node
					prev = prev.prev;
					if (prev == null)  // this is the first node
						break;
					setFocus(prev, prev.osuFiles.size(), true);

					// move the start node forward if off the screen
					int size = prev.osuFiles.size();
					while (size-- >= MAX_BUTTONS)
						startNode = startNode.next;
				} else {
					setFocus(prev, 0, false);
					changeIndex(-1);
				}
			}
			break;
		case Input.KEY_NEXT:
			changeIndex(MAX_BUTTONS);
			break;
		case Input.KEY_PRIOR:
			changeIndex(-MAX_BUTTONS);
			break;
		default:
			// wait for user to finish typing
			if (Character.isLetterOrDigit(c) || key == Input.KEY_BACK)
				searchTimer = 0;
			break;
		}
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		// check mouse button (right click scrolls faster)
		int multiplier;
		if (input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON))
			multiplier = 4;
		else if (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON))
			multiplier = 1;
		else
			return;

		int diff = newy - oldy;
		if (diff != 0) {
			diff = ((diff < 0) ? 1 : -1) * multiplier;
			changeIndex(diff);
		}
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		changeIndex((newValue < 0) ? 1 : -1);
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		Display.setTitle(game.getTitle());
		search.setFocus(true);
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
		search.setFocus(false);
	}

	/**
	 * Shifts the startNode forward (+) or backwards (-) by a given number of nodes.
	 * Initiates sliding "animation" by shifting the button Y position.
	 */
	private void changeIndex(int shift) {
		while (shift != 0) {
			if (startNode == null)
				break;
	
			int height = container.getHeight();
			if (shift < 0 && startNode.prev != null) {
				startNode = startNode.prev;
				buttonY += buttonOffset / 4;
				if (buttonY > height * 0.18f)
					buttonY = height * 0.18f;
				shift++;
			} else if (shift > 0 && startNode.next != null &&
					   Opsu.groups.getNode(startNode, MAX_BUTTONS) != null) {
				startNode = startNode.next;
				buttonY -= buttonOffset / 4;
				if (buttonY < height * 0.14f)
					buttonY = height * 0.14f;
				shift--;
			} else
				break;
		}
		return;
	}

	/**
	 * Sets a new focus node.
	 * @param node the base node; it will be expanded if it isn't already
	 * @param pos the OsuFile element to focus; if out of bounds, it will be randomly chosen
	 * @param flag if true, startNode will be set to the song group node
	 * @return the old focus node
	 */
	public OsuGroupNode setFocus(OsuGroupNode node, int pos, boolean flag) {
		if (node == null)
			return null;
		
		OsuGroupNode oldFocus = focusNode;

		// expand node before focusing it
		if (node.index != Opsu.groups.getExpandedIndex())
			Opsu.groups.expand(node.index);

		// check pos bounds
		int length = node.osuFiles.size();
		if (pos < 0 || pos > length)  // set a random pos
			pos = (int) (Math.random() * length) + 1;

		if (flag)
			startNode = node;
		focusNode = Opsu.groups.getNode(node, pos);
		MusicController.play(focusNode.osuFiles.get(focusNode.osuFileIndex), true);

		// check startNode bounds
		if (focusNode.index - startNode.index == MAX_BUTTONS - 1)
			changeIndex(1);
		while (startNode.index >= Opsu.groups.size() + length + 1 - MAX_BUTTONS &&
				startNode.prev != null)
			changeIndex(-1);

		return oldFocus;
	}

	/**
	 * Starts the game.
	 * @param osu the OsuFile to send to the game
	 */
	private void startGame() {
		if (MusicController.isConverting())
			return;

		SoundController.playSound(SoundController.SOUND_MENUHIT);
		OsuFile osu = MusicController.getOsuFile();
		Display.setTitle(String.format("%s - %s", game.getTitle(), osu.toString()));
		OsuParser.parseHitObjects(osu);
		SoundController.setSampleSet(osu.sampleSet);
		Game.setRestart(Game.RESTART_NEW);
		game.enterState(Opsu.STATE_GAME, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
	}
}
