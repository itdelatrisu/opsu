package itdelatrisu.opsu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import itdelatrisu.opsu.states.Options.OpsuOptions;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Log;

public class Resources {
	
	/**
	 * @param <T> the type of the loaded resource
	 */
	public interface Resource<T> {
		/**
		 * Declares the file name of this resource
		 * 
		 * @return does not include the file name extension
		 */
		String getName();
		
		/**
		 * Declares possible file name extensions for this resource
		 * 
		 * @return element of extensions including a leading dot. the extensions will be attempted in the given order.
		 */
		List<String> getExtensions();
		
		/**
		 * Declares where this resource may be found
		 * 
		 * @return order of the array determines order of origins when trying to locate the resource
		 */
		List<Origin> getOrigins();
		
		/**
		 * cleans up after loaded data
		 * 
		 * @param data not null
		 */
		void unload(T data);
	}
	
	public interface SoundResource extends Resource<Clip> {
		
	}
	
	public interface ImageResource extends Resource<Image> {
		
	}
	
	private class LoadedResource<T> {
		Origin origin;
		T data;
		boolean tryToOverride = false;
	}

	/**
	 * Enumerates the possible origins of a resource
	 */
	public enum Origin {
		/**
		 * bundled game resources
		 */
		GAME,
		SKIN,
		BEATMAP
	}

	private OpsuOptions options;
	private File currentBeatmapDir;
	
	public void setCurrentBeatmapDir(File currentBeatmapDir) {
		this.currentBeatmapDir = currentBeatmapDir;
		
		removeAll(Origin.BEATMAP);
	}

	public void removeAll(Origin origin) {
		for (Iterator<Entry<Resource<?>, LoadedResource<?>>> iterator = loadedResources.entrySet().iterator(); iterator.hasNext();) {
			Entry<Resource<?>, LoadedResource<?>> entry = iterator.next();
			Resource<?> resource = entry.getKey();
			LoadedResource<?> cached = entry.getValue();
			
			if(resource.getOrigins().indexOf(cached.origin) > 0)
				cached.tryToOverride = true;
			
			if(cached.origin == origin) {
				unload(entry);
				iterator.remove();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T> void unload(Entry<Resource<?>, LoadedResource<?>> entry) {
		((Resource<T>) entry.getKey()).unload((T) entry.getValue().data);
	}
	
	public Resources(OpsuOptions options) {
		this.options = options;
	}
	
	/**
	 * Locates a resource as declared by {@link OpsuOptions#isLoadFromOrigin(Resource, Origin)}
	 * @param resource
	 * @return null if not found
	 */
	@CheckForNull
	<T> LoadedResource<T> loadResource(Resource<?> resource, LoadedResource<T> existingResource) {
		System.out.println("loading " + resource.getName());
		for (Origin origin : resource.getOrigins()) {
			if(!options.isLoadFromOrigin(resource, origin))
				continue;
			if(existingResource != null && origin == existingResource.origin)
				return existingResource;
			
			for(String extension : resource.getExtensions()) {
				String filename = resource.getName() + extension;
				InputStream stream = openResource(filename, origin);
				if(stream != null) {
					@SuppressWarnings("unchecked")
					T data = (T) loadResource(resource, stream, filename);
					if(data != null) {
						LoadedResource<T> loadedResource = new LoadedResource<>();
						loadedResource.data = data;
						loadedResource.origin = origin;
						return loadedResource;
					}
				}
			}
		}
		return null;
	}
	
	@CheckForNull
	InputStream openResource(String resource, @Nonnull Origin origin) {
		switch (origin) {
		case GAME:
			return ClassLoader.getSystemResourceAsStream(resource);
		case SKIN:
			if(options.getSkinDir() == null)
				return null;
			try {
				return new FileInputStream(new File(options.getSkinDir(), resource));
			} catch (FileNotFoundException e) {
				return null;
			}
		case BEATMAP:
			if(currentBeatmapDir == null)
				return null;
			try {
				return new FileInputStream(new File(currentBeatmapDir, resource));
			} catch (FileNotFoundException e) {
				return null;
			}
		default:
			throw new RuntimeException();
		}
	}
	
	Map<Resource<?>, LoadedResource<?>> loadedResources = new HashMap<>();
	
	<T> T loadResource(Resource<T> resource, InputStream inputStream, String filename) {
		if (resource instanceof ImageResource) {
			try {
				@SuppressWarnings("unchecked")
				T image = (T) new Image(inputStream, filename, false);
				return image;
			} catch (SlickException e) {
				Log.error("can't load " + filename, e);
				return null;
			}
		}
		if(resource instanceof SoundResource) {
			@SuppressWarnings("unchecked")
			T clip = (T) loadClip(inputStream, filename);
			return clip;

		}
		
		throw new RuntimeException();
	}

	public static Clip loadClip(InputStream inputStream, String filename) {
		try {
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(inputStream);

			// GNU/Linux workaround
//				Clip clip = AudioSystem.getClip();
			AudioFormat format = audioIn.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			Clip clip = (Clip) AudioSystem.getLine(info);
			clip.open(audioIn);
			return clip;
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			Log.error(String.format("Failed to load file '%s'.", filename), e);
		}
		return null;
	}
	
	public void notifySkinDirChanged() {
		removeAll(Origin.SKIN);
	}
	
	public <T> T getResource(Resource<T> resource) {
		@SuppressWarnings("unchecked")
		LoadedResource<T> cached = (LoadedResource<T>) loadedResources.get(resource);
		
		if(cached != null && cached.tryToOverride == false) {
			return cached.data;
		}
		
		LoadedResource<T> loaded = loadResource(resource, cached);
		
		if(loaded != null) {
			if(loaded != cached) {
				loadedResources.put(resource, loaded);
			}
			
			return loaded.data;
		}
		
		return null;
	}
}
