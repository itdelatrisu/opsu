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
import fluddokt.opsu.fake.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
//import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
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

	/** The local path. */
	private File localFile;

	/** The local path to rename the file to when finished. */
	private String rename;

	/** The download URL. */
	private URL url;

	/** The readable byte channel. */
	private ReadableByteChannelWrapper rbc;

	/** The file output stream. */
	private FileOutputStream fos;

	/** The size of the download. */
	private int contentLength = -1;

	/** The download status. */
	private Status status = Status.WAITING;

	Thread dlThread;
	
	private long lastUpdateETA;
	private long lastReadSoFar;
	String ETAstr = "Start";
	/**
	 * Constructor.
	 * @param remoteURL the download URL
	 * @param localPath the path to save the download
	 */
	public Download(String remoteURL, File localPath) {
		this(remoteURL, localPath, null);
	}

	/**
	 * Constructor.
	 * @param remoteURL the download URL
	 * @param file the path to save the download
	 * @param rename the file name to rename the download to when complete
	 */
	public Download(String remoteURL, File file, String rename) {
		try {
			this.url = new URL(remoteURL);
		} catch (MalformedURLException e) {
			this.status = Status.ERROR;
			ErrorHandler.error(String.format("Bad download URL: '%s'", remoteURL), e, true);
			return;
		}
		this.localFile = file;
		this.rename = rename;
	}

	/**
	 * Starts the download from the "waiting" status.
	 */
	public void start() {
		if (status != Status.WAITING)
			return;

		dlThread = new Thread() {
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
					if(contentLength<0)
						throw new IOException("Negative content length");
				} catch (IOException e) {
					status = Status.ERROR;
					ErrorHandler.error("Failed to open connection.", e, false);
					return;
				}

				// download file
				try (
					InputStream in = conn.getInputStream();
					ReadableByteChannel readableByteChannel = Channels.newChannel(in);
					FileOutputStream fileOutputStream = new FileOutputStream(localFile.getIOFile());
				) {
					rbc = new ReadableByteChannelWrapper(readableByteChannel);
					fos = fileOutputStream;
					FileChannel foschannel = fos.getChannel();
					status = Status.DOWNLOADING;
					int total = 0;
					
					
					/*
					ByteBuffer buf = ByteBuffer.allocate(8192*32);
					long len = 0;
					while(status == Status.DOWNLOADING && (len = rbc.read(buf)) != -1) {
						buf.flip();
						fos.getChannel().write(buf);
						buf.clear();
					}
					/*/
					while(status == Status.DOWNLOADING && total < contentLength){
						long readed = foschannel.transferFrom(rbc, total, Math.min(2048, contentLength-total));
						total += readed;
						//System.out.println("readed "+readed);
					}
					//*/
					if (status == Status.DOWNLOADING) {  // not interrupted
						status = Status.COMPLETE;
						rbc.close();
						fos.close();
						if (rename != null) {
							String cleanedName = Utils.cleanFileName(rename, '-');
							move(localFile, new File(localFile.getParentFile(),cleanedName));
							//Path source = localFile.toPath();
							//Files.move(source, source.resolveSibling(cleanedName), StandardCopyOption.REPLACE_EXISTING);
						}
					}else{
						status = Status.CANCELLED;
					}
				} catch (Exception e) {
					status = Status.ERROR;
					ErrorHandler.error("Failed to start download.", e, false);
				}
			}

			//http://stackoverflow.com/questions/4770004/how-to-move-rename-file-from-internal-app-storage-to-external-storage-on-android
			private void move(File src, File dst) throws IOException {
				FileChannel inChannel = new FileInputStream(src.getIOFile()).getChannel();
			    FileChannel outChannel = new FileOutputStream(dst.getIOFile()).getChannel();
			    try
			    {
			        inChannel.transferTo(0, inChannel.size(), outChannel);
			    }
			    finally
			    {
			        if (inChannel != null)
			            inChannel.close();
			        if (outChannel != null)
			            outChannel.close();
			    }
			    src.delete();
			}
		};
		dlThread.start();
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
	
	public String ETA(){
		if(lastUpdateETA<=0){
			lastUpdateETA = System.currentTimeMillis();
			lastReadSoFar = readSoFar();
		}
		if(System.currentTimeMillis() > lastUpdateETA + 1000){
			long thisReadSoFar = readSoFar();
			long thisUpdateETA = System.currentTimeMillis();
			long dlspeed = (thisReadSoFar-lastReadSoFar)*1000/(thisUpdateETA-lastUpdateETA);
			if(dlspeed>0)
				ETAstr = Utils.bytesToString(dlspeed)+"/s "+ (timeStr((contentLength-thisReadSoFar)/dlspeed));
			else
				ETAstr = "";
			lastUpdateETA = thisUpdateETA;
			lastReadSoFar = thisReadSoFar;
		}
		return ETAstr;
	}
	public String timeStr(long t){
		//t/=1000;
		return t/60+"m"+t%60+"s";
	}

	/**
	 * Cancels the download, if running.
	 */
	public void cancel() {
		try {
			this.status = Status.CANCELLED;
			boolean transferring = isTransferring();
			dlThread.interrupt();
			try {
				dlThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (rbc != null && rbc.isOpen())
				rbc.close();
			if (fos != null && fos.getChannel().isOpen())
				fos.close();
			if (transferring) {
				File f = localFile;
				if (f.isFile())
					f.delete();
			}
		} catch (IOException e) {
			this.status = Status.ERROR;
			ErrorHandler.error("Failed to cancel download.", e, true);
		}
	}
}
