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

package itdelatrisu.opsu;

import itdelatrisu.opsu.GameData.Grade;
import itdelatrisu.opsu.states.SongMenu;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * Class encapsulating and drawing all score data.
 */
public class ScoreData implements Comparable<ScoreData> {
	/** The time when the score was achieved (Unix time). */
	public long timestamp;

	/** The beatmap and beatmap set IDs. */
	public int MID, MSID;

	/** Beatmap metadata. */
	public String title, artist, creator, version;

	/** Hit counts. */
	public int hit300, hit100, hit50, geki, katu, miss;

	/** The score. */
	public long score;

	/** The max combo. */
	public int combo;

	/** Whether or not a full combo was achieved. */
	public boolean perfect;

	/** Game mod bitmask. */
	public int mods;

	/** The replay string. */
	public String replayString;

	/** The player name. */
	public String playerName;

	/** Time since the score was achieved. */
	private String timeSince;

	/** The grade. */
	private Grade grade;

	/** The score percent. */
	private float scorePercent = -1f;

	/** A formatted string of the timestamp. */
	private String timeString;

	/** The tooltip string. */
	private String tooltip;

	/** Drawing values. */
	private static float baseX, baseY, buttonWidth, buttonHeight, buttonOffset, buttonAreaHeight;

	/** The container width. */
	private static int containerWidth;

	/**
	 * Initializes the base coordinates for drawing.
	 * @param containerWidth the container width
	 * @param topY the top y coordinate
	 */
	public static void init(int containerWidth, float topY) {
		ScoreData.containerWidth = containerWidth;
		baseX = containerWidth * 0.01f;
		baseY = topY;
		buttonWidth = containerWidth * 0.4f;
		float gradeHeight = GameImage.MENU_BUTTON_BG.getImage().getHeight() * 0.45f;
		buttonHeight = Math.max(gradeHeight, Fonts.DEFAULT.getLineHeight() * 3.03f);
		buttonOffset = buttonHeight + gradeHeight / 10f;
		buttonAreaHeight = (SongMenu.MAX_SCORE_BUTTONS - 1) * buttonOffset + buttonHeight;
	}

	/**
	 * Returns the Buttons Offset
	 */
	public static float getButtonOffset() { return buttonOffset; }

	/**
	 * Returns true if the coordinates are within the bounds of the
	 * button at the given index.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @param index the index (to offset the button from the topmost button)
	 */
	public static boolean buttonContains(float cx, float cy, int index) {
		float y = baseY + (index * buttonOffset);
		return ((cx >= 0 && cx < baseX + buttonWidth) &&
		        (cy > y && cy < y + buttonHeight));
	}

	/**
	 * Returns true if the coordinates are within the bounds of the
	 * score button area.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public static boolean areaContains(float cx, float cy) {
		return ((cx >= 0 && cx < baseX + buttonWidth) &&
		        (cy > baseY && cy < baseY + buttonOffset * SongMenu.MAX_SCORE_BUTTONS));
	}

	/**
	 * Draws the scroll bar for the score buttons.
	 * @param g the graphics context
	 * @param pos the start button index
	 * @param total the total number of buttons
	 */
	public static void drawScrollbar(Graphics g, float pos, float total) {
		UI.drawScrollbar(g, pos, total, SongMenu.MAX_SCORE_BUTTONS * buttonOffset, 0, baseY,
				0, buttonAreaHeight, null, Color.white, false);
	}

	/**
	 * Sets a vertical clip to the area.
	 * @param g the graphics context
	 */
	public static void clipToArea(Graphics g) {
		g.setClip(0, (int) baseY, containerWidth, (int) buttonAreaHeight);
	}

	/**
	 * Creates an empty score data object.
	 */
	public ScoreData() {}

	/**
	 * Builds a score data object from a database result set.
	 * @param rs the {@link ResultSet} to read from (at the current cursor position)
	 * @throws SQLException if a database access error occurs or the result set is closed
	 */
	public ScoreData(ResultSet rs) throws SQLException {
		this.timestamp = rs.getLong(1);
		this.MID = rs.getInt(2);
		this.MSID = rs.getInt(3);
		this.title = rs.getString(4);
		this.artist = rs.getString(5);
		this.creator = rs.getString(6);
		this.version = rs.getString(7);
		this.hit300 = rs.getInt(8);
		this.hit100 = rs.getInt(9);
		this.hit50 = rs.getInt(10);
		this.geki = rs.getInt(11);
		this.katu = rs.getInt(12);
		this.miss = rs.getInt(13);
		this.score = rs.getLong(14);
		this.combo = rs.getInt(15);
		this.perfect = rs.getBoolean(16);
		this.mods = rs.getInt(17);
		this.replayString = rs.getString(18);
		this.playerName = rs.getString(19);
	}

	/**
	 * Returns the timestamp as a string.
	 */
	public String getTimeString() {
		if (timeString == null)
			timeString = new SimpleDateFormat("M/d/yyyy h:mm:ss a").format(new Date(timestamp * 1000L));
		return timeString;
	}

