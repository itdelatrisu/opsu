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

package itdelatrisu.opsu.downloads;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.OpsuConstants;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.downloads.Download.DownloadListener;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.UI;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.newdawn.slick.Color;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * Handles automatic program updates.
 */
public class Updater {
	/** The single instance of this class. */
	private static Updater updater = new Updater();

	/** The exit confirmation message. */
	public static final String EXIT_CONFIRMATION = String.format(
		"A new update is being downloaded.\nAre you sure you want to quit %s?",
		OpsuConstants.PROJECT_NAME
	);

	/**
	 * Returns the single instance of this class.
	 */
	public static Updater get() { return updater; }

	/** Updater status. */
	public enum Status {
		INITIAL (""),
		CHECKING ("Checking for updates..."),
		CONNECTION_ERROR ("Connection error."),
		INTERNAL_ERROR ("Internal error."),
		UP_TO_DATE ("Up to date!"),
		UPDATE_AVAILABLE ("Update available!\nClick to download."),
		UPDATE_DOWNLOADING ("Downloading update...") {
			@Override
			public String getDescription() {
				Download d = updater.download;
				if (d != null && d.getStatus() == Download.Status.DOWNLOADING) {
					return String.format("Downloading update...\n%.1f%% complete (%s/%s)",
							d.getProgress(), Utils.bytesToString(d.readSoFar()), Utils.bytesToString(d.contentLength()));
				} else
					return super.getDescription();
			}
		},
		UPDATE_DOWNLOADED ("Download complete.\nClick to restart."),
		UPDATE_FINAL ("Update queued.");

		/** The status description. */
		private final String description;

		/**
		 * Constructor.
		 * @param description the status description
		 */
		Status(String description) {
			this.description = description;
		}

		/**
		 * Returns the status description.
		 */
		public String getDescription() { return description; }
	};

	/** The current updater status. */
	private Status status;

	/** The current and latest versions. */
	private DefaultArtifactVersion currentVersion, latestVersion;

	/** The version information if the program was just updated. */
	private String updatedFromVersion, updatedToVersion;

	/** The build date. */
	private int buildDate = -1;

	/** The download object. */
	private Download download;

	/**
	 * Constructor.
	 */
	private Updater() {
		status = Status.INITIAL;
	}

	/**
	 * Returns the updater status.
	 */
	public Status getStatus() { return status; }

	/**
	 * Returns whether or not the updater button should be displayed.
	 */
	public boolean showButton() {
		return (status == Status.UPDATE_AVAILABLE || status == Status.UPDATE_DOWNLOADED || status == Status.UPDATE_DOWNLOADING);
	}

	/**
	 * Returns the build date, or the current date if not available.
	 */
	public int getBuildDate() {
		if (buildDate == -1) {
			Date date = null;
			try {
				Properties props = new Properties();
				props.load(ResourceLoader.getResourceAsStream(Options.VERSION_FILE));
				String build = props.getProperty("build.date");
				if (build == null || build.equals("${timestamp}") || build.equals("${maven.build.timestamp}"))
					date = new Date();
				else {
					DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
					date = format.parse(build);
				}
			} catch (Exception e) {
				date = new Date();
			} finally {
				DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
				buildDate = Integer.parseInt(dateFormat.format(date));
			}
		}
		return buildDate;
	}

	/**
	 * Sets the version information if the program was just updated.
	 * @param fromVersion the previous version
	 * @param toVersion the new version
	 */
	public void setUpdateInfo(String fromVersion, String toVersion) {
		this.updatedFromVersion = fromVersion;
		this.updatedToVersion = toVersion;
	}

	/**
	 * Returns whether or not the program was just updated.
	 */
	public boolean justUpdated() { return (updatedFromVersion != null && updatedToVersion != null); }

	/**
	 * Returns the version the program was just updated from, or null if not updated.
	 */
	public String updatedFromVersion() { return (justUpdated()) ? updatedFromVersion : null; }

	/**
	 * Returns the version the program was just updated to, or null if not updated.
	 */
	public String updatedToVersion() { return (justUpdated()) ? updatedToVersion : null; }

