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

package itdelatrisu.opsu.replay;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.ScoreData;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.io.OsuReader;
import itdelatrisu.opsu.io.OsuWriter;
import itdelatrisu.opsu.options.Options;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.newdawn.slick.util.Log;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAInputStream;
import org.tukaani.xz.LZMAOutputStream;

/**
 * Captures osu! replay data.
 * https://osu.ppy.sh/wiki/Osr_%28file_format%29
 *
 * @author smoogipooo (https://github.com/smoogipooo/osu-Replay-API/)
 */
public class Replay {
	/** The associated file. */
	private File file;

	/** The associated score data. */
	private ScoreData scoreData;

	/** Whether or not the replay data has been loaded from the file. */
	public boolean loaded = false;

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
	public int score;

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

	/** Seed. (?) */
	public int seed;

	/** Seed string. */
	private static final String SEED_STRING = "-12345";

	/**
	 * Empty constructor.
	 */
	public Replay() {}

	/**
	 * Constructor.
	 * @param file the file to load from
	 */
	public Replay(File file) {
		this.file = file;
	}

	/**
	 * Loads the replay data.
	 * @throws IOException failure to load the data
	 */
	public void load() throws IOException {
		if (loaded)
			return;

		OsuReader reader = new OsuReader(file);
		loadHeader(reader);
		loadData(reader);
		reader.close();
		loaded = true;
	}

