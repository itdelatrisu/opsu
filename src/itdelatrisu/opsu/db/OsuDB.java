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
import itdelatrisu.opsu.OsuParser;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles connections and queries with the cached beatmap database.
 */
public class OsuDB {
	/**
	 * Current database version.
	 * This value should be changed whenever the database format changes.
	 */
	private static final String DATABASE_VERSION = "2014-03-04";

	/** Database connection. */
	private static Connection connection;

	/** Query statements. */
	private static PreparedStatement insertStmt, selectStmt, lastModStmt, deleteMapStmt, deleteGroupStmt;

	// This class should not be instantiated.
	private OsuDB() {}

	/**
	 * Initializes the database connection.
	 */
	public static void init() {
		// create a database connection
		try {
			connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", Options.OSU_DB.getPath()));
		} catch (SQLException e) {
			// if the error message is "out of memory", it probably means no database file is found
			ErrorHandler.error("Could not connect to beatmap database.", e, true);
		}

		// create the database
		createDatabase();

		// check the database version
		checkVersion();

		// prepare sql statements
		try {
			insertStmt = connection.prepareStatement(
				"INSERT INTO beatmaps VALUES (" +
				"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
				"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
			);
			lastModStmt = connection.prepareStatement("SELECT dir, file, lastModified FROM beatmaps");
			selectStmt = connection.prepareStatement("SELECT * FROM beatmaps WHERE dir = ? AND file = ?");
			deleteMapStmt = connection.prepareStatement("DELETE FROM beatmaps WHERE dir = ? AND file = ?");
			deleteGroupStmt = connection.prepareStatement("DELETE FROM beatmaps WHERE dir = ?");
		} catch (SQLException e) {
			ErrorHandler.error("Failed to prepare beatmap statements.", e, true);
		}
	}

	/**
	 * Creates the database, if it does not exist.
	 */
	private static void createDatabase() {
		try (Statement stmt = connection.createStatement()) {
			String sql =
				"CREATE TABLE IF NOT EXISTS beatmaps (" +
					"dir TEXT, file TEXT, lastModified INTEGER, " +
					"MID INTEGER, MSID INTEGER, " +
					"title TEXT, titleUnicode TEXT, artist TEXT, artistUnicode TEXT, " +
					"creator TEXT, version TEXT, source TEXT, tags TEXT, " +
					"circles INTEGER, sliders INTEGER, spinners INTEGER, " +
					"hp REAL, cs REAL, od REAL, ar REAL, sliderMultiplier REAL, sliderTickRate REAL, " +
					"bpmMin INTEGER, bpmMax INTEGER, endTime INTEGER, " +
					"audioFile TEXT, audioLeadIn INTEGER, previewTime INTEGER, countdown INTEGER, sampleSet TEXT, stackLeniency REAL, " +
					"mode INTEGER, letterboxInBreaks BOOLEAN, widescreenStoryboard BOOLEAN, epilepsyWarning BOOLEAN, " +
					"bg TEXT, timingPoints TEXT, breaks TEXT, combo TEXT" +
				"); " +
				"CREATE TABLE IF NOT EXISTS info (" +
					"key TEXT NOT NULL UNIQUE, value TEXT" +
				")";
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			ErrorHandler.error("Could not create beatmap database.", e, true);
		}
	}

	/**
	 * Checks the stored table version, clears the beatmap database if different
	 * from the current version, then updates the version field.
	 */
	private static void checkVersion() {
		try (Statement stmt = connection.createStatement()) {
			// get the stored version
			String sql = "SELECT value FROM info WHERE key = 'version'";
			ResultSet rs = stmt.executeQuery(sql);
			String version = (rs.next()) ? rs.getString(1) : "";
			rs.close();

			// if different from current version, clear the database
			if (!version.equals(DATABASE_VERSION))
				clearDatabase();

			// update version
			PreparedStatement ps = connection.prepareStatement("REPLACE INTO info (key, value) VALUES ('version', ?)");
			ps.setString(1, DATABASE_VERSION);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			ErrorHandler.error("Beatmap database version checks failed.", e, true);
		}
	}