	/**
	 * Loads and returns the current program version.
	 * @return the version string, or {@code null} if unable to determine the version
	 */
	public String getCurrentVersion() {
		if (currentVersion == null) {
			try {
				Properties props = new Properties();
				props.load(ResourceLoader.getResourceAsStream(Options.VERSION_FILE));
				if ((currentVersion = getVersion(props)) == null)
					return null;
			} catch (IOException e) {
				return null;
			}
		}
		return currentVersion.toString();
	}

	/**
	 * Returns the version from a set of properties.
	 * @param props the set of properties
	 * @return the version, or null if not found
	 */
	private DefaultArtifactVersion getVersion(Properties props) {
		String version = props.getProperty("version");
		if (version == null || version.equals("${pom.version}"))
			return null;
		else
			return new DefaultArtifactVersion(version);
	}

	/**
	 * Checks the program version against the version file on the update server.
	 * @throws IOException if an I/O exception occurs
	 */
	public void checkForUpdates() throws IOException {
		if (status != Status.INITIAL || Options.USE_XDG)
			return;

		status = Status.CHECKING;

		// get current version
		if (currentVersion == null) {
			Properties props = new Properties();
			props.load(ResourceLoader.getResourceAsStream(Options.VERSION_FILE));
			if ((currentVersion = getVersion(props)) == null) {
				status = Status.INTERNAL_ERROR;
				return;
			}
		}

		// get latest version
		String s = null;
		try {
			s = Utils.readDataFromUrl(new URL(OpsuConstants.VERSION_REMOTE));
		} catch (UnknownHostException e) {
			Log.warn(String.format(
				"Check for updates failed. Please check your internet connection, or your connection to %s.",
				OpsuConstants.VERSION_REMOTE)
			);
		}
		if (s == null) {
			status = Status.CONNECTION_ERROR;
			return;
		}
		Properties props = new Properties();
		props.load(new StringReader(s));
		if ((latestVersion = getVersion(props)) == null) {
			status = Status.INTERNAL_ERROR;
			return;
		}

		// compare versions
		if (latestVersion.compareTo(currentVersion) <= 0)
			status = Status.UP_TO_DATE;
		else {
			String updateURL = props.getProperty("file");
			if (updateURL == null) {
				status = Status.INTERNAL_ERROR;
				return;
			}
			status = Status.UPDATE_AVAILABLE;
			String localPath = String.format("%s%copsu-update-%s",
					System.getProperty("user.dir"), File.separatorChar, latestVersion.toString());
			String rename = String.format("opsu-%s.jar", latestVersion.toString());
			download = new Download(updateURL, localPath, rename);
			download.setListener(new DownloadListener() {
				@Override
				public void completed() {
					status = Status.UPDATE_DOWNLOADED;
					UI.getNotificationManager().sendNotification("Update has finished downloading.", Colors.GREEN);
				}

				@Override
				public void error() {
					status = Status.CONNECTION_ERROR;
					UI.getNotificationManager().sendNotification("Update failed due to a connection error.", Color.red);
				}
			});
		}
	}

	/**
	 * Starts the download, if available.
	 */
	public void startDownload() {
		if (status != Status.UPDATE_AVAILABLE || download == null || download.getStatus() != Download.Status.WAITING)
			return;

		status = Status.UPDATE_DOWNLOADING;
		download.start();
	}

	/**
	 * Prepares to run the update when the application closes.
	 */
	public void prepareUpdate() {
		if (status != Status.UPDATE_DOWNLOADED || download == null || download.getStatus() != Download.Status.COMPLETE)
			return;

		status = Status.UPDATE_FINAL;
	}

	/**
	 * Hands over execution to the updated file, if available.
	 */
	public void runUpdate() {
		if (status != Status.UPDATE_FINAL)
			return;

		try {
			// TODO: it is better to wait for the process? is this portable?
			ProcessBuilder pb = new ProcessBuilder(
					"java", "-jar", download.getLocalPath(),
					currentVersion.toString(), latestVersion.toString()
			);
			pb.start();
		} catch (IOException e) {
			status = Status.INTERNAL_ERROR;
			ErrorHandler.error("Failed to start new process.", e, true);
		}
	}
}
