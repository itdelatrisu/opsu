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

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.downloads.DownloadNode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.newdawn.slick.util.Log;

/**
 * Download server: http://osu.yas-online.net/
 * <p>
 * <i>This server no longer hosts downloads as of March 2017.</i>
 */
public class YaSOnlineServer extends DownloadServer {
	/** Server name. */
	private static final String SERVER_NAME = "YaS Online";

	/** Formatted download URL (returns JSON): {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "https://osu.yas-online.net/json.mapdata.php?mapId=%d";

	/**
	 * Formatted download fetch URL: {@code downloadLink}
	 * (e.g. {@code /fetch/49125122158ef360a66a07bce2d0483596913843-m-10418.osz})
	 */
	private static final String DOWNLOAD_FETCH_URL = "https://osu.yas-online.net%s";

	/** Maximum beatmaps displayed per page. */
	private static final int PAGE_LIMIT = 25;

	/** Formatted home URL: {@code page} */
	private static final String HOME_URL = "https://osu.yas-online.net/json.maplist.php?o=%d";

	/** Formatted search URL: {@code query} */
	private static final String SEARCH_URL = "https://osu.yas-online.net/json.search.php?searchQuery=%s";

	/** Total result count from the last query. */
	private int totalResults = -1;

	/** Max server download ID seen (for approximating total pages). */
	private int maxServerID = 0;

	/** Constructor. */
	public YaSOnlineServer() {}

	@Override
	public String getName() { return SERVER_NAME; }

	@Override
	public String getDownloadURL(int beatmapSetID) {
		try {
			// TODO: do this asynchronously (will require lots of changes...)
			return getDownloadURLFromMapData(beatmapSetID);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Returns the beatmap download URL by downloading its map data.
	 * <p>
	 * This is needed because there is no other way to find a beatmap's direct
	 * download URL.
	 * @param beatmapSetID the beatmap set ID
	 * @return the URL string, or null if the address could not be determined
	 * @throws IOException if any connection error occurred
	 */
	private String getDownloadURLFromMapData(int beatmapSetID) throws IOException {
		try {
			Utils.setSSLCertValidation(false);

			// read JSON
			String search = String.format(DOWNLOAD_URL, beatmapSetID);
			JSONObject json = Utils.readJsonObjectFromUrl(new URL(search));
			JSONObject results;
			if (json == null ||
			    !json.getString("result").equals("success") ||
			    (results = json.getJSONObject("success")).length() == 0) {
				return null;
			}

			// parse result
			Iterator<?> keys = results.keys();
			if (!keys.hasNext())
				return null;
			String key = (String) keys.next();
			JSONObject item = results.getJSONObject(key);
			String downloadLink = item.getString("downloadLink");
			return String.format(DOWNLOAD_FETCH_URL, downloadLink);
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			ErrorHandler.error(String.format("Problem retrieving download URL for beatmap '%d'.", beatmapSetID), e, true);
			return null;
		} finally {
			Utils.setSSLCertValidation(true);
		}
	}

	@Override
	public DownloadNode[] resultList(String query, int page, boolean rankedOnly) throws IOException {
		DownloadNode[] nodes = null;
		try {
			Utils.setSSLCertValidation(false);

			// read JSON
			String search;
			boolean isSearch;
			if (query.isEmpty()) {
				isSearch = false;
				search = String.format(HOME_URL, (page - 1) * PAGE_LIMIT);
			} else {
				isSearch = true;
				search = String.format(SEARCH_URL, URLEncoder.encode(query, "UTF-8"));
			}
			JSONObject json = Utils.readJsonObjectFromUrl(new URL(search));
			if (json == null) {
				this.totalResults = -1;
				return null;
			}
			JSONObject results;
			if (!json.getString("result").equals("success") ||
			    (results = json.getJSONObject("success")).length() == 0) {
				this.totalResults = 0;
				return new DownloadNode[0];
			}

			// parse result list
			List<DownloadNode> nodeList = new ArrayList<DownloadNode>();
			for (Object obj : results.keySet()) {
				String key = (String) obj;
				JSONObject item = results.getJSONObject(key);

				// parse title and artist
				String title, artist;
				String str = item.getString("map");
				int index = str.indexOf(" - ");
				if (index > -1) {
					title = str.substring(0, index);
					artist = str.substring(index + 3);
				} else {  // should never happen...
					title = str;
					artist = "?";
				}

				// only contains date added if part of a beatmap pack
				int added = item.getInt("added");
				String date = (added == 0) ? "?" : formatDate(added);

				// approximate page count
				int serverID = item.getInt("id");
				if (serverID > maxServerID)
					maxServerID = serverID;

				nodeList.add(new DownloadNode(item.getInt("mapid"), date, title, null, artist, null, ""));
			}
			nodes = nodeList.toArray(new DownloadNode[nodeList.size()]);

			// store total result count
			if (isSearch)
				this.totalResults = nodes.length;
			else
				this.totalResults = maxServerID;
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			ErrorHandler.error(String.format("Problem loading result list for query '%s'.", query), e, true);
		} catch (JSONException e) {
			Log.error(e);
		} finally {
			Utils.setSSLCertValidation(true);
		}
		return nodes;
	}

	@Override
	public int minQueryLength() { return 3; }

	@Override
	public int totalResults() { return totalResults; }

	/**
	 * Returns a formatted date string from a raw date.
	 * @param timestamp the UTC timestamp, in seconds
	 * @return the formatted date
	 */
	private String formatDate(int timestamp) {
		Date d = new Date(timestamp * 1000L);
		DateFormat fmt = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
		return fmt.format(d);
	}
}
