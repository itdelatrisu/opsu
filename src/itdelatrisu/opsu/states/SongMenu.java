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

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameData.Grade;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.ScoreData;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MultiClip;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapDifficultyCalculator;
import itdelatrisu.opsu.beatmap.BeatmapGroup;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.BeatmapSet;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.beatmap.BeatmapSetNode;
import itdelatrisu.opsu.beatmap.BeatmapSortOrder;
import itdelatrisu.opsu.beatmap.BeatmapWatchService;
import itdelatrisu.opsu.beatmap.BeatmapWatchService.BeatmapWatchServiceListener;
import itdelatrisu.opsu.beatmap.LRUCache;
import itdelatrisu.opsu.beatmap.OszUnpacker;
import itdelatrisu.opsu.db.BeatmapDB;
import itdelatrisu.opsu.db.ScoreDB;
import itdelatrisu.opsu.options.OptionGroup;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.options.OptionsOverlay;
import itdelatrisu.opsu.states.ButtonMenu.MenuState;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.DropdownMenu;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.KineticScrolling;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.StarStream;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import itdelatrisu.opsu.user.UserButton;
import itdelatrisu.opsu.user.UserList;
import itdelatrisu.opsu.user.UserSelectOverlay;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.Map;
import java.util.Stack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.gui.TextField;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.EasedFadeOutTransition;
import org.newdawn.slick.state.transition.FadeInTransition;

/**
 * "Song Selection" state.
 * <p>
 * Players are able to select a beatmap to play, view previous scores, choose game mods,
 * manage beatmaps, or change game options from this state.
 */
public class SongMenu extends BasicGameState {
	/** The max number of song buttons to be shown on each screen. */
	public static final int MAX_SONG_BUTTONS = 6;

	/** The max number of score buttons to be shown at a time. */
	public static final int MAX_SCORE_BUTTONS = 7;

	/** Delay time, in milliseconds, between each search. */
	private static final int SEARCH_DELAY = 500;

	/** Delay time, in milliseconds, before moving to the beatmap menu after a right click. */
	private static final int BEATMAP_MENU_DELAY = 600;

	/** Maximum x offset of song buttons for mouse hover, in pixels. */
	private static final float MAX_HOVER_OFFSET = 30f;

	/** Time, in milliseconds, for the search bar to fade in or out. */
	private static final int SEARCH_TRANSITION_TIME = 250;

	/** Line width of the header/footer divider. */
	private static final int DIVIDER_LINE_WIDTH = 4;

	/** Fast scrolling speed multiplier. */
	private static final float FAST_SCROLL_SPEED = 2f;

	/** Song node class representing an BeatmapSetNode and file index. */
	private static class SongNode {
		/** Song node. */
		private BeatmapSetNode node;

		/** File index. */
		private int index;

		/**
		 * Constructor.
		 * @param node the BeatmapSetNode
		 * @param index the file index
		 */
		public SongNode(BeatmapSetNode node, int index) {
			this.node = node;
			this.index = index;
		}

		/**
		 * Returns the associated BeatmapSetNode.
		 */
		public BeatmapSetNode getNode() { return node; }

		/**
		 * Returns the associated file index.
		 */
		public int getIndex() { return index; }
	}

	/** Current start node (topmost menu entry). */
	private BeatmapSetNode startNode;

	/** The first node is about this high above the header. */
	private KineticScrolling songScrolling = new KineticScrolling();

	/** The number of Nodes to offset from the top to the startNode. */
	private int startNodeOffset;

	/** Current focused (selected) node. */
	private BeatmapSetNode focusNode;

	/** The base node of the previous focus node. */
	private SongNode oldFocusNode = null;

	/** Stack of previous "random" (F2) focus nodes. */
	private Stack<SongNode> randomStack = new Stack<SongNode>();

	/** Current focus node's song information. */
	private String[] songInfo;

	/** Button coordinate values. */
	private float buttonX, buttonY, buttonOffset, buttonWidth, buttonHeight;

	/** Horizontal offset of song buttons for mouse hover, in pixels. */
	private AnimatedValue hoverOffset = new AnimatedValue(250, 0, MAX_HOVER_OFFSET, AnimationEquation.OUT_QUART);

	/** Current index of hovered song button. */
	private BeatmapSetNode hoverIndex = null;

	/** The selection buttons. */
	private MenuButton selectModsButton, selectRandomButton, selectMapOptionsButton, selectOptionsButton;

	/** The search textfield. */
	private TextField search;

	/** The search font. */
	private UnicodeFont searchFont;

	/**
	 * Delay timer, in milliseconds, before running another search.
	 * This is overridden by character entry (reset) and 'esc' (immediate search).
	 */
	private int searchTimer = 0;

	/** Information text to display based on the search query. */
	private String searchResultString = null, lastSearchResultString = null;

	/** Loader animation. */
	private Animation loader;

	/** Whether or not to reset game data upon entering the state. */
	private boolean resetGame = false;

	/** Whether or not to reset music track upon entering the state. */
	private boolean resetTrack = false;

	/** If non-null, determines the action to perform upon entering the state. */
	private MenuState stateAction;

	/** If non-null, the node that stateAction acts upon. */
	private BeatmapSetNode stateActionNode;

	/** If non-null, the score data that stateAction acts upon. */
	private ScoreData stateActionScore;

	/** Timer before moving to the beatmap menu with the current focus node. */
	private int beatmapMenuTimer = -1;

	/** Beatmap reloading thread. */
	private BeatmapReloadThread reloadThread;

	/** Thread for reloading beatmaps. */
	private class BeatmapReloadThread extends Thread {
		/** If true, also clear the beatmap cache and invoke the unpacker. */
		private final boolean fullReload;

		/** Whether this thread has completed execution. */
		private boolean finished = false;

		/** Returns true only if this thread has completed execution. */
		public boolean isFinished() { return finished; }

		/**
		 * Constructor.
		 * @param fullReload if true, also clear the beatmap cache and invoke the unpacker
		 */
		public BeatmapReloadThread(boolean fullReload) {
			this.fullReload = fullReload;
		}

		@Override
		public void run() {
			try {
				reloadBeatmaps();
			} finally {
				finished = true;
			}
		};

		/** Reloads all beatmaps. */
		private void reloadBeatmaps() {
			File beatmapDir = Options.getBeatmapDir();

			if (fullReload) {
				// clear the beatmap cache
				BeatmapDB.clearDatabase();

				// invoke unpacker
				OszUnpacker.unpackAllFiles(Options.getImportDir(), beatmapDir);
			}

			// invoke parser
			BeatmapParser.parseAllFiles(beatmapDir, BeatmapSetList.get());
		}
	}

	/** Current map of scores (Version, ScoreData[]). */
	private Map<String, ScoreData[]> scoreMap;

	/** Scores for the current focus node. */
	private ScoreData[] focusScores;

	/** Current start score (topmost score entry). */
	private KineticScrolling startScorePos = new KineticScrolling();

	/** Header and footer end and start y coordinates, respectively. */
	private float headerY, footerY;

	/** Footer pulsing logo button. */
	private MenuButton footerLogoButton;

	/** Size of the pulsing logo in the footer. */
	private float footerLogoSize;

	/** Time, in milliseconds, for fading the search bar. */
	private int searchTransitionTimer = SEARCH_TRANSITION_TIME;

	/** The text length of the last string in the search TextField. */
	private int lastSearchTextLength = 0;

	/** Whether the song folder changed (notified via the watch service). */
	private boolean songFolderChanged = false;

	/** The last selected beatmap. */
	private Beatmap lastBeatmap;

	/** The last beatmap for fading out. */
	private Beatmap lastFadeBeatmap;

	/** Background alpha levels (for crossfade effect). */
	private AnimatedValue
		bgAlpha = new AnimatedValue(600, 0f, 1f, AnimationEquation.OUT_QUAD),
		playfieldAlpha = new AnimatedValue(600, 0f, 1f, AnimationEquation.IN_QUAD),
		lastBgAlpha = new AnimatedValue(600, 1f, 0f, AnimationEquation.IN_QUAD);

	/** Timer for animations when a new song node is selected. */
	private AnimatedValue songChangeTimer = new AnimatedValue(900, 0f, 1f, AnimationEquation.LINEAR);

	/** Timer for the music icon animation when a new song node is selected. */
	private AnimatedValue musicIconBounceTimer = new AnimatedValue(350, 0f, 1f, AnimationEquation.LINEAR);

	/**
	 * Beatmaps whose difficulties were recently computed (if flag is non-null).
	 * Unless the Boolean flag is null, then upon removal, the beatmap's objects will
	 * be cleared (to be garbage collected). If the flag is true, also clear the
	 * beatmap's array fields (timing points, etc.).
	 */
	@SuppressWarnings("serial")
	private LRUCache<Beatmap, Boolean> beatmapsCalculated = new LRUCache<Beatmap, Boolean>(12) {
		@Override
		public void eldestRemoved(Map.Entry<Beatmap, Boolean> eldest) {
			Boolean b = eldest.getValue();
			if (b != null) {
				Beatmap beatmap = eldest.getKey();
				beatmap.objects = null;
				if (b) {
					beatmap.timingPoints = null;
					beatmap.breaks = null;
					beatmap.combo = null;
				}
			}
		}
	};

