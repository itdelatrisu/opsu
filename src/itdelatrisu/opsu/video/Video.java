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

package itdelatrisu.opsu.video;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

import net.indiespot.media.VideoStream;
import net.indiespot.media.impl.VideoMetadata;

/**
 * Video player (with no audio).
 *
 * @author Riven (base, heavily modified)
 */
public class Video implements Closeable {
	/** The source file. */
	private final File file;

	/** The video metadata. */
	private final VideoMetadata metadata;

	/** The frame interval. */
	private final long frameInterval;

	/** The current video frame. */
	private final Image image;

	/** The video stream. */
	private VideoStream videoStream;

	/** The initial frame time. */
	private long initFrame;

	/** The current frame index. */
	private int videoIndex;

	/** Whether the video is finished playing. */
	private boolean finished;

	/** Whether video data has been initialized. */
	private boolean initialized;

	/** Whether the video stream is closed. */
	private boolean closed;

	/** The time when the video was paused, or 0 if not paused. */
	private long pauseFrame;

	/**
	 * Creates a video from the source file.
	 * Call {@link #seek(int)} to load the video stream.
	 * @param file the source video file
	 */
	public Video(File file) throws IOException, SlickException {
		this.file = file;
		this.metadata = FFmpeg.extractMetadata(file);
		this.frameInterval = (long) (1000_000_000L / metadata.framerate);
		this.image = new Image(metadata.width, metadata.height, Image.FILTER_LINEAR);
		this.finished = false;
		this.initialized = false;
		this.closed = false;
		this.pauseFrame = 0;
	}

	/**
	 * Seeks to a position.
	 * @param msec the time offset (in milliseconds)
	 */
	public void seek(int msec) throws IOException {
		if (videoStream != null && !closed)
			videoStream.close();
		this.videoStream = getVideoStreamAtOffset(Math.max(0, msec));
		this.finished = false;
		this.initialized = false;
		this.pauseFrame = 0;
	}

	/** Returns a video stream from the given millisecond offset. */
	private VideoStream getVideoStreamAtOffset(int msec) throws IOException {
		InputStream rgb24Stream = FFmpeg.extractVideoAsRGB24(file, msec);
		return new VideoStream(rgb24Stream, metadata);
	}

	/** Returns whether the video has started playing. */
	public boolean isStarted() { return initialized; }

	/** Returns whether the video has finished playing. */
	public boolean isFinished() { return finished; }

	/** Pauses the video. */
	public void pause() {
		if (pauseFrame == 0)
			pauseFrame = System.nanoTime();
	}

	/** Resumes the video. */
	public void resume() {
		if (pauseFrame > 0) {
			if (initialized)
				initFrame += (System.nanoTime() - pauseFrame);
			pauseFrame = 0;
		}
	}

	/**
	 * Renders the current frame.
	 * @param x the top-left x coordinate
	 * @param y the top-left y coordinate
	 * @param width the width to render at
	 * @param height the height to render at
	 * @param alpha the alpha level to render at
	 */
	public void render(int x, int y, int width, int height, float alpha) {
		image.setAlpha(alpha);
		image.draw(x, y, width, height);
	}

	/** Returns whether the next frame time has passed. */
	private boolean isTimeForNextFrame(long syncTime) { return hasVideoBacklogOver(0, syncTime); }

	/** Returns whether the frame backlog is over the given number of frames. */
	private boolean hasVideoBacklogOver(int frameCount, long syncTime) {
		return (videoIndex + frameCount) * frameInterval <= syncTime;
	}

	/**
	 * Updates the video, syncing based on an internal timer.
	 */
	public void update() { update(System.nanoTime() - initFrame); }

	/**
	 * Updates the video, syncing based on a given time.
	 * @param syncTime the millisecond time the video should sync to (in the
	 *                 forward direction only), relative to the initial time
	 *                 passed to {@link #seek(int)}
	 */
	public void update(int syncTime) { update(syncTime * 1_000_000L); }

	/**
	 * Updates the video.
	 * @param syncTime the nanosecond time the video should sync to (forward direction only)
	 */
	private void update(long syncTime) {
		if (finished || closed || pauseFrame > 0 || videoStream == null)
			return;

		// initialize frames
		if (!initialized) {
			videoIndex = 0;
			initFrame = System.nanoTime();
			if (pauseFrame != 0)
				pauseFrame = initFrame;

			// change pixel store alignment to prevent distortion
			GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
			GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);

			initialized = true;
		}

		if (!isTimeForNextFrame(syncTime))
			return;

		// grab and skip frames (if needed)
		ByteBuffer texBuffer = null;
		final int backlog = 5;
		int framesRead = 0;
		do {
			// free extra frames
			if (framesRead > 0) {
				videoStream.freeFrameData(texBuffer);
				texBuffer = null;
				videoIndex++;
			}

			// grab next frame
			texBuffer = videoStream.pollFrameData();
			if (texBuffer == VideoStream.EOF) {
				finished = true;
				return;
			}
			if (texBuffer == null)
				return;

			framesRead++;
		} while (hasVideoBacklogOver(backlog, syncTime));

		// render to texture
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, image.getTexture().getTextureID());
		GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, metadata.width, metadata.height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, texBuffer);

		videoStream.freeFrameData(texBuffer);
		videoIndex++;
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			videoStream.close();
			try {
				image.destroy();
			} catch (SlickException e) {}
		}
	}
}
