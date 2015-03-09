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

package itdelatrisu.opsu.replay;

import itdelatrisu.opsu.OsuReader;
import itdelatrisu.opsu.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.newdawn.slick.util.Log;

/**
 * Captures osu! replay data.
 * https://osu.ppy.sh/wiki/Osr_%28file_format%29
 *
 * @author smoogipooo (https://github.com/smoogipooo/osu-Replay-API/)
 */
public class Replay {
	/** The associated file. */
	private File file;

	/** The game mode. */
	public byte mode;

	/** Game version when the replay was created. */
	public int version;

	/** Beatmap MD5 hash. */
	public String beatmapHash;

	/** The player's name. */
	public String playerName;

	/** Replay MD5 hash. */
	public String replayHash;

	/** Hit result counts. */
	public short hit300, hit100, hit50, geki, katu, miss;

	/** The score. */
	public long score;

	/** The max combo. */
	public short combo;

	/** Whether or not a full combo was achieved. */
	public boolean perfect;

	/** Game mod bitmask. */
	public int mods;

	/** Life frames. */
	public LifeFrame[] lifeFrames;

	/** The time when the replay was created. */
	public Date timestamp;

	/** Length of the replay data. */
	public int replayLength;

	/** Replay frames. */
	public ReplayFrame[] frames;

	/**
	 * Constructor.
	 * @param file the file to load from
	 */
	public Replay(File file) {
		this.file = file;
		try {
			OsuReader reader = new OsuReader(file);
			loadHeader(reader);
			loadData(reader);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the replay header data.
	 * @param reader the associated reader
	 * @throws IOException
	 */
	private void loadHeader(OsuReader reader) throws IOException {
		this.mode = reader.readByte();
		this.version = reader.readInt();
		this.beatmapHash = reader.readString();
		this.playerName = reader.readString();
		this.replayHash = reader.readString();
		this.hit300 = reader.readShort();
		this.hit100 = reader.readShort();
		this.hit50 = reader.readShort();
		this.geki = reader.readShort();
		this.katu = reader.readShort();
		this.miss = reader.readShort();
		this.score = reader.readInt();
		this.combo = reader.readShort();
		this.perfect = reader.readBoolean();
		this.mods = reader.readInt();
	}

	/**
	 * Loads the replay data.
	 * @param reader the associated reader
	 * @throws IOException
	 */
	private void loadData(OsuReader reader) throws IOException {
		// life data
		String[] lifeData = reader.readString().split(",");
		List<LifeFrame> lifeFrameList = new ArrayList<LifeFrame>(lifeData.length);
		for (String frame : lifeData) {
			String[] tokens = frame.split("\\|");
			if (tokens.length < 2)
				continue;
			try {
				int time = Integer.parseInt(tokens[0]);
				float percentage = Float.parseFloat(tokens[1]);
				lifeFrameList.add(new LifeFrame(time, percentage));
			} catch (NumberFormatException | NullPointerException e) {
				Log.warn(String.format("Failed to life frame: '%s'", frame), e);
			}
		}
		this.lifeFrames = lifeFrameList.toArray(new LifeFrame[lifeFrameList.size()]);

		// timestamp
		this.timestamp = reader.readDate();

		// LZMA-encoded replay data
		this.replayLength = reader.readInt();
		if (replayLength > 0) {
			LZMACompressorInputStream lzma = new LZMACompressorInputStream(reader.getInputStream());
			String[] replayFrames = Utils.convertStreamToString(lzma).split(",");
			List<ReplayFrame> replayFrameList = new ArrayList<ReplayFrame>(replayFrames.length);
			int lastTime = 0;
			for (String frame : replayFrames) {
				if (frame.isEmpty())
					continue;
				String[] tokens = frame.split("\\|");
				if (tokens.length < 4)
					continue;
				try {
					int timeDiff = Integer.parseInt(tokens[0]);
					int time = timeDiff + lastTime;
					float x = Float.parseFloat(tokens[1]);
					float y = Float.parseFloat(tokens[2]);
					int keys = Integer.parseInt(tokens[3]);
					replayFrameList.add(new ReplayFrame(timeDiff, time, x, y, keys));
					lastTime = time;
				} catch (NumberFormatException | NullPointerException e) {
					Log.warn(String.format("Failed to parse frame: '%s'", frame), e);
				}
			}
			this.frames = replayFrameList.toArray(new ReplayFrame[replayFrameList.size()]);
		}
	}

	@Override
	public String toString() {
		final int LINE_SPLIT = 10;
		StringBuilder sb = new StringBuilder();
		sb.append("File: "); sb.append(file.getName()); sb.append('\n');
		sb.append("Mode: "); sb.append(mode); sb.append('\n');
		sb.append("Version: "); sb.append(version); sb.append('\n');
		sb.append("Beatmap hash: "); sb.append(beatmapHash); sb.append('\n');
		sb.append("Player name: "); sb.append(playerName); sb.append('\n');
		sb.append("Replay hash: "); sb.append(replayHash); sb.append('\n');
		sb.append("Hits: ");
		sb.append(hit300); sb.append(' ');
		sb.append(hit100); sb.append(' ');
		sb.append(hit50); sb.append(' ');
		sb.append(geki); sb.append(' ');
		sb.append(katu); sb.append(' ');
		sb.append(miss); sb.append('\n');
		sb.append("Score: "); sb.append(score); sb.append('\n');
		sb.append("Max combo: "); sb.append(combo); sb.append('\n');
		sb.append("Perfect: "); sb.append(perfect); sb.append('\n');
		sb.append("Mods: "); sb.append(mods); sb.append('\n');
		sb.append("Life data:\n");
		for (int i = 0; i < lifeFrames.length; i++) {
			if (i % LINE_SPLIT == 0)
				sb.append('\t');
			sb.append(lifeFrames[i]);
			sb.append((i % LINE_SPLIT == LINE_SPLIT - 1) ? '\n' : ' ');
		}
		sb.append('\n');
		sb.append("Timestamp: "); sb.append(timestamp); sb.append('\n');
		sb.append("Replay length: "); sb.append(replayLength); sb.append('\n');
		if (frames != null) {
			sb.append("Frames:\n");
			for (int i = 0; i < frames.length; i++) {
				if (i % LINE_SPLIT == 0)
					sb.append('\t');
				sb.append(frames[i]);
				sb.append((i % LINE_SPLIT == LINE_SPLIT - 1) ? '\n' : ' ');
			}
		}
		return sb.toString();
	}
}
