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

package itdelatrisu.opsu.crash;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.options.Options;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;

import org.lwjgl.opengl.GL11;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * A more elaborate crash reporting system where technical details are provided.
 * Code sections may use this reporting system to provide more details about why
 * a certain error has occurred.
 * 
 * @author Lyonlancer5
 */
public class CrashReport {

	/** Information about the system and current running environment */
	private static CrashInfo environmentInfo;

	/** The description of the crash */
	private final String description;
	/** The cause of the crash */
	private final Throwable cause;
	/** The list of sections which detail specific parts of the crash */
	private final List<CrashInfo> crashInformation;

	/** Stack trace of the crash */
	private StackTraceElement[] stacktrace = new StackTraceElement[0];

	/**
	 * Constructs a new crash report.
	 * 
	 * @param description
	 *            A short description of the crash
	 * @param cause
	 *            The cause of the crash
	 */
	public CrashReport(String description, Throwable cause) {
		this.description = description;
		this.cause = cause;
		this.crashInformation = new ArrayList<CrashInfo>();
	}

	/**
	 * Populates the information sheet with the system/environment details and
	 * caches it for later use.
	 */
	public static CrashInfo getEnvironmentInfo() {
		if (environmentInfo == null) {
			environmentInfo = new CrashInfo("System Information");

			// opsu! version running
			environmentInfo.addSectionSafe("Game Version", new Callable<String>() {
				public String call() throws Exception {
					StringBuilder builder = new StringBuilder();
					Properties props = new Properties();
					props.load(ResourceLoader.getResourceAsStream(Options.VERSION_FILE));

					String version = props.getProperty("version");
					if (version != null && !version.equals("${pom.version}")) {
						builder.append(version);
						String hash = Utils.getGitHash();
						if (hash != null) {
							builder.append(" (");
							builder.append(hash.substring(0, 12));
							builder.append(")");
						}
					}

					String timestamp = props.getProperty("build.date");
					if (timestamp != null && !timestamp.equals("${maven.build.timestamp}")
							&& !timestamp.equals("${timestamp}")) {
						builder.append(" - built on ");
						builder.append(timestamp);
					}

					return builder.toString();
				}
			});

			// Operating system details
			environmentInfo.addSectionSafe("Operating System", new Callable<String>() {
				public String call() {
					return System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version "
							+ System.getProperty("os.version");
				}
			});

			// Java Version details
			environmentInfo.addSectionSafe("Java Version", new Callable<String>() {
				public String call() {
					return System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
				}
			});

			// Java VM details
			environmentInfo.addSectionSafe("Java VM Details", new Callable<String>() {
				public String call() {
					return System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), "
							+ System.getProperty("java.vm.vendor");
				}
			});

			// OpenGL version
			environmentInfo.addSectionSafe("OpenGL Version", new Callable<String>() {
				public String call() {
					return GL11.glGetString(GL11.GL_VERSION);
				}
			});

			// OpenGL renderer
			environmentInfo.addSectionSafe("OpenGL Renderer", new Callable<String>() {
				public String call() {
					return GL11.glGetString(GL11.GL_RENDERER);
				}
			});

			// OpenGL vendor (manufacturer of the graphics card)
			environmentInfo.addSectionSafe("OpenGL Vendor", new Callable<String>() {
				public String call() {
					return GL11.glGetString(GL11.GL_VENDOR);
				}
			});
		}
		return environmentInfo;
	}

	/**
	 * Returns the environment details as an overview, used for automatic report
	 * generation.
	 */
	public static String getEnvironmentInfoString() {
		List<CrashInfo.Section> envSections = environmentInfo.getSections();
		StringBuilder builder = new StringBuilder();
		builder.append("**Version:** ");
		builder.append(envSections.get(0).value);
		builder.append("\n**OS:** ");
		builder.append(envSections.get(1).value);
		builder.append("\n**JRE:** ");
		builder.append(envSections.get(2).value);
		builder.append("\n**GL Version:** ");
		builder.append(envSections.get(5).value);
		return builder.toString();
	}

	/**
	 * Gets the description of the crash
	 * 
	 * @return the crash description
	 */
	public String getCrashDescription() {
		return description != null ? description : (getCrashCause() != null ? getCrashCause().getMessage() : "null");
	}

	/**
	 * Gets the throwable which caused the crash
	 * 
	 * @return the crash cause
	 */
	public Throwable getCrashCause() {
		return cause;
	}

	public void addCrashInfo(CrashInfo info) {
		crashInformation.add(info);
	}

	/**
	 * Writes all crash information sections to the specified string builder
	 * 
	 * @param builder
	 *            A {@link StringBuilder} instance
	 */
	public void appendSections(StringBuilder builder) {
		if ((stacktrace == null || stacktrace.length <= 0) && crashInformation.size() > 0)
			stacktrace = crashInformation.get(0).getStackTrace();

		if (stacktrace != null && stacktrace.length > 0) {
			builder.append("-- Head --\n");
			builder.append("Stack trace:\n");
			for (StackTraceElement ste : stacktrace)
				builder.append("\tat").append(ste.toString()).append("\n");

			builder.append("\n");
		}

		for (CrashInfo info : crashInformation) {
			info.appendTo(builder);
			builder.append("\n\n");
		}

		getEnvironmentInfo().appendTo(builder);
	}

	/**
	 * Encodes the cause's stack trace to a string
	 * 
	 * @return a stack trace, in string form
	 */
	public String getCauseTrace() {
		Throwable traced = this.cause;
		if (traced != null) {
			StringWriter stringwriter = null;
			PrintWriter printwriter = null;
			if (traced.getMessage() == null) {
				if (traced instanceof NullPointerException)
					traced = new NullPointerException(this.description);
				else if (traced instanceof StackOverflowError)
					traced = new StackOverflowError(this.description);
				else if (traced instanceof OutOfMemoryError)
					traced = new OutOfMemoryError(this.description);

				traced.setStackTrace(this.cause.getStackTrace());
			}

			String causeTrace = traced.toString();

			try {
				stringwriter = new StringWriter();
				printwriter = new PrintWriter(stringwriter);
				traced.printStackTrace(printwriter);
				causeTrace = stringwriter.toString();
			} finally {
				try {
					stringwriter.close();
				} catch (IOException e) {
				}
			}

			return causeTrace;
		}

		return "No stack trace for the cause is available";
	}

	/**
	 * Compiles the report to a single string instance
	 * 
	 * @return The complete crash report
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		// Header
		builder.append("---- opsu! ~ Error Report ----");
		builder.append("\n\n");
		// Timestamp
		builder.append("Timestamp: ");
		builder.append((new SimpleDateFormat()).format(new Date()));
		builder.append("\n");
		// Crash description
		builder.append("Description: ");
		builder.append(getCrashDescription());
		builder.append("\n\n");
		// stack trace
		builder.append(getCauseTrace());

		// crash information
		builder.append("\n\nRelevant details about the error is listed below:\n");
		for (int i = 0; i < 87; i++)
			builder.append("-");

		builder.append("\n\n");
		appendSections(builder);
		return builder.toString();
	}

	/**
	 * Writes this crash report to a separate file and returns it. If the report
	 * was not written, return the log.
	 */
	public File writeToFile() {
		File report = new File(Options.CRASH_REPORT_DIR,
				"crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + ".txt");
		report.getParentFile().mkdirs();
		try {
			FileWriter writer = new FileWriter(report);
			writer.write(toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			Log.error("Cannot save crash report");
			report = Options.LOG_FILE;
		}
		return report;
	}

	/**
	 * Posts the entire crash report to
	 * <a href="https://hastebin.com/about.md">hastebin</a> to avoid cluttering
	 * the GitHub reporting system.
	 * 
	 * @return A formatted response for use in the crash overview.
	 * @see #getOverview()
	 */
	public String haste() {
		String reply = "";
		try {
			byte[] post = toString().getBytes(StandardCharsets.UTF_8);
			HttpsURLConnection.setFollowRedirects(false);
			HttpsURLConnection conn = (HttpsURLConnection) new URL("https://hastebin.com/documents").openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.setRequestProperty("Content-Type", "text/plain");
			conn.setRequestProperty("Content-Length", String.valueOf(post.length));

			conn.setDoOutput(true);
			OutputStream os = conn.getOutputStream();
			os.write(post);
			os.flush();
			os.close();

			if (conn.getResponseCode() != 200)
				return "Remote server responded with \'" + conn.getResponseMessage() + "\'";

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			reply = reader.readLine();

			String[] split = reply.replaceAll("\\\"", "").split(":");
			reply = split[1].substring(0, split[1].length() - 1);
			return "The full report has been posted [here](https://hastebin.com/" + reply + ")";
		} catch (IOException e) {
			Log.error("Cannot upload crash report", e);
			return "Cannot upload the full crash report.";
		}
	}

	/**
	 * Returns a URL-friendly overview of the crash report.
	 * 
	 * @return The crash overview with the pastebin link
	 * @see #haste()
	 */
	public String getOverview() {
		StringBuilder builder = new StringBuilder();
		builder.append(getEnvironmentInfoString());
		builder.append("\n**Error:** ");
		builder.append(description);
		if (getCrashCause() != null) {
			builder.append("\n**Exception Message:** ");
			builder.append(getCrashCause().getMessage());
		}

		builder.append("\n\n-----\n\n");
		builder.append(haste());

		return builder.toString();
	}
}
