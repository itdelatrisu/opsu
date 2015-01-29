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

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameData.Grade;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.MenuButton;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuGroupList;
import itdelatrisu.opsu.OsuGroupNode;
import itdelatrisu.opsu.OsuParser;
import itdelatrisu.opsu.OszUnpacker;
import itdelatrisu.opsu.ScoreData;
import itdelatrisu.opsu.ScoreDB;
import itdelatrisu.opsu.SongSort;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.HitSound;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;

import java.io.File;
import java.util.Map;
import java.util.Stack;

import org.lwjgl.opengl.Display;
import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;
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
	/** The max number of song buttons to be shown on each screen. */
	private static final int MAX_SONG_BUTTONS = 6;

	/** The max number of score buttons to be shown at a time. */
	public static final int MAX_SCORE_BUTTONS = 7;

	/** Delay time, in milliseconds, between each search. */
	private static final int SEARCH_DELAY = 500;

	/** Maximum x offset of song buttons for mouse hover, in pixels. */
	private static final float MAX_HOVER_OFFSET = 30f;

	/** Song node class representing an OsuGroupNode and file index. */
	private static class SongNode {
		/** Song node. */
		private OsuGroupNode node;

		/** File index. */
		private int index;

		/**
		 * Constructor.
		 * @param node the OsuGroupNode
		 * @param index the file index
		 */
		public SongNode(OsuGroupNode node, int index) {
			this.node = node;
			this.index = index;
		}

		/**
		 * Returns the associated OsuGroupNode.
		 */
		public OsuGroupNode getNode() { return node; }

		/**
		 * Returns the associated file index.
		 */
		public int getIndex() { return index; }
	}

	/** Current start node (topmost menu entry). */
	private OsuGroupNode startNode;

	/** Current focused (selected) node. */
	private OsuGroupNode focusNode;

	/** The base node of the previous focus node. */
	private SongNode oldFocusNode = null;

	/** Stack of previous "random" (F2) focus nodes. */
	private Stack<SongNode> randomStack = new Stack<SongNode>();

	/** Current focus node's song information. */
	private String[] songInfo;

	/** Button coordinate values. */
	private float
		buttonX, buttonY, buttonOffset,
		buttonWidth, buttonHeight;

	/** Current x offset of song buttons for mouse hover, in pixels. */
	private float hoverOffset = 0f;

	/** Current index of hovered song button. */
	private int hoverIndex = -1;

	/** The options button (to enter the "Game Options" menu). */
	private MenuButton optionsButton;

	/** The search textfield. */
	private TextField search;

	/**
	 * Delay timer, in milliseconds, before running another search.
	 * This is overridden by character entry (reset) and 'esc' (immediate search).
	 */
	private int searchTimer;

	/** Information text to display based on the search query. */
	private String searchResultString;

	/** Loader animation. */
	private Animation loader;

	/** Whether or not to reset game data upon entering the state. */
	private boolean resetGame = false;

	/** Whether or not to reset music track upon entering the state. */
	private boolean resetTrack = false;

	/** Beatmap reloading thread. */
	private Thread reloadThread;

	/** Current map of scores (Version, ScoreData[]). */
	private Map<String, ScoreData[]> scoreMap;

	/** Scores for the current focus node. */
	private ScoreData[] focusScores;

	/** Current start score (topmost score entry). */
	private int startScore = 0;

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
		Image menuBackground = GameImage.MENU_BUTTON_BG.getImage();

		// song button coordinates
		buttonX = width * 0.6f;
		buttonY = height * 0.16f;
		buttonWidth = menuBackground.getWidth();
		buttonHeight = menuBackground.getHeight();
		buttonOffset = (height * 0.8f) / MAX_SONG_BUTTONS;

		// search
		searchTimer = 0;
		searchResultString = "Type to search!";
		Image searchIcon = GameImage.MENU_SEARCH.getImage();
		Image tab = GameImage.MENU_TAB.getImage();
		search = new TextField(
				container, Utils.FONT_DEFAULT,
				(int) buttonX + (tab.getWidth() / 2) + searchIcon.getWidth(),
				(int) ((height * 0.15f) - (tab.getHeight() * 2.5f)),
				(int) (buttonWidth / 2), Utils.FONT_DEFAULT.getLineHeight()
		);
		search.setBackgroundColor(Color.transparent);
		search.setBorderColor(Color.transparent);
		search.setTextColor(Color.white);
		search.setConsumeEvents(false);
		search.setMaxLength(60);

		// options button
		Image optionsIcon = GameImage.MENU_OPTIONS.getImage();
		optionsButton = new MenuButton(optionsIcon, search.getX() - (optionsIcon.getWidth() * 1.5f), search.getY());
		optionsButton.setHoverScale(1.75f);

		// loader
		int loaderDim = GameImage.MENU_MUSICNOTE.getImage().getWidth();
		SpriteSheet spr = new SpriteSheet(GameImage.MENU_LOADER.getImage(), loaderDim, loaderDim);
		loader = new Animation(spr, 50);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Color.black);

		int width = container.getWidth();
		int height = container.getHeight();
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();

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
			float marginX = width * 0.005f, marginY = height * 0.005f;

			Image musicNote = GameImage.MENU_MUSICNOTE.getImage();
			if (MusicController.isTrackLoading())
				loader.draw(marginX, marginY);
			else
				musicNote.draw(marginX, marginY);
			int iconWidth = musicNote.getWidth();
			int iconHeight = musicNote.getHeight();

			if (songInfo == null)
				songInfo = focusNode.getInfo();
			marginX += 5;
			Utils.FONT_LARGE.drawString(marginX + iconWidth, marginY, songInfo[0], Color.white);
			Utils.FONT_DEFAULT.drawString(marginX + iconWidth, marginY + Utils.FONT_LARGE.getLineHeight() * 0.75f, songInfo[1], Color.white);
			float headerY = marginY + iconHeight;
			Utils.FONT_BOLD.drawString(marginX, headerY, songInfo[2], Color.white);
			headerY += Utils.FONT_BOLD.getLineHeight() - 6;
			Utils.FONT_DEFAULT.drawString(marginX, headerY, songInfo[3], Color.white);
			headerY += Utils.FONT_DEFAULT.getLineHeight() - 4;
			Utils.FONT_SMALL.drawString(marginX, headerY, songInfo[4], Color.white);
		}

		// song buttons
		OsuGroupNode node = startNode;
		for (int i = 0; i < MAX_SONG_BUTTONS && node != null; i++, node = node.next) {
			// draw the node
			float offset = (i == hoverIndex) ? hoverOffset : 0f;
			ScoreData[] scores = getScoreDataForNode(node, false);
			node.draw(
					buttonX - offset, buttonY + (i*buttonOffset),
					(scores == null) ? Grade.NULL : scores[0].getGrade(),
					(node == focusNode)
			);

			// load glyphs
			Utils.loadGlyphs(node.osuFiles.get(0));
		}

		// score buttons
		if (focusScores != null) {
			for (int i = 0; i < MAX_SCORE_BUTTONS; i++) {
				int rank = startScore + i;
				if (rank >= focusScores.length)
					break;
				long prevScore = (rank + 1 < focusScores.length) ?
						focusScores[rank + 1].score : -1;
				focusScores[rank].draw(g, i, rank, prevScore, ScoreData.buttonContains(mouseX, mouseY, i));
			}

			// scroll bar
			if (focusScores.length > MAX_SCORE_BUTTONS && ScoreData.areaContains(mouseX, mouseY))
				ScoreData.drawScrollbar(g, startScore, focusScores.length);
		}

		// options button
		optionsButton.draw();

		// sorting tabs
		SongSort currentSort = SongSort.getSort();
		SongSort hoverSort = null;
		for (SongSort sort : SongSort.values()) {
			if (sort.contains(mouseX, mouseY)) {
				hoverSort = sort;
				break;
			}
		}
		for (SongSort sort : SongSort.VALUES_REVERSED) {
			if (sort != currentSort)
				sort.draw(false, sort == hoverSort);
		}
		currentSort.draw(true, false);

		// search
		Image searchIcon = GameImage.MENU_SEARCH.getImage();
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
			g.fillRoundRect(width - 10, scrollStartY + (scrollEndY * startNode.index / OsuGroupList.get().size()), 5, 20, 4);
		}

		// reloading beatmaps
		if (reloadThread != null) {
			// darken the screen
			g.setColor(Utils.COLOR_BLACK_ALPHA);
			g.fillRect(0, 0, width, height);

			Utils.drawLoadingProgress(g);
		}

		// back button
		else
			Utils.getBackButton().draw();

		Utils.drawVolume(g);
		Utils.drawFPS();
		Utils.drawCursor();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		Utils.updateCursor(delta);
		Utils.updateVolumeDisplay(delta);
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		Utils.getBackButton().hoverUpdate(delta, mouseX, mouseY);
		optionsButton.hoverUpdate(delta, mouseX, mouseY);

		// search
		search.setFocus(true);
		searchTimer += delta;
		if (searchTimer >= SEARCH_DELAY && reloadThread == null) {
			searchTimer = 0;

			// store the start/focus nodes
			if (focusNode != null)
				oldFocusNode = new SongNode(OsuGroupList.get().getBaseNode(focusNode.index), focusNode.osuFileIndex);

			if (OsuGroupList.get().search(search.getText())) {
				// reset song stack
				randomStack = new Stack<SongNode>();

				// empty search
				if (search.getText().isEmpty())
					searchResultString = "Type to search!";

				// search produced new list: re-initialize it
				startNode = focusNode = null;
				if (OsuGroupList.get().size() > 0) {
					OsuGroupList.get().init();
					if (search.getText().isEmpty()) {  // cleared search
						// use previous start/focus if possible
						if (oldFocusNode != null)
							setFocus(oldFocusNode.getNode(), oldFocusNode.getIndex(), true);
						else
							setFocus(OsuGroupList.get().getRandomNode(), -1, true);
					} else {
						int size = OsuGroupList.get().size();
						searchResultString = String.format("%d match%s found!",
								size, (size == 1) ? "" : "es");
						setFocus(OsuGroupList.get().getRandomNode(), -1, true);
					}
					oldFocusNode = null;
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

		// mouse hover
		OsuGroupNode node = startNode;
		boolean isHover = false;
		for (int i = 0; i < MAX_SONG_BUTTONS && node != null; i++, node = node.next) {
			float cx = (node.index == OsuGroupList.get().getExpandedIndex()) ? buttonX * 0.9f : buttonX;
			if ((mouseX > cx && mouseX < cx + buttonWidth) &&
				(mouseY > buttonY + (i * buttonOffset) && mouseY < buttonY + (i * buttonOffset) + buttonHeight)) {
				if (i == hoverIndex) {
					if (hoverOffset < MAX_HOVER_OFFSET) {
						hoverOffset += delta / 3f;
						if (hoverOffset > MAX_HOVER_OFFSET)
							hoverOffset = MAX_HOVER_OFFSET;
					}
				} else {
					hoverIndex = i;
					hoverOffset = 0f;
				}
				isHover = true;
				break;
			}
		}
		if (!isHover) {
			hoverOffset = 0f;
			hoverIndex = -1;
		}
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button
		if (button != Input.MOUSE_LEFT_BUTTON)
			return;

		// block input during beatmap reloading
		if (reloadThread != null)
			return;

		// back
		if (Utils.getBackButton().contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			((MainMenu) game.getState(Opsu.STATE_MAINMENU)).reset();
			game.enterState(Opsu.STATE_MAINMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			return;
		}

		// options
		if (optionsButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			game.enterState(Opsu.STATE_OPTIONSMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			return;
		}

		if (focusNode == null)
			return;

		// sorting buttons
		for (SongSort sort : SongSort.values()) {
			if (sort.contains(x, y)) {
				if (sort != SongSort.getSort()) {
					SongSort.setSort(sort);
					SoundController.playSound(SoundEffect.MENUCLICK);
					OsuGroupNode oldFocusBase = OsuGroupList.get().getBaseNode(focusNode.index);
					int oldFocusFileIndex = focusNode.osuFileIndex;
					focusNode = null;
					OsuGroupList.get().init();
					setFocus(oldFocusBase, oldFocusFileIndex, true);
				}
				return;
			}
		}

		// song buttons
		int expandedIndex = OsuGroupList.get().getExpandedIndex();
		OsuGroupNode node = startNode;
		for (int i = 0; i < MAX_SONG_BUTTONS && node != null; i++, node = node.next) {
			// is button at this index clicked?
			float cx = (node.index == expandedIndex) ? buttonX * 0.9f : buttonX;
			if ((x > cx && x < cx + buttonWidth) &&
				(y > buttonY + (i * buttonOffset) && y < buttonY + (i * buttonOffset) + buttonHeight)) {
				float oldHoverOffset = hoverOffset;
				int oldHoverIndex = hoverIndex;

				// clicked node is already expanded
				if (node.index == expandedIndex) {
					if (node.osuFileIndex == focusNode.osuFileIndex) {
						// if already focused, load the beatmap
						startGame();

					} else {
						// focus the node
						SoundController.playSound(SoundEffect.MENUCLICK);
						setFocus(node, 0, false);
					}
				}

				// clicked node is a new group
				else {
					SoundController.playSound(SoundEffect.MENUCLICK);
					setFocus(node, -1, false);
				}

				// restore hover data
				hoverOffset = oldHoverOffset;
				hoverIndex = oldHoverIndex;

				return;
			}
		}

		// score buttons
		if (focusScores != null && ScoreData.areaContains(x, y)) {
			for (int i = 0; i < MAX_SCORE_BUTTONS; i++) {
				int rank = startScore + i;
				if (rank >= focusScores.length)
					break;
				if (ScoreData.buttonContains(x, y, i)) {
					// view score
					GameData data = new GameData(focusScores[rank], container.getWidth(), container.getHeight());
					((GameRanking) game.getState(Opsu.STATE_GAMERANKING)).setGameData(data);
					game.enterState(Opsu.STATE_GAMERANKING, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
					return;
				}
			}
		}
	}

	@Override
	public void keyPressed(int key, char c) {
		// block input during beatmap reloading
		if (reloadThread != null && !(key == Input.KEY_ESCAPE || key == Input.KEY_F12))
			return;

		switch (key) {
		case Input.KEY_ESCAPE:
			if (reloadThread != null) {
				// beatmap reloading: stop parsing OsuFiles by sending interrupt to OsuParser
				if (reloadThread != null)
					reloadThread.interrupt();
			} else if (!search.getText().isEmpty()) {
				// clear search text
				search.setText("");
				searchTimer = SEARCH_DELAY;
			} else {
				// return to main menu
				SoundController.playSound(SoundEffect.MENUBACK);
				((MainMenu) game.getState(Opsu.STATE_MAINMENU)).reset();
				game.enterState(Opsu.STATE_MAINMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			}
			break;
		case Input.KEY_F1:
			SoundController.playSound(SoundEffect.MENUHIT);
			game.enterState(Opsu.STATE_OPTIONSMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			break;
		case Input.KEY_F2:
			if (focusNode == null)
				break;
			if (input.isKeyDown(Input.KEY_RSHIFT) || input.isKeyDown(Input.KEY_LSHIFT)) {
				// shift key: previous random track
				SongNode prev;
				if (randomStack.isEmpty() || (prev = randomStack.pop()) == null)
					break;
				setFocus(prev.getNode(), prev.getIndex(), true);
			} else {
				// random track, add previous to stack
				randomStack.push(new SongNode(OsuGroupList.get().getBaseNode(focusNode.index), focusNode.osuFileIndex));
				setFocus(OsuGroupList.get().getRandomNode(), -1, true);
			}
			break;
		case Input.KEY_F5:
			// TODO: osu! has a confirmation menu
			SoundController.playSound(SoundEffect.MENUCLICK);

			// reset state and node references
			MusicController.reset();
			startNode = focusNode = null;
			oldFocusNode = null;
			randomStack = new Stack<SongNode>();
			songInfo = null;
			hoverOffset = 0f;
			hoverIndex = -1;
			search.setText("");
			searchTimer = SEARCH_DELAY;
			searchResultString = "Type to search!";

			// reload songs in new thread
			reloadThread = new Thread() {
				@Override
				public void run() {
					// invoke unpacker and parser
					File beatmapDir = Options.getBeatmapDir();
					OszUnpacker.unpackAllFiles(Options.getOSZDir(), beatmapDir);
					OsuParser.parseAllFiles(beatmapDir, container.getWidth(), container.getHeight());

					// initialize song list
					if (OsuGroupList.get().size() > 0) {
						OsuGroupList.get().init();
						setFocus(OsuGroupList.get().getRandomNode(), -1, true);
					} else if (Options.isThemSongEnabled())
						MusicController.playThemeSong();

					reloadThread = null;
				}
			};
			reloadThread.start();
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		case Input.KEY_ENTER:
			if (focusNode == null)
				break;
			if (input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) {
				// turn on "auto" mod
				if (!GameMod.AUTO.isActive())
					GameMod.AUTO.toggle(true);
			}
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
				SoundController.playSound(SoundEffect.MENUCLICK);
				OsuGroupNode oldStartNode = startNode;
				float oldHoverOffset = hoverOffset;
				int oldHoverIndex = hoverIndex;
				setFocus(next, 0, false);
				if (startNode == oldStartNode) {
					hoverOffset = oldHoverOffset;
					hoverIndex = oldHoverIndex;
				}
			}
			break;
		case Input.KEY_LEFT:
			if (focusNode == null)
				break;
			OsuGroupNode prev = focusNode.prev;
			if (prev != null) {
				SoundController.playSound(SoundEffect.MENUCLICK);
				OsuGroupNode oldStartNode = startNode;
				float oldHoverOffset = hoverOffset;
				int oldHoverIndex = hoverIndex;
				setFocus(prev, (prev.index == focusNode.index) ? 0 : prev.osuFiles.size() - 1, false);
				if (startNode == oldStartNode) {
					hoverOffset = oldHoverOffset;
					hoverIndex = oldHoverIndex;
				}
			}
			break;
		case Input.KEY_NEXT:
			changeIndex(MAX_SONG_BUTTONS);
			break;
		case Input.KEY_PRIOR:
			changeIndex(-MAX_SONG_BUTTONS);
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
		// block input during beatmap reloading
		if (reloadThread != null)
			return;

		int diff = newy - oldy;
		if (diff == 0)
			return;
		int shift = (diff < 0) ? 1 : -1;

		// check mouse button (right click scrolls faster on songs)
		int multiplier;
		if (input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON))
			multiplier = 4;
		else if (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON))
			multiplier = 1;
		else
			return;

		// score buttons
		if (focusScores != null && focusScores.length >= MAX_SCORE_BUTTONS && ScoreData.areaContains(oldx, oldy)) {
			int newStartScore = startScore + shift;
			if (newStartScore >= 0 && newStartScore + MAX_SCORE_BUTTONS <= focusScores.length)
				startScore = newStartScore;
		}

		// song buttons
		else
			changeIndex(shift * multiplier);
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		// block input during beatmap reloading
		if (reloadThread != null)
			return;

		int shift = (newValue < 0) ? 1 : -1;
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();

		// score buttons
		if (focusScores != null && focusScores.length >= MAX_SCORE_BUTTONS && ScoreData.areaContains(mouseX, mouseY)) {
			int newStartScore = startScore + shift;
			if (newStartScore >= 0 && newStartScore + MAX_SCORE_BUTTONS <= focusScores.length)
				startScore = newStartScore;
		}

		// song buttons
		else
			changeIndex(shift);
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		Display.setTitle(game.getTitle());
		Utils.getBackButton().setScale(1f);
		optionsButton.setScale(1f);
		hoverOffset = 0f;
		hoverIndex = -1;
		startScore = 0;

		// stop playing the theme song
		if (MusicController.isThemePlaying() && focusNode != null)
			MusicController.play(focusNode.osuFiles.get(focusNode.osuFileIndex), true);

		// reset music track
		else if (resetTrack) {
			MusicController.pause();
			MusicController.playAt(MusicController.getOsuFile().previewTime, true);
			resetTrack = false;
		}

		// unpause track
		else if (MusicController.isPaused())
			MusicController.resume();

		// undim track
		if (MusicController.isTrackDimmed())
			MusicController.toggleTrackDimmed(1f);

		// reset song stack
		randomStack = new Stack<SongNode>();

		// reset game data
		if (resetGame) {
			((Game) game.getState(Opsu.STATE_GAME)).resetGameData();

			// destroy skin images, if any
			for (GameImage img : GameImage.values()) {
				if (img.isSkinnable())
					img.destroySkinImage();
			}

			// reload scores
			if (focusNode != null) {
				scoreMap = ScoreDB.getMapSetScores(focusNode.osuFiles.get(focusNode.osuFileIndex));
				focusScores = getScoreDataForNode(focusNode, true);
			}

			resetGame = false;
		}
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
		search.setFocus(false);
	}

	/**
	 * Shifts the startNode forward (+) or backwards (-) by a given number of nodes.
	 * Initiates sliding "animation" by shifting the button Y position.
	 * @param shift the number of nodes to shift
	 */
	private void changeIndex(int shift) {
		if (shift == 0)
			return;

		int n = shift;
		boolean shifted = false;
		while (n != 0) {
			if (startNode == null)
				break;

			int height = container.getHeight();
			if (n < 0 && startNode.prev != null) {
				startNode = startNode.prev;
				buttonY += buttonOffset / 4;
				if (buttonY > height * 0.18f)
					buttonY = height * 0.18f;
				n++;
				shifted = true;
			} else if (n > 0 && startNode.next != null &&
			           OsuGroupList.get().getNode(startNode, MAX_SONG_BUTTONS) != null) {
				startNode = startNode.next;
				buttonY -= buttonOffset / 4;
				if (buttonY < height * 0.14f)
					buttonY = height * 0.14f;
				n--;
				shifted = true;
			} else
				break;
		}
		if (shifted) {
			hoverOffset = 0f;
			hoverIndex = -1;
		}
		return;
	}

	/**
	 * Sets a new focus node.
	 * @param node the base node; it will be expanded if it isn't already
	 * @param pos the OsuFile element to focus; if out of bounds, it will be randomly chosen
	 * @param flag if true, startNode will be set to the first node in the group
	 * @return the old focus node
	 */
	public OsuGroupNode setFocus(OsuGroupNode node, int pos, boolean flag) {
		if (node == null)
			return null;

		hoverOffset = 0f;
		hoverIndex = -1;
		songInfo = null;
		OsuGroupNode oldFocus = focusNode;

		// expand node before focusing it
		int expandedIndex = OsuGroupList.get().getExpandedIndex();
		if (node.index != expandedIndex) {
			node = OsuGroupList.get().expand(node.index);

			// if start node was previously expanded, move it
			if (startNode != null && startNode.index == expandedIndex)
				startNode = node;
		}

		// check pos bounds
		int length = node.osuFiles.size();
		if (pos < 0 || pos > length - 1)  // set a random pos
			pos = (int) (Math.random() * length);

		// change the focus node
		if (flag || (startNode.index == 0 && startNode.osuFileIndex == -1 && startNode.prev == null))
			startNode = node;
		focusNode = OsuGroupList.get().getNode(node, pos);
		OsuFile osu = focusNode.osuFiles.get(focusNode.osuFileIndex);
		MusicController.play(osu, true);
		Utils.loadGlyphs(osu);

		// load scores
		scoreMap = ScoreDB.getMapSetScores(osu);
		focusScores = getScoreDataForNode(focusNode, true);
		startScore = 0;

		// check startNode bounds
		while (startNode.index >= OsuGroupList.get().size() + length - MAX_SONG_BUTTONS && startNode.prev != null)
			startNode = startNode.prev;

		// make sure focusNode is on the screen (TODO: cleanup...)
		int val = focusNode.index + focusNode.osuFileIndex - (startNode.index + MAX_SONG_BUTTONS) + 1;
		if (val > 0)  // below screen
			changeIndex(val);
		else {  // above screen
			if (focusNode.index == startNode.index) {
				val = focusNode.index + focusNode.osuFileIndex - (startNode.index + startNode.osuFileIndex);
				if (val < 0)
					changeIndex(val);
			} else if (startNode.index > focusNode.index) {
				val = focusNode.index - focusNode.osuFiles.size() + focusNode.osuFileIndex - startNode.index + 1;
				if (val < 0)
					changeIndex(val);
			}
		}

		// if start node is expanded and on group node, move it
		if (startNode.index == focusNode.index && startNode.osuFileIndex == -1)
			changeIndex(1);

		return oldFocus;
	}

	/**
	 * Triggers a reset of game data upon entering this state.
	 */
	public void resetGameDataOnLoad() { resetGame = true; }

	/**
	 * Triggers a reset of the music track upon entering this state.
	 */
	public void resetTrackOnLoad() { resetTrack = true; }

	/**
	 * Returns all the score data for an OsuGroupNode from scoreMap.
	 * If no score data is available for the node, return null.
	 * @param node the OsuGroupNode
	 * @param setTimeSince whether or not to set the "time since" field for the scores
	 * @return the ScoreData array
	 */
	private ScoreData[] getScoreDataForNode(OsuGroupNode node, boolean setTimeSince) {
		if (scoreMap == null || scoreMap.isEmpty() || node.osuFileIndex == -1)  // node not expanded
			return null;

		OsuFile osu = node.osuFiles.get(node.osuFileIndex);
		ScoreData[] scores = scoreMap.get(osu.version);
		if (scores == null || scores.length < 1)  // no scores
			return null;

		ScoreData s = scores[0];
		if (osu.beatmapID == s.MID && osu.beatmapSetID == s.MSID &&
		    osu.title.equals(s.title) && osu.artist.equals(s.artist) &&
		    osu.creator.equals(s.creator)) {
			if (setTimeSince) {
				for (int i = 0; i < scores.length; i++)
					scores[i].getTimeSince();
			}
			return scores;
		} else
			return null;  // incorrect map
	}

	/**
	 * Starts the game.
	 * @param osu the OsuFile to send to the game
	 */
	private void startGame() {
		if (MusicController.isTrackLoading())
			return;

		SoundController.playSound(SoundEffect.MENUHIT);
		OsuFile osu = MusicController.getOsuFile();
		Display.setTitle(String.format("%s - %s", game.getTitle(), osu.toString()));
		OsuParser.parseHitObjects(osu);
		HitSound.setSampleSet(osu.sampleSet);
		((Game) game.getState(Opsu.STATE_GAME)).setRestart(Game.Restart.NEW);
		game.enterState(Opsu.STATE_GAME, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
	}
}
