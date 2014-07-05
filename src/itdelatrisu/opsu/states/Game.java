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
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameScore;
import itdelatrisu.opsu.MusicController;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.OsuTimingPoint;
import itdelatrisu.opsu.SoundController;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.Circle;
import itdelatrisu.opsu.objects.Slider;
import itdelatrisu.opsu.objects.Spinner;

import java.io.File;
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
	 * Whether the countdown sound has been played.
	 */
	private boolean
		countdownReadySound, countdown3Sound, countdown1Sound,
		countdown2Sound, countdownGoSound;

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

	/**
	 * Default playfield background (optional).
	 * Overridden by song background unless "ForceDefaultPlayfield" option enabled.
	 */
	private Image playfield;

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

		// create the associated GameScore object
		score = new GameScore(width, height);

		// playfield background
		try {
			playfield = new Image("playfield.png").getScaledCopy(width, height);
		} catch (Exception e) {
			// optional
		}
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();

		// background
		g.setBackground(Color.black);
		float dimLevel = Options.getBackgroundDim();
		if (Options.isDefaultPlayfieldForced() && playfield != null) {
			playfield.setAlpha(dimLevel);
			playfield.draw();
		} else if (!osu.drawBG(width, height, dimLevel) && playfield != null) {
			playfield.setAlpha(dimLevel);
			playfield.draw();
		}

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
						GameImage.SECTION_PASS.getImage().drawCentered(width / 2f, height / 2f);
						if (!breakSound) {
							SoundController.playSound(SoundController.SOUND_SECTIONPASS);
							breakSound = true;
						}
					} else {
						GameImage.SECTION_FAIL.getImage().drawCentered(width / 2f, height / 2f);
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
						Image arrow = GameImage.WARNINGARROW.getImage();
						arrow.setRotation(0);
						arrow.draw(width * 0.15f, height * 0.15f);
						arrow.draw(width * 0.15f, height * 0.75f);
						arrow.setRotation(180);
						arrow.draw(width * 0.75f, height * 0.15f);
						arrow.draw(width * 0.75f, height * 0.75f);
					}
				}

				if (Options.isModActive(Options.MOD_AUTO))
					GameImage.UNRANKED.getImage().drawCentered(width / 2, height * 0.077f);
				Utils.drawFPS();
				Utils.drawCursor();
				return;
			}
		}

		// game elements
		score.drawGameElements(g, mapLength, false, objectIndex == 0);

		// skip beginning
		if (objectIndex == 0 &&
			osu.objects[0].time - skipOffsetTime > 5000 &&
			trackPosition < osu.objects[0].time - skipOffsetTime)
			skipButton.draw();

		// mod icons
		if ((objectIndex == 0 && trackPosition < osu.objects[0].time) ||
			Options.isModActive(Options.MOD_AUTO)) {
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

		if (isLeadIn())
			trackPosition = leadInTime * -1;  // render approach circles during song lead-in

		// countdown
		if (osu.countdown > 0) {  // TODO: implement half/double rate settings
			int timeDiff = osu.objects[0].time - trackPosition;
			if (timeDiff >= 500 && timeDiff < 3000) {
				if (timeDiff >= 1500) {
					GameImage.COUNTDOWN_READY.getImage().drawCentered(width / 2, height / 2);
					if (!countdownReadySound) {
						SoundController.playSound(SoundController.SOUND_READY);
						countdownReadySound = true;
					}
				}
				if (timeDiff < 2000) {
					GameImage.COUNTDOWN_3.getImage().draw(0, 0);
					if (!countdown3Sound) {
						SoundController.playSound(SoundController.SOUND_COUNT3);
						countdown3Sound = true;
					}
				}
				if (timeDiff < 1500) {
					GameImage.COUNTDOWN_2.getImage().draw(width - GameImage.COUNTDOWN_2.getImage().getWidth(), 0);
					if (!countdown2Sound) {
						SoundController.playSound(SoundController.SOUND_COUNT2);
						countdown2Sound = true;
					}
				}
				if (timeDiff < 1000) {
					GameImage.COUNTDOWN_1.getImage().drawCentered(width / 2, height / 2);
					if (!countdown1Sound) {
						SoundController.playSound(SoundController.SOUND_COUNT1);
						countdown1Sound = true;
					}
				}
			} else if (timeDiff >= -500 && timeDiff < 500) {
				Image go = GameImage.COUNTDOWN_GO.getImage();
				go.setAlpha((timeDiff < 0) ? 1 - (timeDiff / -1000f) : 1);
				go.drawCentered(width / 2, height / 2);
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

		if (Options.isModActive(Options.MOD_AUTO))
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

		Utils.drawFPS();
		Utils.drawCursor();
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

		score.updateScoreDisplay(delta);

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

		score.updateComboBurst(delta);

		// drain health
		score.changeHealth(delta / -200f);
		if (!score.isAlive()) {
			// game over, force a restart
			restart = RESTART_LOSE;
			game.enterState(Opsu.STATE_GAMEPAUSEMENU);
		}

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
			if (pauseTime < 0 && breakTime <= 0 &&
				trackPosition >= osu.objects[0].time &&
				!Options.isModActive(Options.MOD_AUTO)) {
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
			Utils.takeScreenShot();
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
			int circleRadius = GameImage.HITCIRCLE.getImage().getWidth() / 2;
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
		if (restart == RESTART_NEW)
			osu = MusicController.getOsuFile();

		if (osu == null || osu.objects == null)
			throw new RuntimeException("Running game with no OsuFile loaded.");

		// grab the mouse (not working for touchscreen)
//		container.setMouseGrabbed(true);

		// restart the game
		if (restart != RESTART_FALSE) {
			// new game
			if (restart == RESTART_NEW) {
				loadImages();
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

//	@Override
//	public void leave(GameContainer container, StateBasedGame game)
//			throws SlickException {
//		container.setMouseGrabbed(false);
//	}

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
	 * Loads all game images.
	 * @throws SlickException
	 */
	private void loadImages() throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();

		// set images
		File parent = osu.getFile().getParentFile();
		for (GameImage o : GameImage.values())
			o.setSkinImage(parent);

		// skip button
		Image skip = GameImage.SKIP.getImage();
		if (!GameImage.SKIP.isScaled()) {
			float skipScale = (height * 0.1f) / skip.getHeight();
			skip = skip.getScaledCopy(skipScale);
			GameImage.SKIP.setScaled();
		}
		skipButton = new GUIMenuButton(skip,
				width - (skip.getWidth() / 2f),
				height - (skip.getHeight() / 2f));

		// countdown
		float countdownHeight = height / 3f;
		if (!GameImage.COUNTDOWN_READY.isScaled()) {
			Image countdownReady = GameImage.COUNTDOWN_READY.getImage();
			GameImage.COUNTDOWN_READY.setImage(
					countdownReady.getScaledCopy(countdownHeight / countdownReady.getHeight()));
			GameImage.COUNTDOWN_READY.setScaled();
		}
		if (!GameImage.COUNTDOWN_3.isScaled()) {
			Image countdown3 = GameImage.COUNTDOWN_3.getImage();
			GameImage.COUNTDOWN_3.setImage(
					countdown3.getScaledCopy(countdownHeight / countdown3.getHeight()));
			GameImage.COUNTDOWN_3.setScaled();
		}
		if (!GameImage.COUNTDOWN_2.isScaled()) {
			Image countdown2 = GameImage.COUNTDOWN_2.getImage();
			GameImage.COUNTDOWN_2.setImage(
					countdown2.getScaledCopy(countdownHeight / countdown2.getHeight()));
			GameImage.COUNTDOWN_2.setScaled();
		}
		if (!GameImage.COUNTDOWN_1.isScaled()) {
			Image countdown1 = GameImage.COUNTDOWN_1.getImage();
			GameImage.COUNTDOWN_1.setImage(
					countdown1.getScaledCopy(countdownHeight / countdown1.getHeight()));
			GameImage.COUNTDOWN_1.setScaled();
		}
		if (!GameImage.COUNTDOWN_GO.isScaled()) {
			Image countdownGo = GameImage.COUNTDOWN_GO.getImage();
			GameImage.COUNTDOWN_GO.setImage(
					countdownGo.getScaledCopy(countdownHeight / countdownGo.getHeight()));
			GameImage.COUNTDOWN_GO.setScaled();
		}

		// load other images...
		((GamePauseMenu) game.getState(Opsu.STATE_GAMEPAUSEMENU)).loadImages();
		score.loadImages();
	}

	/**
	 * Set map modifiers.
	 */
	private void setMapModifiers() {
		try {
			// map-based properties, re-initialized each game
			float circleSize = osu.circleSize;
			float approachRate = osu.approachRate;
			float overallDifficulty = osu.overallDifficulty;
			float HPDrainRate = osu.HPDrainRate;

			// fixed difficulty overrides
			if (Options.getFixedCS() > 0f)
				circleSize = Options.getFixedCS();
			if (Options.getFixedAR() > 0f)
				approachRate = Options.getFixedAR();
			if (Options.getFixedOD() > 0f)
				overallDifficulty = Options.getFixedOD();
			if (Options.getFixedHP() > 0f)
				HPDrainRate = Options.getFixedHP();

			// hard rock modifiers
			if (Options.isModActive(Options.MOD_HARD_ROCK)) {
				circleSize = Math.min(circleSize * 1.4f, 10);
				approachRate = Math.min(approachRate * 1.4f, 10);
				overallDifficulty = Math.min(overallDifficulty * 1.4f, 10);
				HPDrainRate = Math.min(HPDrainRate * 1.4f, 10);
			}

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
