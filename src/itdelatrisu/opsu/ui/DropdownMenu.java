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

package itdelatrisu.opsu.ui;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.UnicodeFont;

/**
 * Simple dropdown menu.
 */
public class DropdownMenu<E> {
	/** Padding ratios for drawing. */
	private static final float PADDING_Y = 0.1f, CHEVRON_X = 0.03f;

	/** The menu items. */
	private E[] items;

	/** The menu item names. */
	private String[] itemNames;

	/** The index of the selected item. */
	private int itemIndex = 0;

	/** Whether the menu is expanded. */
	private boolean expanded = false;

	/** The expanding animation progress. */
	private AnimatedValue expandProgress = new AnimatedValue(300, 0f, 1f, AnimationEquation.LINEAR);

	/** The top-left coordinates. */
	private float x, y;

	/** The width and height of the dropdown menu. */
	private int width, height;

	/** The height of the base item. */
	private int baseHeight;

	/** The vertical offset between items. */
	private float offsetY;

	/** The colors to use. */
	private Color
		textColor = Color.white, backgroundColor = Color.black,
		highlightColor = Colors.BLUE_DIVIDER, borderColor = Colors.BLUE_DIVIDER,
		chevronDownColor = textColor, chevronRightColor = backgroundColor;

	/** The fonts to use. */
	private UnicodeFont fontNormal = Fonts.MEDIUM, fontSelected = Fonts.MEDIUMBOLD;

	/** The chevron images. */
	private Image chevronDown, chevronRight;

	/**
	 * Creates a new dropdown menu.
	 * @param items the list of items (with names given as their {@code toString()} methods)
	 * @param x the top-left x coordinate
	 * @param y the top-left y coordinate
	 */
	public DropdownMenu(E[] items, float x, float y) {
		this(items, x, y, 0);
	}

	/**
	 * Creates a new dropdown menu with the given fonts.
	 * @param items the list of items (with names given as their {@code toString()} methods)
	 * @param x the top-left x coordinate
	 * @param y the top-left y coordinate
	 * @param normal the normal font
	 * @param selected the font for the selected item
	 */
	public DropdownMenu(E[] items, float x, float y, UnicodeFont normal, UnicodeFont selected) {
		this(items, x, y, 0, normal, selected);
	}

	/**
	 * Creates a new dropdown menu with the given width.
	 * @param items the list of items (with names given as their {@code toString()} methods)
	 * @param x the top-left x coordinate
	 * @param y the top-left y coordinate
	 * @param width the menu width
	 */
	public DropdownMenu(E[] items, float x, float y, int width) {
		init(items, x, y, width);
	}

	/**
	 * Creates a new dropdown menu with the given width and fonts.
	 * @param items the list of items (with names given as their {@code toString()} methods)
	 * @param x the top-left x coordinate
	 * @param y the top-left y coordinate
	 * @param width the menu width
	 * @param normal the normal font
	 * @param selected the font for the selected item
	 */
	public DropdownMenu(E[] items, float x, float y, int width, UnicodeFont normal, UnicodeFont selected) {
		this.fontNormal = normal;
		this.fontSelected = selected;
		init(items, x, y, width);
	}

	/**
	 * Returns the maximum item width from the list.
	 */
	private int getMaxItemWidth() {
		int maxWidth = 0;
		for (int i = 0; i < itemNames.length; i++) {
			int w = fontSelected.getWidth(itemNames[i]);
			if (w > maxWidth)
				maxWidth = w;
		}
		return maxWidth;
	}

	/**
	 * Initializes the component.
	 */
	private void init(E[] items, float x, float y, int width) {
		this.items = items;
		this.itemNames = new String[items.length];
		for (int i = 0; i < itemNames.length; i++)
			itemNames[i] = items[i].toString();
		this.x = x;
		this.y = y;
		this.baseHeight = fontNormal.getLineHeight();
		this.offsetY = baseHeight + baseHeight * PADDING_Y;
		this.height = (int) (offsetY * (items.length + 1));
		int chevronDownSize = baseHeight * 4 / 5;
		this.chevronDown = GameImage.CHEVRON_DOWN.getImage().getScaledCopy(chevronDownSize, chevronDownSize);
		int chevronRightSize = baseHeight * 2 / 3;
		this.chevronRight = GameImage.CHEVRON_RIGHT.getImage().getScaledCopy(chevronRightSize, chevronRightSize);
		int maxItemWidth = getMaxItemWidth();
		int minWidth = maxItemWidth + chevronRight.getWidth() * 2;
		this.width = Math.max(width, minWidth);
	}

	/**
	 * Returns the width of the menu.
	 */
	public int getWidth() { return width; }

	/**
	 * Returns the height of the base item.
	 */
	public int getHeight() { return baseHeight; }

	/**
	 * Returns whether the dropdown menu is currently open.
	 * @return true if open, false otherwise
	 */
	public boolean isOpen() { return expanded; }

	/**
	 * Opens or closes the dropdown menu.
	 * @param flag true to open, false to close
	 */
	public void open(boolean flag) { this.expanded = flag; }

