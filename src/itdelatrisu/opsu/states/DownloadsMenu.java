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
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.beatmap.BeatmapSetNode;
import itdelatrisu.opsu.beatmap.OszUnpacker;
import itdelatrisu.opsu.downloads.Download;
import itdelatrisu.opsu.downloads.DownloadList;
import itdelatrisu.opsu.downloads.DownloadNode;
import itdelatrisu.opsu.downloads.servers.BloodcatServer;
import itdelatrisu.opsu.downloads.servers.DownloadServer;
import itdelatrisu.opsu.downloads.servers.HexideServer;
import itdelatrisu.opsu.downloads.servers.MnetworkServer;
import itdelatrisu.opsu.downloads.servers.RippleServer;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.DropdownMenu;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.KineticScrolling;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.NotificationManager.NotificationListener;
import itdelatrisu.opsu.ui.UI;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.gui.TextField;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.EasedFadeOutTransition;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.util.Log;

/**
 * Downloads menu.
 * <p>
 * Players are able to download beatmaps off of various servers and import them
 * from this state.
 */
public class DownloadsMenu extends BasicGameState {
	/** Delay time, in milliseconds, between each search. */
	private static final int SEARCH_DELAY = 700;

	/** Delay time, in milliseconds, for double-clicking focused result. */
	private static final int FOCUS_DELAY = 250;

	/** Minimum time, in milliseconds, that must elapse between queries. */
	private static final int MIN_REQUEST_INTERVAL = 300;

	/** Available beatmap download servers. */
	private static final DownloadServer[] SERVERS = {
		new RippleServer(),
		new MnetworkServer(),
		new HexideServer(),
		new BloodcatServer()
	};

	/** The current list of search results. */
	private DownloadNode[] resultList;

	/** Current focused (selected) result. */
	private int focusResult = -1;

	/** Delay time, in milliseconds, for double-clicking focused result. */
	private int focusTimer = 0;

	/** Current start result button (topmost entry). */
	private KineticScrolling startResultPos = new KineticScrolling();

	/** Total number of results for current query. */
	private int totalResults = 0;

	/** Page of current query results. */
	private int page = 1;

	/** Total number of results across pages seen so far. */
	private int pageResultTotal = 0;

	/** Page navigation. */
	private enum Page { RESET, CURRENT, PREVIOUS, NEXT };

	/** Page direction for next query. */
	private Page pageDir = Page.RESET;

	/** Whether to only show ranked maps. */
	private boolean rankedOnly = true;

	/** Current start download index. */
	private KineticScrolling startDownloadIndexPos = new KineticScrolling();

	/** Query thread. */
	private Thread queryThread;

	/** The search textfield. */
	private TextField search;

	/** The search font. */
	private UnicodeFont searchFont;

	/**
	 * Delay timer, in milliseconds, before running another search.
	 * This is overridden by character entry (reset) and 'esc'/'enter' (immediate search).
	 */
	private int searchTimer;

	/** Information text to display based on the search query. */
	private String searchResultString;

	/** Whether or not the search timer has been manually reset; reset after search delay passes. */
	private boolean searchTimerReset = false;

	/** The last search query. */
	private String lastQuery;

	/** Page direction for last query. */
	private Page lastQueryDir = Page.RESET;

	/** Previous and next page buttons. */
	private MenuButton prevPage, nextPage;

	/** Buttons. */
	private MenuButton clearButton, importButton, resetButton, rankedButton;

	/** Dropdown menu. */
	private DropdownMenu<DownloadServer> serverMenu;

	/** Beatmap importing thread. */
	private BeatmapImportThread importThread;

	/** Beatmap set ID of the current beatmap being previewed, or -1 if none. */
	private int previewID = -1;

	/** The bar notification to send upon entering the state. */
	private String barNotificationOnLoad;

	/** Search query, executed in {@code queryThread}. */
	private SearchQuery searchQuery;

	/** Search query helper class. */
	private class SearchQuery implements Runnable {
		/** The search query. */
		private final String query;

		/** The download server. */
		private final DownloadServer server;

