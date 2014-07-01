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
import itdelatrisu.opsu.GameScore;
import itdelatrisu.opsu.MusicController;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.OsuTimingPoint;
import itdelatrisu.opsu.SoundController;
import itdelatrisu.opsu.objects.Circle;
import itdelatrisu.opsu.objects.Slider;
import itdelatrisu.opsu.objects.Spinner;

import java.util.HashMap;
import java.util.Stack;

import org.lwjgl.input.Keyboard;
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
import org.newdawn.slick.util.Log;

/**
 * "Game" state.
 */
public class Game extends BasicGameState {
	/**
	 * Game restart states.
	 */
	public static final byte
		RESTART_FALSE   = 0,
		RESTART_NEW     = 1,   // first time loading song
		RESTART_MANUAL  = 2,   // retry
		RESTART_LOSE    = 3;   // health is zero: no-continue/force restart

	/**
	 * Current restart state.
	 */
	private static byte restart;

	/**
	 * The associated OsuFile object.
	 */
	private static OsuFile osu;

	/**
	 * The associated GameScore object (holds all score data).
	 */
	private static GameScore score;

	/**
	 * Current hit object index in OsuHitObject[] array.
	 */
	private int objectIndex = 0;

	/**
	 * This map's hit circles objects, keyed by objectIndex.
	 */
	private HashMap<Integer, Circle> circles;

	/**
	 * This map's slider objects, keyed by objectIndex.
	 */
	private HashMap<Integer, Slider> sliders;

	/**
	 * This map's spinner objects, keyed by objectIndex.
	 */
	private HashMap<Integer, Spinner> spinners;

	/**
	 * Delay time, in milliseconds, before song starts.
	 */
	private static int leadInTime;

	/**
	 * Hit object approach time, in milliseconds.
	 */
	private int approachTime;

	/**
	 * Time offsets for obtaining each hit result (indexed by HIT_* constants).
	 */
	private int[] hitResultOffset;

	/**
	 * Time, in milliseconds, between the first and last hit object.
	 */
	private int mapLength;

	/**
	 * Current break index in breaks ArrayList.
	 */
	private int breakIndex;

	/**
	 * Warning arrows, pointing right and left.
	 */
	private Image warningArrowR, warningArrowL;

	/**
	 * Section pass and fail images (displayed at start of break, when necessary).
	 */
	private Image breakStartPass, breakStartFail;

	/**
	 * Break start time (0 if not in break).
	 */
	private int breakTime = 0;

	/**
	 * Whether the break sound has been played.
	 */
	private boolean breakSound;

	/**
	 * Skip button (displayed at song start, when necessary).
	 */
	private GUIMenuButton skipButton;

	/**
	 * Minimum time before start of song, in milliseconds, to process skip-related actions.
	 */
	private final int skipOffsetTime = 2000;

	/**
	 * Current timing point index in timingPoints ArrayList.
	 */
	private int timingPointIndex;

	/**
	 * Current beat lengths (base value and inherited value).
	 */
	private float beatLengthBase, beatLength;

	/**
	 * Countdown-related images.
	 */
	private Image
		countdownReady,         // "READY?" text
		countdown3,             // "3" text
		countdown1,             // "2" text
		countdown2,             // "1" text
		countdownGo;            // "GO!" text

	/**
	 * Whether the countdown sound has been played.
	 */
	private boolean
		countdownReadySound, countdown3Sound, countdown1Sound,
		countdown2Sound, countdownGoSound;

	/**
	 * Glowing hit circle outline which must be clicked when returning from pause menu.
	 */
	private Image hitCircleSelect;

	/**
	 * Mouse coordinates before game paused.
	 */
	private int pausedMouseX = -1, pausedMouseY = -1;

	/**
	 * Track position when game paused.
	 */
	private int pauseTime = -1;

	/**
	 * Value for handling hitCircleSelect pulse effect (expanding, alpha level).
	 */
	private float pausePulse;

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

		// spinners have fixed properties, and only need to be initialized once
		Spinner.init(container);

