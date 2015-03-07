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
import itdelatrisu.opsu.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Download server: http://bloodcat.com/osu/
 */
public class BloodcatServer implements DownloadServer {
	/** Formatted download URL: {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "http://bloodcat.com/osu/s/%d";

	/** Formatted search URL: {@code query,rankedOnly,page} */
	private static final String SEARCH_URL = "http://bloodcat.com/osu/?q=%s&m=b&c=%s&g=&d=0&s=date&o=0&p=%d&mod=json";

	/** Total result count from the last query. */
	private int totalResults = -1;

	/** Constructor. */
	public BloodcatServer() {}

	@Override
	public String getURL(int beatmapSetID) {
		return String.format(DOWNLOAD_URL, beatmapSetID);
	}

	@Override
	public DownloadNode[] resultList(String query, int page, boolean rankedOnly) throws IOException {
		DownloadNode[] nodes = null;
		try {
			// read JSON
			String search = String.format(SEARCH_URL, URLEncoder.encode(query, "UTF-8"), rankedOnly ? "0" : "", page);
			JSONObject json = readJsonFromUrl(new URL(search));
			if (json == null) {
				this.totalResults = -1;
				return null;
			}

			// parse result list
			JSONArray arr = json.getJSONArray("results");
			nodes = new DownloadNode[arr.length()];
			for (int i = 0; i < nodes.length; i++) {
				JSONObject item = arr.getJSONObject(i);
				nodes[i] = new DownloadNode(
					item.getInt("id"), item.getString("date"),
					item.getString("title"), item.isNull("titleUnicode") ? null : item.getString("titleUnicode"),
					item.getString("artist"), item.isNull("artistUnicode") ? null : item.getString("artistUnicode"),
					item.getString("creator")
				);
			}

			// store total result count
			this.totalResults = json.getInt("resultCount");
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			ErrorHandler.error(String.format("Problem loading result list for query '%s'.", query), e, true);
		}
		return nodes;
	}

	@Override
	public int totalResults() { return totalResults; }

	/**
	 * Returns a JSON object from a URL.
	 * @param url the remote URL
	 * @return the JSON object
	 * @author Roland Illig (http://stackoverflow.com/a/4308662)
	 */
	private static JSONObject readJsonFromUrl(URL url) throws IOException {
		String s = Utils.readDataFromUrl(url);
		JSONObject json = null;
		if (s != null) {
			try {
				json = new JSONObject(s);
			} catch (JSONException e) {
				ErrorHandler.error("Failed to create JSON object.", e, true);
			}
		}
		return json;
	}
}
