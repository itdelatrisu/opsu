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
import itdelatrisu.opsu.options.Options;

import java.awt.Cursor;
import java.awt.Desktop;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.newdawn.slick.util.Log;

/**
 * Error handler to log and display errors.
 */
public class ErrorHandler {
	/** Error popup title. */
	private static final String title = "opsu! encountered an error";

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
	private static final JTextArea textArea = new JTextArea(16, 50);
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

		CrashReport crash = new CrashReport(error, e);
		error(crash, report);
	}
	
	public static void error(CrashReport crash, boolean report) {
		// log the error
		Log.error(crash.toString());

		// set the textArea to the error message
		textArea.setText(crash.toString());
				
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
							desktop.browse(getIssueURI(crash.getCrashDescription(), crash));
						else if (n == 1)
							desktop.open(Options.LOG_FILE);
					} else {  // only ask to report the error
						int n = JOptionPane.showOptionDialog(null, message, title,
								JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
								null, optionsReport, optionsReport[1]);
						if (n == 0)
							desktop.browse(getIssueURI(crash.getCrashDescription(), crash));
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
	 * Returns the issue reporting URI.
	 * This will auto-fill the report with the relevant information if possible.
	 * @param error a description of the error
	 * @param e the exception causing the error
	 * @param trace the stack trace
	 * @return the created URI
	 */
	private static URI getIssueURI(String error, CrashReport report) {
		// generate report information
		String issueTitle = (error != null) ? error 
				: (!report.getCrashDescription().isEmpty()) ? report.getCrashDescription() : "null";

		// return auto-filled URI
		return getIssueURI(issueTitle, report.toString());
	}
}
