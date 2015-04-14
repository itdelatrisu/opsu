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

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.MenuButton;
import itdelatrisu.opsu.MenuButton.Expand;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuGroupList;
import itdelatrisu.opsu.OsuGroupNode;
import itdelatrisu.opsu.UI;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.states.ButtonMenu.MenuState;

import java.awt.Desktop;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

/**
 * "Main Menu" state.
 * <ul>
 * <li>[Play]    - move to song selection menu
 * <li>[Exit]    - move to confirm exit menu
 * </ul>
 */
public class MainMenu extends BasicGameState {
	/** Idle time, in milliseconds, before returning the logo to its original position. */
	private static final short MOVE_DELAY = 5000;

	/** Max alpha level of the menu background. */
	private static final float BG_MAX_ALPHA = 0.9f;

	/** Logo button that reveals other buttons on click. */
	private MenuButton logo;

	/** Whether or not the logo has been clicked. */
	private boolean logoClicked = false;

	/** Delay timer, in milliseconds, before starting to move the logo back to the center. */
	private int logoTimer = 0;

	/** Main "Play" and "Exit" buttons. */
	private MenuButton playButton, exitButton;

	/** Music control buttons. */
	private MenuButton musicPlay, musicPause, musicNext, musicPrevious;

	/** Button linking to Downloads menu. */
	private MenuButton downloadsButton;

	/** Button linking to repository. */
	private MenuButton repoButton;

	/** Button for installing updates. */
	private MenuButton updateButton;

	/** Application start time, for drawing the total running time. */
	private long osuStartTime;

	/** Indexes of previous songs. */
	private Stack<Integer> previous;

	/** Background alpha level (for fade-in effect). */
	private float bgAlpha = 0f;

	/** Whether or not a notification was already sent upon entering. */
	private boolean enterNotification = false;

	/** Music position bar coordinates and dimensions. */
	private float musicBarX, musicBarY, musicBarWidth, musicBarHeight;

	/** Music position bar background colors. */
	private static final Color
		BG_NORMAL = new Color(0, 0, 0, 0.25f),
		BG_HOVER  = new Color(0, 0, 0, 0.5f);

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private int state;

	public MainMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		this.input = container.getInput();

		osuStartTime = System.currentTimeMillis();
		previous = new Stack<Integer>();

		int width = container.getWidth();
		int height = container.getHeight();

		// initialize menu buttons
		Image logoImg = GameImage.MENU_LOGO.getImage();
		Image playImg = GameImage.MENU_PLAY.getImage();
		Image exitImg = GameImage.MENU_EXIT.getImage();
		float exitOffset = (playImg.getWidth() - exitImg.getWidth()) / 3f;
		logo = new MenuButton(logoImg, width / 2f, height / 2f);
		playButton = new MenuButton(playImg,
				width * 0.75f, (height / 2) - (logoImg.getHeight() / 5f)
		);
		exitButton = new MenuButton(exitImg,
				width * 0.75f - exitOffset, (height / 2) + (exitImg.getHeight() / 2f)
		);
		logo.setHoverExpand(1.05f);
		playButton.setHoverExpand(1.05f);
		exitButton.setHoverExpand(1.05f);

		// initialize music buttons
		int musicWidth  = GameImage.MUSIC_PLAY.getImage().getWidth();
		int musicHeight = GameImage.MUSIC_PLAY.getImage().getHeight();
		musicPlay     = new MenuButton(GameImage.MUSIC_PLAY.getImage(), width - (2 * musicWidth), musicHeight / 1.5f);
		musicPause    = new MenuButton(GameImage.MUSIC_PAUSE.getImage(), width - (2 * musicWidth), musicHeight / 1.5f);
		musicNext     = new MenuButton(GameImage.MUSIC_NEXT.getImage(), width - musicWidth, musicHeight / 1.5f);
		musicPrevious = new MenuButton(GameImage.MUSIC_PREVIOUS.getImage(), width - (3 * musicWidth), musicHeight / 1.5f);
		musicPlay.setHoverExpand(1.5f);
		musicPause.setHoverExpand(1.5f);
		musicNext.setHoverExpand(1.5f);
		musicPrevious.setHoverExpand(1.5f);

