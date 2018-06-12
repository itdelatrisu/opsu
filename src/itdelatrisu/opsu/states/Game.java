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
import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.ScoreData;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.HitSound;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapHPDropRateCalculator;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.beatmap.TimingPoint;
import itdelatrisu.opsu.db.BeatmapDB;
import itdelatrisu.opsu.db.ScoreDB;
import itdelatrisu.opsu.objects.Circle;
import itdelatrisu.opsu.objects.DummyObject;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.Slider;
import itdelatrisu.opsu.objects.Spinner;
import itdelatrisu.opsu.objects.curves.Curve;
import itdelatrisu.opsu.objects.curves.FakeCombinedCurve;
import itdelatrisu.opsu.objects.curves.Vec2f;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.render.FrameBufferCache;
import itdelatrisu.opsu.replay.LifeFrame;
import itdelatrisu.opsu.replay.PlaybackSpeed;
import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.replay.ReplayFrame;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.InputOverlayKey;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.StarStream;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import itdelatrisu.opsu.user.User;
import itdelatrisu.opsu.user.UserList;
import itdelatrisu.opsu.video.FFmpeg;
import itdelatrisu.opsu.video.Video;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.DelayedFadeOutTransition;
import org.newdawn.slick.state.transition.EasedFadeOutTransition;
import org.newdawn.slick.state.transition.EmptyTransition;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.util.Log;

/**
 * "Game" state.
 */
public class Game extends BasicGameState {
	/** Game play states. */
	public enum PlayState {
		/** Normal play. */
		NORMAL,
		/** First time loading the song. */
		FIRST_LOAD,
		/** Manual retry. */
		RETRY,
		/** Replay. */
		REPLAY,
		/** Health is zero: no-continue/force restart. */
		LOSE
	}

	/** Music fade-out time, in milliseconds. */
	private static final int MUSIC_FADEOUT_TIME = 2000;

	/** Screen fade-out time, in milliseconds, when health hits zero. */
	private static final int LOSE_FADEOUT_TIME = 500;

	/** Game element fade-out time, in milliseconds, when the game ends. */
	private static final int FINISHED_FADEOUT_TIME = 400;

	/** Maximum rotation, in degrees, over fade out upon death. */
	private static final float MAX_ROTATION = 90f;

	/** The duration of the score changing animation. */
	private static final float SCOREBOARD_ANIMATION_TIME = 500f;

	/** The time the scoreboard takes to fade in. */
	private static final float SCOREBOARD_FADE_IN_TIME = 300f;

	/** Minimum time before start of song, in milliseconds, to process skip-related actions. */
	private static final int SKIP_OFFSET = 2000;

	/** Tolerance in case if hit object is not snapped to the grid. */
	private static final float STACK_LENIENCE = 3f;

	/** Stack position offset modifier. */
	private static final float STACK_OFFSET_MODIFIER = 0.05f;

	/** The associated beatmap. */
	private Beatmap beatmap;

	/** The associated GameData object. */
	private GameData data;

	/** Current hit object index (in both hit object arrays). */
	private int objectIndex = 0;

	/** The map's game objects, indexed by objectIndex. */
	private GameObject[] gameObjects;

	/** Any passed, unfinished hit object indices before objectIndex. */
	private List<Integer> passedObjects;

	/** Delay time, in milliseconds, before song starts. */
	private int leadInTime;

	/** Hit object approach time, in milliseconds. */
	private int approachTime;

	/** The amount of time for hit objects to fade in, in milliseconds. */
	private int fadeInTime;

	/** Decay time for hit objects in the "Hidden" mod, in milliseconds. */
	private int hiddenDecayTime;

	/** Time before the hit object time by which the objects have completely faded in the "Hidden" mod, in milliseconds. */
	private int hiddenTimeDiff;

	/** Time offsets for obtaining each hit result (indexed by HIT_* constants). */
	private int[] hitResultOffset;

	/** Current play state. */
	private PlayState playState;

	/** Current break index in breaks ArrayList. */
	private int breakIndex;

	/** Break start time (0 if not in break). */
	private int breakTime = 0;

	/** Whether the break sound has been played. */
	private boolean breakSound;

	/** Skip button (displayed at song start, when necessary). */
	private MenuButton skipButton;

	/** Current timing point index in timingPoints ArrayList. */
	private int timingPointIndex;

	/** Current beat lengths (base value and inherited value). */
	private float beatLengthBase, beatLength;

	/** Whether the countdown sound has been played. */
	private boolean
		countdownReadySound, countdown3Sound, countdown1Sound,
		countdown2Sound, countdownGoSound;

	/** Mouse coordinates before game paused. */
	private Vec2f pausedMousePosition;

	/** Track position when game paused. */
	private int pauseTime = -1;

	/** Value for handling hitCircleSelect pulse effect (expanding, alpha level). */
	private float pausePulse;

	/** Whether a checkpoint has been loaded during this game. */
	private boolean checkpointLoaded = false;

	/** Number of deaths, used if "Easy" mod is enabled. */
	private byte deaths = 0;

	/** Track position at death, used if "Easy" mod is enabled. */
	private int deathTime = -1;

	/** System time position at death. */
	private long failTime;

	/** Track time position at death. */
	private int failTrackTime;

	/** Rotations for game objects at death. */
	private IdentityHashMap<GameObject, Float> rotations;

	/** Number of retries. */
	private int retries = 0;

	/** Whether or not this game is a replay. */
	private boolean isReplay = false;

	/** The replay, if any. */
	private Replay replay;

	/** The current replay frame index. */
	private int replayIndex = 0;

	/** The replay cursor coordinates. */
	private int replayX, replayY;

	/** Whether a replay key is currently pressed. */
	private boolean replayKeyPressed;

	/** The replay skip time, or -1 if none. */
	private int replaySkipTime = -1;

	/** The last replay frame time. */
	private int lastReplayTime = 0;

	/** The keys from the previous replay frame. */
	private int lastReplayKeys = 0;

	/** The last game keys pressed. */
	private int lastKeysPressed = ReplayFrame.KEY_NONE;

	/** The previous game mod state (before the replay). */
	private int previousMods = 0;

	/** The list of current replay frames (for recording replays). */
	private LinkedList<ReplayFrame> replayFrames;

	/** The list of current life frames (for recording replays). */
	private LinkedList<LifeFrame> lifeFrames;

	/** The offscreen image rendered to. */
	private Image offscreen;

	/** The offscreen graphics. */
	private Graphics gOffscreen;

	/** The current flashlight area radius. */
	private int flashlightRadius;

	/** The cursor coordinates using the "auto" or "relax" mods. */
	private Vec2f autoMousePosition;

	/** Whether or not the cursor should be pressed using the "auto" mod. */
	private boolean autoMousePressed;

	/** Playback speed (used in replays and "auto" mod). */
	private PlaybackSpeed playbackSpeed;

	/** Whether the game is currently seeking to a replay position. */
	private boolean isSeeking;

	/** Music position bar coordinates and dimensions (for replay seeking). */
	private float musicBarX, musicBarY, musicBarWidth, musicBarHeight;

	/** The previous scores. */
	private ScoreData[] previousScores;

	/** The current rank in the scores. */
	private int currentRank;

	/** The time the rank was last updated. */
	private int lastRankUpdateTime;

	/** Whether the scoreboard is visible. */
	private boolean scoreboardVisible;

	/** The current alpha of the scoreboard. */
	private float currentScoreboardAlpha;

	/** The star stream shown when passing another score. */
	private StarStream scoreboardStarStream;

	/** Whether the game is finished (last hit object passed). */
	private boolean gameFinished = false;

	/** Timer after game has finished, before changing states. */
	private AnimatedValue gameFinishedTimer = new AnimatedValue(2500, 0, 1, AnimationEquation.LINEAR);

	/** The HP drop rate. */
	private float hpDropRate = 0.05f;

	/** The last track position. */
	private int lastTrackPosition = 0;

	/** The beatmap video (if any). */
	private Video video;

	/** The video start time (if any), otherwise -1. */
	private int videoStartTime;

	/** The video seek time (if any). */
	private int videoSeekTime;

	/** The single merged slider (if enabled). */
	private FakeCombinedCurve mergedSlider;

	/** The objects holding data for the input overlay. */
	private InputOverlayKey[] inputOverlayKeys;

	/** Music position bar background colors. */
	private static final Color
		MUSICBAR_NORMAL = new Color(12, 9, 10, 0.25f),
		MUSICBAR_HOVER  = new Color(12, 9, 10, 0.35f),
		MUSICBAR_FILL   = new Color(255, 255, 255, 0.75f);

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private final int state;