	/**
	 * Returns the raw score percentage based on score data.
	 * @see GameData#getScorePercent(int, int, int, int)
	 */
	private float getScorePercent() {
		if (scorePercent < 0f)
			scorePercent = GameData.getScorePercent(hit300, hit100, hit50, miss);
		return scorePercent;
	}

	/**
	 * Returns letter grade based on score data,
	 * or Grade.NULL if no objects have been processed.
	 * @see GameData#getGrade(int, int, int, int, boolean)
	 */
	public Grade getGrade() {
		if (grade == null)
			grade = GameData.getGrade(hit300, hit100, hit50, miss,
					((mods & GameMod.HIDDEN.getBit()) > 0 || (mods & GameMod.FLASHLIGHT.getBit()) > 0));
		return grade;
	}

	/**
	 * Returns the time since achieving the score, or null if over 24 hours.
	 * This value will not change after the first call.
	 * @return a string: {number}{s|m|h}
	 */
	public String getTimeSince() {
		if (timeSince == null) {
			long seconds = (System.currentTimeMillis() / 1000L) - timestamp;
			if (seconds < 60)
				timeSince = String.format("%ds", seconds);
			else if (seconds < 3600)
				timeSince = String.format("%dm", seconds / 60L);
			else if (seconds < 86400)
				timeSince = String.format("%dh", seconds / 3600L);
			else
				timeSince = "";
		}
		return (timeSince.isEmpty()) ? null : timeSince;
	}

	/**
	 * Draws the score data as a rectangular button.
	 * @param g the graphics context
	 * @param position the index (to offset the button from the topmost button)
	 * @param rank the score rank
	 * @param prevScore the previous (lower) score, or -1 if none
	 * @param focus whether the button is focused
	 * @param t the animation progress [0,1]
	 */
	public void draw(Graphics g, float position, int rank, long prevScore, boolean focus, float t) {
		float x = baseX - buttonWidth * (1 - AnimationEquation.OUT_BACK.calc(t)) / 2.5f;
		float rankX = x + buttonWidth * 0.04f;
		float edgeX = x + buttonWidth * 0.98f;
		float y = baseY + position;
		float midY = y + buttonHeight / 2f;
		float marginY = Fonts.DEFAULT.getLineHeight() * 0.01f;
		Color c = Colors.WHITE_FADE;
		float alpha = t;
		float oldAlpha = c.a;
		c.a = alpha;

		// rectangle outline
		g.setLineWidth(1f);
		Color rectColor = (focus) ? Colors.BLACK_BG_HOVER : Colors.BLACK_BG_NORMAL;
		float oldRectAlpha = rectColor.a;
		rectColor.a *= AnimationEquation.IN_QUAD.calc(alpha);
		g.setColor(rectColor);
		g.fillRect(x + 1, y + 1, buttonWidth - 1, buttonHeight - 1);
		rectColor.a *= 1.25f;
		g.setColor(rectColor);
		g.drawRect(x, y, buttonWidth, buttonHeight);
		rectColor.a = oldRectAlpha;

		// rank
		if (focus) {
			Fonts.LARGE.drawString(
				rankX, y + (buttonHeight - Fonts.LARGE.getLineHeight()) / 2f,
				Integer.toString(rank + 1), c
			);
		}

		// grade image
		float gradeX = rankX + Fonts.LARGE.getWidth("###");
		Image img = getGrade().getMenuImage();
		img.setAlpha(alpha);
		img.draw(gradeX, midY - img.getHeight() / 2f);
		img.setAlpha(1f);

		// player
		float textX = gradeX + img.getWidth() * 1.2f;
		float textOffset = (buttonHeight - Fonts.LARGE.getLineHeight() - Fonts.MEDIUM.getLineHeight()) / 2f;
		if (playerName != null)
			Fonts.LARGE.drawString(textX, y + textOffset, playerName);
		textOffset += Fonts.LARGE.getLineHeight() - 4;

		// score
		Fonts.MEDIUM.drawString(
			textX, y + textOffset,
			String.format("Score: %s (%dx)", NumberFormat.getNumberInstance().format(score), combo), c
		);

		// mods
		StringBuilder sb = new StringBuilder();
		for (GameMod mod : GameMod.values()) {
			if ((mod.getBit() & mods) > 0) {
				sb.append(mod.getAbbreviation());
				sb.append(',');
			}
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
			String modString = sb.toString();
			Fonts.DEFAULT.drawString(edgeX - Fonts.DEFAULT.getWidth(modString), y + marginY, modString, c);
		}

		// accuracy
		String accuracy = String.format("%.2f%%", getScorePercent());
		Fonts.DEFAULT.drawString(edgeX - Fonts.DEFAULT.getWidth(accuracy), y + marginY + Fonts.DEFAULT.getLineHeight(), accuracy, c);

		// score difference
		String diff = (prevScore < 0 || score < prevScore) ?
			"-" : String.format("+%s", NumberFormat.getNumberInstance().format(score - prevScore));
		Fonts.DEFAULT.drawString(edgeX - Fonts.DEFAULT.getWidth(diff), y + marginY + Fonts.DEFAULT.getLineHeight() * 2, diff, c);

		// time since
		if (getTimeSince() != null) {
			Image clock = GameImage.HISTORY.getImage();
			clock.drawCentered(x + buttonWidth * 1.02f + clock.getWidth() / 2f, midY);
			Fonts.DEFAULT.drawString(
				x + buttonWidth * 1.03f + clock.getWidth(),
				midY - Fonts.DEFAULT.getLineHeight() / 2f,
				getTimeSince(), c
			);
		}

		c.a = oldAlpha;
	}

