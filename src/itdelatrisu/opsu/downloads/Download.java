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

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * File download.
 */
public class Download {
	/** Connection timeout, in ms. */
	public static final int CONNECTION_TIMEOUT = 5000;

	/** Read timeout, in ms. */
	public static final int READ_TIMEOUT = 10000;

	/** Time between download speed and ETA updates, in ms. */
	private static final int UPDATE_INTERVAL = 1000;

	/** Download statuses. */
	public enum Status {
		WAITING ("Waiting"),
		DOWNLOADING ("Downloading"),
		COMPLETE ("Complete"),
		CANCELLED ("Cancelled"),
		ERROR ("Error");

		/** The status name. */
		private String name;

		/**
		 * Constructor.
		 * @param name the status name.
		 */
		Status(String name) {
			this.name = name;
		}

		/**
		 * Returns the status name.
		 */
		public String getName() { return name; }
	}

	/** Download listener interface. */
	public interface DownloadListener {
		/** Indication that a download has completed. */
		public void completed();
	}

	/** The local path. */
	private String localPath;

	/** The local path to rename the file to when finished. */
	private String rename;

	/** The download URL. */
	private URL url;

	/** The download listener. */
	private DownloadListener listener;

	/** The readable byte channel. */
	private ReadableByteChannelWrapper rbc;

	/** The file output stream. */
	private FileOutputStream fos;

	/** The size of the download. */
	private int contentLength = -1;

	/** The download status. */
	private Status status = Status.WAITING;

	/** Time when lastReadSoFar was updated. */
	private long lastReadSoFarTime = -1;

	/** Last readSoFar amount. */
	private long lastReadSoFar = -1;

	/** Last calculated download speed string. */
	private String lastDownloadSpeed;

	/** Last calculated ETA string. */
	private String lastTimeRemaining;

	/**
	 * Constructor.
	 * @param remoteURL the download URL
	 * @param localPath the path to save the download
	 */
	public Download(String remoteURL, String localPath) {
		this(remoteURL, localPath, null);
	}

	/**
	 * Constructor.
	 * @param remoteURL the download URL
	 * @param localPath the path to save the download
	 * @param rename the file name to rename the download to when complete
	 */
	public Download(String remoteURL, String localPath, String rename) {
		try {
			this.url = new URL(remoteURL);
		} catch (MalformedURLException e) {
			this.status = Status.ERROR;
			ErrorHandler.error(String.format("Bad download URL: '%s'", remoteURL), e, true);
			return;
		}
		this.localPath = localPath;
		this.rename = rename;
	}

	/**
	 * Sets the download listener.
	 * @param listener the listener to set
	 */
	public void setListener(DownloadListener listener) { this.listener = listener; }

	/**
	 * Starts the download from the "waiting" status.
	 */
	public void start() {
		if (status != Status.WAITING)
			return;

		new Thread() {
			@Override
			public void run() {
				// open connection, get content length
				HttpURLConnection conn = null;
				try {
					conn = (HttpURLConnection) url.openConnection();
					conn.setConnectTimeout(CONNECTION_TIMEOUT);
					conn.setReadTimeout(READ_TIMEOUT);
					conn.setUseCaches(false);
					contentLength = conn.getContentLength();
				} catch (IOException e) {
					status = Status.ERROR;
					ErrorHandler.error("Failed to open connection.", e, false);
					return;
				}

				// download file
				try (
					InputStream in = conn.getInputStream();
					ReadableByteChannel readableByteChannel = Channels.newChannel(in);
					FileOutputStream fileOutputStream = new FileOutputStream(localPath);
				) {
					rbc = new ReadableByteChannelWrapper(readableByteChannel);
					fos = fileOutputStream;
					status = Status.DOWNLOADING;
					updateReadSoFar();
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					if (status == Status.DOWNLOADING) {  // not interrupted
						status = Status.COMPLETE;
						rbc.close();
						fos.close();
						if (rename != null) {
							String cleanedName = Utils.cleanFileName(rename, '-');
							Path source = new File(localPath).toPath();
							Files.move(source, source.resolveSibling(cleanedName), StandardCopyOption.REPLACE_EXISTING);
						}
						if (listener != null)
							listener.completed();
					}
				} catch (Exception e) {
					status = Status.ERROR;
					ErrorHandler.error("Failed to start download.", e, false);
				}
			}
		}.start();
	}