		// breaks
		breakStartPass = new Image("section-pass.png");
		breakStartFail = new Image("section-fail.png");
		warningArrowR  = new Image("play-warningarrow.png");
		warningArrowL  = warningArrowR.getFlippedCopy(true, false);

		// skip button
		Image skip = new Image("play-skip.png");
		float skipScale = (height * 0.1f) / skip.getHeight();
		skip = skip.getScaledCopy(skipScale);
		skipButton = new GUIMenuButton(skip,
				width - (skip.getWidth() / 2f),
				height - (skip.getHeight() / 2f));

		// countdown
		float countdownHeight = height / 3f;
		countdownReady = new Image("ready.png");
		countdownReady = countdownReady.getScaledCopy(countdownHeight / countdownReady.getHeight());
		countdown3 = new Image("count3.png");
		countdown3 = countdown3.getScaledCopy(countdownHeight / countdown3.getHeight());
		countdown2 = new Image("count2.png");
		countdown2 = countdown2.getScaledCopy(countdownHeight / countdown2.getHeight());
		countdown1 = new Image("count1.png");
		countdown1 = countdown1.getScaledCopy(countdownHeight / countdown1.getHeight());
		countdownGo = new Image("go.png");
		countdownGo = countdownGo.getScaledCopy(countdownHeight / countdownGo.getHeight());

		// hit circle select
		hitCircleSelect = new Image("hitcircleselect.png");

		// create the associated GameScore object
		score = new GameScore(width, height);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();

		// background
		if (!osu.drawBG(width, height, 0.7f))
			g.setBackground(Color.black);

		Options.drawFPS();

		int trackPosition = MusicController.getPosition();
		if (pauseTime > -1)  // returning from pause screen
			trackPosition = pauseTime;

		// break periods
		if (osu.breaks != null && breakIndex < osu.breaks.size()) {
			if (breakTime > 0) {
				int endTime = osu.breaks.get(breakIndex);
				int breakLength = endTime - breakTime;

				// letterbox effect (black bars on top/bottom)
				if (osu.letterboxInBreaks && breakLength >= 4000) {
					g.setColor(Color.black);
					g.fillRect(0, 0, width, height * 0.125f);
					g.fillRect(0, height * 0.875f, width, height * 0.125f);
				}

				score.drawGameElements(g, mapLength, true, objectIndex == 0);

				if (breakLength >= 8000 &&
					trackPosition - breakTime > 2000 &&
					trackPosition - breakTime < 5000) {
					// show break start
					if (score.getHealth() >= 50) {
						breakStartPass.drawCentered(width / 2f, height / 2f);
						if (!breakSound) {
							SoundController.playSound(SoundController.SOUND_SECTIONPASS);
							breakSound = true;
						}
					} else {
						breakStartFail.drawCentered(width / 2f, height / 2f);
						if (!breakSound) {
							SoundController.playSound(SoundController.SOUND_SECTIONFAIL);
							breakSound = true;
						}
					}
				} else if (breakLength >= 4000) {
					// show break end (flash twice for 500ms)
					int endTimeDiff = endTime - trackPosition;
					if ((endTimeDiff > 1500 && endTimeDiff < 2000) ||
						(endTimeDiff > 500 && endTimeDiff < 1000)) {
						warningArrowR.draw(width * 0.15f, height * 0.15f);
						warningArrowR.draw(width * 0.15f, height * 0.75f);
						warningArrowL.draw(width * 0.75f, height * 0.15f);
						warningArrowL.draw(width * 0.75f, height * 0.75f);
					}
				}
				return;
			}
		}

		// game elements
		score.drawGameElements(g, mapLength, false, objectIndex == 0);

