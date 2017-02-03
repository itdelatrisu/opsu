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

import itdelatrisu.opsu.options.Options;

import java.awt.Cursor;
import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.lwjgl.opengl.GL11;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * Error handler to log and display errors.
 */
public class ErrorHandler {
	/** Error popup title. */
	private static final String title = "Error";

	/** Error popup description text. */
	private static final String
		desc = "An error occurred. :(",
		descReport = "Something bad happened. Please report this!";

	/** Error popup button options. */
	private static final String[]
		optionsLog  = {"View Error Log", "Close"},
		optionsReport = {"Send Report", "Close"},
		optionsLogReport = {"Send Report", "View Error Log", "Close"};

	/** Text area for Exception. */
	private static final JTextArea textArea = new JTextArea(7, 30);
	static {
		textArea.setEditable(false);
		textArea.setBackground(UIManager.getColor("Panel.background"));
		textArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		textArea.setTabSize(2);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
	}

	/** Scroll pane holding JTextArea. */
	private static final JScrollPane scroll = new JScrollPane(textArea);

	/** Error popup objects. */
	private static final Object[]
		message  = { desc, scroll },
		messageReport = { descReport, scroll };

	/** OpenGL string (if any). */
	private static String glString = null;

	// This class should not be instantiated.
	private ErrorHandler() {}

	/**
	 * Sets the OpenGL version string.
	 */
	public static void setGlString() {
		try {
			String glVersion = GL11.glGetString(GL11.GL_VERSION);
			String glVendor = GL11.glGetString(GL11.GL_VENDOR);
			glString = String.format("%s (%s)", glVersion, glVendor);
		} catch (Exception e) {}
	}

	/**
	 * Displays an error popup and logs the given error.
	 * @param error a description of the error
	 * @param e the exception causing the error
	 * @param report whether to ask to report the error
	 */
	public static void error(String error, Throwable e, boolean report) {
		if (error == null && e == null)
			return;

		// log the error
		if (error == null)
			Log.error(e);
		else if (e == null)
			Log.error(error);
		else
			Log.error(error, e);

		// set the textArea to the error message
		textArea.setText(null);
		if (error != null) {
			textArea.append(error);
			textArea.append("\n");
		}
		String trace = null;
		if (e != null) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			trace = sw.toString();
			textArea.append(trace);
		}

		// display popup
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			Desktop desktop = null;
			boolean isBrowseSupported = false, isOpenSupported = false;
			if (Desktop.isDesktopSupported()) {
				desktop = Desktop.getDesktop();
				isBrowseSupported = desktop.isSupported(Desktop.Action.BROWSE);
				isOpenSupported = desktop.isSupported(Desktop.Action.OPEN);
			}
			if (desktop != null && (isOpenSupported || (report && isBrowseSupported))) {  // try to open the log file and/or issues webpage
				if (report && isBrowseSupported) {  // ask to report the error
					if (isOpenSupported) {  // also ask to open the log
						int n = JOptionPane.showOptionDialog(null, messageReport, title,
								JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
								null, optionsLogReport, optionsLogReport[2]);
						if (n == 0)
							desktop.browse(getIssueURI(error, e, trace));
						else if (n == 1)
							desktop.open(Options.LOG_FILE);
					} else {  // only ask to report the error
						int n = JOptionPane.showOptionDialog(null, message, title,
								JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
								null, optionsReport, optionsReport[1]);
						if (n == 0)
							desktop.browse(getIssueURI(error, e, trace));
					}
				} else {  // don't report the error
					int n = JOptionPane.showOptionDialog(null, message, title,
							JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
							null, optionsLog, optionsLog[1]);
					if (n == 0)
						desktop.open(Options.LOG_FILE);
				}
			} else {  // display error only
				JOptionPane.showMessageDialog(null, report ? messageReport : message,
						title, JOptionPane.ERROR_MESSAGE);
			}
		} catch (Exception e1) {
			Log.error("An error occurred in the crash popup.", e1);
		}
	}

	/**
	 * Returns the issue reporting URI with the given title and body.
	 * @param title the issue title
	 * @param body the issue body
	 * @return the encoded URI
	 */
	public static URI getIssueURI(String title, String body) {
		try {
			return URI.create(String.format(OpsuConstants.ISSUES_URL,
				URLEncoder.encode(title, "UTF-8"),
				URLEncoder.encode(body, "UTF-8"))
			);
		} catch (UnsupportedEncodingException e) {
			Log.warn("URLEncoder failed to encode the auto-filled issue report URL.");
			return URI.create(String.format(OpsuConstants.ISSUES_URL, "", ""));
		}
	}

	/**
	 * Returns environment information formatted in markdown (for issue reports).
	 */
	public static String getEnvironmentInfoForIssue() {
		StringBuilder sb = new StringBuilder();
		try {
			// read version and build date from version file, if possible
			Properties props = new Properties();
			props.load(ResourceLoader.getResourceAsStream(Options.VERSION_FILE));
			String version = props.getProperty("version");
			if (version != null && !version.equals("${pom.version}")) {
				sb.append("**Version:** ");
				sb.append(version);
				String hash = Utils.getGitHash();
				if (hash != null) {
					sb.append(" (");
					sb.append(hash.substring(0, 12));
					sb.append(')');
				}
				sb.append('\n');
			}
			String timestamp = props.getProperty("build.date");
			if (timestamp != null &&
			    !timestamp.equals("${maven.build.timestamp}") && !timestamp.equals("${timestamp}")) {
				sb.append("**Build date:** ");
				sb.append(timestamp);
				sb.append('\n');
			}
		} catch (IOException e1) {
			Log.warn("Could not read version file.", e1);
		}
		sb.append("**OS:** ");
		sb.append(System.getProperty("os.name"));
		sb.append(" (");
		sb.append(System.getProperty("os.arch"));
		sb.append(")\n");
		sb.append("**JRE:** ");
		sb.append(System.getProperty("java.version"));
		sb.append('\n');
		if (glString != null) {
			sb.append("**OpenGL version:** ");
			sb.append(glString);
			sb.append('\n');
		}
		return sb.toString();
	}

	/**
	 * Returns the issue reporting URI.
	 * This will auto-fill the report with the relevant information if possible.
	 * @param error a description of the error
	 * @param e the exception causing the error
	 * @param trace the stack trace
	 * @return the created URI
	 */
	private static URI getIssueURI(String error, Throwable e, String trace) {
		// generate report information
		String issueTitle = (error != null) ? error : (e.getMessage() != null) ? e.getMessage() : "null";
		StringBuilder sb = new StringBuilder();
		sb.append(getEnvironmentInfoForIssue());
		if (error != null) {
			sb.append("**Error:** `");
			sb.append(error);
			sb.append("`\n");
		}
		if (trace != null) {
			sb.append("**Stack trace:**");
			sb.append("\n```\n");
			sb.append(trace);
			sb.append("```");
		}

		// return auto-filled URI
		return getIssueURI(issueTitle, sb.toString());
	}
}
