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

package itdelatrisu.opsu.beatmap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.ImageData;
import org.newdawn.slick.opengl.ImageDataFactory;
import org.newdawn.slick.opengl.LoadableImageData;
import org.newdawn.slick.util.Log;

/**
 * Simple threaded image loader for a single image file.
 */
public class ImageLoader {
	/** The image file. */
	private final File file;

	/** The loaded image. */
	private Image image;

	/** The image data. */
	private LoadedImageData data;

	/** The image loader thread. */
	private Thread loaderThread;

	/** ImageData wrapper, needed because {@code ImageIOImageData} doesn't implement {@code getImageBufferData()}. */
	private class LoadedImageData implements ImageData {
		/** The image data implementation. */
		private final ImageData imageData;

		/** The stored image. */
		private final ByteBuffer buffer;

		/**
		 * Constructor.
		 * @param imageData the class holding the image properties
		 * @param buffer the stored image
		 */
		public LoadedImageData(ImageData imageData, ByteBuffer buffer) {
			this.imageData = imageData;
			this.buffer = buffer;
		}

		@Override public int getDepth() { return imageData.getDepth(); }
		@Override public int getWidth() { return imageData.getWidth(); }
		@Override public int getHeight() { return imageData.getHeight();}
		@Override public int getTexWidth() { return imageData.getTexWidth(); }
		@Override public int getTexHeight() { return imageData.getTexHeight(); }
		@Override public ByteBuffer getImageBufferData() { return buffer; }
	}

	/** Image loading thread. */
	private class ImageLoaderThread extends Thread {
		/** The image file input stream. */
		private BufferedInputStream in;

		@Override
		public void interrupt() {
			super.interrupt();
			if (in != null) {
				try {
					in.close();  // interrupt I/O
				} catch (IOException e) {}
			}
		}

		@Override
		public void run() {
			// load image data into a ByteBuffer to use constructor Image(ImageData)
			LoadableImageData imageData = ImageDataFactory.getImageDataFor(file.getAbsolutePath());
			try (BufferedInputStream in = this.in = new BufferedInputStream(new FileInputStream(file))) {
				ByteBuffer textureBuffer = imageData.loadImage(in, false, null);
				if (!isInterrupted())
					data = new LoadedImageData(imageData, textureBuffer);
			} catch (Exception e) {
				if (!isInterrupted())
					Log.warn(String.format("Failed to load background image '%s'.", file), e);
			}
			this.in = null;
		}
	}

	/**
	 * Constructor. Call {@link ImageLoader#load(boolean)} to load the image.
	 * @param file the image file
	 */
	public ImageLoader(File file) {
		this.file = file;
	}

	/**
	 * Loads the image.
	 * @param threaded true to load the image data in a new thread
	 */
	public void load(boolean threaded) {
		if (!file.isFile())
			return;

		if (threaded) {
			if (loaderThread != null && loaderThread.isAlive())
				loaderThread.interrupt();
			loaderThread = new ImageLoaderThread();
			loaderThread.start();
		} else {
			try {
				image = new Image(file.getAbsolutePath());
			} catch (SlickException e) {
				Log.warn(String.format("Failed to load background image '%s'.", file), e);
			}
		}
	}

	/**
	 * Returns the image.
	 * @return the loaded image, or null if not loaded
	 */
	public Image getImage() {
		if (image == null && data != null) {
			image = new Image(data);
			data = null;
		}
		return image;
	}

	/**
	 * Returns whether an image is currently loading in another thread.
	 * @return true if loading, false otherwise
	 */
	public boolean isLoading() { return (loaderThread != null && loaderThread.isAlive()); }

	/**
	 * Interrupts the image loader, if running.
	 */
	public void interrupt() {
		if (isLoading())
			loaderThread.interrupt();
	}

	/**
	 * Releases all resources.
	 */
	public void destroy() {
		interrupt();
		loaderThread = null;
		if (image != null && !image.isDestroyed()) {
			try {
				image.destroy();
			} catch (SlickException e) {
				Log.warn(String.format("Failed to destroy image '%s'.", image.getResourceReference()), e);
			}
			image = null;
		}
		data = null;
	}
}
