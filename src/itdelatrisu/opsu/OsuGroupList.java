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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Indexed, expanding, doubly-linked list data type for song groups.
 */
public class OsuGroupList {
	/**
	 * Sorting orders.
	 */
	public static final byte
		SORT_TITLE   = 0,
		SORT_ARTIST  = 1,
		SORT_CREATOR = 2,
		SORT_BPM     = 3,
		SORT_MAX     = 4;    // not a sort

	/**
	 * Sorting order names (indexed by SORT_* constants).
	 */
	public static final String[] SORT_NAMES = {
		"Title", "Artist", "Creator", "BPM"
	};

	/**
	 * List containing all parsed nodes.
	 */
	private ArrayList<OsuGroupNode> parsedNodes;

	/**
	 * Total number of maps (i.e. OsuFile objects).
	 */
	private int mapCount = 0;

	/**
	 * Current list of nodes.
	 * (For searches; otherwise, a pointer to parsedNodes.)
	 */
	private ArrayList<OsuGroupNode> nodes;

	/**
	 * Index of current expanded node.
	 * If no node is expanded, the value will be -1.
	 */
	private int expandedIndex = -1;

	/**
	 * The last search query.
	 */
	private String lastQuery = "";
	
	/**
	 * Constructor.
	 */
	public OsuGroupList() {
		parsedNodes = new ArrayList<OsuGroupNode>();
		nodes = parsedNodes;
	}

	/**
	 * Returns the number of elements.
	 */
	public int size() { return nodes.size(); }

	/**
	 * Adds a song group.
	 * @param osuFiles the list of OsuFile objects in the group
	 */
	public void addSongGroup(ArrayList<OsuFile> osuFiles) {
		parsedNodes.add(new OsuGroupNode(osuFiles));
		mapCount += osuFiles.size();
	}

	/**
	 * Returns the total number of parsed maps (i.e. OsuFile objects).
	 */
	public int getMapCount() { return mapCount; }

	/**
	 * Returns the OsuGroupNode at an index, disregarding expansions.
	 */
	public OsuGroupNode getBaseNode(int index) {
		if (index < 0 || index >= size())
			return null;

		return nodes.get(index);
	}

	/**
	 * Returns a random base node.
	 */
	public OsuGroupNode getRandomNode() {
		OsuGroupNode node = getBaseNode((int) (Math.random() * size()));
		if (node != null && node.index == expandedIndex)  // don't choose an expanded group node
			node = node.next;
		return node;
	}

	/**
	 * Returns the OsuGroupNode a given number of positions forward or backwards.
	 * @param node the starting node
	 * @param shift the number of nodes to shift forward (+) or backward (-).
	 */
	public OsuGroupNode getNode(OsuGroupNode node, int shift) {
		OsuGroupNode startNode = node;
		if (shift > 0) {
			for (int i = 0; i < shift && startNode != null; i++)
				startNode = startNode.next;
		} else {
			for (int i = 0; i < shift && startNode != null; i++)
				startNode = startNode.prev;
		}
		return startNode;
	}

	/**
	 * Returns the index of the expanded node (or -1 if nothing is expanded).
	 */
	public int getExpandedIndex() { return expandedIndex; }

	/**
	 * Expands the node at an index by inserting a new node for each OsuFile
	 * in that node and hiding the group node.
	 * @return the first of the newly-inserted nodes
	 */
	public OsuGroupNode expand(int index) {
		// undo the previous expansion
		unexpand();

		OsuGroupNode node = getBaseNode(index);
		if (node == null)
			return null;

		OsuGroupNode firstInserted = null;

		// create new nodes
		ArrayList<OsuFile> osuFiles = node.osuFiles;
		OsuGroupNode prevNode = node.prev;
		OsuGroupNode nextNode = node.next;
		for (int i = 0; i < node.osuFiles.size(); i++) {
			OsuGroupNode newNode = new OsuGroupNode(osuFiles);
			newNode.index = index;
			newNode.osuFileIndex = i;
			newNode.prev = node;

			// unlink the group node
			if (i == 0) {
				firstInserted = newNode;
				newNode.prev = prevNode;
				if (prevNode != null)
					prevNode.next = newNode;
			}

			node.next = newNode;
			node = node.next;
		}
		if (nextNode != null) {
			node.next = nextNode;
			nextNode.prev = node;
		}

		expandedIndex = index;
		return firstInserted;
	}

	/**
	 * Undoes the current expansion, if any.
	 */
	private void unexpand() {
		if (expandedIndex < 0 || expandedIndex >= size())
			return;

		// recreate surrounding links
		OsuGroupNode
			ePrev = getBaseNode(expandedIndex - 1),
			eCur  = getBaseNode(expandedIndex),
			eNext = getBaseNode(expandedIndex + 1);
		if (ePrev != null)
			ePrev.next = eCur;
		eCur.prev = ePrev;
		eCur.index = expandedIndex;
		eCur.next = eNext;
		if (eNext != null)
			eNext.prev = eCur;

		expandedIndex = -1;
		return;
	}

	/**
	 * Initializes the links in the list, given a sorting order (SORT_* constants).
	 */
	public void init(byte order) {
		if (size() < 1)
			return;

		// sort the list
		switch (order) {
		case SORT_TITLE:
			Collections.sort(nodes);
			break;
		case SORT_ARTIST:
			Collections.sort(nodes, new OsuGroupNode.ArtistOrder());
			break;
		case SORT_CREATOR:
			Collections.sort(nodes, new OsuGroupNode.CreatorOrder());
			break;
		case SORT_BPM:
			Collections.sort(nodes, new OsuGroupNode.BPMOrder());
			break;
		}
		expandedIndex = -1;

		// create links
		OsuGroupNode lastNode = nodes.get(0);
		lastNode.index = 0;
		lastNode.prev = null;
		for (int i = 1, size = size(); i < size; i++) {
			OsuGroupNode node = nodes.get(i);
			lastNode.next = node;
			node.index = i;
			node.prev = lastNode;

			lastNode = node;
		}
		lastNode.next = null;
	}

	/**
	 * Creates a new list of song groups in which each group contains a match to a search query. 
	 * @param query the search query (terms separated by spaces)
	 * @return false if query is the same as the previous one, true otherwise
	 */
	public boolean search(String query) {
		if (query == null)
			return false;

		// don't redo the same search
		query = query.trim().toLowerCase();
		if (lastQuery != null && query.equals(lastQuery))
			return false;
		lastQuery = query;
		String[] terms = query.split("\\s+");

		// if empty query, reset to original list
		if (query.isEmpty() || terms.length < 1) {
			nodes = parsedNodes;
			return true;
		}

		// build list from first search term
		nodes = new ArrayList<OsuGroupNode>();
		for (OsuGroupNode node : parsedNodes) {
			if (node.matches(terms[0])) {
				nodes.add(node);
				continue;
			}
		}

		// remove nodes from list if they don't match all remaining terms
		for (int i = 1; i < terms.length; i++) {
			Iterator<OsuGroupNode> iter = nodes.iterator();
			while (iter.hasNext()) {
				OsuGroupNode node = iter.next();
				if (!node.matches(terms[i]))
					iter.remove();
			}
		}

		return true;
	}
}