package itdelatrisu.opsu.downloads.servers;

import java.util.Map;

public class OsuPpyLinkTo implements DownloadServer {
	/** Server name. */
	private static final String SERVER_NAME = "osu.ppy.sh";
	
	/** Formatted download URL: {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "https://osu.ppy.sh/d/%d";

	@Override
	public String toString() { return SERVER_NAME; }
	
	@Override
	public String getDownloadURL(int beatmapSetID) {
		return String.format(DOWNLOAD_URL, beatmapSetID);
	}

	@Override
	public Map<String, String> getDownloadRequestHeaders() {
		return null;
	}

	@Override
	public boolean isOpenInBrowser() {
		return true;
	}


}