	/**
	 * Returns the download status.
	 */
	public Status getStatus() { return status; }

	/**
	 * Returns true if transfers are currently taking place.
	 */
	public boolean isTransferring() {
		return (rbc != null && rbc.isOpen() && fos != null && fos.getChannel().isOpen());
	}

	/**
	 * Returns true if the download is active.
	 */
	public boolean isActive() {
		return (status == Status.WAITING || status == Status.DOWNLOADING);
	}

	/**
	 * Returns the size of the download content in bytes, or -1 if not calculated
	 * (or if an error has occurred).
	 */
	public int contentLength() { return contentLength; }

	/**
	 * Returns the download completion percentage, or -1f if an error has occurred.
	 */
	public float getProgress() {
		switch (status) {
		case WAITING:
			return 0f;
		case COMPLETE:
			return 100f;
		case DOWNLOADING:
			if (rbc != null && fos != null && contentLength > 0)
				return (float) rbc.getReadSoFar() / (float) contentLength * 100f;
			else
				return 0f;
		case CANCELLED:
		case ERROR:
		default:
			return -1f;
		}
	}

	/**
	 * Returns the number of bytes read so far.
	 */
	public long readSoFar() {
		switch (status) {
		case COMPLETE:
			return contentLength;
		case DOWNLOADING:
			if (rbc != null)
				return rbc.getReadSoFar();
			// else fall through
		case WAITING:
		case CANCELLED:
		case ERROR:
		default:
			return 0;
		}
	}

	/**
	 * Returns the last calculated download speed, or null if not downloading.
	 */
	public String getDownloadSpeed() {
		updateReadSoFar();
		return lastDownloadSpeed;
	}

	/**
	 * Returns the last calculated ETA, or null if not downloading.
	 */
	public String getTimeRemaining() {
		updateReadSoFar();
		return lastTimeRemaining;
	}

	/**
	 * Updates the last readSoFar and related fields.
	 */
	private void updateReadSoFar() {
		// only update while downloading
		if (status != Status.DOWNLOADING) {
			this.lastDownloadSpeed = null;
			this.lastTimeRemaining = null;
			return;
		}

		// update download speed and ETA
		if (System.currentTimeMillis() > lastReadSoFarTime + UPDATE_INTERVAL) {
			long readSoFar = readSoFar();
			long readSoFarTime = System.currentTimeMillis();
			long dlspeed = (readSoFar - lastReadSoFar) * 1000 / (readSoFarTime - lastReadSoFarTime);
			if (dlspeed > 0) {
				this.lastDownloadSpeed = String.format("%s/s", Utils.bytesToString(dlspeed));
				long t = (contentLength - readSoFar) / dlspeed;
				if (t >= 3600)
					this.lastTimeRemaining = String.format("%dh%dm%ds", t / 3600, (t / 60) % 60, t % 60);
				else
					this.lastTimeRemaining = String.format("%dm%ds", t / 60, t % 60);
			} else {
				this.lastDownloadSpeed = String.format("%s/s", Utils.bytesToString(0));
				this.lastTimeRemaining = "?";
			}
			this.lastReadSoFarTime = readSoFarTime;
			this.lastReadSoFar = readSoFar;
		}

		// first call
		else if (lastReadSoFarTime <= 0) {
			this.lastReadSoFar = readSoFar();
			this.lastReadSoFarTime = System.currentTimeMillis();
		}
	}

	/**
	 * Cancels the download, if running.
	 */
	public void cancel() {
		try {
			this.status = Status.CANCELLED;
			boolean transferring = isTransferring();
			if (rbc != null && rbc.isOpen())
				rbc.close();
			if (fos != null && fos.getChannel().isOpen())
				fos.close();
			if (transferring) {
				File f = new File(localPath);
				if (f.isFile())
					f.delete();
			}
		} catch (IOException e) {
			this.status = Status.ERROR;
			ErrorHandler.error("Failed to cancel download.", e, true);
		}
	}
}
