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

import itdelatrisu.opsu.OpsuConstants;
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
 * An error report comprised of sections of technical details. Code sections may
 * use this reporting system to provide more details about why a certain error
 * has occurred.
 * 
 * @author Lyonlancer5
 */
public class ErrorReport {

	/** Information about the system and current running environment */
	private static ErrorReportCategory environmentInfo;

	/** The description of the error */
	private final String description;
	/** The cause of the error */
	private final Throwable cause;
	/** The list of sections which detail specific parts of the error */
	private final List<ErrorReportCategory> errorInformation;

	/**
	 * Constructs a new error report.
	 * 
	 * @param description
	 *            A short description of the error
	 * @param cause
	 *            The cause of the error
	 */
	public ErrorReport(String description, Throwable cause) {
		this.description = description;
		this.cause = cause;
		this.errorInformation = new ArrayList<ErrorReportCategory>();
	}

	/**
	 * Populates the information sheet with the system/environment details and
	 * caches it for later use.
	 */
	public static ErrorReportCategory getEnvironmentInfo() {
		if (environmentInfo == null) {
			environmentInfo = new ErrorReportCategory("System Information");

			// opsu! version running
			environmentInfo.addSection("Game Version", new Callable<String>() {
				@Override
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
			environmentInfo.addSection("Operating System", new Callable<String>() {
				@Override
				public String call() throws Exception {
					return System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version "
							+ System.getProperty("os.version");
				}
			});

			// Java Version details
			environmentInfo.addSection("Java Version", new Callable<String>() {
				public String call() {
					return System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
				}
			});

			// Java VM details
			environmentInfo.addSection("Java VM Details", new Callable<String>() {
				@Override
				public String call() throws Exception {
					return System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), "
							+ System.getProperty("java.vm.vendor");
				}
			});

			// OpenGL version
			environmentInfo.addSection("OpenGL Version", new Callable<String>() {
				@Override
				public String call() throws Exception {
					return GL11.glGetString(GL11.GL_VERSION);
				}
			});

			// OpenGL renderer
			environmentInfo.addSection("OpenGL Renderer", new Callable<String>() {
				@Override
				public String call() throws Exception {
					return GL11.glGetString(GL11.GL_RENDERER);
				}
			});

			// OpenGL vendor (manufacturer of the graphics card)
			environmentInfo.addSection("OpenGL Vendor", new Callable<String>() {
				@Override
				public String call() throws Exception {
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
		List<ErrorReportCategory.Section> envSections = environmentInfo.getSections();
		StringBuilder builder = new StringBuilder();
		builder.append("**Version:** ");
		builder.append(envSections.get(0).getValue());
		builder.append("\n**OS:** ");
		builder.append(envSections.get(1).getValue());
		builder.append("\n**JRE:** ");
		builder.append(envSections.get(2).getValue());
		builder.append("\n**GL Version:** ");
		builder.append(envSections.get(5).getValue());
		return builder.toString();
	}

	/**
	 * Gets the description of the error
	 * 
	 * @return the error description
	 */
	public String getDescription() {
		if (description != null)
			return description;

		if (getCause() != null)
			return getCause().getMessage();

		return "No description.";
	}

	/**
	 * Gets the throwable which caused the error
	 * 
	 * @return the error cause
	 */
	public Throwable getCause() {
		return cause;
	}

	/**
	 * Adds a category to this error report
	 * 
	 * @param info
	 *            the category to add.
	 */
	public void addInfo(ErrorReportCategory info) {
		errorInformation.add(info);
	}

	/**
	 * Writes all information sections to the specified string builder
	 * 
	 * @param builder
	 *            A {@link StringBuilder} instance
	 */
	public void appendSections(StringBuilder builder) {
		for (ErrorReportCategory info : errorInformation) {
			builder.append(info);
			builder.append("\n\n");
		}

		builder.append(getEnvironmentInfo());
	}

	/**
	 * Encodes the cause's stack trace to a string
	 * 
	 * @return a stack trace, in string form
	 */
	public String getCauseTrace() {
		if (cause != null) {
			// Write the stack trace to a string writer
			StringWriter writer = new StringWriter();
			cause.printStackTrace(new PrintWriter(writer));
			return writer.toString();
		}

		return "No stack trace for the cause is available.";
	}

	/**
	 * Compiles the report to a single string instance
	 * 
	 * @return The complete error report
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		// Header
		builder.append("---- " + OpsuConstants.PROJECT_NAME + " Error Report ----");
		builder.append("\n\n");
		// Timestamp
		builder.append("Timestamp: ");
		builder.append((new SimpleDateFormat()).format(new Date()));
		builder.append("\n");
		// Error description
		builder.append("Description: ");
		builder.append(getDescription());
		builder.append("\n\n");
		// stack trace
		builder.append(getCauseTrace());

		// Additional information
		builder.append("\n\nAdditional information:\n");
		builder.append("----------------------------------------------------------------");
		builder.append("\n\n");
		appendSections(builder);
		return builder.toString();
	}

	/**
	 * Writes this error report to a separate file and returns it. If the report
	 * was not written, return the log.
	 */
	public File writeToFile() {
		File report = new File(Options.ERROR_REPORTS_DIR,
				"error-report-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + ".txt");
		try {
			report.getParentFile().mkdirs();
			FileWriter writer = new FileWriter(report);
			writer.write(toString());
			writer.flush();
			writer.close();
		} catch (Exception e) {
			Log.error("Cannot save error report");
			report = Options.LOG_FILE;
		}
		return report;
	}

	/**
	 * Posts the entire error report to
	 * <a href="https://hastebin.com/about.md">hastebin</a> to avoid cluttering
	 * the GitHub reporting system.
	 * 
	 * @return A formatted response for use in the error overview.
	 * @see #getOverview()
	 */
	public String haste() {
		try {
			byte[] post = toString().getBytes(StandardCharsets.UTF_8);
			HttpsURLConnection.setFollowRedirects(false);
			// Open connection to hastebin's documents and POST the report
			HttpsURLConnection conn = (HttpsURLConnection) new URL("https://hastebin.com/documents").openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.setRequestProperty("Content-Type", "text/plain");
			conn.setRequestProperty("Content-Length", String.valueOf(post.length));

			// Write the POST request
			conn.setDoOutput(true);
			OutputStream os = conn.getOutputStream();
			os.write(post);
			os.flush();
			os.close();

			if (conn.getResponseCode() != 200)
				return "Remote server responded with \'" + conn.getResponseMessage() + "\'.";

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String reply = reader.readLine();

			// Parse the JSON response manually
			// Sample -> {"key":"abcdefghi"}
			String[] split = reply.replaceAll("\\\"", "").split(":");
			reply = split[1].substring(0, split[1].length() - 1);
			return "The full report has been posted [here](https://hastebin.com/" + reply + ")";
		} catch (IOException e) {
			Log.error("Cannot upload error report", e);
			return "Cannot upload the full error report.";
		}
	}

	/**
	 * Returns a URL-friendly overview of the error report.
	 * 
	 * @return The error overview with the pastebin link
	 * @see #haste()
	 */
	public String getOverview() {
		StringBuilder builder = new StringBuilder();
		builder.append(getEnvironmentInfoString());
		builder.append("\n**Error:** ");
		builder.append(getDescription());
		if (getCause() != null) {
			builder.append("\n**Stack trace:**\n ");
			builder.append("```\n");
			builder.append(getCauseTrace());
			builder.append("\n```");
		}

		builder.append("\n\n-----\n\n");
		builder.append(haste());

		return builder.toString();
	}
}