	/** The star stream. */
	private StarStream starStream;

	/** The maximum number of stars in the star stream. */
	private static final int MAX_STREAM_STARS = 20;

	/** Whether the menu is currently scrolling to the focus node (blocks other actions). */
	private boolean isScrollingToFocusNode = false;

	/** Sort order dropdown menu. */
	private DropdownMenu<BeatmapSortOrder> sortMenu;

	/** Options overlay. */
	private OptionsOverlay optionsOverlay;

	/** Whether the options overlay is being shown. */
	private boolean showOptionsOverlay = false;

	/** The options overlay show/hide animation progress. */
	private AnimatedValue optionsOverlayProgress = new AnimatedValue(500, 0f, 1f, AnimationEquation.LINEAR);

	/** The user button. */
	private UserButton userButton;

	/** User selection overlay. */
	private UserSelectOverlay userOverlay;

	/** Whether the user overlay is being shown. */
	private boolean showUserOverlay = false;

	/** The user overlay show/hide animation progress. */
	private AnimatedValue userOverlayProgress = new AnimatedValue(750, 0f, 1f, AnimationEquation.OUT_CUBIC);

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private final int state;

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

		// header/footer coordinates
		headerY = height * 0.0075f + GameImage.MENU_MUSICNOTE.getImage().getHeight() +
				Fonts.BOLD.getLineHeight() + Fonts.DEFAULT.getLineHeight() +
				Fonts.SMALL.getLineHeight();
		footerY = height - GameImage.SELECTION_MODS.getImage().getHeight();

		// footer logo coordinates
		float footerHeight = height - footerY;
		footerLogoSize = footerHeight * 3.25f;
		Image logo = GameImage.MENU_LOGO.getImage();
		logo = logo.getScaledCopy(footerLogoSize / logo.getWidth());
		footerLogoButton = new MenuButton(logo, width - footerHeight * 0.8f, height - footerHeight * 0.65f);
		footerLogoButton.setHoverAnimationDuration(1);
		footerLogoButton.setHoverExpand(1.2f);

		// initialize sorts
		sortMenu = new DropdownMenu<BeatmapSortOrder>(container, BeatmapSortOrder.values(), 0, 0, (int) (width * 0.12f)) {
			@Override
			public void itemSelected(int index, BeatmapSortOrder item) {
				BeatmapSortOrder.set(item);
				if (focusNode == null)
					return;
				BeatmapSetNode oldFocusBase = BeatmapSetList.get().getBaseNode(focusNode.index);
				int oldFocusFileIndex = focusNode.beatmapIndex;
				focusNode = null;
				BeatmapSetList.get().init();
				SongMenu.this.setFocus(oldFocusBase, oldFocusFileIndex, true, true);
			}

			@Override
			public boolean menuClicked(int index) {
				if (isInputBlocked())
					return false;

				SoundController.playSound(SoundEffect.MENUCLICK);
				return true;
			}
		};
		sortMenu.setLocation(
			(int) (width * 0.99f - sortMenu.getWidth()),
			(int) (headerY - GameImage.MENU_TAB.getImage().getHeight() * 2.25f)
		);
		sortMenu.setBackgroundColor(Colors.BLACK_BG_HOVER);
		sortMenu.setBorderColor(Colors.BLUE_DIVIDER);
		sortMenu.setChevronRightColor(Color.white);

		// initialize group tabs
		for (BeatmapGroup group : BeatmapGroup.values())
			group.init(width, headerY - DIVIDER_LINE_WIDTH / 2);

		// initialize score data buttons
		ScoreData.init(width, headerY + height * 0.01f);

		// song button background & graphics context
		Image menuBackground = GameImage.MENU_BUTTON_BG.getImage();

		// song button coordinates
		buttonX = width * 0.6f;
		//buttonY = headerY;
		buttonWidth = menuBackground.getWidth();
		buttonHeight = menuBackground.getHeight();
		buttonOffset = (footerY - headerY - DIVIDER_LINE_WIDTH) / MAX_SONG_BUTTONS;

		// search
		int textFieldX = (int) (width * 0.7125f + Fonts.BOLD.getWidth("Search: "));
		int textFieldY = (int) (headerY + Fonts.BOLD.getLineHeight() / 3f);
		searchFont = Fonts.BOLD;
		search = new TextField(
				container, searchFont, textFieldX, textFieldY,
				(int) (width * 0.99f) - textFieldX, Fonts.BOLD.getLineHeight()
		);
		search.setBackgroundColor(Color.transparent);
		search.setBorderColor(Color.transparent);
		search.setTextColor(Color.white);
		search.setConsumeEvents(false);
		search.setMaxLength(60);

		// selection buttons
		Image selectionMods = GameImage.SELECTION_MODS.getImage();
		float selectX = width * 0.183f + selectionMods.getWidth() / 2f;
		float selectY = height - selectionMods.getHeight() / 2f;
		float selectOffset = selectionMods.getWidth() * 1.05f;
		selectModsButton = new MenuButton(GameImage.SELECTION_MODS_OVERLAY.getImage(),
				selectX, selectY);
		selectRandomButton = new MenuButton(GameImage.SELECTION_RANDOM_OVERLAY.getImage(),
				selectX + selectOffset, selectY);
		selectMapOptionsButton = new MenuButton(GameImage.SELECTION_OPTIONS_OVERLAY.getImage(),
				selectX + selectOffset * 2f, selectY);
		selectOptionsButton = new MenuButton(GameImage.SELECTION_OTHER_OPTIONS_OVERLAY.getImage(),
				selectX + selectOffset * 3f, selectY);
		selectModsButton.setHoverFade(0f);
		selectRandomButton.setHoverFade(0f);
		selectMapOptionsButton.setHoverFade(0f);
		selectOptionsButton.setHoverFade(0f);

		// loader
		int loaderDim = GameImage.MENU_MUSICNOTE.getImage().getWidth();
		SpriteSheet spr = new SpriteSheet(GameImage.MENU_LOADER.getImage(), loaderDim, loaderDim);
		loader = new Animation(spr, 50);

		// beatmap watch service listener
		final StateBasedGame game_ = game;
		BeatmapWatchService.addListener(new BeatmapWatchServiceListener() {
			@Override
			public void eventReceived(Kind<?> kind, Path child) {
				if (!songFolderChanged && kind != StandardWatchEventKinds.ENTRY_MODIFY) {
					songFolderChanged = true;
					if (game_.getCurrentStateID() == Opsu.STATE_SONGMENU)
						UI.getNotificationManager().sendNotification("Changes in Songs folder detected.\nHit F5 to refresh.");
				}
			}
		});

		// star stream
		starStream = new StarStream(width, (height - GameImage.STAR.getImage().getHeight()) / 2, -width, 0, MAX_STREAM_STARS);
		starStream.setPositionSpread(height / 20f);
		starStream.setDirectionSpread(10f);

		// options overlay
		optionsOverlay = new OptionsOverlay(container, OptionGroup.ALL_OPTIONS, new OptionsOverlay.OptionsOverlayListener() {
			@Override
			public void close() {
				showOptionsOverlay = false;
				optionsOverlay.deactivate();
				optionsOverlay.reset();
				optionsOverlayProgress.setTime(0);
			}
		});
		optionsOverlay.setConsumeAndClose(true);

		// user button
		userButton = new UserButton(width / 2, height - UserButton.getHeight(), Color.white);

