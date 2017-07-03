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

import itdelatrisu.opsu.downloads.DownloadNode;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract class for beatmap download servers.
 */
public abstract class DownloadServer {
	/** Track preview URL. */
	private static final String PREVIEW_URL = "http://b.ppy.sh/preview/%d.mp3";

	/**
	 * Returns the name of the download server.
	 * @return the server name
	 */
	public abstract String getName();

	/**
	 * Returns a web address to download the given beatmap.
	 * @param beatmapSetID the beatmap set ID
	 * @return the URL string, or null if the address could not be determined
	 */
	public abstract String getDownloadURL(int beatmapSetID);

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
	 * Returns the minimum allowable length of a search query.
	 * @return the minimum length, or 0 if none
	 */
	public abstract int minQueryLength();

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

	/**
	 * Returns any HTTP request headers that should be set in the download request.
	 * @return the map of headers (key -> value), or null if none
	 */
	public Map<String, String> getDownloadRequestHeaders() { return null; }

	/**
	 * Returns whether downloads must be made through a web browser.
	 * @return true if downloads should launch a web browser
	 */
	public boolean isDownloadInBrowser() { return false; }

	/**
	 * Returns whether SSL certificate validation should be disabled for downloads.
	 * @return true if validation should be disabled
	 */
	public boolean disableSSLInDownloads() { return false; }

	@Override
	public String toString() { return getName(); }
}
