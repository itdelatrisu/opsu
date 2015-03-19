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
import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.MenuButton;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.OsuParser;
import itdelatrisu.opsu.OsuTimingPoint;
import itdelatrisu.opsu.ScoreData;
import itdelatrisu.opsu.UI;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.HitSound;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.db.OsuDB;
import itdelatrisu.opsu.db.ScoreDB;
import itdelatrisu.opsu.objects.Circle;
import itdelatrisu.opsu.objects.HitObject;
import itdelatrisu.opsu.objects.Slider;
import itdelatrisu.opsu.objects.Spinner;
import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.replay.ReplayFrame;

import java.io.File;
import java.util.LinkedList;
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
import org.newdawn.slick.state.transition.EmptyTransition;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

/**
 * "Game" state.
 */
public class Game extends BasicGameState {
	/** Game restart states. */
	public enum Restart {
		/** No restart. */
		FALSE,
		/** First time loading the song. */
		NEW,
		/** Manual retry. */
		MANUAL,
		/** Replay. */
		REPLAY,
		/** Health is zero: no-continue/force restart. */
		LOSE;
	}

	/** Minimum time before start of song, in milliseconds, to process skip-related actions. */
	private static final int SKIP_OFFSET = 2000;

	/** The associated OsuFile object. */
	private OsuFile osu;

	/** The associated GameData object. */
	private GameData data;

	/** Current hit object index in OsuHitObject[] array. */
	private int objectIndex = 0;

	/** The map's HitObjects, indexed by objectIndex. */
	private HitObject[] hitObjects;

	/** Delay time, in milliseconds, before song starts. */
	private int leadInTime;

	/** Hit object approach time, in milliseconds. */
	private int approachTime;

	/** Time offsets for obtaining each hit result (indexed by HIT_* constants). */
	private int[] hitResultOffset;

	/** Current restart state. */
	private Restart restart;

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
	private int pausedMouseX = -1, pausedMouseY = -1;

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

	/** The replay keys pressed. */
	private int replayKeys;

	/** Whether a replay key is currently pressed. */
	private boolean replayKeyPressed;

	/** The replay skip time, or -1 if none. */
	private int replaySkipTime = -1;

	/** The thread updating the replay frames. */
	private Thread replayThread;

	/** Whether or not the replay thread should continue running. */
	private boolean replayThreadRunning;

	/** The last replay frame time. */
	private int lastReplayTime = 0;

	/** The last game keys pressed. */
	private int lastKeysPressed = ReplayFrame.KEY_NONE;

	/** The previous game mod state (before the replay). */
	private int previousMods = 0;

	/** The list of current replay frames (for recording replays). */
	private LinkedList<ReplayFrame> replayFrames;

	/** The offscreen image rendered to. */
	private Image offscreen;

	/** The offscreen graphics. */
	private Graphics gOffscreen;

	/** The current flashlight area radius. */
	private int flashlightRadius;

	/** The cursor coordinates using the "auto" or "relax" mods. */
	private int autoMouseX = 0, autoMouseY = 0;

	/** Whether or not the cursor should be pressed using the "auto" mod. */
	private boolean autoMousePressed;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private int state;

