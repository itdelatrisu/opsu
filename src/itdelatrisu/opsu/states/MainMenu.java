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
import itdelatrisu.opsu.OsuGroupNode;
import itdelatrisu.opsu.SoundController;
import itdelatrisu.opsu.Utils;

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
	/**
	 * Idle time, in milliseconds, before returning the logo to its original position.
	 */
	private static final short MOVE_DELAY = 5000;

	/**
	 * Logo button that reveals other buttons on click.
	 */
	private GUIMenuButton logo;

	/**
	 * Whether or not the logo has been clicked.
	 */
	private boolean logoClicked = false;

	/**
	 * Delay timer, in milliseconds, before starting to move the logo back to the center.
	 */
	private int logoTimer = 0;

	/**
	 * Main "Play" and "Exit" buttons.
	 */
	private GUIMenuButton playButton, exitButton;

	/**
	 * Music control buttons.
	 */
	private GUIMenuButton musicPlay, musicPause, musicNext, musicPrevious;

	/**
	 * Application start time, for drawing the total running time.
	 */
	private long osuStartTime;

	/**
	 * Indexes of previous songs.
	 */
	private static Stack<Integer> previous;

	/**
	 * Main menu background image (optional).
	 */
	private Image backgroundImage;

	/**
	 * Background alpha level (for fade-in effect).
	 */
	private float bgAlpha = 0f;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private int state;

	public MainMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;

		osuStartTime = System.currentTimeMillis();
		previous = new Stack<Integer>();

		int width = container.getWidth();
		int height = container.getHeight();

		// initialize buttons
		Image logoImg = new Image("logo.png");
		float buttonScale = (height / 1.2f) / logoImg.getHeight();
		Image logoImgScaled = logoImg.getScaledCopy(buttonScale);
		logo = new GUIMenuButton(logoImgScaled, width / 2f, height / 2f);

		Image playImg = new Image("menu-play.png");
		Image exitImg = new Image("menu-exit.png");
		playImg = playImg.getScaledCopy((logoImg.getWidth() * 0.83f) / playImg.getWidth());
		exitImg = exitImg.getScaledCopy((logoImg.getWidth() * 0.66f) / exitImg.getWidth());
		float exitOffset = (playImg.getWidth() - exitImg.getWidth()) / 3f;
		playButton = new GUIMenuButton(playImg.getScaledCopy(buttonScale),
				width * 0.75f, (height / 2) - (logoImgScaled.getHeight() / 5f)
		);
		exitButton = new GUIMenuButton(exitImg.getScaledCopy(buttonScale),
				width * 0.75f - exitOffset, (height / 2) + (exitImg.getHeight() / 2f)
		);

		// initialize music buttons
		int musicWidth  = 48;
		int musicHeight = 30;
		musicPlay     = new GUIMenuButton(new Image("music-play.png"), width - (2 * musicWidth), musicHeight);
		musicPause    = new GUIMenuButton(new Image("music-pause.png"), width - (2 * musicWidth), musicHeight);
		musicNext     = new GUIMenuButton(new Image("music-next.png"), width - musicWidth, musicHeight);
		musicPrevious = new GUIMenuButton(new Image("music-previous.png"), width - (3 * musicWidth), musicHeight);

		// menu background
		try {
			backgroundImage = new Image("menu-background.jpg").getScaledCopy(width, height);
			backgroundImage.setAlpha(0.9f);
		} catch (Exception e) {
			// optional
		}
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();
		
		// draw background
		OsuFile osu = MusicController.getOsuFile();
		if (Options.isDynamicBackgroundEnabled() &&
			osu != null && osu.drawBG(width, height, bgAlpha))
				;
		else if (backgroundImage != null) {
			backgroundImage.setAlpha(bgAlpha);
			backgroundImage.draw();
		} else
			g.setBackground(Utils.COLOR_BLUE_BACKGROUND);

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

		// draw text
		g.setFont(Utils.FONT_MEDIUM);
		int lineHeight = Utils.FONT_MEDIUM.getLineHeight();
		g.drawString(String.format("Loaded %d songs and %d beatmaps.",
				Opsu.groups.size(), Opsu.groups.getMapCount()), 25, 25);
		if (MusicController.isTrackLoading())
			g.drawString("Track loading...", 25, 25 + lineHeight);
		else if (MusicController.trackExists()) {
			g.drawString((MusicController.isPlaying()) ? "Now Playing:" : "Paused:", 25, 25 + lineHeight);
			g.drawString(String.format("%s: %s",
					MusicController.getArtistName(),
					MusicController.getTrackName()),
					50, 25 + (lineHeight * 2));
		}
		long time = System.currentTimeMillis() - osuStartTime;
		g.drawString(String.format("opsu! has been running for %d minutes, %d seconds.",
				TimeUnit.MILLISECONDS.toMinutes(time),
				TimeUnit.MILLISECONDS.toSeconds(time) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))),
				25, height - 25 - (lineHeight * 2));
		g.drawString(String.format("The current time is %s.",
				new SimpleDateFormat("h:mm a").format(new Date())),
				25, height - 25 - lineHeight);

		Utils.drawFPS();
		Utils.drawCursor();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		Utils.updateCursor(delta);

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
		logoClicked = false;
		logoTimer = 0;
		logo.setX(container.getWidth() / 2);
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button 
		if (button != Input.MOUSE_LEFT_BUTTON)
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
			OsuGroupNode node = menu.setFocus(Opsu.groups.getRandomNode(), -1, true);
			if (node != null && !isTheme)
				previous.add(node.index);
			if (Options.isDynamicBackgroundEnabled() &&
				MusicController.getOsuFile() != null && !MusicController.isThemePlaying())
				bgAlpha = 0f;
		} else if (musicPrevious.contains(x, y)) {
			if (!previous.isEmpty()) {
				SongMenu menu = (SongMenu) game.getState(Opsu.STATE_SONGMENU);
				menu.setFocus(Opsu.groups.getBaseNode(previous.pop()), -1, true);
				if (Options.isDynamicBackgroundEnabled())
					bgAlpha = 0f;
			} else
				MusicController.setPosition(0);
		}

		// start moving logo (if clicked)
		else if (!logoClicked) {
			if (logo.contains(x, y)) {
				logoClicked = true;
				logoTimer = 0;
				playButton.getImage().setAlpha(0f);
				exitButton.getImage().setAlpha(0f);
				SoundController.playSound(SoundController.SOUND_MENUHIT);
			}
		}

		// other button actions (if visible)
		else if (logoClicked) {
			if (logo.contains(x, y) || playButton.contains(x, y)) {
				SoundController.playSound(SoundController.SOUND_MENUHIT);
				game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			} else if (exitButton.contains(x, y)) {
				SoundController.playSound(SoundController.SOUND_MENUHIT);
				Options.saveOptions();
				Opsu.closeSocket();
				container.exit();
			}
		}
	}

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
			if (logoClicked)
				logoTimer = MOVE_DELAY;
			else
				game.enterState(Opsu.STATE_MAINMENUEXIT);
			break;
		case Input.KEY_Q:
			game.enterState(Opsu.STATE_MAINMENUEXIT);
			break;
		case Input.KEY_P:
			if (!logoClicked) {
				logoClicked = true;
				logoTimer = 0;
				playButton.getImage().setAlpha(0f);
				exitButton.getImage().setAlpha(0f);
				SoundController.playSound(SoundController.SOUND_MENUHIT);
			} else {
				SoundController.playSound(SoundController.SOUND_MENUHIT);
				game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			}
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		}
		
	}
}
