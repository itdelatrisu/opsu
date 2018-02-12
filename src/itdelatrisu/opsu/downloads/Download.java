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
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.options.Options;

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
import java.util.Map;

import org.newdawn.slick.util.Log;

/**
 * File download.
 */
public class Download {
	/** Connection timeout, in ms. */
	public static final int CONNECTION_TIMEOUT = 5000;

	/** Read timeout, in ms. */
	public static final int READ_TIMEOUT = 10000;

	/** Maximum number of HTTP/HTTPS redirects to follow. */
	public static final int MAX_REDIRECTS = 3;

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
		private final String name;

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

		/** Indication that an error has occurred. */
		public void error();
	}

	/** The local path. */
	private String localPath;

	/** The local path to rename the file to when finished. */
	private String rename;

	/** The download URL. */
	private URL url;

	/** The download listener. */
	private DownloadListener listener;

	/** The additional HTTP request headers. */
	private Map<String, String> requestHeaders;

	/** Whether SSL certificate validation should be disabled. */
	private boolean disableSSLCertValidation = false;

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

	/** EWMA download speed. */
	private long avgDownloadSpeed = 0;

	/** EWMA smoothing factor (alpha) for computing average download speed. */
	private static final double DOWNLOAD_SPEED_SMOOTHING = 0.25;

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
		this.rename = Utils.cleanFileName(rename, '-');
	}

	/**
	 * Returns the remote download URL.
	 */
	public URL getRemoteURL() { return url; }

	/**
	 * Returns the local path to save the download (after renamed).
	 */
	public String getLocalPath() { return (rename != null) ? rename : localPath; }

	/**
	 * Sets the download listener.
	 * @param listener the listener to set
	 */
	public void setListener(DownloadListener listener) { this.listener = listener; }

	/**
	 * Sets additional HTTP headers to use in the download request.
	 * @param headers the map of headers (key -> value)
	 */
	public void setRequestHeaders(Map<String, String> headers) { this.requestHeaders = headers; }

	/**
	 * Switches validation of SSL certificates on or off.
	 * @param enabled whether to validate SSL certificates
	 */
	public void setSSLCertValidation(boolean enabled) { this.disableSSLCertValidation = !enabled; }

	/**
	 * Starts the download from the "waiting" status.
	 * @return the started download thread, or {@code null} if none started
	 */
	public Thread start() {
		if (status != Status.WAITING)
			return null;

		Thread t = new Thread() {
			@Override
			public void run() {
				// open connection
				HttpURLConnection conn = null;
				try {
					if (disableSSLCertValidation)
						Utils.setSSLCertValidation(false);

					URL downloadURL = url;
					int redirectCount = 0;
					boolean isRedirect = false;
					do {
						isRedirect = false;

						conn = (HttpURLConnection) downloadURL.openConnection();
						conn.setConnectTimeout(CONNECTION_TIMEOUT);
						conn.setReadTimeout(READ_TIMEOUT);
						conn.setUseCaches(false);

						// allow HTTP <--> HTTPS redirects
						// http://download.java.net/jdk7u2/docs/technotes/guides/deployment/deployment-guide/upgrade-guide/article-17.html
						conn.setInstanceFollowRedirects(false);
						conn.setRequestProperty("User-Agent", Options.USER_AGENT);
						if (requestHeaders != null) {
							for (Map.Entry<String, String> entry : requestHeaders.entrySet())
								conn.setRequestProperty(entry.getKey(), entry.getValue());
						}

						// check for redirect
						int status = conn.getResponseCode();
						if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM ||
						    status == HttpURLConnection.HTTP_SEE_OTHER || status == HttpURLConnection.HTTP_USE_PROXY) {
							URL base = conn.getURL();
							String location = conn.getHeaderField("Location");
							URL target = null;
							if (location != null)
								target = new URL(base, location);
							conn.disconnect();

							// check for problems
							String error = null;
							if (location == null)
								error = String.format("Download for URL '%s' is attempting to redirect without a 'location' header.", base.toString());
							else if (!target.getProtocol().equals("http") && !target.getProtocol().equals("https"))
								error = String.format("Download for URL '%s' is attempting to redirect to a non-HTTP/HTTPS protocol '%s'.", base.toString(), target.getProtocol());
							else if (redirectCount > MAX_REDIRECTS)
								error = String.format("Download for URL '%s' is attempting too many redirects (over %d).", base.toString(), MAX_REDIRECTS);
							if (error != null) {
								ErrorHandler.error(error, null, false);
								throw new IOException();
							}

							// follow redirect
							downloadURL = target;
							redirectCount++;
							isRedirect = true;
						}
					} while (isRedirect);

					// store content length
					contentLength = conn.getContentLength();
				} catch (IOException e) {
					status = Status.ERROR;
					Log.warn("Failed to open connection.", e);
					if (listener != null)
						listener.error();
					return;
				} finally {
					if (disableSSLCertValidation)
						Utils.setSSLCertValidation(true);
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
					long bytesRead = fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					if (status == Status.DOWNLOADING) {  // not interrupted
						// check if the entire file was received
						if (bytesRead < contentLength) {
							status = Status.ERROR;
							Log.warn(String.format("Download '%s' failed: %d bytes expected, %d bytes received.", url.toString(), contentLength, bytesRead));
							if (listener != null)
								listener.error();
							return;
						}

						// mark download as complete
						status = Status.COMPLETE;
						rbc.close();
						fos.close();
						if (rename != null) {
							Path source = new File(localPath).toPath();
							Files.move(source, source.resolveSibling(rename), StandardCopyOption.REPLACE_EXISTING);
						}
						if (listener != null)
							listener.completed();
					}
				} catch (Exception e) {
					status = Status.ERROR;
					Log.warn("Failed to start download.", e);
					if (listener != null)
						listener.error();
				}
			}
		};
		t.start();
		return t;
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
			return (rbc != null) ? rbc.getReadSoFar() : contentLength;
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
			this.avgDownloadSpeed = 0;
			return;
		}

		// update download speed and ETA
		if (System.currentTimeMillis() > lastReadSoFarTime + UPDATE_INTERVAL) {
			long readSoFar = readSoFar();
			long readSoFarTime = System.currentTimeMillis();
			long dlspeed = (readSoFar - lastReadSoFar) * 1000 / (readSoFarTime - lastReadSoFarTime);
			if (dlspeed > 0) {
				this.avgDownloadSpeed = (avgDownloadSpeed == 0) ? dlspeed :
					(long) (DOWNLOAD_SPEED_SMOOTHING * dlspeed + (1 - DOWNLOAD_SPEED_SMOOTHING) * avgDownloadSpeed);
				this.lastDownloadSpeed = String.format("%s/s", Utils.bytesToString(avgDownloadSpeed));
				long t = (contentLength - readSoFar) / avgDownloadSpeed;
				if (t >= 3600)
					this.lastTimeRemaining = String.format("%dh%dm%ds", t / 3600, (t / 60) % 60, t % 60);
				else
					this.lastTimeRemaining = String.format("%dm%ds", t / 60, t % 60);
			} else {
				this.avgDownloadSpeed = 0;
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
