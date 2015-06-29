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

package itdelatrisu.opsu.audio;

import org.gstreamer.*;
import org.newdawn.slick.util.Log;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class GStreamerPlayer extends MusicPlayer {
	private Pipeline pipeline;

	private float rate = 1f;

	private boolean loop;

	/** Start gain for fading in/out */
	protected float fadeStartGain;

	/** End gain for fading in/out */
	protected float fadeEndGain;

	/** Countdown for fading in/out */
	protected int fadeTime;

	/** Duration for fading in/out */
	protected int fadeDuration;

	/** True if music should be stopped after fading in/out */
	protected boolean stopAfterFade;

	public static boolean check() {
		try {
			ElementFactory.make("filesrc", 		"sourceTest");
			ElementFactory.make("decodebin2", 	"decoderTest");
			ElementFactory.make("audioconvert", 	"converterTest");
			ElementFactory.make("volume", 		"volumeTest");
			ElementFactory.make("scaletempo",	"tempoTest");
			ElementFactory.make("pitch", 		"pitchTest");
			ElementFactory.make("autoaudiosink", 	"sinkTest");
			return true;
		} catch (Exception e) {
			Log.error(e);
			return false;
		}
	}

	@Override
	public void load(File file) {
        pipeline = new Pipeline();
		final Element src, decoder, conv, volume, tempo, pitch, reconv, sink;
		src	=	ElementFactory.make("filesrc", 		"source");
		decoder	=	ElementFactory.make("decodebin2", 	"decoder");
		conv	=	ElementFactory.make("audioconvert", 	"converter");
		volume	=	ElementFactory.make("volume", 		"volume");
		tempo	=	ElementFactory.make("scaletempo", 	"tempo");
		pitch	=	ElementFactory.make("pitch", 		"pitch");
		reconv	=	ElementFactory.make("audioconvert", 	"reconverter");
		sink	=	ElementFactory.make("autoaudiosink", 	"sink");

		pipeline.addMany(src, decoder, conv, volume, tempo, pitch, reconv, sink);

		// source ~> decoder ~> converter -> volume -> tempo -> pitch -> sink
		src.link(decoder);
		Element.linkMany(conv, volume, tempo, pitch, reconv, sink);

		// connect dynamic pads
		// http://gstreamer.freedesktop.org/data/doc/gstreamer/head/manual/html/section-components-decodebin.html
		decoder.connect(new Element.PAD_ADDED() {
			@Override
			public void padAdded(Element element, Pad pad) {
				pad.link(conv.getStaticPad("sink"));
			}
		});

		pipeline.getBus().connect(new Bus.ERROR() {
			public void errorMessage(GstObject source, int code, String message) {
				Log.error(message);
			}
		});

		pipeline.getBus().connect(new Bus.EOS() {
			public void endOfStream(GstObject source) {
				if (loop)
					setPosition(0);
			}
		});

		src.set("location", file.getAbsolutePath());
		volume.set("volume", this.volume);

		// getting everything in sync
		pipeline.pause();
		pipeline.getState();
	}

	@Override
	public void loop() {
		loop = true;
		setVolume(volume);
		pipeline.play();
	}

	@Override
	public void play() {
		loop = false;
		setVolume(volume);
		pipeline.play();
	}

	@Override
	public void setPosition(int time) {
		pipeline.getState(); // make sure elements are not in async state
		pipeline.seek(rate, Format.TIME, SeekFlags.FLUSH | SeekFlags.ACCURATE,
				SeekType.SET, TimeUnit.NANOSECONDS.convert(time, TimeUnit.MILLISECONDS), SeekType.NONE, -1);
		pipeline.getState(); // wait until async done
	}

	@Override
	public int getPosition() {
		return (int) (pipeline.queryPosition(TimeUnit.MILLISECONDS));
	}

	@Override
	public boolean isPlaying() {
		return pipeline != null && pipeline.isPlaying();
	}

	@Override
	public void stop() {
		pipeline.stop();
	}

	@Override
	public void pause() {
		pipeline.pause();
	}

	@Override
	public void resume() {
		pipeline.play();
	}

	@Override
	public void update(int delta) {
		if (fadeTime > 0) {
			fadeTime -= delta;

			if (fadeTime <= 0) {
				fadeTime = 0;

				if (stopAfterFade)
					pause();
			}

			float offset = (fadeEndGain - fadeStartGain) * (1 - (fadeTime / (float)fadeDuration));
			pipeline.getElementByName("volume").set("volume", fadeStartGain + offset); //TODO
		}
	}

	@Override
	public void fade(int duration, float endVolume, boolean stopAfterFade) {
		this.stopAfterFade = stopAfterFade;
		fadeStartGain = volume;
		fadeEndGain = endVolume;
		fadeDuration = duration;
		fadeTime = duration;
	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;
		if (pipeline != null) pipeline.getElementByName("volume").set("volume", volume);
	}

	@Override
	public void close() {
		if (pipeline != null) pipeline.setState(State.NULL);
	}

	@Override
	public void setPitch(float pitch) {
		pipeline.getElementByName("pitch").set("pitch", pitch);
	}

	@Override
	public void setSpeed(float speed) {
		if (speed == rate)
			return;

		rate = speed;

		// http://gstreamer.freedesktop.org/data/doc/gstreamer/head/manual/html/section-eventsseek.html
		pipeline.seek(rate, Format.TIME, SeekFlags.FLUSH | SeekFlags.KEY_UNIT | SeekFlags.ACCURATE,
				SeekType.SET, pipeline.queryPosition(Format.TIME), SeekType.NONE, -1);
	}
}