		/** Whether the query was interrupted. */
		private boolean interrupted = false;

		/** Whether the query has completed execution. */
		private boolean complete = false;

		/**
		 * Constructor.
		 * @param query the search query
		 * @param server the download server
		 */
		public SearchQuery(String query, DownloadServer server) {
			this.query = query;
			this.server = server;
		}

		/** Interrupt the query and prevent the results from being processed, if not already complete. */
		public void interrupt() { interrupted = true; }

		/** Returns whether the query has completed execution. */
		public boolean isComplete() { return complete; }

		@Override
		public void run() {
			// check page direction
			Page lastPageDir = pageDir;
			pageDir = Page.RESET;
			int lastPageSize = (resultList != null) ? resultList.length : 0;
			int newPage = page;
			if (lastPageDir == Page.RESET)
				newPage = 1;
			else if (lastPageDir == Page.NEXT)
				newPage++;
			else if (lastPageDir == Page.PREVIOUS)
				newPage--;
			try {
				DownloadNode[] nodes = server.resultList(query, newPage, rankedOnly);
				if (!interrupted) {
					// update page total
					page = newPage;
					if (nodes != null) {
						if (lastPageDir == Page.NEXT)
							pageResultTotal += nodes.length;
						else if (lastPageDir == Page.PREVIOUS)
							pageResultTotal -= lastPageSize;
						else if (lastPageDir == Page.RESET)
							pageResultTotal = nodes.length;
					} else
						pageResultTotal = 0;

					resultList = nodes;
					totalResults = server.totalResults();
					focusResult = -1;
					startResultPos.setPosition(0);
					if (nodes == null)
						searchResultString = "An error occurred. See log for details.";
					else {
						if (query.isEmpty())
							searchResultString = "Type to search!";
						else if (totalResults == 0 || resultList.length == 0)
							searchResultString = "No results found.";
						else
							searchResultString = String.format("%d result%s found!",
									totalResults, (totalResults == 1) ? "" : "s");
					}
				}
			} catch (IOException e) {
				if (!interrupted)
					searchResultString = "Could not establish connection to server.";
			} finally {
				complete = true;
			}
		}
	}

	/** Thread for importing packed beatmaps. */
	private class BeatmapImportThread extends Thread {
		/** Whether this thread has completed execution. */
		private boolean finished = false;

		/** The last imported beatmap set node, if any. */
		private BeatmapSetNode importedNode;

		/** Returns true only if this thread has completed execution. */
		public boolean isFinished() { return finished; }

		/** Returns an imported beatmap set node, or null if none. */
		public BeatmapSetNode getImportedBeatmap() { return importedNode; }

		@Override
		public void run() {
			try {
				importBeatmaps();
			} finally {
				finished = true;
			}
		};

		/** Imports all packed beatmaps. */
		private void importBeatmaps() {
			// invoke unpacker and parser
			File[] dirs = OszUnpacker.unpackAllFiles(Options.getImportDir(), Options.getBeatmapDir());
			if (dirs != null && dirs.length > 0) {
				this.importedNode = BeatmapParser.parseDirectories(dirs);
				if (importedNode == null)
					UI.getNotificationManager().sendNotification("No Standard beatmaps could be loaded.", Color.red);
			}

			DownloadList.get().clearDownloads(Download.Status.COMPLETE);
		}
	}

	// game-related variables
	private StateBasedGame game;
	private Input input;
	private final int state;

	public DownloadsMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.game = game;
		this.input = container.getInput();

		int width = container.getWidth();
		int height = container.getHeight();
		float baseX = width * 0.024f;
		float searchY = (height * 0.04f) + Fonts.LARGE.getLineHeight();
		float searchWidth = width * 0.3f;

		// search
		searchTimer = SEARCH_DELAY;
		searchResultString = "Loading data from server...";
		searchFont = Fonts.DEFAULT;
		search = new TextField(
				container, searchFont, (int) baseX, (int) searchY,
				(int) searchWidth, Fonts.MEDIUM.getLineHeight()
		);
		search.setBackgroundColor(Colors.BLACK_BG_NORMAL);
		search.setBorderColor(Color.white);
		search.setTextColor(Color.white);
		search.setConsumeEvents(false);
		search.setMaxLength(255);

