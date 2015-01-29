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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Indexed, expanding, doubly-linked list data type for song groups.
 */
public class OsuGroupList {
	/** Song group structure (each group contains of an ArrayList of OsuFiles). */
	private static OsuGroupList list;

	/** Search pattern for conditional expressions. */
	private static final Pattern SEARCH_CONDITION_PATTERN = Pattern.compile(
		"(ar|cs|od|hp|bpm|length)(=|==|>|>=|<|<=)((\\d*\\.)?\\d+)"
	);

	/** List containing all parsed nodes. */
	private ArrayList<OsuGroupNode> parsedNodes;

	/** Total number of beatmaps (i.e. OsuFile objects). */
	private int mapCount = 0;

	/** Current list of nodes (subset of parsedNodes, used for searches). */
	private ArrayList<OsuGroupNode> nodes;

	/** Index of current expanded node (-1 if no node is expanded). */
	private int expandedIndex;

	/** The last search query. */
	private String lastQuery;

	/**
	 * Creates a new instance of this class (overwriting any previous instance).
	 */
	public static void create() { list = new OsuGroupList(); }

	/**
	 * Returns the single instance of this class.
	 */
	public static OsuGroupList get() { return list; }

	/**
	 * Constructor.
	 */
	private OsuGroupList() {
		parsedNodes = new ArrayList<OsuGroupNode>();
		reset();
	}

	/**
	 * Resets the list's fields.
	 * This does not erase any parsed nodes.
	 */
	public void reset() {
		nodes = parsedNodes;
		expandedIndex = -1;
		lastQuery = "";
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
	 * Initializes the links in the list.
	 */
	public void init() {
		if (size() < 1)
			return;

		// sort the list
		Collections.sort(nodes, SongSort.getSort().getComparator());
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
		LinkedList<String> terms = new LinkedList<String>(Arrays.asList(query.split("\\s+")));

		// if empty query, reset to original list
		if (query.isEmpty() || terms.isEmpty()) {
			nodes = parsedNodes;
			return true;
		}

		// find and remove any conditional search terms
		LinkedList<String> condType     = new LinkedList<String>();
		LinkedList<String> condOperator = new LinkedList<String>();
		LinkedList<Float>  condValue    = new LinkedList<Float>();

		Iterator<String> termIter = terms.iterator();
		while (termIter.hasNext()) {
			String term = termIter.next();
			Matcher m = SEARCH_CONDITION_PATTERN.matcher(term);
			if (m.find()) {
				condType.add(m.group(1));
				condOperator.add(m.group(2));
				condValue.add(Float.parseFloat(m.group(3)));
				termIter.remove();
			}
		}

		// build an initial list from first search term
		nodes = new ArrayList<OsuGroupNode>();
		if (terms.isEmpty()) {
			// conditional term
			String type = condType.remove();
			String operator = condOperator.remove();
			float value = condValue.remove();
			for (OsuGroupNode node : parsedNodes) {
				if (node.matches(type, operator, value))
					nodes.add(node);
			}
		} else {
			// normal term
			String term = terms.remove();
			for (OsuGroupNode node : parsedNodes) {
				if (node.matches(term))
					nodes.add(node);
			}
		}

		// iterate through remaining normal search terms
		while (!terms.isEmpty()) {
			if (nodes.isEmpty())
				return true;

			String term = terms.remove();

			// remove nodes from list if they don't match all terms
			Iterator<OsuGroupNode> nodeIter = nodes.iterator();
			while (nodeIter.hasNext()) {
				OsuGroupNode node = nodeIter.next();
				if (!node.matches(term))
					nodeIter.remove();
			}
		}

		// iterate through remaining conditional terms
		while (!condType.isEmpty()) {
			if (nodes.isEmpty())
				return true;

			String type = condType.remove();
			String operator = condOperator.remove();
			float value = condValue.remove();

			// remove nodes from list if they don't match all terms
			Iterator<OsuGroupNode> nodeIter = nodes.iterator();
			while (nodeIter.hasNext()) {
				OsuGroupNode node = nodeIter.next();
				if (!node.matches(type, operator, value))
					nodeIter.remove();
			}
		}

		return true;
	}
}