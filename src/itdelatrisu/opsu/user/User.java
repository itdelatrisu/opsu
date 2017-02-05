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

package itdelatrisu.opsu.user;

/**
 * User profile.
 */
public class User implements Comparable<User> {
	/** Display name. */
	private String name;

	/** Total score. */
	private long score;

	/** Total accuracy. */
	private double accuracy;

	/** Total number of plays passed. */
	private int playsPassed;

	/** Total number of plays. */
	private int playsTotal;

	/** Current level. */
	private int level;

	/** Next level progress. */
	private double levelProgress;

	/** Profile icon identifier. */
	private int icon;

	/**
	 * Creates a new user with the given name and icon.
	 * @param name the user's name
	 * @param icon the user's icon
	 */
	public User(String name, int icon) { this(name, 0L, 0.0, 0, 0, icon); }

	/**
	 * Creates a user with existing stats.
	 * @param name the user's name
	 * @param score the user's total score
	 * @param accuracy the user's total accuracy
	 * @param playsPassed the user's total passed play count
	 * @param playsTotal the user's total play count
	 * @param icon the user's icon identifier
	 */
	public User(String name, long score, double accuracy, int playsPassed, int playsTotal, int icon) {
		this.name = name;
		this.score = score;
		this.accuracy = accuracy;
		this.playsPassed = playsPassed;
		this.playsTotal = playsTotal;
		this.icon = icon;
		calculateLevel();
	}

	/**
	 * Adds stats from a play (passed).
	 * @param score the score for the game
	 * @param accuracy the accuracy for the game
	 */
	public void add(long score, double accuracy) {
		this.score += score;
		this.accuracy = ((this.accuracy * this.playsPassed) + accuracy) / (this.playsPassed + 1);
		this.playsPassed++;
		this.playsTotal++;
		calculateLevel();
	}

	/**
	 * Adds stats from a play (failed).
	 * @param score the score for the game
	 */
	public void add(long score) {
		this.score += score;
		this.playsTotal++;
		calculateLevel();
	}

	/** Returns the user's name. */
	public String getName() { return name; }

	/** Returns the user's total score. */
	public long getScore() { return score; }

	/** Returns the user's total accuracy. */
	public double getAccuracy() { return accuracy; }

	/** Returns the user's total passed play count. */
	public int getPassedPlays() { return playsPassed; }

	/** Returns the user's total play count. */
	public int getTotalPlays() { return playsTotal; }

	/** Returns the user's icon identifier. */
	public int getIconId() { return icon; }

	/** Returns the user's level. */
	public int getLevel() { return level; }

	/** Returns the progress to the next level in [0,1). */
	public double getNextLevelProgress() { return levelProgress; }

	/** Calculates the user's current level and next level progress. */
	private void calculateLevel() {
		if (score == 0) {
			this.level = 1;
			this.levelProgress = 0.0;
			return;
		}

		int l;
		for (l = 1; this.score >= getScoreForLevel(l); l++) {}
		l--;
		this.level = l;
		long baseScore = getScoreForLevel(l);
		this.levelProgress = (double) (this.score - baseScore) / (getScoreForLevel(l + 1) - baseScore);
	}

	/**
	 * Returns the total score needed for a given level:
	 * <ul>
	 * <li><strong>Level <= 100:</strong>
	 * <p>5,000 / 3 * (4n^3 - 3n^2 - n) + 1.25 * 1.8^(n - 60)
	 * <li><strong>Level > 100:</strong>
	 * <p>26,931,190,829 + 100,000,000,000 * (n - 100)
	 * </ul>
	 * @param level the level
	 * @return the total score needed
	 * @see <a href="https://osu.ppy.sh/wiki/Score#Level">https://osu.ppy.sh/wiki/Score#Level</a>
	 */
	private static long getScoreForLevel(int level) {
		if (level <= 1)
			return 1L;
		else if (level <= 100)
			return (long) (5000.0 / 3 * (4 * Math.pow(level, 3) - 3 * Math.pow(level, 2) - level) + 1.25 * Math.pow(1.8, level - 60));
		else
			return 26_931_190_829L + 100_000_000_000L * (level - 100);
	}

	/** Sets the user's name. */
	public void setName(String name) { this.name = name; }

	/** Sets the user's icon identifier. */
	public void setIconId(int id) { this.icon = id; }

	@Override
	public int compareTo(User other) {
		return this.getName().compareToIgnoreCase(other.getName());
	}
}
