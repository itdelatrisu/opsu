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

package itdelatrisu.opsu.downloads;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.downloads.Download.DownloadListener;
import itdelatrisu.opsu.downloads.Download.Status;
import itdelatrisu.opsu.downloads.servers.DownloadServer;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.UI;

import java.io.File;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * Node containing song data and a Download object.
 */
public class DownloadNode {
	/** The associated Download object. */
	private Download download;

	/** Beatmap set ID. */
	private final int beatmapSetID;

	/** Last updated date string. */
	private final String date;

	/** Song title. */
	private final String title, titleUnicode;

	/** Song artist. */
	private final String artist, artistUnicode;

	/** Beatmap creator. */
	private final String creator;

	/** Button drawing values. */
	private static float buttonBaseX, buttonBaseY, buttonWidth, buttonHeight, buttonOffset;

	/** Information drawing values. */
	private static float infoBaseX, infoBaseY, infoWidth, infoHeight;

	/** Maximum number of results and downloads to display on one screen. */
	private static int maxResultsShown, maxDownloadsShown;

	/** Container width. */
	private static int containerWidth;

	/**
	 * Initializes the base coordinates for drawing.
	 * @param width the container width
	 * @param height the container height
	 */
	public static void init(int width, int height) {
		containerWidth = width;

		// download result buttons
		buttonBaseX = width * 0.024f;
		buttonBaseY = height * 0.2f;
		buttonWidth = width * 0.7f;
		buttonHeight = Fonts.MEDIUM.getLineHeight() * 2.1f;
		buttonOffset = buttonHeight * 1.1f;

		// download info
		infoBaseX = width * 0.75f;
		infoBaseY = height * 0.07f + Fonts.LARGE.getLineHeight() * 2f;
		infoWidth = width * 0.25f;
		infoHeight = Fonts.DEFAULT.getLineHeight() * 2.4f;

		float searchY = (height * 0.05f) + Fonts.LARGE.getLineHeight();
		float buttonHeight = height * 0.038f;
		maxResultsShown = (int) ((height - buttonBaseY - searchY) / buttonOffset);
		maxDownloadsShown = (int) ((height - infoBaseY - searchY - buttonHeight) / infoHeight);
	}

	/**
	 * Returns the max number of search result buttons to be shown at a time.
	 */
	public static int maxResultsShown() { return maxResultsShown; }

	/**
	 * Returns the max number of downloads to be shown at a time.
	 */
	public static int maxDownloadsShown() { return maxDownloadsShown; }

	/**
	 * Returns true if the coordinates are within the bounds of the
	 * download result button at the given index.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @param index the index (to offset the button from the topmost button)
	 */
	public static boolean resultContains(float cx, float cy, int index) {
		float y = buttonBaseY + (index * buttonOffset);
		return ((cx > buttonBaseX && cx < buttonBaseX + buttonWidth) &&
		        (cy > y && cy < y + buttonHeight));
	}

	/**
	 * Returns true if the coordinates are within the bounds of the
	 * download result action icon at the given index.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @param index the index (to offset the button from the topmost button)
	 */
	public static boolean resultIconContains(float cx, float cy, int index) {
		int iconWidth = GameImage.MUSIC_PLAY.getImage().getWidth();
		float x = buttonBaseX + buttonWidth * 0.001f;
		float y = buttonBaseY + (index * buttonOffset) + buttonHeight / 2f;
		return ((cx > x && cx < x + iconWidth) &&
		        (cy > y - iconWidth / 2 && cy < y + iconWidth / 2));
	}

	/**
	 * Returns true if the coordinates are within the bounds of the
	 * download result button area.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public static boolean resultAreaContains(float cx, float cy) {
		return ((cx > buttonBaseX && cx < buttonBaseX + buttonWidth) &&
		        (cy > buttonBaseY && cy < buttonBaseY + buttonOffset * maxResultsShown));
	}

	/**
	 * Sets a clip to the download result button area.
	 * @param g the graphics context
	 */
	public static void clipToResultArea(Graphics g) {
		g.setClip((int) buttonBaseX, (int) buttonBaseY, (int) buttonWidth, (int) (buttonOffset * maxResultsShown));
	}

	/**
	 * Sets a clip to the download area.
	 * @param g the graphics context
	 */
	public static void clipToDownloadArea(Graphics g) {
		g.setClip((int) infoBaseX, (int) infoBaseY, (int) infoWidth, (int) (infoHeight * maxDownloadsShown));
	}

	/**
	 * Returns true if the coordinates are within the bounds of the
	 * download information button at the given index.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @param index the index (to offset the button from the topmost button)
	 */
	public static boolean downloadContains(float cx, float cy, int index) {
		float y = infoBaseY + (index * infoHeight);
		return ((cx > infoBaseX && cx <= containerWidth) &&
		        (cy > y && cy < y + infoHeight));
	}

