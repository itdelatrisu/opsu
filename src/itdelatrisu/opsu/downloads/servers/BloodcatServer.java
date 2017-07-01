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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.newdawn.slick.util.Log;

/**
 * Download server: http://bloodcat.com/osu/
 * <p>
 * <i>This server uses captchas as of March 2017.</i>
 */
public class BloodcatServer extends DownloadServer {
	/** Server name. */
	private static final String SERVER_NAME = "Bloodcat";

	/** Formatted download URL: {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "http://bloodcat.com/osu/s/%d";

	/** Formatted search URL: {@code query,rankedOnly,page} */
	private static final String SEARCH_URL = "http://bloodcat.com/osu/?q=%s&c=b&s=%s&m=0&p=%d&mod=json";//"?q=%s&m=b&c=%s&g=&d=0&s=date&o=0&p=%d&mod=json";

	/** Maximum beatmaps displayed per page. */
	private static final int PAGE_LIMIT = 61;

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
			String search = String.format(SEARCH_URL, URLEncoder.encode(query, "UTF-8"), rankedOnly ? "1" : "", page);
			JSONArray arr = Utils.readJsonArrayFromUrl(new URL(search));
			if (arr == null) {
				this.totalResults = -1;
				return null;
			}

			// parse result list
			//JSONArray arr = json.getJSONArray("results");
			nodes = new DownloadNode[arr.length()];
			for (int i = 0; i < nodes.length; i++) {
				JSONObject item = arr.getJSONObject(i);
				nodes[i] = new DownloadNode(
					item.getInt("id"), formatDate(item.getString("synced")),  //"date"
					item.getString("title"), item.isNull("titleU") ? null : item.getString("titleU"),  //"titleUnicode"
					item.getString("artist"), item.isNull("artistU") ? null : item.getString("artistU"),  //"artistUnicode"
					item.getString("creator")
				);
			}

			// store total result count
			//this.totalResults = arr.getInt("resultCount");
			int resultCount = nodes.length + (page - 1) * PAGE_LIMIT;
			if (nodes.length == PAGE_LIMIT)
				resultCount++;
			this.totalResults = resultCount;
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			ErrorHandler.error(String.format("Problem loading result list for query '%s'.", query), e, true);
		} catch (JSONException e) {
			Log.error(e);
		}
		return nodes;
	}

	@Override
	public int minQueryLength() { return 0; }

	@Override
	public int totalResults() { return totalResults; }

	@Override
	public boolean isDownloadInBrowser() { return true; /* uses captchas */ }

	/**
	 * Returns a formatted date string from a raw date.
	 * @param s the raw date string (e.g. "2015-09-30 09:39:04.536")
	 * @return the formatted date, or the raw string if it could not be parsed
	 */
	private String formatDate(String s) {
		try {
			// old format: "2015-05-14T23:38:47+09:00"
			// make string parseable by SimpleDateFormat
//			int index = s.lastIndexOf(':');
//			if (index == -1)
//				return s;
//			s = new StringBuilder(s).deleteCharAt(index).toString();

			DateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  //"yyyy-MM-dd'T'HH:mm:ssZ"
			Date d = f.parse(s);
			DateFormat fmt = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
			return fmt.format(d);
		} catch (StringIndexOutOfBoundsException | ParseException e) {
			return s;
		}
	}
}