	public Game(int state) {
		this.state = state;
		inputOverlayKeys = new InputOverlayKey[] {
			new InputOverlayKey("K1", ReplayFrame.KEY_K1, 0, new Color(248, 216, 0)),
			new InputOverlayKey("K2", ReplayFrame.KEY_K2, 0, new Color(248, 216, 0)),
			new InputOverlayKey("M1", ReplayFrame.KEY_M1, 4, new Color(248, 0, 158)),
			new InputOverlayKey("M2", ReplayFrame.KEY_M2, 8, new Color(248, 0, 158)),
		};
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		input = container.getInput();

		int width = container.getWidth();
		int height = container.getHeight();

		// create offscreen graphics
		offscreen = new Image(width, height);
		gOffscreen = offscreen.getGraphics();
		gOffscreen.setBackground(Color.black);

		// initialize music position bar location
		musicBarX = width * 0.01f;
		musicBarY = height * 0.05f;
		musicBarWidth = Math.max(width * 0.005f, 7);
		musicBarHeight = height * 0.9f;

		// initialize scoreboard star stream
		scoreboardStarStream = new StarStream(0, height * 2f / 3f, width / 4, 0, 0);
		scoreboardStarStream.setPositionSpread(height / 20f);
		scoreboardStarStream.setDirectionSpread(10f);
		scoreboardStarStream.setDurationSpread(700, 100);

		// create the associated GameData object
		data = new GameData(width, height);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();
		int trackPosition = MusicController.getPosition(true);
		if (pauseTime > -1)  // returning from pause screen
			trackPosition = pauseTime;
		else if (deathTime > -1)  // "Easy" mod: health bar increasing
			trackPosition = deathTime;
		int firstObjectTime = beatmap.objects[0].getTime();
		int timeDiff = firstObjectTime - trackPosition;

		g.setBackground(Color.black);

		// "flashlight" mod: initialize offscreen graphics
		if (GameMod.FLASHLIGHT.isActive()) {
			gOffscreen.clear();
			Graphics.setCurrent(gOffscreen);
		}

		// background
		float dimLevel = Options.getBackgroundDim();
		if (video != null && video.isStarted() && !video.isFinished()) {
			// video
			video.render(0, 0, width, height, dimLevel);
		} else {
			// image
			if (trackPosition < firstObjectTime) {
				if (timeDiff < approachTime)
					dimLevel += (1f - dimLevel) * ((float) timeDiff / approachTime);
				else
					dimLevel = 1f;
			}
			if (Options.isDefaultPlayfieldForced() || !beatmap.drawBackground(width, height, 0, 0, dimLevel, false)) {
				Image bg = GameImage.MENU_BG.getImage();
				bg.setAlpha(dimLevel);
				bg.drawCentered(width / 2, height / 2);
				bg.setAlpha(1f);
			}
		}

		if (GameMod.FLASHLIGHT.isActive())
			Graphics.setCurrent(g);

		// "auto" and "autopilot" mods: move cursor automatically
		// TODO: this should really be in update(), not render()
		autoMousePosition.set(width / 2, height / 2);
		autoMousePressed = false;
		if (GameMod.AUTO.isActive() || GameMod.AUTOPILOT.isActive()) {
			Vec2f autoPoint = null;
			if (gameFinished) {
				// game finished, do nothing
			} else if (isLeadIn()) {
				// lead-in
				float progress = Math.max((float) (leadInTime - beatmap.audioLeadIn) / approachTime, 0f);
				autoMousePosition.y = height / (2f - progress);
			} else if (objectIndex == 0 && trackPosition < firstObjectTime) {
				// before first object
				timeDiff = firstObjectTime - trackPosition;
				if (timeDiff < approachTime) {
					Vec2f point = gameObjects[0].getPointAt(trackPosition);
					autoPoint = getPointAt(autoMousePosition.x, autoMousePosition.y, point.x, point.y, 1f - ((float) timeDiff / approachTime));
				}
			} else if (objectIndex < beatmap.objects.length) {
				// normal object
				int objectTime = beatmap.objects[objectIndex].getTime();
				if (trackPosition < objectTime) {
					Vec2f startPoint = gameObjects[objectIndex - 1].getPointAt(trackPosition);
					int startTime = gameObjects[objectIndex - 1].getEndTime();
					if (beatmap.breaks != null && breakIndex < beatmap.breaks.size()) {
						// starting a break: keep cursor at previous hit object position
						if (breakTime > 0 || objectTime > beatmap.breaks.get(breakIndex))
							autoPoint = startPoint;

						// after a break ends: move startTime to break end time
						else if (breakIndex > 1) {
							int lastBreakEndTime = beatmap.breaks.get(breakIndex - 1);
							if (objectTime > lastBreakEndTime && startTime < lastBreakEndTime)
								startTime = lastBreakEndTime;
						}
					}
					if (autoPoint == null) {
						Vec2f endPoint = gameObjects[objectIndex].getPointAt(trackPosition);
						int totalTime = objectTime - startTime;

						if (totalTime == 0)
							// this can happen on 2B maps, see issue 401
							autoPoint = endPoint;
						else
							autoPoint = getPointAt(startPoint.x, startPoint.y, endPoint.x, endPoint.y, (float) (trackPosition - startTime) / totalTime);

						// hit circles: show a mouse press
						int offset300 = hitResultOffset[GameData.HIT_300];
						if ((beatmap.objects[objectIndex].isCircle() && objectTime - trackPosition < offset300) ||
						    (beatmap.objects[objectIndex - 1].isCircle() && trackPosition - beatmap.objects[objectIndex - 1].getTime() < offset300))
							autoMousePressed = true;
					}
				} else {
					autoPoint = gameObjects[objectIndex].getPointAt(trackPosition);
					autoMousePressed = true;
				}
			} else {
				// last object
				autoPoint = gameObjects[objectIndex - 1].getPointAt(trackPosition);
			}

			// set mouse coordinates
			if (autoPoint != null)
				autoMousePosition.set(autoPoint.x, autoPoint.y);
		}

		// "flashlight" mod: restricted view of hit objects around cursor
		if (GameMod.FLASHLIGHT.isActive()) {
			// render hit objects offscreen
			Graphics.setCurrent(gOffscreen);
			int trackPos = (isLeadIn()) ? (leadInTime - Options.getMusicOffset() - beatmap.localMusicOffset) * -1 : trackPosition;
			drawHitObjects(gOffscreen, trackPos);

			// restore original graphics context
			gOffscreen.flush();
			Graphics.setCurrent(g);

			// draw alpha map around cursor
			g.setDrawMode(Graphics.MODE_ALPHA_MAP);
			g.clearAlphaMap();
			int mouseX, mouseY;
			if (pauseTime > -1 && pausedMousePosition != null) {
				mouseX = (int) pausedMousePosition.x;
				mouseY = (int) pausedMousePosition.y;
			} else if (GameMod.AUTO.isActive() || GameMod.AUTOPILOT.isActive()) {
				mouseX = (int) autoMousePosition.x;
				mouseY = (int) autoMousePosition.y;
			} else if (isReplay) {
				mouseX = replayX;
				mouseY = replayY;
			} else {
				mouseX = input.getMouseX();
				mouseY = input.getMouseY();
			}
			int alphaRadius = flashlightRadius * 256 / 215;
			int alphaX = mouseX - alphaRadius / 2;
			int alphaY = mouseY - alphaRadius / 2;
			GameImage.ALPHA_MAP.getImage().draw(alphaX, alphaY, alphaRadius, alphaRadius);

			// blend offscreen image
			g.setDrawMode(Graphics.MODE_ALPHA_BLEND);
			g.setClip(alphaX, alphaY, alphaRadius, alphaRadius);
			g.drawImage(offscreen, 0, 0);
			g.clearClip();
			g.setDrawMode(Graphics.MODE_NORMAL);
		}

		// break periods
		if (beatmap.breaks != null && breakIndex < beatmap.breaks.size() && breakTime > 0) {
			int endTime = beatmap.breaks.get(breakIndex);
			int breakLength = endTime - breakTime;

			// letterbox effect (black bars on top/bottom)
			if (beatmap.letterboxInBreaks && breakLength >= 4000) {
				// let it fade in/out
				float a = Colors.BLACK_ALPHA.a;
				if (trackPosition - breakTime > breakLength / 2) {
					Colors.BLACK_ALPHA.a = (Math.min(500f, breakTime + breakLength - trackPosition)) / 500f;
				} else {
					Colors.BLACK_ALPHA.a = Math.min(500, trackPosition - breakTime) / 500f;
				}
				g.setColor(Colors.BLACK_ALPHA);
				g.fillRect(0, 0, width, height * 0.125f);
				g.fillRect(0, height * 0.875f, width, height * 0.125f);
				Colors.BLACK_ALPHA.a = a;
			}

			data.drawGameElements(g, true, objectIndex == 0, 1f);

			if (breakLength >= 8000 &&
				trackPosition - breakTime > 2000 &&
				trackPosition - breakTime < 5000) {
				// show break start
				if (data.getHealthPercent() >= 50) {
					GameImage.SECTION_PASS.getImage().drawCentered(width / 2f, height / 2f);
					if (!breakSound) {
						playSoundEffect(SoundEffect.SECTIONPASS);
						breakSound = true;
					}
				} else {
					GameImage.SECTION_FAIL.getImage().drawCentered(width / 2f, height / 2f);
					if (!breakSound) {
						playSoundEffect(SoundEffect.SECTIONFAIL);
						breakSound = true;
					}
				}
			} else if (breakLength >= 4000) {
				// show break end (flash twice for 500ms)
				int endTimeDiff = endTime - trackPosition;
				if ((endTimeDiff > 1500 && endTimeDiff < 2000) ||
					(endTimeDiff > 500 && endTimeDiff < 1000)) {
					Image arrow = GameImage.WARNINGARROW.getImage();
					Color color = (Options.getSkin().getVersion() == 1) ? Color.white : Color.red;
					arrow.setRotation(0);
					arrow.draw(width * 0.15f, height * 0.15f, color);
					arrow.draw(width * 0.15f, height * 0.75f, color);
					arrow.setRotation(180);
					arrow.draw(width * 0.75f, height * 0.15f, color);
					arrow.draw(width * 0.75f, height * 0.75f, color);
				}
			}
		}

		// non-break
		else {
			// game elements
			float gameElementAlpha = 1f;
			if (gameFinished) {
				// game finished: fade everything out
				float t = 1f - Math.min(gameFinishedTimer.getTime() / (float) FINISHED_FADEOUT_TIME, 1f);
				gameElementAlpha = AnimationEquation.OUT_CUBIC.calc(t);
			}
			data.drawGameElements(g, false, objectIndex == 0, gameElementAlpha);

			// skip beginning
			if (objectIndex == 0 &&
			    trackPosition < beatmap.objects[0].getTime() - SKIP_OFFSET)
				skipButton.draw();

			// show retries
			if (objectIndex == 0 && retries >= 2 && timeDiff >= -1000) {
				int retryHeight = Math.max(
						GameImage.SCOREBAR_BG.getImage().getHeight(),
						GameImage.SCOREBAR_KI.getImage().getHeight()
				);
				float oldAlpha = Colors.WHITE_FADE.a;
				if (timeDiff < -500)
					Colors.WHITE_FADE.a = (1000 + timeDiff) / 500f;
				Fonts.MEDIUM.drawString(
						2 + (width / 100), retryHeight,
						String.format("%d retries and counting...", retries),
						Colors.WHITE_FADE
				);
				Colors.WHITE_FADE.a = oldAlpha;
			}

			if (isLeadIn())  // render approach circles during song lead-in
				trackPosition = (leadInTime - Options.getMusicOffset() - beatmap.localMusicOffset) * -1;

			// countdown
			if (beatmap.countdown > 0) {
				float speedModifier = GameMod.getSpeedMultiplier() * playbackSpeed.getModifier();
				timeDiff = firstObjectTime - trackPosition;
				if (timeDiff >= 500 * speedModifier && timeDiff < 3000 * speedModifier) {
					if (timeDiff >= 1500 * speedModifier) {
						GameImage.COUNTDOWN_READY.getImage().drawCentered(width / 2, height / 2);
						if (!countdownReadySound) {
							playSoundEffect(SoundEffect.READY);
							countdownReadySound = true;
						}
					}
					if (timeDiff < 2000 * speedModifier) {
						GameImage.COUNTDOWN_3.getImage().draw(0, 0);
						if (!countdown3Sound) {
							playSoundEffect(SoundEffect.COUNT3);
							countdown3Sound = true;
						}
					}
					if (timeDiff < 1500 * speedModifier) {
						GameImage.COUNTDOWN_2.getImage().draw(width - GameImage.COUNTDOWN_2.getImage().getWidth(), 0);
						if (!countdown2Sound) {
							playSoundEffect(SoundEffect.COUNT2);
							countdown2Sound = true;
						}
					}
					if (timeDiff < 1000 * speedModifier) {
						GameImage.COUNTDOWN_1.getImage().drawCentered(width / 2, height / 2);
						if (!countdown1Sound) {
							playSoundEffect(SoundEffect.COUNT1);
							countdown1Sound = true;
						}
					}
				} else if (timeDiff >= -500 * speedModifier && timeDiff < 500 * speedModifier) {
					Image go = GameImage.COUNTDOWN_GO.getImage();
					go.setAlpha((timeDiff < 0) ? 1 - (timeDiff / speedModifier / -500f) : 1);
					go.drawCentered(width / 2, height / 2);
					if (!countdownGoSound) {
						playSoundEffect(SoundEffect.GO);
						countdownGoSound = true;
					}
				}
			}

			// draw hit objects
			if (!GameMod.FLASHLIGHT.isActive())
				drawHitObjects(g, trackPosition);
		}

		// in-game scoreboard
		if (previousScores != null && trackPosition >= firstObjectTime && !GameMod.RELAX.isActive() && !GameMod.AUTOPILOT.isActive()) {
			// NOTE: osu! uses the actual score, but we use sliding score instead
			ScoreData currentScore = data.getCurrentScoreData(beatmap, true);
			while (currentRank > 0 && previousScores[currentRank - 1].score < currentScore.score) {
				currentRank--;
				scoreboardStarStream.burst(20);
				lastRankUpdateTime = trackPosition;
			}

			float animation = AnimationEquation.IN_OUT_QUAD.calc(
				Utils.clamp((trackPosition - lastRankUpdateTime) / SCOREBOARD_ANIMATION_TIME, 0f, 1f)
			);
			int scoreboardPosition = 2 * container.getHeight() / 3;

			// draw star stream behind the scores
			scoreboardStarStream.draw();

			if (currentRank < 4) {
				// draw the (new) top 5 ranks
				for (int i = 0; i < 4; i++) {
					int index = i + (i >= currentRank ? 1 : 0);
					if (i < previousScores.length) {
						float position = index + (i == currentRank ? animation - 3f : -2f);
						previousScores[i].drawSmall(g, scoreboardPosition, index + 1, position, data, currentScoreboardAlpha, false);
					}
				}
				currentScore.drawSmall(g, scoreboardPosition, currentRank + 1, currentRank - 1f - animation, data, currentScoreboardAlpha, true);
			} else {
				// draw the top 2 and next 2 ranks
				previousScores[0].drawSmall(g, scoreboardPosition, 1, -2f, data, currentScoreboardAlpha, false);
				previousScores[1].drawSmall(g, scoreboardPosition, 2, -1f, data, currentScoreboardAlpha, false);
				previousScores[currentRank - 2].drawSmall(
					g, scoreboardPosition, currentRank - 1, animation - 1f, data, currentScoreboardAlpha * animation, false
				);
				previousScores[currentRank - 1].drawSmall(g, scoreboardPosition, currentRank, animation, data, currentScoreboardAlpha, false);
				currentScore.drawSmall(g, scoreboardPosition, currentRank + 1, 2f, data, currentScoreboardAlpha, true);
				if (animation < 1.0f && currentRank < previousScores.length) {
					previousScores[currentRank].drawSmall(
						g, scoreboardPosition, currentRank + 2, 1f + 5 * animation, data, currentScoreboardAlpha * (1f - animation), false
					);
				}
			}
		}

		if (GameMod.AUTO.isActive())
			GameImage.UNRANKED.getImage().drawCentered(width / 2, height * 0.077f);

		// draw replay speed button
		if (isReplay || GameMod.AUTO.isActive())
			playbackSpeed.getButton().draw();

		// draw music position bar (for replay seeking)
		if (isReplay && Options.isReplaySeekingEnabled()) {
			int mouseX = input.getMouseX(), mouseY = input.getMouseY();
			g.setColor((musicPositionBarContains(mouseX, mouseY)) ? MUSICBAR_HOVER : MUSICBAR_NORMAL);
			g.fillRoundRect(musicBarX, musicBarY, musicBarWidth, musicBarHeight, 4);
			if (!isLeadIn()) {
				g.setColor(MUSICBAR_FILL);
				float musicBarPosition = Math.min((float) trackPosition / beatmap.endTime, 1f);
				g.fillRoundRect(musicBarX, musicBarY, musicBarWidth, musicBarHeight * musicBarPosition, 4);
			}
		}

		// key overlay
		if (isReplay || Options.alwaysShowKeyOverlay()) {
			final float BTNSIZE = container.getHeight() * 0.0615f;
			int x = (int) (container.getWidth() - BTNSIZE / 2f);
			int y = (int) (container.getHeight() / 2f - BTNSIZE - BTNSIZE / 2f);
			Image bg = GameImage.INPUTOVERLAY_BACKGROUND.getImage();
			bg = bg.getScaledCopy(BTNSIZE * 4.3f / bg.getWidth());
			bg.rotate(90f);
			bg.drawCentered(container.getWidth() - bg.getHeight() / 2, container.getHeight() / 2);
			Image keyimg =
				GameImage.INPUTOVERLAY_KEY.getImage().getScaledCopy((int) BTNSIZE, (int) BTNSIZE);
			for (int i = 0; i < 4; i++) {
				inputOverlayKeys[i].render(g, x, y, keyimg);
				y += BTNSIZE;
			}
		}

		// returning from pause screen
		if (pauseTime > -1 && pausedMousePosition != null) {
			// darken the screen
			g.setColor(Colors.BLACK_ALPHA);
			g.fillRect(0, 0, width, height);

			// draw overlay text
			String overlayText = "Click on the pulsing cursor to continue play!";
			int textWidth = Fonts.LARGE.getWidth(overlayText), textHeight = Fonts.LARGE.getLineHeight();
			int textX = (width - textWidth) / 2, textY = (height - textHeight) / 2;
			int paddingX = 8, paddingY = 4;
			g.setLineWidth(1f);
			g.setColor(Color.black);
			g.fillRect(textX - paddingX, textY - paddingY, textWidth + paddingX * 2, textHeight + paddingY * 2);
			g.setColor(Colors.LIGHT_BLUE);
			g.drawRect(textX - paddingX, textY - paddingY, textWidth + paddingX * 2, textHeight + paddingY * 2);
			g.setColor(Color.white);
			Fonts.LARGE.drawString(textX, textY, overlayText);

			// draw glowing hit select circle and pulse effect
			int circleDiameter = GameImage.HITCIRCLE.getImage().getWidth();
			Image cursorCircle = GameImage.HITCIRCLE_SELECT.getImage().getScaledCopy(circleDiameter, circleDiameter);
			cursorCircle.setAlpha(1.0f);
			cursorCircle.drawCentered(pausedMousePosition.x, pausedMousePosition.y);
			Image cursorCirclePulse = cursorCircle.getScaledCopy(1f + pausePulse);
			cursorCirclePulse.setAlpha(1f - pausePulse);
			cursorCirclePulse.drawCentered(pausedMousePosition.x, pausedMousePosition.y);
		}

		if (isReplay)
			UI.draw(g, replayX, replayY, replayKeyPressed);
		else if (GameMod.AUTO.isActive())
			UI.draw(g, (int) autoMousePosition.x, (int) autoMousePosition.y, autoMousePressed);
		else if (GameMod.AUTOPILOT.isActive())
			UI.draw(g, (int) autoMousePosition.x, (int) autoMousePosition.y, Utils.isGameKeyPressed());
		else
			UI.draw(g);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		UI.update(delta);
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		skipButton.hoverUpdate(delta, mouseX, mouseY);
		if (isReplay || GameMod.AUTO.isActive())
			playbackSpeed.getButton().hoverUpdate(delta, mouseX, mouseY);
		int trackPosition = MusicController.getPosition(true);
		int firstObjectTime = beatmap.objects[0].getTime();
		scoreboardStarStream.update(delta);

		// returning from pause screen: must click previous mouse position
		if (pauseTime > -1) {
			// paused during lead-in or break, or "relax" or "autopilot": continue immediately
			if (pausedMousePosition == null || (GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive())) {
				pauseTime = -1;
				if (!isLeadIn())
					MusicController.resume();
				if (video != null)
					video.resume();
			}

			// focus lost: go back to pause screen
			else if (!container.hasFocus()) {
				game.enterState(Opsu.STATE_GAMEPAUSEMENU);
				pausePulse = 0f;
			}

			// advance pulse animation
			else {
				pausePulse += delta / 750f;
				if (pausePulse > 1f)
					pausePulse = 0f;

				// is mouse within the circle?
				double distance = Math.hypot(pausedMousePosition.x - mouseX, pausedMousePosition.y - mouseY);
				int circleRadius = GameImage.HITCIRCLE.getImage().getWidth() / 2;
				if (distance < circleRadius)
					UI.updateTooltip(delta, "Click to resume gameplay.", false);
			}
			return;
		}

		// replays: skip intro
		if (isReplay && replaySkipTime > -1 && trackPosition >= replaySkipTime) {
			if (skipIntro())
				trackPosition = MusicController.getPosition(true);
		}

		// "flashlight" mod: calculate visible area radius
		updateFlashlightRadius(delta, trackPosition);

		// stop updating during song lead-in
		if (isLeadIn()) {
			leadInTime -= delta;
			if (!isLeadIn())
				MusicController.resume();
			return;
		}

		// "Easy" mod: multiple "lives"
		if (GameMod.EASY.isActive() && deathTime > -1) {
			if (data.getHealthPercent() < 99f) {
				data.changeHealth(delta / 5f);
				data.updateDisplays(delta);
				return;
			}
			MusicController.resume();
			if (video != null)
				video.resume();
			deathTime = -1;
		}

		// update video
		if (video != null && !video.isFinished()) {
			if (trackPosition >= videoStartTime)
				video.update(trackPosition - videoStartTime - videoSeekTime);
		}

		// normal game update
		if (!isReplay && !gameFinished) {
			addReplayFrameAndRun(mouseX, mouseY, lastKeysPressed, trackPosition);
			addLifeFrame(trackPosition);
		}

		// watching replay
		else if (!gameFinished) {
			// out of frames, use previous data
			if (replayIndex >= replay.frames.length)
				updateGame(replayX, replayY, delta, MusicController.getPosition(true), lastKeysPressed);

			boolean hasVideo = (video != null);

			// seeking to a position earlier than original track position
			if (isSeeking && replayIndex - 1 >= 1 && replayIndex < replay.frames.length &&
			    trackPosition < replay.frames[replayIndex - 1].getTime()) {
				replayIndex = 0;
				while (objectIndex >= 0) {
					gameObjects[objectIndex].reset();
					objectIndex--;
				}

				// reset game data
				FakeCombinedCurve oldMergedSlider = mergedSlider;
				resetGameData();
				mergedSlider = oldMergedSlider;

				// load the first timingPoint
				if (!beatmap.timingPoints.isEmpty()) {
					TimingPoint timingPoint = beatmap.timingPoints.get(0);
					if (!timingPoint.isInherited()) {
						setBeatLength(timingPoint, true);
						timingPointIndex++;
					}
				}
			}

			// update and run replay frames
			while (replayIndex < replay.frames.length && trackPosition >= replay.frames[replayIndex].getTime()) {
				ReplayFrame frame = replay.frames[replayIndex];
				replayX = frame.getScaledX();
				replayY = frame.getScaledY();
				replayKeyPressed = frame.isKeyPressed();
				lastKeysPressed = frame.getKeys();
				runReplayFrame(frame);
				replayIndex++;
			}
			mouseX = replayX;
			mouseY = replayY;

			// unmute sounds
			if (isSeeking) {
				isSeeking = false;
				SoundController.mute(false);
				if (hasVideo)
					loadVideo(trackPosition);
			}
		}

		// update key overlay
		if (isReplay || Options.alwaysShowKeyOverlay()) {
			for (int i = 0; i < 4; i++) {
				int keys = autoMousePressed ? 1 : lastKeysPressed;
				boolean countpresses = breakTime == 0 && !isLeadIn() &&
					trackPosition > firstObjectTime;
				inputOverlayKeys[i].update(keys, countpresses, delta);
			}
		}

		lastTrackPosition = trackPosition;

		// update in-game scoreboard
		if (previousScores != null && trackPosition > firstObjectTime) {
			// show scoreboard if selected, and always in break
			// hide when game ends
			if ((scoreboardVisible || breakTime > 0) && !gameFinished) {
				currentScoreboardAlpha += 1f / SCOREBOARD_FADE_IN_TIME * delta;
				if (currentScoreboardAlpha > 1f)
					currentScoreboardAlpha = 1f;
			} else {
				currentScoreboardAlpha -= 1f / SCOREBOARD_FADE_IN_TIME * delta;
				if (currentScoreboardAlpha < 0f)
					currentScoreboardAlpha = 0f;
			}
		}

		data.updateDisplays(delta);

		// game finished: change state after timer expires
		if (gameFinished && !gameFinishedTimer.update(delta)) {
			if (checkpointLoaded)  // if checkpoint used, skip ranking screen
				game.closeRequested();
			else {  // go to ranking screen
				MusicController.setPitch(1f);
				game.enterState(Opsu.STATE_GAMERANKING, new EasedFadeOutTransition(), new FadeInTransition());
			}
		}
	}

