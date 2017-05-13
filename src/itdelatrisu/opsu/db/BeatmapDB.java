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
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.options.Options;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.newdawn.slick.util.Log;

/**
 * Handles connections and queries with the cached beatmap database.
 */
public class BeatmapDB {
	/**
	 * Current database version.
	 * This value should be changed whenever the database format changes.
	 * Add any update queries to the {@link #getUpdateQueries(int)} method.
	 */
	private static final int DATABASE_VERSION = 20170221;

	/**
	 * Returns a list of SQL queries to apply, in order, to update from
	 * the given database version to the latest version.
	 * @param version the current version
	 * @return a list of SQL queries
	 */
	private static List<String> getUpdateQueries(int version) {
		List<String> list = new LinkedList<String>();
		if (version < 20161222) {
			list.add("ALTER TABLE beatmaps ADD COLUMN dateAdded INTEGER");
			list.add("ALTER TABLE beatmaps ADD COLUMN favorite BOOLEAN");
			list.add("ALTER TABLE beatmaps ADD COLUMN playCount INTEGER");
			list.add("ALTER TABLE beatmaps ADD COLUMN lastPlayed INTEGER");
			list.add("UPDATE beatmaps SET dateAdded = 0, favorite = 0, playCount = 0, lastPlayed = 0");
		}
		if (version < 20161225) {
			list.add("ALTER TABLE beatmaps ADD COLUMN localOffset INTEGER");
			list.add("UPDATE beatmaps SET localOffset = 0");
		}
		if (version < 20170128) {
			list.add("ALTER TABLE beatmaps ADD COLUMN video TEXT");
			list.add("ALTER TABLE beatmaps ADD COLUMN videoOffset INTEGER");
			list.add("UPDATE beatmaps SET videoOffset = 0");
		}
		if (version < 20170221) {
			list.add("UPDATE beatmaps SET stars = -1");
		}

		/* add future updates here */

		return list;
	}

	/** Minimum batch size ratio ({@code batchSize/cacheSize}) to invoke batch loading. */
	private static final float LOAD_BATCH_MIN_RATIO = 0.2f;

	/** Minimum batch size to invoke batch insertion. */
	private static final int INSERT_BATCH_MIN = 100;

	/** Beatmap loading flags. */
	public static final int LOAD_NONARRAY = 1, LOAD_ARRAY = 2, LOAD_ALL = 3;

	/** Represents an entry in the last modified map. */
	public static class LastModifiedMapEntry {
		/** The last modified time. */
		private final long lastModified;

		/** The game mode. */
		private final byte mode;

		/**
		 * Creates a new entry.
		 * @param lastModified the last modified time
		 * @param mode the game mode (Beatmap.MODE_*)
		 */
		public LastModifiedMapEntry(long lastModified, byte mode) {
			this.lastModified = lastModified;
			this.mode = mode;
		}

		/** Returns the last modified time. */
		public long getLastModified() { return lastModified; }

		/** Returns the game mode (Beatmap.MODE_*). */
		public byte getMode() { return mode; }
	}

	/** Database connection. */
	private static Connection connection;

	/** Query statements. */
	private static PreparedStatement
		insertStmt, selectStmt, deleteMapStmt, deleteGroupStmt,
		setStarsStmt, updatePlayStatsStmt, setFavoriteStmt, setLocalOffsetStmt, updateSizeStmt;

	/** Current size of beatmap cache table. */
	private static int cacheSize = -1;

	// This class should not be instantiated.
	private BeatmapDB() {}