		// user selection overlay
		userOverlay = new UserSelectOverlay(container, new UserSelectOverlay.UserSelectOverlayListener() {
			@Override
			public void close(boolean userChanged) {
				showUserOverlay = false;
				userOverlay.deactivate();
				userOverlayProgress.setTime(0);
				if (userChanged)
					userButton.flash();
			}
		});
		userOverlay.setConsumeAndClose(true);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Color.black);

		int width = container.getWidth();
		int height = container.getHeight();
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		boolean inDropdownMenu = sortMenu.contains(mouseX, mouseY);

		// background (crossfade)
		float parallaxX = 0, parallaxY = 0;
		if (Options.isParallaxEnabled()) {
			int offset = (int) (height * (GameImage.PARALLAX_SCALE - 1f));
			parallaxX = -offset / 2f * (mouseX - width / 2) / (width / 2);
			parallaxY = -offset / 2f * (mouseY - height / 2) / (height / 2);
		}
		if (!lastBgAlpha.isFinished() && lastFadeBeatmap != null && lastFadeBeatmap.hasLoadedBackground())
			lastFadeBeatmap.drawBackground(width, height, parallaxX, parallaxY, lastBgAlpha.getValue(), true);
		if (playfieldAlpha.getTime() > 0) {
			Image bg = GameImage.MENU_BG.getImage();
			if (Options.isParallaxEnabled()) {
				bg = bg.getScaledCopy(GameImage.PARALLAX_SCALE);
				bg.setAlpha(playfieldAlpha.getValue());
				bg.drawCentered(width / 2 + parallaxX, height / 2 + parallaxY);
			} else {
				bg.setAlpha(playfieldAlpha.getValue());
				bg.drawCentered(width / 2, height / 2);
				bg.setAlpha(1f);
			}
		}
		if (lastBeatmap != null && lastBeatmap.hasLoadedBackground())
			lastBeatmap.drawBackground(width, height, parallaxX, parallaxY, bgAlpha.getValue(), true);

		// star stream
		starStream.draw();

		// song buttons
		BeatmapSetNode node = startNode;
		int songButtonIndex = 0;
		if (node != null && node.prev != null) {
			node = node.prev;
			songButtonIndex = -1;
		}
		g.setClip(0, (int) (headerY + DIVIDER_LINE_WIDTH / 2), width, (int) (footerY - headerY));
		for (int i = startNodeOffset + songButtonIndex; i < MAX_SONG_BUTTONS + 1 && node != null; i++, node = node.next) {
			// draw the node
			float offset = (node == hoverIndex) ? hoverOffset.getValue() : 0f;
			float ypos = buttonY + (i * buttonOffset);
			float mid = (height / 2) - ypos - (buttonOffset / 2);
			final float circleRadi = 700 * GameImage.getUIscale();
			//finds points along a very large circle  (x^2 = h^2 - y^2)
			float t = circleRadi * circleRadi - (mid * mid);
			float xpos = (float) ((t > 0) ? Math.sqrt(t) : 0) - circleRadi + 50 * GameImage.getUIscale();
			ScoreData[] scores = getScoreDataForNode(node, false);
			node.draw(buttonX - offset - xpos, ypos,
			          (scores == null) ? Grade.NULL : scores[0].getGrade(), (node == focusNode));
		}
		g.clearClip();

		// scroll bar
		if (focusNode != null && startNode != null) {
			int focusNodes = focusNode.getBeatmapSet().size();
			int totalNodes = BeatmapSetList.get().size() + focusNodes - 1;
			if (totalNodes > MAX_SONG_BUTTONS) {
				UI.drawScrollbar(g,
						songScrolling.getPosition(),
						totalNodes * buttonOffset,
						MAX_SONG_BUTTONS * buttonOffset,
						width, headerY + DIVIDER_LINE_WIDTH / 2,
						0, MAX_SONG_BUTTONS * buttonOffset,
						Colors.BLACK_ALPHA, Color.white, true);
			}
		}

		// score buttons
		if (focusScores != null) {
			ScoreData.clipToArea(g);
			int startScore = (int) (startScorePos.getPosition() / ScoreData.getButtonOffset());
			int offset = (int) (-startScorePos.getPosition() + startScore * ScoreData.getButtonOffset());

			int scoreButtons = Math.min(focusScores.length - startScore, MAX_SCORE_BUTTONS + 1);
			float timerScale = 1f - (1 / 3f) * ((MAX_SCORE_BUTTONS - scoreButtons) / (float) (MAX_SCORE_BUTTONS - 1));
			int duration = (int) (songChangeTimer.getDuration() * timerScale);
			int segmentDuration = (int) ((2 / 3f) * songChangeTimer.getDuration());
			int time = songChangeTimer.getTime();
			for (int i = 0, rank = startScore; i < scoreButtons; i++, rank++) {
				if (rank < 0)
					continue;
				long prevScore = (rank + 1 < focusScores.length) ? focusScores[rank + 1].score : -1;
				boolean focus = ScoreData.buttonContains(mouseX, mouseY - offset, i) && !showOptionsOverlay && !showUserOverlay;
				float t = Utils.clamp((time - (i * (duration - segmentDuration) / scoreButtons)) / (float) segmentDuration, 0f, 1f);
				focusScores[rank].draw(g, offset + i * ScoreData.getButtonOffset(), rank, prevScore, focus, t);
			}
			g.clearClip();

			// scroll bar
			if (focusScores.length > MAX_SCORE_BUTTONS && ScoreData.areaContains(mouseX, mouseY) && !inDropdownMenu)
				ScoreData.drawScrollbar(g, startScorePos.getPosition(), focusScores.length * ScoreData.getButtonOffset());
		}

		// top/bottom bars
		g.setColor(Colors.BLACK_ALPHA);
		g.fillRect(0, 0, width, headerY);
		g.fillRect(0, footerY, width, height - footerY);
		g.setColor(Colors.BLUE_DIVIDER);
		g.setLineWidth(DIVIDER_LINE_WIDTH);
		g.drawLine(0, headerY, width, headerY);
		g.drawLine(0, footerY, width, footerY);
		g.resetLineWidth();

		// footer logo (pulsing)
		Float position = MusicController.getBeatProgress();
		if (position == null)  // default to 60bpm
			position = System.currentTimeMillis() % 1000 / 1000f;
		if (footerLogoButton.contains(mouseX, mouseY, 0.25f) && !inDropdownMenu) {
			// hovering over logo: stop pulsing
			footerLogoButton.draw();
		} else {
			float expand = position * 0.15f;
			footerLogoButton.draw(Color.white, 1f - expand);
			Image ghostLogo = GameImage.MENU_LOGO.getImage();
			ghostLogo = ghostLogo.getScaledCopy((1f + expand) * footerLogoSize / ghostLogo.getWidth());
			float oldGhostAlpha = Colors.GHOST_LOGO.a;
			Colors.GHOST_LOGO.a *= (1f - position);
			ghostLogo.drawCentered(footerLogoButton.getX(), footerLogoButton.getY(), Colors.GHOST_LOGO);
			Colors.GHOST_LOGO.a = oldGhostAlpha;
		}

		// header
		if (focusNode != null) {
			// music/loader icon
			float marginX = width * 0.005f, marginY = height * 0.005f;
			Image musicNote = GameImage.MENU_MUSICNOTE.getImage();
			if (MusicController.isTrackLoading() && musicIconBounceTimer.isFinished())
				loader.draw(marginX, marginY);
			else {
				float t = musicIconBounceTimer.getValue() * 2f;
				if (t > 1)
					t = 2f - t;
				float musicNoteScale = 1f + 0.3f * t;
				musicNote.getScaledCopy(musicNoteScale).drawCentered(marginX + musicNote.getWidth() / 2f, marginY + musicNote.getHeight() / 2f);
			}
			int iconWidth = musicNote.getWidth();

			// song info text
			if (songInfo == null) {
				songInfo = focusNode.getInfo();
				Beatmap beatmap = focusNode.getBeatmapSet().get(0);
				if (!beatmap.source.isEmpty())
					Fonts.loadGlyphs(Fonts.LARGE, beatmap.source);
				if (Options.useUnicodeMetadata()) {  // load glyphs
					Fonts.loadGlyphs(Fonts.LARGE, beatmap.titleUnicode);
					Fonts.loadGlyphs(Fonts.LARGE, beatmap.artistUnicode);
				}
			}
			marginX += 5;
			Color c = Colors.WHITE_FADE;
			float oldAlpha = c.a;
			float t = AnimationEquation.OUT_QUAD.calc(songChangeTimer.getValue());
			float headerTextY = marginY * 0.2f;
			c.a = Math.min(t * songInfo.length / 1.5f, 1f);
			if (c.a > 0)
				Fonts.LARGE.drawString(marginX + iconWidth * 1.05f, headerTextY, songInfo[0], c);
			headerTextY += Fonts.LARGE.getLineHeight() - 6;
			c.a = Math.min((t - 1f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0)
				Fonts.DEFAULT.drawString(marginX + iconWidth * 1.05f, headerTextY, songInfo[1], c);
			headerTextY += Fonts.DEFAULT.getLineHeight() - 2;
			c.a = Math.min((t - 2f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0) {
				float speedModifier = GameMod.getSpeedMultiplier();
				Color color2 = (speedModifier == 1f) ? c :
					(speedModifier > 1f) ? Colors.RED_HIGHLIGHT : Colors.BLUE_HIGHLIGHT;
				float oldAlpha2 = color2.a;
				color2.a = c.a;
				Fonts.BOLD.drawString(marginX, headerTextY, songInfo[2], color2);
				color2.a = oldAlpha2;
			}
			headerTextY += Fonts.BOLD.getLineHeight() - 4;
			c.a = Math.min((t - 3f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0)
				Fonts.DEFAULT.drawString(marginX, headerTextY, songInfo[3], c);
			headerTextY += Fonts.DEFAULT.getLineHeight() - 2;
			c.a = Math.min((t - 4f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0) {
				float multiplier = GameMod.getDifficultyMultiplier();
				Color color4 = (multiplier == 1f) ? c :
					(multiplier > 1f) ? Colors.RED_HIGHLIGHT : Colors.BLUE_HIGHLIGHT;
				float oldAlpha4 = color4.a;
				color4.a = c.a;
				Fonts.SMALL.drawString(marginX, headerTextY, songInfo[4], color4);
				color4.a = oldAlpha4;
			}
			c.a = oldAlpha;
		}

		// selection buttons
		GameImage.SELECTION_MODS.getImage().drawCentered(selectModsButton.getX(), selectModsButton.getY());
		selectModsButton.draw();
		GameImage.SELECTION_RANDOM.getImage().drawCentered(selectRandomButton.getX(), selectRandomButton.getY());
		selectRandomButton.draw();
		GameImage.SELECTION_OPTIONS.getImage().drawCentered(selectMapOptionsButton.getX(), selectMapOptionsButton.getY());
		selectMapOptionsButton.draw();
		GameImage.SELECTION_OTHER_OPTIONS.getImage().drawCentered(selectOptionsButton.getX(), selectOptionsButton.getY());
		selectOptionsButton.draw();

		// user button
		userButton.setUser(UserList.get().getCurrentUser());
		userButton.draw(g);

		// group tabs
		BeatmapGroup currentGroup = BeatmapGroup.current();
		BeatmapGroup hoverGroup = null;
		if (!inDropdownMenu) {
			for (BeatmapGroup group : BeatmapGroup.values()) {
				if (group.contains(mouseX, mouseY)) {
					hoverGroup = group;
					break;
				}
			}
		}
		for (BeatmapGroup group : BeatmapGroup.VALUES_REVERSED) {
			if (group != currentGroup)
				group.draw(false, group == hoverGroup);
		}
		currentGroup.draw(true, false);

		// search
		boolean searchEmpty = search.getText().isEmpty();
		int searchX = search.getX(), searchY = search.getY();
		float searchBaseX = width * 0.7f;
		float searchTextX = width * 0.7125f;
		float searchRectHeight = Fonts.BOLD.getLineHeight() * 2;
		float searchExtraHeight = Fonts.DEFAULT.getLineHeight() * 0.7f;
		float searchProgress = (searchTransitionTimer < SEARCH_TRANSITION_TIME) ?
				((float) searchTransitionTimer / SEARCH_TRANSITION_TIME) : 1f;
		float oldAlpha = Colors.BLACK_ALPHA.a;
		if (searchEmpty) {
			searchRectHeight += (1f - searchProgress) * searchExtraHeight;
			Colors.BLACK_ALPHA.a = 0.3f - searchProgress * 0.15f;
		} else {
			searchRectHeight += searchProgress * searchExtraHeight;
			Colors.BLACK_ALPHA.a = 0.15f + searchProgress * 0.15f;
		}
		g.setColor(Colors.BLACK_ALPHA);
		g.fillRect(searchBaseX, headerY + DIVIDER_LINE_WIDTH / 2, width - searchBaseX, searchRectHeight);
		Colors.BLACK_ALPHA.a = oldAlpha;
		Fonts.BOLD.drawString(searchTextX, searchY, "Search:", Colors.GREEN_SEARCH);
		if (searchEmpty)
			Fonts.BOLD.drawString(searchX, searchY, "Type to search!", Color.white);
		else {
			g.setColor(Color.white);
			// TODO: why is this needed to correctly position the TextField?
			search.setLocation(searchX - 3, searchY - 1);
			search.render(container, g);
			search.setLocation(searchX, searchY);
			Fonts.DEFAULT.drawString(searchTextX, searchY + Fonts.BOLD.getLineHeight(),
					(searchResultString == null) ? "Searching..." : searchResultString, Color.white);
		}

		// sorting options
		sortMenu.render(container, g);

		// reloading beatmaps
		if (reloadThread != null) {
			// darken the screen
			g.setColor(Colors.BLACK_ALPHA);
			g.fillRect(0, 0, width, height);

			UI.drawLoadingProgress(g, 1f);
		}

		// back button
		else if (!showOptionsOverlay && !showUserOverlay)
			UI.getBackButton().draw(g);

		// options overlay
		if (showOptionsOverlay || !optionsOverlayProgress.isFinished())
			optionsOverlay.render(container, g);

		// user overlay
		if (showUserOverlay || !userOverlayProgress.isFinished())
			userOverlay.render(container, g);

		UI.draw(g);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		UI.update(delta);
		if (reloadThread == null)
			MusicController.loopTrackIfEnded(true);
		else if (reloadThread.isFinished()) {
			BeatmapGroup.set(BeatmapGroup.ALL);
			BeatmapSortOrder.set(BeatmapSortOrder.TITLE);
			BeatmapSetList.get().reset();
			BeatmapSetList.get().init();
			if (BeatmapSetList.get().size() > 0) {
				// initialize song list
				setFocus(BeatmapSetList.get().getRandomNode(), 0, true, true);
			} else
				MusicController.playThemeSong();
			reloadThread = null;
		}
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		boolean inDropdownMenu = sortMenu.contains(mouseX, mouseY);
		UI.getBackButton().hoverUpdate(delta, mouseX, mouseY);
		selectModsButton.hoverUpdate(delta, mouseX, mouseY);
		selectRandomButton.hoverUpdate(delta, mouseX, mouseY);
		selectMapOptionsButton.hoverUpdate(delta, mouseX, mouseY);
		selectOptionsButton.hoverUpdate(delta, mouseX, mouseY);
		userButton.hoverUpdate(delta, userButton.contains(mouseX, mouseY));
		footerLogoButton.hoverUpdate(delta, mouseX, mouseY, 0.25f);

		// options overlay
		if (optionsOverlayProgress.update(delta)) {
			// slide in/out
			float t = optionsOverlayProgress.getValue();
			float navigationAlpha;
			if (!showOptionsOverlay) {
				navigationAlpha = 1f - AnimationEquation.IN_CIRC.calc(t);
				t = 1f - t;
			} else
				navigationAlpha = Utils.clamp(t * 10f, 0f, 1f);
			t = AnimationEquation.OUT_CUBIC.calc(t);
			optionsOverlay.setWidth((int) (optionsOverlay.getTargetWidth() * t));
			optionsOverlay.setAlpha(t, navigationAlpha);
		} else if (showOptionsOverlay)
			optionsOverlay.update(delta);

		// user overlay
		if (userOverlayProgress.update(delta)) {
			// fade in/out
			float t = userOverlayProgress.getValue();
			userOverlay.setAlpha(showUserOverlay ? t : 1f - t);
		} else if (showUserOverlay)
			userOverlay.update(delta);

		// beatmap menu timer
		if (beatmapMenuTimer > -1) {
			beatmapMenuTimer += delta;
			if (beatmapMenuTimer >= BEATMAP_MENU_DELAY) {
				beatmapMenuTimer = -1;
				if (focusNode != null) {
					MenuState state = focusNode.getBeatmapSet().isFavorite() ?
						MenuState.BEATMAP_FAVORITE : MenuState.BEATMAP;
					((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(state, focusNode);
					game.enterState(Opsu.STATE_BUTTONMENU);
				}
				return;
			}
		}

		// background (crossfade)
		lastBgAlpha.update(delta);
		if (lastBeatmap != null && lastBeatmap.hasLoadedBackground()) {
			bgAlpha.update(delta);
			playfieldAlpha.update(-delta);
		} else
			playfieldAlpha.update(delta);

		// song change timers
		if (focusNode != null) {
			songChangeTimer.update(delta);
			musicIconBounceTimer.update(delta);
		}

		// star stream
		starStream.update(delta);

		// search
		search.setFocus(true);
		searchTimer += delta;
		if (searchTimer >= SEARCH_DELAY && reloadThread == null && beatmapMenuTimer == -1) {
			searchTimer = 0;
			updateSearch();
		}
		if (searchTransitionTimer < SEARCH_TRANSITION_TIME) {
			searchTransitionTimer += delta;
			if (searchTransitionTimer > SEARCH_TRANSITION_TIME)
				searchTransitionTimer = SEARCH_TRANSITION_TIME;
		}

		// scores
		if (focusScores != null) {
			startScorePos.setMinMax(0, (focusScores.length - MAX_SCORE_BUTTONS) * ScoreData.getButtonOffset());
			startScorePos.update(delta);
		}

		// scrolling
		songScrolling.update(delta);
		if (isScrollingToFocusNode) {
			float distanceDiff = Math.abs(songScrolling.getPosition() - songScrolling.getTargetPosition());
			if (distanceDiff <= buttonOffset / 8f) {  // close enough, stop blocking input
				songScrolling.scrollToPosition(songScrolling.getTargetPosition());
				songScrolling.setSpeedMultiplier(1f);
				isScrollingToFocusNode = false;
			}
		}
		updateDrawnSongPosition();

		// mouse hover
		BeatmapSetNode node = getNodeAtPosition(mouseX, mouseY);
		if (node != null && !inDropdownMenu && !showOptionsOverlay && !showUserOverlay) {
			if (node == hoverIndex)
				hoverOffset.update(delta);
			else {
				hoverIndex = node;
				hoverOffset.setTime(0);
			}
			return;
		} else {  // not hovered
			hoverOffset.setTime(0);
			hoverIndex = null;
		}

		// tooltips
		if (sortMenu.baseContains(mouseX, mouseY))
			UI.updateTooltip(delta, "Sort by...", false);
		else if (focusScores != null && ScoreData.areaContains(mouseX, mouseY) && !showOptionsOverlay && !showUserOverlay) {
			int startScore = (int) (startScorePos.getPosition() / ScoreData.getButtonOffset());
			int offset = (int) (-startScorePos.getPosition() + startScore * ScoreData.getButtonOffset());
			int scoreButtons = Math.min(focusScores.length - startScore, MAX_SCORE_BUTTONS + 1);
			for (int i = 0, rank = startScore; i < scoreButtons; i++, rank++) {
				if (rank < 0)
					continue;
				if (ScoreData.buttonContains(mouseX, mouseY - offset, i)) {
					UI.updateTooltip(delta, focusScores[rank].getTooltipString(), true);
					break;
				}
			}
		}
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// block input
		if (isInputBlocked())
			return;

		if (showOptionsOverlay || !optionsOverlayProgress.isFinished() ||
		    showUserOverlay || !userOverlayProgress.isFinished())
			return;

		if (isScrollingToFocusNode)
			return;

		if (input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON)) {
			songScrolling.setSpeedMultiplier(FAST_SCROLL_SPEED);

			// check if anything was clicked
			for (BeatmapGroup group : BeatmapGroup.values()) {
				if (group.contains(x, y))
					return;
			}
			if (UI.getBackButton().contains(x, y) ||
			    selectModsButton.contains(x, y) || selectRandomButton.contains(x, y) ||
			    selectMapOptionsButton.contains(x, y) || selectOptionsButton.contains(x, y) ||
			    focusNode == null || getNodeAtPosition(x, y) != null ||
			    footerLogoButton.contains(x, y, 0.25f) || ScoreData.areaContains(x, y))
				return;

			// scroll to the mouse position on the screen
			scrollSongsToPosition(y);
		} else
			songScrolling.pressed();
		startScorePos.pressed();
	}

	@Override
	public void mouseReleased(int button, int x, int y) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		if (isScrollingToFocusNode)
			return;

		songScrolling.setSpeedMultiplier(1f);
		songScrolling.released();
		startScorePos.released();
	}

	@Override
	public void mouseClicked(int button, int x, int y, int clickCount) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// block input
		if (isInputBlocked())
			return;

		if (showOptionsOverlay || !optionsOverlayProgress.isFinished() ||
		    showUserOverlay || !userOverlayProgress.isFinished())
			return;

		// back
		if (UI.getBackButton().contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			((MainMenu) game.getState(Opsu.STATE_MAINMENU)).reset();
			game.enterState(Opsu.STATE_MAINMENU, new EasedFadeOutTransition(), new FadeInTransition());
			return;
		}

		// selection buttons
		if (selectModsButton.contains(x, y)) {
			openModsMenu();
			return;
		} else if (selectRandomButton.contains(x, y)) {
			randomBeatmap(false);
			return;
		} else if (selectMapOptionsButton.contains(x, y)) {
			openBeatmapOptionsMenu();
			return;
		} else if (selectOptionsButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			showOptionsOverlay = true;
			optionsOverlayProgress.setTime(0);
			optionsOverlay.activate();
			return;
		}

		// user button
		if (userButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUCLICK);
			showUserOverlay = true;
			userOverlayProgress.setTime(0);
			userOverlay.activate();
			return;
		}

		// group tabs
		for (BeatmapGroup group : BeatmapGroup.values()) {
			if (group.contains(x, y)) {
				if (group != BeatmapGroup.current()) {
					BeatmapGroup.set(group);
					SoundController.playSound(SoundEffect.MENUCLICK);
					startNode = focusNode = null;
					oldFocusNode = null;
					randomStack = new Stack<SongNode>();
					songInfo = null;
					scoreMap = null;
					focusScores = null;
					search.setText("");
					searchTimer = SEARCH_DELAY;
					searchTransitionTimer = SEARCH_TRANSITION_TIME;
					searchResultString = null;
					lastSearchResultString = null;
					BeatmapSetList.get().reset();
					BeatmapSetList.get().init();
					setFocus(BeatmapSetList.get().getRandomNode(), 0, true, true);

					if (BeatmapSetList.get().size() < 1 && group.getEmptyMessage() != null)
						UI.getNotificationManager().sendBarNotification(group.getEmptyMessage());
				}
				return;
			}
		}

		if (focusNode == null)
			return;

		// logo: start game
		if (footerLogoButton.contains(x, y, 0.25f)) {
			startGame();
			return;
		}

		// song buttons
		BeatmapSetNode node = getNodeAtPosition(x, y);
		if (node != null) {
			int expandedIndex = BeatmapSetList.get().getExpandedIndex();
			int oldHoverOffsetTime = hoverOffset.getTime();
			BeatmapSetNode oldHoverIndex = hoverIndex;

			// clicked node is already expanded
			if (node.index == expandedIndex) {
				if (node.beatmapIndex == focusNode.beatmapIndex) {
					// if already focused, load the beatmap
					if (button != Input.MOUSE_RIGHT_BUTTON)
						startGame();
					else
						SoundController.playSound(SoundEffect.MENUCLICK);
				} else {
					// focus the node
					SoundController.playSound(SoundEffect.MENUCLICK);
					setFocus(node, 0, false, true);
				}
			}

			// clicked node is a new group
			else {
				SoundController.playSound(SoundEffect.MENUCLICK);
				setFocus(node, 0, false, true);
			}

			// restore hover data
			hoverOffset.setTime(oldHoverOffsetTime);
			hoverIndex = oldHoverIndex;

			// open beatmap menu
			if (button == Input.MOUSE_RIGHT_BUTTON)
				beatmapMenuTimer = (node.index == expandedIndex) ? BEATMAP_MENU_DELAY * 4 / 5 : 0;

			return;
		}

		// score buttons
		if (focusScores != null && ScoreData.areaContains(x, y)) {
			int startScore = (int) (startScorePos.getPosition() / ScoreData.getButtonOffset());
			int offset = (int) (-startScorePos.getPosition() + startScore * ScoreData.getButtonOffset());
			int scoreButtons = Math.min(focusScores.length - startScore, MAX_SCORE_BUTTONS + 1);
			for (int i = 0, rank = startScore; i < scoreButtons; i++, rank++) {
				if (ScoreData.buttonContains(x, y - offset, i)) {
					SoundController.playSound(SoundEffect.MENUHIT);
					if (button != Input.MOUSE_RIGHT_BUTTON) {
						// view score
						GameData data = new GameData(focusScores[rank], container.getWidth(), container.getHeight());
						((GameRanking) game.getState(Opsu.STATE_GAMERANKING)).setGameData(data);
						game.enterState(Opsu.STATE_GAMERANKING, new EasedFadeOutTransition(), new FadeInTransition());
					} else {
						// score management
						((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(MenuState.SCORE, focusScores[rank]);
						game.enterState(Opsu.STATE_BUTTONMENU);
					}
					return;
				}
			}
		}
	}

	@Override
	public void keyPressed(int key, char c) {
		// block input
		if ((reloadThread != null && !(key == Input.KEY_ESCAPE || key == Input.KEY_F12)) || beatmapMenuTimer > -1 || isScrollingToFocusNode)
			return;

		if (UI.globalKeyPressed(key))
			return;

		switch (key) {
		case Input.KEY_ESCAPE:
			// Esc: Cancel/back.
			if (reloadThread != null) {
				// beatmap reloading: stop parsing beatmaps by sending interrupt to BeatmapParser
				reloadThread.interrupt();
			} else if (!search.getText().isEmpty()) {
				// clear search text
				search.setText("");
				searchTimer = SEARCH_DELAY;
				searchTransitionTimer = 0;
				searchResultString = null;
				lastSearchTextLength = 0;
			} else {
				// return to main menu
				SoundController.playSound(SoundEffect.MENUBACK);
				((MainMenu) game.getState(Opsu.STATE_MAINMENU)).reset();
				game.enterState(Opsu.STATE_MAINMENU, new EasedFadeOutTransition(), new FadeInTransition());
			}
			break;
		case Input.KEY_F1:
			// F1: Open game mods menu.
			openModsMenu();
			break;
		case Input.KEY_F2:
			// F2: Random song.
			if (Keyboard.isRepeatEvent())
				break;
			randomBeatmap(input.isKeyDown(Input.KEY_RSHIFT) || input.isKeyDown(Input.KEY_LSHIFT));
			break;
		case Input.KEY_F3:
			// F3: Open beatmap options menu.
			openBeatmapOptionsMenu();
			break;
		case Input.KEY_F5:
			// F5: Reload beatmaps.
			SoundController.playSound(SoundEffect.MENUHIT);
			if (songFolderChanged)
				reloadBeatmaps(false);
			else {
				((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(MenuState.RELOAD);
				game.enterState(Opsu.STATE_BUTTONMENU);
			}
			break;
		case Input.KEY_DELETE:
			// Shift+Del: Delete beatmap.
			if (focusNode == null)
				break;
			if (input.isKeyDown(Input.KEY_RSHIFT) || input.isKeyDown(Input.KEY_LSHIFT)) {
				SoundController.playSound(SoundEffect.MENUHIT);
				MenuState ms = (focusNode.beatmapIndex == -1 || focusNode.getBeatmapSet().size() == 1) ?
						MenuState.BEATMAP_DELETE_CONFIRM : MenuState.BEATMAP_DELETE_SELECT;
				((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(ms, focusNode);
				game.enterState(Opsu.STATE_BUTTONMENU);
			}
			break;
		case Input.KEY_ENTER:
			// Enter: Start game.
			if (focusNode == null)
				break;
			startGame();
			break;
		case Input.KEY_DOWN:
			// Down arrow: Scroll down one node.
			if (focusNode == null)
				break;
			if (focusNode.next != null) {
				if (focusNode.next.index == focusNode.index)
					moveFocus(focusNode.next);
				else
					changeIndex(1);
			}
			break;
		case Input.KEY_UP:
			// Up arrow: Scroll up one node.
			if (focusNode == null)
				break;
			if (focusNode.prev != null) {
				if (focusNode.prev.index == focusNode.index)
					moveFocus(focusNode.prev);
				else
					changeIndex(-1);
			}
			break;
		case Input.KEY_RIGHT:
			// Right arrow: Scroll down one map set.
			if (focusNode == null)
				break;
			if (Keyboard.isRepeatEvent())
				break;
			BeatmapSetNode next = focusNode;
			while ((next = next.next) != null && next.index == focusNode.index)
				;
			if (next != null)
				moveFocus(next);
			break;
		case Input.KEY_LEFT:
			// Left arrow: Scroll up one map set.
			if (focusNode == null)
				break;
			if (Keyboard.isRepeatEvent())
				break;
			BeatmapSetNode prev = focusNode;
			while ((prev = prev.prev) != null && prev.index == focusNode.index)
				;
			if (prev != null)
				moveFocus(prev);
			break;
		case Input.KEY_NEXT:
			// PgDown: Scroll down one page.
			changeIndex(MAX_SONG_BUTTONS);
			break;
		case Input.KEY_PRIOR:
			// PgUp: Scroll up one page.
			changeIndex(-MAX_SONG_BUTTONS);
			break;
		default:
			// Ctrl+O: Open options overlay.
			if (key == Input.KEY_O && (input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL))) {
				SoundController.playSound(SoundEffect.MENUHIT);
				showOptionsOverlay = true;
				optionsOverlayProgress.setTime(0);
				optionsOverlay.activate();
				break;
			}

			// wait for user to finish typing
			if (Character.isLetterOrDigit(c) || key == Input.KEY_BACK || key == Input.KEY_SPACE) {
				// load glyphs
				if (c > 255)
					Fonts.loadGlyphs(searchFont, c);

				// reset search timer
				searchTimer = 0;
				searchResultString = null;
				int textLength = search.getText().length();
				if (lastSearchTextLength != textLength) {
					if (key == Input.KEY_BACK) {
						if (textLength == 0)
							searchTransitionTimer = 0;
					} else if (textLength == 1)
						searchTransitionTimer = 0;
					lastSearchTextLength = textLength;
				}
			}
			break;
		}
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		// block input
		if (isInputBlocked())
			return;

		// check mouse button
		if (input.isMouseButtonDown(Input.MOUSE_MIDDLE_BUTTON))
			return;

		int diff = newy - oldy;
		if (diff == 0)
			return;

		// score buttons
		if (focusScores != null && focusScores.length >= MAX_SCORE_BUTTONS && ScoreData.areaContains(oldx, oldy))
			startScorePos.dragged(-diff);

		// song buttons
		else {
			if (songScrolling.isPressed())
				songScrolling.dragged(-diff);
			else if (songScrolling.getSpeedMultiplier() == FAST_SCROLL_SPEED)  // make sure mousePressed() preceded this event
				scrollSongsToPosition(newy);
		}
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		// change volume
		if (UI.globalMouseWheelMoved(newValue, true))
			return;

		// block input
		if (isInputBlocked())
			return;

		int shift = (newValue < 0) ? 1 : -1;
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();

		// score buttons
		if (focusScores != null && focusScores.length >= MAX_SCORE_BUTTONS && ScoreData.areaContains(mouseX, mouseY))
			startScorePos.scrollOffset(ScoreData.getButtonOffset() * shift);

		// song buttons
		else
			changeIndex(shift);
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		Display.setTitle(game.getTitle());
		selectModsButton.resetHover();
		selectRandomButton.resetHover();
		selectMapOptionsButton.resetHover();
		selectOptionsButton.resetHover();
		userButton.resetHover();
		hoverOffset.setTime(0);
		hoverIndex = null;
		isScrollingToFocusNode = false;
		songScrolling.released();
		songScrolling.setSpeedMultiplier(1f);
		startScorePos.setPosition(0);
		beatmapMenuTimer = -1;
		searchTransitionTimer = SEARCH_TRANSITION_TIME;
		songInfo = null;
		lastFadeBeatmap = null;
		if (focusNode != null && focusNode.getSelectedBeatmap().hasLoadedBackground())
			bgAlpha.setTime(bgAlpha.getDuration());
		else
			bgAlpha.setTime(0);
		playfieldAlpha.setTime(0);
		lastBgAlpha.setTime(0);
		songChangeTimer.setTime(songChangeTimer.getDuration());
		musicIconBounceTimer.setTime(musicIconBounceTimer.getDuration());
		starStream.clear();
		sortMenu.activate();
		sortMenu.reset();
		optionsOverlay.deactivate();
		optionsOverlay.reset();
		showOptionsOverlay = false;
		optionsOverlayProgress.setTime(optionsOverlayProgress.getDuration());
		userOverlay.deactivate();
		showUserOverlay = false;
		userOverlayProgress.setTime(userOverlayProgress.getDuration());

		// reset song stack
		randomStack = new Stack<SongNode>();

		// reload beatmaps if song folder changed
		if (songFolderChanged && stateAction != MenuState.RELOAD)
			reloadBeatmaps(false);

		// set focus node if not set (e.g. theme song playing)
		else if (focusNode == null && BeatmapSetList.get().size() > 0)
			setFocus(BeatmapSetList.get().getRandomNode(), -1, true, true);

		// reset music track
		else if (resetTrack) {
			MusicController.pause();
			MusicController.playAt(MusicController.getBeatmap().previewTime, true);
			MusicController.setPitch(1.0f);
			resetTrack = false;
		}

		// unpause track
		else if (MusicController.isPaused())
			MusicController.resume();

		// undim track
		if (MusicController.isTrackDimmed())
			MusicController.toggleTrackDimmed(1f);

		// reset game data
		if (resetGame) {
			((Game) game.getState(Opsu.STATE_GAME)).resetGameData();

			// destroy extra Clips
			MultiClip.destroyExtraClips();

			// destroy skin images, if any
			for (GameImage img : GameImage.values()) {
				if (img.isBeatmapSkinnable())
					img.destroyBeatmapSkinImage();
			}

			// reload scores
			if (focusNode != null) {
				scoreMap = ScoreDB.getMapSetScores(focusNode.getSelectedBeatmap());
				focusScores = getScoreDataForNode(focusNode, true);
			}

			// turn off "auto" mod
			if (GameMod.AUTO.isActive())
				GameMod.AUTO.toggle(false);

			// re-sort (in case play count updated)
			if (BeatmapSortOrder.current() == BeatmapSortOrder.PLAYS) {
				BeatmapSetNode oldFocusBase = BeatmapSetList.get().getBaseNode(focusNode.index);
				int oldFocusFileIndex = focusNode.beatmapIndex;
				focusNode = null;
				BeatmapSetList.get().init();
				setFocus(oldFocusBase, oldFocusFileIndex, true, true);
			}

			resetGame = false;
		}

		// state-based action
		if (stateAction != null) {
			switch (stateAction) {
			case BEATMAP:  // clear all scores
				if (stateActionNode == null || stateActionNode.beatmapIndex == -1)
					break;
				Beatmap beatmap = stateActionNode.getSelectedBeatmap();
				ScoreDB.deleteScore(beatmap);
				if (stateActionNode == focusNode) {
					focusScores = null;
					scoreMap.remove(beatmap.version);
				}
				break;
			case SCORE:  // clear single score
				if (stateActionScore == null)
					break;
				ScoreDB.deleteScore(stateActionScore);
				scoreMap = ScoreDB.getMapSetScores(focusNode.getSelectedBeatmap());
				focusScores = getScoreDataForNode(focusNode, true);
				startScorePos.setPosition(0);
				break;
			case BEATMAP_DELETE_CONFIRM:  // delete song group
				if (stateActionNode == null)
					break;
				BeatmapSetNode
					prev = BeatmapSetList.get().getBaseNode(stateActionNode.index - 1),
					next = BeatmapSetList.get().getBaseNode(stateActionNode.index + 1);
				int oldIndex = stateActionNode.index, focusNodeIndex = focusNode.index, startNodeIndex = startNode.index;
				BeatmapSetList.get().deleteSongGroup(stateActionNode);
				if (oldIndex == focusNodeIndex) {
					if (prev != null)
						setFocus(prev, 0, true, true);
					else if (next != null)
						setFocus(next, 0, true, true);
					else {
						startNode = focusNode = null;
						oldFocusNode = null;
						randomStack = new Stack<SongNode>();
						songInfo = null;
						scoreMap = null;
						focusScores = null;
					}
				} else if (oldIndex == startNodeIndex) {
					if (startNode.prev != null)
						startNode = startNode.prev;
					else if (startNode.next != null)
						startNode = startNode.next;
					else {
						startNode = null;
						songInfo = null;
					}
				}
				break;
			case BEATMAP_DELETE_SELECT:  // delete single song
				if (stateActionNode == null)
					break;
				int index = stateActionNode.index;
				BeatmapSetList.get().deleteSong(stateActionNode);
				if (stateActionNode == focusNode) {
					if (stateActionNode.prev != null &&
					    !(stateActionNode.next != null && stateActionNode.next.index == index))
						setFocus(stateActionNode.prev, 0, true, true);
					else if (stateActionNode.next != null)
						setFocus(stateActionNode.next, 0, true, true);
				} else if (stateActionNode == startNode) {
					if (startNode.prev != null)
						startNode = startNode.prev;
					else if (startNode.next != null)
						startNode = startNode.next;
				}
				break;
			case RELOAD:  // reload beatmaps
				reloadBeatmaps(true);
				break;
			case BEATMAP_FAVORITE:  // removed favorite, reset beatmap list
				if (BeatmapGroup.current() == BeatmapGroup.FAVORITE) {
					startNode = focusNode = null;
					oldFocusNode = null;
					randomStack = new Stack<SongNode>();
					songInfo = null;
					scoreMap = null;
					focusScores = null;
					BeatmapSetList.get().reset();
					BeatmapSetList.get().init();
					setFocus(BeatmapSetList.get().getRandomNode(), 0, true, true);
				}
				break;
			default:
				break;
			}
			stateAction = null;
			stateActionNode = null;
			stateActionScore = null;
		}
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
		search.setFocus(false);
		sortMenu.deactivate();
		optionsOverlay.deactivate();
		optionsOverlay.reset();
		showOptionsOverlay = false;
		userOverlay.deactivate();
		showUserOverlay = false;
	}

	/** Updates the search. */
	private void updateSearch() {
		// don't initially search
		if (lastSearchResultString == null && search.getText().isEmpty())
			return;

		// store the start/focus nodes
		if (focusNode != null)
			oldFocusNode = new SongNode(BeatmapSetList.get().getBaseNode(focusNode.index), focusNode.beatmapIndex);

		if (BeatmapSetList.get().search(search.getText())) {
			// reset song stack
			randomStack = new Stack<SongNode>();

			// empty search
			if (search.getText().isEmpty())
				searchResultString = null;

			// search produced new list: re-initialize it
			startNode = focusNode = null;
			scoreMap = null;
			focusScores = null;
			int size = BeatmapSetList.get().size();
			if (size > 0) {
				BeatmapSetList.get().init();
				String results = String.format("%d match%s found!", size, (size == 1) ? "" : "es");
				if (search.getText().isEmpty()) {  // cleared search
					// use previous start/focus if possible
					if (oldFocusNode != null) {
						setFocus(oldFocusNode.getNode(), oldFocusNode.getIndex(), true, true);
						songChangeTimer.setTime(songChangeTimer.getDuration());
						musicIconBounceTimer.setTime(musicIconBounceTimer.getDuration());
					} else
						setFocus(BeatmapSetList.get().getRandomNode(), 0, true, true);
				} else {
					searchResultString = results;
					setFocus(BeatmapSetList.get().getRandomNode(), 0, true, true);
				}
				oldFocusNode = null;
				lastSearchResultString = results;
			} else if (!search.getText().isEmpty())
				searchResultString = lastSearchResultString = "No matches found. Hit ESC to reset.";
		} else
			searchResultString = lastSearchResultString;
	}

	/**
	 * Shifts the focus node to a new node.
	 * @param node the new node
	 */
	private void moveFocus(BeatmapSetNode node) {
		SoundController.playSound(SoundEffect.MENUCLICK);
		BeatmapSetNode oldStartNode = startNode;
		int oldHoverOffsetTime = hoverOffset.getTime();
		BeatmapSetNode oldHoverIndex = hoverIndex;
		setFocus(node, 0, false, true);
		if (startNode == oldStartNode) {
			hoverOffset.setTime(oldHoverOffsetTime);
			hoverIndex = oldHoverIndex;
		}
	}

	/**
	 * Focuses a random beatmap.
	 * @param previous if true, pops from the random track stack instead
	 */
	private void randomBeatmap(boolean previous) {
		if (focusNode == null)
			return;

		SoundController.playSound(SoundEffect.MENUHIT);
		if (previous) {
			// shift key: previous random track
			SongNode prev;
			if (randomStack.isEmpty() || (prev = randomStack.pop()) == null)
				return;
			BeatmapSetNode node = prev.getNode();
			int expandedIndex = BeatmapSetList.get().getExpandedIndex();
			if (node.index == expandedIndex)
				node = node.next;  // move past base node
			setFocus(node, prev.getIndex(), true, true);
		} else {
			// random track, add previous to stack
			randomStack.push(new SongNode(BeatmapSetList.get().getBaseNode(focusNode.index), focusNode.beatmapIndex));
			setFocus(BeatmapSetList.get().getRandomNode(), 0, true, true);
		}
	}

	/**
	 * Shifts the scroll position forward (+) or backwards (-) by a given number
	 * of nodes.
	 * @param shift the number of nodes to shift
	 */
	private void changeIndex(int shift) {
		if (shift == 0)
			return;

		songScrolling.scrollOffset(shift * buttonOffset);
	}

	/**
	 * Updates the song list data required for drawing.
	 */
	private void updateDrawnSongPosition() {
		float songNodePosDrawn = songScrolling.getPosition();

		int startNodeIndex = (int) (songNodePosDrawn / buttonOffset);
		buttonY = -songNodePosDrawn + buttonOffset * startNodeIndex + headerY - DIVIDER_LINE_WIDTH;

		float max = (BeatmapSetList.get().size() + (focusNode != null ? focusNode.getBeatmapSet().size() : 0));
		songScrolling.setMinMax(0 - buttonOffset * 2, (max - MAX_SONG_BUTTONS - 1 + 2) * buttonOffset);

		// negative startNodeIndex means the first Node is below the header so offset it.
		if (startNodeIndex <= 0) {
			startNodeOffset = -startNodeIndex;
			startNodeIndex = 0;
		} else {
			startNodeOffset = 0;
		}

		// Finds the start node with the expanded focus node in mind.
		if (focusNode != null && startNodeIndex >= focusNode.index) {
			// below the focus node.
			if (startNodeIndex <= focusNode.index + focusNode.getBeatmapSet().size()) {
				// inside the focus nodes expanded nodes.
				int nodeIndex = startNodeIndex - focusNode.index;
				startNode = BeatmapSetList.get().getBaseNode(focusNode.index);
				startNode = startNode.next;
				for (int i = 0; i < nodeIndex; i++)
					startNode = startNode.next;
			} else {
				startNodeIndex -= focusNode.getBeatmapSet().size() - 1;
				startNode = BeatmapSetList.get().getBaseNode(startNodeIndex);
			}
		} else
			startNode = BeatmapSetList.get().getBaseNode(startNodeIndex);
	}

	/**
	 * Sets a new focus node.
	 * @param node the new node to focus
	 * @param beatmapIndex the beatmap element to focus (if out of bounds, randomly chosen)
	 * @param forceFastScroll if fast scroll should always be used to scroll to the new focus node
	 * @param preview whether to start at the preview time (true) or beginning (false)
	 * @return the old focus node
	 */
	public BeatmapSetNode setFocus(BeatmapSetNode node, int beatmapIndex, boolean forceFastScroll, boolean preview) {
		if (node == null)
			return null;

		hoverOffset.setTime(0);
		hoverIndex = null;
		songInfo = null;
		songChangeTimer.setTime(0);
		musicIconBounceTimer.setTime(0);
		BeatmapSetNode oldFocus = focusNode;

		// expand node before focusing it
		int expandedIndex = BeatmapSetList.get().getExpandedIndex();
		if (node.index != expandedIndex) {
			node = BeatmapSetList.get().expand(node.index);

			// calculate difficulties
			calculateStarRatings(node.getBeatmapSet());

			// if start node was previously expanded, move it
			if (startNode != null && startNode.index == expandedIndex)
				startNode = BeatmapSetList.get().getBaseNode(startNode.index);
		}

		// check beatmap index bounds
		int length = node.getBeatmapSet().size();
		if (beatmapIndex < 0 || beatmapIndex > length - 1)  // set a random index
			beatmapIndex = (int) (Math.random() * length);

		// focus the node
		focusNode = BeatmapSetList.get().getNode(node, beatmapIndex);
		Beatmap beatmap = focusNode.getSelectedBeatmap();
		if (beatmap.timingPoints == null) {
			// load timing points so we can pulse the logo
			BeatmapDB.load(beatmap, BeatmapDB.LOAD_ARRAY);
		}
		MusicController.play(beatmap, false, preview);

		// load scores
		scoreMap = ScoreDB.getMapSetScores(beatmap);
		focusScores = getScoreDataForNode(focusNode, true);
		startScorePos.setPosition(0);

		// change the scroll position
		float position = buttonOffset *
			(focusNode.index + focusNode.beatmapIndex - MAX_SONG_BUTTONS / 2 +
			0.5f * ((MAX_SONG_BUTTONS + 1) % 2));
		if (startNode == null || game.getCurrentStateID() != Opsu.STATE_SONGMENU)
			songScrolling.setPosition(position);
		else {
			songScrolling.scrollToPosition(position);
			if (forceFastScroll || (position - songScrolling.getPosition()) / buttonOffset > MAX_SONG_BUTTONS / 2f) {
				isScrollingToFocusNode = true;
				songScrolling.setSpeedMultiplier(FAST_SCROLL_SPEED);
				songScrolling.released();
			}
		}

		updateDrawnSongPosition();

		// load background image
		beatmap.loadBackground();
		lastFadeBeatmap = lastBeatmap;
		lastBeatmap = beatmap;
		boolean lastBgExists = lastFadeBeatmap != null && lastFadeBeatmap.hasLoadedBackground();
		boolean thisBgExists = beatmap.bg != null;
		if (lastBgExists != thisBgExists || (lastBgExists && thisBgExists && !beatmap.bg.equals(lastFadeBeatmap.bg))) {
			bgAlpha.setTime(0);
			playfieldAlpha.setTime(lastBgExists ? 0 : playfieldAlpha.getDuration());
			lastBgAlpha.setTime(0);
		}

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
	 * Performs an action based on a menu state upon entering this state.
	 * @param menuState the menu state determining the action
	 */
	public void doStateActionOnLoad(MenuState menuState) { doStateActionOnLoad(menuState, null, null); }

	/**
	 * Performs an action based on a menu state upon entering this state.
	 * @param menuState the menu state determining the action
	 * @param node the song node to perform the action on
	 */
	public void doStateActionOnLoad(MenuState menuState, BeatmapSetNode node) {
		doStateActionOnLoad(menuState, node, null);
	}

	/**
	 * Performs an action based on a menu state upon entering this state.
	 * @param menuState the menu state determining the action
	 * @param scoreData the score data to perform the action on
	 */
	public void doStateActionOnLoad(MenuState menuState, ScoreData scoreData) {
		doStateActionOnLoad(menuState, null, scoreData);
	}

	/**
	 * Performs an action based on a menu state upon entering this state.
	 * @param menuState the menu state determining the action
	 * @param node the song node to perform the action on
	 * @param scoreData the score data to perform the action on
	 */
	private void doStateActionOnLoad(MenuState menuState, BeatmapSetNode node, ScoreData scoreData) {
		stateAction = menuState;
		stateActionNode = node;
		stateActionScore = scoreData;
	}

	/**
	 * Returns all the score data for an BeatmapSetNode from scoreMap.
	 * If no score data is available for the node, return null.
	 * @param node the BeatmapSetNode
	 * @param setTimeSince whether or not to set the "time since" field for the scores
	 * @return the ScoreData array
	 */
	private ScoreData[] getScoreDataForNode(BeatmapSetNode node, boolean setTimeSince) {
		if (scoreMap == null || scoreMap.isEmpty() || node.beatmapIndex == -1)  // node not expanded
			return null;

		Beatmap beatmap = node.getSelectedBeatmap();
		ScoreData[] scores = scoreMap.get(beatmap.version);
		if (scores == null || scores.length < 1)  // no scores
			return null;

		ScoreData s = scores[0];
		if (beatmap.beatmapID == s.MID && beatmap.beatmapSetID == s.MSID &&
		    beatmap.title.equals(s.title) && beatmap.artist.equals(s.artist) &&
		    beatmap.creator.equals(s.creator)) {
			for (int i = 0; i < scores.length; i++) {
				if (setTimeSince)
					scores[i].getTimeSince();
				scores[i].loadGlyphs();
			}
			return scores;
		} else
			return null;  // incorrect map
	}

	/**
	 * Reloads all beatmaps.
	 * @param fullReload if true, also clear the beatmap cache and invoke the unpacker
	 */
	private void reloadBeatmaps(final boolean fullReload) {
		songFolderChanged = false;

		// reset state and node references
		MusicController.reset();
		startNode = focusNode = null;
		scoreMap = null;
		focusScores = null;
		oldFocusNode = null;
		randomStack = new Stack<SongNode>();
		songInfo = null;
		hoverOffset.setTime(0);
		hoverIndex = null;
		search.setText("");
		searchTimer = SEARCH_DELAY;
		searchTransitionTimer = SEARCH_TRANSITION_TIME;
		searchResultString = null;
		lastSearchResultString = null;
		lastBeatmap = null;
		lastFadeBeatmap = null;
		lastSearchTextLength = 0;

		// reload songs in new thread
		reloadThread = new BeatmapReloadThread(fullReload);
		reloadThread.start();
	}

	/**
	 * Returns whether a delayed/animated event is currently blocking user input.
	 * @return true if blocking input
	 */
	private boolean isInputBlocked() {
		return (reloadThread != null || beatmapMenuTimer > -1 || isScrollingToFocusNode);
	}

	/**
	 * Returns the beatmap node at the given location.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return the node, or {@code null} if none
	 */
	private BeatmapSetNode getNodeAtPosition(int x, int y) {
		if (y <= headerY || y >= footerY)
			return null;

		int expandedIndex = BeatmapSetList.get().getExpandedIndex();
		BeatmapSetNode node = startNode;
		for (int i = startNodeOffset; i < MAX_SONG_BUTTONS + 1 && node != null; i++, node = node.next) {
			float cx = (node.index == expandedIndex) ? buttonX * 0.9f : buttonX;
			if ((x > cx && x < cx + buttonWidth) &&
				(y > buttonY + (i * buttonOffset) && y < buttonY + (i * buttonOffset) + buttonHeight))
				return node;
		}
		return null;
	}

	/**
	 * Scrolls the song list to the given y position.
	 * @param y the y coordinate (will be clamped)
	 */
	private void scrollSongsToPosition(int y) {
		float scrollBase = headerY + DIVIDER_LINE_WIDTH / 2;
		float scrollHeight = MAX_SONG_BUTTONS * buttonOffset;
		float t = Utils.clamp((y - scrollBase) / scrollHeight, 0f, 1f);
		songScrolling.scrollToPosition(songScrolling.getMin() + t * (songScrolling.getMax() - songScrolling.getMin()));
	}

	/**
	 * Calculates all star ratings for a beatmap set.
	 * @param beatmapSet the set of beatmaps
	 */
	private void calculateStarRatings(BeatmapSet beatmapSet) {
		for (Beatmap beatmap : beatmapSet) {
			if (beatmap.starRating >= 0) {  // already calculated
				beatmapsCalculated.put(beatmap, beatmapsCalculated.get(beatmap));
				continue;
			}

			// if timing points are already loaded before this (for whatever reason),
			// don't clear the array fields to be safe
			boolean hasTimingPoints = (beatmap.timingPoints != null);

			BeatmapDifficultyCalculator diffCalc = new BeatmapDifficultyCalculator(beatmap);
			diffCalc.calculate();
			if (diffCalc.getStarRating() == -1)
				continue;  // calculations failed

			// save star rating
			beatmap.starRating = diffCalc.getStarRating();
			BeatmapDB.setStars(beatmap);
			beatmapsCalculated.put(beatmap, !hasTimingPoints);
		}
	}

	/** Enters the game mods menu. */
	private void openModsMenu() {
		SoundController.playSound(SoundEffect.MENUHIT);
		((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(MenuState.MODS);
		game.enterState(Opsu.STATE_BUTTONMENU);
	}

	/** Enters the beatmap options menu. */
	private void openBeatmapOptionsMenu() {
		if (focusNode == null)
			return;

		SoundController.playSound(SoundEffect.MENUHIT);
		MenuState state = focusNode.getBeatmapSet().isFavorite() ?
			MenuState.BEATMAP_FAVORITE : MenuState.BEATMAP;
		((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(state, focusNode);
		game.enterState(Opsu.STATE_BUTTONMENU);
	}

	/**
	 * Starts the game.
	 */
	private void startGame() {
		if (MusicController.isTrackLoading())
			return;

		Beatmap beatmap = MusicController.getBeatmap();
		if (focusNode == null || beatmap != focusNode.getSelectedBeatmap()) {
			UI.getNotificationManager().sendBarNotification("Unable to load the beatmap audio.");
			return;
		}

		// turn on "auto" mod if holding "ctrl" key
		if (input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) {
			if (!GameMod.AUTO.isActive())
				GameMod.AUTO.toggle(true);
		}

		SoundController.playSound(SoundEffect.MENUHIT);
		MultiClip.destroyExtraClips();
		Game gameState = (Game) game.getState(Opsu.STATE_GAME);
		gameState.loadBeatmap(beatmap);
		gameState.setPlayState(Game.PlayState.FIRST_LOAD);
		gameState.setReplay(null);
		game.enterState(Opsu.STATE_GAME, new EasedFadeOutTransition(), new FadeInTransition());
	}
}