	/**
	 * Returns true if the coordinates are within the bounds of the
	 * download action icon at the given index.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @param index the index (to offset the button from the topmost button)
	 */
	public static boolean downloadIconContains(float cx, float cy, int index) {
		int iconWidth = GameImage.DELETE.getImage().getWidth();
		float edgeX = infoBaseX + infoWidth * 0.985f;
		float y = infoBaseY + (index * infoHeight);
		float marginY = infoHeight * 0.04f;
		return ((cx > edgeX - iconWidth && cx < edgeX) &&
		        (cy > y + marginY && cy < y + marginY + iconWidth));
	}

	/**
	 * Returns the download results button offset.
	 * @return the offset
	 */
	public static float getButtonOffset() { return buttonOffset; }

	/**
	 * Returns the download info height.
	 * @return the height
	 */
	public static float getInfoHeight() { return infoHeight; }

	/**
	 * Returns true if the coordinates are within the bounds of the
	 * download information button area.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public static boolean downloadAreaContains(float cx, float cy) {
		return ((cx > infoBaseX && cx <= containerWidth) &&
		        (cy > infoBaseY && cy < infoBaseY + infoHeight * maxDownloadsShown));
	}

	/**
	 * Draws the scroll bar for the download result buttons.
	 * @param g the graphics context
	 * @param position the start button index
	 * @param total the total number of buttons
	 */
	public static void drawResultScrollbar(Graphics g, float position, float total) {
		UI.drawScrollbar(g, position, total, maxResultsShown * buttonOffset, buttonBaseX, buttonBaseY,
				buttonWidth * 1.01f, (maxResultsShown-1) * buttonOffset + buttonHeight,
				Colors.BLACK_BG_NORMAL, Color.white, true);
	}

	/**
	 * Draws the scroll bar for the download information area.
	 * @param g the graphics context
	 * @param index the start index
	 * @param total the total number of downloads
	 */
	public static void drawDownloadScrollbar(Graphics g, float index, float total) {
		UI.drawScrollbar(g, index, total, maxDownloadsShown * infoHeight, infoBaseX, infoBaseY,
				infoWidth, maxDownloadsShown * infoHeight, Colors.BLACK_BG_NORMAL, Color.white, true);
	}

	/**
	 * Constructor.
	 * @param beatmapSetID the beatmap set ID
	 * @param date the last modified date string
	 * @param title the song title
	 * @param titleUnicode the Unicode song title (or {@code null} if none)
	 * @param artist the song artist
	 * @param artistUnicode the Unicode song artist (or {@code null} if none)
	 * @param creator the beatmap creator
	 */
	public DownloadNode(int beatmapSetID, String date, String title,
			String titleUnicode, String artist, String artistUnicode, String creator) {
		this.beatmapSetID = beatmapSetID;
		this.date = date;
		this.title = title;
		this.titleUnicode = titleUnicode;
		this.artist = artist;
		this.artistUnicode = artistUnicode;
		this.creator = creator;
	}

	/**
	 * Creates a download object for this node.
	 * @param server the server to download from
	 * @see #getDownload()
	 */
	public void createDownload(DownloadServer server) {
		if (download != null)
			return;

		String url = server.getDownloadURL(beatmapSetID);
		if (url == null)
			return;
		String path = String.format("%s%c%d", Options.getImportDir(), File.separatorChar, beatmapSetID);
		String rename = String.format("%d %s - %s.osz", beatmapSetID, artist, title);
		Download download = new Download(url, path, rename);
		download.setListener(new DownloadListener() {
			@Override
			public void completed() {
				UI.getNotificationManager().sendNotification(String.format("Download complete: %s", getTitle()), Colors.GREEN);
			}

			@Override
			public void error() {
				UI.getNotificationManager().sendNotification("Download failed due to a connection error.", Color.red);
			}
		});
		download.setRequestHeaders(server.getDownloadRequestHeaders());
		download.setSSLCertValidation(!server.disableSSLInDownloads());
		this.download = download;
		if (Options.useUnicodeMetadata())  // load glyphs
			Fonts.loadGlyphs(Fonts.LARGE, getTitle());
	}

	/**
	 * Returns the associated download object, or null if none.
	 * @see #createDownload(DownloadServer)
	 */
	public Download getDownload() { return download; }

	/**
	 * Clears the associated download object, if any.
	 * @see #createDownload(DownloadServer)
	 */
	public void clearDownload() { download = null; }

	/**
	 * Returns the beatmap set ID.
	 */
	public int getID() { return beatmapSetID; }

	/**
	 * Returns the last updated date.
	 */
	public String getDate() { return date; }

	/**
	 * Returns the song title.
	 * If configured, the Unicode string will be returned instead.
	 */
	public String getTitle() {
		return (Options.useUnicodeMetadata() && titleUnicode != null && !titleUnicode.isEmpty()) ? titleUnicode : title;
	}

	/**
	 * Returns the song artist.
	 * If configured, the Unicode string will be returned instead.
	 */
	public String getArtist() {
		return (Options.useUnicodeMetadata() && artistUnicode != null && !artistUnicode.isEmpty()) ? artistUnicode : artist;
	}

	/**
	 * Returns the song creator.
	 */
	public String getCreator() { return creator; }

