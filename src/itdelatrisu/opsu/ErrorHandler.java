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

import java.awt.Cursor;
import java.awt.Desktop;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

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
		desc  = "opsu! has encountered an error.",
		descR = "opsu! has encountered an error. Please report this!";

	/** Error popup button options. */
	private static final String[]
		options  = {"View Error Log", "Close"},
		optionsR = {"Send Report", "View Error Log", "Close"};

	/** Text area for Exception. */
	private static final JTextArea textArea = new JTextArea(7, 30);
	static {
		textArea.setEditable(false);
		textArea.setBackground(UIManager.getColor("Panel.background"));
		textArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		textArea.setTabSize(2);
		textArea.setLineWrap(true);
	}

	/** Scroll pane holding JTextArea. */
	private static final JScrollPane scroll = new JScrollPane(textArea);

	/** Error popup objects. */
	private static final Object[]
		message  = { desc, scroll },
		messageR = { descR, scroll };

	// This class should not be instantiated.
	private ErrorHandler() {}

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
			if (Desktop.isDesktopSupported()) {
				// try to open the log file and/or issues webpage
				if (report) {
					// ask to report the error
					int n = JOptionPane.showOptionDialog(null, messageR, title,
							JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
							null, optionsR, optionsR[2]);
					if (n == 0) {
						// auto-fill debug information
						String issueTitle = (error != null) ? error : e.getMessage();
						StringBuilder sb = new StringBuilder();
						Properties props = new Properties();
						props.load(ResourceLoader.getResourceAsStream(Options.VERSION_FILE));
						String version = props.getProperty("version");
						if (version != null && !version.equals("${pom.version}")) {
							sb.append("**Version:** ");
							sb.append(version);
							sb.append('\n');
						}
						String timestamp = props.getProperty("build.date");
						if (timestamp != null &&
						    !timestamp.equals("${maven.build.timestamp}") && !timestamp.equals("${timestamp}")) {
							sb.append("**Build date:** ");
							sb.append(timestamp);
							sb.append('\n');
						}
						sb.append("**OS:** ");
						sb.append(System.getProperty("os.name"));
						sb.append(" (");
						sb.append(System.getProperty("os.arch"));
						sb.append(")\n");
						sb.append("**JRE:** ");
						sb.append(System.getProperty("java.version"));
						sb.append('\n');
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
						URI uri = URI.create(String.format(Options.ISSUES_URL,
								URLEncoder.encode(issueTitle, "UTF-8"),
								URLEncoder.encode(sb.toString(), "UTF-8")));
						Desktop.getDesktop().browse(uri);
					} else if (n == 1)
						Desktop.getDesktop().open(Options.LOG_FILE);
				} else {
					// don't report the error
					int n = JOptionPane.showOptionDialog(null, message, title,
							JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
							null, options, options[1]);
					if (n == 0)
						Desktop.getDesktop().open(Options.LOG_FILE);
				}
			} else {
				// display error only
				JOptionPane.showMessageDialog(null, report ? messageR : message,
						title, JOptionPane.ERROR_MESSAGE);
			}
		} catch (Exception e1) {
			Log.error("Error opening crash popup.", e1);
		}
	}
}
