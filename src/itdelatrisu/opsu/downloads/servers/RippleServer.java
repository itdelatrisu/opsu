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
 * Download server: https://ripple.moe/
 */
public class RippleServer extends DownloadServer {
	/** Server name. */
	private static final String SERVER_NAME = "Ripple";

	/** Formatted download URL: {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "https://storage.ripple.moe/d/%d";

	/** Formatted search URL: {@code query,amount,offset} */
	private static final String SEARCH_URL = "https://storage.ripple.moe/api/search?query=%s&mode=0&amount=%d&offset=%d";

	/**
	 * Maximum beatmaps displayed per page.
	 * Supports up to 100, but response sizes become very large (>100KB).
	 */
	private static final int PAGE_LIMIT = 20;

	/** Total result count from the last query. */
	private int totalResults = -1;

	/** Constructor. */
	public RippleServer() {}

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
			Utils.setSSLCertValidation(false);

			// read JSON
			int offset = (page - 1) * PAGE_LIMIT;
			String search = String.format(SEARCH_URL, URLEncoder.encode(query, "UTF-8"), PAGE_LIMIT, offset);
			if (rankedOnly)
				search += "&status=1";
			JSONArray arr = Utils.readJsonArrayFromUrl(new URL(search));
			if (arr == null) {
				this.totalResults = -1;
				return null;
			}

			// parse result list
			nodes = new DownloadNode[arr.length()];
			for (int i = 0; i < nodes.length; i++) {
				JSONObject item = arr.getJSONObject(i);
				nodes[i] = new DownloadNode(
					item.getInt("SetID"), formatDate(item.getString("LastUpdate")),
					item.getString("Title"), null, item.getString("Artist"), null,
					item.getString("Creator")
				);
			}

			// store total result count
			int resultCount = nodes.length + offset;
			if (nodes.length == PAGE_LIMIT)
				resultCount++;
			this.totalResults = resultCount;
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

	@Override
	public boolean disableSSLInDownloads() { return true; }

	/**
	 * Returns a formatted date string from a raw date.
	 * @param s the raw date string (e.g. "2015-09-30T09:39:04Z")
	 * @return the formatted date, or the raw string if it could not be parsed
	 */
	private String formatDate(String s) {
		try {
			DateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			Date d = f.parse(s);
			DateFormat fmt = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
			return fmt.format(d);
		} catch (StringIndexOutOfBoundsException | ParseException e) {
			return s;
		}
	}
}