		// page buttons
		float pageButtonY = height * 0.2f;
		float pageButtonWidth = width * 0.7f;
		Image prevImg = GameImage.MUSIC_PREVIOUS.getImage();
		Image nextImg = GameImage.MUSIC_NEXT.getImage();
		prevPage = new MenuButton(prevImg, baseX + prevImg.getWidth() / 2f,
				pageButtonY - prevImg.getHeight() / 2f);
		nextPage = new MenuButton(nextImg, baseX + pageButtonWidth - nextImg.getWidth() / 2f,
				pageButtonY - nextImg.getHeight() / 2f);
		prevPage.setHoverExpand(1.5f);
		nextPage.setHoverExpand(1.5f);

		// buttons
		float buttonMarginX = width * 0.004f;
		float buttonHeight = height * 0.038f;
		float resetWidth = width * 0.085f;
		float rankedWidth = width * 0.15f;
		float lowerWidth = width * 0.12f;
		float topButtonY = searchY + Fonts.MEDIUM.getLineHeight() / 2f;
		float lowerButtonY = height * 0.995f - searchY - buttonHeight / 2f;
		Image button = GameImage.MENU_BUTTON_MID.getImage();
		Image buttonL = GameImage.MENU_BUTTON_LEFT.getImage();
		Image buttonR = GameImage.MENU_BUTTON_RIGHT.getImage();
		buttonL = buttonL.getScaledCopy(buttonHeight / buttonL.getHeight());
		buttonR = buttonR.getScaledCopy(buttonHeight / buttonR.getHeight());
		int lrButtonWidth = buttonL.getWidth() + buttonR.getWidth();
		Image resetButtonImage = button.getScaledCopy((int) resetWidth - lrButtonWidth, (int) buttonHeight);
		Image rankedButtonImage = button.getScaledCopy((int) rankedWidth - lrButtonWidth, (int) buttonHeight);
		Image lowerButtonImage = button.getScaledCopy((int) lowerWidth - lrButtonWidth, (int) buttonHeight);
		float resetButtonWidth = resetButtonImage.getWidth() + lrButtonWidth;
		float rankedButtonWidth = rankedButtonImage.getWidth() + lrButtonWidth;
		float lowerButtonWidth = lowerButtonImage.getWidth() + lrButtonWidth;
		clearButton = new MenuButton(lowerButtonImage, buttonL, buttonR,
				width * 0.75f + buttonMarginX + lowerButtonWidth / 2f, lowerButtonY);
		importButton = new MenuButton(lowerButtonImage, buttonL, buttonR,
				width - buttonMarginX - lowerButtonWidth / 2f, lowerButtonY);
		resetButton = new MenuButton(resetButtonImage, buttonL, buttonR,
				baseX + searchWidth + buttonMarginX + resetButtonWidth / 2f, topButtonY);
		rankedButton = new MenuButton(rankedButtonImage, buttonL, buttonR,
				baseX + searchWidth + buttonMarginX * 2f + resetButtonWidth + rankedButtonWidth / 2f, topButtonY);
		clearButton.setText("Clear", Fonts.MEDIUM, Color.white);
		importButton.setText("Import All", Fonts.MEDIUM, Color.white);
		resetButton.setText("Reset", Fonts.MEDIUM, Color.white);
		clearButton.setHoverFade();
		importButton.setHoverFade();
		resetButton.setHoverFade();
		rankedButton.setHoverFade();

