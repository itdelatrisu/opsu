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

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.Utils;

/**
 * Health bar.
 *
 * @author peppy (ppy/osu-iPhone:OsuAppDelegate.m,OsuFunctions.h)
 */
public class Health {
	/** Maximum HP. */
	public static final float HP_MAX = 200f;

	/** HP increase values. */
	private static final float
		HP_50           = 0.4f,
		HP_100          = 2.2f,
		HP_300          = 6f,
		HP_100K         = 10f,   // 100-Katu
		HP_300K         = 10f,   // 300-Katu
		HP_300G         = 14f,   // Geki
		HP_MU           = 6f,
		HP_SLIDER10     = 3f,
		HP_SLIDER30     = 4f,
		HP_SPINNERSPIN  = 1.7f,
		HP_SPINNERBONUS = 2f;

	/** Current health. */
	private float health;

	/** Displayed health (for animation, slightly behind health). */
	private float healthDisplay;

	/** Uncapped health. */
	private float healthUncapped;

	/** The HP drain rate (0:easy ~ 10:hard) */
	private float hpDrainRate;

	/** The normal HP multiplier. */
	private float hpMultiplierNormal;

	/** The combo-end HP multiplier. */
	private float hpMultiplierComboEnd;

	/** Constructor. */
	public Health() {
		reset();
	}

	/**
	 * Sets the health modifiers.
	 * @param hpDrainRate the HP drain rate
	 * @param hpMultiplierNormal the normal HP multiplier
	 * @param hpMultiplierComboEnd the combo-end HP multiplier
	 */
	public void setModifiers(float hpDrainRate, float hpMultiplierNormal, float hpMultiplierComboEnd) {
		this.hpDrainRate = hpDrainRate;
		this.hpMultiplierNormal = hpMultiplierNormal;
		this.hpMultiplierComboEnd = hpMultiplierComboEnd;
	}

	/**
	 * Updates displayed health based on a delta value.
	 * @param delta the delta interval since the last call
	 */
	public void update(int delta) {
		float multiplier = delta / 32f;
		if (healthDisplay < health)
			healthDisplay = Utils.clamp(healthDisplay + (health - healthDisplay) * multiplier, 0f, health);
		else if (healthDisplay > health) {
			if (health < 10f)
				multiplier *= 10f;
			healthDisplay = Utils.clamp(healthDisplay - (healthDisplay - health) * multiplier, health, HP_MAX);
		}
	}

	/** Returns the current health percentage. */
	public float getHealth() { return health / HP_MAX * 100f; }

	/** Returns the displayed health percentage. */
	public float getHealthDisplay() { return healthDisplay / HP_MAX * 100f; }

	/** Returns the current health value. */
	public float getRawHealth() { return health; }

	/** Returns the current uncapped health value. */
	public float getUncappedRawHealth() { return healthUncapped; }

	/** Sets the current health value. */
	public void setHealth(float value) { health = healthUncapped = value; }

	/** Changes the current health by the given value. */
	public void changeHealth(float value) {
		health = Utils.clamp(health + value, 0f, HP_MAX);
		healthUncapped += value;
	}

	/**
	 * Changes the current health value based on a hit.
	 * @param type the hit type (HIT_* constants)
	 */
	public void changeHealthForHit(int type) {
		switch (type) {
		case GameData.HIT_MISS:
			changeHealth(Utils.mapDifficultyRange(hpDrainRate, -6, -25, -40));
			break;
		case GameData.HIT_50:
			changeHealth(hpMultiplierNormal * Utils.mapDifficultyRange(hpDrainRate, HP_50 * 8, HP_50, HP_50));
			break;
		case GameData.HIT_100:
			changeHealth(hpMultiplierNormal * Utils.mapDifficultyRange(hpDrainRate, HP_100 * 8, HP_100, HP_100));
			break;
		case GameData.HIT_300:
			changeHealth(hpMultiplierNormal * HP_300);
			break;
		case GameData.HIT_100K:
			changeHealth(hpMultiplierComboEnd * HP_100K);
			break;
		case GameData.HIT_300K:
			changeHealth(hpMultiplierComboEnd * HP_300K);
			break;
		case GameData.HIT_300G:
			changeHealth(hpMultiplierComboEnd * HP_300G);
			break;
		case GameData.HIT_MU:
			changeHealth(hpMultiplierNormal * HP_MU);
			break;
		case GameData.HIT_SLIDER10:
			changeHealth(hpMultiplierNormal * HP_SLIDER10);
			break;
		case GameData.HIT_SLIDER30:
			changeHealth(hpMultiplierNormal * HP_SLIDER30);
			break;
		case GameData.HIT_SPINNERSPIN:
			changeHealth(hpMultiplierNormal * HP_SPINNERSPIN);
			break;
		case GameData.HIT_SPINNERBONUS:
			changeHealth(hpMultiplierNormal * HP_SPINNERBONUS);
			break;
		default:
			break;
		}
	}

	/** Resets health. */
	public void reset() {
		health = healthDisplay = healthUncapped = HP_MAX;
		hpDrainRate = 5f;
		hpMultiplierNormal = hpMultiplierComboEnd = 1f;
	}
}
