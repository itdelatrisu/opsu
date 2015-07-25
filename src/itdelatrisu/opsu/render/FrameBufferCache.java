/*
 *  opsu! - an open-source osu! client
 *  Copyright (C) 2014, 2015 Jeffrey Han
 *
 *  opsu! is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  opsu! is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */
package itdelatrisu.opsu.render;

import itdelatrisu.opsu.beatmap.HitObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This is cache for OpenGL FrameBufferObjects. This is currently only used
 * to draw curve objects of the new slider style. Does currently not integrate
 * well and requires some manual OpenGL state manipulation to use it.
 *
 * @author Bigpet {@literal <dravorek (at) gmail.com>}
 */
public class FrameBufferCache {
	/** The single framebuffer cache instance. */
	private static FrameBufferCache instance = null;

	/** The mapping from hit objects to framebuffers. */
	private Map<HitObject, Rendertarget> cacheMap;

	/** */
	private ArrayList<Rendertarget> cache;

	/** Container dimensions. */
	public static int width, height;

	/**
	 * Set the width and height of the framebuffers in this cache.
	 * Should be called before anything is inserted into the map.
	 * @param width the container width
	 * @param height the container height
	 */
	public static void init(int width, int height) {
		FrameBufferCache.width = width;
		FrameBufferCache.height = height;
	}

	/**
	 * Constructor.
	 */
	private FrameBufferCache() {
		cache = new ArrayList<Rendertarget>();
		cacheMap = new HashMap<HitObject, Rendertarget>();
	}

	/**
	 * Check if there is a framebuffer object mapped to {@code obj}.
	 * @param obj the hit object
	 * @return true if there is a framebuffer mapped for this {@code HitObject}, else false
	 */
	public boolean contains(HitObject obj) {
		return cacheMap.containsKey(obj);
	}

	/**
	 * Get the {@code Rendertarget} mapped to {@code obj}.
	 * @param obj the hit object
	 * @return the {@code Rendertarget} if there's one mapped to {@code obj}, otherwise null
	 */
	public Rendertarget get(HitObject obj) {
		return cacheMap.get(obj);
	}

	/**
	 * Clear the mapping for {@code obj} to free it up to get used by another {@code HitObject}.
	 * @param obj the hit object
	 * @return true if there was a mapping for {@code obj} and false if there was no mapping for it.
	 */
	public boolean freeMappingFor(HitObject obj) {
		return cacheMap.remove(obj) != null;
	}

	/**
	 * Clear the cache of all the mappings. This does not actually delete the
	 * cached framebuffers, it merely frees them all up to get mapped anew.
	 */
	public void freeMap() {
		cacheMap.clear();
	}

	/**
	 * Create a mapping from {@code obj} to a framebuffer. If there was already
	 * a mapping from {@code obj} this will associate another framebuffer with it
	 * (thereby freeing up the previously mapped framebuffer).
	 * @param obj the hit object
	 * @return the {@code Rendertarget} newly mapped to {@code obj}
	 */
	public Rendertarget insert(HitObject obj) {
		// find first RTTFramebuffer that's not mapped to anything and return it
		Rendertarget buffer;
		for (int i = 0; i < cache.size(); ++i) {
			buffer = cache.get(i);
			if (!cacheMap.containsValue(buffer)) {
				cacheMap.put(obj, buffer);
				return buffer;
			}
		}

		// no unmapped RTTFramebuffer found, create a new one
		buffer = Rendertarget.createRTTFramebuffer(width, height);
		//buffer = Rendertarget.createRTTFramebuffer(512, 512);
		
		cache.add(buffer);
		cacheMap.put(obj, buffer);
		return buffer;
	}

	/**
	 * Clear the cache pool of Framebuffers.
	 * If there were any previous Framebuffers in the cache, delete them.
	 * <p>
	 * This is necessary for cases when the game gets restarted with a
	 * different resolution without closing the process.
	 */
	public static void shutdown() {
		FrameBufferCache fbcInstance = FrameBufferCache.getInstance();
		for (Rendertarget target : fbcInstance.cache) {
			target.destroyRTT();
		}
		fbcInstance.cache.clear();
		fbcInstance.freeMap();
	}

	/**
	 * There should only ever be one framebuffer cache, this function returns
	 * that one framebuffer cache instance.
	 * If there was no instance created already then this function creates it.
	 * @return the instance of FrameBufferCache
	 */
	public static FrameBufferCache getInstance() {
		if (instance == null)
			instance = new FrameBufferCache();
		return instance;
	}
}
