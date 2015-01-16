/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014 Jeffrey Han
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

import itdelatrisu.opsu.states.Options;

import java.awt.Cursor;
import java.awt.Desktop;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.newdawn.slick.util.Log;

/**
 * Error handler to log and display errors.
 */
public class ErrorHandler {
	/**
	 * Error popup title.
	 */
	private static final String title = "Error";

	/**
	 * Error popup description text.
	 */
	private static final String
		desc = "opsu! has encountered an error.",
		descR = "opsu! has encountered an error. Please report this!";

	/**
	 * Error popup button options.
	 */
	private static final String[]
		options = {"View Error Log", "Close"},
		optionsR = {"Send Report", "View Error Log", "Close"};

	/**
	 * Text area for Exception.
	 */
	private static final JTextArea textArea = new JTextArea(7, 30);
	static {
		textArea.setEditable(false);
		textArea.setBackground(UIManager.getColor("Panel.background"));
		textArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		textArea.setTabSize(2);
		textArea.setLineWrap(true);
	}

	/**
	 * Scroll pane holding JTextArea.
	 */
	private static final JScrollPane scroll = new JScrollPane(textArea);

	/**
	 * Error popup objects.
	 */
	private static final Object[]
		message = { desc, scroll },
		messageR = { descR, scroll };

	/**
	 * Address to report issues.
	 */
	private static URI uri;
	static {
		try {
			uri = new URI("https://github.com/itdelatrisu/opsu/issues/new");
		} catch (URISyntaxException e) {
			Log.error("Problem with error URI.", e);
		}
	}

	// This class should not be instantiated.
	private ErrorHandler() {}

	/**
	 * Displays an error popup and logs the given error.
	 * @param error a descR of the error
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
		if (e != null) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			textArea.append(sw.toString());
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
						Desktop.getDesktop().browse(uri);
						Desktop.getDesktop().open(Options.LOG_FILE);
					} else if (n == 1)
						Desktop.getDesktop().open(Options.LOG_FILE);
				} else {
					// don't report the error
					int n = JOptionPane.showOptionDialog(null, message, title,
							JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
							null, options, optionsR[1]);
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
