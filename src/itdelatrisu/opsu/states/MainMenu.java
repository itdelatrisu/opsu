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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OpsuConstants;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.beatmap.BeatmapSetNode;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.options.OptionGroup;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.options.OptionsOverlay;
import itdelatrisu.opsu.states.ButtonMenu.MenuState;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.MenuButton.Expand;
import itdelatrisu.opsu.ui.NotificationManager.NotificationListener;
import itdelatrisu.opsu.ui.StarFountain;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import itdelatrisu.opsu.user.UserButton;
import itdelatrisu.opsu.user.UserList;
import itdelatrisu.opsu.user.UserSelectOverlay;

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
import org.newdawn.slick.fills.GradientFill;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.EasedFadeOutTransition;
import org.newdawn.slick.state.transition.FadeInTransition;

/**
 * "Main Menu" state.
 * <p>
 * Players are able to enter the song menu or downloads menu from this state.
 */
public class MainMenu extends BasicGameState {
	/** Idle time, in milliseconds, before returning the logo to its original position. */
	private static final short LOGO_IDLE_DELAY = 10000;

	/** Max alpha level of the menu background. */
	private static final float BG_MAX_ALPHA = 0.9f;

	/** Logo button that reveals other buttons on click. */
	private MenuButton logo;

	/** Logo states. */
	private enum LogoState { DEFAULT, OPENING, OPEN, CLOSING }

	/** Current logo state. */
	private LogoState logoState = LogoState.DEFAULT;

	/** Delay timer, in milliseconds, before starting to move the logo back to the center. */
	private int logoTimer = 0;

	/** Logo horizontal offset for opening and closing actions. */
	private AnimatedValue logoOpen, logoClose;

	/** Logo button alpha levels. */
	private AnimatedValue logoButtonAlpha;

	/** Main "Play" and "Exit" buttons. */
	private MenuButton playButton, exitButton;

	/** Music control buttons. */
	private MenuButton musicPlay, musicPause, musicNext, musicPrevious;

	/** Button linking to Downloads menu. */
	private MenuButton downloadsButton;

	/** Button linking to repository. */
	private MenuButton repoButton;

	/** Buttons for installing updates. */
	private MenuButton updateButton, restartButton;

	/** Application start time, for drawing the total running time. */
	private long programStartTime;

	/** Indexes of previous songs. */
	private Stack<Integer> previous;

	/** Background alpha level (for fade-in effect). */
	private AnimatedValue bgAlpha = new AnimatedValue(1100, 0f, BG_MAX_ALPHA, AnimationEquation.LINEAR);

	/** Whether or not a notification was already sent upon entering. */
	private boolean enterNotification = false;

	/** Music position bar coordinates and dimensions. */
	private float musicBarX, musicBarY, musicBarWidth, musicBarHeight;

	/** Last measure progress value. */
	private float lastMeasureProgress = 0f;

	/** The star fountain. */
	private StarFountain starFountain;

	/** Music info bar "Now Playing" image. */
	private Image musicInfoImg;

	/** Music info bar rectangle. */
	private Rectangle musicInfoRect;

	/** Music info bar fill. */
	private GradientFill musicInfoFill;

	/** Music info bar animation progress. */
	private AnimatedValue musicInfoProgress = new AnimatedValue(600, 0f, 1f, AnimationEquation.OUT_CUBIC);

	/** Options overlay. */
	private OptionsOverlay optionsOverlay;

	/** Whether the options overlay is being shown. */
	private boolean showOptionsOverlay = false;

	/** The options overlay show/hide animation progress. */
	private AnimatedValue optionsOverlayProgress = new AnimatedValue(500, 0f, 1f, AnimationEquation.LINEAR);

	/** The user button. */
	private UserButton userButton;

	/** Whether the user button has been flashed. */
	private boolean userButtonFlashed = false;

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

	public MainMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		this.input = container.getInput();

