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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.OpsuConstants;
import itdelatrisu.opsu.OptionGroup;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Options.GameOption;
import itdelatrisu.opsu.Options.GameOption.OptionType;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.gui.AbstractComponent;
import org.newdawn.slick.gui.GUIContext;

/**
 * Options overlay.
 *
 * @author yugecin (https://github.com/yugecin) (base, heavily modified)
 */
public class OptionsOverlay extends AbstractComponent {
	/** Listener for events. */
	public interface OptionsOverlayListener {
		/** Notification that the overlay was closed. */
		void close();
	}

	/** Whether this component is active. */
	private boolean active;

	/** The option groups to show. */
	private final OptionGroup[] groups;

	/** The event listener. */
	private final OptionsOverlayListener listener;

	/** Control images. */
	private final Image sliderBallImg, checkOnImg, checkOffImg;

	/** Control image size. */
	private final int iconSize;

	/** Chevron images. */
	private final Image chevronDownImg, chevronRightImg;

	/** Buttons. */
	private final MenuButton backButton, restartButton;

	/** The top-left coordinates. */
	private float x, y;

	/** The width and height of the overlay. */
	private final int width, height;

	/** The current hovered option. */
	private GameOption hoverOption;

	/** The current selected option (between a mouse press and release). */
	private GameOption selectedOption;

	/** The relative offsets of the start of the options section. */
	private final int optionStartX, optionStartY;

	/** The dimensions of an option. */
	private final int optionWidth, optionHeight;

	/** The padding for an option group title. */
	private final int optionGroupPadding;

	/** Whether or not a slider is currently being adjusted. */
	private boolean isAdjustingSlider;

	/** The current absolute x-coordinate of the selected slider. */
	private int sliderOptionStartX;

	/** The current width of the selected slider. */
	private int sliderOptionWidth;

	/** Whether or not a list option is open. */
	private boolean isListOptionOpen;

	/** The height of a list item. */
	private final int listItemHeight;

	/** The current absolute list coordinates. */
	private int listStartX, listStartY;

	/** The current list dimensions. */
	private int listWidth, listHeight;

	/** The hover index in the current list. */
	private int listHoverIndex = -1;

	/** Kinetic scrolling. */
	private final KineticScrolling scrolling;

	/** The maximum scroll offset. */
	private final int maxScrollOffset;

	/**
	 * The y coordinate of a mouse press, recorded in {@link #mousePressed(int, int, int)}.
	 * If this is -1 directly after a mouse press, then it was not within the overlay.
	 */
	private int mousePressY = -1;

	/** Last mouse position recorded in {@link #update(int)}. */
	private int prevMouseX = -1, prevMouseY = -1;

	/** The delay before the next slider movement sound effect, in ms. */
	private int sliderSoundDelay;

	/** The interval between slider movement sound effects, in ms. */
	private static final int SLIDER_SOUND_INTERVAL = 90;

	/** Key entry states. */
	private boolean keyEntryLeft = false, keyEntryRight = false;

	/** Should all unprocessed events be consumed, and the overlay closed? */
	private boolean consumeAndClose = false;

	/** Whether to show the restart button. */
	private boolean showRestartButton = false;

	/** Colors. */
	private static final Color
		BLACK_ALPHA_75 = new Color(0, 0, 0, 0.75f),
		BLACK_ALPHA_85 = new Color(0, 0, 0, 0.85f);

	// game-related variables
	private GameContainer container;
	private Input input;
	private int containerWidth, containerHeight;

