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
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.downloads.BloodcatServer;
import itdelatrisu.opsu.downloads.Download;
import itdelatrisu.opsu.downloads.DownloadList;
import itdelatrisu.opsu.downloads.DownloadNode;
import itdelatrisu.opsu.downloads.DownloadServer;

import java.io.IOException;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.gui.TextField;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

/**
 * Downloads menu.
 */
public class DownloadsMenu extends BasicGameState {
	/** The max number of search result buttons to be shown at a time. */
	public static final int MAX_RESULT_BUTTONS = 10;

	/** The max number of downloads to be shown at a time. */
	public static final int MAX_DOWNLOADS_SHOWN = 11;

	/** Delay time, in milliseconds, between each search. */
	private static final int SEARCH_DELAY = 700;

	/** Delay time, in milliseconds, for double-clicking focused result. */
	private static final int FOCUS_DELAY = 250;

	/** Minimum time, in milliseconds, that must elapse between queries. */
	private static final int MIN_REQUEST_INTERVAL = 300;

	/** The beatmap download server. */
	private DownloadServer server = new BloodcatServer();

	/** The current list of search results. */
	private DownloadNode[] resultList;

	/** Current focused (selected) result. */
	private int focusResult = -1;

	/** Delay time, in milliseconds, for double-clicking focused result. */
	private int focusTimer = 0;

	/** Current start result button (topmost entry). */
	private int startResult = 0;

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
	private int startDownloadIndex = 0;

	/** Query thread. */
	private Thread queryThread;

	/** The search textfield. */
	private TextField search;

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

	/** Number of active requests. */
	private int activeRequests = 0;

	/** Previous and next page buttons. */
	private MenuButton prevPage, nextPage;

	/** "Ranked only?" checkbox coordinates. */
	private float rankedBoxX, rankedBoxY, rankedBoxLength;

	// game-related variables
	private StateBasedGame game;
	private Input input;
	private int state;

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

		// search
		searchTimer = SEARCH_DELAY;
		searchResultString = "Type to search!";
		search = new TextField(
				container, Utils.FONT_DEFAULT,
				(int) baseX, (int) (height * 0.05f) + Utils.FONT_LARGE.getLineHeight(),
				(int) (width * 0.35f), Utils.FONT_MEDIUM.getLineHeight()
		);
		search.setBackgroundColor(DownloadNode.BG_NORMAL);
		search.setBorderColor(Color.white);
		search.setTextColor(Color.white);
		search.setConsumeEvents(false);
		search.setMaxLength(255);

		// ranked only?
		rankedBoxX = search.getX() + search.getWidth() * 1.2f;
		rankedBoxY = search.getY();
		rankedBoxLength = search.getHeight();

		// page buttons
		float buttonY = height * 0.2f;
		float buttonWidth = width * 0.7f;
		Image prevImg = GameImage.MUSIC_PREVIOUS.getImage();
		Image nextImg = GameImage.MUSIC_NEXT.getImage();
		prevPage = new MenuButton(prevImg, baseX + prevImg.getWidth() / 2f,
				buttonY - prevImg.getHeight() / 2f);
		nextPage = new MenuButton(nextImg, baseX + buttonWidth - nextImg.getWidth() / 2f,
				buttonY - nextImg.getHeight() / 2f);
		prevPage.setHoverScale(1.5f);
		nextPage.setHoverScale(1.5f);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();

		// background
		GameImage.SEARCH_BG.getImage().draw();

		// title
		Utils.FONT_LARGE.drawString(width * 0.024f, height * 0.04f, "Download Beatmaps!", Color.white);

		// search
		g.setColor(Color.white);
		search.render(container, g);
		Utils.FONT_BOLD.drawString(
				search.getX() + search.getWidth() * 0.01f, search.getY() + search.getHeight() * 1.3f,
				searchResultString, Color.white
		);

		// ranked only?
		if (rankedOnly)
			g.fillRect(rankedBoxX, rankedBoxY, rankedBoxLength, rankedBoxLength);
		else
			g.drawRect(rankedBoxX, rankedBoxY, rankedBoxLength, rankedBoxLength);
		Utils.FONT_MEDIUM.drawString(rankedBoxX + rankedBoxLength * 1.5f, rankedBoxY, "Show ranked maps only?", Color.white);