		// initialize music position bar location
		musicBarX = width - musicWidth * 3.5f;
		musicBarY = musicHeight * 1.25f;
		musicBarWidth = musicWidth * 3f;
		musicBarHeight = musicHeight * 0.11f;

		// initialize downloads button
		Image dlImg = GameImage.DOWNLOADS.getImage();
		downloadsButton = new MenuButton(dlImg, width - dlImg.getWidth() / 2f, height / 2f);
		downloadsButton.setHoverExpand(1.03f, Expand.LEFT);

		// initialize repository button
		float startX = width * 0.997f, startY = height * 0.997f;
		if (Desktop.isDesktopSupported()) {  // only if a webpage can be opened
			Image repoImg = GameImage.REPOSITORY.getImage();
			repoButton = new MenuButton(repoImg,
					startX - repoImg.getWidth(), startY - repoImg.getHeight()
			);
			repoButton.setHoverExpand();
			startX -= repoImg.getWidth() * 1.75f;
		} else
			startX -= width * 0.005f;

		// initialize update button
		Image bangImg = GameImage.BANG.getImage();
		updateButton = new MenuButton(bangImg, startX - bangImg.getWidth(), startY - bangImg.getHeight());
		updateButton.setHoverExpand(1.15f);

		reset();
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();

		// draw background
		OsuFile osu = MusicController.getOsuFile();
		if (Options.isDynamicBackgroundEnabled() &&
			osu != null && osu.drawBG(width, height, bgAlpha, true))
				;
		else {
			Image bg = GameImage.MENU_BG.getImage();
			bg.setAlpha(bgAlpha);
			bg.draw();
		}

		// top/bottom horizontal bars
		float oldAlpha = Utils.COLOR_BLACK_ALPHA.a;
		Utils.COLOR_BLACK_ALPHA.a = 0.5f;
		g.setColor(Utils.COLOR_BLACK_ALPHA);
		g.fillRect(0, 0, width, height / 9f);
		g.fillRect(0, height * 8 / 9f, width, height / 9f);
		Utils.COLOR_BLACK_ALPHA.a = oldAlpha;

		// draw downloads button
		downloadsButton.draw();

		// draw buttons
		if (logoTimer > 0) {
			playButton.draw();
			exitButton.draw();
		}
		logo.draw();

		// draw music buttons
		if (MusicController.isPlaying())
			musicPause.draw();
		else
			musicPlay.draw();
		musicNext.draw();
		musicPrevious.draw();

		// draw music position bar
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		g.setColor((musicPositionBarContains(mouseX, mouseY)) ? BG_HOVER : BG_NORMAL);
		g.fillRoundRect(musicBarX, musicBarY, musicBarWidth, musicBarHeight, 4);
		g.setColor(Color.white);
		if (!MusicController.isTrackLoading() && osu != null) {
			float musicBarPosition = Math.min((float) MusicController.getPosition() / MusicController.getDuration(), 1f);
			g.fillRoundRect(musicBarX, musicBarY, musicBarWidth * musicBarPosition, musicBarHeight, 4);
		}

		// draw repository button
		if (repoButton != null)
			repoButton.draw();

		// draw update button
		if (Updater.get().showButton()) {
			Color updateColor = null;
			switch (Updater.get().getStatus()) {
			case UPDATE_AVAILABLE:
				updateColor = Color.red;
				break;
			case UPDATE_DOWNLOADED:
				updateColor = Color.green;
				break;
			case UPDATE_DOWNLOADING:
				updateColor = Color.yellow;
				break;
			default:
				updateColor = Color.white;
				break;
			}
			updateButton.draw(updateColor);
		}