	/**
	 * Draws the score in-game (smaller and with less information).
	 * @param g the current graphics context
	 * @param vPos the base y position of the scoreboard
	 * @param rank the current rank of this score
	 * @param position the animated position offset
	 * @param data an instance of GameData to draw rank number
	 * @param alpha the transparency of the score
	 * @param isActive if this score is the one currently played
	 */
	public void drawSmall(Graphics g, int vPos, int rank, float position, GameData data, float alpha, boolean isActive) {
		int rectWidth = (int) (145 * GameImage.getUIscale());  //135
		int rectHeight = data.getScoreSymbolImage('0').getHeight();
		int vertDistance = rectHeight + 10;
		int yPos = (int) (vPos + position * vertDistance - rectHeight / 2);
		int xPaddingLeft = Math.max(4, (int) (rectWidth * 0.04f));
		int xPaddingRight = Math.max(2, (int) (rectWidth * 0.02f));
		int yPadding = Math.max(2, (int) (rectHeight * 0.02f));
		String scoreString = String.format(Locale.US, "%,d", score);
		String comboString = String.format("%dx", combo);
		String rankString = String.format("%d", rank);

		Color white = Colors.WHITE_ALPHA, blue = Colors.BLUE_SCOREBOARD, black = Colors.BLACK_ALPHA;
		float oldAlphaWhite = white.a, oldAlphaBlue = blue.a, oldAlphaBlack = black.a;

		// rectangle background
		Color rectColor = isActive ? white : blue;
		rectColor.a = alpha * 0.2f;
		g.setColor(rectColor);
		g.fillRect(0, yPos, rectWidth, rectHeight);
		black.a = alpha * 0.2f;
		g.setColor(black);
		float oldLineWidth = g.getLineWidth();
		g.setLineWidth(1f);
		g.drawRect(0, yPos, rectWidth, rectHeight);
		g.setLineWidth(oldLineWidth);

		// rank
		data.drawSymbolString(rankString, rectWidth, yPos, 1.0f, alpha * 0.2f, true);

		white.a = blue.a = alpha * 0.75f;

		// player name
		if (playerName != null)
			Fonts.MEDIUM.drawString(xPaddingLeft, yPos + yPadding, playerName, white);

		// score
		Fonts.DEFAULT.drawString(
			xPaddingLeft, yPos + rectHeight - Fonts.DEFAULT.getLineHeight() - yPadding, scoreString, white
		);

		// combo
		Fonts.DEFAULT.drawString(
			rectWidth - Fonts.DEFAULT.getWidth(comboString) - xPaddingRight,
			yPos + rectHeight - Fonts.DEFAULT.getLineHeight() - yPadding,
			comboString, blue
		);

		white.a = oldAlphaWhite;
		blue.a = oldAlphaBlue;
		black.a = oldAlphaBlack;
	}

	/**
	 * Loads glyphs necessary for rendering the player name.
	 */
	public void loadGlyphs() {
		if (playerName != null) {
			Fonts.loadGlyphs(Fonts.LARGE, playerName);
			Fonts.loadGlyphs(Fonts.MEDIUM, playerName);
		}
	}

	/**
	 * Returns the tooltip string for this score.
	 */
	public String getTooltipString() {
		if (tooltip == null)
			tooltip = String.format(
					"Achieved on %s\n300:%d 100:%d 50:%d Miss:%d\nAccuracy: %.2f%%\nMods: %s",
					getTimeString(), hit300, hit100, hit50, miss, getScorePercent(), GameMod.getModString(mods));
		return tooltip;
	}

	@Override
	public String toString() {
		return String.format(
			"%s | ID: (%d, %d) | %s - %s [%s] (by %s) | " +
			"Hits: (%d, %d, %d, %d, %d, %d) | Score: %d (%d combo%s) | Mods: %s",
			getTimeString(), MID, MSID, artist, title, version, creator,
			hit300, hit100, hit50, geki, katu, miss, score, combo, perfect ? ", FC" : "", GameMod.getModString(mods)
		);
	}

	@Override
	public int compareTo(ScoreData that) {
		if (this.score != that.score)
			return Long.compare(this.score, that.score);
		else
			return Long.compare(this.timestamp, that.timestamp);
	}
}