		programStartTime = System.currentTimeMillis();
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
		final int logoAnimationDuration = 350;
		logo.setHoverAnimationDuration(logoAnimationDuration);
		playButton.setHoverAnimationDuration(logoAnimationDuration);
		exitButton.setHoverAnimationDuration(logoAnimationDuration);
		final AnimationEquation logoAnimationEquation = AnimationEquation.IN_OUT_BACK;
		logo.setHoverAnimationEquation(logoAnimationEquation);
		playButton.setHoverAnimationEquation(logoAnimationEquation);
		exitButton.setHoverAnimationEquation(logoAnimationEquation);
		final float logoHoverScale = 1.08f;
		logo.setHoverExpand(logoHoverScale);
		playButton.setHoverExpand(logoHoverScale);
		exitButton.setHoverExpand(logoHoverScale);

		// initialize music info bar
		int musicInfoHeight = (int) (Fonts.MEDIUM.getLineHeight() * 1.3f);
		Image infoImg = GameImage.MUSIC_NOW_PLAYING.getImage();
		musicInfoImg = infoImg.getScaledCopy((float) musicInfoHeight / infoImg.getHeight());
		musicInfoFill = new GradientFill(
			0, 0, Color.transparent,
			musicInfoImg.getWidth(), musicInfoImg.getHeight(), new Color(0, 0, 0, 0.9f),
			true
		);
		musicInfoRect = new Rectangle(0, 0, 1, musicInfoHeight);

		// initialize music buttons
		int musicInfoOffset = (int) (musicInfoHeight * 0.6f);
		int musicWidth  = GameImage.MUSIC_PLAY.getImage().getWidth();
		int musicHeight = GameImage.MUSIC_PLAY.getImage().getHeight();
		musicPlay     = new MenuButton(GameImage.MUSIC_PLAY.getImage(), width - (2 * musicWidth), musicInfoOffset + musicHeight / 1.5f);
		musicPause    = new MenuButton(GameImage.MUSIC_PAUSE.getImage(), width - (2 * musicWidth), musicInfoOffset + musicHeight / 1.5f);
		musicNext     = new MenuButton(GameImage.MUSIC_NEXT.getImage(), width - musicWidth, musicInfoOffset + musicHeight / 1.5f);
		musicPrevious = new MenuButton(GameImage.MUSIC_PREVIOUS.getImage(), width - (3 * musicWidth), musicInfoOffset + musicHeight / 1.5f);
		musicPlay.setHoverExpand(1.5f);
		musicPause.setHoverExpand(1.5f);
		musicNext.setHoverExpand(1.5f);
		musicPrevious.setHoverExpand(1.5f);

		// initialize music position bar location
		musicBarX = width - musicWidth * 3.5f;
		musicBarY = musicInfoOffset + musicHeight * 1.1f;
		musicBarWidth = musicWidth * 3f;
		musicBarHeight = musicHeight * 0.11f;

		// initialize downloads button
		Image dlImg = GameImage.DOWNLOADS.getImage();
		downloadsButton = new MenuButton(dlImg, width - dlImg.getWidth() / 2f, height / 2f);
		downloadsButton.setHoverAnimationDuration(350);
		downloadsButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
		downloadsButton.setHoverExpand(1.03f, Expand.LEFT);

		// initialize repository button
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {  // only if a webpage can be opened
			Image repoImg = GameImage.REPOSITORY.getImage();
			int repoMargin = (int) (height * 0.01f);
			float repoScale = 1.25f;
			repoButton = new MenuButton(repoImg,
				repoMargin + repoImg.getWidth() * repoScale / 2,
				height - repoMargin - repoImg.getHeight() * repoScale / 2
			);
			repoButton.setHoverAnimationDuration(350);
			repoButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
			repoButton.setHoverExpand(repoScale);
		}