		// first object...
		if (objectIndex == 0) {
			// skip beginning
			if (osu.objects[objectIndex].time - skipOffsetTime > 5000 &&
				trackPosition < osu.objects[objectIndex].time - skipOffsetTime)
				skipButton.draw();
			
			// mod icons
			if (trackPosition < osu.objects[objectIndex].time) {
				for (int i = Options.MOD_MAX - 1; i >= 0; i--) {
					if (Options.isModActive(i)) {
						Image modImage = Options.getModImage(i);
						modImage.draw(
								(width * 0.85f) + ((i - (Options.MOD_MAX / 2)) * modImage.getWidth() / 3f),
								height / 10f
						);
					}
				}
			}
		}

		if (isLeadIn())
			trackPosition = leadInTime * -1;  // render approach circles during song lead-in

		// countdown
		if (osu.countdown > 0) {  // TODO: implement half/double rate settings
			int timeDiff = osu.objects[0].time - trackPosition;
			if (timeDiff >= 500 && timeDiff < 3000) {
				if (timeDiff >= 1500) {
					countdownReady.drawCentered(width / 2, height / 2);
					if (!countdownReadySound) {
						SoundController.playSound(SoundController.SOUND_READY);
						countdownReadySound = true;
					}
				}
				if (timeDiff < 2000) {
					countdown3.draw(0, 0);
					if (!countdown3Sound) {
						SoundController.playSound(SoundController.SOUND_COUNT3);
						countdown3Sound = true;
					}
				}
				if (timeDiff < 1500) {
					countdown2.draw(width - countdown2.getWidth(), 0);
					if (!countdown2Sound) {
						SoundController.playSound(SoundController.SOUND_COUNT2);
						countdown2Sound = true;
					}
				}
				if (timeDiff < 1000) {
					countdown1.drawCentered(width / 2, height / 2);
					if (!countdown1Sound) {
						SoundController.playSound(SoundController.SOUND_COUNT1);
						countdown1Sound = true;
					}
				}
			} else if (timeDiff >= -500 && timeDiff < 500) {
				countdownGo.setAlpha((timeDiff < 0) ? 1 - (timeDiff / -1000f) : 1);
				countdownGo.drawCentered(width / 2, height / 2);
				if (!countdownGoSound) {
					SoundController.playSound(SoundController.SOUND_GO);
					countdownGoSound = true;
				}
			}
		}

		// draw hit objects in reverse order, or else overlapping objects are unreadable
		Stack<Integer> stack = new Stack<Integer>();
		for (int i = objectIndex; i < osu.objects.length && osu.objects[i].time < trackPosition + approachTime; i++)
			stack.add(i);

		while (!stack.isEmpty()) {
			int i = stack.pop();
			OsuHitObject hitObject = osu.objects[i];

			if ((hitObject.type & OsuHitObject.TYPE_CIRCLE) > 0)
				circles.get(i).draw(trackPosition);
			else if ((hitObject.type & OsuHitObject.TYPE_SLIDER) > 0)
				sliders.get(i).draw(trackPosition, stack.isEmpty());
			else if ((hitObject.type & OsuHitObject.TYPE_SPINNER) > 0) {
				if (stack.isEmpty())  // only draw spinner at objectIndex
					spinners.get(i).draw(trackPosition, g);
				else
					continue;
			}
		}

		// draw OsuHitObjectResult objects
		score.drawHitResults(trackPosition);

		// returning from pause screen
		if (pauseTime > -1 && pausedMouseX > -1 && pausedMouseY > -1) {
			// darken the screen
			g.setColor(Options.COLOR_BLACK_ALPHA);
			g.fillRect(0, 0, width, height);

			// draw glowing hit select circle and pulse effect
			int circleRadius = Circle.getHitCircle().getWidth();
			Image cursorCircle = hitCircleSelect.getScaledCopy(circleRadius, circleRadius);
			cursorCircle.setAlpha(1.0f);
			cursorCircle.drawCentered(pausedMouseX, pausedMouseY);
			Image cursorCirclePulse = cursorCircle.getScaledCopy(1f + pausePulse);
			cursorCirclePulse.setAlpha(1f - pausePulse);
			cursorCirclePulse.drawCentered(pausedMouseX, pausedMouseY);
		}
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		if (isLeadIn()) {  // stop updating during song lead-in
			leadInTime -= delta;
			if (!isLeadIn())
				MusicController.playAt(0, false);
			return;
		}