	/**
	 * Clears the database.
	 */
	public static void clearDatabase() {
		// drop the table, then recreate it
		try (Statement stmt = connection.createStatement()) {
			String sql = "DROP TABLE beatmaps";
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			ErrorHandler.error("Could not drop beatmap database.", e, true);
		}
		createDatabase();
	}

	/**
	 * Adds the OsuFile to the database.
	 * @param osu the OsuFile object
	 */
	public static void insert(OsuFile osu) {
		try {
			setStatementFields(insertStmt, osu);
			insertStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to add beatmap to database.", e, true);
		}
	}

	/**
	 * Adds the OsuFiles to the database in a batch.
	 * @param batch a list of OsuFile objects
	 */
	public static void insert(List<OsuFile> batch) {
		try {
			// turn off auto-commit mode
			boolean autoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			// batch insert
			for (OsuFile osu : batch) {
				setStatementFields(insertStmt, osu);
				insertStmt.addBatch();
			}
			insertStmt.executeBatch();
			connection.commit();

			// restore previous auto-commit mode
			connection.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			ErrorHandler.error("Failed to add beatmaps to database.", e, true);
		}
	}

	/**
	 * Sets all statement fields using a given OsuFile object.
	 * @param stmt the statement to set fields for
	 * @param osu the OsuFile
	 * @throws SQLException
	 */
	private static void setStatementFields(PreparedStatement stmt, OsuFile osu)
			throws SQLException {
		stmt.setString(1, osu.getFile().getParentFile().getName());
		stmt.setString(2, osu.getFile().getName());
		stmt.setLong(3, osu.getFile().lastModified());
		stmt.setInt(4, osu.beatmapID);
		stmt.setInt(5, osu.beatmapSetID);
		stmt.setString(6, osu.title);
		stmt.setString(7, osu.titleUnicode);
		stmt.setString(8, osu.artist);
		stmt.setString(9, osu.artistUnicode);
		stmt.setString(10, osu.creator);
		stmt.setString(11, osu.version);
		stmt.setString(12, osu.source);
		stmt.setString(13, osu.tags);
		stmt.setInt(14, osu.hitObjectCircle);
		stmt.setInt(15, osu.hitObjectSlider);
		stmt.setInt(16, osu.hitObjectSpinner);
		stmt.setFloat(17, osu.HPDrainRate);
		stmt.setFloat(18, osu.circleSize);
		stmt.setFloat(19, osu.overallDifficulty);
		stmt.setFloat(20, osu.approachRate);
		stmt.setFloat(21, osu.sliderMultiplier);
		stmt.setFloat(22, osu.sliderTickRate);
		stmt.setInt(23, osu.bpmMin);
		stmt.setInt(24, osu.bpmMax);
		stmt.setInt(25, osu.endTime);
		stmt.setString(26, osu.audioFilename.getName());
		stmt.setInt(27, osu.audioLeadIn);
		stmt.setInt(28, osu.previewTime);
		stmt.setByte(29, osu.countdown);
		stmt.setString(30, osu.sampleSet);
		stmt.setFloat(31, osu.stackLeniency);
		stmt.setByte(32, osu.mode);
		stmt.setBoolean(33, osu.letterboxInBreaks);
		stmt.setBoolean(34, osu.widescreenStoryboard);
		stmt.setBoolean(35, osu.epilepsyWarning);
		stmt.setString(36, osu.bg);
		stmt.setString(37, osu.timingPointsToString());
		stmt.setString(38, osu.breaksToString());
		stmt.setString(39, osu.comboToString());
	}

