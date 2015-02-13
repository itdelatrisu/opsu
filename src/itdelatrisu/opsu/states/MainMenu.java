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
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.states.ButtonMenu.MenuState;

import java.awt.Desktop;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

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

	/** Application start time, for drawing the total running time. */
	private long osuStartTime;

	/** Indexes of previous songs. */
	private Stack<Integer> previous;

	/** Background alpha level (for fade-in effect). */
	private float bgAlpha = 0f;

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
		Image playImg = GameImage.MENU_PlAY.getImage();
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
		int musicWidth  = 48;
		int musicHeight = 30;
		musicPlay     = new MenuButton(GameImage.MUSIC_PLAY.getImage(), width - (2 * musicWidth), musicHeight);
		musicPause    = new MenuButton(GameImage.MUSIC_PAUSE.getImage(), width - (2 * musicWidth), musicHeight);
		musicNext     = new MenuButton(GameImage.MUSIC_NEXT.getImage(), width - musicWidth, musicHeight);
		musicPrevious = new MenuButton(GameImage.MUSIC_PREVIOUS.getImage(), width - (3 * musicWidth), musicHeight);
		musicPlay.setHoverExpand(1.5f);
		musicPause.setHoverExpand(1.5f);
		musicNext.setHoverExpand(1.5f);
		musicPrevious.setHoverExpand(1.5f);

		// initialize downloads button
		Image dlImg = GameImage.DOWNLOADS.getImage();
		downloadsButton = new MenuButton(dlImg, width - dlImg.getWidth() / 2f, height / 2f);
		downloadsButton.setHoverExpand(1.03f, Expand.LEFT);

		// initialize repository button
		if (Desktop.isDesktopSupported()) {  // only if a webpage can be opened
			Image repoImg = GameImage.REPOSITORY.getImage();
			repoButton = new MenuButton(repoImg,
					(width * 0.997f) - repoImg.getWidth(), (height * 0.997f) - repoImg.getHeight()
			);
			repoButton.setHoverExpand();
		}

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

		float oldAlpha = Utils.COLOR_BLACK_ALPHA.a;
		Utils.COLOR_BLACK_ALPHA.a = 0.2f;
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
		g.setColor(Utils.COLOR_BLACK_ALPHA);
		g.fillRoundRect(width - 168, 54, 148, 5, 4);
		g.setColor(Color.white);
		if (!MusicController.isTrackLoading() && osu != null)
			g.fillRoundRect(width - 168, 54,
				148f * MusicController.getPosition() / osu.endTime, 5, 4);

		// draw repository button
		if (repoButton != null)
			repoButton.draw();

		// draw text
		float marginX = width * 0.015f, marginY = height * 0.015f;
		g.setFont(Utils.FONT_MEDIUM);
		int lineHeight = Utils.FONT_MEDIUM.getLineHeight() * 9 / 10;
		g.drawString(String.format("Loaded %d songs and %d beatmaps.",
				OsuGroupList.get().size(), OsuGroupList.get().getMapCount()), marginX, marginY);
		if (MusicController.isTrackLoading())
			g.drawString("Track loading...", marginX, marginY + lineHeight);
		else if (MusicController.trackExists()) {
			g.drawString((MusicController.isPlaying()) ? "Now Playing:" : "Paused:", marginX, marginY + lineHeight);
			g.drawString(String.format("%s: %s",
					MusicController.getArtistName(),
					MusicController.getTrackName()),
					marginX + 25, marginY + (lineHeight * 2));
		}
		long time = System.currentTimeMillis() - osuStartTime;
		g.drawString(String.format("opsu! has been running for %d minutes, %d seconds.",
				TimeUnit.MILLISECONDS.toMinutes(time),
				TimeUnit.MILLISECONDS.toSeconds(time) -
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))),
				marginX, height - marginY - (lineHeight * 2));
		g.drawString(String.format("The current time is %s.",
				new SimpleDateFormat("h:mm a").format(new Date())),
				marginX, height - marginY - lineHeight);

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
		logo.hoverUpdate(delta, mouseX, mouseY);
		playButton.hoverUpdate(delta, mouseX, mouseY);
		exitButton.hoverUpdate(delta, mouseX, mouseY);
		if (repoButton != null)
			repoButton.hoverUpdate(delta, mouseX, mouseY);
		downloadsButton.hoverUpdate(delta, mouseX, mouseY);
		musicPlay.hoverUpdate(delta, mouseX, mouseY);
		musicPause.hoverUpdate(delta, mouseX, mouseY);
		if (musicPlay.contains(mouseX, mouseY))
			mouseX = mouseY = -1;  // ensure only one button is in hover state at once
		musicNext.hoverUpdate(delta, mouseX, mouseY);
		musicPrevious.hoverUpdate(delta, mouseX, mouseY);

		// window focus change: increase/decrease theme song volume
		if (MusicController.isThemePlaying() &&
		    MusicController.isTrackDimmed() == container.hasFocus())
				MusicController.toggleTrackDimmed(0.33f);

		// fade in background
		if (bgAlpha < 0.9f) {
			bgAlpha += delta / 1000f;
			if (bgAlpha > 0.9f)
				bgAlpha = 0.9f;
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
	}

	@Override
	public int getID() { return state; }

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		// reset button hover states if mouse is not currently hovering over the button
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		if (!logo.contains(mouseX, mouseY))
			logo.resetHover();
		if (!playButton.contains(mouseX, mouseY))
			playButton.resetHover();
		if (!exitButton.contains(mouseX, mouseY))
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
		if (!downloadsButton.contains(mouseX, mouseY))
			downloadsButton.resetHover();
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// music button actions
		if (musicPlay.contains(x, y)) {
			if (MusicController.isPlaying())
				MusicController.pause();
			else if (!MusicController.isTrackLoading())
				MusicController.resume();
		} else if (musicNext.contains(x, y)) {
			boolean isTheme = MusicController.isThemePlaying();
			SongMenu menu = (SongMenu) game.getState(Opsu.STATE_SONGMENU);
			OsuGroupNode node = menu.setFocus(OsuGroupList.get().getRandomNode(), -1, true);
			boolean sameAudio = false;
			if (node != null) {
				sameAudio = MusicController.getOsuFile().audioFilename.equals(node.osuFiles.get(0).audioFilename);
				if (!isTheme && !sameAudio)
					previous.add(node.index);
			}
			if (Options.isDynamicBackgroundEnabled() && !sameAudio && !MusicController.isThemePlaying())
				bgAlpha = 0f;
		} else if (musicPrevious.contains(x, y)) {
			if (!previous.isEmpty()) {
				SongMenu menu = (SongMenu) game.getState(Opsu.STATE_SONGMENU);
				menu.setFocus(OsuGroupList.get().getBaseNode(previous.pop()), -1, true);
				if (Options.isDynamicBackgroundEnabled())
					bgAlpha = 0f;
			} else
				MusicController.setPosition(0);
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

		// start moving logo (if clicked)
		else if (!logoClicked) {
			if (logo.contains(x, y)) {
				logoClicked = true;
				logoTimer = 0;
				playButton.getImage().setAlpha(0f);
				exitButton.getImage().setAlpha(0f);
				SoundController.playSound(SoundEffect.MENUHIT);
			}
		}

		// other button actions (if visible)
		else if (logoClicked) {
			if (logo.contains(x, y) || playButton.contains(x, y)) {
				SoundController.playSound(SoundEffect.MENUHIT);
				game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			} else if (exitButton.contains(x, y))
				container.exit();
		}
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		Utils.changeVolume((newValue < 0) ? -1 : 1);
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
			if (!logoClicked) {
				logoClicked = true;
				logoTimer = 0;
				playButton.getImage().setAlpha(0f);
				exitButton.getImage().setAlpha(0f);
				SoundController.playSound(SoundEffect.MENUHIT);
			} else {
				SoundController.playSound(SoundEffect.MENUHIT);
				game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			}
			break;
		case Input.KEY_UP:
			Utils.changeVolume(1);
			break;
		case Input.KEY_DOWN:
			Utils.changeVolume(-1);
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		}
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
		downloadsButton.resetHover();
	}
}