		// draw text
		float marginX = width * 0.015f, marginY = height * 0.015f;
		g.setFont(Utils.FONT_MEDIUM);
		int lineHeight = Utils.FONT_MEDIUM.getLineHeight() * 9 / 10;
		g.drawString(String.format("opsu! has loaded %d songs and %d beatmaps.",
				OsuGroupList.get().getMapSetCount(), OsuGroupList.get().getMapCount()), marginX, marginY);
		if (MusicController.isTrackLoading()){
			g.drawString(String.format("opsu! is loading track...",osu.getArtist(),osu.getTitle()), marginX, marginY + lineHeight);
                        g.drawString(String.format("%s - %s", osu.getArtist(), osu.getTitle()), marginX + 25, marginY + (lineHeight * 2));
                }
		else if (MusicController.trackExists()) {
			if (Options.useUnicodeMetadata())  // load glyphs
				Utils.loadGlyphs(Utils.FONT_MEDIUM, osu.titleUnicode, osu.artistUnicode);
			g.drawString((MusicController.isPlaying()) ? "Now Playing:" : "Paused:", marginX, marginY + lineHeight);
			g.drawString(String.format("%s - %s", osu.getArtist(), osu.getTitle()), marginX + 25, marginY + (lineHeight * 2));
		}
		g.drawString(String.format("opsu! has been running for %s.",
				Utils.getTimeString((int) (System.currentTimeMillis() - osuStartTime) / 1000)),
				marginX, height - marginY - (lineHeight * 2));
		g.drawString(String.format("It is currently %s.",
				new SimpleDateFormat("h:mm a").format(new Date())),
				marginX, height - marginY - lineHeight);

		UI.draw(g);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		UI.update(delta);
		if (MusicController.trackEnded())
			nextTrack();  // end of track: go to next track
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		logo.hoverUpdate(delta, mouseX, mouseY, 0.25f);
		playButton.hoverUpdate(delta, mouseX, mouseY, 0.25f);
		exitButton.hoverUpdate(delta, mouseX, mouseY, 0.25f);
		if (repoButton != null)
			repoButton.hoverUpdate(delta, mouseX, mouseY);
		updateButton.hoverUpdate(delta, mouseX, mouseY);
		downloadsButton.hoverUpdate(delta, mouseX, mouseY);
		// ensure only one button is in hover state at once
		boolean noHoverUpdate = musicPositionBarContains(mouseX, mouseY);
		boolean contains = musicPlay.contains(mouseX, mouseY);
		musicPlay.hoverUpdate(delta, !noHoverUpdate && contains);
		musicPause.hoverUpdate(delta, !noHoverUpdate && contains);
		noHoverUpdate |= contains;
		musicNext.hoverUpdate(delta, !noHoverUpdate && musicNext.contains(mouseX, mouseY));
		musicPrevious.hoverUpdate(delta, !noHoverUpdate && musicPrevious.contains(mouseX, mouseY));

		// window focus change: increase/decrease theme song volume
		if (MusicController.isThemePlaying() &&
		    MusicController.isTrackDimmed() == container.hasFocus())
				MusicController.toggleTrackDimmed(0.33f);

		// fade in background
		if (bgAlpha < BG_MAX_ALPHA) {
			bgAlpha += delta / 1000f;
			if (bgAlpha > BG_MAX_ALPHA)
				bgAlpha = BG_MAX_ALPHA;
		}

		// buttons
		if (logoClicked) {
			if (logoTimer == 0) {  // shifting to left
				if (logo.getX() > container.getWidth() / 3.3f)
					logo.setX(logo.getX() - delta);
				else
					logoTimer = 1;
			} else if (logoTimer >= MOVE_DELAY)  // timer over: shift back to center
				logoClicked = false;
			else {  // increment timer
				logoTimer += delta;
				if (logoTimer <= 500) {
					// fade in buttons
					playButton.getImage().setAlpha(logoTimer / 400f);
					exitButton.getImage().setAlpha(logoTimer / 400f);
				}
			}
		} else {
			// fade out buttons
			if (logoTimer > 0) {
				float alpha = playButton.getImage().getAlpha();
				if (alpha > 0f) {
					playButton.getImage().setAlpha(alpha - (delta / 200f));
					exitButton.getImage().setAlpha(alpha - (delta / 200f));
				} else
					logoTimer = 0;
			}

			// move back to original location
			if (logo.getX() < container.getWidth() / 2) {
				logo.setX(logo.getX() + (delta / 3f));
				if (logo.getX() > container.getWidth() / 2)
					logo.setX(container.getWidth() / 2);
			}
		}