		// dropdown menu
		int serverWidth = (int) (width * 0.12f);
		serverMenu = new DropdownMenu<DownloadServer>(container, SERVERS,
				baseX + searchWidth + buttonMarginX * 3f + resetButtonWidth + rankedButtonWidth, searchY, serverWidth) {
			@Override
			public void itemSelected(int index, DownloadServer item) {
				resultList = null;
				startResultPos.setPosition(0);
				focusResult = -1;
				totalResults = 0;
				page = 0;
				pageResultTotal = 1;
				pageDir = Page.RESET;
				searchResultString = "Loading data from server...";
				lastQuery = null;
				pageDir = Page.RESET;
				if (searchQuery != null)
					searchQuery.interrupt();
				resetSearchTimer();
			}

			@Override
			public boolean menuClicked(int index) {
				// block input during beatmap importing
				if (importThread != null)
					return false;

				SoundController.playSound(SoundEffect.MENUCLICK);
				return true;
			}
		};
		serverMenu.setBackgroundColor(Colors.BLACK_BG_HOVER);
		serverMenu.setBorderColor(Color.black);
		serverMenu.setChevronRightColor(Color.white);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		boolean inDropdownMenu = serverMenu.contains(mouseX, mouseY);

		// background
		Image bg = GameImage.SEARCH_BG.getImage();
		if (Options.isParallaxEnabled()) {
			int offset = (int) (height * (GameImage.PARALLAX_SCALE - 1f));
			float parallaxX = -offset / 2f * (mouseX - width / 2) / (width / 2);
			float parallaxY = -offset / 2f * (mouseY - height / 2) / (height / 2);
			bg = bg.getScaledCopy(GameImage.PARALLAX_SCALE);
			bg.drawCentered(width / 2 + parallaxX, height / 2 + parallaxY);
		} else
			bg.drawCentered(width / 2, height / 2);

		// title
		Fonts.LARGE.drawString(width * 0.024f, height * 0.03f, "Download Beatmaps!", Color.white);

		// search
		g.setColor(Color.white);
		g.setLineWidth(2f);
		search.render(container, g);
		Fonts.BOLD.drawString(
				search.getX() + search.getWidth() * 0.01f, search.getY() + search.getHeight() * 1.3f,
				searchResultString, Color.white
		);

		// search results
		DownloadNode[] nodes = resultList;
		if (nodes != null) {
			DownloadNode.clipToResultArea(g);
			int maxResultsShown = DownloadNode.maxResultsShown();
			int startResult = (int) (startResultPos.getPosition() / DownloadNode.getButtonOffset());
			int offset = (int) (-startResultPos.getPosition() + startResult * DownloadNode.getButtonOffset());

			for (int i = 0; i < maxResultsShown + 1; i++) {
				int index = startResult + i;
				if (index < 0)
					continue;
				if (index >= nodes.length)
					break;
				nodes[index].drawResult(g, offset + i * DownloadNode.getButtonOffset(),
						DownloadNode.resultContains(mouseX, mouseY - offset, i) && !inDropdownMenu,
						(index == focusResult), (previewID == nodes[index].getID()));
			}
			g.clearClip();

			// scroll bar
			if (nodes.length > maxResultsShown)
				DownloadNode.drawResultScrollbar(g, startResultPos.getPosition(), nodes.length * DownloadNode.getButtonOffset());

			// pages
			if (nodes.length > 0) {
				float baseX = width * 0.024f;
				float buttonY = height * 0.2f;
				float buttonWidth = width * 0.7f;
				Fonts.BOLD.drawString(
						baseX + (buttonWidth - Fonts.BOLD.getWidth("Page 1")) / 2f,
						buttonY - Fonts.BOLD.getLineHeight() * 1.3f,
						String.format("Page %d", page), Color.white
				);
				if (page > 1)
					prevPage.draw();
				if (pageResultTotal < totalResults)
					nextPage.draw();
			}
		}

