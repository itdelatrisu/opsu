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
import java.util.HashMap;
import java.util.HashSet;

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
	 * Song tags.
	 * Each tag value is a HashSet which points song group ArrayLists.
	 */
	private HashMap<String, HashSet<ArrayList<OsuFile>>> tags;

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
		tags = new HashMap<String, HashSet<ArrayList<OsuFile>>>();
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
	 * Adds a tag.
	 * @param tag the tag string (key)
	 * @param osuFiles the song group associated with the tag (value)
	 */
	public void addTag(String tag, ArrayList<OsuFile> osuFiles) {
		tag = tag.toLowerCase();
		HashSet<ArrayList<OsuFile>> tagSet = tags.get(tag);
		if (tagSet == null) {
			tagSet = new HashSet<ArrayList<OsuFile>>();
			tags.put(tag, tagSet);
		}
		tagSet.add(osuFiles);
	}

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
		if (node.index == expandedIndex)  // don't choose an expanded group node
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
	 * @param query the search query (tag terms separated by spaces)
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

		// if empty query, reset to original list
		if (query.isEmpty()) {
			nodes = parsedNodes;
			return true;
		}

		// tag search: check if each word is contained in global tag structure
		HashSet<ArrayList<OsuFile>> taggedGroups = new HashSet<ArrayList<OsuFile>>();
		String[] terms = query.split("\\s+");
		for (String term : terms) {
			if (tags.containsKey(term))
				taggedGroups.addAll(tags.get(term));  // add all matches
		}

		// traverse parsedNodes, comparing each element with the query
		nodes = new ArrayList<OsuGroupNode>();
		for (OsuGroupNode node : parsedNodes) {
			// search: tags
			if (taggedGroups.contains(node.osuFiles)) {
				nodes.add(node);
				continue;
			}

			OsuFile osu = node.osuFiles.get(0);

			// search: title, artist, creator, source, version (first OsuFile)
			if (osu.title.toLowerCase().contains(query) ||
				osu.artist.toLowerCase().contains(query) ||
				osu.creator.toLowerCase().contains(query) ||
				osu.source.toLowerCase().contains(query) ||
				osu.version.toLowerCase().contains(query)) {
				nodes.add(node);
				continue;
			}

			// search: versions (all OsuFiles)
			for (int i = 1; i < node.osuFiles.size(); i++) {
				if (node.osuFiles.get(i).version.toLowerCase().contains(query)) {
					nodes.add(node);
					break;
				}
			}
		}

		return true;
	}
}