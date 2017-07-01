package itdelatrisu.opsu.downloads.servers;
import itdelatrisu.opsu.downloads.DownloadNode;

import java.io.IOException;
import java.util.Map;

public interface SearchServer {
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

	
}
