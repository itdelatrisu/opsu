package org.newdawn.slick.openal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A utility to provide a simple and rational interface to the
 * slick internals
 * 
 * @author kevin
 */
public class AudioLoader {
	/** AIF Format Indicator */
	private static final String AIF = "AIF";
	/** WAV Format Indicator */
	private static final String WAV = "WAV";
	/** OGG Format Indicator */
	private static final String OGG = "OGG";
	/** MOD/XM Format Indicator */
	private static final String MOD = "MOD";
	/** MOD/XM Format Indicator */
	private static final String XM = "XM";

	/** True if the audio loader has be initialised */
	private static boolean inited = false;
	
	/**
	 * Initialise the audio loader 
	 */
	private static void init() {
		if (!inited) {
			SoundStore.get().init();
			inited = true;
		}
	}
	
	/**
	 * Get audio data in a playable state by loading the complete audio into 
	 * memory.
	 * 
	 * @param format The format of the audio to be loaded (something like "XM" or "OGG")
	 * @param in The input stream from which to load the audio data
	 * @return An object representing the audio data 
	 * @throws IOException Indicates a failure to access the audio data
	 */
	public static Audio getAudio(String format, InputStream in) throws IOException {
		init();
		
		if (format.equals(AIF)) {
			return SoundStore.get().getAIF(in);
		}
		if (format.equals(WAV)) {
			return SoundStore.get().getWAV(in);
		}
		if (format.equals(OGG)) {
			return SoundStore.get().getOgg(in);
		}
		
		throw new IOException("Unsupported format for non-streaming Audio: "+format);
	}
	
	/**
	 * Get audio data in a playable state by setting up a stream that can be piped into
	 * OpenAL - i.e. streaming audio
	 * 
	 * @param format The format of the audio to be loaded (something like "XM" or "OGG")
	 * @param url The location of the data that should be streamed
	 * @return An object representing the audio data 
	 * @throws IOException Indicates a failure to access the audio data
	 */
	public static Audio getStreamingAudio(String format, URL url) throws IOException {
		init();
		
		if (format.equals(OGG)) {
			return SoundStore.get().getOggStream(url);
		}
		if (format.equals(MOD)) {
			return SoundStore.get().getMOD(url.openStream());
		}
		if (format.equals(XM)) {
			return SoundStore.get().getMOD(url.openStream());
		}
		
		throw new IOException("Unsupported format for streaming Audio: "+format);
	}
	
	/**
	 * Allow the streaming system to update itself
	 */
	public static void update() {
		init();
		
		SoundStore.get().poll(0);
	}
}