	/**
	 * Draws the download result as a rectangular button.
	 * @param g the graphics context
	 * @param position the index (to offset the button from the topmost button)
	 * @param hover true if the mouse is hovering over this button
	 * @param focus true if the button is focused
	 * @param previewing true if the beatmap is currently being previewed
	 */
	public void drawResult(Graphics g, float position, boolean hover, boolean focus, boolean previewing) {
		float textX = buttonBaseX + buttonWidth * 0.001f;
		float edgeX = buttonBaseX + buttonWidth * 0.985f;
		float y = buttonBaseY + position;
		float marginY = buttonHeight * 0.04f;
		Download dl = DownloadList.get().getDownload(beatmapSetID);

		// rectangle outline
		g.setColor((focus) ? Colors.BLACK_BG_FOCUS : (hover) ? Colors.BLACK_BG_HOVER : Colors.BLACK_BG_NORMAL);
		g.fillRect(buttonBaseX, y, buttonWidth, buttonHeight);

		// map is already loaded
		if (BeatmapSetList.get().containsBeatmapSetID(beatmapSetID)) {
			g.setColor(Colors.BLUE_BUTTON);
			g.fillRect(buttonBaseX, y, buttonWidth, buttonHeight);
		}

		// download progress
		if (dl != null) {
			float progress = dl.getProgress();
			if (progress > 0f) {
				g.setColor(Colors.GREEN);
				g.fillRect(buttonBaseX, y, buttonWidth * progress / 100f, buttonHeight);
			}
		}

		// preview button
		Image img = (previewing) ? GameImage.MUSIC_PAUSE.getImage() : GameImage.MUSIC_PLAY.getImage();
		img.drawCentered(textX + img.getWidth() / 2, y + buttonHeight / 2f);
		textX += img.getWidth() + buttonWidth * 0.001f;

		// text
		// TODO: if the title/artist line is too long, shorten it (e.g. add "...") instead of just clipping
		if (Options.useUnicodeMetadata()) {  // load glyphs
			Fonts.loadGlyphs(Fonts.BOLD, getTitle());
			Fonts.loadGlyphs(Fonts.BOLD, getArtist());
		}
		// TODO can't set clip again or else old clip will be cleared
		//g.setClip((int) textX, (int) (y + marginY), (int) (edgeX - textX - Fonts.DEFAULT.getWidth(creator)), Fonts.BOLD.getLineHeight());
		Fonts.BOLD.drawString(
				textX, y + marginY,
				String.format("%s - %s%s", getArtist(), getTitle(),
						(dl != null) ? String.format(" [%s]", dl.getStatus().getName()) : ""), Color.white);
		//g.clearClip();
		Fonts.DEFAULT.drawString(
				textX, y + marginY + Fonts.BOLD.getLineHeight(),
				String.format("Last updated: %s", date), Color.white);
		Fonts.DEFAULT.drawString(
				edgeX - Fonts.DEFAULT.getWidth(creator), y + marginY,
				creator, Color.white);
	}

	/**
	 * Draws the download information.
	 * @param g the graphics context
	 * @param position the index (to offset from the topmost position)
	 * @param id the list index
	 * @param hover true if the mouse is hovering over this button
	 */
	public void drawDownload(Graphics g, float position, int id, boolean hover) {
		Download download = this.download;  // in case clearDownload() is called asynchronously
		if (download == null) {
			ErrorHandler.error("Trying to draw download information for button without Download object.", null, false);
			return;
		}

		float textX = infoBaseX + infoWidth * 0.02f;
		float edgeX = infoBaseX + infoWidth * 0.985f;
		float y = infoBaseY + position;
		float marginY = infoHeight * 0.04f;

		// rectangle outline
		g.setColor((id % 2 == 0) ? Colors.BLACK_BG_HOVER : Colors.BLACK_BG_NORMAL);
		g.fillRect(infoBaseX, y, infoWidth, infoHeight);

		// text
		String info;
		Status status = download.getStatus();
		float progress = download.getProgress();
		if (progress < 0f)
			info = status.getName();
		else if (status == Download.Status.WAITING)
			info = String.format("%s...", status.getName());
		else {
			if (hover && status == Download.Status.DOWNLOADING)
				info = String.format("%s: %s left (%s)", status.getName(), download.getTimeRemaining(), download.getDownloadSpeed());
			else
				info = String.format("%s: %.1f%% (%s/%s)", status.getName(), progress,
						Utils.bytesToString(download.readSoFar()), Utils.bytesToString(download.contentLength()));
		}
		Fonts.BOLD.drawString(textX, y + marginY, getTitle(), Color.white);
		Fonts.DEFAULT.drawString(textX, y + marginY + Fonts.BOLD.getLineHeight(), info, Color.white);

		// 'x' button
		if (hover) {
			Image img = GameImage.DELETE.getImage();
			img.draw(edgeX - img.getWidth(), y + marginY);
		}
	}

	@Override
	public String toString() {
		return String.format("[%d] %s - %s (by %s)", beatmapSetID, getArtist(), getTitle(), creator);
	}
}
