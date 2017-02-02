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

package itdelatrisu.opsu.ui;

import org.newdawn.slick.Color;

/**
 * Colors used for drawing.
 */
public class Colors {
	public static final Color
		BLACK_ALPHA     = new Color(0, 0, 0, 0.5f),
		WHITE_ALPHA     = new Color(255, 255, 255, 0.5f),
		BLUE_DIVIDER    = new Color(49, 94, 237),
		BLUE_BACKGROUND = new Color(74, 130, 255),
		BLUE_BUTTON     = new Color(40, 129, 237),
		ORANGE_BUTTON   = new Color(200, 90, 3),
		PINK_BUTTON     = new Color(223, 71, 147),
		YELLOW_ALPHA    = new Color(255, 255, 0, 0.4f),
		WHITE_FADE      = new Color(255, 255, 255, 1f),
		RED_HOVER       = new Color(255, 112, 112),
		GREEN           = new Color(137, 201, 79),
		LIGHT_ORANGE    = new Color(255, 192, 128),
		LIGHT_GREEN     = new Color(128, 255, 128),
		LIGHT_BLUE      = new Color(128, 128, 255),
		GREEN_SEARCH    = new Color(173, 255, 47),
		DARK_GRAY       = new Color(0.3f, 0.3f, 0.3f, 1f),
		RED_HIGHLIGHT   = new Color(246, 154, 161),
		BLUE_HIGHLIGHT  = new Color(173, 216, 230),
		BLUE_SCOREBOARD = new Color(133, 208, 212),
		BLACK_BG_NORMAL = new Color(0, 0, 0, 0.25f),
		BLACK_BG_HOVER  = new Color(0, 0, 0, 0.5f),
		BLACK_BG_FOCUS  = new Color(0, 0, 0, 0.75f),
		GHOST_LOGO      = new Color(1.0f, 1.0f, 1.0f, 0.25f),
		PINK_OPTION     = new Color(235, 117, 139),
		BLUE_OPTION     = new Color(88, 217, 253),
		PURPLE          = new Color(138, 43, 226),
		YELLOW_FILL     = new Color(255, 219, 124);

	// This class should not be instantiated.
	private Colors() {}
}