	/**
	 * Initializes the database connection.
	 */
	public static void init() throws SQLException {
		// create a database connection
		connection = DBController.createConnection(Options.BEATMAP_DB.getPath());

		// run any database updates
		updateDatabase();

		// create the database
		createDatabase();

		// prepare sql statements (used below)
		updateSizeStmt = connection.prepareStatement("REPLACE INTO info (key, value) VALUES ('size', ?)");

		// retrieve the cache size
		getCacheSize();

		// prepare sql statements (not used here)
		insertStmt = connection.prepareStatement(
			"INSERT INTO beatmaps VALUES (" +
				"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
				"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
				"?, ?, ?, ?, ?, ?, ?, ?, ?" +
			")"
		);
		selectStmt = connection.prepareStatement("SELECT * FROM beatmaps WHERE dir = ? AND file = ?");
		deleteMapStmt = connection.prepareStatement("DELETE FROM beatmaps WHERE dir = ? AND file = ?");
		deleteGroupStmt = connection.prepareStatement("DELETE FROM beatmaps WHERE dir = ?");
		setStarsStmt = connection.prepareStatement("UPDATE beatmaps SET stars = ? WHERE dir = ? AND file = ?");
		updatePlayStatsStmt = connection.prepareStatement("UPDATE beatmaps SET playCount = ?, lastPlayed = ? WHERE dir = ? AND file = ?");
		setFavoriteStmt = connection.prepareStatement("UPDATE beatmaps SET favorite = ? WHERE dir = ? AND file = ?");
		setLocalOffsetStmt = connection.prepareStatement("UPDATE beatmaps SET localOffset = ? WHERE dir = ? AND file = ?");
	}