	/**
	 * Loads the replay header only.
	 * @throws IOException failure to load the data
	 */
	public void loadHeader() throws IOException {
		OsuReader reader = new OsuReader(file);
		loadHeader(reader);
		reader.close();
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
			} catch (NumberFormatException e) {
				Log.warn(String.format("Failed to load life frame: '%s'", frame), e);
			}
		}
		this.lifeFrames = lifeFrameList.toArray(new LifeFrame[lifeFrameList.size()]);

		// timestamp
		this.timestamp = reader.readDate();

		// LZMA-encoded replay data
		this.replayLength = reader.readInt();
		if (replayLength > 0) {
			LZMAInputStream lzma = new LZMAInputStream(reader.getInputStream());
			String[] replayFrames = Utils.convertStreamToString(lzma).split(",");
			lzma.close();
			List<ReplayFrame> replayFrameList = new ArrayList<ReplayFrame>(replayFrames.length);
			int lastTime = 0;
			for (String frame : replayFrames) {
				if (frame.isEmpty())
					continue;
				String[] tokens = frame.split("\\|");
				if (tokens.length < 4)
					continue;
				try {
					if (tokens[0].equals(SEED_STRING)) {
						seed = Integer.parseInt(tokens[3]);
						continue;
					}
					int timeDiff = Integer.parseInt(tokens[0]);
					int time = timeDiff + lastTime;
					float x = Float.parseFloat(tokens[1]);
					float y = Float.parseFloat(tokens[2]);
					int keys = Integer.parseInt(tokens[3]);
					replayFrameList.add(new ReplayFrame(timeDiff, time, x, y, keys));
					lastTime = time;
				} catch (NumberFormatException e) {
					Log.warn(String.format("Failed to parse frame: '%s'", frame), e);
				}
			}
			this.frames = replayFrameList.toArray(new ReplayFrame[replayFrameList.size()]);
		}
	}

	/**
	 * Returns a ScoreData object encapsulating all replay data.
	 * If score data already exists, the existing object will be returned
	 * (i.e. this will not overwrite existing data).
	 * @param beatmap the beatmap
	 * @return the ScoreData object
	 */
	public ScoreData getScoreData(Beatmap beatmap) {
		if (scoreData != null)
			return scoreData;

		scoreData = new ScoreData();
		scoreData.timestamp = file.lastModified() / 1000L;
		scoreData.MID = beatmap.beatmapID;
		scoreData.MSID = beatmap.beatmapSetID;
		scoreData.title = beatmap.title;
		scoreData.artist = beatmap.artist;
		scoreData.creator = beatmap.creator;
		scoreData.version = beatmap.version;
		scoreData.hit300 = hit300;
		scoreData.hit100 = hit100;
		scoreData.hit50 = hit50;
		scoreData.geki = geki;
		scoreData.katu = katu;
		scoreData.miss = miss;
		scoreData.score = score;
		scoreData.combo = combo;
		scoreData.perfect = perfect;
		scoreData.mods = mods;
		scoreData.replayString = getReplayFilename();
		scoreData.playerName = playerName;
		return scoreData;
	}

	/**
	 * Saves the replay data to a file in the replays directory.
	 */
	public void save() {
		// create replay directory
		File dir = Options.getReplayDir();
		if (!dir.isDirectory()) {
			if (!dir.mkdir()) {
				ErrorHandler.error("Failed to create replay directory.", null, false);
				return;
			}
		}

		// write file in new thread
		final File file = new File(dir, String.format("%s.osr", getReplayFilename()));
		new Thread() {
			@Override
			public void run() {
				try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
					OsuWriter writer = new OsuWriter(out);

					// header
					writer.write(mode);
					writer.write(version);
					writer.write(beatmapHash);
					writer.write(playerName);
					writer.write(replayHash);
					writer.write(hit300);
					writer.write(hit100);
					writer.write(hit50);
					writer.write(geki);
					writer.write(katu);
					writer.write(miss);
					writer.write(score);
					writer.write(combo);
					writer.write(perfect);
					writer.write(mods);

					// life data
					StringBuilder sb = new StringBuilder();
					if (lifeFrames != null && lifeFrames.length > 0) {
						NumberFormat nf = new DecimalFormat("##.##");
						int lastFrameTime = 0;
						for (int i = 0; i < lifeFrames.length; i++) {
							LifeFrame frame = lifeFrames[i];
							if (i > 0 && frame.getTime() - lastFrameTime < LifeFrame.SAMPLE_INTERVAL)
								continue;

							sb.append(String.format("%d|%s,", frame.getTime(), nf.format(frame.getHealth())));
							lastFrameTime = frame.getTime();
						}
					}
					writer.write(sb.toString());

					// timestamp
					writer.write(timestamp);

					// LZMA-encoded replay data
					if (frames != null && frames.length > 0) {
						// build full frame string
						NumberFormat nf = new DecimalFormat("###.#####");
						sb = new StringBuilder();
						for (int i = 0; i < frames.length; i++) {
							ReplayFrame frame = frames[i];
							sb.append(String.format("%d|%s|%s|%d,",
									frame.getTimeDiff(), nf.format(frame.getX()),
									nf.format(frame.getY()), frame.getKeys()));
						}
						sb.append(String.format("%s|0|0|%d", SEED_STRING, seed));

						// get bytes from string
						CharsetEncoder encoder = StandardCharsets.US_ASCII.newEncoder();
						CharBuffer buffer = CharBuffer.wrap(sb);
						byte[] bytes = encoder.encode(buffer).array();

						// compress data
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						LZMAOutputStream lzma = new LZMAOutputStream(bout, new LZMA2Options(), bytes.length);
						try {
							lzma.write(bytes);
						} catch (IOException e) {
							ErrorHandler.error("LZMA encoding of the reply frames failed.", e, true);
						}
						lzma.close();
						bout.close();

						// write to file
						byte[] compressed = bout.toByteArray();
						writer.write(compressed.length);
						writer.write(compressed);
					} else
						writer.write(0);

					writer.close();
				} catch (IOException e) {
					ErrorHandler.error("Could not save replay data.", e, true);
				}
			}
		}.start();
	}

	/**
	 * Returns the file name of where the replay should be saved and loaded,
	 * or null if the required fields are not set.
	 */
	public String getReplayFilename() {
		if (replayHash == null)
			return null;

		return String.format("%s-%d%d%d%d%d%d",
				replayHash, hit300, hit100, hit50, geki, katu, miss);
	}

	@Override
	public String toString() {
		final int LINE_SPLIT = 5;
		final int MAX_LINES = LINE_SPLIT * 10;

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
		sb.append("Life data ("); sb.append(lifeFrames.length); sb.append(" total):\n");
		for (int i = 0; i < lifeFrames.length && i < MAX_LINES; i++) {
			if (i % LINE_SPLIT == 0)
				sb.append('\t');
			sb.append(lifeFrames[i]);
			sb.append((i % LINE_SPLIT == LINE_SPLIT - 1) ? '\n' : ' ');
		}
		sb.append('\n');
		sb.append("Timestamp: "); sb.append(timestamp); sb.append('\n');
		sb.append("Replay length: "); sb.append(replayLength); sb.append('\n');
		if (frames != null) {
			sb.append("Frames ("); sb.append(frames.length); sb.append(" total):\n");
			for (int i = 0; i < frames.length && i < MAX_LINES; i++) {
				if (i % LINE_SPLIT == 0)
					sb.append('\t');
				sb.append(frames[i]);
				sb.append((i % LINE_SPLIT == LINE_SPLIT - 1) ? '\n' : ' ');
			}
			sb.append('\n');
		}
		sb.append("Seed: "); sb.append(seed); sb.append('\n');
		return sb.toString();
	}
}