	/**
	 * Creates the options overlay.
	 * @param container the game container
	 * @param groups the option groups
	 * @param listener the event listener
	 */
	public OptionsOverlay(GameContainer container, OptionGroup[] groups, OptionsOverlayListener listener) {
		super(container);
		this.container = container;
		this.groups = groups;
		this.listener = listener;

		this.input = container.getInput();
		this.containerWidth = container.getWidth();
		this.containerHeight = container.getHeight();

		// control images
		this.iconSize = (int) (18 * GameImage.getUIscale());
		this.sliderBallImg = GameImage.CONTROL_SLIDER_BALL.getImage().getScaledCopy(iconSize, iconSize);
		this.checkOnImg = GameImage.CONTROL_CHECK_ON.getImage().getScaledCopy(iconSize, iconSize);
		this.checkOffImg = GameImage.CONTROL_CHECK_OFF.getImage().getScaledCopy(iconSize, iconSize);
		int chevronSize = iconSize - 4;
		this.chevronDownImg = GameImage.CHEVRON_DOWN.getImage().getScaledCopy(chevronSize, chevronSize);
		this.chevronRightImg = GameImage.CHEVRON_RIGHT.getImage().getScaledCopy(chevronSize, chevronSize);

		// overlay positions
		this.x = 0;
		this.y = 0;
		this.width = (int) (containerWidth * 0.42f);
		this.height = containerHeight;

		// option positions
		int paddingX = 24;
		this.optionStartX = paddingX;
		this.optionStartY = (int) (containerHeight * 0.036f) + Fonts.XLARGE.getLineHeight() + Fonts.DEFAULT.getLineHeight() + 4;
		this.optionWidth = width - paddingX * 2;
		this.optionHeight = (int) (Fonts.MEDIUM.getLineHeight() * 1.1f);
		this.optionGroupPadding = Fonts.MEDIUM.getLineHeight() * 2 + 4;
		this.listItemHeight = (int) (optionHeight * 4f / 5f);

		// back button
		int backSize = Fonts.XLARGE.getLineHeight() * 2 / 3;
		Image backArrow = GameImage.CHEVRON_LEFT.getImage().getScaledCopy(backSize, backSize);
		this.backButton = new MenuButton(backArrow, 0, 0);
		backButton.setHoverExpand(1.5f);

		// restart button
		Image restartImg = GameImage.UPDATE.getImage().getScaledCopy(backSize, backSize);
		this.restartButton = new MenuButton(restartImg, 0, 0);
		restartButton.setHoverAnimationDuration(2000);
		restartButton.setHoverRotate(360);

		// calculate scroll offsets
		int maxScrollOffset = optionGroupPadding * groups.length;
		for (OptionGroup group : groups)
			maxScrollOffset += group.getOptions().length * optionHeight;
		maxScrollOffset -= (int) (height * 0.97f - optionStartY);
		this.maxScrollOffset = maxScrollOffset;
		this.scrolling = new KineticScrolling();
		scrolling.setMinMax(0f, maxScrollOffset);
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

	@Override
	public int getHeight() { return height; }

	/**
	 * Returns true if the coordinates are within the overlay bounds.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public boolean contains(float cx, float cy) {
		return ((cx > x && cx < x + width) && (cy > y && cy < y + height));
	}

	/** Activates the component. */
	public void activate() { this.active = true; }

	/** Deactivates the component. */
	public void deactivate() { this.active = false; }

	/**
	 * Whether to consume all unprocessed events, and close the overlay.
	 * @param flag {@code true} to consume all events (default is {@code false})
	 */
	public void setConsumeAndClose(boolean flag) { this.consumeAndClose = flag; }

	@Override
	public void render(GUIContext container, Graphics g) throws SlickException {
		// background
		g.setColor(BLACK_ALPHA_75);
		g.fillRect(x, y, width, height);

		// title
		String title = "Options";
		String subtitle = String.format("Change the way %s behaves", OpsuConstants.PROJECT_NAME);
		float titleX = x + width / 2 - Fonts.XLARGE.getWidth(title) / 2;
		float titleY = y + height * 0.03f;
		float subtitleX = x + width / 2 - Fonts.DEFAULT.getWidth(subtitle) / 2;
		float subtitleY = titleY + Fonts.XLARGE.getLineHeight() + 4;
		Fonts.XLARGE.drawString(titleX, titleY, title, Color.white);
		Fonts.DEFAULT.drawString(subtitleX, subtitleY, subtitle, Colors.PINK_OPTION);

		// back arrow
		backButton.setX(x + backButton.getImage().getWidth() * 2);
		backButton.setY(titleY + Fonts.XLARGE.getLineHeight() * 0.6f);
		backButton.draw();

		// restart button
		if (showRestartButton) {
			restartButton.setX(x + width - restartButton.getImage().getWidth() * 2);
			restartButton.setY(titleY + Fonts.XLARGE.getLineHeight() * 0.6f);
			restartButton.draw();
		}

		// options
		g.setClip((int) x, (int) y + optionStartY, width, containerHeight - optionStartY);
		renderOptions(g);
		if (isListOptionOpen) {
			renderOpenList(g);
		}

		// scrollbar
		int scrollbarWidth = 10, scrollbarHeight = 45;
		float scrollbarX = x + width - scrollbarWidth;
		float scrollbarY = y + optionStartY + (scrolling.getPosition() / maxScrollOffset) * (containerHeight - optionStartY - scrollbarHeight);
		g.setColor(Color.white);
		g.fillRect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight);
		g.clearClip();

		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			g.setColor(BLACK_ALPHA_75);
			g.fillRect(0, 0, containerWidth, containerHeight);
			g.setColor(Color.white);
			String prompt = keyEntryLeft ?
				"Press the new left-click key." : "Press the new right-click key.";
			String subtext = "Click anywhere or hit ESC to cancel.";
			float promptY = (containerHeight - Fonts.XLARGE.getLineHeight() - Fonts.DEFAULT.getLineHeight()) / 2 - 2;
			float subtextY = promptY + Fonts.XLARGE.getLineHeight() + 2;
			Fonts.XLARGE.drawString((containerWidth - Fonts.XLARGE.getWidth(prompt)) / 2, promptY, prompt);
			Fonts.DEFAULT.drawString((containerWidth - Fonts.DEFAULT.getWidth(subtext)) / 2, subtextY, subtext);
		}
	}

	/**
	 * Renders all options.
	 * @param g the graphics context
	 */
	private void renderOptions(Graphics g) {
		listStartX = listStartY = listWidth = listHeight = 0; // render out of the screen
		int cy = (int) (y + optionStartY - scrolling.getPosition());
		boolean render = true;
		for (int groupIdx = 0; groupIdx < groups.length; groupIdx++) {
			OptionGroup group = groups[groupIdx];
			if (cy > 0 && render) {
				int cx = (int) x + optionStartX + (optionWidth - Fonts.LARGE.getWidth(group.getName())) / 2;
				Fonts.LARGE.drawString(cx, cy + Fonts.LARGE.getLineHeight() * 0.6f, group.getName(), Colors.BLUE_OPTION);
			}
			cy += optionGroupPadding;
			for (int optionIdx = 0; optionIdx < group.getOptions().length; optionIdx++) {
				GameOption option = group.getOption(optionIdx);
				if ((cy > 0 && render) || (isListOptionOpen && hoverOption == option))
					renderOption(g, option, cy, option == hoverOption);
				cy += optionHeight;
				if (cy > containerHeight) {
					render = false;
					groupIdx = groups.length;
				}
			}
		}
	}

	/**
	 * Renders the given option.
	 * @param g the graphics context
	 * @param option the game option
	 * @param cy the y coordinate
	 * @param focus whether this option is focused
	 */
	private void renderOption(Graphics g, GameOption option, int cy, boolean focus) {
		Color color = focus ? Colors.GREEN : Colors.WHITE_FADE;
		OptionType type = option.getType();
		Object[] items = option.getItemList();
		if (items != null)
			renderListOption(g, option, cy, color, items);
		else if (type == OptionType.BOOLEAN)
			renderCheckOption(g, option, cy, color);
		else if (type == OptionType.NUMERIC)
			renderSliderOption(g, option, cy, color);
		else
			renderGenericOption(g, option, cy, color);
	}

	/**
	 * Renders a list option.
	 * @param g the graphics context
	 * @param option the game option
	 * @param cy the y coordinate
	 * @param textColor the text color
	 * @param listItems the items in the list
	 */
	private void renderListOption(Graphics g, GameOption option, int cy, Color textColor, Object[] listItems) {
		// draw option name
		int nameWidth = Fonts.MEDIUM.getWidth(option.getName());
		Fonts.MEDIUM.drawString(x + optionStartX, cy, option.getName(), textColor);

		// draw list box and value
		int padding = (int) (optionHeight / 10f);
		nameWidth += 20;
		int itemStart = (int) x + optionStartX + nameWidth;
		int itemWidth = optionWidth - nameWidth;
		Color backColor = BLACK_ALPHA_85;
		if (hoverOption == option && listHoverIndex == -1)
			backColor = Colors.PINK_OPTION;
		g.setColor(backColor);
		g.fillRect(itemStart, cy + padding, itemWidth, listItemHeight);
		g.setColor(Color.white);
		g.setLineWidth(1f);
		g.drawRect(itemStart, cy + padding, itemWidth, listItemHeight);
		chevronDownImg.draw(itemStart + itemWidth - chevronDownImg.getWidth() - 4, cy + padding + 2);
		Fonts.MEDIUM.drawString(itemStart + 20, cy, option.getValueString(), Color.white);
		if (isListOptionOpen && hoverOption == option) {
			listStartX = (int) x + optionStartX + nameWidth;
			listStartY = cy + padding + listItemHeight;
			listWidth = itemWidth;
			listHeight = listItems.length * listItemHeight;
		}
	}

	/**
	 * Renders a boolean option.
	 * @param g the graphics context
	 * @param option the game option
	 * @param cy the y coordinate
	 * @param textColor the text color
	 */
	private void renderCheckOption(Graphics g, GameOption option, int cy, Color textColor) {
		// draw checkbox
		Image img = option.getBooleanValue() ? checkOnImg : checkOffImg;
		img.draw(x + optionStartX, cy + optionHeight / 2 - iconSize / 2, Colors.PINK_OPTION);

		// draw option name
		Fonts.MEDIUM.drawString(x + optionStartX + iconSize * 1.5f, cy, option.getName(), textColor);
	}

	/**
	 * Renders a slider option.
	 * @param g the graphics context
	 * @param option the game option
	 * @param cy the y coordinate
	 * @param textColor the text color
	 */
	private void renderSliderOption(Graphics g, GameOption option, int cy, Color textColor) {
		// draw option name and value
		String value = option.getValueString();
		int nameWidth = Fonts.MEDIUM.getWidth(option.getName());
		int valueWidth = Fonts.MEDIUM.getWidth(value);
		Fonts.MEDIUM.drawString(x + optionStartX, cy, option.getName(), textColor);
		Fonts.MEDIUM.drawString(x + optionStartX + optionWidth - valueWidth, cy, value, Colors.BLUE_OPTION);

		// draw slider
		int sliderWidth = optionWidth - nameWidth - valueWidth - 50;
		if (hoverOption == option) {
			if (!isAdjustingSlider) {
				sliderOptionWidth = sliderWidth;
				sliderOptionStartX = (int) x + optionStartX + nameWidth + 25;
			} else {
				sliderWidth = sliderOptionWidth;
			}
		}
		g.setColor(Colors.PINK_OPTION);
		g.setLineWidth(3f);
		float sliderStartX = x + optionStartX + nameWidth + 25;
		float sliderY = cy + optionHeight / 2;
		g.drawLine(sliderStartX, sliderY, sliderStartX + sliderWidth, sliderY);
		float sliderValue = (float) (sliderWidth + 10) * (option.getIntegerValue() - option.getMinValue()) / (option.getMaxValue() - option.getMinValue());
		sliderBallImg.draw((int) (sliderStartX + sliderValue - iconSize / 2f), (int) (sliderY - iconSize / 2f), Colors.PINK_OPTION);
	}

	/**
	 * Renders a generic option.
	 * @param g the graphics context
	 * @param option the game option
	 * @param cy the y coordinate
	 * @param textColor the text color
	 */
	private void renderGenericOption(Graphics g, GameOption option, int cy, Color textColor) {
		// draw option name and value
		String value = option.getValueString();
		int valueWidth = Fonts.MEDIUM.getWidth(value);
		Fonts.MEDIUM.drawString(x + optionStartX, cy, option.getName(), textColor);
		Fonts.MEDIUM.drawString(x + optionStartX + optionWidth - valueWidth, cy, value, Colors.BLUE_OPTION);
	}

	/**
	 * Renders an open list.
	 * @param g the graphics context
	 */
	private void renderOpenList(Graphics g) {
		// draw list rectangles
		g.setColor(BLACK_ALPHA_85);
		g.fillRect(listStartX, listStartY, listWidth, listHeight);
		if (listHoverIndex != -1) {
			int cy = listStartY + listHoverIndex * listItemHeight;
			g.setColor(Colors.PINK_OPTION);
			g.fillRect(listStartX, cy, listWidth, listItemHeight);
			chevronRightImg.draw(listStartX + 3, cy + (listItemHeight - chevronRightImg.getHeight()) / 2);
		}
		g.setLineWidth(1f);
		g.setColor(Color.white);
		g.drawRect(listStartX, listStartY, listWidth, listHeight);

		// draw list items
		Object[] listItems = hoverOption.getItemList();
		int cy = listStartY;
		for (Object item : listItems) {
			Fonts.MEDIUM.drawString(listStartX + iconSize, cy - Fonts.MEDIUM.getLineHeight() * 0.05f, item.toString());
			cy += listItemHeight;
		}
	}

	/**
	 * Updates the overlay.
	 * @param delta the delta interval since the last call
	 */
	public void update(int delta) {
		if (!active)
			return;

		// check if mouse moved
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		boolean mouseMoved;
		if (mouseX == prevMouseX && mouseY == prevMouseY)
			mouseMoved = false;
		else {
			mouseMoved = true;
			updateHoverOption(mouseX, mouseY);
			prevMouseX = mouseX;
			prevMouseY = mouseY;
		}

		// delta updates
		if (hoverOption != null && getOptionAtPosition(mouseX, mouseY) == hoverOption && !keyEntryLeft && !keyEntryRight)
			UI.updateTooltip(delta, hoverOption.getDescription(), true);
		else if (showRestartButton && restartButton.contains(mouseX, mouseY) && !keyEntryLeft && !keyEntryRight)
			UI.updateTooltip(delta, "Click to restart the game.", false);
		backButton.hoverUpdate(delta, backButton.contains(mouseX, mouseY) && !keyEntryLeft && !keyEntryRight);
		if (showRestartButton)
			restartButton.autoHoverUpdate(delta, false);
		sliderSoundDelay = Math.max(sliderSoundDelay - delta, 0);
		scrolling.update(delta);

		if (!mouseMoved)
			return;

		// update slider option
		if (isAdjustingSlider)
			adjustSlider(mouseX, mouseY);

		// update list option
		else if (isListOptionOpen)
			listHoverIndex = getListIndex(mouseX, mouseY);
	}

	/**
	 * Updates a slider option based on the current mouse coordinates.
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 */
	private void updateSliderOption(int mouseX, int mouseY) {
		int min = hoverOption.getMinValue();
		int max = hoverOption.getMaxValue();
		int value = min + Math.round((float) (max - min) * (mouseX - sliderOptionStartX) / sliderOptionWidth);
		hoverOption.setValue(Utils.clamp(value, min, max));
	}

	/**
	 * Updates the "hovered" option based on the current mouse coordinates.
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 */
	private void updateHoverOption(int mouseX, int mouseY) {
		if (isListOptionOpen || keyEntryLeft || keyEntryRight)
			return;
		if (selectedOption != null) {
			hoverOption = selectedOption;
			return;
		}

		hoverOption = getOptionAtPosition(mouseX, mouseY);
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			keyEntryLeft = keyEntryRight = false;
			consumeEvent();
			return;
		}

		if (!active)
			return;

		if (!contains(x, y)) {
			if (consumeAndClose) {
				consumeEvent();
				listener.close();
			}
			return;
		}

		consumeEvent();

		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// list option already open
		if (isListOptionOpen) {
			SoundController.playSound(SoundEffect.MENUCLICK);
			int index = getListIndex(x, y);
			if (y > optionStartY && index != -1) {
				String oldValue = hoverOption.getValueString();
				hoverOption.selectItem(index, container);

				// show restart button?
				if (!oldValue.equals(hoverOption.getValueString()) &&
				    (hoverOption == GameOption.SCREEN_RESOLUTION || hoverOption == GameOption.SKIN)) {
					showRestartButton = true;
					UI.getNotificationManager().sendBarNotification("Restart to apply changes.");
				}
			}
			isListOptionOpen = false;
			listHoverIndex = -1;
			updateHoverOption(x, y);
			return;
		}

		// back button: close overlay
		if (backButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUCLICK);
			listener.close();
			return;
		}

		// restart button: restart game
		if (showRestartButton && restartButton.contains(x, y)) {
			container.setForceExit(false);
			container.exit();
			return;
		}

		scrolling.pressed();

		// clicked an option?
		hoverOption = selectedOption = getOptionAtPosition(x, y);
		mousePressY = y;
		if (hoverOption != null) {
			if (hoverOption.getType() == OptionType.NUMERIC) {
				isAdjustingSlider = sliderOptionStartX <= x && x < sliderOptionStartX + sliderOptionWidth;
				if (isAdjustingSlider) {
					SoundController.playSound(SoundEffect.MENUCLICK);
					updateSliderOption(x, y);
				}
			}
		}
	}

	@Override
	public void mouseReleased(int button, int x, int y) {
		if (!active)
			return;

		// check if associated mouse press was in the overlay
		if (mousePressY == -1)
			return;

		consumeEvent();

		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// finish adjusting slider
		if (isAdjustingSlider) {
			isAdjustingSlider = false;
			adjustSlider(x, y);
		}

		// check if clicked, not dragged
		boolean mouseDragged = (Math.abs(y - mousePressY) >= 5);

		mousePressY = -1;
		selectedOption = null;
		sliderOptionWidth = 0;
		scrolling.released();

		if (mouseDragged)
			return;

		// update based on option type
		if (hoverOption != null) {
			if (hoverOption.getType() == OptionType.BOOLEAN) {
				SoundController.playSound(SoundEffect.MENUCLICK);
				boolean oldValue = hoverOption.getBooleanValue();
				hoverOption.toggle(container);

				// show restart button?
				if (oldValue != hoverOption.getBooleanValue() && hoverOption == GameOption.FULLSCREEN) {
					showRestartButton = true;
					UI.getNotificationManager().sendBarNotification("Restart to apply changes.");
				}
			} else if (hoverOption.getItemList() != null) {
				SoundController.playSound(SoundEffect.MENUCLICK);
				isListOptionOpen = true;
			} else if (hoverOption == GameOption.KEY_LEFT) {
				SoundController.playSound(SoundEffect.MENUCLICK);
				keyEntryLeft = true;
			} else if (hoverOption == GameOption.KEY_RIGHT) {
				SoundController.playSound(SoundEffect.MENUCLICK);
				keyEntryRight = true;
			}
		}
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (!active || !contains(oldx, oldy))
			return;

		if (!isAdjustingSlider) {
			int diff = newy - oldy;
			if (diff != 0)
				scrolling.dragged(-diff);
		}
		consumeEvent();
	}

	@Override
	public void mouseWheelMoved(int delta) {
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		if (!active)
			return;

		if (!contains(mouseX, mouseY)) {
			if (consumeAndClose) {
				consumeEvent();
				listener.close();
			}
			return;
		}

		if (!isAdjustingSlider)
			scrolling.scrollOffset(-delta);
		updateHoverOption(prevMouseX, prevMouseY);
		consumeEvent();
	}

	@Override
	public void keyPressed(int key, char c) {
		if (!active)
			return;

		// key entry state
		if (keyEntryRight) {
			Options.setGameKeyRight(key);
			keyEntryRight = false;
			consumeEvent();
			return;
		} else if (keyEntryLeft) {
			Options.setGameKeyLeft(key);
			keyEntryLeft = false;
			consumeEvent();
			return;
		}

		// esc: close open list option, otherwise close overlay
		if (key == Input.KEY_ESCAPE) {
			consumeEvent();
			if (isListOptionOpen) {
				isListOptionOpen = false;
				listHoverIndex = -1;
			} else {
				listener.close();
			}
			return;
		}

		if (consumeAndClose &&
		    key != Input.KEY_RCONTROL && key != Input.KEY_LCONTROL &&
		    key != Input.KEY_RSHIFT && key != Input.KEY_LSHIFT &&
		    key != Input.KEY_RALT && key != Input.KEY_LALT) {
			consumeEvent();
			listener.close();
		}
	}

	/**
	 * Handles a slider adjustment to the given mouse coordinates.
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 */
	private void adjustSlider(int mouseX, int mouseY) {
		int oldSliderValue = hoverOption.getIntegerValue();

		// set new value
		updateSliderOption(mouseX, mouseY);

		// play sound effect
		if (hoverOption.getIntegerValue() != oldSliderValue && sliderSoundDelay == 0) {
			sliderSoundDelay = SLIDER_SOUND_INTERVAL;
			SoundController.playSound(SoundEffect.MENUCLICK);
		}
	}

	/**
	 * Returns the option at the given position, using the current scroll offset.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @return the option, or {@code null} if none
	 */
	private GameOption getOptionAtPosition(int cx, int cy) {
		if (cy < y + optionStartY || cx < x + optionStartX || cx > x + optionStartX + optionWidth)
			return null;  // out of bounds

		int mouseVirtualY = (int) (scrolling.getPosition() + cy - y - optionStartY);
		for (OptionGroup group : groups) {
			mouseVirtualY -= optionGroupPadding;
			for (int i = 0; i < group.getOptions().length; i++) {
				if (mouseVirtualY <= optionHeight)
					return (mouseVirtualY >= 0) ? group.getOption(i) : null;
				mouseVirtualY -= optionHeight;
			}
		}
		return null;
	}

	/**
	 * Returns the list index at the given coordinates.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @return the index in the list, or -1 if none
	 */
	private int getListIndex(int cx, int cy) {
		if ((listStartX <= cx && cx < listStartX + listWidth) &&
		    (listStartY <= cy && cy < listStartY + listHeight))
			return (cy - listStartY) / listItemHeight;
		else
			return -1;
	}

	@Override
	public void setFocus(boolean focus) { /* does not currently use the "focus" concept */ }

	/**
	 * Resets all state.
	 */
	public void reset() {
		hoverOption = selectedOption = null;
		isAdjustingSlider = false;
		isListOptionOpen = false;
		listStartX = listStartY = 0;
		listWidth = listHeight = 0;
		listHoverIndex = -1;
		sliderOptionStartX = sliderOptionWidth = 0;
		scrolling.setPosition(0f);
		keyEntryLeft = keyEntryRight = false;
		mousePressY = -1;
		prevMouseX = prevMouseY = -1;
		sliderSoundDelay = 0;
		backButton.resetHover();
		restartButton.resetHover();
	}
}