		// tooltips
		if (musicPositionBarContains(mouseX, mouseY))
			UI.updateTooltip(delta, "Click to seek to a specific point in the song.", false);
		else if (musicPlay.contains(mouseX, mouseY))
			UI.updateTooltip(delta, (MusicController.isPlaying()) ? "Pause" : "Play", false);
		else if (musicNext.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Next track", false);
		else if (musicPrevious.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Previous track", false);
		else if (Updater.get().showButton() && updateButton.contains(mouseX, mouseY))
			UI.updateTooltip(delta, Updater.get().getStatus().getDescription(), true);
	}

	@Override
	public int getID() { return state; }

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		if (!enterNotification) {
			if (Updater.get().getStatus() == Updater.Status.UPDATE_AVAILABLE) {
				UI.sendBarNotification("An opsu! update is available.");
				enterNotification = true;
			} else if (Updater.get().justUpdated()) {
				UI.sendBarNotification("opsu! is now up to date!");
				enterNotification = true;
			}
		}

		// reset button hover states if mouse is not currently hovering over the button
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		if (!logo.contains(mouseX, mouseY, 0.25f))
			logo.resetHover();
		if (!playButton.contains(mouseX, mouseY, 0.25f))
			playButton.resetHover();
		if (!exitButton.contains(mouseX, mouseY, 0.25f))
			exitButton.resetHover();
		if (!musicPlay.contains(mouseX, mouseY))
			musicPlay.resetHover();
		if (!musicPause.contains(mouseX, mouseY))
			musicPause.resetHover();
		if (!musicNext.contains(mouseX, mouseY))
			musicNext.resetHover();
		if (!musicPrevious.contains(mouseX, mouseY))
			musicPrevious.resetHover();
		if (repoButton != null && !repoButton.contains(mouseX, mouseY))
			repoButton.resetHover();
		if (!updateButton.contains(mouseX, mouseY))
			updateButton.resetHover();
		if (!downloadsButton.contains(mouseX, mouseY))
			downloadsButton.resetHover();
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
		if (MusicController.isTrackDimmed())
			MusicController.toggleTrackDimmed(1f);
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// music position bar
		if (MusicController.isPlaying()) {
			if (musicPositionBarContains(x, y)) {
				float pos = (x - musicBarX) / musicBarWidth;
				MusicController.setPosition((int) (pos * MusicController.getDuration()));
				return;
			}
		}

		// music button actions
		if (musicPlay.contains(x, y)) {
			if (MusicController.isPlaying()) {
				MusicController.pause();
				UI.sendBarNotification("Pause");
			} else if (!MusicController.isTrackLoading()) {
				MusicController.resume();
				UI.sendBarNotification("Play");
			}
		} else if (musicNext.contains(x, y)) {
			nextTrack();
			UI.sendBarNotification(">> Next");
		} else if (musicPrevious.contains(x, y)) {
			if (!previous.isEmpty()) {
				SongMenu menu = (SongMenu) game.getState(Opsu.STATE_SONGMENU);
				menu.setFocus(OsuGroupList.get().getBaseNode(previous.pop()), -1, true, false);
				if (Options.isDynamicBackgroundEnabled())
					bgAlpha = 0f;
			} else
				MusicController.setPosition(0);
			UI.sendBarNotification("<< Previous");
		}

		// downloads button actions
		else if (downloadsButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			game.enterState(Opsu.STATE_DOWNLOADSMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		}

		// repository button actions
		else if (repoButton != null && repoButton.contains(x, y)) {
			try {
				Desktop.getDesktop().browse(Options.REPOSITORY_URI);
			} catch (IOException e) {
				ErrorHandler.error("Could not browse to repository URI.", e, false);
			}
		}

		// update button actions
		else if (Updater.get().showButton() && updateButton.contains(x, y)) {
			switch (Updater.get().getStatus()) {
			case UPDATE_AVAILABLE:
				SoundController.playSound(SoundEffect.MENUHIT);
				Updater.get().startDownload();
				break;
			case UPDATE_DOWNLOADED:
				SoundController.playSound(SoundEffect.MENUHIT);
				Updater.get().prepareUpdate();
				container.setForceExit(false);
				container.exit();
				break;
			default:
				break;
			}
		}

		// start moving logo (if clicked)
		else if (!logoClicked) {
			if (logo.contains(x, y, 0.25f)) {
				logoClicked = true;
				logoTimer = 0;
				playButton.getImage().setAlpha(0f);
				exitButton.getImage().setAlpha(0f);
				SoundController.playSound(SoundEffect.MENUHIT);
			}
		}

		// other button actions (if visible)
		else if (logoClicked) {
			if (logo.contains(x, y, 0.25f) || playButton.contains(x, y, 0.25f)) {
				SoundController.playSound(SoundEffect.MENUHIT);
				enterSongMenu();
			} else if (exitButton.contains(x, y, 0.25f))
				container.exit();
		}
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		UI.changeVolume((newValue < 0) ? -1 : 1);
	}

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
		case Input.KEY_Q:
			((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(MenuState.EXIT);
			game.enterState(Opsu.STATE_BUTTONMENU);
			break;
		case Input.KEY_P:
			SoundController.playSound(SoundEffect.MENUHIT);
			if (!logoClicked) {
				logoClicked = true;
				logoTimer = 0;
				playButton.getImage().setAlpha(0f);
				exitButton.getImage().setAlpha(0f);
			} else
				enterSongMenu();
			break;
		case Input.KEY_D:
			SoundController.playSound(SoundEffect.MENUHIT);
			game.enterState(Opsu.STATE_DOWNLOADSMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			break;
		case Input.KEY_R:
			nextTrack();
			break;
		case Input.KEY_UP:
			UI.changeVolume(1);
			break;
		case Input.KEY_DOWN:
			UI.changeVolume(-1);
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
		}
	}

	/**
	 * Returns true if the coordinates are within the music position bar bounds.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	private boolean musicPositionBarContains(float cx, float cy) {
		return ((cx > musicBarX && cx < musicBarX + musicBarWidth) &&
		        (cy > musicBarY && cy < musicBarY + musicBarHeight));
	}

	/**
	 * Resets the button states.
	 */
	public void reset() {
		// reset logo
		logo.setX(container.getWidth() / 2);
		logoClicked = false;
		logoTimer = 0;

		logo.resetHover();
		playButton.resetHover();
		exitButton.resetHover();
		musicPlay.resetHover();
		musicPause.resetHover();
		musicNext.resetHover();
		musicPrevious.resetHover();
		if (repoButton != null)
			repoButton.resetHover();
		updateButton.resetHover();
		downloadsButton.resetHover();
	}

	/**
	 * Plays the next track, and adds the previous one to the stack.
	 */
	private void nextTrack() {
		boolean isTheme = MusicController.isThemePlaying();
		SongMenu menu = (SongMenu) game.getState(Opsu.STATE_SONGMENU);
		OsuGroupNode node = menu.setFocus(OsuGroupList.get().getRandomNode(), -1, true, false);
		boolean sameAudio = false;
		if (node != null) {
			sameAudio = MusicController.getOsuFile().audioFilename.equals(node.osuFiles.get(0).audioFilename);
			if (!isTheme && !sameAudio)
				previous.add(node.index);
		}
		if (Options.isDynamicBackgroundEnabled() && !sameAudio && !MusicController.isThemePlaying())
			bgAlpha = 0f;
	}

	/**
	 * Enters the song menu, or the downloads menu if no beatmaps are loaded.
	 */
	private void enterSongMenu() {
		int state = Opsu.STATE_SONGMENU;
		if (OsuGroupList.get().getMapSetCount() == 0) {
			((DownloadsMenu) game.getState(Opsu.STATE_DOWNLOADSMENU)).notifyOnLoad("Download some beatmaps to get started!");
			state = Opsu.STATE_DOWNLOADSMENU;
		}
		game.enterState(state, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
	}
}
