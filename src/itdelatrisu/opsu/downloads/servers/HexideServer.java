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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.newdawn.slick.util.Log;

/**
 * Download server: https://osu.hexide.com/
 */
public class HexideServer extends DownloadServer {
	/** Server name. */
	private static final String SERVER_NAME = "Hexide";

	/** Formatted download URL: {@code beatmapSetID,beatmapSetID} */
	private static final String DOWNLOAD_URL = "https://osu.hexide.com/beatmaps/%d/download/%d.osz";

	/** API fields. */
	private static final String API_FIELDS = "maps.ranked_id;maps.title;maps.date;metadata.m_title;metadata.m_artist;metadata.m_creator";

	/** Maximum beatmaps displayed per page. */
	private static final int PAGE_LIMIT = 20;

	/** Formatted home URL: {@code page} */
	private static final String HOME_URL = "https://osu.hexide.com/search/" + API_FIELDS + "/maps.size.gt.0/order.date.desc/limit.%d." + (PAGE_LIMIT + 1);

	/** Formatted search URL: {@code query,page} */
	private static final String SEARCH_URL = "https://osu.hexide.com/search/" + API_FIELDS + "/maps.title.like.%s/order.date.desc/limit.%d." + (PAGE_LIMIT + 1);

	/** Total result count from the last query. */
	private int totalResults = -1;

	/** Constructor. */
	public HexideServer() {}

	@Override
	public String getName() { return SERVER_NAME; }

	@Override
	public String getDownloadURL(int beatmapSetID) {
		return String.format(DOWNLOAD_URL, beatmapSetID, beatmapSetID);
	}

	@Override
	public DownloadNode[] resultList(String query, int page, boolean rankedOnly) throws IOException {
		DownloadNode[] nodes = null;
		try {
			Utils.setSSLCertValidation(false);

			// read JSON
			int resultIndex = (page - 1) * PAGE_LIMIT;
			String search;
			if (query.isEmpty())
				search = String.format(HOME_URL, resultIndex);
			else
				search = String.format(SEARCH_URL, URLEncoder.encode(query, "UTF-8"), resultIndex);
			URL searchURL = new URL(search);
			JSONArray arr = null;
			try {
				arr = Utils.readJsonArrayFromUrl(searchURL);
			} catch (IOException e1) {
				// a valid search with no results still throws an exception (?)
				this.totalResults = 0;
				return new DownloadNode[0];
			}
			if (arr == null) {
				this.totalResults = -1;
				return null;
			}

			// parse result list
			nodes = new DownloadNode[Math.min(arr.length(), PAGE_LIMIT)];
			for (int i = 0; i < nodes.length && i < PAGE_LIMIT; i++) {
				JSONObject item = arr.getJSONObject(i);
				String title, artist, creator;
				if (item.has("versions")) {
					JSONArray versions = item.getJSONArray("versions");
					JSONObject version = versions.getJSONObject(0);
					title = version.getString("m_title");
					artist = version.getString("m_artist");
					creator = version.getString("m_creator");
				} else {  // "versions" is sometimes missing (?)
					String str = item.getString("title");
					int index = str.indexOf(" - ");
					if (index > -1) {
						title = str.substring(0, index);
						artist = str.substring(index + 3);
						creator = "?";
					} else {  // should never happen...
						title = str;
						artist = creator = "?";
					}
				}
				nodes[i] = new DownloadNode(
					item.getInt("ranked_id"), item.getString("date"),
					title, null, artist, null, creator
				);
			}

			// store total result count
			// NOTE: The API doesn't provide a result count without retrieving
			// all results at once; this approach just gets pagination correct.
			this.totalResults = arr.length() + resultIndex;
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
	public int minQueryLength() { return 0; }

	@Override
	public int totalResults() { return totalResults; }

	@Override
	public boolean disableSSLInDownloads() { return true; }
}
