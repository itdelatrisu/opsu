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

import java.io.IOException;

/**
 * Abstract class for beatmap download servers.
 */
public abstract class DownloadServer {
	/** Track preview URL. */
	private static final String PREVIEW_URL = "http://b.ppy.sh/preview/%d.mp3";

	/**
	 * Returns a web address to download the given beatmap.
	 * @param beatmapSetID the beatmap set ID
	 * @return the URL string
	 */
	public abstract String getURL(int beatmapSetID);

	/**
	 * Returns a list of results for a given search query, or null if the
	 * list could not be created.
	 * @param query the search query
	 * @param page the result page (starting at 1)
	 * @param rankedOnly whether to only show ranked maps
	 * @return the result array
	 * @throws IOException if any connection problem occurs
	 */
	public abstract DownloadNode[] resultList(String query, int page, boolean rankedOnly) throws IOException;

	/**
	 * Returns the total number of results for the last search query.
	 * This will differ from the the size of the array returned by
	 * {@link #resultList(String, int, boolean)} if multiple pages exist.
	 * @return the result count, or -1 if no query
	 */
	public abstract int totalResults();

	/**
	 * Returns a web address to preview the given beatmap.
	 * @param beatmapSetID the beatmap set ID
	 * @return the URL string
	 */
	public String getPreviewURL(int beatmapSetID) {
		return String.format(PREVIEW_URL, beatmapSetID);
	}
}