		// initialize update buttons
		float updateX = width / 2f, updateY = height * 17 / 18f;
		Image downloadImg = GameImage.DOWNLOAD.getImage();
		updateButton = new MenuButton(downloadImg, updateX, updateY);
		updateButton.setHoverAnimationDuration(400);
		updateButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_QUAD);
		updateButton.setHoverExpand(1.1f);
		Image updateImg = GameImage.UPDATE.getImage();
		restartButton = new MenuButton(updateImg, updateX, updateY);
		restartButton.setHoverAnimationDuration(2000);
		restartButton.setHoverAnimationEquation(AnimationEquation.LINEAR);
		restartButton.setHoverRotate(360);

		// initialize star fountain
		starFountain = new StarFountain(width, height);

		// logo animations
		float centerOffsetX = width / 5f;
		logoOpen = new AnimatedValue(400, 0, centerOffsetX, AnimationEquation.OUT_QUAD);
		logoClose = new AnimatedValue(2200, centerOffsetX, 0, AnimationEquation.OUT_QUAD);
		logoButtonAlpha = new AnimatedValue(200, 0f, 1f, AnimationEquation.LINEAR);

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

		// initialize user button
		userButton = new UserButton(0, 0, Color.white);

		// initialize user selection overlay
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

		reset();
		musicInfoProgress.setTime(0);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();

		Beatmap beatmap = MusicController.getBeatmap();

		// draw background
		float parallaxX = 0, parallaxY = 0;
		if (Options.isParallaxEnabled()) {
			int offset = (int) (height * (GameImage.PARALLAX_SCALE - 1f));
			parallaxX = -offset / 2f * (mouseX - width / 2) / (width / 2);
			parallaxY = -offset / 2f * (mouseY - height / 2) / (height / 2);
		}
		if (Options.isDynamicBackgroundEnabled() && beatmap != null &&
			beatmap.drawBackground(width, height, parallaxX, parallaxY, bgAlpha.getValue(), true))
			;
		else {
			Image bg = GameImage.MENU_BG.getImage();
			if (Options.isParallaxEnabled()) {
				bg = bg.getScaledCopy(GameImage.PARALLAX_SCALE);
				bg.setAlpha(bgAlpha.getValue());
				bg.drawCentered(width / 2 + parallaxX, height / 2 + parallaxY);
			} else {
				bg.setAlpha(bgAlpha.getValue());
				bg.drawCentered(width / 2, height / 2);
			}
		}

		// top/bottom horizontal bars
		g.setColor(Colors.BLACK_ALPHA);
		g.fillRect(0, 0, width, height / 9f);
		g.fillRect(0, height * 8 / 9f, width, height / 9f);

		// draw star fountain
		starFountain.draw();

		// draw downloads button
		downloadsButton.draw();

		// draw buttons
		if (logoState == LogoState.OPEN || logoState == LogoState.CLOSING) {
			playButton.draw();
			exitButton.draw();
		}

		// draw logo (pulsing)
		Float position = MusicController.getBeatProgress();
		if (position == null)  // default to 60bpm
			position = System.currentTimeMillis() % 1000 / 1000f;
		float scale = 1f + position * 0.05f;
		logo.draw(Color.white, scale);
		float ghostScale = logo.getLastScale() / scale * 1.05f;
		Image ghostLogo = GameImage.MENU_LOGO.getImage().getScaledCopy(ghostScale);
		ghostLogo.drawCentered(logo.getX(), logo.getY(), Colors.GHOST_LOGO);

		// draw music info bar
		if (MusicController.trackExists()) {
			if (!beatmap.source.isEmpty())
				Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.source);
			if (Options.useUnicodeMetadata()) {  // load glyphs
				Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.titleUnicode);
				Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.artistUnicode);
			}
			float t = musicInfoProgress.getValue();
			int animX = (int) ((1f - t) * (musicInfoImg.getWidth() * 2));
			String s = String.format("%s - %s", beatmap.getArtist(), beatmap.getTitle());
			int sWidth = Fonts.MEDIUM.getWidth(s);
			int margin = (int) (width * 0.01f);
			int rectHeight = (int) musicInfoRect.getHeight();
			int imgX = width - margin * 2 - sWidth - musicInfoImg.getWidth() + animX;
			int rectX = imgX - margin, rectWidth = width - rectX;
			musicInfoRect.setX(rectX);
			musicInfoRect.setWidth(rectWidth);
			musicInfoFill.setStart(margin, 0);
			g.fill(musicInfoRect, musicInfoFill);
			g.setLineWidth(2f);
			g.drawGradientLine(rectX, rectHeight, 0f, 0f, 0f, 0f, width, rectHeight, 1f, 1f, 1f, 1f);
			g.resetLineWidth();
			musicInfoImg.setAlpha(t);
			musicInfoImg.draw(imgX, 0);
			float oldWhiteAlpha = Colors.WHITE_FADE.a;
			Colors.WHITE_FADE.a = t;
			Fonts.MEDIUM.drawString(
				width - margin - sWidth + animX,
				musicInfoImg.getHeight() / 2 - Fonts.MEDIUM.getLineHeight() / 2,
				s, Colors.WHITE_FADE);
			Colors.WHITE_FADE.a = oldWhiteAlpha;
		}

		// draw music buttons
		if (MusicController.isPlaying())
			musicPause.draw();
		else
			musicPlay.draw();
		musicNext.draw();
		musicPrevious.draw();

		// draw music position bar
		boolean inMusicPosBar = musicPositionBarContains(mouseX, mouseY);
		g.setColor(inMusicPosBar ? Colors.BLACK_BG_HOVER : Colors.BLACK_BG_NORMAL);
		g.fillRect(musicBarX, musicBarY, musicBarWidth, musicBarHeight);
		if (!MusicController.isTrackLoading() && beatmap != null) {
			float oldWhiteAlpha = Colors.WHITE_FADE.a;
			float musicPosAlpha = inMusicPosBar ? 0.8f : 0.65f;
			Colors.WHITE_FADE.a = musicPosAlpha;
			g.setColor(Colors.WHITE_FADE);
			float musicBarPosition = Math.min((float) MusicController.getPosition(false) / MusicController.getDuration(), 1f);
			g.fillRect(musicBarX, musicBarY, musicBarWidth * musicBarPosition, musicBarHeight);
			Colors.WHITE_FADE.a = oldWhiteAlpha;
		}

		// draw repository button
		if (repoButton != null)
			repoButton.draw();

		// draw update button
		if (Updater.get().showButton()) {
			Updater.Status status = Updater.get().getStatus();
			if (status == Updater.Status.UPDATE_AVAILABLE || status == Updater.Status.UPDATE_DOWNLOADING)
				updateButton.draw();
			else if (status == Updater.Status.UPDATE_DOWNLOADED)
				restartButton.draw();
		}

		// draw user button
		userButton.setUser(UserList.get().getCurrentUser());
		userButton.draw(g);

		// draw text
		float textAlpha;
		if (logoState == LogoState.DEFAULT)
			textAlpha = 0f;
		else if (logoState == LogoState.OPEN)
			textAlpha = 1f;
		else if (logoState == LogoState.OPENING)
			textAlpha = logoOpen.getEquation().calc((float) logoOpen.getTime() / logoOpen.getDuration());
		else //if (logoState == LogoState.CLOSING)
			textAlpha = 1f - logoClose.getEquation().calc(Math.min(logoClose.getTime() * 2f / logoClose.getDuration(), 1f));
		float oldWhiteAlpha = Colors.WHITE_FADE.a;
		Colors.WHITE_FADE.a = textAlpha;
		float marginX = UserButton.getWidth() + 8, topMarginY = 4;
		Fonts.MEDIUM.drawString(marginX, topMarginY,
			String.format("You have %d beatmaps available!", BeatmapSetList.get().getMapCount()),
			Colors.WHITE_FADE
		);
		float lineHeight = Fonts.MEDIUM.getLineHeight() * 0.925f;
		Fonts.MEDIUM.drawString(marginX, topMarginY + lineHeight,
			String.format("%s has been running for %s.",
				OpsuConstants.PROJECT_NAME,
				Utils.getTimeString((int) (System.currentTimeMillis() - programStartTime) / 1000)),
			Colors.WHITE_FADE
		);
		lineHeight += Fonts.MEDIUM.getLineHeight() * 0.925f;
		Fonts.MEDIUM.drawString(marginX, topMarginY + lineHeight,
			String.format("It is currently %s.",
				new SimpleDateFormat("h:mm a").format(new Date())),
			Colors.WHITE_FADE
		);
		Colors.WHITE_FADE.a = oldWhiteAlpha;

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
		if (MusicController.trackEnded())
			nextTrack(false);  // end of track: go to next track
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		if (showOptionsOverlay || showUserOverlay) {
			logo.hoverUpdate(delta, false);
			playButton.hoverUpdate(delta, false);
			exitButton.hoverUpdate(delta, false);
		} else {
			logo.hoverUpdate(delta, mouseX, mouseY, 0.25f);
			playButton.hoverUpdate(delta, mouseX, mouseY, 0.25f);
			exitButton.hoverUpdate(delta, mouseX, mouseY, 0.25f);
		}
		if (repoButton != null)
			repoButton.hoverUpdate(delta, mouseX, mouseY);
		if (Updater.get().showButton()) {
			updateButton.autoHoverUpdate(delta, true);
			restartButton.autoHoverUpdate(delta, false);
		}
		downloadsButton.hoverUpdate(delta, mouseX, mouseY);
		// ensure only one button is in hover state at once
		boolean noHoverUpdate = musicPositionBarContains(mouseX, mouseY);
		boolean contains = musicPlay.contains(mouseX, mouseY);
		musicPlay.hoverUpdate(delta, !noHoverUpdate && contains);
		musicPause.hoverUpdate(delta, !noHoverUpdate && contains);
		noHoverUpdate |= contains;
		musicNext.hoverUpdate(delta, !noHoverUpdate && musicNext.contains(mouseX, mouseY));
		musicPrevious.hoverUpdate(delta, !noHoverUpdate && musicPrevious.contains(mouseX, mouseY));
		starFountain.update(delta);
		if (!userButtonFlashed) {  // flash user button once
			userButton.flash();
			userButtonFlashed = true;
		}
		userButton.hoverUpdate(delta, userButton.contains(mouseX, mouseY));
		if (MusicController.trackExists())
			musicInfoProgress.update(delta);

		// window focus change: increase/decrease theme song volume
		if (MusicController.isThemePlaying() &&
		    MusicController.isTrackDimmed() == container.hasFocus())
				MusicController.toggleTrackDimmed(0.33f);

		// fade in background
		Beatmap beatmap = MusicController.getBeatmap();
		if (!(Options.isDynamicBackgroundEnabled() && beatmap != null && beatmap.isBackgroundLoading()))
			bgAlpha.update(delta);

		// check measure progress
		Float measureProgress = MusicController.getMeasureProgress(2);
		if (measureProgress != null) {
			if (measureProgress < lastMeasureProgress)
				starFountain.burst(true);
			lastMeasureProgress = measureProgress;
		}

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

		// buttons
		int centerX = container.getWidth() / 2;
		float currentLogoButtonAlpha;
		switch (logoState) {
		case DEFAULT:
			break;
		case OPENING:
			if (logoOpen.update(delta))  // shifting to left
				logo.setX(centerX - logoOpen.getValue());
			else {
				logoState = LogoState.OPEN;
				logoTimer = 0;
				logoButtonAlpha.setTime(0);
			}
			break;
		case OPEN:
			if (logoButtonAlpha.update(delta)) {  // fade in buttons
				currentLogoButtonAlpha = logoButtonAlpha.getValue();
				playButton.getImage().setAlpha(currentLogoButtonAlpha);
				exitButton.getImage().setAlpha(currentLogoButtonAlpha);
			} else if (logoTimer >= LOGO_IDLE_DELAY) {  // timer over: shift back to center
				logoState = LogoState.CLOSING;
				logoClose.setTime(0);
				logoTimer = 0;
			} else  // increment timer
				logoTimer += delta;
			break;
		case CLOSING:
			if (logoButtonAlpha.update(-delta)) {  // fade out buttons
				currentLogoButtonAlpha = logoButtonAlpha.getValue();
				playButton.getImage().setAlpha(currentLogoButtonAlpha);
				exitButton.getImage().setAlpha(currentLogoButtonAlpha);
			}
			if (logoClose.update(delta))  // shifting to right
				logo.setX(centerX - logoClose.getValue());
			else
				logoState = LogoState.DEFAULT;
			break;
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
		else if (repoButton != null && repoButton.contains(mouseX, mouseY)) {
			String version = Updater.get().getCurrentVersion();
			String tooltip = String.format(
				"running %s %s\ncreated by %s",
				OpsuConstants.PROJECT_NAME,
				(version == null) ? "(unknown version)" : "v" + version,
				OpsuConstants.PROJECT_AUTHOR
			);
			UI.updateTooltip(delta, tooltip, true);
		} else if (Updater.get().showButton()) {
			Updater.Status status = Updater.get().getStatus();
			if (((status == Updater.Status.UPDATE_AVAILABLE || status == Updater.Status.UPDATE_DOWNLOADING) && updateButton.contains(mouseX, mouseY)) ||
			    (status == Updater.Status.UPDATE_DOWNLOADED && restartButton.contains(mouseX, mouseY)))
				UI.updateTooltip(delta, status.getDescription(), true);
		}
	}

	@Override
	public int getID() { return state; }

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		if (!enterNotification) {
			if (Updater.get().getStatus() == Updater.Status.UPDATE_AVAILABLE) {
				UI.getNotificationManager().sendNotification("A new update is available!", Colors.GREEN);
				enterNotification = true;
			} else if (Updater.get().justUpdated()) {
				String updateMessage = OpsuConstants.PROJECT_NAME + " is now up to date!";
				final String version = Updater.get().getCurrentVersion();
				if (version != null && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					updateMessage += "\nClick to see what changed!";
					UI.getNotificationManager().sendNotification(updateMessage, Colors.GREEN, new NotificationListener() {
						@Override
						public void click() {
							try {
								Desktop.getDesktop().browse(OpsuConstants.getChangelogURI(version));
							} catch (IOException e) {
								UI.getNotificationManager().sendBarNotification("The web page could not be opened.");
							}
						}
					});
				} else
					UI.getNotificationManager().sendNotification(updateMessage);
				enterNotification = true;
			}
		}

		// reset measure info
		lastMeasureProgress = 0f;
		starFountain.clear();

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
		updateButton.resetHover();
		restartButton.resetHover();
		if (!downloadsButton.contains(mouseX, mouseY))
			downloadsButton.resetHover();
		if (!userButton.contains(mouseX, mouseY))
			userButton.resetHover();

		// reset overlays
		optionsOverlay.deactivate();
		optionsOverlay.reset();
		showOptionsOverlay = false;
		optionsOverlayProgress.setTime(optionsOverlayProgress.getDuration());
		userOverlay.deactivate();
		showUserOverlay = false;
		userOverlayProgress.setTime(userOverlayProgress.getDuration());
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
		if (MusicController.isTrackDimmed())
			MusicController.toggleTrackDimmed(1f);

		// reset overlays
		optionsOverlay.deactivate();
		optionsOverlay.reset();
		showOptionsOverlay = false;
		userOverlay.deactivate();
		showUserOverlay = false;
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		if (showOptionsOverlay || !optionsOverlayProgress.isFinished() ||
		    showUserOverlay || !userOverlayProgress.isFinished())
			return;

		// music position bar
		if (MusicController.isPlaying()) {
			if (musicPositionBarContains(x, y)) {
				lastMeasureProgress = 0f;
				float pos = (x - musicBarX) / musicBarWidth;
				MusicController.setPosition((int) (pos * MusicController.getDuration()));
				return;
			}
		}

		// music button actions
		if (musicPlay.contains(x, y)) {
			if (MusicController.isPlaying()) {
				MusicController.pause();
				UI.getNotificationManager().sendBarNotification("Pause");
			} else if (!MusicController.isTrackLoading()) {
				MusicController.resume();
				UI.getNotificationManager().sendBarNotification("Play");
			}
			return;
		} else if (musicNext.contains(x, y)) {
			nextTrack(true);
			UI.getNotificationManager().sendBarNotification(">> Next");
			return;
		} else if (musicPrevious.contains(x, y)) {
			previousTrack();
			UI.getNotificationManager().sendBarNotification("<< Prev");
			return;
		}

		// downloads button actions
		if (downloadsButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			game.enterState(Opsu.STATE_DOWNLOADSMENU, new EasedFadeOutTransition(), new FadeInTransition());
			return;
		}

		// repository button actions
		if (repoButton != null && repoButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(MenuState.ABOUT);
			game.enterState(Opsu.STATE_BUTTONMENU);
			return;
		}

		// update button actions
		if (Updater.get().showButton()) {
			Updater.Status status = Updater.get().getStatus();
			if (updateButton.contains(x, y) && status == Updater.Status.UPDATE_AVAILABLE) {
				SoundController.playSound(SoundEffect.MENUHIT);
				Updater.get().startDownload();
				updateButton.removeHoverEffects();
				updateButton.setHoverAnimationDuration(800);
				updateButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_QUAD);
				updateButton.setHoverFade(0.6f);
				return;
			} else if (restartButton.contains(x, y) && status == Updater.Status.UPDATE_DOWNLOADED) {
				SoundController.playSound(SoundEffect.MENUHIT);
				Updater.get().prepareUpdate();
				container.setForceExit(false);
				container.exit();
				return;
			}
		}

		// user button actions
		if (userButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUCLICK);
			showUserOverlay = true;
			userOverlayProgress.setTime(0);
			userOverlay.activate();
			return;
		}

		// start moving logo (if clicked)
		if (logoState == LogoState.DEFAULT || logoState == LogoState.CLOSING) {
			if (logo.contains(x, y, 0.25f)) {
				SoundController.playSound(SoundEffect.MENUHIT);
				openLogoMenu();
				return;
			}
		}

		// other button actions (if visible)
		else if (logoState == LogoState.OPEN || logoState == LogoState.OPENING) {
			if (logo.contains(x, y, 0.25f) || playButton.contains(x, y, 0.25f)) {
				SoundController.playSound(SoundEffect.MENUHIT);
				enterSongMenu();
				return;
			} else if (exitButton.contains(x, y, 0.25f)) {
				container.exit();
				return;
			}
		}
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		UI.globalMouseWheelMoved(newValue, false);
	}

	@Override
	public void keyPressed(int key, char c) {
		if (UI.globalKeyPressed(key))
			return;

		switch (key) {
		case Input.KEY_ESCAPE:
		case Input.KEY_Q:
			((ButtonMenu) game.getState(Opsu.STATE_BUTTONMENU)).setMenuState(MenuState.EXIT);
			game.enterState(Opsu.STATE_BUTTONMENU);
			break;
		case Input.KEY_P:
			SoundController.playSound(SoundEffect.MENUHIT);
			if (logoState == LogoState.DEFAULT || logoState == LogoState.CLOSING)
				openLogoMenu();
			else
				enterSongMenu();
			break;
		case Input.KEY_D:
			SoundController.playSound(SoundEffect.MENUHIT);
			game.enterState(Opsu.STATE_DOWNLOADSMENU, new EasedFadeOutTransition(), new FadeInTransition());
			break;
		case Input.KEY_O:
			SoundController.playSound(SoundEffect.MENUHIT);
			if ((logoState == LogoState.DEFAULT || logoState == LogoState.CLOSING) &&
			    !(input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)))
				openLogoMenu();
			else {
				showOptionsOverlay = true;
				optionsOverlayProgress.setTime(0);
				optionsOverlay.activate();
				input.consumeEvent();  // don't let options overlay consume this keypress
			}
			break;
		case Input.KEY_F:
			Options.toggleFPSCounter();
			break;
		case Input.KEY_Z:
			previousTrack();
			UI.getNotificationManager().sendBarNotification("<< Prev");
			break;
		case Input.KEY_X:
			if (MusicController.isPlaying()) {
				lastMeasureProgress = 0f;
				MusicController.setPosition(0);
			} else if (!MusicController.isTrackLoading())
				MusicController.resume();
			UI.getNotificationManager().sendBarNotification("Play");
			break;
		case Input.KEY_C:
			if (MusicController.isPlaying()) {
				MusicController.pause();
				UI.getNotificationManager().sendBarNotification("Pause");
			} else if (!MusicController.isTrackLoading()) {
				MusicController.resume();
				UI.getNotificationManager().sendBarNotification("Unpause");
			}
			break;
		case Input.KEY_V:
			nextTrack(true);
			UI.getNotificationManager().sendBarNotification(">> Next");
			break;
		case Input.KEY_R:
			nextTrack(true);
			break;
		case Input.KEY_UP:
			UI.changeVolume(1);
			break;
		case Input.KEY_DOWN:
			UI.changeVolume(-1);
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
		logoOpen.setTime(0);
		logoClose.setTime(0);
		logoButtonAlpha.setTime(0);
		logoTimer = 0;
		logoState = LogoState.DEFAULT;

		musicInfoProgress.setTime(musicInfoProgress.getDuration());
		optionsOverlay.deactivate();
		optionsOverlay.reset();
		showOptionsOverlay = false;
		optionsOverlayProgress.setTime(optionsOverlayProgress.getDuration());
		userOverlay.deactivate();
		showUserOverlay = false;
		userOverlayProgress.setTime(userOverlayProgress.getDuration());

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
		restartButton.resetHover();
		downloadsButton.resetHover();
		userButton.resetHover();
	}

	/**
	 * Opens the logo menu.
	 */
	private void openLogoMenu() {
		logoState = LogoState.OPENING;
		logoOpen.setTime(0);
		logoTimer = 0;
		playButton.getImage().setAlpha(0f);
		exitButton.getImage().setAlpha(0f);
	}

	/**
	 * Plays the next track, and adds the previous one to the stack.
	 * @param user {@code true} if this was user-initiated, false otherwise (track end)
	 */
	private void nextTrack(boolean user) {
		lastMeasureProgress = 0f;
		boolean isTheme = MusicController.isThemePlaying();
		if (isTheme && !user) {
			// theme was playing, restart
			// NOTE: not looping due to inaccurate track positions after loop
			MusicController.playAt(0, false);
			return;
		}
		SongMenu menu = (SongMenu) game.getState(Opsu.STATE_SONGMENU);
		BeatmapSetNode node = menu.setFocus(BeatmapSetList.get().getRandomNode(), -1, true, false);
		boolean sameAudio = false;
		if (node != null) {
			sameAudio = MusicController.getBeatmap().audioFilename.equals(node.getBeatmapSet().get(0).audioFilename);
			if (!isTheme && !sameAudio)
				previous.add(node.index);
		}
		if (Options.isDynamicBackgroundEnabled() && !sameAudio && !MusicController.isThemePlaying())
			bgAlpha.setTime(0);
		musicInfoProgress.setTime(0);
	}

	/**
	 * Plays the previous track, or does nothing if the stack is empty.
	 */
	private void previousTrack() {
		if (!previous.isEmpty()) {
			SongMenu menu = (SongMenu) game.getState(Opsu.STATE_SONGMENU);
			menu.setFocus(BeatmapSetList.get().getBaseNode(previous.pop()), -1, true, false);
			lastMeasureProgress = 0f;
			if (Options.isDynamicBackgroundEnabled())
				bgAlpha.setTime(0);
		}
		musicInfoProgress.setTime(0);
	}

	/**
	 * Enters the song menu, or the downloads menu if no beatmaps are loaded.
	 */
	private void enterSongMenu() {
		int state = Opsu.STATE_SONGMENU;
		if (BeatmapSetList.get().getMapSetCount() == 0) {
			((DownloadsMenu) game.getState(Opsu.STATE_DOWNLOADSMENU)).notifyOnLoad("Download some beatmaps to get started!");
			state = Opsu.STATE_DOWNLOADSMENU;
		}
		game.enterState(state, new EasedFadeOutTransition(), new FadeInTransition());
	}
}
