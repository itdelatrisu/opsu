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

import java.util.Map;

/**
 * Abstract class for beatmap download servers.
 */
public interface DownloadServer {
	/**
	 * Returns a web address to download the given beatmap.
	 * @param beatmapSetID the beatmap set ID
	 * @return the URL string, or null if the address could not be determined
	 */
	public abstract String getDownloadURL(int beatmapSetID);
	
	/**
	 * Returns any HTTP request headers that should be set in the download request.
	 * @return the map of headers (key -> value), or null if none
	 */
	public abstract  Map<String, String> getDownloadRequestHeaders();
	
	public abstract boolean isOpenInBrowser();

}