		// search results
		DownloadNode[] nodes = resultList;
		if (nodes != null) {
			for (int i = 0; i < MAX_RESULT_BUTTONS; i++) {
				int index = startResult + i;
				if (index >= nodes.length)
					break;
				nodes[index].drawResult(g, i, DownloadNode.resultContains(mouseX, mouseY, i), (index == focusResult));
			}

			// scroll bar
			if (nodes.length > MAX_RESULT_BUTTONS)
				DownloadNode.drawResultScrollbar(g, startResult, nodes.length);

			// pages
			if (nodes.length > 0) {
				float baseX = width * 0.024f;
				float buttonY = height * 0.2f;
				float buttonWidth = width * 0.7f;
				Utils.FONT_BOLD.drawString(
						baseX + (buttonWidth - Utils.FONT_BOLD.getWidth("Page 1")) / 2f,
						buttonY - Utils.FONT_BOLD.getLineHeight() * 1.3f,
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
		g.setColor(DownloadNode.BG_NORMAL);
		g.fillRect(downloadsX, downloadsY,
				width * 0.25f, height - downloadsY * 2f);
		Utils.FONT_LARGE.drawString(downloadsX + width * 0.015f, downloadsY + height * 0.015f, "Downloads", Color.white);
		int downloadsSize = DownloadList.get().size();
		if (downloadsSize > 0) {
			for (int i = 0; i < MAX_DOWNLOADS_SHOWN; i++) {
				int index = startDownloadIndex + i;
				if (index >= downloadsSize)
					break;
				DownloadList.get().getNode(index).drawDownload(g, i, index, DownloadNode.downloadContains(mouseX, mouseY, i));
			}

			// scroll bar
			if (downloadsSize > MAX_DOWNLOADS_SHOWN)
				DownloadNode.drawDownloadScrollbar(g, startDownloadIndex, downloadsSize);
		}

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
		prevPage.hoverUpdate(delta, mouseX, mouseY);
		nextPage.hoverUpdate(delta, mouseX, mouseY);

		// focus timer
		if (focusResult != -1 && focusTimer < FOCUS_DELAY)
			focusTimer += delta;

		// search
		search.setFocus(true);
		searchTimer += delta;
		if (searchTimer >= SEARCH_DELAY) {
			searchTimer = 0;
			searchTimerReset = false;

			final String query = search.getText().trim().toLowerCase();
			if (lastQuery == null || !query.equals(lastQuery)) {
				lastQuery = query;
				lastQueryDir = pageDir;

				if (queryThread != null && queryThread.isAlive())
					queryThread.interrupt();

				// execute query
				queryThread = new Thread() {
					@Override
					public void run() {
						activeRequests++;

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
							if (activeRequests - 1 == 0) {
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
								startResult = 0;
								if (nodes == null)
									searchResultString = "An error has occurred.";
								else {
									if (query.isEmpty())
										searchResultString = "Type to search!";
									else if (totalResults == 0)
										searchResultString = "No results found.";
									else
										searchResultString = String.format("%d result%s found!",
												totalResults, (totalResults == 1) ? "" : "s");
								}
							}
						} catch (IOException e) {
							searchResultString = "Could not establish connection to server.";
						} finally {
							activeRequests--;
							queryThread = null;
						}
					}
				};
				queryThread.start();
			}
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
			SoundController.playSound(SoundEffect.MENUBACK);
			((MainMenu) game.getState(Opsu.STATE_MAINMENU)).reset();
			game.enterState(Opsu.STATE_MAINMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			return;
		}

		// ranked only?
		if ((x > rankedBoxX && x < rankedBoxX + rankedBoxLength) &&
		    (y > rankedBoxY && y < rankedBoxY + rankedBoxLength)) {
			rankedOnly = !rankedOnly;
			lastQuery = null;
			pageDir = Page.CURRENT;
			resetSearchTimer();
			return;
		}

		// search results
		DownloadNode[] nodes = resultList;
		if (nodes != null) {
			if (DownloadNode.resultAreaContains(x, y)) {
				for (int i = 0; i < MAX_RESULT_BUTTONS; i++) {
					int index = startResult + i;
					if (index >= nodes.length)
						break;
					if (DownloadNode.resultContains(x, y, i)) {
						if (index == focusResult) {
							if (focusTimer >= FOCUS_DELAY) {
								// too slow for double-click
								focusTimer = 0;
							} else {
								// start download
								DownloadNode node = nodes[index];
								if (!DownloadList.get().contains(node.getID())) {
									DownloadList.get().addNode(node);
									node.createDownload(server);
									node.getDownload().start();
								}
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
					if (lastQueryDir == Page.PREVIOUS && queryThread != null && queryThread.isAlive())
						;  // don't send consecutive requests
					else {
						pageDir = Page.PREVIOUS;
						lastQuery = null;
						resetSearchTimer();
					}
					return;
				}
				if (pageResultTotal < totalResults && nextPage.contains(x, y)) {
					if (lastQueryDir == Page.NEXT && queryThread != null && queryThread.isAlive())
						;  // don't send consecutive requests
					else {
						pageDir = Page.NEXT;
						lastQuery = null;
						resetSearchTimer();
						return;
					}
				}
			}
		}

		// downloads
		if (!DownloadList.get().isEmpty() && DownloadNode.downloadAreaContains(x, y)) {
			for (int i = 0, n = DownloadList.get().size(); i < MAX_DOWNLOADS_SHOWN; i++) {
				int index = startDownloadIndex + i;
				if (index >= n)
					break;
				if (DownloadNode.downloadIconContains(x, y, i)) {
					DownloadNode node = DownloadList.get().getNode(index);
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
	public void mouseWheelMoved(int newValue) {
		int shift = (newValue < 0) ? 1 : -1;
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		scrollLists(mouseX, mouseY, shift);
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		// check mouse button
		if (!input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON) &&
			!input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON))
			return;

		int diff = newy - oldy;
		if (diff == 0)
			return;
		int shift = (diff < 0) ? 1 : -1;
		scrollLists(oldx, oldy, shift);
	}

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
			if (!search.getText().isEmpty()) {
				// clear search text
				search.setText("");
				pageDir = Page.RESET;
				resetSearchTimer();
			} else {
				// return to main menu
				SoundController.playSound(SoundEffect.MENUBACK);
				((MainMenu) game.getState(Opsu.STATE_MAINMENU)).reset();
				game.enterState(Opsu.STATE_MAINMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			}
			break;
		case Input.KEY_ENTER:
			if (!search.getText().isEmpty()) {
				pageDir = Page.RESET;
				resetSearchTimer();
			}
			break;
		case Input.KEY_F5:
			lastQuery = null;
			pageDir = Page.CURRENT;
			resetSearchTimer();
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		default:
			// wait for user to finish typing
			if (Character.isLetterOrDigit(c) || key == Input.KEY_BACK) {
				searchTimer = 0;
				pageDir = Page.RESET;
			}
			break;
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		Utils.getBackButton().setScale(1f);
		prevPage.setScale(1f);
		nextPage.setScale(1f);
		focusResult = -1;
		startResult = 0;
		startDownloadIndex = 0;
		pageDir = Page.RESET;
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
		if (DownloadNode.resultAreaContains(cx, cy)) {
			DownloadNode[] nodes = resultList;
			if (nodes != null && nodes.length >= MAX_RESULT_BUTTONS) {
				int newStartResult = startResult + shift;
				if (newStartResult >= 0 && newStartResult + MAX_RESULT_BUTTONS <= nodes.length)
					startResult = newStartResult;
			}
		}

		// downloads
		else if (DownloadNode.downloadAreaContains(cx, cy)) {
			if (DownloadList.get().size() >= MAX_DOWNLOADS_SHOWN) {
				int newStartDownloadIndex = startDownloadIndex + shift;
				if (newStartDownloadIndex >= 0 && newStartDownloadIndex + MAX_DOWNLOADS_SHOWN <= DownloadList.get().size())
					startDownloadIndex = newStartDownloadIndex;
			}
		}
	}
}