	/**
	 * Updates the game.
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 * @param delta the delta interval
	 * @param trackPosition the track position
	 * @param keys the keys that are pressed
	 */
	private void updateGame(int mouseX, int mouseY, int delta, int trackPosition, int keys) {
		// map complete!
		if (!hasMoreObjects() || (MusicController.trackEnded() && objectIndex > 0)) {
			// track ended before last object(s) was processed: force a hit result
			if (MusicController.trackEnded() && hasMoreObjects()) {
				for (int index : passedObjects)
					gameObjects[index].update(delta, mouseX, mouseY, false, trackPosition);
				passedObjects.clear();
				for (int i = objectIndex; i < gameObjects.length; i++)
					gameObjects[i].update(delta, mouseX, mouseY, false, trackPosition);
				objectIndex = gameObjects.length;
			}

			// save score and replay
			if (!checkpointLoaded) {
				boolean unranked = (GameMod.AUTO.isActive() || GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive());
				((GameRanking) game.getState(Opsu.STATE_GAMERANKING)).setGameData(data);
				if (isReplay)
					data.setReplay(replay);
				else if (replayFrames != null) {
					// finalize replay frames with start/skip frames
					if (!replayFrames.isEmpty())
						replayFrames.getFirst().setTimeDiff(replaySkipTime * -1);
					replayFrames.addFirst(ReplayFrame.getStartFrame(replaySkipTime));
					replayFrames.addFirst(ReplayFrame.getStartFrame(0));
					Replay r = data.getReplay(
						replayFrames.toArray(new ReplayFrame[replayFrames.size()]),
						lifeFrames.toArray(new LifeFrame[lifeFrames.size()]),
						beatmap
					);
					if (r != null && !unranked)
						r.save();
				}
				ScoreData score = data.getScoreData(beatmap);
				data.setGameplay(!isReplay);

				// add score to database and user stats
				if (!unranked && !isReplay) {
					ScoreDB.addScore(score);
					User user = UserList.get().getCurrentUser();
					user.add(data.getScore(), data.getScorePercent());
					ScoreDB.updateUser(user);
				}
			}

			// start timer
			gameFinished = true;
			gameFinishedTimer.setTime(0);

			return;
		}

		// timing points
		if (timingPointIndex < beatmap.timingPoints.size()) {
			TimingPoint timingPoint = beatmap.timingPoints.get(timingPointIndex);
			if (trackPosition >= timingPoint.getTime()) {
				setBeatLength(timingPoint, true);
				timingPointIndex++;
			}
		}

		// song beginning
		if (objectIndex == 0 && trackPosition < beatmap.objects[0].getTime())
			return;  // nothing to do here

		// break periods
		if (beatmap.breaks != null && breakIndex < beatmap.breaks.size()) {
			int breakValue = beatmap.breaks.get(breakIndex);
			if (breakTime > 0) {  // in a break period
				if (trackPosition < breakValue &&
				    trackPosition < beatmap.objects[objectIndex].getTime() - approachTime)
					return;
				else {
					// break is over
					breakTime = 0;
					breakIndex++;
				}
			} else if (trackPosition >= breakValue) {
				// start a break
				breakTime = breakValue;
				breakSound = false;
				breakIndex++;
				return;
			}
		}

		// pause game if focus lost
		if (!container.hasFocus() && !GameMod.AUTO.isActive() && !isReplay) {
			if (pauseTime < 0) {
				pausedMousePosition = new Vec2f(mouseX, mouseY);
				pausePulse = 0f;
			}
			if (MusicController.isPlaying() || isLeadIn())
				pauseTime = trackPosition;
			if (video != null)
				video.pause();
			game.enterState(Opsu.STATE_GAMEPAUSEMENU, new EmptyTransition(), new FadeInTransition());
		}

		// drain health
		if (lastTrackPosition > 0)
			data.changeHealth((trackPosition - lastTrackPosition) * -1 * hpDropRate);

		// health ran out?
		if (!data.isAlive()) {
			// "Easy" mod
			if (GameMod.EASY.isActive() && !GameMod.SUDDEN_DEATH.isActive()) {
				deaths++;
				if (deaths < 3) {
					deathTime = trackPosition;
					MusicController.pause();
					if (video != null)
						video.pause();
					return;
				}
			}

			// game over, force a restart
			if (!isReplay) {
				if (playState != PlayState.LOSE) {
					playState = PlayState.LOSE;
					failTime = System.currentTimeMillis();
					failTrackTime = MusicController.getPosition(true);
					MusicController.fadeOut(MUSIC_FADEOUT_TIME);
					MusicController.pitchFadeOut(MUSIC_FADEOUT_TIME);
					rotations = new IdentityHashMap<GameObject, Float>();
					SoundController.playSound(SoundEffect.FAIL);

					// record to stats
					User user = UserList.get().getCurrentUser();
					user.add(data.getScore());
					ScoreDB.updateUser(user);

					// fade to pause menu
					game.enterState(Opsu.STATE_GAMEPAUSEMENU,
							new DelayedFadeOutTransition(Color.black, MUSIC_FADEOUT_TIME, MUSIC_FADEOUT_TIME - LOSE_FADEOUT_TIME),
							new FadeInTransition());
					return;
				}
			}
		}

		// don't process hit results when already lost
		if (playState != PlayState.LOSE) {
			boolean keyPressed = keys != ReplayFrame.KEY_NONE;

			// update passed objects
			Iterator<Integer> iter = passedObjects.iterator();
			while (iter.hasNext()) {
				int index = iter.next();
				if (gameObjects[index].update(delta, mouseX, mouseY, keyPressed, trackPosition))
					iter.remove();
			}

			// update objects (loop over any skipped indexes)
			while (objectIndex < gameObjects.length && trackPosition > beatmap.objects[objectIndex].getTime()) {
				// check if we've already passed the next object's start time
				boolean overlap =
					(objectIndex + 1 < gameObjects.length &&
					trackPosition > beatmap.objects[objectIndex + 1].getTime() - hitResultOffset[GameData.HIT_50]);

				// update hit object and check completion status
				if (gameObjects[objectIndex].update(delta, mouseX, mouseY, keyPressed, trackPosition)) {
					// done, so increment object index
					objectIndex++;
				} else if (overlap) {
					// overlap, so save the current object and increment object index
					passedObjects.add(objectIndex);
					objectIndex++;
				} else
					break;
			}
		}
	}

