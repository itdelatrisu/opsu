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

import itdelatrisu.opsu.OpsuConstants;
import itdelatrisu.opsu.downloads.Download.Status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Maintains the current downloads list.
 */
public class DownloadList {
	/** The single instance of this class. */
	private static DownloadList list = new DownloadList();

	/** The exit confirmation message. */
	public static final String EXIT_CONFIRMATION = String.format(
		"Beatmap downloads are in progress.\nAre you sure you want to quit %s?",
		OpsuConstants.PROJECT_NAME
	);

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
	 * Returns the DownloadNode at an index, or null if the index is out of bounds.
	 */
	public DownloadNode getNode(int index) {
		try {
			return nodes.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Gets the Download for a beatmap set ID, or null if not in the list.
	 */
	public Download getDownload(int beatmapSetID) {
		DownloadNode node = map.get(beatmapSetID);
		return (node == null) ? null : node.getDownload();
	}

	/**
	 * Returns the size of the downloads list.
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
		try {
			DownloadNode node = nodes.remove(index);
			map.remove(node.getID());
		} catch (IndexOutOfBoundsException e) {}
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
	 * Removes all inactive downloads from the list.
	 */
	public void clearInactiveDownloads() {
		Iterator<DownloadNode> iter = nodes.iterator();
		while (iter.hasNext()) {
			DownloadNode node = iter.next();
			Download dl = node.getDownload();
			if (dl != null && !dl.isActive()) {
				node.clearDownload();
				iter.remove();
				map.remove(node.getID());
			}
		}
	}

	/**
	 * Removes all downloads with the given status from the list.
	 * @param status the download status
	 */
	public void clearDownloads(Status status) {
		Iterator<DownloadNode> iter = nodes.iterator();
		while (iter.hasNext()) {
			DownloadNode node = iter.next();
			Download dl = node.getDownload();
			if (dl != null && dl.getStatus() == status) {
				node.clearDownload();
				iter.remove();
				map.remove(node.getID());
			}
		}
	}
}
