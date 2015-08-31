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
import java.util.ArrayList;
import java.util.List;

/**
 * Download server: http://osu.mengsky.net/
 */
public class MengSkyServer extends DownloadServer {
	/** Server name. */
	private static final String SERVER_NAME = "MengSky";

	/** Formatted download URL: {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "http://osu.mengsky.net/d.php?id=%d";

	/** Formatted search URL: {@code query} */
	private static final String SEARCH_URL = "http://osu.mengsky.net/index.php?search_keywords=%s";

	/** Formatted home URL: {@code page} */
	private static final String HOME_URL = "http://osu.mengsky.net/index.php?next=1&page=%d";

	/** Maximum beatmaps displayed per page. */
	private static final int PAGE_LIMIT = 20;

	/** Total result count from the last query. */
	private int totalResults = -1;

	/** Constructor. */
	public MengSkyServer() {}

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
			String search;
			boolean isSearch;
			if (query.isEmpty()) {
				isSearch = false;
				search = String.format(HOME_URL, page - 1);
			} else {
				isSearch = true;
				search = String.format(SEARCH_URL, URLEncoder.encode(query, "UTF-8"));
			}
			String html = Utils.readDataFromUrl(new URL(search));
			if (html == null) {
				this.totalResults = -1;
				return null;
			}

			// parse results
			// NOTE: Maybe an HTML parser would be better for this...
			// FORMAT:
			//   <div class="beatmap" style="{{...}}">
			//       <div class="preview" style="background-image:url(http://b.ppy.sh/thumb/{{id}}l.jpg)"></div>
			//       <div class="name"> <a href="">{{artist}} - {{title}}</a> </div>
			//       <div class="douban_details">
			//           <span>Creator:</span> {{creator}}<br>
			//           <span>MaxBpm:</span> {{bpm}}<br>
			//           <span>Title:</span> {{titleUnicode}}<br>
			//           <span>Artist:</span> {{artistUnicode}}<br>
			//           <span>Status:</span> <font color={{"#00CD00" || "#EE0000"}}>{{"Ranked?" || "Unranked"}}</font><br>
			//       </div>
			//       <div class="details"> <a href=""></a> <br>
			//           <span>Fork:</span> bloodcat<br>
			//           <span>UpdateTime:</span> {{yyyy}}/{{mm}}/{{dd}} {{hh}}:{{mm}}:{{ss}}<br>
			//           <span>Mode:</span>  <img id="{{'s' || 'c' || ...}}" src="/img/{{'s' || 'c' || ...}}.png"> {{...}}
			//       </div>
			//       <div class="download">
			//           <a href="https://osu.ppy.sh/s/{{id}}" class=" btn" target="_blank">Osu.ppy</a>
			//       </div>
			//       <div class="download">
			//           <a href="http://osu.mengsky.net/d.php?id={{id}}" class=" btn" target="_blank">DownLoad</a>
			//       </div>
			//   </div>
			List<DownloadNode> nodeList = new ArrayList<DownloadNode>();
			final String
				START_TAG = "<div class=\"beatmap\"", NAME_TAG = "<div class=\"name\"> <a href=\"\">",
				CREATOR_TAG = "<span>Creator:</span> ", TITLE_TAG = "<span>Title:</span> ", ARTIST_TAG = "<span>Artist:</span> ",
				TIMESTAMP_TAG = "<span>UpdateTime:</span> ", DOWNLOAD_TAG = "<div class=\"download\">",
				BR_TAG = "<br>", HREF_TAG = "<a href=\"", HREF_TAG_END = "</a>";
			int index = -1;
			int nextIndex = html.indexOf(START_TAG, index + 1);
			int divCount = 0;
			while ((index = nextIndex) != -1) {
				nextIndex = html.indexOf(START_TAG, index + 1);
				int n = (nextIndex == -1) ? html.length() : nextIndex;
				divCount++;
				int i, j;

				// find beatmap
				i = html.indexOf(NAME_TAG, index + START_TAG.length());
				if (i == -1 || i > n) continue;
				j = html.indexOf(HREF_TAG_END, i + 1);
				if (j == -1 || j > n) continue;
				String beatmap = html.substring(i + NAME_TAG.length(), j);
				String[] beatmapTokens = beatmap.split(" - ", 2);
				if (beatmapTokens.length < 2)
					continue;
				String artist = beatmapTokens[0];
				String title = beatmapTokens[1];

				// find other beatmap details
				i = html.indexOf(CREATOR_TAG, j + HREF_TAG_END.length());
				if (i == -1 || i > n) continue;
				j = html.indexOf(BR_TAG, i + CREATOR_TAG.length());
				if (j == -1 || j > n) continue;
				String creator = html.substring(i + CREATOR_TAG.length(), j);
				i = html.indexOf(TITLE_TAG, j + BR_TAG.length());
				if (i == -1 || i > n) continue;
				j = html.indexOf(BR_TAG, i + TITLE_TAG.length());
				if (j == -1 || j > n) continue;
				String titleUnicode = html.substring(i + TITLE_TAG.length(), j);
				i = html.indexOf(ARTIST_TAG, j + BR_TAG.length());
				if (i == -1 || i > n) continue;
				j = html.indexOf(BR_TAG, i + ARTIST_TAG.length());
				if (j == -1 || j > n) continue;
				String artistUnicode = html.substring(i + ARTIST_TAG.length(), j);
				i = html.indexOf(TIMESTAMP_TAG, j + BR_TAG.length());
				if (i == -1 || i >= n) continue;
				j = html.indexOf(BR_TAG, i + TIMESTAMP_TAG.length());
				if (j == -1 || j > n) continue;
				String date = html.substring(i + TIMESTAMP_TAG.length(), j);

				// find beatmap ID
				i = html.indexOf(DOWNLOAD_TAG, j + BR_TAG.length());
				if (i == -1 || i >= n) continue;
				i = html.indexOf(HREF_TAG, i + DOWNLOAD_TAG.length());
				if (i == -1 || i > n) continue;
				j = html.indexOf('"', i + HREF_TAG.length());
				if (j == -1 || j > n) continue;
				String downloadURL = html.substring(i + HREF_TAG.length(), j);
				String[] downloadTokens = downloadURL.split("(?=\\d*$)", 2);
				if (downloadTokens[1].isEmpty()) continue;
				int id;
				try {
					id = Integer.parseInt(downloadTokens[1]);
				} catch (NumberFormatException e) {
					continue;
				}

				DownloadNode node = new DownloadNode(id, date, title, titleUnicode, artist, artistUnicode, creator);
				System.out.println(node);
				nodeList.add(node);
			}

			nodes = nodeList.toArray(new DownloadNode[nodeList.size()]);

			// store total result count
			if (isSearch)
				this.totalResults = nodes.length;
			else {
				int resultCount = nodes.length + (page - 1) * PAGE_LIMIT;
				if (divCount == PAGE_LIMIT)
					resultCount++;
				this.totalResults = resultCount;
			}
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			ErrorHandler.error(String.format("Problem loading result list for query '%s'.", query), e, true);
		}
		return nodes;
	}

	@Override
	public int minQueryLength() { return 2; }

	@Override
	public int totalResults() { return totalResults; }
}
