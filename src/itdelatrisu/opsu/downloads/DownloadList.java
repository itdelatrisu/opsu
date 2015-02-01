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

package itdelatrisu.opsu.downloads;

import itdelatrisu.opsu.ErrorHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * Maintains the current downloads list.
 */
public class DownloadList {
	/** The single instance of this class. */
	private static DownloadList list = new DownloadList();

	/** Current list of downloads. */
	private List<DownloadNode> nodes;

	/** The map of beatmap set IDs to DownloadNodes for the current downloads. */
	private Map<Integer, DownloadNode> map;

	/**
	 * Returns the single instance of this class.
	 */
	public static DownloadList get() { return list; }

	/**
	 * Constructor.
	 */
	private DownloadList() {
		nodes = new ArrayList<DownloadNode>();
		map = new HashMap<Integer, DownloadNode>();
	}

	/**
	 * Returns the DownloadNode at an index.
	 */
	public DownloadNode getNode(int index) { return nodes.get(index); }

	/**
	 * Gets the Download for a beatmap set ID, or null if not in the list.
	 */
	public Download getDownload(int beatmapSetID) {
		DownloadNode node = map.get(beatmapSetID);
		return (node == null) ? null : node.getDownload();
	}

	/**
	 * Returns the size of the doownloads list.
	 */
	public int size() { return nodes.size(); }

	/**
	 * Returns {@code true} if this list contains no elements.
	 */
	public boolean isEmpty() { return nodes.isEmpty(); }

	/**
	 * Returns {@code true} if this list contains the beatmap set ID.
	 */
	public boolean contains(int beatmapSetID) { return map.containsKey(beatmapSetID); }

	/**
	 * Adds a DownloadNode to the list.
	 */
	public void addNode(DownloadNode node) {
		nodes.add(node);
		map.put(node.getID(), node);
	}

	/**
	 * Removes a DownloadNode from the list.
	 */
	public void remove(DownloadNode node) { remove(nodes.indexOf(node)); }

	/**
	 * Removes a DownloadNode from the list at the given index.
	 */
	public void remove(int index) {
		DownloadNode node = nodes.remove(index);
		map.remove(node.getID());
	}

	/**
	 * Returns {@code true} if the list contains any downloads that are active.
	 */
	public boolean hasActiveDownloads() {
		for (DownloadNode node: nodes) {
			Download dl = node.getDownload();
			if (dl != null && dl.isActive())
				return true;
		}
		return false;
	}

	/**
	 * Cancels all downloads.
	 */
	public void cancelAllDownloads() {
		for (DownloadNode node : nodes) {
			Download dl = node.getDownload();
			if (dl != null && dl.isActive())
				dl.cancel();
		}
	}

	/**
	 * Shows a confirmation dialog (used before exiting the game).
	 * @return true if user selects "yes", false otherwise
	 */
	public static boolean showExitConfirmation() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			ErrorHandler.error("Could not set system look and feel for DownloadList.", e, true);
		}
		int n = JOptionPane.showConfirmDialog(null,
				"Beatmap downloads are in progress.\nAre you sure you want to quit opsu!?",
				"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		return (n != JOptionPane.YES_OPTION);
	}
}
