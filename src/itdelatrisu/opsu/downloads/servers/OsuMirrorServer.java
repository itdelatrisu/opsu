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

package itdelatrisu.opsu.downloads.servers;

import fluddokt.opsu.fake.Log;
import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.downloads.DownloadNode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/*
import org.newdawn.slick.util.Log;
*/

/**
 * Download server: http://loli.al/
 * <p>
 * <i>This server went offline in August 2015.</i>
 */
public class OsuMirrorServer extends DownloadServer {
	/** Server name. */
	private static final String SERVER_NAME = "osu!Mirror";

	/** Formatted download URL: {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "http://loli.al/d/%d/";

	/** Formatted search URL: {@code page,query} */
	private static final String SEARCH_URL = "http://loli.al/mirror/search/%d.json?keyword=%s";

	/** Formatted home URL: {@code page} */
	private static final String HOME_URL = "http://loli.al/mirror/home/%d.json";

	/** Minimum allowable length of a search query. */
	private static final int MIN_QUERY_LENGTH = 3;

	/** Total result count from the last query. */
	private int totalResults = -1;

	/** Max server download ID seen (for approximating total pages). */
	private int maxServerID = 0;

	/** Lookup table from beatmap set ID -> server download ID. */
	private HashMap<Integer, Integer> idTable = new HashMap<Integer, Integer>();

	/** Constructor. */
	public OsuMirrorServer() {}

	@Override
	public String getName() { return SERVER_NAME; }

	@Override
	public String getDownloadURL(int beatmapSetID) {
		return (idTable.containsKey(beatmapSetID)) ? String.format(DOWNLOAD_URL, idTable.get(beatmapSetID)) : null;
	}

	@Override
	public DownloadNode[] resultList(String query, int page, boolean rankedOnly) throws IOException {
		// NOTE: ignores 'rankedOnly' flag.
		DownloadNode[] nodes = null;
		try {
			// read JSON
			String search;
			boolean isSearch;
			if (query.isEmpty()) {
				isSearch = false;
				search = String.format(HOME_URL, page);
			} else {
				isSearch = true;
				search = String.format(SEARCH_URL, page, URLEncoder.encode(query, "UTF-8"));
			}
			JSONObject json = Utils.readJsonObjectFromUrl(new URL(search));
			if (json == null || json.getInt("code") != 0) {
				this.totalResults = -1;
				return null;
			}

			// parse result list
			JSONArray arr = json.getJSONArray("maplist");
			nodes = new DownloadNode[arr.length()];
			for (int i = 0; i < nodes.length; i++) {
				JSONObject item = arr.getJSONObject(i);
				int beatmapSetID = item.getInt("OSUSetid");
				int serverID = item.getInt("id");
				nodes[i] = new DownloadNode(
					beatmapSetID, formatDate(item.getString("ModifyDate")),
					item.getString("Title"), null,
					item.getString("Artist"), null,
					item.getString("Mapper")
				);
				idTable.put(beatmapSetID, serverID);
				if (serverID > maxServerID)
					maxServerID = serverID;
			}

			// store total result count
			if (isSearch)
				this.totalResults = json.getInt("totalRows");
			else
				this.totalResults = maxServerID;
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			ErrorHandler.error(String.format("Problem loading result list for query '%s'.", query), e, true);
		} catch (JSONException e) {
			Log.error(e);
		}
		return nodes;
	}

	@Override
	public int minQueryLength() { return MIN_QUERY_LENGTH; }

	@Override
	public int totalResults() { return totalResults; }

	/**
	 * Returns a formatted date string from a raw date.
	 * @param s the raw date string (e.g. "2015-05-14T23:38:47Z")
	 * @return the formatted date, or the raw string if it could not be parsed
	 */
	private String formatDate(String s) {
		try {
			DateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			f.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date d = f.parse(s);
			DateFormat fmt = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
			return fmt.format(d);
		} catch (ParseException e) {
			return s;
		}
	}
}