	public Game(int state) {
		this.state = state;
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

		// create the associated GameData object
		data = new GameData(width, height);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();
		g.setBackground(Color.black);

		// "flashlight" mod: initialize offscreen graphics
		if (GameMod.FLASHLIGHT.isActive()) {
			gOffscreen.clear();
			Graphics.setCurrent(gOffscreen);
		}

		// background
		float dimLevel = Options.getBackgroundDim();
		if (Options.isDefaultPlayfieldForced() || !osu.drawBG(width, height, dimLevel, false)) {
			Image playfield = GameImage.PLAYFIELD.getImage();
			playfield.setAlpha(dimLevel);
			playfield.draw();
			playfield.setAlpha(1f);
		}

		if (GameMod.FLASHLIGHT.isActive())
			Graphics.setCurrent(g);

		int trackPosition = MusicController.getPosition();
		if (pauseTime > -1)  // returning from pause screen
			trackPosition = pauseTime;
		else if (deathTime > -1)  // "Easy" mod: health bar increasing
			trackPosition = deathTime;
		int firstObjectTime = osu.objects[0].getTime();
		int timeDiff = firstObjectTime - trackPosition;

		// "auto" and "autopilot" mods: move cursor automatically
		// TODO: this should really be in update(), not render()
		autoMouseX = width / 2;
		autoMouseY = height / 2;
		autoMousePressed = false;
		if (GameMod.AUTO.isActive() || GameMod.AUTOPILOT.isActive()) {
			float[] autoXY = null;
			if (isLeadIn()) {
				// lead-in
				float progress = Math.max((float) (leadInTime - osu.audioLeadIn) / approachTime, 0f);
				autoMouseY = (int) (height / (2f - progress));
			} else if (objectIndex == 0 && trackPosition < firstObjectTime) {
				// before first object
				timeDiff = firstObjectTime - trackPosition;
				if (timeDiff < approachTime) {
					float[] xy = hitObjects[0].getPointAt(trackPosition);
					autoXY = getPointAt(autoMouseX, autoMouseY, xy[0], xy[1], 1f - ((float) timeDiff / approachTime));
				}
			} else if (objectIndex < osu.objects.length) {
				// normal object
				int objectTime = osu.objects[objectIndex].getTime();
				if (trackPosition < objectTime) {
					float[] xyStart = hitObjects[objectIndex - 1].getPointAt(trackPosition);
					int startTime = hitObjects[objectIndex - 1].getEndTime();
					if (osu.breaks != null && breakIndex < osu.breaks.size()) {
						// starting a break: keep cursor at previous hit object position
						if (breakTime > 0 || objectTime > osu.breaks.get(breakIndex))
							autoXY = xyStart;

						// after a break ends: move startTime to break end time
						else if (breakIndex > 1) {
							int lastBreakEndTime = osu.breaks.get(breakIndex - 1);
							if (objectTime > lastBreakEndTime && startTime < lastBreakEndTime)
								startTime = lastBreakEndTime;
						}
					}
					if (autoXY == null) {
						float[] xyEnd = hitObjects[objectIndex].getPointAt(trackPosition);
						int totalTime = objectTime - startTime;
						autoXY = getPointAt(xyStart[0], xyStart[1], xyEnd[0], xyEnd[1], (float) (trackPosition - startTime) / totalTime);

						// hit circles: show a mouse press
						int offset300 = hitResultOffset[GameData.HIT_300];
						if ((osu.objects[objectIndex].isCircle() && objectTime - trackPosition < offset300) ||
						    (osu.objects[objectIndex - 1].isCircle() && trackPosition - osu.objects[objectIndex - 1].getTime() < offset300))
							autoMousePressed = true;
					}
				} else {
					autoXY = hitObjects[objectIndex].getPointAt(trackPosition);
					autoMousePressed = true;
				}
			} else {
				// last object
				autoXY = hitObjects[objectIndex - 1].getPointAt(trackPosition);
			}

			// set mouse coordinates
			if (autoXY != null) {
				autoMouseX = (int) autoXY[0];
				autoMouseY = (int) autoXY[1];
			}
		}

		// "flashlight" mod: restricted view of hit objects around cursor
		if (GameMod.FLASHLIGHT.isActive()) {
			// render hit objects offscreen
			Graphics.setCurrent(gOffscreen);
			int trackPos = (isLeadIn()) ? (leadInTime - Options.getMusicOffset()) * -1 : trackPosition;
			drawHitObjects(gOffscreen, trackPos);

			// restore original graphics context
			gOffscreen.flush();
			Graphics.setCurrent(g);

			// draw alpha map around cursor
			g.setDrawMode(Graphics.MODE_ALPHA_MAP);
			g.clearAlphaMap();
			int mouseX, mouseY;
			if (pauseTime > -1 && pausedMouseX > -1 && pausedMouseY > -1) {
				mouseX = pausedMouseX;
				mouseY = pausedMouseY;
			} else if (GameMod.AUTO.isActive() || GameMod.AUTOPILOT.isActive()) {
				mouseX = autoMouseX;
				mouseY = autoMouseY;
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
		if (osu.breaks != null && breakIndex < osu.breaks.size() && breakTime > 0) {
			int endTime = osu.breaks.get(breakIndex);
			int breakLength = endTime - breakTime;

			// letterbox effect (black bars on top/bottom)
			if (osu.letterboxInBreaks && breakLength >= 4000) {
				g.setColor(Color.black);
				g.fillRect(0, 0, width, height * 0.125f);
				g.fillRect(0, height * 0.875f, width, height * 0.125f);
			}

			data.drawGameElements(g, true, objectIndex == 0);

			if (breakLength >= 8000 &&
				trackPosition - breakTime > 2000 &&
				trackPosition - breakTime < 5000) {
				// show break start
				if (data.getHealth() >= 50) {
					GameImage.SECTION_PASS.getImage().drawCentered(width / 2f, height / 2f);
					if (!breakSound) {
						SoundController.playSound(SoundEffect.SECTIONPASS);
						breakSound = true;
					}
				} else {
					GameImage.SECTION_FAIL.getImage().drawCentered(width / 2f, height / 2f);
					if (!breakSound) {
						SoundController.playSound(SoundEffect.SECTIONFAIL);
						breakSound = true;
					}
				}
			} else if (breakLength >= 4000) {
				// show break end (flash twice for 500ms)
				int endTimeDiff = endTime - trackPosition;
				if ((endTimeDiff > 1500 && endTimeDiff < 2000) ||
					(endTimeDiff > 500 && endTimeDiff < 1000)) {
					Image arrow = GameImage.WARNINGARROW.getImage();
					arrow.setRotation(0);
					arrow.draw(width * 0.15f, height * 0.15f);
					arrow.draw(width * 0.15f, height * 0.75f);
					arrow.setRotation(180);
					arrow.draw(width * 0.75f, height * 0.15f);
					arrow.draw(width * 0.75f, height * 0.75f);
				}
			}
		}

		// non-break
		else {
			// game elements
			data.drawGameElements(g, false, objectIndex == 0);
	
			// skip beginning
			if (objectIndex == 0 &&
			    trackPosition < osu.objects[0].getTime() - SKIP_OFFSET)
				skipButton.draw();

			// show retries
			if (retries >= 2 && timeDiff >= -1000) {
				int retryHeight = Math.max(
						GameImage.SCOREBAR_BG.getImage().getHeight(),
						GameImage.SCOREBAR_KI.getImage().getHeight()
				);
				float oldAlpha = Utils.COLOR_WHITE_FADE.a;
				if (timeDiff < -500)
					Utils.COLOR_WHITE_FADE.a = (1000 + timeDiff) / 500f;
				Utils.FONT_MEDIUM.drawString(
						2 + (width / 100), retryHeight,
						String.format("%d retries and counting...", retries),
						Utils.COLOR_WHITE_FADE
				);
				Utils.COLOR_WHITE_FADE.a = oldAlpha;
			}

			if (isLeadIn())
				trackPosition = (leadInTime - Options.getMusicOffset()) * -1;  // render approach circles during song lead-in

			// countdown
			if (osu.countdown > 0) {  // TODO: implement half/double rate settings
				timeDiff = firstObjectTime - trackPosition;
				if (timeDiff >= 500 && timeDiff < 3000) {
					if (timeDiff >= 1500) {
						GameImage.COUNTDOWN_READY.getImage().drawCentered(width / 2, height / 2);
						if (!countdownReadySound) {
							SoundController.playSound(SoundEffect.READY);
							countdownReadySound = true;
						}
					}
					if (timeDiff < 2000) {
						GameImage.COUNTDOWN_3.getImage().draw(0, 0);
						if (!countdown3Sound) {
							SoundController.playSound(SoundEffect.COUNT3);
							countdown3Sound = true;
						}
					}
					if (timeDiff < 1500) {
						GameImage.COUNTDOWN_2.getImage().draw(width - GameImage.COUNTDOWN_2.getImage().getWidth(), 0);
						if (!countdown2Sound) {
							SoundController.playSound(SoundEffect.COUNT2);
							countdown2Sound = true;
						}
					}
					if (timeDiff < 1000) {
						GameImage.COUNTDOWN_1.getImage().drawCentered(width / 2, height / 2);
						if (!countdown1Sound) {
							SoundController.playSound(SoundEffect.COUNT1);
							countdown1Sound = true;
						}
					}
				} else if (timeDiff >= -500 && timeDiff < 500) {
					Image go = GameImage.COUNTDOWN_GO.getImage();
					go.setAlpha((timeDiff < 0) ? 1 - (timeDiff / -1000f) : 1);
					go.drawCentered(width / 2, height / 2);
					if (!countdownGoSound) {
						SoundController.playSound(SoundEffect.GO);
						countdownGoSound = true;
					}
				}
			}

			// draw hit objects
			if (!GameMod.FLASHLIGHT.isActive())
				drawHitObjects(g, trackPosition);
		}

		if (GameMod.AUTO.isActive())
			GameImage.UNRANKED.getImage().drawCentered(width / 2, height * 0.077f);

		// returning from pause screen
		if (pauseTime > -1 && pausedMouseX > -1 && pausedMouseY > -1) {
			// darken the screen
			g.setColor(Utils.COLOR_BLACK_ALPHA);
			g.fillRect(0, 0, width, height);

			// draw glowing hit select circle and pulse effect
			int circleRadius = GameImage.HITCIRCLE.getImage().getWidth();
			Image cursorCircle = GameImage.HITCIRCLE_SELECT.getImage().getScaledCopy(circleRadius, circleRadius);
			cursorCircle.setAlpha(1.0f);
			cursorCircle.drawCentered(pausedMouseX, pausedMouseY);
			Image cursorCirclePulse = cursorCircle.getScaledCopy(1f + pausePulse);
			cursorCirclePulse.setAlpha(1f - pausePulse);
			cursorCirclePulse.drawCentered(pausedMouseX, pausedMouseY);
		}

		if (GameMod.AUTO.isActive())
			UI.draw(g, autoMouseX, autoMouseY, autoMousePressed);
		else if (GameMod.AUTOPILOT.isActive())
			UI.draw(g, autoMouseX, autoMouseY, Utils.isGameKeyPressed());
		else if (!isReplay)
			UI.draw(g);
		else
			UI.draw(g, replayX, replayY, replayKeyPressed);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		UI.update(delta);
		int mouseX, mouseY;
		if (GameMod.AUTO.isActive() || GameMod.AUTOPILOT.isActive()) {
			mouseX = autoMouseX;
			mouseY = autoMouseY;
		} else if (!isReplay) {
			mouseX = input.getMouseX();
			mouseY = input.getMouseY();
		} else {
			mouseX = replayX;
			mouseY = replayY;
		}
		skipButton.hoverUpdate(delta, mouseX, mouseY);
		int trackPosition = MusicController.getPosition();

		// "flashlight" mod: calculate visible area radius
		if (GameMod.FLASHLIGHT.isActive()) {
			int width = container.getWidth(), height = container.getHeight();
			boolean firstObject = (objectIndex == 0 && trackPosition < osu.objects[0].getTime());
			if (isLeadIn()) {
				// lead-in: expand area
				float progress = Math.max((float) (leadInTime - osu.audioLeadIn) / approachTime, 0f);
				flashlightRadius = width - (int) ((width - (height * 2 / 3)) * progress);
			} else if (firstObject) {
				// before first object: shrink area
				int timeDiff = osu.objects[0].getTime() - trackPosition;
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
				if (osu.breaks != null && breakIndex < osu.breaks.size() && breakTime > 0) {
					// breaks: expand at beginning, shrink at end
					flashlightRadius = targetRadius;
					int endTime = osu.breaks.get(breakIndex);
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

		if (isLeadIn()) {  // stop updating during song lead-in
			leadInTime -= delta;
			if (!isLeadIn())
				MusicController.resume();
			return;
		}

		// returning from pause screen: must click previous mouse position
		if (pauseTime > -1) {
			// paused during lead-in or break, or "relax" or "autopilot": continue immediately
			if ((pausedMouseX < 0 && pausedMouseY < 0) ||
			    (GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive())) {
				pauseTime = -1;
				if (!isLeadIn())
					MusicController.resume();
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
			}
			return;
		}

		// "Easy" mod: multiple "lives"
		if (GameMod.EASY.isActive() && deathTime > -1) {
			if (data.getHealth() < 99f)
				data.changeHealth(delta / 10f);
			else {
				MusicController.resume();
				deathTime = -1;
			}
		}

		data.updateDisplays(delta);

		// map complete!
		if (objectIndex >= hitObjects.length || (MusicController.trackEnded() && objectIndex > 0)) {
			// track ended before last object was processed: force a hit result
			if (MusicController.trackEnded() && objectIndex < hitObjects.length)
				hitObjects[objectIndex].update(true, delta, mouseX, mouseY, false);

			// if checkpoint used, skip ranking screen
			if (checkpointLoaded)
				game.closeRequested();

			// go to ranking screen
			else {
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
					Replay r = data.getReplay(replayFrames.toArray(new ReplayFrame[replayFrames.size()]));
					if (r != null && !unranked)
						r.save();
				}
				ScoreData score = data.getScoreData(osu);

				// add score to database
				if (!unranked && !isReplay)
					ScoreDB.addScore(score);

				game.enterState(Opsu.STATE_GAMERANKING, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			}
			return;
		}

		// timing points
		if (timingPointIndex < osu.timingPoints.size()) {
			OsuTimingPoint timingPoint = osu.timingPoints.get(timingPointIndex);
			if (trackPosition >= timingPoint.getTime()) {
				if (!timingPoint.isInherited())
					beatLengthBase = beatLength = timingPoint.getBeatLength();
				else
					beatLength = beatLengthBase * timingPoint.getSliderMultiplier();
				HitSound.setDefaultSampleSet(timingPoint.getSampleType());
				SoundController.setSampleVolume(timingPoint.getSampleVolume());
				timingPointIndex++;
			}
		}

		// replays
		if (isReplay) {
			// skip intro
			if (replaySkipTime > 0 && trackPosition > replaySkipTime) {
				skipIntro();
				replaySkipTime = -1;
			}
		}

		// song beginning
		if (objectIndex == 0 && trackPosition < osu.objects[0].getTime())
			return;  // nothing to do here

		// break periods
		if (osu.breaks != null && breakIndex < osu.breaks.size()) {
			int breakValue = osu.breaks.get(breakIndex);
			if (breakTime > 0) {  // in a break period
				if (trackPosition < breakValue)
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
				pausedMouseX = mouseX;
				pausedMouseY = mouseY;
				pausePulse = 0f;
			}
			if (MusicController.isPlaying() || isLeadIn())
				pauseTime = trackPosition;
			game.enterState(Opsu.STATE_GAMEPAUSEMENU);
		}

		// drain health
		data.changeHealth(delta * -1 * GameData.HP_DRAIN_MULTIPLIER);
		if (!data.isAlive()) {
			// "Easy" mod
			if (GameMod.EASY.isActive() && !GameMod.SUDDEN_DEATH.isActive()) {
				deaths++;
				if (deaths < 3) {
					deathTime = trackPosition;
					MusicController.pause();
					return;
				}
			}

			// game over, force a restart
			if (!isReplay) {
				restart = Restart.LOSE;
				game.enterState(Opsu.STATE_GAMEPAUSEMENU);
			}
		}

		// update objects (loop in unlikely event of any skipped indexes)
		boolean keyPressed = (isReplay) ? replayKeyPressed : Utils.isGameKeyPressed();
		while (objectIndex < hitObjects.length && trackPosition > osu.objects[objectIndex].getTime()) {
			// check if we've already passed the next object's start time
			boolean overlap = (objectIndex + 1 < hitObjects.length &&
					trackPosition > osu.objects[objectIndex + 1].getTime() - hitResultOffset[GameData.HIT_300]);

			// update hit object and check completion status
			if (hitObjects[objectIndex].update(overlap, delta, mouseX, mouseY, keyPressed))
				objectIndex++;  // done, so increment object index
			else
				break;
		}
	}

	@Override
	public int getID() { return state; }

	@Override
	public void keyPressed(int key, char c) {
		int trackPosition = MusicController.getPosition();

		// game keys
		if (!Keyboard.isRepeatEvent() && !isReplay) {
			if (key == Options.getGameKeyLeft())
				gameKeyPressed(ReplayFrame.KEY_K1, input.getMouseX(), input.getMouseY());
			else if (key == Options.getGameKeyRight())
				gameKeyPressed(ReplayFrame.KEY_K2, input.getMouseX(), input.getMouseY());
		}

		switch (key) {
		case Input.KEY_ESCAPE:
			// "auto" mod or watching replay: go back to song menu
			if (GameMod.AUTO.isActive() || isReplay) {
				game.closeRequested();
				break;
			}

			// pause game
			if (pauseTime < 0 && breakTime <= 0 && trackPosition >= osu.objects[0].getTime()) {
				pausedMouseX = input.getMouseX();
				pausedMouseY = input.getMouseY();
				pausePulse = 0f;
			}
			if (MusicController.isPlaying() || isLeadIn())
				pauseTime = trackPosition;
			game.enterState(Opsu.STATE_GAMEPAUSEMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			break;
		case Input.KEY_SPACE:
			// skip intro
			skipIntro();
			break;
		case Input.KEY_R:
			// restart
			if (input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) {
				try {
					if (trackPosition < osu.objects[0].getTime())
						retries--;  // don't count this retry (cancel out later increment)
					restart = Restart.MANUAL;
					enter(container, game);
					skipIntro();
				} catch (SlickException e) {
					ErrorHandler.error("Failed to restart game.", e, false);
				}
			}
			break;
		case Input.KEY_S:
			// save checkpoint
			if (input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) {
				if (isLeadIn())
					break;

				int position = (pauseTime > -1) ? pauseTime : trackPosition;
				if (Options.setCheckpoint(position / 1000)) {
					SoundController.playSound(SoundEffect.MENUCLICK);
					UI.sendBarNotification("Checkpoint saved.");
				}
			}
			break;
		case Input.KEY_L:
			// load checkpoint
			if (input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) {
				int checkpoint = Options.getCheckpoint();
				if (checkpoint == 0 || checkpoint > osu.endTime)
					break;  // invalid checkpoint
				try {
					killReplayThread();
					restart = Restart.MANUAL;
					enter(container, game);
					checkpointLoaded = true;
					if (isLeadIn()) {
						leadInTime = 0;
						MusicController.resume();
					}
					SoundController.playSound(SoundEffect.MENUHIT);
					UI.sendBarNotification("Checkpoint loaded.");

					// skip to checkpoint
					MusicController.setPosition(checkpoint);
					while (objectIndex < hitObjects.length &&
							osu.objects[objectIndex++].getTime() <= checkpoint)
						;
					objectIndex--;
				} catch (SlickException e) {
					ErrorHandler.error("Failed to load checkpoint.", e, false);
				}
			}
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

	@Override
	public void mousePressed(int button, int x, int y) {
		if (Options.isMouseDisabled())
			return;

		// watching replay
		if (isReplay) {
			// only allow skip button
			if (button != Input.MOUSE_MIDDLE_BUTTON && skipButton.contains(x, y))
				skipIntro();
			return;
		}

		// mouse wheel: pause the game
		if (button == Input.MOUSE_MIDDLE_BUTTON && !Options.isMouseWheelDisabled()) {
			int trackPosition = MusicController.getPosition();
			if (pauseTime < 0 && breakTime <= 0 && trackPosition >= osu.objects[0].getTime()) {
				pausedMouseX = x;
				pausedMouseY = y;
				pausePulse = 0f;
			}
			if (MusicController.isPlaying() || isLeadIn())
				pauseTime = trackPosition;
			game.enterState(Opsu.STATE_GAMEPAUSEMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			return;
		}

		gameKeyPressed((button == Input.MOUSE_LEFT_BUTTON) ? ReplayFrame.KEY_M1 : ReplayFrame.KEY_M2, x, y);
	}

	/**
	 * Handles a game key pressed event.
	 * @param keys the game keys pressed
	 * @param x the mouse x coordinate
	 * @param y the mouse y coordinate
	 */
	private void gameKeyPressed(int keys, int x, int y) {
		// returning from pause screen
		if (pauseTime > -1) {
			double distance = Math.hypot(pausedMouseX - x, pausedMouseY - y);
			int circleRadius = GameImage.HITCIRCLE.getImage().getWidth() / 2;
			if (distance < circleRadius) {
				// unpause the game
				pauseTime = -1;
				pausedMouseX = -1;
				pausedMouseY = -1;
				if (!isLeadIn())
					MusicController.resume();
			}
			return;
		}

		if (objectIndex >= hitObjects.length)  // nothing left to do here
			return;

		OsuHitObject hitObject = osu.objects[objectIndex];

		// skip beginning
		if (skipButton.contains(x, y)) {
			if (skipIntro())
				return;  // successfully skipped
		}

		// "auto" and "relax" mods: ignore user actions
		if (GameMod.AUTO.isActive() || GameMod.RELAX.isActive())
			return;

		// "autopilot" mod: ignore actual cursor coordinates
		int cx, cy;
		if (GameMod.AUTOPILOT.isActive()) {
			cx = autoMouseX;
			cy = autoMouseY;
		} else {
			cx = x;
			cy = y;
		}

		if (!isReplay)
			addReplayFrame(cx, cy, lastKeysPressed | keys);

		// circles
		if (hitObject.isCircle() && hitObjects[objectIndex].mousePressed(cx, cy))
			objectIndex++;  // circle hit

		// sliders
		else if (hitObject.isSlider())
			hitObjects[objectIndex].mousePressed(cx, cy);
	}

	@Override
	public void mouseReleased(int button, int x, int y) {
		if (Options.isMouseDisabled())
			return;

		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		if (!isReplay && lastKeysPressed != ReplayFrame.KEY_NONE)
			addReplayFrame(x, y, ReplayFrame.KEY_NONE);
	}

	@Override
	public void keyReleased(int key, char c) {
		if (!isReplay && lastKeysPressed != ReplayFrame.KEY_NONE && (key == Options.getGameKeyLeft() || key == Options.getGameKeyRight()))
			addReplayFrame(input.getMouseX(), input.getMouseY(), ReplayFrame.KEY_NONE);
	}

	@Override
	public void mouseMoved(int oldx, int oldy, int newx, int newy) {
		if (!isReplay)
			addReplayFrame(newx, newy, lastKeysPressed);
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (!isReplay)
			addReplayFrame(newx, newy, lastKeysPressed);
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		if (Options.isMouseWheelDisabled() || Options.isMouseDisabled())
			return;

		UI.changeVolume((newValue < 0) ? -1 : 1);
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();

		if (osu == null || osu.objects == null)
			throw new RuntimeException("Running game with no OsuFile loaded.");

		// grab the mouse (not working for touchscreen)
//		container.setMouseGrabbed(true);

		// restart the game
		if (restart != Restart.FALSE) {
			if (restart == Restart.NEW) {
				// new game
				loadImages();
				setMapModifiers();
				retries = 0;
			} else if (restart == Restart.MANUAL) {
				// retry
				retries++;
			} else if (restart == Restart.REPLAY)
				retries = 0;

			// reset game data
			resetGameData();

			// needs to play before setting position to resume without lag later
			MusicController.play(false);
			MusicController.setPosition(0);
			MusicController.pause();

			// initialize object maps
			for (int i = 0; i < osu.objects.length; i++) {
				OsuHitObject hitObject = osu.objects[i];

				// is this the last note in the combo?
				boolean comboEnd = false;
				if (i + 1 < osu.objects.length && osu.objects[i + 1].isNewCombo())
					comboEnd = true;

				Color color = osu.combo[hitObject.getComboIndex()];
				if (hitObject.isCircle())
					hitObjects[i] = new Circle(hitObject, this, data, color, comboEnd);
				else if (hitObject.isSlider())
					hitObjects[i] = new Slider(hitObject, this, data, color, comboEnd);
				else if (hitObject.isSpinner())
					hitObjects[i] = new Spinner(hitObject, this, data);
			}

			// load the first timingPoint
			if (!osu.timingPoints.isEmpty()) {
				OsuTimingPoint timingPoint = osu.timingPoints.get(0);
				if (!timingPoint.isInherited()) {
					beatLengthBase = beatLength = timingPoint.getBeatLength();
					HitSound.setDefaultSampleSet(timingPoint.getSampleType());
					SoundController.setSampleVolume(timingPoint.getSampleVolume());
					timingPointIndex++;
				}
			}

			// unhide cursor for "auto" mod and replays
			if (GameMod.AUTO.isActive() || isReplay)
				UI.showCursor();

			// load replay frames
			if (isReplay) {
				// load mods
				previousMods = GameMod.getModState();
				GameMod.loadModState(replay.mods);

				// load initial data
				replayX = container.getWidth() / 2;
				replayY = container.getHeight() / 2;
				replayKeys = ReplayFrame.KEY_NONE;
				replayKeyPressed = false;
				replaySkipTime = -1;
				for (replayIndex = 0; replayIndex < replay.frames.length; replayIndex++) {
					ReplayFrame frame = replay.frames[replayIndex];
					if (frame.getY() < 0) {  // skip time (?)
						if (frame.getTime() > 0)
							replaySkipTime = frame.getTime();
					} else if (frame.getTime() == 0) {
						replayX = frame.getScaledX();
						replayY = frame.getScaledY();
						replayKeys = frame.getKeys();
						replayKeyPressed = frame.isKeyPressed();
					} else
						break;
				}

				// run frame updates in another thread
				killReplayThread();
				replayThread = new Thread() {
					@Override
					public void run() {
						while (replayThreadRunning) {
							// update frames
							int trackPosition = MusicController.getPosition();
							while (replayIndex < replay.frames.length && trackPosition >= replay.frames[replayIndex].getTime()) {
								ReplayFrame frame = replay.frames[replayIndex];
								replayX = frame.getScaledX();
								replayY = frame.getScaledY();
								replayKeyPressed = frame.isKeyPressed();
								int keys = frame.getKeys();
								if (replayKeyPressed && keys != replayKeys)  // send a key press
									gameKeyPressed(frame.getKeys(), replayX, replayY);
								replayKeys = keys;
								replayIndex++;
							}

							// out of frames
							if (replayIndex >= replay.frames.length)
								break;

							// sleep execution
							try {
								int diff = replay.frames[replayIndex].getTime() - trackPosition - 1;
								if (diff < 1)
									Thread.sleep(0, 256000);
								else
									Thread.sleep(diff);
							} catch (InterruptedException e) {}
						}
					}
				};
				if (!GameMod.AUTO.isActive()) {  // "auto" mod: ignore replay frames
					replayThreadRunning = true;
					replayThread.start();
				}
			}

			// initialize replay-recording structures
			else {
				lastReplayTime = 0;
				lastKeysPressed = ReplayFrame.KEY_NONE;
				replaySkipTime = -1;
				replayFrames = new LinkedList<ReplayFrame>();
				replayFrames.add(new ReplayFrame(0, 0, input.getMouseX(), input.getMouseY(), 0));
			}

			leadInTime = osu.audioLeadIn + approachTime;
			restart = Restart.FALSE;
		}

		skipButton.resetHover();
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
//		container.setMouseGrabbed(false);

		// re-hide cursor
		if (GameMod.AUTO.isActive() || isReplay)
			UI.hideCursor();

		// replays
		if (isReplay) {
			GameMod.loadModState(previousMods);
			killReplayThread();
		}
	}

	/**
	 * Draws hit objects, hit results, and follow points.
	 * @param g the graphics context
	 * @param trackPosition the track position
	 */
	private void drawHitObjects(Graphics g, int trackPosition) {
		// include previous object in follow points
		int lastObjectIndex = -1;
		if (objectIndex > 0 && objectIndex < osu.objects.length &&
		    trackPosition < osu.objects[objectIndex].getTime() && !osu.objects[objectIndex - 1].isSpinner())
			lastObjectIndex = objectIndex - 1;

		// draw hit objects in reverse order, or else overlapping objects are unreadable
		Stack<Integer> stack = new Stack<Integer>();
		for (int index = objectIndex; index < hitObjects.length && osu.objects[index].getTime() < trackPosition + approachTime; index++) {
			stack.add(index);

			// draw follow points
			if (!Options.isFollowPointEnabled())
				continue;
			if (osu.objects[index].isSpinner()) {
				lastObjectIndex = -1;
				continue;
			}
			if (lastObjectIndex != -1 && !osu.objects[index].isNewCombo()) {
				// calculate points
				final int followPointInterval = container.getHeight() / 14;
				int lastObjectEndTime = hitObjects[lastObjectIndex].getEndTime() + 1;
				int objectStartTime = osu.objects[index].getTime();
				float[] startXY = hitObjects[lastObjectIndex].getPointAt(lastObjectEndTime);
				float[] endXY = hitObjects[index].getPointAt(objectStartTime);
				float xDiff = endXY[0] - startXY[0];
				float yDiff = endXY[1] - startXY[1];
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
						float x = startXY[0] + xDiff * t;
						float y = startXY[1] + yDiff * t;
						float nextT = t + step;
						if (lastObjectIndex < objectIndex) {  // fade the previous trail
							if (progress < nextT) {
								if (progress > t)
									followPoint.setAlpha(1f - ((progress - t + step) / (step * 2f)));
								else if (progress > t - step)
									followPoint.setAlpha(1f - ((progress - (t - step)) / (step * 2f)));
								else
									followPoint.setAlpha(1f);
								followPoint.draw(x, y);
							}
						} else
							followPoint.draw(x, y);
						t = nextT;
					}
					followPoint.setAlpha(1f);
				}
			}
			lastObjectIndex = index;
		}

		while (!stack.isEmpty())
			hitObjects[stack.pop()].draw(g, trackPosition);

		// draw OsuHitObjectResult objects
		data.drawHitResults(trackPosition);
	}

	/**
	 * Loads all required data from an OsuFile.
	 * @param osu the OsuFile to load
	 */
	public void loadOsuFile(OsuFile osu) {
		this.osu = osu;
		Display.setTitle(String.format("%s - %s", game.getTitle(), osu.toString()));
		if (osu.timingPoints == null || osu.combo == null)
			OsuDB.load(osu, OsuDB.LOAD_ARRAY);
		OsuParser.parseHitObjects(osu);
		HitSound.setDefaultSampleSet(osu.sampleSet);
	}

	/**
	 * Resets all game data and structures.
	 */
	public void resetGameData() {
		hitObjects = new HitObject[osu.objects.length];
		data.clear();
		objectIndex = 0;
		breakIndex = 0;
		breakTime = 0;
		breakSound = false;
		timingPointIndex = 0;
		beatLengthBase = beatLength = 1;
		pauseTime = -1;
		pausedMouseX = -1;
		pausedMouseY = -1;
		countdownReadySound = false;
		countdown3Sound = false;
		countdown1Sound = false;
		countdown2Sound = false;
		countdownGoSound = false;
		checkpointLoaded = false;
		deaths = 0;
		deathTime = -1;
		replayFrames = null;
		autoMouseX = 0;
		autoMouseY = 0;
		autoMousePressed = false;

		System.gc();
	}

	/**
	 * Skips the beginning of a track.
	 * @return true if skipped, false otherwise
	 */
	private synchronized boolean skipIntro() {
		int firstObjectTime = osu.objects[0].getTime();
		int trackPosition = MusicController.getPosition();
		if (objectIndex == 0 && trackPosition < firstObjectTime - SKIP_OFFSET) {
			if (isLeadIn()) {
				leadInTime = 0;
				MusicController.resume();
			}
			MusicController.setPosition(firstObjectTime - SKIP_OFFSET);
			replaySkipTime = (isReplay) ? -1 : trackPosition;
			if (replayThread != null && replayThread.isAlive()) {
				replayX = (int) skipButton.getX();
				replayY = (int) skipButton.getY();
				replayThread.interrupt();
			}
			SoundController.playSound(SoundEffect.MENUHIT);
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
		File parent = osu.getFile().getParentFile();
		for (GameImage img : GameImage.values()) {
			if (img.isSkinnable()) {
				img.setDefaultImage();
				img.setSkinImage(parent);
			}
		}

		// skip button
		if (GameImage.SKIP.getImages() != null) {
			Animation skip = GameImage.SKIP.getAnimation(120);
			skipButton = new MenuButton(skip, width - skip.getWidth() / 2f, height - (skip.getHeight() / 2f));
		} else {
			Image skip = GameImage.SKIP.getImage();
			skipButton = new MenuButton(skip, width - skip.getWidth() / 2f, height - (skip.getHeight() / 2f));
		}
		skipButton.setHoverExpand(1.1f, MenuButton.Expand.UP_LEFT);

		// load other images...
		((GamePauseMenu) game.getState(Opsu.STATE_GAMEPAUSEMENU)).loadImages();
		data.loadImages();
	}

	/**
	 * Set map modifiers.
	 */
	private void setMapModifiers() {
		// map-based properties, re-initialized each game
		float circleSize = osu.circleSize;
		float approachRate = osu.approachRate;
		float overallDifficulty = osu.overallDifficulty;
		float HPDrainRate = osu.HPDrainRate;

		// "Hard Rock" modifiers
		if (GameMod.HARD_ROCK.isActive()) {
			circleSize = Math.min(circleSize * 1.4f, 10);
			approachRate = Math.min(approachRate * 1.4f, 10);
			overallDifficulty = Math.min(overallDifficulty * 1.4f, 10);
			HPDrainRate = Math.min(HPDrainRate * 1.4f, 10);
		}

		// "Easy" modifiers
		else if (GameMod.EASY.isActive()) {
			circleSize /= 2f;
			approachRate /= 2f;
			overallDifficulty /= 2f;
			HPDrainRate /= 2f;
		}

		// fixed difficulty overrides
		if (Options.getFixedCS() > 0f)
			circleSize = Options.getFixedCS();
		if (Options.getFixedAR() > 0f)
			approachRate = Options.getFixedAR();
		if (Options.getFixedOD() > 0f)
			overallDifficulty = Options.getFixedOD();
		if (Options.getFixedHP() > 0f)
			HPDrainRate = Options.getFixedHP();

		// initialize objects
		Circle.init(container, circleSize);
		Slider.init(container, circleSize, osu);
		Spinner.init(container);

		// approachRate (hit object approach time)
		if (approachRate < 5)
			approachTime = (int) (1800 - (approachRate * 120));
		else
			approachTime = (int) (1200 - ((approachRate - 5) * 150));

		// overallDifficulty (hit result time offsets)
		hitResultOffset = new int[GameData.HIT_MAX];
		hitResultOffset[GameData.HIT_300]  = (int) (78 - (overallDifficulty * 6));
		hitResultOffset[GameData.HIT_100]  = (int) (138 - (overallDifficulty * 8));
		hitResultOffset[GameData.HIT_50]   = (int) (198 - (overallDifficulty * 10));
		hitResultOffset[GameData.HIT_MISS] = (int) (500 - (overallDifficulty * 10));

		// HPDrainRate (health change), overallDifficulty (scoring)
		data.setDrainRate(HPDrainRate);
		data.setDifficulty(overallDifficulty);
		data.setHitResultOffset(hitResultOffset);
	}

	/**
	 * Sets/returns whether entering the state will restart it.
	 */
	public void setRestart(Restart restart) { this.restart = restart; }
	public Restart getRestart() { return restart; }

	/**
	 * Returns whether or not the track is in the lead-in time state.
	 */
	public boolean isLeadIn() { return leadInTime > 0; }

	/**
	 * Returns the object approach time, in milliseconds.
	 */
	public int getApproachTime() { return approachTime; }

	/**
	 * Returns an array of hit result offset times, in milliseconds (indexed by GameData.HIT_* constants).
	 */
	public int[] getHitResultOffsets() { return hitResultOffset; }

	/**
	 * Returns the beat length.
	 */
	public float getBeatLength() { return beatLength; }

	/**
	 * Returns the slider multiplier given by the current timing point.
	 */
	public float getTimingPointMultiplier() { return beatLength / beatLengthBase; }

	/**
	 * Kills the running replay updating thread, if any.
	 */
	private void killReplayThread() {
		if (replayThread != null && replayThread.isAlive()) {
			replayThreadRunning = false;
			replayThread.interrupt();
		}
		replayThread = null;
	}

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
	 * Adds a replay frame to the list.
	 * @param x the cursor x coordinate
	 * @param y the cursor y coordinate
	 * @param keys the keys pressed
	 */
	private void addReplayFrame(int x, int y, int keys) {
		int time = MusicController.getPosition();
		int timeDiff = time - lastReplayTime;
		lastReplayTime = time;
		lastKeysPressed = keys;
		int cx = (int) ((x - OsuHitObject.getXOffset()) / OsuHitObject.getXMultiplier());
		int cy = (int) ((y - OsuHitObject.getYOffset()) / OsuHitObject.getYMultiplier());
		replayFrames.add(new ReplayFrame(timeDiff, time, cx, cy, lastKeysPressed));
	}

	/**
	 * Returns the point at the t value between a start and end point.
	 * @param startX the starting x coordinate
	 * @param startY the starting y coordinate
	 * @param endX the ending x coordinate
	 * @param endY the ending y coordinate
	 * @param t the t value [0, 1]
	 * @return the [x,y] coordinates
	 */
	private float[] getPointAt(float startX, float startY, float endX, float endY, float t) {
		// "autopilot" mod: move quicker between objects
		if (GameMod.AUTOPILOT.isActive())
			t = Utils.clamp(t * 2f, 0f, 1f);

		float[] xy = new float[2];
		xy[0] = startX + (endX - startX) * t;
		xy[1] = startY + (endY - startY) * t;
		return xy;
	}
}
