//TODO rename

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

import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.db.OsuDB;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

	/** Set of all beatmap set IDs for the parsed beatmaps. */
	private HashSet<Integer> MSIDdb;
	
	/** Map of all hash to OsuFile . */
	public HashMap<String, OsuFile> beatmapHashesToFile;


	/** Index of current expanded node (-1 if no node is expanded). */
	private int expandedIndex;

	/** Start and end nodes of expanded group. */
	private OsuGroupNode expandedStartNode, expandedEndNode;

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
		MSIDdb = new HashSet<Integer>();
		beatmapHashesToFile = new HashMap<String, OsuFile>();
		reset();
	}

	/**
	 * Resets the list's fields.
	 * This does not erase any parsed nodes.
	 */
	public void reset() {
		nodes = parsedNodes;
		expandedIndex = -1;
		expandedStartNode = expandedEndNode = null;
		lastQuery = "";
	}

	/**
	 * Returns the number of elements.
	 */
	public int size() { return nodes.size(); }

	/**
	 * Adds a song group.
	 * @param osuFiles the list of OsuFile objects in the group
	 * @return the new OsuGroupNode
	 */
	public OsuGroupNode addSongGroup(ArrayList<OsuFile> osuFiles) {
		OsuGroupNode node = new OsuGroupNode(osuFiles);
		parsedNodes.add(node);
		mapCount += osuFiles.size();

		// add beatmap set ID to set
		int msid = osuFiles.get(0).beatmapSetID;
		if (msid > 0)
			MSIDdb.add(msid);
		for(OsuFile f : osuFiles) {
			beatmapHashesToFile.put(f.md5Hash, f);
		}

		return node;
	}

	/**
	 * Deletes a song group from the list, and also deletes the beatmap
	 * directory associated with the node.
	 * @param node the node containing the song group to delete
	 * @return true if the song group was deleted, false otherwise
	 */
	public boolean deleteSongGroup(OsuGroupNode node) {
		if (node == null)
			return false;

		// re-link base nodes
		int index = node.index;
		OsuGroupNode ePrev = getBaseNode(index - 1), eCur = getBaseNode(index), eNext = getBaseNode(index + 1);
		if (ePrev != null) {
			if (ePrev.index == expandedIndex)
				expandedEndNode.next = eNext;
			else if (eNext != null && eNext.index == expandedIndex)
				ePrev.next = expandedStartNode;
			else
				ePrev.next = eNext;
		}
		if (eNext != null) {
			if (eNext.index == expandedIndex)
				expandedStartNode.prev = ePrev;
			else if (ePrev != null && ePrev.index == expandedIndex)
				eNext.prev = expandedEndNode;
			else
				eNext.prev = ePrev;
		}

		// remove all node references
		OsuFile osu = node.osuFiles.get(0);
		nodes.remove(index);
		parsedNodes.remove(eCur);
		mapCount -= node.osuFiles.size();
		if (osu.beatmapSetID > 0)
			MSIDdb.remove(osu.beatmapSetID);

		// reset indices
		for (int i = index, size = size(); i < size; i++)
			nodes.get(i).index = i;
		if (index == expandedIndex) {
			expandedIndex = -1;
			expandedStartNode = expandedEndNode = null;
		} else if (expandedIndex > index) {
			expandedIndex--;
			OsuGroupNode expandedNode = expandedStartNode;
			for (int i = 0, size = expandedNode.osuFiles.size();
			     i < size && expandedNode != null;
			     i++, expandedNode = expandedNode.next)
				expandedNode.index = expandedIndex;
		}

		// stop playing the track
		File dir = osu.getFile().getParentFile();
		if (MusicController.trackExists() || MusicController.isTrackLoading()) {
			File audioFile = MusicController.getOsuFile().audioFilename;
			if (audioFile != null && audioFile.equals(osu.audioFilename)) {
				MusicController.reset();
				System.gc();  // TODO: why can't files be deleted without calling this?
			}
		}

		// remove entry from cache
		OsuDB.delete(dir.getName());

		// delete the associated directory
		try {
			Utils.deleteToTrash(dir);
		} catch (IOException e) {
			ErrorHandler.error("Could not delete song group.", e, true);
		}

		return true;
	}

	/**
	 * Deletes a song from a song group, and also deletes the beatmap file.
	 * If this causes the song group to be empty, then the song group and
	 * beatmap directory will be deleted altogether.
	 * @param node the node containing the song group to delete (expanded only)
	 * @return true if the song or song group was deleted, false otherwise
	 * @see #deleteSongGroup(OsuGroupNode)
	 */
	public boolean deleteSong(OsuGroupNode node) {
		if (node == null || node.osuFileIndex == -1 || node.index != expandedIndex)
			return false;

		// last song in group?
		int size = node.osuFiles.size();
		if (node.osuFiles.size() == 1)
			return deleteSongGroup(node);

		// reset indices
		OsuGroupNode expandedNode = node.next;
		for (int i = node.osuFileIndex + 1;
		     i < size && expandedNode != null && expandedNode.index == node.index;
		     i++, expandedNode = expandedNode.next)
			expandedNode.osuFileIndex--;

		// remove song reference
		OsuFile osu = node.osuFiles.remove(node.osuFileIndex);
		mapCount--;

		// re-link nodes
		if (node.prev != null)
			node.prev.next = node.next;
		if (node.next != null)
			node.next.prev = node.prev;

		// remove entry from cache
		File file = osu.getFile();
		OsuDB.delete(file.getParentFile().getName(), file.getName());

		// delete the associated file
		try {
			Utils.deleteToTrash(file);
		} catch (IOException e) {
			ErrorHandler.error("Could not delete song.", e, true);
		}

		return true;
	}

	/**
	 * Returns the total number of parsed maps (i.e. OsuFile objects).
	 */
	public int getMapCount() { return mapCount; }

	/**
	 * Returns the total number of parsed maps sets.
	 */
	public int getMapSetCount() { return parsedNodes.size(); }

	/**
	 * Returns the OsuGroupNode at an index, disregarding expansions.
	 * @param index the node index
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

		expandedStartNode = expandedEndNode = null;

		// create new nodes
		ArrayList<OsuFile> osuFiles = node.osuFiles;
		OsuGroupNode prevNode = node.prev;
		OsuGroupNode nextNode = node.next;
		for (int i = 0, size = node.osuFiles.size(); i < size; i++) {
			OsuGroupNode newNode = new OsuGroupNode(osuFiles);
			newNode.index = index;
			newNode.osuFileIndex = i;
			newNode.prev = node;

			// unlink the group node
			if (i == 0) {
				expandedStartNode = newNode;
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
		expandedEndNode = node;

		expandedIndex = index;
		return expandedStartNode;
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
		expandedStartNode = expandedEndNode = null;
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
		expandedStartNode = expandedEndNode = null;

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

	/**
	 * Returns whether or not the list contains the given beatmap set ID.
	 * <p>
	 * Note that IDs for older maps might have been improperly parsed, so
	 * there is no guarantee that this method will return an accurate value.
	 * @param id the beatmap set ID to check
	 * @return true if id is in the list
	 */
	public boolean containsBeatmapSetID(int id) { return MSIDdb.contains(id); }
	
	public OsuFile getFileFromBeatmapHash(String beatmapHash) {
		return beatmapHashesToFile.get(beatmapHash);
	}
}