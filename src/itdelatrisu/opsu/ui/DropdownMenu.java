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

import fluddokt.opsu.fake.*;
import fluddokt.opsu.fake.gui.AbstractComponent;
import fluddokt.opsu.fake.gui.GUIContext;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

/*
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.gui.AbstractComponent;
import org.newdawn.slick.gui.GUIContext;
*/

/**
 * Simple dropdown menu.
 * <p>
 * Basic usage:
 * <ul>
 * <li>Override {@link #menuClicked(int)} to perform actions when the menu is clicked
 *     (e.g. play a sound effect, block input under certain conditions).
 * <li>Override {@link #itemSelected(int, Object)} to perform actions when a new item is selected.
 * <li>Call {@link #activate()}/{@link #deactivate()} whenever the component is needed
 *     (e.g. in a state's {@code enter} and {@code leave} events.
 * </ul>
 *
 * @param <E> the type of the elements in the menu
 */
public class DropdownMenu<E> extends AbstractComponent {
	/** Padding ratios for drawing. */
	private static final float PADDING_Y = 0.1f, CHEVRON_X = 0.03f;

	/** Whether this component is active. */
	private boolean active;

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

	/** The last update time, in milliseconds. */
	private long lastUpdateTime;

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

	/** Should the next click be blocked? */
	private boolean blockClick = false;

	/**
	 * Creates a new dropdown menu.
	 * @param container the container rendering this menu
	 * @param items the list of items (with names given as their {@code toString()} methods)
	 * @param x the top-left x coordinate
	 * @param y the top-left y coordinate
	 */
	public DropdownMenu(GUIContext container, E[] items, float x, float y) {
		this(container, items, x, y, 0);
	}

	/**
	 * Creates a new dropdown menu with the given fonts.
	 * @param container the container rendering this menu
	 * @param items the list of items (with names given as their {@code toString()} methods)
	 * @param x the top-left x coordinate
	 * @param y the top-left y coordinate
	 * @param normal the normal font
	 * @param selected the font for the selected item
	 */
	public DropdownMenu(GUIContext container, E[] items, float x, float y, UnicodeFont normal, UnicodeFont selected) {
		this(container, items, x, y, 0, normal, selected);
	}

	/**
	 * Creates a new dropdown menu with the given width.
	 * @param container the container rendering this menu
	 * @param items the list of items (with names given as their {@code toString()} methods)
	 * @param x the top-left x coordinate
	 * @param y the top-left y coordinate
	 * @param width the menu width
	 */
	public DropdownMenu(GUIContext container, E[] items, float x, float y, int width) {
		super(container);
		init(items, x, y, width);
	}

	/**
	 * Creates a new dropdown menu with the given width and fonts.
	 * @param container the container rendering this menu
	 * @param items the list of items (with names given as their {@code toString()} methods)
	 * @param x the top-left x coordinate
	 * @param y the top-left y coordinate
	 * @param width the menu width
	 * @param normal the normal font
	 * @param selected the font for the selected item
	 */
	public DropdownMenu(GUIContext container, E[] items, float x, float y, int width, UnicodeFont normal, UnicodeFont selected) {
		super(container);
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

	@Override
	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public int getX() { return (int) x; }

	@Override
	public int getY() { return (int) y; }

	@Override
	public int getWidth() { return width; }

	/** Sets the width of the dropdown menu. */
	public void setWidth(int width) { this.width = width; }

	@Override
	public int getHeight() { return (expanded) ? height : baseHeight; }

	/** Activates the component. */
	public void activate() { this.active = true; }

	/** Deactivates the component. */
	public void deactivate() { this.active = false; }

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

	@Override
	public void render(GUIContext container, Graphics g) throws SlickException {
		// update animation
		long time = container.getTime();
		if (lastUpdateTime > 0) {
			int delta = (int) (time - lastUpdateTime);
			expandProgress.update((expanded) ? delta : -delta * 2);
		}
		this.lastUpdateTime = time;

		// get parameters
		Input input = container.getInput();
		int idx = getIndexAt(input.getMouseX(), input.getMouseY());
		float t = expandProgress.getValue();
		if (expanded)
			t = AnimationEquation.OUT_CUBIC.calc(t);

		// background and border
		Color oldGColor = g.getColor();
		float oldLineWidth = g.getLineWidth();
		final int cornerRadius = 6;
		g.setLineWidth(1f);
		g.setColor((idx == -1) ? highlightColor : backgroundColor);
		g.fillRoundRect((int) x, (int) y, width, baseHeight, cornerRadius);
		g.setColor(borderColor);
		g.drawRoundRect((int) x, (int) y, width, baseHeight, cornerRadius);
		if (expanded || t >= 0.0001) {
			float oldBackgroundAlpha = backgroundColor.a;
			backgroundColor.a *= t;
			g.setColor(backgroundColor);
			g.fillRoundRect((int) x, (int) (y + offsetY), width, (height - offsetY) * t, cornerRadius);
			backgroundColor.a = oldBackgroundAlpha;
		}
		if (idx >= 0 && t >= 0.9999) {
			g.setColor(highlightColor);
			float yPos = y + offsetY + (offsetY * idx);
			int yOff = 0, hOff = 0;
			if (idx == 0 || idx == items.length - 1) {
				g.fillRoundRect((int) x, (int) yPos, width, offsetY, cornerRadius);
				if (idx == 0)
					yOff = cornerRadius;
				hOff = cornerRadius;
			}
			g.fillRect((int) x, (int) (yPos + yOff), width, offsetY - hOff);
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
	 * Resets the menu state.
	 */
	public void reset() {
		this.expanded = false;
		this.lastUpdateTime = 0;
		expandProgress.setTime(0);
		blockClick = false;
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		if (!active)
			return;

		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		int idx = getIndexAt(x, y);
		if (idx == -2) {
			this.expanded = false;
			return;
		}
		if (!menuClicked(idx))
			return;
		this.expanded = (idx == -1) ? !expanded : expanded;
		blockClick = true;
		consumeEvent();
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		if(expanded){
			consumeEvent();
		}
	}

	@Override
	public void mouseClicked(int button, int x, int y, int clickCount) {
		if (!active)
			return;

		if (blockClick) {
			blockClick = false;
			consumeEvent();
		}
	}

	/**
	 * Notification that a new item was selected (via override).
	 * @param index the index of the item selected
	 * @param item the item selected
	 */
	public void itemSelected(int index, E item) {}

	/**
	 * Notification that the menu was clicked (via override).
	 * @param index the index of the item clicked, or -1 for the base item
	 * @return true to process the click, or false to block/intercept it
	 */
	public boolean menuClicked(int index) { return true; }

	@Override
	public void setFocus(boolean focus) { /* does not currently use the "focus" concept */ }

	@Override
	public void mouseReleased(int button, int x, int y) {
		if (!active)
			return;
		int idx = getIndexAt(x, y);
		if (idx == -2) {
			this.expanded = false;
			return;
		}
		if (!menuClicked(idx))
			return;
		this.expanded = (idx == -1) ? expanded : false;
		if (idx >= 0 && itemIndex != idx) {
			this.itemIndex = idx;
			itemSelected(idx, items[idx]);
		}
		blockClick = true;
		consumeEvent();
	}

	/**
	 * Selects the item at the given index.
	 * @param index the list item index
	 * @throws IllegalArgumentException if {@code index} is negative or greater than or equal to size
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
