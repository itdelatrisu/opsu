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

package itdelatrisu.opsu.beatmap;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Log;

/**
 * LRU cache for beatmap background images.
 */
public class BeatmapImageCache {
	/** Maximum number of cached images. */
	private static final int MAX_CACHE_SIZE = 10;

	/** Map of all loaded background images. */
	private LinkedHashMap<File, Image> cache;

	/**
	 * Constructor.
	 */
	@SuppressWarnings("serial")
	public BeatmapImageCache() {
		this.cache = new LinkedHashMap<File, Image>(MAX_CACHE_SIZE + 1, 1.1f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<File, Image> eldest) {
				if (size() > MAX_CACHE_SIZE) {
					// destroy the eldest image
					Image img = eldest.getValue();
					if (img != null && !img.isDestroyed()) {
						try {
							img.destroy();
						} catch (SlickException e) {
							Log.warn(String.format("Failed to destroy image '%s'.", img.getResourceReference()), e);
						}
					}
					return true;
				}
				return false;
			}
		};
	}

	/**
	 * Returns the image mapped to the specified beatmap.
     * @param beatmap the Beatmap
	 * @return the Image, or {@code null} if no such mapping exists
	 */
	public Image get(Beatmap beatmap) { return cache.get(beatmap.bg); }

	/**
	 * Creates a mapping from the specified beatmap to the given image.
	 * @param beatmap the Beatmap
	 * @param image the Image
	 * @return the previously mapped Image, or {@code null} if no such mapping existed
	 */
	public Image put(Beatmap beatmap, Image image) { return cache.put(beatmap.bg, image); }

	/**
	 * Removes all entries from the cache.
	 * <p>
	 * NOTE: This does NOT destroy the images in the cache, and will cause
	 * memory leaks if all images have not been destroyed.
	 */
	public void clear() { cache.clear(); }
}
