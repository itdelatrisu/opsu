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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Least recently used cache.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
@SuppressWarnings("serial")
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
	/** The cache capacity. */
	private final int capacity;

	/**
	 * Creates a least recently used cache with the given capacity.
	 * @param capacity the capacity
	 */
	public LRUCache(int capacity) {
		super(capacity + 1, 1.1f, true);
		this.capacity = capacity;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		if (size() > capacity) {
			eldestRemoved(eldest);
			return true;
		}
		return false;
	}

	/**
	 * Notification that the eldest entry was removed.
	 * Can be used to clean up any resources when this happens (via override).
	 * @param eldest the removed entry
	 */
	public void eldestRemoved(Map.Entry<K, V> eldest) {}
}