	@Override
	public int getID() { return state; }

	@Override
	public void keyPressed(int key, char c) {
		int trackPosition = MusicController.getPosition(true);
		int mouseX = input.getMouseX();
		int mouseY = input.getMouseY();

		// game keys
		if (!Keyboard.isRepeatEvent() && !gameFinished) {
			int keys = ReplayFrame.KEY_NONE;
			if (key == Options.getGameKeyLeft())
				keys = ReplayFrame.KEY_K1;
			else if (key == Options.getGameKeyRight())
				keys = ReplayFrame.KEY_K2;
			if (keys != ReplayFrame.KEY_NONE)
				gameKeyPressed(keys, mouseX, mouseY, trackPosition);
		}

		if (UI.globalKeyPressed(key))
			return;

		switch (key) {
		case Input.KEY_ESCAPE:
			// game finished: only advance the timer
			if (gameFinished) {
				gameFinishedTimer.setTime(gameFinishedTimer.getDuration());
				break;
			}

			// "auto" mod or watching replay: go back to song menu
			if (GameMod.AUTO.isActive() || isReplay) {
				game.closeRequested();
				break;
			}

			// pause game
			if (pauseTime < 0 && breakTime <= 0 && trackPosition >= beatmap.objects[0].getTime()) {
				pausedMousePosition = new Vec2f(mouseX, mouseY);
				pausePulse = 0f;
			}
			if (MusicController.isPlaying() || isLeadIn())
				pauseTime = trackPosition;
			if (video != null)
				video.pause();
			game.enterState(Opsu.STATE_GAMEPAUSEMENU, new EmptyTransition(), new FadeInTransition());
			break;
		case Input.KEY_SPACE:
			// skip intro
			if (!gameFinished)
				skipIntro();
			break;
		case Input.KEY_R:
			// restart
			if (!input.isKeyDown(Input.KEY_RCONTROL) && !input.isKeyDown(Input.KEY_LCONTROL))
				break;
			// fall through
		case Input.KEY_GRAVE:
			// restart
			if (gameFinished)
				break;
			try {
				if (trackPosition < beatmap.objects[0].getTime())
					retries--;  // don't count this retry (cancel out later increment)
				playState = PlayState.RETRY;
				enter(container, game);
				skipIntro();
			} catch (SlickException e) {
				ErrorHandler.error("Failed to restart game.", e, false);
			}
			break;
		case Input.KEY_S:
			// save checkpoint
			if (gameFinished)
				break;
			if (input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) {
				if (isLeadIn())
					break;

				int position = (pauseTime > -1) ? pauseTime : trackPosition;
				if (Options.setCheckpoint(position / 1000)) {
					playSoundEffect(SoundEffect.MENUCLICK);
					UI.getNotificationManager().sendBarNotification("Checkpoint saved.");
				}
			}
			break;
		case Input.KEY_L:
			// load checkpoint
			if (gameFinished)
				break;
			if (input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) {
				int checkpoint = Options.getCheckpoint();
				if (checkpoint == 0 || checkpoint > beatmap.endTime)
					break;  // invalid checkpoint
				try {
					playState = PlayState.RETRY;
					enter(container, game);
					checkpointLoaded = true;
					if (isLeadIn()) {
						leadInTime = 0;
						MusicController.resume();
					}
					playSoundEffect(SoundEffect.MENUHIT);
					UI.getNotificationManager().sendBarNotification("Checkpoint loaded.");

					// skip to checkpoint
					MusicController.setPosition(checkpoint);
					MusicController.setPitch(getCurrentPitch());
					if (video != null)
						loadVideo(checkpoint);
					while (objectIndex < gameObjects.length &&
							beatmap.objects[objectIndex++].getTime() <= checkpoint)
						;
					objectIndex--;
					lastReplayTime = beatmap.objects[objectIndex].getTime();
					lastTrackPosition = checkpoint;
				} catch (SlickException e) {
					ErrorHandler.error("Failed to load checkpoint.", e, false);
				}
			}
			break;
		case Input.KEY_F:
			// change playback speed
			if (gameFinished)
				break;
			if (isReplay || GameMod.AUTO.isActive()) {
				playbackSpeed = playbackSpeed.next();
				MusicController.setPitch(getCurrentPitch());
			}
			break;
		case Input.KEY_UP:
			UI.changeVolume(1);
			break;
		case Input.KEY_DOWN:
			UI.changeVolume(-1);
			break;
		case Input.KEY_TAB:
			if (!gameFinished)
				scoreboardVisible = !scoreboardVisible;
			break;
		case Input.KEY_EQUALS:
		case Input.KEY_ADD:
			if (!Keyboard.isRepeatEvent() && !gameFinished)
				adjustLocalMusicOffset(1);
			break;
		case Input.KEY_MINUS:
		case Input.KEY_SUBTRACT:
			if (!Keyboard.isRepeatEvent() && !gameFinished)
				adjustLocalMusicOffset(-1);
			break;
		}
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		if (gameFinished)
			return;

		// watching replay
		if (isReplay || GameMod.AUTO.isActive()) {
			if (button == Input.MOUSE_MIDDLE_BUTTON)
				return;

			// skip button
			if (skipButton.contains(x, y))
				skipIntro();

			// playback speed button
			else if (playbackSpeed.getButton().contains(x, y)) {
				playbackSpeed = playbackSpeed.next();
				MusicController.setPitch(getCurrentPitch());
			}

			// replay seeking
			else if (Options.isReplaySeekingEnabled() && !GameMod.AUTO.isActive() && musicPositionBarContains(x, y)) {
				SoundController.mute(true);  // mute sounds while seeking
				float pos = (y - musicBarY) / musicBarHeight * beatmap.endTime;
				MusicController.setPosition((int) pos);
				lastTrackPosition = (int) pos;
				isSeeking = true;
			}
			return;
		}

		if (Options.isMouseDisabled())
			return;

		// mouse wheel: pause the game
		if (button == Input.MOUSE_MIDDLE_BUTTON && !Options.isMouseWheelDisabled()) {
			int trackPosition = MusicController.getPosition(true);
			if (pauseTime < 0 && breakTime <= 0 && trackPosition >= beatmap.objects[0].getTime()) {
				pausedMousePosition = new Vec2f(x, y);
				pausePulse = 0f;
			}
			if (MusicController.isPlaying() || isLeadIn())
				pauseTime = trackPosition;
			if (video != null)
				video.pause();
			game.enterState(Opsu.STATE_GAMEPAUSEMENU, new EmptyTransition(), new FadeInTransition());
			return;
		}

		// game keys
		int keys = ReplayFrame.KEY_NONE;
		if (button == Input.MOUSE_LEFT_BUTTON)
			keys = ReplayFrame.KEY_M1;
		else if (button == Input.MOUSE_RIGHT_BUTTON)
			keys = ReplayFrame.KEY_M2;
		if (keys != ReplayFrame.KEY_NONE)
			gameKeyPressed(keys, x, y, MusicController.getPosition(true));
	}

