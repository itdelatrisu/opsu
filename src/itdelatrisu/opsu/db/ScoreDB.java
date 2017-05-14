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

package itdelatrisu.opsu.db;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.ScoreData;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.user.User;
import itdelatrisu.opsu.user.UserList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Handles connections and queries with the scores database.
 */
public class ScoreDB {
	/**
	 * Current database version.
	 * This value should be changed whenever the database format changes.
	 * Add any update queries to the {@link #getUpdateQueries(int)} method.
	 */
	private static final int DATABASE_VERSION = 20170201;

	/**
	 * Returns a list of SQL queries to apply, in order, to update from
	 * the given database version to the latest version.
	 * @param version the current version
	 * @return a list of SQL queries
	 */
	private static List<String> getUpdateQueries(int version) {
		List<String> list = new LinkedList<String>();
		if (version < 20140311)
			list.add("ALTER TABLE scores ADD COLUMN replay TEXT");
		if (version < 20150401)
			list.add("ALTER TABLE scores ADD COLUMN playerName TEXT");
		if (version < 20170201)
			list.add(String.format("UPDATE scores SET playerName = '%s' WHERE playerName IS NULL", UserList.DEFAULT_USER_NAME));

		/* add future updates here */

		return list;
	}

	/** Database connection. */
	private static Connection connection;

	/** Score insertion statement. */
	private static PreparedStatement insertStmt;

	/** Score select statement. */
	private static PreparedStatement selectMapStmt, selectMapSetStmt;

	/** Score deletion statement. */
	private static PreparedStatement deleteSongStmt, deleteScoreStmt;

	/** User-related statements. */
	private static PreparedStatement setCurrentUserStmt, insertUserStmt, deleteUserStmt;

	// This class should not be instantiated.
	private ScoreDB() {}

	/**
	 * Initializes the database connection.
	 */
	public static void init() throws SQLException {
		// create a database connection
		connection = DBController.createConnection(Options.SCORE_DB.getPath());

		// run any database updates
		updateDatabase();

		// create the database
		createDatabase();

		// prepare sql statements
		insertStmt = connection.prepareStatement(
			// TODO: There will be problems if multiple replays have the same
			// timestamp (e.g. when imported) due to timestamp being the primary key.
			"INSERT OR IGNORE INTO scores VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
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
			"geki = ? AND katu = ? AND miss = ? AND score = ? AND combo = ? AND perfect = ? AND mods = ? AND " +
			"(replay = ? OR (replay IS NULL AND ? IS NULL)) AND " +
			"(playerName = ? OR (playerName IS NULL AND ? IS NULL))"
			// TODO: extra playerName checks not needed if name is guaranteed not null
		);
		setCurrentUserStmt = connection.prepareStatement("INSERT OR REPLACE INTO info VALUES ('user', ?)");
		insertUserStmt = connection.prepareStatement("INSERT OR REPLACE INTO users VALUES (?, ?, ?, ?, ?, ?)");
		deleteUserStmt = connection.prepareStatement("DELETE FROM users WHERE name = ?");
	}