	/**
	 * Returns an OsuFile from the database, or null if any error occurred.
	 * @param dir the directory
	 * @param file the file
	 */
	public static OsuFile getOsuFile(File dir, File file) {
		try {
			OsuFile osu = new OsuFile(file);
			selectStmt.setString(1, dir.getName());
			selectStmt.setString(2, file.getName());
			ResultSet rs = selectStmt.executeQuery();
			while (rs.next()) {
				osu.beatmapID = rs.getInt(4);
				osu.beatmapSetID = rs.getInt(5);
				osu.title = OsuParser.getDBString(rs.getString(6));
				osu.titleUnicode = OsuParser.getDBString(rs.getString(7));
				osu.artist = OsuParser.getDBString(rs.getString(8));
				osu.artistUnicode = OsuParser.getDBString(rs.getString(9));
				osu.creator = OsuParser.getDBString(rs.getString(10));
				osu.version = OsuParser.getDBString(rs.getString(11));
				osu.source = OsuParser.getDBString(rs.getString(12));
				osu.tags = OsuParser.getDBString(rs.getString(13));
				osu.hitObjectCircle = rs.getInt(14);
				osu.hitObjectSlider = rs.getInt(15);
				osu.hitObjectSpinner = rs.getInt(16);
				osu.HPDrainRate = rs.getFloat(17);
				osu.circleSize = rs.getFloat(18);
				osu.overallDifficulty = rs.getFloat(19);
				osu.approachRate = rs.getFloat(20);
				osu.sliderMultiplier = rs.getFloat(21);
				osu.sliderTickRate = rs.getFloat(22);
				osu.bpmMin = rs.getInt(23);
				osu.bpmMax = rs.getInt(24);
				osu.endTime = rs.getInt(25);
				osu.audioFilename = new File(dir, OsuParser.getDBString(rs.getString(26)));
				osu.audioLeadIn = rs.getInt(27);
				osu.previewTime = rs.getInt(28);
				osu.countdown = rs.getByte(29);
				osu.sampleSet = OsuParser.getDBString(rs.getString(30));
				osu.stackLeniency = rs.getFloat(31);
				osu.mode = rs.getByte(32);
				osu.letterboxInBreaks = rs.getBoolean(33);
				osu.widescreenStoryboard = rs.getBoolean(34);
				osu.epilepsyWarning = rs.getBoolean(35);
				osu.bg = OsuParser.getDBString(rs.getString(36));
				osu.timingPointsFromString(rs.getString(37));
				osu.breaksFromString(rs.getString(38));
				osu.comboFromString(rs.getString(39));
			}
			rs.close();
			return osu;
		} catch (SQLException e) {
			ErrorHandler.error("Failed to get OsuFile from database.", e, true);
			return null;
		}
	}

	/**
	 * Returns a map of file paths ({dir}/{file}) to last modified times, or
	 * null if any error occurred.
	 */
	public static Map<String, Long> getLastModifiedMap() {
		try {
			Map<String, Long> map = new HashMap<String, Long>();
			ResultSet rs = lastModStmt.executeQuery();
			while (rs.next()) {
				String path = String.format("%s/%s", rs.getString(1), rs.getString(2));
				long lastModified = rs.getLong(3);
				map.put(path, lastModified);
			}
			rs.close();
			return map;
		} catch (SQLException e) {
			ErrorHandler.error("Failed to get last modified map from database.", e, true);
			return null;
		}
	}

	/**
	 * Deletes the beatmap entry from the database.
	 * @param dir the directory
	 * @param file the file
	 */
	public static void delete(String dir, String file) {
		try {
			deleteMapStmt.setString(1, dir);
			deleteMapStmt.setString(2, file);
			deleteMapStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to delete beatmap entry from database.", e, true);
		}
	}

	/**
	 * Deletes the beatmap group entry from the database.
	 * @param dir the directory
	 */
	public static void delete(String dir) {
		try {
			deleteGroupStmt.setString(1, dir);
			deleteGroupStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to delete beatmap group entry from database.", e, true);
		}
	}

	/**
	 * Closes the connection to the database.
	 */
	public static void closeConnection() {
		if (connection != null) {
			try {
				insertStmt.close();
				lastModStmt.close();
				selectStmt.close();
				deleteMapStmt.close();
				deleteGroupStmt.close();
				connection.close();
			} catch (SQLException e) {
				ErrorHandler.error("Failed to close beatmap database.", e, true);
			}
		}
	}
}