	/**
	 * Handles a game key pressed event.
	 * @param keys the game keys pressed
	 * @param x the mouse x coordinate
	 * @param y the mouse y coordinate
	 * @param trackPosition the track position
	 */
	private void gameKeyPressed(int keys, int x, int y, int trackPosition) {
		// returning from pause screen
		if (pauseTime > -1) {
			double distance = Math.hypot(pausedMousePosition.x - x, pausedMousePosition.y - y);
			int circleRadius = GameImage.HITCIRCLE.getImage().getWidth() / 2;
			if (distance < circleRadius) {
				// unpause the game
				pauseTime = -1;
				pausedMousePosition = null;
				if (!isLeadIn())
					MusicController.resume();
				if (video != null)
					video.resume();
			}
			return;
		}

		// skip beginning
		if (skipButton.contains(x, y)) {
			if (skipIntro())
				return;  // successfully skipped
		}

		// "auto" and "relax" mods: ignore user actions
		if (GameMod.AUTO.isActive() || GameMod.RELAX.isActive())
			return;

		// send a game key press
		if (!isReplay && !gameFinished && keys != ReplayFrame.KEY_NONE) {
			lastKeysPressed |= keys;  // set keys bits
			addReplayFrameAndRun(x, y, lastKeysPressed, trackPosition);
		}
	}

	@Override
	public void mouseReleased(int button, int x, int y) {
		if (gameFinished)
			return;

		if (Options.isMouseDisabled())
			return;

		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		int keys = ReplayFrame.KEY_NONE;
		if (button == Input.MOUSE_LEFT_BUTTON)
			keys = ReplayFrame.KEY_M1;
		else if (button == Input.MOUSE_RIGHT_BUTTON)
			keys = ReplayFrame.KEY_M2;
		if (keys != ReplayFrame.KEY_NONE)
			gameKeyReleased(keys, x, y, MusicController.getPosition(true));
	}

	@Override
	public void keyReleased(int key, char c) {
		if (gameFinished)
			return;

		int keys = ReplayFrame.KEY_NONE;
		if (key == Options.getGameKeyLeft())
			keys = ReplayFrame.KEY_K1;
		else if (key == Options.getGameKeyRight())
			keys = ReplayFrame.KEY_K2;
		if (keys != ReplayFrame.KEY_NONE)
			gameKeyReleased(keys, input.getMouseX(), input.getMouseY(), MusicController.getPosition(true));
	}

	/**
	 * Handles a game key released event.
	 * @param keys the game keys released
	 * @param x the mouse x coordinate
	 * @param y the mouse y coordinate
	 * @param trackPosition the track position
	 */
	private void gameKeyReleased(int keys, int x, int y, int trackPosition) {
		if (!isReplay && keys != ReplayFrame.KEY_NONE && !isLeadIn() && pauseTime == -1) {
			lastKeysPressed &= ~keys;  // clear keys bits
			addReplayFrameAndRun(x, y, lastKeysPressed, trackPosition);
		}
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		if (Options.isMouseWheelDisabled())
			return;

		UI.globalMouseWheelMoved(newValue, false);
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();

		if (beatmap == null || beatmap.objects == null)
			throw new RuntimeException("Running game with no beatmap loaded.");

		// free all previously cached hitobject to framebuffer mappings if some still exist
		FrameBufferCache.getInstance().freeMap();

		// grab the mouse (not working for touchscreen)
//		container.setMouseGrabbed(true);

		// restart the game
		if (playState != PlayState.NORMAL) {
			// reset key states
			lastKeysPressed = 0;
			for (int i = 0; i < 4; i++)
				inputOverlayKeys[i].reset();

			// update play stats
			if (playState == PlayState.FIRST_LOAD) {
				beatmap.incrementPlayCounter();
				BeatmapDB.updatePlayStatistics(beatmap);
			}

			// load mods
			if (isReplay) {
				previousMods = GameMod.getModState();
				GameMod.loadModState(replay.mods);
			}

			data.setGameplay(true);

			// check play state
			if (playState == PlayState.FIRST_LOAD) {
				loadImages();
				setMapModifiers();
				retries = 0;
			} else if (playState == PlayState.RETRY && !GameMod.AUTO.isActive()) {
				retries++;
			} else if (playState == PlayState.REPLAY || GameMod.AUTO.isActive()) {
				retries = 0;
			}

			gameObjects = new GameObject[beatmap.objects.length];
			playbackSpeed = PlaybackSpeed.NORMAL;

			// reset game data
			resetGameData();

			// load the first timingPoint for stacking
			if (!beatmap.timingPoints.isEmpty()) {
				TimingPoint timingPoint = beatmap.timingPoints.get(0);
				if (!timingPoint.isInherited()) {
					setBeatLength(timingPoint, true);
					timingPointIndex++;
				}
			}

			// initialize object maps
			boolean ignoreSkins = Options.isBeatmapSkinIgnored();
			Color[] combo = ignoreSkins ? Options.getSkin().getComboColors() : beatmap.getComboColors();
			int comboIndex = 0;
			for (int i = 0; i < beatmap.objects.length; i++) {
				HitObject hitObject = beatmap.objects[i];

				// is this the last note in the combo?
				boolean comboEnd = false;
				if (i + 1 >= beatmap.objects.length || beatmap.objects[i + 1].isNewCombo())
					comboEnd = true;

				// calculate color index if ignoring beatmap skin
				Color color;
				if (ignoreSkins) {
					if (hitObject.isNewCombo() || i == 0) {
						int skip = (hitObject.isSpinner() ? 0 : 1) + hitObject.getComboSkip();
						for (int j = 0; j < skip; j++)
							comboIndex = (comboIndex + 1) % combo.length;
					}
					color = combo[comboIndex];
				} else
					color = combo[hitObject.getComboIndex()];

				// pass beatLength to hit objects
				int hitObjectTime = hitObject.getTime();
				while (timingPointIndex < beatmap.timingPoints.size()) {
					TimingPoint timingPoint = beatmap.timingPoints.get(timingPointIndex);
					if (timingPoint.getTime() > hitObjectTime)
						break;
					setBeatLength(timingPoint, false);
					timingPointIndex++;
				}

				try {
					if (hitObject.isCircle())
						gameObjects[i] = new Circle(hitObject, this, data, color, comboEnd);
					else if (hitObject.isSlider())
						gameObjects[i] = new Slider(hitObject, this, data, color, comboEnd);
					else if (hitObject.isSpinner())
						gameObjects[i] = new Spinner(hitObject, this, data);
					else  // invalid hit object, use a dummy GameObject
						gameObjects[i] = new DummyObject(hitObject);
				} catch (Exception e) {
					// try to handle the error gracefully: substitute in a dummy GameObject
					ErrorHandler.error(String.format("Failed to create %s at index %d:\n%s",
							hitObject.getTypeName(), i, hitObject.toString()), e, true);
					gameObjects[i] = new DummyObject(hitObject);
					continue;
				}
			}

			// stack calculations
			calculateStacks();

			// load the first timingPoint
			timingPointIndex = 0;
			beatLengthBase = beatLength = 1;
			if (!beatmap.timingPoints.isEmpty()) {
				TimingPoint timingPoint = beatmap.timingPoints.get(0);
				if (!timingPoint.isInherited()) {
					setBeatLength(timingPoint, true);
					timingPointIndex++;
				}
			}

			// experimental merged slider
			if (Options.isExperimentalSliderMerging())
				createMergedSlider();

			// unhide cursor for "auto" mod and replays
			if (GameMod.AUTO.isActive() || isReplay)
				UI.getCursor().show();

			// load replay frames
			if (isReplay) {
				// load initial data
				replayX = container.getWidth() / 2;
				replayY = container.getHeight() / 2;
				replayKeyPressed = false;
				replaySkipTime = -1;
				for (replayIndex = 0; replayIndex < replay.frames.length; replayIndex++) {
					ReplayFrame frame = replay.frames[replayIndex];
					if (frame.getY() < 0) {  // skip time (?)
						if (frame.getTime() >= 0 && replayIndex > 0)
							replaySkipTime = frame.getTime();
					} else if (frame.getTime() == 0) {
						replayX = frame.getScaledX();
						replayY = frame.getScaledY();
						replayKeyPressed = frame.isKeyPressed();
					} else
						break;
				}
			}

			// initialize replay-recording structures
			else {
				lastKeysPressed = ReplayFrame.KEY_NONE;
				replaySkipTime = -1;
				replayFrames = new LinkedList<ReplayFrame>();
				replayFrames.add(new ReplayFrame(0, 0, input.getMouseX(), input.getMouseY(), 0));
				lifeFrames = new LinkedList<LifeFrame>();
			}

			leadInTime = beatmap.audioLeadIn + approachTime;
			playState = PlayState.NORMAL;

			// fetch previous scores
			previousScores = ScoreDB.getMapScoresExcluding(beatmap, replay == null ? null : replay.getReplayFilename());
			lastRankUpdateTime = -1000;
			if (previousScores != null)
				currentRank = previousScores.length;
			scoreboardVisible = previousScores.length > 0;
			currentScoreboardAlpha = 0f;

			// using local offset?
			if (beatmap.localMusicOffset != 0)
				UI.getNotificationManager().sendBarNotification(String.format("Using local beatmap offset (%dms)", beatmap.localMusicOffset));

			// using custom difficulty settings?
			if (Options.getFixedCS() > 0f || Options.getFixedAR() > 0f || Options.getFixedOD() > 0f ||
				Options.getFixedHP() > 0f || Options.getFixedSpeed() > 0f)
				UI.getNotificationManager().sendNotification("Playing with custom difficulty settings.");

			// load video
			if (beatmap.video != null) {
				loadVideo((beatmap.videoOffset < 0) ? -beatmap.videoOffset : 0);
				videoStartTime = Math.max(0, beatmap.videoOffset);
			}

			// needs to play before setting position to resume without lag later
			MusicController.playAt(0, false);
			MusicController.pause();

			SoundController.mute(false);
		}

		skipButton.resetHover();
		if (isReplay || GameMod.AUTO.isActive())
			playbackSpeed.getButton().resetHover();
		MusicController.setPitch(getCurrentPitch());
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
//		container.setMouseGrabbed(false);

		// re-hide cursor
		if (GameMod.AUTO.isActive() || isReplay)
			UI.getCursor().hide();

		// replays
		if (isReplay)
			GameMod.loadModState(previousMods);
	}

