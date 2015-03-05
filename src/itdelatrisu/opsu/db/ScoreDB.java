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

package itdelatrisu.opsu.db;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.ScoreData;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles connections and queries with the scores database.
 */
public class ScoreDB {
	/** Database connection. */
	private static Connection connection;

	/** Score insertion statement. */
	private static PreparedStatement insertStmt;

	/** Score select statement. */
	private static PreparedStatement selectMapStmt, selectMapSetStmt;

	/** Score deletion statement. */
	private static PreparedStatement deleteSongStmt, deleteScoreStmt;

	// This class should not be instantiated.
	private ScoreDB() {}

	/**
	 * Initializes the database connection.
	 */
	public static void init() {
		// create a database connection
		try {
			connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", Options.SCORE_DB.getPath()));
		} catch (SQLException e) {
			// if the error message is "out of memory", it probably means no database file is found
			ErrorHandler.error("Could not connect to score database.", e, true);
		}

		// create the database
		createDatabase();

		// prepare sql statements
		try {
			insertStmt = connection.prepareStatement(
				"INSERT INTO scores VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
			);
			selectMapStmt = connection.prepareStatement(
				"SELECT * FROM scores WHERE " +
				"MID = ? AND title = ? AND artist = ? AND creator = ? AND version = ?"
			);
			selectMapSetStmt = connection.prepareStatement(
				"SELECT * FROM scores WHERE " +
				"MSID = ? AND title = ? AND artist = ? AND creator = ? ORDER BY version DESC"
			);
			deleteSongStmt = connection.prepareStatement(
				"DELETE FROM scores WHERE " +
				"MID = ? AND title = ? AND artist = ? AND creator = ? AND version = ?"
			);
			deleteScoreStmt = connection.prepareStatement(
				"DELETE FROM scores WHERE " +
				"timestamp = ? AND MID = ? AND MSID = ? AND title = ? AND artist = ? AND " +
				"creator = ? AND version = ? AND hit300 = ? AND hit100 = ? AND hit50 = ? AND " +
				"geki = ? AND katu = ? AND miss = ? AND score = ? AND combo = ? AND perfect = ? AND mods = ?"
			);
		} catch (SQLException e) {
			ErrorHandler.error("Failed to prepare score statements.", e, true);
		}
	}

	/**
	 * Creates the database, if it does not exist.
	 */
	private static void createDatabase() {
		try (Statement stmt = connection.createStatement()) {
			String sql =
				"CREATE TABLE IF NOT EXISTS scores (" +
					"timestamp INTEGER PRIMARY KEY, " +
					"MID INTEGER, MSID INTEGER, " +
					"title TEXT, artist TEXT, creator TEXT, version TEXT, " +
					"hit300 INTEGER, hit100 INTEGER, hit50 INTEGER, " +
					"geki INTEGER, katu INTEGER, miss INTEGER, " +
					"score INTEGER, " +
					"combo INTEGER, " +
					"perfect BOOLEAN, " +
					"mods INTEGER" +
				")";
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			ErrorHandler.error("Could not create score database.", e, true);
		}
	}

	/**
	 * Adds the game score to the database.
	 * @param data the GameData object
	 */
	public static void addScore(ScoreData data) {
		try {
			setStatementFields(insertStmt, data);
			insertStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to save score to database.", e, true);
		}
	}

	/**
	 * Deletes the given score from the database.
	 * @param data the score to delete
	 */
	public static void deleteScore(ScoreData data) {
		try {
			setStatementFields(deleteScoreStmt, data);
			deleteScoreStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to delete score from database.", e, true);
		}
	}

	/**
	 * Deletes all the scores for the given beatmap from the database.
	 * @param osu the OsuFile object
	 */
	public static void deleteScore(OsuFile osu) {
		try {
			deleteSongStmt.setInt(1, osu.beatmapID);
			deleteSongStmt.setString(2, osu.title);
			deleteSongStmt.setString(3, osu.artist);
			deleteSongStmt.setString(4, osu.creator);
			deleteSongStmt.setString(5, osu.version);
			deleteSongStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to delete scores from database.", e, true);
		}
	}