	/**
	 * Returns true if the coordinates are within the menu bounds.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public boolean contains(float cx, float cy) {
		return (cx > x && cx < x + width && (
		        (cy > y && cy < y + baseHeight) ||
		        (expanded && cy > y + offsetY && cy < y + height)));
	}

	/**
	 * Returns true if the coordinates are within the base item bounds.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public boolean baseContains(float cx, float cy) {
		return (cx > x && cx < x + width && cy > y && cy < y + baseHeight);
	}

	/**
	 * Draws the dropdown menu.
	 * @param g the graphics context
	 * @param cx the mouse x coordinate
	 * @param cy the mouse y coordinate
	 */
	public void draw(Graphics g, float cx, float cy) {
		int idx = getIndexAt(cx, cy);
		float t = expandProgress.getValue();
		if (expanded)
			t = AnimationEquation.OUT_CUBIC.calc(t);

		// background and border
		Color oldGColor = g.getColor();
		float oldLineWidth = g.getLineWidth();
		final int cornerRadius = 6;
		g.setLineWidth(1f);
		g.setColor((idx == -1) ? highlightColor : backgroundColor);
		g.fillRoundRect(x, y, width, baseHeight, cornerRadius);
		g.setColor(borderColor);
		g.drawRoundRect(x, y, width, baseHeight, cornerRadius);
		if (expanded || t >= 0.0001) {
			float oldBackgroundAlpha = backgroundColor.a;
			backgroundColor.a *= t;
			g.setColor(backgroundColor);
			g.fillRoundRect(x, y + offsetY, width, (height - offsetY) * t, cornerRadius);
			backgroundColor.a = oldBackgroundAlpha;
		}
		if (idx >= 0 && t >= 0.9999) {
			g.setColor(highlightColor);
			float yPos = y + offsetY + (offsetY * idx);
			int yOff = 0, hOff = 0;
			if (idx == 0 || idx == items.length - 1) {
				g.fillRoundRect(x, yPos, width, offsetY, cornerRadius);
				if (idx == 0)
					yOff = cornerRadius;
				hOff = cornerRadius;
			}
			g.fillRect(x, yPos + yOff, width, offsetY - hOff);
		}
		g.setColor(oldGColor);
		g.setLineWidth(oldLineWidth);

		// text
		chevronDown.draw(x + width - chevronDown.getWidth() - width * CHEVRON_X, y + (baseHeight - chevronDown.getHeight()) / 2f, chevronDownColor);
		fontNormal.drawString(x + (width * 0.03f), y + (fontNormal.getPaddingTop() + fontNormal.getPaddingBottom()) / 2f, itemNames[itemIndex], textColor);
		float oldTextAlpha = textColor.a;
		textColor.a *= t;
		if (expanded || t >= 0.0001) {
			for (int i = 0; i < itemNames.length; i++) {
				Font f = (i == itemIndex) ? fontSelected : fontNormal;
				if (i == idx && t >= 0.999)
					chevronRight.draw(x, y + offsetY + (offsetY * i) + (offsetY - chevronRight.getHeight()) / 2f, chevronRightColor);
				f.drawString(x + chevronRight.getWidth(), y + offsetY + (offsetY * i * t), itemNames[i], textColor);
			}
		}
		textColor.a = oldTextAlpha;
	}

	/**
	 * Returns the index of the item at the given location, -1 for the base item,
	 * and -2 if there is no item at the location.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	private int getIndexAt(float cx, float cy) {
		if (!contains(cx, cy))
			return -2;
		if (cy <= y + baseHeight)
			return -1;
		if (!expanded)
			return -2;
		return (int) ((cy - (y + offsetY)) / offsetY);
	}

	/**
	 * Updates the animations by a delta interval.
	 * @param delta the delta interval since the last call
	 */
	public void update(int delta) {
		expandProgress.update((expanded) ? delta : -delta * 2);
	}

	/**
	 * Registers a click at the given location.
	 * If the base item is clicked and the menu is unexpanded, it will be expanded;
	 * in all other cases, the menu will be unexpanded. If an item different from
	 * the current one is selected, that item will be selected.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @return the index of the item at the given location, -1 for the base item,
	 *         and -2 if there is no item at the location
	 */
	public int click(float cx, float cy) {
		int idx = getIndexAt(cx, cy);
		this.expanded = (idx == -1) ? !expanded : false;
		if (idx >= 0)
			this.itemIndex = idx;
		return idx;
	}

	/**
	 * Resets the menu state.
	 */
	public void reset() {
		this.expanded = false;
		expandProgress.setTime(0);
	}

	/**
	 * Selects the item at the given index.
	 * @param index the list item index
	 * @throws IllegalArgumentException if index < -1 or index is greater than or equal to size
	 */
	public void setSelectedIndex(int index) {
		if (index < 0 || index >= items.length)
			throw new IllegalArgumentException();
		this.itemIndex = index;
	}

	/**
	 * Returns the index of the selected item.
	 */
	public int getSelectedIndex() { return itemIndex; }

	/**
	 * Returns the selected item.
	 */
	public E getSelectedItem() { return items[itemIndex]; }

	/**
	 * Returns the item at the given index.
	 * @param index the list item index
	 */
	public E getItemAt(int index) { return items[index]; }

	/**
	 * Returns the number of items in the list.
	 */
	public int getItemCount() { return items.length; }

	/** Sets the text color. */
	public void setTextColor(Color c) { this.textColor = c; }

	/** Sets the background color. */
	public void setBackgroundColor(Color c) { this.backgroundColor = c; }

	/** Sets the highlight color. */
	public void setHighlightColor(Color c) { this.highlightColor = c; }

	/** Sets the border color. */
	public void setBorderColor(Color c) { this.borderColor = c; }

	/** Sets the down chevron color. */
	public void setChevronDownColor(Color c) { this.chevronDownColor = c; }

	/** Sets the right chevron color. */
	public void setChevronRightColor(Color c) { this.chevronRightColor = c; }
}
