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

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.options.Options;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.newdawn.slick.util.Log;

/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Watches the beatmap directory tree for changes.
 *
 * @author The Java Tutorials (http://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java) (base)
 */
public class BeatmapWatchService {
	/** Beatmap watcher service instance. */
	private static BeatmapWatchService ws;

	/**
	 * Creates a new watch service instance (overwriting any previous instance),
	 * registers the beatmap directory, and starts processing events.
	 */
	public static void create() {
		// close the existing watch service
		destroy();

		// create a new watch service
		try {
			ws = new BeatmapWatchService();
			ws.register(Options.getBeatmapDir().toPath());
		} catch (IOException e) {
			ErrorHandler.error("An I/O exception occurred while creating the watch service.", e, true);
			return;
		}

		// start processing events
		ws.start();
	}

	/**
	 * Destroys the watch service instance, if any.
	 * Subsequent calls to {@link #get()} will return {@code null}.
	 */
	public static void destroy() {
		if (ws == null)
			return;

		try {
			ws.watcher.close();
			ws.service.shutdownNow();
			ws = null;
		} catch (IOException e) {
			ws = null;
			ErrorHandler.error("An I/O exception occurred while closing the previous watch service.", e, true);
		}
	}

	/**
	 * Returns the single instance of this class.
	 */
	public static BeatmapWatchService get() { return ws; }

	/** Watch service listener interface. */
	public interface BeatmapWatchServiceListener {
		/**
		 * Indication that an event was received.
		 * @param kind the event kind
		 * @param child the child directory
		 */
		public void eventReceived(WatchEvent.Kind<?> kind, Path child);
	}

	/** The list of listeners. */
	private static final List<BeatmapWatchServiceListener> listeners = new ArrayList<BeatmapWatchServiceListener>();

	/**
	 * Adds a listener.
	 * @param listener the listener to add
	 */
	public static void addListener(BeatmapWatchServiceListener listener) { listeners.add(listener); }

	/**
	 * Removes a listener.
	 * @param listener the listener to remove
	 */
	public static void removeListener(BeatmapWatchServiceListener listener) { listeners.remove(listener); }

	/**
	 * Removes all listeners.
	 */
	public static void removeListeners() { listeners.clear(); }

	/** The watch service. */
	private final WatchService watcher;

	/** The WatchKey -> Path mapping for registered directories. */
	private final Map<WatchKey, Path> keys;

	/** The Executor. */
	private ExecutorService service;

	/** Whether the watch service is paused (i.e. does not fire events). */
	private boolean paused = false;

	/**
	 * Creates the WatchService.
	 * @throws IOException if an I/O error occurs
	 */
	private BeatmapWatchService() throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new ConcurrentHashMap<WatchKey, Path>();
	}

	/**
	 * Register the given directory with the WatchService.
	 * @param dir the directory to register
	 * @throws IOException if an I/O error occurs
	 */
	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher,
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY);
		keys.put(key, dir);
	}

	/**
	 * Register the given directory, and all its sub-directories, with the WatchService.
	 * @param start the root directory to register
	 */
	public void registerAll(final Path start) {
		try {
			Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					try {
						register(dir);
					} catch (IOException e) {
						Log.warn(String.format("Failed to register path '%s' with the watch service.", dir.toString()), e);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			Log.warn(String.format("Failed to register paths from root directory '%s' with the watch service.", start.toString()), e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	/**
	 * Start processing events in a new thread.
	 */
	private void start() {
		if (service != null)
			return;

		this.service = Executors.newCachedThreadPool();
		service.submit(new Runnable() {
			@Override
			public void run() { ws.processEvents(); }
		});
	}

	/**
	 * Process all events for keys queued to the watcher
	 */
	private void processEvents() {
		while (true) {
			// wait for key to be signaled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException | ClosedWatchServiceException e) {
				return;
			}

			Path dir = keys.get(key);
			if (dir == null)
				continue;

			boolean isPaused = paused;
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				if (kind == StandardWatchEventKinds.OVERFLOW)
					continue;

				// context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);
				//System.out.printf("%s: %s\n", kind.name(), child);

				// fire listeners
				if (!isPaused) {
					for (BeatmapWatchServiceListener listener : listeners)
						listener.eventReceived(kind, child);
				}

				// if directory is created, then register it and its sub-directories
				if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS))
						registerAll(child);
				}
			}

			// reset key and remove from set if directory no longer accessible
			if (!key.reset()) {
				keys.remove(key);
				if (keys.isEmpty())
					break;  // all directories are inaccessible
			}
		}
	}

	/**
	 * Stops listener events from being fired.
	 */
	public void pause() { paused = true; }

	/**
	 * Resumes firing listener events.
	 */
	public void resume() { paused = false; }
}