	/**
	 * Draws hit objects, hit results, and follow points.
	 * @param g the graphics context
	 * @param trackPosition the track position
	 */
	private void drawHitObjects(Graphics g, int trackPosition) {
		// draw result objects (under)
		data.drawHitResults(trackPosition, false);

		// include previous object in follow points
		int lastObjectIndex = -1;
		if (objectIndex > 0 && objectIndex < beatmap.objects.length &&
		    trackPosition < beatmap.objects[objectIndex].getTime() && !beatmap.objects[objectIndex - 1].isSpinner())
			lastObjectIndex = objectIndex - 1;

		boolean loseState = (playState == PlayState.LOSE);
		if (loseState)
			trackPosition = failTrackTime + (int) (System.currentTimeMillis() - failTime);

		// draw merged slider
		if (!loseState && mergedSlider != null && Options.isExperimentalSliderMerging()) {
			mergedSlider.draw(Color.white);
			mergedSlider.clearPoints();
		}

		// get hit objects in reverse order, or else overlapping objects are unreadable
		Stack<Integer> stack = new Stack<Integer>();
		int spinnerIndex = -1;  // draw spinner first (assume there can only be 1...)
		for (int index : passedObjects) {
			if (beatmap.objects[index].isSpinner()) {
				if (spinnerIndex == -1)
					spinnerIndex = index;
			} else
				stack.add(index);
		}
		for (int index = objectIndex; index < gameObjects.length && beatmap.objects[index].getTime() < trackPosition + approachTime; index++) {
			if (beatmap.objects[index].isSpinner()) {
				if (spinnerIndex == -1)
					spinnerIndex = index;
			} else
				stack.add(index);

			// draw follow points
			if (Options.isFollowPointEnabled() && !loseState)
				lastObjectIndex = drawFollowPointsBetween(objectIndex, lastObjectIndex, trackPosition);
		}
		if (spinnerIndex != -1)
			stack.add(spinnerIndex);

		// draw hit objects
		while (!stack.isEmpty()){
			int idx = stack.pop();
			GameObject gameObj = gameObjects[idx];

			// normal case
			if (!loseState)
				gameObj.draw(g, trackPosition);

			// death: make objects "fall" off the screen
			else {
				// time the object began falling
				int objTime = Math.max(beatmap.objects[idx].getTime() - approachTime, failTrackTime);
				float dt = (trackPosition - objTime) / (float) (MUSIC_FADEOUT_TIME);

				// would the object already be visible?
				if (dt <= 0)
					continue;

				// generate rotation speeds for each objects
				final float rotSpeed;
				if (rotations.containsKey(gameObj)) {
					rotSpeed = rotations.get(gameObj);
				} else {
					rotSpeed = (float) (2.0f * (Math.random() - 0.5f) * MAX_ROTATION);
					rotations.put(gameObj, rotSpeed);
				}

				g.pushTransform();

				// translate and rotate the object
				g.translate(0, dt * dt * container.getHeight());
				Vec2f rotationCenter = gameObj.getPointAt((beatmap.objects[idx].getTime() + beatmap.objects[idx].getEndTime()) / 2);
				g.rotate(rotationCenter.x, rotationCenter.y, rotSpeed * dt);
				gameObj.draw(g, trackPosition);

				g.popTransform();
			}
		}

		// draw result objects (over)
		data.drawHitResults(trackPosition, true);
	}

	/**
	 * Draws follow points between two objects.
	 * @param index the current object index
	 * @param lastObjectIndex the last object index
	 * @param trackPosition the current track position
	 * @return the new lastObjectIndex value
	 */
	private int drawFollowPointsBetween(int index, int lastObjectIndex, int trackPosition) {
		if (lastObjectIndex == -1)
			return -1;
		if (beatmap.objects[index].isSpinner() || beatmap.objects[index].isNewCombo())
			return lastObjectIndex;

		// calculate points
		final int followPointInterval = container.getHeight() / 14;
		int lastObjectEndTime = gameObjects[lastObjectIndex].getEndTime() + 1;
		int objectStartTime = beatmap.objects[index].getTime();
		Vec2f startPoint = gameObjects[lastObjectIndex].getPointAt(lastObjectEndTime);
		Vec2f endPoint = gameObjects[index].getPointAt(objectStartTime);
		float xDiff = endPoint.x - startPoint.x;
		float yDiff = endPoint.y - startPoint.y;
		float dist = (float) Math.hypot(xDiff, yDiff);
		int numPoints = (int) ((dist - GameImage.HITCIRCLE.getImage().getWidth()) / followPointInterval);
		if (numPoints > 0) {
			// set the image angle
			Image followPoint = GameImage.FOLLOWPOINT.getImage();
			float angle = (float) Math.toDegrees(Math.atan2(yDiff, xDiff));
			followPoint.setRotation(angle);

			// draw points
			float progress = 0f, alpha = 1f;
			if (lastObjectIndex < objectIndex)
				progress = (float) (trackPosition - lastObjectEndTime) / (objectStartTime - lastObjectEndTime);
			else {
				alpha = Utils.clamp((1f - ((objectStartTime - trackPosition) / (float) approachTime)) * 2f, 0, 1);
				followPoint.setAlpha(alpha);
			}

			float step = 1f / (numPoints + 1);
			float t = step;
			for (int i = 0; i < numPoints; i++) {
				float x = startPoint.x + xDiff * t;
				float y = startPoint.y + yDiff * t;
				float nextT = t + step;
				if (lastObjectIndex < objectIndex) {  // fade the previous trail
					if (progress < nextT) {
						if (progress > t)
							followPoint.setAlpha(1f - ((progress - t + step) / (step * 2f)));
						else if (progress > t - step)
							followPoint.setAlpha(1f - ((progress - (t - step)) / (step * 2f)));
						else
							followPoint.setAlpha(1f);
						followPoint.drawCentered(x, y);
					}
				} else
					followPoint.drawCentered(x, y);
				t = nextT;
			}
			followPoint.setAlpha(1f);
		}

		return index;
	}

	/**
	 * Loads all required data from a beatmap.
	 * @param beatmap the beatmap to load
	 */
	public void loadBeatmap(Beatmap beatmap) {
		this.beatmap = beatmap;
		Display.setTitle(String.format("%s - %s", game.getTitle(), beatmap.toString()));
		if (beatmap.timingPoints == null)
			BeatmapDB.load(beatmap, BeatmapDB.LOAD_ARRAY);
		BeatmapParser.parseHitObjects(beatmap);
		HitSound.setDefaultSampleSet(beatmap.sampleSet);

		Utils.gc(true);
	}