	/**
	 * Creates the database, if it does not exist.
	 */
	private static void createDatabase() throws SQLException {
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
					"bg TEXT, sliderBorder TEXT, timingPoints TEXT, breaks TEXT, combo TEXT, " +
					"md5hash TEXT, stars REAL, " +
					"dateAdded INTEGER, favorite BOOLEAN, playCount INTEGER, lastPlayed INTEGER, localOffset INTEGER, " +
					"video TEXT, videoOffset INTEGER" +
				"); " +
				"CREATE TABLE IF NOT EXISTS info (" +
					"key TEXT NOT NULL UNIQUE, value TEXT" +
				"); " +
				"CREATE INDEX IF NOT EXISTS idx ON beatmaps (dir, file); " +

				// extra optimizations
				"PRAGMA locking_mode = EXCLUSIVE; " +
				"PRAGMA journal_mode = WAL;";
			stmt.executeUpdate(sql);

			// set the version key, if empty
			sql = String.format("INSERT OR IGNORE INTO info(key, value) VALUES('version', '%s')", DATABASE_VERSION);
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
				// if 'beatmaps' table also does not exist, databases not yet created
				sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'beatmaps'";
				ResultSet beatmapsRS = stmt.executeQuery(sql);
				boolean beatmapsExists = beatmapsRS.isBeforeFirst();
				beatmapsRS.close();
				if (!beatmapsExists)
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
	 * Retrieves the size of the beatmap cache from the 'info' table.
	 */
	private static void getCacheSize() {
		try (Statement stmt = connection.createStatement()) {
			String sql = "SELECT value FROM info WHERE key = 'size'";
			ResultSet rs = stmt.executeQuery(sql);
			try {
				cacheSize = (rs.next()) ? Integer.parseInt(rs.getString(1)) : 0;
			} catch (NumberFormatException e) {
				cacheSize = 0;
			}
			rs.close();
		} catch (SQLException e) {
			ErrorHandler.error("Could not get beatmap cache size.", e, true);
		}
	}

	/**
	 * Updates the size of the beatmap cache in the 'info' table.
	 */
	private static void updateCacheSize() {
		if (connection == null)
			return;

		try {
			updateSizeStmt.setString(1, Integer.toString(Math.max(cacheSize, 0)));
			updateSizeStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error("Could not update beatmap cache size.", e, true);
		}
	}

	/**
	 * Clears the database.
	 */
	public static void clearDatabase() {
		if (connection == null)
			return;

		// drop the table
		try (Statement stmt = connection.createStatement()) {
			String sql = "DROP TABLE beatmaps";
			stmt.executeUpdate(sql);
			cacheSize = 0;
			updateCacheSize();
		} catch (SQLException e) {
			ErrorHandler.error("Could not drop beatmap database.", e, true);
		}

		// recreate it
		try {
			createDatabase();
		} catch (SQLException e) {
			ErrorHandler.error("Could not create beatmap database.", e, true);
		}
	}

	/**
	 * Adds the beatmap to the database.
	 * @param beatmap the beatmap
	 */
	public static void insert(Beatmap beatmap) {
		if (connection == null)
			return;

		try {
			setStatementFields(insertStmt, beatmap);
			cacheSize += insertStmt.executeUpdate();
			updateCacheSize();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to add beatmap to database.", e, true);
		}
	}

	/**
	 * Adds the beatmaps to the database in a batch.
	 * @param batch a list of beatmaps
	 */
	public static void insert(List<Beatmap> batch) {
		if (connection == null)
			return;

		try (Statement stmt = connection.createStatement()) {
			// turn off auto-commit mode
			boolean autoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			// drop indexes
			boolean recreateIndexes = (batch.size() >= INSERT_BATCH_MIN);
			if (recreateIndexes) {
				String sql = "DROP INDEX IF EXISTS idx";
				stmt.executeUpdate(sql);
			}

			// batch insert
			for (Beatmap beatmap : batch) {
				try {
					setStatementFields(insertStmt, beatmap);
				} catch (SQLException e) {
					Log.error(String.format("Failed to insert map '%s' into database.", beatmap.getFile().getPath()), e);
					continue;
				}
				insertStmt.addBatch();
			}
			int[] results = insertStmt.executeBatch();
			for (int i = 0; i < results.length; i++) {
				if (results[i] > 0)
					cacheSize += results[i];
			}

			// re-create indexes
			if (recreateIndexes) {
				String sql = "CREATE INDEX idx ON beatmaps (dir, file)";
				stmt.executeUpdate(sql);
			}

			// restore previous auto-commit mode
			connection.commit();
			connection.setAutoCommit(autoCommit);

			// update cache size
			updateCacheSize();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to add beatmaps to database.", e, true);
		}
	}

	/**
	 * Sets all statement fields using a given beatmap.
	 * @param stmt the statement to set fields for
	 * @param beatmap the beatmap
	 * @throws SQLException
	 */
	private static void setStatementFields(PreparedStatement stmt, Beatmap beatmap)
			throws SQLException {
		try {
			stmt.setString(1, beatmap.getFile().getParentFile().getName());
			stmt.setString(2, beatmap.getFile().getName());
			stmt.setLong(3, beatmap.getFile().lastModified());
			stmt.setInt(4, beatmap.beatmapID);
			stmt.setInt(5, beatmap.beatmapSetID);
			stmt.setString(6, beatmap.title);
			stmt.setString(7, beatmap.titleUnicode);
			stmt.setString(8, beatmap.artist);
			stmt.setString(9, beatmap.artistUnicode);
			stmt.setString(10, beatmap.creator);
			stmt.setString(11, beatmap.version);
			stmt.setString(12, beatmap.source);
			stmt.setString(13, beatmap.tags);
			stmt.setInt(14, beatmap.hitObjectCircle);
			stmt.setInt(15, beatmap.hitObjectSlider);
			stmt.setInt(16, beatmap.hitObjectSpinner);
			stmt.setFloat(17, beatmap.HPDrainRate);
			stmt.setFloat(18, beatmap.circleSize);
			stmt.setFloat(19, beatmap.overallDifficulty);
			stmt.setFloat(20, beatmap.approachRate);
			stmt.setFloat(21, beatmap.sliderMultiplier);
			stmt.setFloat(22, beatmap.sliderTickRate);
			stmt.setInt(23, beatmap.bpmMin);
			stmt.setInt(24, beatmap.bpmMax);
			stmt.setInt(25, beatmap.endTime);
			stmt.setString(26, beatmap.audioFilename.getName());
			stmt.setInt(27, beatmap.audioLeadIn);
			stmt.setInt(28, beatmap.previewTime);
			stmt.setByte(29, beatmap.countdown);
			stmt.setString(30, beatmap.sampleSet);
			stmt.setFloat(31, beatmap.stackLeniency);
			stmt.setByte(32, beatmap.mode);
			stmt.setBoolean(33, beatmap.letterboxInBreaks);
			stmt.setBoolean(34, beatmap.widescreenStoryboard);
			stmt.setBoolean(35, beatmap.epilepsyWarning);
			stmt.setString(36, (beatmap.bg == null) ? null : beatmap.bg.getName());
			stmt.setString(37, beatmap.sliderBorderToString());
			stmt.setString(38, beatmap.timingPointsToString());
			stmt.setString(39, beatmap.breaksToString());
			stmt.setString(40, beatmap.comboToString());
			stmt.setString(41, beatmap.md5Hash);
			stmt.setDouble(42, beatmap.starRating);
			stmt.setLong(43, beatmap.dateAdded);
			stmt.setBoolean(44, beatmap.favorite);
			stmt.setInt(45, beatmap.playCount);
			stmt.setLong(46, beatmap.lastPlayed);
			stmt.setInt(47, beatmap.localMusicOffset);
			stmt.setString(48, (beatmap.video == null) ? null : beatmap.video.getName());
			stmt.setInt(49, beatmap.videoOffset);
		} catch (SQLException e) {
			throw e;
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	/**
	 * Loads beatmap fields from the database.
	 * @param beatmap the beatmap
	 * @param flag whether to load all fields (LOAD_ALL), non-array
	 *        fields (LOAD_NONARRAY), or array fields (LOAD_ARRAY)
	 */
	public static void load(Beatmap beatmap, int flag) {
		if (connection == null)
			return;

		try {
			selectStmt.setString(1, beatmap.getFile().getParentFile().getName());
			selectStmt.setString(2, beatmap.getFile().getName());
			ResultSet rs = selectStmt.executeQuery();
			if (rs.next()) {
				if ((flag & LOAD_NONARRAY) > 0)
					setBeatmapFields(rs, beatmap);
				if ((flag & LOAD_ARRAY) > 0)
					setBeatmapArrayFields(rs, beatmap);
			}
			rs.close();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to load Beatmap from database.", e, true);
		}
	}

	/**
	 * Loads Beatmap fields from the database in a batch.
	 * @param batch a list of beatmaps
	 * @param flag whether to load all fields (LOAD_ALL), non-array
	 *        fields (LOAD_NONARRAY), or array fields (LOAD_ARRAY)
	 */
	public static void load(List<Beatmap> batch, int flag) {
		if (connection == null)
			return;

		// batch size too small
		int size = batch.size();
		if (size < cacheSize * LOAD_BATCH_MIN_RATIO) {
			for (Beatmap beatmap : batch)
				load(beatmap, flag);
			return;
		}

		try (Statement stmt = connection.createStatement()) {
			// create map
			HashMap<String, HashMap<String, Beatmap>> map = new HashMap<String, HashMap<String, Beatmap>>();
			for (Beatmap beatmap : batch) {
				String parent = beatmap.getFile().getParentFile().getName();
				String name = beatmap.getFile().getName();
				HashMap<String, Beatmap> m = map.get(parent);
				if (m == null) {
					m = new HashMap<String, Beatmap>();
					map.put(parent, m);
				}
				m.put(name, beatmap);
			}

			// iterate through database to load beatmaps
			int count = 0;
			stmt.setFetchSize(100);
			String sql = "SELECT * FROM beatmaps";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String parent = rs.getString(1);
				HashMap<String, Beatmap> m = map.get(parent);
				if (m != null) {
					String name = rs.getString(2);
					Beatmap beatmap = m.get(name);
					if (beatmap != null) {
						try {
							if ((flag & LOAD_NONARRAY) > 0)
								setBeatmapFields(rs, beatmap);
							if ((flag & LOAD_ARRAY) > 0)
								setBeatmapArrayFields(rs, beatmap);
						} catch (SQLException e) {
							Log.error(String.format("Failed to load map '%s/%s' from database.", parent, name), e);
						}
						if (++count >= size)
							break;
					}
				}
			}
			rs.close();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to load beatmaps from database.", e, true);
		}
	}

	/**
	 * Sets all beatmap non-array fields using a given result set.
	 * @param rs the result set containing the fields
	 * @param beatmap the beatmap
	 * @throws SQLException
	 */
	private static void setBeatmapFields(ResultSet rs, Beatmap beatmap) throws SQLException {
		try {
			File dir = beatmap.getFile().getParentFile();
			beatmap.beatmapID = rs.getInt(4);
			beatmap.beatmapSetID = rs.getInt(5);
			beatmap.title = BeatmapParser.getDBString(rs.getString(6));
			beatmap.titleUnicode = BeatmapParser.getDBString(rs.getString(7));
			beatmap.artist = BeatmapParser.getDBString(rs.getString(8));
			beatmap.artistUnicode = BeatmapParser.getDBString(rs.getString(9));
			beatmap.creator = BeatmapParser.getDBString(rs.getString(10));
			beatmap.version = BeatmapParser.getDBString(rs.getString(11));
			beatmap.source = BeatmapParser.getDBString(rs.getString(12));
			beatmap.tags = BeatmapParser.getDBString(rs.getString(13));
			beatmap.hitObjectCircle = rs.getInt(14);
			beatmap.hitObjectSlider = rs.getInt(15);
			beatmap.hitObjectSpinner = rs.getInt(16);
			beatmap.HPDrainRate = rs.getFloat(17);
			beatmap.circleSize = rs.getFloat(18);
			beatmap.overallDifficulty = rs.getFloat(19);
			beatmap.approachRate = rs.getFloat(20);
			beatmap.sliderMultiplier = rs.getFloat(21);
			beatmap.sliderTickRate = rs.getFloat(22);
			beatmap.bpmMin = rs.getInt(23);
			beatmap.bpmMax = rs.getInt(24);
			beatmap.endTime = rs.getInt(25);
			beatmap.audioFilename = new File(dir, BeatmapParser.getDBString(rs.getString(26)));
			beatmap.audioLeadIn = rs.getInt(27);
			beatmap.previewTime = rs.getInt(28);
			beatmap.countdown = rs.getByte(29);
			beatmap.sampleSet = BeatmapParser.getDBString(rs.getString(30));
			beatmap.stackLeniency = rs.getFloat(31);
			beatmap.mode = rs.getByte(32);
			beatmap.letterboxInBreaks = rs.getBoolean(33);
			beatmap.widescreenStoryboard = rs.getBoolean(34);
			beatmap.epilepsyWarning = rs.getBoolean(35);
			String bg = rs.getString(36);
			if (bg != null)
				beatmap.bg = new File(dir, BeatmapParser.getDBString(bg));
			beatmap.sliderBorderFromString(rs.getString(37));
			beatmap.md5Hash = rs.getString(41);
			beatmap.starRating = rs.getDouble(42);
			beatmap.dateAdded = rs.getLong(43);
			beatmap.favorite = rs.getBoolean(44);
			beatmap.playCount = rs.getInt(45);
			beatmap.lastPlayed = rs.getLong(46);
			beatmap.localMusicOffset = rs.getInt(47);
			String video = rs.getString(48);
			if (video != null)
				beatmap.video = new File(dir, BeatmapParser.getDBString(video));
			beatmap.videoOffset = rs.getInt(49);
		} catch (SQLException e) {
			throw e;
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	/**
	 * Sets all Beatmap array fields using a given result set.
	 * @param rs the result set containing the fields
	 * @param beatmap the beatmap
	 * @throws SQLException
	 */
	private static void setBeatmapArrayFields(ResultSet rs, Beatmap beatmap) throws SQLException {
		try {
			beatmap.timingPointsFromString(rs.getString(38));
			beatmap.breaksFromString(rs.getString(39));
			beatmap.comboFromString(rs.getString(40));
		} catch (SQLException e) {
			throw e;
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	/**
	 * Returns a map of file paths ({dir}/{file}) to last modified map entries,
	 * or null if any error occurred.
	 */
	public static Map<String, LastModifiedMapEntry> getLastModifiedMap() {
		if (connection == null)
			return null;

		try (Statement stmt = connection.createStatement()) {
			Map<String, LastModifiedMapEntry> map = new HashMap<String, LastModifiedMapEntry>();
			String sql = "SELECT dir, file, lastModified, mode FROM beatmaps";
			ResultSet rs = stmt.executeQuery(sql);
			stmt.setFetchSize(100);
			while (rs.next()) {
				String path = String.format("%s/%s", rs.getString(1), rs.getString(2));
				long lastModified = rs.getLong(3);
				byte mode = rs.getByte(4);
				map.put(path, new LastModifiedMapEntry(lastModified, mode));
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
		if (connection == null)
			return;

		try {
			deleteMapStmt.setString(1, dir);
			deleteMapStmt.setString(2, file);
			cacheSize -= deleteMapStmt.executeUpdate();
			updateCacheSize();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to delete beatmap entry from database.", e, true);
		}
	}

	/**
	 * Deletes the beatmap group entry from the database.
	 * @param dir the directory
	 */
	public static void delete(String dir) {
		if (connection == null)
			return;

		try {
			deleteGroupStmt.setString(1, dir);
			cacheSize -= deleteGroupStmt.executeUpdate();
			updateCacheSize();
		} catch (SQLException e) {
			ErrorHandler.error("Failed to delete beatmap group entry from database.", e, true);
		}
	}

	/**
	 * Sets the star rating for a beatmap in the database.
	 * @param beatmap the beatmap
	 */
	public static void setStars(Beatmap beatmap) {
		if (connection == null)
			return;

		try {
			setStarsStmt.setDouble(1, beatmap.starRating);
			setStarsStmt.setString(2, beatmap.getFile().getParentFile().getName());
			setStarsStmt.setString(3, beatmap.getFile().getName());
			setStarsStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error(String.format("Failed to save star rating '%.4f' for beatmap '%s' in database.",
					beatmap.starRating, beatmap.toString()), e, true);
		}
	}

	/**
	 * Updates the play statistics for a beatmap in the database.
	 * @param beatmap the beatmap
	 */
	public static void updatePlayStatistics(Beatmap beatmap) {
		if (connection == null)
			return;

		try {
			updatePlayStatsStmt.setInt(1, beatmap.playCount);
			updatePlayStatsStmt.setLong(2, beatmap.lastPlayed);
			updatePlayStatsStmt.setString(3, beatmap.getFile().getParentFile().getName());
			updatePlayStatsStmt.setString(4, beatmap.getFile().getName());
			updatePlayStatsStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error(String.format("Failed to update play statistics for beatmap '%s' in database.",
					beatmap.toString()), e, true);
		}
	}

	/**
	 * Updates the "favorite" status for a beatmap in the database.
	 * @param beatmap the beatmap
	 */
	public static void updateFavoriteStatus(Beatmap beatmap) {
		if (connection == null)
			return;

		try {
			setFavoriteStmt.setBoolean(1, beatmap.favorite);
			setFavoriteStmt.setString(2, beatmap.getFile().getParentFile().getName());
			setFavoriteStmt.setString(3, beatmap.getFile().getName());
			setFavoriteStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error(String.format("Failed to update favorite status for beatmap '%s' in database.",
					beatmap.toString()), e, true);
		}
	}

	/**
	 * Updates the local music offset for a beatmap in the database.
	 * @param beatmap the beatmap
	 */
	public static void updateLocalOffset(Beatmap beatmap) {
		if (connection == null)
			return;

		try {
			setLocalOffsetStmt.setInt(1, beatmap.localMusicOffset);
			setLocalOffsetStmt.setString(2, beatmap.getFile().getParentFile().getName());
			setLocalOffsetStmt.setString(3, beatmap.getFile().getName());
			setLocalOffsetStmt.executeUpdate();
		} catch (SQLException e) {
			ErrorHandler.error(String.format("Failed to update local music offset for beatmap '%s' in database.",
					beatmap.toString()), e, true);
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
			selectStmt.close();
			deleteMapStmt.close();
			deleteGroupStmt.close();
			setStarsStmt.close();
			setStarsStmt.close();
			updatePlayStatsStmt.close();
			setFavoriteStmt.close();
			setLocalOffsetStmt.close();
			updateSizeStmt.close();
			connection.close();
			connection = null;
		} catch (SQLException e) {
			ErrorHandler.error("Failed to close beatmap database.", e, true);
		}
	}
}
