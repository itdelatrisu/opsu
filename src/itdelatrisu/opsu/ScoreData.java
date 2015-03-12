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

package itdelatrisu.opsu;

import itdelatrisu.opsu.GameData.Grade;
import itdelatrisu.opsu.states.SongMenu;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

	/** Time since the score was achieved. */
	private String timeSince;

	/** The grade. */
	private Grade grade;

	/** The score percent. */
	private float scorePercent = -1f;

	/** Drawing values. */
	private static float baseX, baseY, buttonWidth, buttonHeight, buttonOffset;

	/** Button background colors. */
	private static final Color
		BG_NORMAL = new Color(0, 0, 0, 0.25f),
		BG_FOCUS  = new Color(0, 0, 0, 0.75f);

	/**
	 * Initializes the base coordinates for drawing.
	 * @param containerWidth the container width
	 * @param topY the top y coordinate
	 */
	public static void init(int containerWidth, float topY) {
		baseX = containerWidth * 0.01f;
		baseY = topY;
		buttonWidth = containerWidth * 0.4f;
		float gradeHeight = GameImage.MENU_BUTTON_BG.getImage().getHeight() * 0.45f;
		buttonHeight = Math.max(gradeHeight, Utils.FONT_DEFAULT.getLineHeight() * 3.03f);
		buttonOffset = buttonHeight + gradeHeight / 10f;
	}

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
	 * @param index the start button index
	 * @param total the total number of buttons
	 */
	public static void drawScrollbar(Graphics g, int index, int total) {
		UI.drawScrollbar(g, index, total, SongMenu.MAX_SCORE_BUTTONS, 0, baseY,
				0, buttonHeight, buttonOffset, null, Color.white, false);
	}

	/**
	 * Empty constructor.
	 */
	public ScoreData() {}

	/**
	 * Constructor.
	 * @param rs the ResultSet to read from (at the current cursor position)
	 * @throws SQLException
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
	}

	/**
	 * Returns the timestamp as a string.
	 */
	public String getTimeString() {
		return new SimpleDateFormat("M/d/yyyy h:mm:ss a").format(new Date(timestamp * 1000L));
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
	 * @see GameData#getGrade(int, int, int, int)
	 */
	public Grade getGrade() {
		if (grade == null)
			grade = GameData.getGrade(hit300, hit100, hit50, miss);
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
	 * @param index the index (to offset the button from the topmost button)
	 * @param rank the score rank
	 * @param prevScore the previous (lower) score, or -1 if none
	 * @param focus whether the button is focused
	 */
	public void draw(Graphics g, int index, int rank, long prevScore, boolean focus) {
		Image img = getGrade().getMenuImage();
		float textX = baseX + buttonWidth * 0.24f;
		float edgeX = baseX + buttonWidth * 0.98f;
		float y = baseY + index * (buttonOffset);
		float midY = y + buttonHeight / 2f;
		float marginY = Utils.FONT_DEFAULT.getLineHeight() * 0.01f;

		// rectangle outline
		g.setColor((focus) ? BG_FOCUS : BG_NORMAL);
		g.fillRect(baseX, y, buttonWidth, buttonHeight);

		// rank
		if (focus) {
			Utils.FONT_LARGE.drawString(
					baseX + buttonWidth * 0.04f,
					y + (buttonHeight - Utils.FONT_LARGE.getLineHeight()) / 2f,
					Integer.toString(rank + 1), Color.white
			);
		}

		// grade image
		img.drawCentered(baseX + buttonWidth * 0.15f, midY);

		// score
		float textOffset = (buttonHeight - Utils.FONT_MEDIUM.getLineHeight() - Utils.FONT_SMALL.getLineHeight()) / 2f;
		Utils.FONT_MEDIUM.drawString(
				textX, y + textOffset,
				String.format("Score: %s (%dx)", NumberFormat.getNumberInstance().format(score), combo),
				Color.white
		);

		// hit counts (custom: osu! shows user instead, above score)
		Utils.FONT_SMALL.drawString(
				textX, y + textOffset + Utils.FONT_MEDIUM.getLineHeight(),
				String.format("300:%d  100:%d  50:%d  Miss:%d", hit300, hit100, hit50, miss),
				Color.white
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
			Utils.FONT_DEFAULT.drawString(
					edgeX - Utils.FONT_DEFAULT.getWidth(modString),
					y + marginY, modString, Color.white
			);
		}

		// accuracy
		String accuracy = String.format("%.2f%%", getScorePercent());
		Utils.FONT_DEFAULT.drawString(
				edgeX - Utils.FONT_DEFAULT.getWidth(accuracy),
				y + marginY + Utils.FONT_DEFAULT.getLineHeight(),
				accuracy, Color.white
		);

		// score difference
		String diff = (prevScore < 0 || score < prevScore) ?
				"-" : String.format("+%s", NumberFormat.getNumberInstance().format(score - prevScore));
		Utils.FONT_DEFAULT.drawString(
				edgeX - Utils.FONT_DEFAULT.getWidth(diff),
				y + marginY + Utils.FONT_DEFAULT.getLineHeight() * 2,
				diff, Color.white
		);

		// time since
		if (getTimeSince() != null) {
			Image clock = GameImage.HISTORY.getImage();
			clock.drawCentered(baseX + buttonWidth * 1.02f + clock.getWidth() / 2f, midY);
			Utils.FONT_DEFAULT.drawString(
					baseX + buttonWidth * 1.03f + clock.getWidth(),
					midY - Utils.FONT_DEFAULT.getLineHeight() / 2f,
					getTimeSince(), Color.white
			);
		}
	}

	@Override
	public String toString() {
		return String.format(
			"%s | ID: (%d, %d) | %s - %s [%s] (by %s) | " +
			"Hits: (%d, %d, %d, %d, %d, %d) | Score: %d (%d combo%s) | Mods: %d",
			getTimeString(), MID, MSID, artist, title, version, creator,
			hit300, hit100, hit50, geki, katu, miss, score, combo, perfect ? ", FC" : "", mods
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