	/**
	 * Resets all game data and structures.
	 */
	public void resetGameData() {
		data.clear();
		objectIndex = 0;
		passedObjects = new LinkedList<Integer>();
		breakIndex = 0;
		breakTime = 0;
		breakSound = false;
		timingPointIndex = 0;
		beatLengthBase = beatLength = 1;
		pauseTime = -1;
		pausedMousePosition = null;
		countdownReadySound = false;
		countdown3Sound = false;
		countdown1Sound = false;
		countdown2Sound = false;
		countdownGoSound = false;
		checkpointLoaded = false;
		deaths = 0;
		deathTime = -1;
		replayFrames = null;
		lifeFrames = null;
		lastReplayTime = 0;
		autoMousePosition = new Vec2f();
		autoMousePressed = false;
		flashlightRadius = container.getHeight() * 2 / 3;
		scoreboardStarStream.clear();
		gameFinished = false;
		gameFinishedTimer.setTime(0);
		lastTrackPosition = 0;
		if (video != null) {
			try {
				video.close();
			} catch (IOException e) {}
			video = null;
		}
		videoSeekTime = 0;
		mergedSlider = null;
	}

	/**
	 * Skips the beginning of a track.
	 * @return {@code true} if skipped, {@code false} otherwise
	 */
	private synchronized boolean skipIntro() {
		int firstObjectTime = beatmap.objects[0].getTime();
		int trackPosition = MusicController.getPosition(true);
		if (objectIndex == 0 && trackPosition < firstObjectTime - SKIP_OFFSET) {
			if (isLeadIn()) {
				leadInTime = 0;
				MusicController.resume();
			}
			MusicController.setPosition(firstObjectTime - SKIP_OFFSET);
			MusicController.setPitch(getCurrentPitch());
			replaySkipTime = (isReplay) ? -1 : trackPosition;
			if (isReplay) {
				replayX = (int) skipButton.getX();
				replayY = (int) skipButton.getY();
			}
			playSoundEffect(SoundEffect.MENUHIT);
			return true;
		}
		return false;
	}

	/**
	 * Loads all game images.
	 */
	private void loadImages() {
		int width = container.getWidth();
		int height = container.getHeight();

		// set images
		File parent = beatmap.getFile().getParentFile();
		for (GameImage img : GameImage.values()) {
			if (img.isBeatmapSkinnable()) {
				img.setDefaultImage();
				img.setBeatmapSkinImage(parent);
			}
		}

		// skip button
		if (GameImage.SKIP.getImages() != null) {
			Animation skip = GameImage.SKIP.getAnimation();
			skipButton = new MenuButton(skip, width - skip.getWidth() / 2f, height - (skip.getHeight() / 2f));
		} else {
			Image skip = GameImage.SKIP.getImage();
			skipButton = new MenuButton(skip, width - skip.getWidth() / 2f, height - (skip.getHeight() / 2f));
		}
		skipButton.setHoverAnimationDuration(350);
		skipButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
		skipButton.setHoverExpand(1.1f, MenuButton.Expand.UP_LEFT);

		// load other images...
		((GamePauseMenu) game.getState(Opsu.STATE_GAMEPAUSEMENU)).loadImages();
		data.loadImages();
	}

	/**
	 * Loads the beatmap video (if any).
	 * @param offset the time to seek to (in milliseconds)
	 */
	private void loadVideo(int offset) {
		// close previous video
		if (video != null) {
			try {
				video.close();
			} catch (IOException e) {}
			video = null;
		}

		if (!Options.isBeatmapVideoEnabled() || beatmap.video == null || !beatmap.video.isFile() || !FFmpeg.exists())
			return;

		// load video
		int time = Math.max(0, offset);
		try {
			video = new Video(beatmap.video);
			video.seek(time);
			video.resume();
			videoSeekTime = time;
		} catch (Exception e) {
			video = null;
			videoSeekTime = 0;
			Log.error(e);
			UI.getNotificationManager().sendNotification("Failed to load beatmap video.\nSee log for details.", Color.red);
		}
	}

	/**
	 * Set map modifiers.
	 */
	private void setMapModifiers() {
		// map-based properties, re-initialized each game
		float multiplier = GameMod.getDifficultyMultiplier();
		float circleSize = Math.min(beatmap.circleSize * multiplier, 10f);
		float approachRate = Math.min(beatmap.approachRate * multiplier, 10f);
		float overallDifficulty = Math.min(beatmap.overallDifficulty * multiplier, 10f);
		float HPDrainRate = Math.min(beatmap.HPDrainRate * multiplier, 10f);

		// fixed difficulty overrides
		if (Options.getFixedCS() > 0f)
			circleSize = Options.getFixedCS();
		if (Options.getFixedAR() > 0f)
			approachRate = Options.getFixedAR();
		if (Options.getFixedOD() > 0f)
			overallDifficulty = Options.getFixedOD();
		if (Options.getFixedHP() > 0f)
			HPDrainRate = Options.getFixedHP();

		// Stack modifier scales with hit object size
		// StackOffset = HitObjectRadius / 10
		//int diameter = (int) (104 - (circleSize * 8));
		float diameter = 108.848f - (circleSize * 8.9646f);
		HitObject.setStackOffset(diameter * STACK_OFFSET_MODIFIER);

		// initialize objects
		Circle.init(container, diameter);
		Slider.init(container, diameter, beatmap);
		Spinner.init(container, overallDifficulty);
		Curve.init(container.getWidth(), container.getHeight(), diameter, (Options.isBeatmapSkinIgnored()) ?
				Options.getSkin().getSliderBorderColor() : beatmap.getSliderBorderColor());

		// approachRate (hit object approach time)
		approachTime = (int) Utils.mapDifficultyRange(approachRate, 1800, 1200, 450);

		// overallDifficulty (hit result time offsets)
		hitResultOffset = new int[GameData.HIT_MAX];
		hitResultOffset[GameData.HIT_300]  = (int) Utils.mapDifficultyRange(overallDifficulty, 80, 50, 20);
		hitResultOffset[GameData.HIT_100]  = (int) Utils.mapDifficultyRange(overallDifficulty, 140, 100, 60);
		hitResultOffset[GameData.HIT_50]   = (int) Utils.mapDifficultyRange(overallDifficulty, 200, 150, 100);
		hitResultOffset[GameData.HIT_MISS] = (int) (500 - (overallDifficulty * 10));
		data.setHitResultOffset(hitResultOffset);

		// HPDrainRate (health change)
		BeatmapHPDropRateCalculator hpCalc = new BeatmapHPDropRateCalculator(beatmap, HPDrainRate, overallDifficulty);
		hpCalc.calculate();
		hpDropRate = hpCalc.getHpDropRate();
		data.setHealthModifiers(HPDrainRate, hpCalc.getHpMultiplierNormal(), hpCalc.getHpMultiplierComboEnd());

		// difficulty multiplier (scoring)
		data.calculateDifficultyMultiplier(beatmap.HPDrainRate, beatmap.circleSize, beatmap.overallDifficulty);

		// hit object fade-in time (TODO: formula)
		fadeInTime = Math.min(375, (int) (approachTime / 2.5f));

		// fade times ("Hidden" mod)
		// TODO: find the actual formulas for this
		hiddenDecayTime = (int) (approachTime / 3.6f);
		hiddenTimeDiff = (int) (approachTime / 3.3f);
	}

	/**
	 * Sets the play state.
	 * @param state the new play state
	 */
	public void setPlayState(PlayState state) { this.playState = state; }

	/**
	 * Returns the current play state.
	 */
	public PlayState getPlayState() { return playState; }

	/**
	 * Returns whether or not the track is in the lead-in time state.
	 */
	public boolean isLeadIn() { return leadInTime > 0; }

	/**
	 * Returns the object approach time, in milliseconds.
	 */
	public int getApproachTime() { return approachTime; }

	/**
	 * Returns the amount of time for hit objects to fade in, in milliseconds.
	 */
	public int getFadeInTime() { return fadeInTime; }

	/**
	 * Returns the object decay time in the "Hidden" mod, in milliseconds.
	 */
	public int getHiddenDecayTime() { return hiddenDecayTime; }

	/**
	 * Returns the time before the hit object time by which the objects have
	 * completely faded in the "Hidden" mod, in milliseconds.
	 */
	public int getHiddenTimeDiff() { return hiddenTimeDiff; }

	/**
	 * Returns an array of hit result offset times, in milliseconds (indexed by GameData.HIT_* constants).
	 */
	public int[] getHitResultOffsets() { return hitResultOffset; }

	/**
	 * Returns the beat length.
	 */
	public float getBeatLength() { return beatLength; }

	/**
	 * Sets the beat length fields based on a given timing point.
	 * @param timingPoint the timing point
	 * @param setSampleSet whether to set the hit sample set based on the timing point
	 */
	private void setBeatLength(TimingPoint timingPoint, boolean setSampleSet) {
		if (!timingPoint.isInherited())
			beatLengthBase = beatLength = timingPoint.getBeatLength();
		else
			beatLength = beatLengthBase * timingPoint.getSliderMultiplier();
		if (setSampleSet) {
			HitSound.setDefaultSampleSet(timingPoint.getSampleType());
			SoundController.setSampleVolume(timingPoint.getSampleVolume());
		}
	}

	/**
	 * Returns the slider multiplier given by the current timing point.
	 */
	public float getTimingPointMultiplier() { return beatLength / beatLengthBase; }

	/**
	 * Sets a replay to view, or resets the replay if null.
	 * @param replay the replay
	 */
	public void setReplay(Replay replay) {
		if (replay == null) {
			this.isReplay = false;
			this.replay = null;
		} else {
			if (replay.frames == null) {
				ErrorHandler.error("Attempting to set a replay with no frames.", null, false);
				return;
			}
			this.isReplay = true;
			this.replay = replay;
		}
	}

	/**
	 * Adds a replay frame to the list, if possible, and runs it.
	 * @param x the cursor x coordinate
	 * @param y the cursor y coordinate
	 * @param keys the keys pressed
	 * @param time the time of the replay Frame
	 */
	private synchronized void addReplayFrameAndRun(int x, int y, int keys, int time){
		// "auto" and "autopilot" mods: use automatic cursor coordinates
		if (GameMod.AUTO.isActive() || GameMod.AUTOPILOT.isActive()) {
			x = (int) autoMousePosition.x;
			y = (int) autoMousePosition.y;
		}

		ReplayFrame frame = addReplayFrame(x, y, keys, time);
		if (frame != null)
			runReplayFrame(frame);
	}

	/**
	 * Runs a replay frame.
	 * @param frame the frame to run
	 */
	private void runReplayFrame(ReplayFrame frame){
		int keys = frame.getKeys();
		int replayX = frame.getScaledX();
		int replayY = frame.getScaledY();
		int deltaKeys = (keys & ~lastReplayKeys);  // keys that turned on
		if (deltaKeys != ReplayFrame.KEY_NONE)  // send a key press
			sendGameKeyPress(deltaKeys, replayX, replayY, frame.getTime());
		else if (keys != lastReplayKeys)
			;  // do nothing
		else
			updateGame(replayX, replayY, frame.getTimeDiff(), frame.getTime(), keys);
		lastReplayKeys = keys;
	}