	/**
	 * Creates the database, if it does not exist.
	 */
	private static void createDatabase() throws SQLException {
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
					"mods INTEGER, " +
					"replay TEXT, " +
					"playerName TEXT"+
				");" +
				"CREATE TABLE IF NOT EXISTS users (" +
					"name TEXT NOT NULL UNIQUE, " +
					"score INTEGER, accuracy REAL, " +
					"playsPassed INTEGER, playsTotal INTEGER, " +
					"icon INTEGER" +
				");" +
				"CREATE TABLE IF NOT EXISTS info (" +
					"key TEXT NOT NULL UNIQUE, value TEXT" +
				"); " +
				"CREATE INDEX IF NOT EXISTS idx ON scores (MID, MSID, title, artist, creator, version);";
			stmt.executeUpdate(sql);

			// set the version key, if empty
			sql = String.format("INSERT OR IGNORE INTO info(key, value) VALUES('version', %d)", DATABASE_VERSION);
			stmt.executeUpdate(sql);
		}
	}

	/**
	 * Applies any database updates by comparing the current version to the
	 * stored version.  Does nothing if tables have not been created.
	 */
	private static void updateDatabase() throws SQLException {
		try (Statement stmt = connection.createStatement()) {
			int version = 0;

			// if 'info' table does not exist, assume version 0 and apply all updates
			String sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'info'";
			ResultSet rs = stmt.executeQuery(sql);
			boolean infoExists = rs.isBeforeFirst();
			rs.close();
			if (!infoExists) {
				// if 'scores' table also does not exist, databases not yet created
				sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'scores'";
				ResultSet scoresRS = stmt.executeQuery(sql);
				boolean scoresExists = scoresRS.isBeforeFirst();
				scoresRS.close();
				if (!scoresExists)
					return;
			} else {
				// try to retrieve stored version
				sql = "SELECT value FROM info WHERE key = 'version'";
				ResultSet versionRS = stmt.executeQuery(sql);
				String versionString = (versionRS.next()) ? versionRS.getString(1) : "0";
				versionRS.close();
				try {
					version = Integer.parseInt(versionString);
				} catch (NumberFormatException e) {}
			}

			// database versions match
			if (version >= DATABASE_VERSION)
				return;

			// apply updates
			for (String query : getUpdateQueries(version))
				stmt.executeUpdate(query);

			// update version
			if (infoExists) {
				PreparedStatement ps = connection.prepareStatement("REPLACE INTO info (key, value) VALUES ('version', ?)");
				ps.setString(1, Integer.toString(DATABASE_VERSION));
				ps.executeUpdate();
				ps.close();
			}
		}
	}

	/**
	 * Adds the game score to the database.
	 * @param data the GameData object
	 */
	public static void addScore(ScoreData data) {
		if (connection == null)
			return;

		try {
			setStatementFields(insertStmt, data);
			insertStmt.setString(18, data.replayString);
			insertStmt.setString(19, data.playerName);
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
		if (connection == null)
			return;

		try {
			setStatementFields(deleteScoreStmt, data);
			deleteScoreStmt.setString(18, data.replayString);
			deleteScoreStmt.setString(19, data.replayString);
			deleteScoreStmt.setString(20, data.playerName);
			deleteScoreStmt.setString(21, data.playerName);
			deleteScoreStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to delete score from database.", e, true);
		}
	}

	/**
	 * Deletes all the scores for the given beatmap from the database.
	 * @param beatmap the beatmap
	 */
	public static void deleteScore(Beatmap beatmap) {
		if (connection == null)
			return;

		try {
			deleteSongStmt.setInt(1, beatmap.beatmapID);
			deleteSongStmt.setString(2, beatmap.title);
			deleteSongStmt.setString(3, beatmap.artist);
			deleteSongStmt.setString(4, beatmap.creator);
			deleteSongStmt.setString(5, beatmap.version);
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
	 * Retrieves the game scores for a beatmap.
	 * @param beatmap the beatmap
	 * @return all scores for the beatmap, or null if any error occurred
	 */
	public static ScoreData[] getMapScores(Beatmap beatmap) {
		return getMapScoresExcluding(beatmap, null);
	}

	/**
	 * Retrieves the game scores for a beatmap while excluding a score.
	 * @param beatmap the beatmap
	 * @param exclude the filename (replay string) of the score to exclude
	 * @return all scores for the beatmap except for exclude, or null if any error occurred
	 */
	public static ScoreData[] getMapScoresExcluding(Beatmap beatmap, String exclude) {
		if (connection == null)
			return null;

		List<ScoreData> list = new ArrayList<ScoreData>();
		try {
			selectMapStmt.setInt(1, beatmap.beatmapID);
			selectMapStmt.setString(2, beatmap.title);
			selectMapStmt.setString(3, beatmap.artist);
			selectMapStmt.setString(4, beatmap.creator);
			selectMapStmt.setString(5, beatmap.version);
			ResultSet rs = selectMapStmt.executeQuery();
			while (rs.next()) {
				ScoreData s = new ScoreData(rs);
				if (s.replayString != null && s.replayString.equals(exclude)) {
					// don't return this score
				} else {
					list.add(s);
				}
			}
			rs.close();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to read scores from database.", e, true);
			return null;
		}
		return getSortedArray(list);
	}

	/**
	 * Retrieves the game scores for a beatmap set.
	 * @param beatmap the beatmap
	 * @return all scores for the beatmap set (Version, ScoreData[]),
	 *         or null if any error occurred
	 */
	public static Map<String, ScoreData[]> getMapSetScores(Beatmap beatmap) {
		if (connection == null)
			return null;

		Map<String, ScoreData[]> map = new HashMap<String, ScoreData[]>();
		try {
			selectMapSetStmt.setInt(1, beatmap.beatmapSetID);
			selectMapSetStmt.setString(2, beatmap.title);
			selectMapSetStmt.setString(3, beatmap.artist);
			selectMapSetStmt.setString(4, beatmap.creator);
			ResultSet rs = selectMapSetStmt.executeQuery();

			List<ScoreData> list = null;
			String version = null;  // sorted by version, so pass through and check for differences
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
	 * Retrieves all users.
	 * @return a list containing all users
	 */
	public static List<User> getUsers() {
		List<User> users = new ArrayList<User>();

		if (connection == null)
			return users;

		try (Statement stmt = connection.createStatement()) {
			String sql = "SELECT * FROM users";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
				users.add(new User(
					rs.getString(1), rs.getLong(2), rs.getDouble(3),
					rs.getInt(4), rs.getInt(5), rs.getInt(6)
				));
			rs.close();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to read users from database.", e, true);
		}
		return users;
	}

	/**
	 * Retrieves the current user.
	 * @return the current user's name, or null if not set.
	 */
	public static String getCurrentUser() {
		if (connection == null)
			return null;

		try (Statement stmt = connection.createStatement()) {
			String sql = "SELECT value FROM info WHERE key = 'user'";
			ResultSet rs = stmt.executeQuery(sql);
			String name = (rs.next()) ? rs.getString(1) : null;
			rs.close();
			return name;
		} catch (SQLException e) {
			ErrorHandler.error("Failed to read current user from database.", e, true);
			return null;
		}
	}

	/**
	 * Sets the current user.
	 * @param user the user's name
	 */
	public static void setCurrentUser(String user) {
		if (connection == null)
			return;

		try {
			setCurrentUserStmt.setString(1, user);
			setCurrentUserStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to set current user in database.", e, true);
		}
	}

	/**
	 * Updates a user entry, or creates one if it does not exist.
	 * @param user the user
	 */
	public static void updateUser(User user) {
		if (connection == null)
			return;

		try {
			insertUserStmt.setString(1, user.getName());
			insertUserStmt.setLong(2, user.getScore());
			insertUserStmt.setDouble(3, user.getAccuracy());
			insertUserStmt.setInt(4, user.getPassedPlays());
			insertUserStmt.setInt(5, user.getTotalPlays());
			insertUserStmt.setInt(6, user.getIconId());
			insertUserStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to update user in database.", e, true);
			return;
		}
	}

	/**
	 * Deletes a user.
	 * @param user the user's name
	 */
	public static void deleteUser(String user) {
		if (connection == null)
			return;

		try {
			deleteUserStmt.setString(1, user);
			deleteUserStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to delete user from database.", e, true);
		}
	}

	/**
	 * Closes the connection to the database.
	 */
	public static void closeConnection() {
		if (connection == null)
			return;

		try {
			insertStmt.close();
			selectMapStmt.close();
			selectMapSetStmt.close();
			deleteSongStmt.close();
			deleteScoreStmt.close();
			setCurrentUserStmt.close();
			insertUserStmt.close();
			deleteUserStmt.close();
			connection.close();
			connection = null;
		} catch (SQLException e) {
			ErrorHandler.error("Failed to close score database.", e, true);
		}
	}
}
