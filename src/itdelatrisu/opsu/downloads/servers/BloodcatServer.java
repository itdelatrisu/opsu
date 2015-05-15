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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Download server: http://bloodcat.com/osu/
 */
public class BloodcatServer extends DownloadServer {
	/** Server name. */
	private static final String SERVER_NAME = "Bloodcat";

	/** Formatted download URL: {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "http://bloodcat.com/osu/s/%d";

	/** Formatted search URL: {@code query,rankedOnly,page} */
	private static final String SEARCH_URL = "http://bloodcat.com/osu/?q=%s&m=b&c=%s&g=&d=0&s=date&o=0&p=%d&mod=json";

	/** Total result count from the last query. */
	private int totalResults = -1;

	/** Constructor. */
	public BloodcatServer() {}

	@Override
	public String getName() { return SERVER_NAME; }

	@Override
	public String getDownloadURL(int beatmapSetID) {
		return String.format(DOWNLOAD_URL, beatmapSetID);
	}

	@Override
	public DownloadNode[] resultList(String query, int page, boolean rankedOnly) throws IOException {
		DownloadNode[] nodes = null;
		try {
			// read JSON
			String search = String.format(SEARCH_URL, URLEncoder.encode(query, "UTF-8"), rankedOnly ? "0" : "", page);
			JSONObject json = Utils.readJsonFromUrl(new URL(search));
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
					item.getInt("id"), formatDate(item.getString("date")),
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
	public int minQueryLength() { return 0; }

	@Override
	public int totalResults() { return totalResults; }

	/**
	 * Returns a formatted date string from a raw date.
	 * @param s the raw date string (e.g. "2015-05-14T23:38:47+09:00")
	 * @return the formatted date, or the raw string if it could not be parsed
	 */
	private String formatDate(String s) {
		try {
			// make string parseable by SimpleDateFormat
			int index = s.lastIndexOf(':');
			if (index == -1)
				return s;
			String str = new StringBuilder(s).deleteCharAt(index).toString();

			DateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			Date d = f.parse(str);
			DateFormat fmt = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
			return fmt.format(d);
		} catch (StringIndexOutOfBoundsException | ParseException e) {
			return s;
		}
	}
}
