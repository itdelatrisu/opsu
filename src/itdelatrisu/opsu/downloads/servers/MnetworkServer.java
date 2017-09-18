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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.newdawn.slick.util.Log;

/**
 * Download server: http://osu.uu.gl/
 */
public class MnetworkServer extends DownloadServer {
	/** Server name. */
	private static final String SERVER_NAME = "Mnetwork";

	/** Formatted download URL: {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "http://osu.uu.gl/s/%d";

	/** Formatted search URL: {@code query} */
	private static final String SEARCH_URL = "http://osu.uu.gl/d/%s";

	/** Total result count from the last query. */
	private int totalResults = -1;

	/** Beatmap pattern. */
	private Pattern BEATMAP_PATTERN = Pattern.compile("^(\\d+) ([^-]+) - (.+)\\.osz$");

	/** Constructor. */
	public MnetworkServer() {}

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
			// read HTML
			String queryString = (query.isEmpty()) ? "-" : query;
			String search = String.format(SEARCH_URL, URLEncoder.encode(queryString, "UTF-8").replace("+", "%20"));
			String html = Utils.readDataFromUrl(new URL(search));
			if (html == null) {
				this.totalResults = -1;
				return null;
			}

			// parse results
			// NOTE: Not using a full HTML parser because this is a relatively simple operation.
			// FORMAT:
			//   <div class="tr_title">
			//   <b><a href='/s/{{id}}'>{{id}} {{artist}} - {{title}}.osz</a></b><br />
			//   BPM: {{bpm}} <b>|</b> Total Time: {{m}}:{{s}}<br/>
			//   Genre: {{genre}} <b>|</b> Updated: {{MMM}} {{d}}, {{yyyy}}<br />
			List<DownloadNode> nodeList = new ArrayList<DownloadNode>();
			final String START_TAG = "<div class=\"tr_title\">", HREF_TAG = "<a href=", HREF_TAG_END = "</a>", UPDATED = "Updated: ";
			int index = -1;
			int nextIndex = html.indexOf(START_TAG, index + 1);
			while ((index = nextIndex) != -1) {
				nextIndex = html.indexOf(START_TAG, index + 1);
				int n = (nextIndex == -1) ? html.length() : nextIndex;
				int i, j;

				// find beatmap
				i = html.indexOf(HREF_TAG, index + START_TAG.length());
				if (i == -1 || i > n) continue;
				i = html.indexOf('>', i + HREF_TAG.length());
				if (i == -1 || i >= n) continue;
				j = html.indexOf(HREF_TAG_END, i + 1);
				if (j == -1 || j > n) continue;
				String beatmap = html.substring(i + 1, j).trim();

				// find date
				i = html.indexOf(UPDATED, j);
				if (i == -1 || i >= n) continue;
				j = html.indexOf('<', i + UPDATED.length());
				if (j == -1 || j > n) continue;
				String date = html.substring(i + UPDATED.length(), j).trim();

				// parse id, title, and artist
				Matcher m = BEATMAP_PATTERN.matcher(beatmap);
				if (!m.matches())
					continue;

				nodeList.add(new DownloadNode(Integer.parseInt(m.group(1)), date, m.group(3), null, m.group(2), null, ""));
			}

			nodes = nodeList.toArray(new DownloadNode[nodeList.size()]);

			// store total result count
			this.totalResults = nodes.length;
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
}