		// returning from pause screen: must click previous mouse position
		if (pauseTime > -1) {
			// paused during lead-in or break: continue immediately
			if (pausedMouseX < 0 && pausedMouseY < 0) {
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

		// map complete!
		if (objectIndex >= osu.objects.length) {
			game.enterState(Opsu.STATE_GAMERANKING, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			return;
		}

		int trackPosition = MusicController.getPosition();

		// timing points
		if (timingPointIndex < osu.timingPoints.size()) {
			OsuTimingPoint timingPoint = osu.timingPoints.get(timingPointIndex);
			if (trackPosition >= timingPoint.time) {
				if (timingPoint.velocity >= 0)
					beatLengthBase = beatLength = timingPoint.beatLength;
				else
					beatLength = beatLengthBase * (timingPoint.velocity / -100f);
				SoundController.setSampleSet(timingPoint.sampleType);
				SoundController.setSampleVolume(timingPoint.sampleVolume);
				timingPointIndex++;
			}
		}

		// song beginning
		if (objectIndex == 0) {
			if (trackPosition < osu.objects[0].time)
				return;  // nothing to do here
		}

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
		if (!container.hasFocus() && !Options.isModActive(Options.MOD_AUTO)) {
			if (pauseTime < 0) {
				pausedMouseX = input.getMouseX();
				pausedMouseY = input.getMouseY();
				pausePulse = 0f;
			}
			if (MusicController.isPlaying() || isLeadIn())
				pauseTime = trackPosition;
			game.enterState(Opsu.STATE_GAMEPAUSEMENU);
		}

		// drain health
		score.changeHealth(delta / -200f);
		if (!score.isAlive()) {
			// game over, force a restart
			restart = RESTART_LOSE;
			game.enterState(Opsu.STATE_GAMEPAUSEMENU);
		}

		score.updateComboBurst(delta);

		// update objects (loop in unlikely event of any skipped indexes)
		while (objectIndex < osu.objects.length && trackPosition > osu.objects[objectIndex].time) {
			OsuHitObject hitObject = osu.objects[objectIndex];

			// check if we've already passed the next object's start time
			boolean overlap = (objectIndex + 1 < osu.objects.length &&
					trackPosition > osu.objects[objectIndex + 1].time - hitResultOffset[GameScore.HIT_300]);

			// check completion status of the hit object
			boolean done = false;
			if ((hitObject.type & OsuHitObject.TYPE_CIRCLE) > 0)
				done = circles.get(objectIndex).update(overlap);
			else if ((hitObject.type & OsuHitObject.TYPE_SLIDER) > 0)
				done = sliders.get(objectIndex).update(overlap, delta, input.getMouseX(), input.getMouseY());
			else if ((hitObject.type & OsuHitObject.TYPE_SPINNER) > 0)
				done = spinners.get(objectIndex).update(overlap, delta, input.getMouseX(), input.getMouseY());

			// increment object index?
			if (done)
				objectIndex++;
			else
				break;
		}
	}

	@Override
	public int getID() { return state; }

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
			// pause game
			int trackPosition = MusicController.getPosition();
			if (pauseTime < 0 && breakTime <= 0 && trackPosition >= osu.objects[0].time) {
				pausedMouseX = input.getMouseX();
				pausedMouseY = input.getMouseY();
				pausePulse = 0f;
			}
			if (MusicController.isPlaying() || isLeadIn())
				pauseTime = trackPosition;
			game.enterState(Opsu.STATE_GAMEPAUSEMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			break;
		case Input.KEY_SPACE:
			// skip
			skipIntro();
			break;
		case Input.KEY_Z:
			// left-click
			if (!Keyboard.isRepeatEvent())
				mousePressed(Input.MOUSE_LEFT_BUTTON, input.getMouseX(), input.getMouseY());
			break;
		case Input.KEY_X:
			// right-click
			if (!Keyboard.isRepeatEvent())
				mousePressed(Input.MOUSE_RIGHT_BUTTON, input.getMouseX(), input.getMouseY());
			break;
		case Input.KEY_F12:
			Options.takeScreenShot();
			break;
		}
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// returning from pause screen
		if (pauseTime > -1) {
			double distance = Math.hypot(pausedMouseX - x, pausedMouseY - y);
			int circleRadius = Circle.getHitCircle().getWidth() / 2;
			if (distance < circleRadius) {
				// unpause the game
				pauseTime = -1;
				pausedMouseX = -1;
				pausedMouseY = -1;
				if (!Game.isLeadIn())
					MusicController.resume();
			}
			return;
		}

		if (objectIndex >= osu.objects.length)  // nothing left to do here
			return;

		OsuHitObject hitObject = osu.objects[objectIndex];

		// skip beginning
		if (skipButton.contains(x, y)) {
			if (skipIntro())
				return;  // successfully skipped
		}

		// "auto" mod: ignore user actions
		if (Options.isModActive(Options.MOD_AUTO))
			return;

		// circles
		if ((hitObject.type & OsuHitObject.TYPE_CIRCLE) > 0) {
			boolean hit = circles.get(objectIndex).mousePressed(x, y);
			if (hit)
				objectIndex++;
		}

		// sliders
		else if ((hitObject.type & OsuHitObject.TYPE_SLIDER) > 0)
			sliders.get(objectIndex).mousePressed(x, y);
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		if (osu == null || osu.objects == null)
			throw new RuntimeException("Running game with no OsuFile loaded.");

		// restart the game
		if (restart != RESTART_FALSE) {
			// new game
			if (restart == RESTART_NEW) {
				setMapModifiers();
				
				// calculate map length (TODO: end on slider?)
				OsuHitObject lastObject = osu.objects[osu.objects.length - 1];
				int endTime;
				if ((lastObject.type & OsuHitObject.TYPE_SPINNER) > 0)
					endTime = lastObject.endTime;
				else
					endTime = lastObject.time;
				mapLength = endTime - osu.objects[0].time;
			}

			// initialize object maps
			circles = new HashMap<Integer, Circle>();
			sliders = new HashMap<Integer, Slider>();
			spinners = new HashMap<Integer, Spinner>();

			for (int i = 0; i < osu.objects.length; i++) {
				OsuHitObject hitObject = osu.objects[i];

				// is this the last note in the combo?
				boolean comboEnd = false;
				if (i + 1 < osu.objects.length &&
					(osu.objects[i + 1].type & OsuHitObject.TYPE_NEWCOMBO) > 0)
					comboEnd = true;

				if ((hitObject.type & OsuHitObject.TYPE_CIRCLE) > 0) {
					circles.put(i, new Circle(hitObject, this, score, osu.combo[hitObject.comboIndex], comboEnd));
				} else if ((hitObject.type & OsuHitObject.TYPE_SLIDER) > 0) {
					sliders.put(i, new Slider(hitObject, this, score, osu.combo[hitObject.comboIndex], comboEnd));
				} else if ((hitObject.type & OsuHitObject.TYPE_SPINNER) > 0) {
					spinners.put(i, new Spinner(hitObject, this, score));
				}
			}

			// reset data
			MusicController.setPosition(0);
			MusicController.pause();
			score.clear();
			objectIndex = 0;
			breakIndex = 0;
			breakTime = 0;
			breakSound = false;
			timingPointIndex = 0;
			pauseTime = -1;
			pausedMouseX = -1;
			pausedMouseY = -1;
			countdownReadySound = false;
			countdown3Sound = false;
			countdown1Sound = false;
			countdown2Sound = false;
			countdownGoSound = false;

			// load the first timingPoint
			if (!osu.timingPoints.isEmpty()) {
				OsuTimingPoint timingPoint = osu.timingPoints.get(0);
				if (timingPoint.velocity >= 0) {
					beatLengthBase = beatLength = timingPoint.beatLength;
					SoundController.setSampleSet(timingPoint.sampleType);
					SoundController.setSampleVolume(timingPoint.sampleVolume);
					timingPointIndex++;
				}
			}

			leadInTime = osu.audioLeadIn + approachTime;
			restart = RESTART_FALSE;
		}
	}

	/**
	 * Skips the beginning of a track.
	 * @return true if skipped, false otherwise
	 */
	private boolean skipIntro() {
		int trackPosition = MusicController.getPosition();
		if (objectIndex == 0 &&
			osu.objects[0].time - skipOffsetTime > 4000 &&
			trackPosition < osu.objects[0].time - skipOffsetTime) {
			if (isLeadIn()) {
				leadInTime = 0;
				MusicController.resume();
			}
			MusicController.setPosition(osu.objects[0].time - skipOffsetTime);
			SoundController.playSound(SoundController.SOUND_MENUHIT);
			return true;
		}
		return false;
	}

	/**
	 * Returns true if an input key is pressed (mouse left/right, keyboard Z/X).
	 */
	public boolean isInputKeyPressed() {
		return (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON) ||
				input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON) ||
				input.isKeyDown(Input.KEY_Z) || input.isKeyDown(Input.KEY_X));
	}

	/**
	 * Set map modifiers.
	 */
	private void setMapModifiers() {
		try {
			// map-based properties, so re-initialize each game
			float circleSize = osu.circleSize;
			float approachRate = osu.approachRate;
			float overallDifficulty = osu.overallDifficulty;
			float HPDrainRate = osu.HPDrainRate;
			if (Options.isModActive(Options.MOD_HARD_ROCK)) {  // hard rock modifiers
				circleSize = Math.max(circleSize - 1, 0);
				approachRate = Math.min(approachRate + 3, 10);
				overallDifficulty = Math.min(overallDifficulty + 3, 10);
				HPDrainRate = Math.min(HPDrainRate + 3, 10);
			}

			Circle.init(container, circleSize);
			Slider.init(container, circleSize, osu);

			// approachRate (hit object approach time)
			if (approachRate < 5)
				approachTime = (int) (1800 - (approachRate * 120));
			else
				approachTime = (int) (1200 - ((approachRate - 5) * 150));

			// overallDifficulty (hit result time offsets)
			hitResultOffset = new int[GameScore.HIT_MAX];
			hitResultOffset[GameScore.HIT_300]  = (int) (78 - (overallDifficulty * 6));
			hitResultOffset[GameScore.HIT_100]  = (int) (138 - (overallDifficulty * 8));
			hitResultOffset[GameScore.HIT_50]   = (int) (198 - (overallDifficulty * 10));
			hitResultOffset[GameScore.HIT_MISS] = (int) (500 - (overallDifficulty * 10));

			// HPDrainRate (health change), overallDifficulty (scoring)
			score.setDrainRate(HPDrainRate);
			score.setDifficulty(overallDifficulty);
		} catch (SlickException e) {
			Log.error("Error while setting map modifiers.", e);
		}
	}

	/**
	 * Sets/returns whether entering the state will restart it.
	 */
	public static void setRestart(byte restart) { Game.restart = restart; }
	public static byte getRestart() { return Game.restart; }

	/**
	 * Sets or returns the associated OsuFile.
	 */
	public static void setOsuFile(OsuFile osu) { Game.osu = osu; }
	public static OsuFile getOsuFile() { return osu; }

	/**
	 * Returns the associated GameScore object.
	 */
	public static GameScore getGameScore() { return score; }

	/**
	 * Returns whether or not the track is in the lead-in time state.
	 */
	public static boolean isLeadIn() { return leadInTime > 0; }

	/**
	 * Returns the object approach time, in milliseconds.
	 */
	public int getApproachTime() { return approachTime; }
	
	/**
	 * Returns an array of hit result offset times, in milliseconds (indexed by GameScore.HIT_* constants).
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
}