		// downloads
		float downloadsX = width * 0.75f, downloadsY = search.getY();
		g.setColor(Colors.BLACK_BG_NORMAL);
		g.fillRect(downloadsX, downloadsY,
				width * 0.25f, height - downloadsY * 2f);
		Fonts.LARGE.drawString(downloadsX + width * 0.015f, downloadsY + height * 0.015f, "Downloads", Color.white);
		int downloadsSize = DownloadList.get().size();
		if (downloadsSize > 0) {
			int maxDownloadsShown = DownloadNode.maxDownloadsShown();
			int startDownloadIndex = (int) (startDownloadIndexPos.getPosition() / DownloadNode.getInfoHeight());
			int offset = (int) (-startDownloadIndexPos.getPosition() + startDownloadIndex * DownloadNode.getInfoHeight());

			DownloadNode.clipToDownloadArea(g);
			for (int i = 0; i < maxDownloadsShown + 1; i++) {
				int index = startDownloadIndex + i;
				if (index >= downloadsSize)
					break;
				DownloadNode node = DownloadList.get().getNode(index);
				if (node == null)
					break;
				node.drawDownload(g, i * DownloadNode.getInfoHeight() + offset, index,
						DownloadNode.downloadContains(mouseX, mouseY - offset, i));
			}
			g.clearClip();

			// scroll bar
			if (downloadsSize > maxDownloadsShown)
				DownloadNode.drawDownloadScrollbar(g, startDownloadIndexPos.getPosition(), downloadsSize * DownloadNode.getInfoHeight());
		}

		// buttons
		clearButton.draw(Color.gray);
		importButton.draw(Color.orange);
		resetButton.draw(Color.red);
		rankedButton.setText((rankedOnly) ? "Show Unranked" : "Hide Unranked", Fonts.MEDIUM, Color.white);
		rankedButton.draw(Color.magenta);

		// dropdown menu
		serverMenu.render(container, g);

		// importing beatmaps
		if (importThread != null) {
			// darken the screen
			g.setColor(Colors.BLACK_ALPHA);
			g.fillRect(0, 0, width, height);

			UI.drawLoadingProgress(g, 1f);
		}

		// back button
		else
			UI.getBackButton().draw(g);