	/**
	 * Sets all statement fields using a given ScoreData object.
	 * @param stmt the statement to set fields for
	 * @param data the score data
	 * @throws SQLException
	 */
	private static void setStatementFields(PreparedStatement stmt, ScoreData data)
			throws SQLException {
		stmt.setLong(1, data.timestamp);
		stmt.setInt(2, data.MID);
		stmt.setInt(3, data.MSID);
		stmt.setString(4, data.title);
		stmt.setString(5, data.artist);
		stmt.setString(6, data.creator);
		stmt.setString(7, data.version);
		stmt.setInt(8, data.hit300);
		stmt.setInt(9, data.hit100);
		stmt.setInt(10, data.hit50);
		stmt.setInt(11, data.geki);
		stmt.setInt(12, data.katu);
		stmt.setInt(13, data.miss);
		stmt.setLong(14, data.score);
		stmt.setInt(15, data.combo);
		stmt.setBoolean(16, data.perfect);
		stmt.setInt(17, data.mods);
	}

	/**
	 * Retrieves the game scores for an OsuFile map.
	 * @param osu the OsuFile
	 * @return all scores for the beatmap
	 */
	public static ScoreData[] getMapScores(OsuFile osu) {
		List<ScoreData> list = new ArrayList<ScoreData>();
		try {
			selectMapStmt.setInt(1, osu.beatmapID);
			selectMapStmt.setString(2, osu.title);
			selectMapStmt.setString(3, osu.artist);
			selectMapStmt.setString(4, osu.creator);
			selectMapStmt.setString(5, osu.version);
			ResultSet rs = selectMapStmt.executeQuery();
			while (rs.next()) {
				ScoreData s = new ScoreData(rs);
				list.add(s);
			}
			rs.close();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to read scores from database.", e, true);
			return null;
		}
		return getSortedArray(list);
	}

	/**
	 * Retrieves the game scores for an OsuFile map set.
	 * @param osu the OsuFile
	 * @return all scores for the beatmap set (Version, ScoreData[])
	 */
	public static Map<String, ScoreData[]> getMapSetScores(OsuFile osu) {
		Map<String, ScoreData[]> map = new HashMap<String, ScoreData[]>();
		try {
			selectMapSetStmt.setInt(1, osu.beatmapSetID);
			selectMapSetStmt.setString(2, osu.title);
			selectMapSetStmt.setString(3, osu.artist);
			selectMapSetStmt.setString(4, osu.creator);
			ResultSet rs = selectMapSetStmt.executeQuery();

			List<ScoreData> list = null;
			String version = "";  // sorted by version, so pass through and check for differences
			while (rs.next()) {
				ScoreData s = new ScoreData(rs);
				if (!s.version.equals(version)) {
					if (list != null)
						map.put(version, getSortedArray(list));
					version = s.version;
					list = new ArrayList<ScoreData>();
				}
				list.add(s);
			}
			if (list != null)
				map.put(version, getSortedArray(list));
			rs.close();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to read scores from database.", e, true);
			return null;
		}
		return map;
	}

	/**
	 * Returns a sorted ScoreData array (in reverse order) from a List.
	 */
	private static ScoreData[] getSortedArray(List<ScoreData> list) {
		ScoreData[] scores = list.toArray(new ScoreData[list.size()]);
		Arrays.sort(scores, Collections.reverseOrder());
		return scores;
	}

	/**
	 * Closes the connection to the database.
	 */
	public static void closeConnection() {
		if (connection != null) {
			try {
				insertStmt.close();
				selectMapStmt.close();
				selectMapSetStmt.close();
				connection.close();
			} catch (SQLException e) {
				ErrorHandler.error("Failed to close score database.", e, true);
			}
		}
	}
}