	/**
	 * Sends a game key press and updates the hit objects.
	 * @param trackPosition the track position
	 * @param x the cursor x coordinate
	 * @param y the cursor y coordinate
	 * @param keys the keys that are pressed
	 */
	private void sendGameKeyPress(int keys, int x, int y, int trackPosition) {
		if (!hasMoreObjects() || gameFinished)  // nothing to do here
			return;

		// check missed objects first
		Iterator<Integer> iter = passedObjects.iterator();
		while (iter.hasNext()) {
			int index = iter.next();
			HitObject hitObject = beatmap.objects[index];
			if (hitObject.isCircle() && gameObjects[index].mousePressed(x, y, trackPosition)) {
				iter.remove();  // circle hit, remove it
				return;
			} else if (hitObject.isSlider() && gameObjects[index].mousePressed(x, y, trackPosition))
				return;  // slider initial circle hit
		}

		// check current object
		if (objectIndex >= gameObjects.length)
			return;
		HitObject hitObject = beatmap.objects[objectIndex];
		if (hitObject.isCircle() && gameObjects[objectIndex].mousePressed(x, y, trackPosition))
			objectIndex++;  // circle hit
		else if (hitObject.isSlider())
			gameObjects[objectIndex].mousePressed(x, y, trackPosition);
	}

	/**
	 * Adds a replay frame to the list, if possible.
	 * @param x the cursor x coordinate
	 * @param y the cursor y coordinate
	 * @param keys the keys pressed
	 * @param time the time of the replay frame
	 * @return a ReplayFrame representing the data
	 */
	private ReplayFrame addReplayFrame(int x, int y, int keys, int time) {
		int timeDiff = time - lastReplayTime;
		lastReplayTime = time;
		int cx = (int) ((x - HitObject.getXOffset()) / HitObject.getXMultiplier());
		int cy = (int) ((y - HitObject.getYOffset()) / HitObject.getYMultiplier());
		ReplayFrame frame = new ReplayFrame(timeDiff, time, cx, cy, keys);
		if (replayFrames != null)
			replayFrames.add(frame);
		return frame;
	}

	/**
	 * Adds a life frame to the list, if possible.
	 * @param time the time of the life frame
	 */
	private void addLifeFrame(int time) {
		if (lifeFrames == null)
			return;

		// don't record life frames before first object
		if (time < beatmap.objects[0].getTime() - approachTime)
			return;

		lifeFrames.add(new LifeFrame(time, data.getHealthPercent() / 100f));
	}

	/**
	 * Returns the point at the t value between a start and end point.
	 * @param startX the starting x coordinate
	 * @param startY the starting y coordinate
	 * @param endX the ending x coordinate
	 * @param endY the ending y coordinate
	 * @param t the t value [0, 1]
	 * @return the position vector
	 */
	private Vec2f getPointAt(float startX, float startY, float endX, float endY, float t) {
		// "autopilot" mod: move quicker between objects
		if (GameMod.AUTOPILOT.isActive())
			t = Utils.clamp(t * 2f, 0f, 1f);
		return new Vec2f(startX + (endX - startX) * t, startY + (endY - startY) * t);
	}

	/**
	 * Updates the current visible area radius (if the "flashlight" mod is enabled).
	 * @param delta the delta interval
	 * @param trackPosition the track position
	 */
	private void updateFlashlightRadius(int delta, int trackPosition) {
		if (!GameMod.FLASHLIGHT.isActive())
			return;

		int width = container.getWidth(), height = container.getHeight();
		boolean firstObject = (objectIndex == 0 && trackPosition < beatmap.objects[0].getTime());
		if (isLeadIn()) {
			// lead-in: expand area
			float progress = Math.max((float) (leadInTime - beatmap.audioLeadIn) / approachTime, 0f);
			flashlightRadius = width - (int) ((width - (height * 2 / 3)) * progress);
		} else if (firstObject) {
			// before first object: shrink area
			int timeDiff = beatmap.objects[0].getTime() - trackPosition;
			flashlightRadius = width;
			if (timeDiff < approachTime) {
				float progress = (float) timeDiff / approachTime;
				flashlightRadius -= (width - (height * 2 / 3)) * (1 - progress);
			}
		} else {
			// gameplay: size based on combo
			int targetRadius;
			int combo = data.getComboStreak();
			if (combo < 100)
				targetRadius = height * 2 / 3;
			else if (combo < 200)
				targetRadius = height / 2;
			else
				targetRadius = height / 3;
			if (beatmap.breaks != null && breakIndex < beatmap.breaks.size() && breakTime > 0) {
				// breaks: expand at beginning, shrink at end
				flashlightRadius = targetRadius;
				int endTime = beatmap.breaks.get(breakIndex);
				int breakLength = endTime - breakTime;
				if (breakLength > approachTime * 3) {
					float progress = 1f;
					if (trackPosition - breakTime < approachTime)
						progress = (float) (trackPosition - breakTime) / approachTime;
					else if (endTime - trackPosition < approachTime)
						progress = (float) (endTime - trackPosition) / approachTime;
					flashlightRadius += (width - flashlightRadius) * progress;
				}
			} else if (flashlightRadius != targetRadius) {
				// radius size change
				float radiusDiff = height * delta / 2000f;
				if (flashlightRadius > targetRadius) {
					flashlightRadius -= radiusDiff;
					if (flashlightRadius < targetRadius)
						flashlightRadius = targetRadius;
				} else {
					flashlightRadius += radiusDiff;
					if (flashlightRadius > targetRadius)
						flashlightRadius = targetRadius;
				}
			}
		}
	}

	/**
	 * Performs stacking calculations on all hit objects, and updates their
	 * positions if necessary.
	 * @author peppy (https://gist.github.com/peppy/1167470)
	 */
	private void calculateStacks() {
		// reverse pass for stack calculation
		for (int i = gameObjects.length - 1; i > 0; i--) {
			HitObject hitObjectI = beatmap.objects[i];

			// already calculated
			if (hitObjectI.getStack() != 0 || hitObjectI.isSpinner())
				continue;

			// search for hit objects in stack
			for (int n = i - 1; n >= 0; n--) {
				HitObject hitObjectN = beatmap.objects[n];
				if (hitObjectN.isSpinner())
					continue;

				// check if in range stack calculation
				float timeI = hitObjectI.getTime() - (approachTime * beatmap.stackLeniency);
				float timeN = hitObjectN.isSlider() ? gameObjects[n].getEndTime() : hitObjectN.getTime();
				if (timeI > timeN)
					break;

				// possible special case: if slider end in the stack,
				// all next hit objects in stack move right down
				if (hitObjectN.isSlider()) {
					Vec2f p1 = gameObjects[i].getPointAt(hitObjectI.getTime());
					Vec2f p2 = gameObjects[n].getPointAt(gameObjects[n].getEndTime());
					float distance = Utils.distance(p1.x, p1.y, p2.x, p2.y);

					// check if hit object part of this stack
					if (distance < STACK_LENIENCE * HitObject.getXMultiplier()) {
						int offset = hitObjectI.getStack() - hitObjectN.getStack() + 1;
						for (int j = n + 1; j <= i; j++) {
							HitObject hitObjectJ = beatmap.objects[j];
							p1 = gameObjects[j].getPointAt(hitObjectJ.getTime());
							distance = Utils.distance(p1.x, p1.y, p2.x, p2.y);

							// hit object below slider end
							if (distance < STACK_LENIENCE * HitObject.getXMultiplier())
								hitObjectJ.setStack(hitObjectJ.getStack() - offset);
						}
						break;  // slider end always start of the stack: reset calculation
					}
				}

				// not a special case: stack moves up left
				float distance = Utils.distance(
						hitObjectI.getX(), hitObjectI.getY(),
						hitObjectN.getX(), hitObjectN.getY()
				);
				if (distance < STACK_LENIENCE) {
					hitObjectN.setStack(hitObjectI.getStack() + 1);
					hitObjectI = hitObjectN;
				}
			}
		}

		// update hit object positions
		for (int i = 0; i < gameObjects.length; i++) {
			if (beatmap.objects[i].getStack() != 0)
				gameObjects[i].updatePosition();
		}
	}

	/** Creates the single merged slider. */
	private void createMergedSlider() {
		// workaround for sliders not appearing after loading a checkpoint
		// https://github.com/yugecin/opsu-dance/issues/130
		if (!Options.isExperimentalSliderShrinking())
			mergedSlider = null;

		// initialize merged slider structures
		if (mergedSlider == null) {
			List<Vec2f> curvePoints = new ArrayList<Vec2f>();
			for (GameObject gameObject : gameObjects) {
				if (gameObject instanceof Slider) {
					Slider slider = (Slider) gameObject;
					slider.baseSliderFrom = curvePoints.size();
					curvePoints.addAll(Arrays.asList(slider.getCurve().getCurvePoints()));
				}
			}
			if (!curvePoints.isEmpty())
				this.mergedSlider = new FakeCombinedCurve(curvePoints.toArray(new Vec2f[curvePoints.size()]));
		} else {
			int base = 0;
			for (GameObject gameObject : gameObjects) {
				if (gameObject instanceof Slider) {
					Slider slider = (Slider) gameObject;
					slider.baseSliderFrom = base;
					base += slider.getCurve().getCurvePoints().length;
				}
			}
		}
	}

	/**
	 * Adds points in the merged slider to render.
	 * @param from the start index to render
	 * @param to the end index to render
	 */
	public void addMergedSliderPointsToRender(int from, int to) {
		mergedSlider.addRange(from, to);
	}

	/** Returns whether there are any more objects remaining in the map. */
	private boolean hasMoreObjects() {
		return objectIndex < gameObjects.length || !passedObjects.isEmpty();
	}

	/** Returns the current pitch. */
	private float getCurrentPitch() {
		float base = (Options.getFixedSpeed() > 0f) ? Options.getFixedSpeed() : 1f;
		return base * GameMod.getSpeedMultiplier() * playbackSpeed.getModifier();
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
	 * Adjusts the beatmap's local music offset.
	 * @param sign the sign (multiplier)
	 */
	private void adjustLocalMusicOffset(int sign) {
		if (pauseTime > -1) {
			UI.getNotificationManager().sendBarNotification("Offset can only be changed while game is not paused.");
			return;
		}

		boolean alt = input.isKeyDown(Input.KEY_LALT) || input.isKeyDown(Input.KEY_RALT);
		int diff = sign * (alt ? 1 : 5);
		int newOffset = Utils.clamp(beatmap.localMusicOffset + diff, -1000, 1000);
		UI.getNotificationManager().sendBarNotification(String.format("Local beatmap offset set to %dms", newOffset));
		if (beatmap.localMusicOffset != newOffset) {
			beatmap.localMusicOffset = newOffset;
			BeatmapDB.updateLocalOffset(beatmap);
		}
	}

	/**
	 * Plays a sound, unless gameplay sounds are disabled.
	 * @param s the sound effect
	 */
	private void playSoundEffect(SoundEffect s) {
		if (!Options.isGameplaySoundDisabled())
			SoundController.playSound(s);
	}
}