		UI.draw(g);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		UI.update(delta);
		if (importThread == null)
			MusicController.loopTrackIfEnded(false);
		else if (importThread.isFinished()) {
			BeatmapSetNode importedNode = importThread.getImportedBeatmap();
			if (importedNode != null) {
				// stop preview
				previewID = -1;
				SoundController.stopTrack();

				// initialize song list
				BeatmapSetList.get().reset();
				BeatmapSetList.get().init();

				// focus new beatmap
				// NOTE: This can't be called in another thread because it makes OpenGL calls.
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).setFocus(importedNode, -1, true, true);
			}
			importThread = null;
		}
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		UI.getBackButton().hoverUpdate(delta, mouseX, mouseY);
		prevPage.hoverUpdate(delta, mouseX, mouseY);
		nextPage.hoverUpdate(delta, mouseX, mouseY);
		clearButton.hoverUpdate(delta, mouseX, mouseY);
		importButton.hoverUpdate(delta, mouseX, mouseY);
		resetButton.hoverUpdate(delta, mouseX, mouseY);
		rankedButton.hoverUpdate(delta, mouseX, mouseY);

		if (DownloadList.get() != null)
			startDownloadIndexPos.setMinMax(0, DownloadNode.getInfoHeight() * (DownloadList.get().size() -  DownloadNode.maxDownloadsShown()));
		startDownloadIndexPos.update(delta);
		if (resultList != null)
			startResultPos.setMinMax(0, DownloadNode.getButtonOffset() * (resultList.length - DownloadNode.maxResultsShown()));
		startResultPos.update(delta);

		// focus timer
		if (focusResult != -1 && focusTimer < FOCUS_DELAY)
			focusTimer += delta;

		// search
		search.setFocus(true);
		searchTimer += delta;
		if (searchTimer >= SEARCH_DELAY && importThread == null) {
			searchTimer = 0;
			searchTimerReset = false;

			String query = search.getText().trim().toLowerCase();
			DownloadServer server = serverMenu.getSelectedItem();
			if ((lastQuery == null || !query.equals(lastQuery)) &&
			    (query.length() == 0 || query.length() >= server.minQueryLength())) {
				lastQuery = query;
				lastQueryDir = pageDir;

				if (queryThread != null && queryThread.isAlive()) {
					queryThread.interrupt();
					if (searchQuery != null)
						searchQuery.interrupt();
				}

				// execute query
				searchQuery = new SearchQuery(query, server);
				queryThread = new Thread(searchQuery);
				queryThread.start();
			}
		}

		// tooltips
		if (resetButton.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Reset the current search.", false);
		else if (rankedButton.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Toggle the display of unranked maps.\nSome download servers may not support this option.", true);
		else if (serverMenu.baseContains(mouseX, mouseY))
			UI.updateTooltip(delta, "Select a download server.", false);
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// block input during beatmap importing
		if (importThread != null)
			return;

		// back
		if (UI.getBackButton().contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			((MainMenu) game.getState(Opsu.STATE_MAINMENU)).reset();
			game.enterState(Opsu.STATE_MAINMENU, new EasedFadeOutTransition(), new FadeInTransition());
			return;
		}

		// search results
		DownloadNode[] nodes = resultList;
		if (nodes != null) {
			if (DownloadNode.resultAreaContains(x, y)) {
				startResultPos.pressed();
				int maxResultsShown = DownloadNode.maxResultsShown();
				for (int i = 0; i < maxResultsShown + 1; i++) {
					int startResult = (int) (startResultPos.getPosition() / DownloadNode.getButtonOffset());
					int offset = (int) (-startResultPos.getPosition() + startResult * DownloadNode.getButtonOffset());

					int index = startResult + i;
					if (index >= nodes.length)
						break;
					if (DownloadNode.resultContains(x, y - offset, i)) {
						final DownloadNode node = nodes[index];

						// check if map is already loaded
						boolean isLoaded = BeatmapSetList.get().containsBeatmapSetID(node.getID());

						// track preview
						if (DownloadNode.resultIconContains(x, y - offset, i)) {
							// set focus
							if (!isLoaded) {
								SoundController.playSound(SoundEffect.MENUCLICK);
								focusResult = index;
								focusTimer = FOCUS_DELAY;
							}

							if (previewID == node.getID()) {
								// stop preview
								previewID = -1;
								SoundController.stopTrack();
							} else {
								// play preview
								final String url = serverMenu.getSelectedItem().getPreviewURL(node.getID());
								MusicController.pause();
								new Thread() {
									@Override
									public void run() {
										try {
											previewID = -1;
											boolean playing = SoundController.playTrack(
												url,
												String.format("%d.mp3", node.getID()),
													new LineListener() {
													@Override
													public void update(LineEvent event) {
														if (event.getType() == LineEvent.Type.STOP) {
															if (previewID != -1)
																previewID = -1;
														}
													}
												}
											);
											if (playing)
												previewID = node.getID();
										} catch (SlickException e) {
											UI.getNotificationManager().sendBarNotification("Failed to load track preview. See log for details.");
											Log.error(e);
										}
									}
								}.start();
							}
							return;
						}

						if (isLoaded)
							return;

						// download beatmap
						SoundController.playSound(SoundEffect.MENUCLICK);
						if (index == focusResult) {
							if (focusTimer >= FOCUS_DELAY) {
								// too slow for double-click
								focusTimer = 0;
							} else {
								downloadBeatmap(node);
							}
						} else {
							// set focus
							focusResult = index;
							focusTimer = 0;
						}
						break;
					}
				}
				return;
			}

			// pages
			if (nodes.length > 0) {
				if (page > 1 && prevPage.contains(x, y)) {
					if (lastQueryDir == Page.PREVIOUS && searchQuery != null && !searchQuery.isComplete())
						;  // don't send consecutive requests
					else {
						SoundController.playSound(SoundEffect.MENUCLICK);
						pageDir = Page.PREVIOUS;
						lastQuery = null;
						if (searchQuery != null)
							searchQuery.interrupt();
						resetSearchTimer();
					}
					return;
				}
				if (pageResultTotal < totalResults && nextPage.contains(x, y)) {
					if (lastQueryDir == Page.NEXT && searchQuery != null && !searchQuery.isComplete())
						;  // don't send consecutive requests
					else {
						SoundController.playSound(SoundEffect.MENUCLICK);
						pageDir = Page.NEXT;
						lastQuery = null;
						if (searchQuery != null)
							searchQuery.interrupt();
						resetSearchTimer();
						return;
					}
				}
			}
		}

		// buttons
		if (clearButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUCLICK);
			DownloadList.get().clearInactiveDownloads();
			return;
		}
		if (importButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUCLICK);

			// import songs in new thread
			importThread = new BeatmapImportThread();
			importThread.start();
			return;
		}
		if (resetButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUCLICK);
			search.setText("");
			lastQuery = null;
			pageDir = Page.RESET;
			if (searchQuery != null)
				searchQuery.interrupt();
			resetSearchTimer();
			return;
		}
		if (rankedButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUCLICK);
			rankedOnly = !rankedOnly;
			lastQuery = null;
			pageDir = Page.RESET;
			if (searchQuery != null)
				searchQuery.interrupt();
			resetSearchTimer();
			return;
		}

		// downloads
		if (!DownloadList.get().isEmpty() && DownloadNode.downloadAreaContains(x, y)) {
			startDownloadIndexPos.pressed();
			int maxDownloadsShown = DownloadNode.maxDownloadsShown();
			int startDownloadIndex = (int) (startDownloadIndexPos.getPosition() / DownloadNode.getInfoHeight());
			int offset = (int) (-startDownloadIndexPos.getPosition() + startDownloadIndex * DownloadNode.getInfoHeight());
			for (int i = 0, n = DownloadList.get().size(); i < maxDownloadsShown + 1; i++) {
				int index = startDownloadIndex + i;
				if (index >= n)
					break;
				if (DownloadNode.downloadIconContains(x, y - offset, i)) {
					SoundController.playSound(SoundEffect.MENUCLICK);
					DownloadNode node = DownloadList.get().getNode(index);
					if (node == null)
						return;
					Download dl = node.getDownload();
					switch (dl.getStatus()) {
					case CANCELLED:
					case COMPLETE:
					case ERROR:
						node.clearDownload();
						DownloadList.get().remove(index);
						break;
					case DOWNLOADING:
					case WAITING:
						dl.cancel();
						break;
					}
					return;
				}
			}
		}
	}

	@Override
	public void mouseReleased(int button, int x, int y) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		startDownloadIndexPos.released();
		startResultPos.released();
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		if (UI.globalMouseWheelMoved(newValue, true))
			return;

		// block input during beatmap importing
		if (importThread != null)
			return;

		int shift = (newValue < 0) ? 1 : -1;
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		scrollLists(mouseX, mouseY, shift);
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		// block input during beatmap importing
		if (importThread != null)
			return;

		// check mouse button
		if (!input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON) &&
			!input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON))
			return;

		int diff = newy - oldy;
		if (diff == 0)
			return;
		startDownloadIndexPos.dragged(-diff);
		startResultPos.dragged(-diff);
	}

	@Override
	public void keyPressed(int key, char c) {
		// block input during beatmap importing
		if (importThread != null && !(key == Input.KEY_ESCAPE || key == Input.KEY_F12))
			return;

		if (UI.globalKeyPressed(key))
			return;

		switch (key) {
		case Input.KEY_ESCAPE:
			if (importThread != null) {
				// beatmap importing: stop parsing beatmaps by sending interrupt to BeatmapParser
				importThread.interrupt();
			} else if (!search.getText().isEmpty()) {
				// clear search text
				search.setText("");
				pageDir = Page.RESET;
				resetSearchTimer();
			} else {
				// return to main menu
				SoundController.playSound(SoundEffect.MENUBACK);
				((MainMenu) game.getState(Opsu.STATE_MAINMENU)).reset();
				game.enterState(Opsu.STATE_MAINMENU, new EasedFadeOutTransition(), new FadeInTransition());
			}
			break;
		case Input.KEY_ENTER:
			if (!search.getText().isEmpty()) {
				pageDir = Page.RESET;
				resetSearchTimer();
			}
			break;
		case Input.KEY_F5:
			SoundController.playSound(SoundEffect.MENUCLICK);
			lastQuery = null;
			pageDir = Page.CURRENT;
			if (searchQuery != null)
				searchQuery.interrupt();
			resetSearchTimer();
			break;
		default:
			// wait for user to finish typing
			if (Character.isLetterOrDigit(c) || key == Input.KEY_BACK || key == Input.KEY_SPACE) {
				// load glyphs
				if (c > 255)
					Fonts.loadGlyphs(searchFont, c);

				// reset search timer
				searchTimer = 0;
				pageDir = Page.RESET;
			}
			break;
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		prevPage.resetHover();
		nextPage.resetHover();
		clearButton.resetHover();
		importButton.resetHover();
		resetButton.resetHover();
		rankedButton.resetHover();
		serverMenu.activate();
		serverMenu.reset();
		focusResult = -1;
		startResultPos.setPosition(0);
		startDownloadIndexPos.setPosition(0);
		pageDir = Page.RESET;
		previewID = -1;
		if (barNotificationOnLoad != null) {
			UI.getNotificationManager().sendBarNotification(barNotificationOnLoad);
			barNotificationOnLoad = null;
		}
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
		search.setFocus(false);
		serverMenu.deactivate();
		SoundController.stopTrack();
		MusicController.resume();
	}

	/**
	 * Resets the search timer, but respects the minimum request interval.
	 */
	private void resetSearchTimer() {
		if (!searchTimerReset) {
			if (searchTimer < MIN_REQUEST_INTERVAL)
				searchTimer = SEARCH_DELAY - MIN_REQUEST_INTERVAL;
			else
				searchTimer = SEARCH_DELAY;
			searchTimerReset = true;
		}
	}

	/**
	 * Processes a shift in the search result and downloads list start indices,
	 * if the mouse coordinates are within the area bounds.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @param shift the number of indices to shift
	 */
	private void scrollLists(int cx, int cy, int shift) {
		// search results
		if (DownloadNode.resultAreaContains(cx, cy))
			startResultPos.scrollOffset(shift * DownloadNode.getButtonOffset());

		// downloads
		else if (DownloadNode.downloadAreaContains(cx, cy))
			startDownloadIndexPos.scrollOffset(shift * DownloadNode.getInfoHeight());
	}

	/**
	 * Sends a bar notification upon entering the state.
	 * @param s the notification string
	 */
	public void notifyOnLoad(String s) { barNotificationOnLoad = s; }

	/**
	 * Downloads the given beatmap.
	 * @param node the download node
	 */
	private void downloadBeatmap(final DownloadNode node) {
		final DownloadServer server = serverMenu.getSelectedItem();
		final String downloadURL = server.getDownloadURL(node.getID());

		// download in browser
		if (server.isDownloadInBrowser()) {
			final String importText = String.format(
				"Save the beatmap in the Import folder and then click \"Import All\":\n%s",
				Options.getImportDir().getAbsolutePath()
			);
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				// launch browser
				try {
					Desktop.getDesktop().browse(new URI(downloadURL));
					UI.getNotificationManager().sendNotification(String.format(
						"Opened a web browser to download \"%s\".\n\n%s",
						node.getTitle(), importText
					));
				} catch (IOException | URISyntaxException e) {
					ErrorHandler.error("Failed to launch browser.", e, true);
				}
			} else {
				// browse not supported: copy URL instead
				String text = String.format("Click here to copy the download URL for \"%s\".", node.getTitle());
				UI.getNotificationManager().sendNotification(text, Color.white, new NotificationListener() {
					@Override
					public void click() {
						try {
							Utils.copyToClipboard(downloadURL);
							UI.getNotificationManager().sendNotification(importText);
						} catch (Exception e) {}
					}
				});
			}
		}

		// download directly
		else if (!DownloadList.get().contains(node.getID())) {
			node.createDownload(server);
			if (node.getDownload() == null)
				UI.getNotificationManager().sendBarNotification("The download could not be started.");
			else {
				DownloadList.get().addNode(node);
				node.getDownload().start();
			}
		}
	}
}
