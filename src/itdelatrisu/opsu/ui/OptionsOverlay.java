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

import itdelatrisu.opsu.ui.animations.AnimationEquation;
import org.newdawn.slick.*;
import org.newdawn.slick.gui.AbstractComponent;
import org.newdawn.slick.gui.GUIContext;
import org.newdawn.slick.gui.TextField;

import java.util.HashMap;

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

	/** Search image. */
	private Image searchImg;

	/** Target duration, in ms, of the move animation for the indicator. */
	private static final int INDICATORMOVEANIMATIONTIME = 166;

	/**  Selected option indicator virtual position. */
	private int indicatorPos;

	/**  Selected option indicator render position. */
	private int indicatorRenderPos;

	/** Selected option indicator offset to next position. */
	private int indicatorOffsetToNextPos;

	/** Selected option indicator move to next position animation time past. */
	private int indicatorMoveAnimationTime;

	/** Target duration, in ms, of the fadeout animation for the indicator. */
	private static final int INDICATORHIDEANIMATIONTIME = 500;

	/** Selected option indicator hide animation time past. */
	private int indicatorHideAnimationTime;

	/** Buttons. */
	private final MenuButton backButton, restartButton;

	/** The top-left coordinates. */
	private float x, y;

	/** The width and height of the overlay. */
	private final int targetWidth, height;

	/** The real width of the overlay, altered by the hide/show animation */
	private int width;

	/** The current hovered option. */
	private GameOption hoverOption;

	/** The current selected option (between a mouse press and release). */
	private GameOption selectedOption;

	/** The relative offsets of the start of the options section. */
	private final int optionStartX, optionStartY;

	/** The dimensions of an option. */
	private int optionWidth, optionHeight;

	/** Y offset from the option position to the option text position. */
	private int optionTextOffsetY;

	/** The size of the control images (sliderball, checkbox). */
	private int controlImageSize;

	/** The vertical padding for the control images to vertical align them. */
	private int controlImagePadding;

	/** The width of the grey line next to groups. */
	private static final int LINEWIDTH = 3;

	/** Right padding. */
	private int paddingRight;

	/** Left padding to the grey line. */
	private int paddingLeft;

	/** Left padding to the option text. */
	private int paddingTextLeft;

	/** Y position of the options text. */
	private int textOptionsY;

	/** Y position of the change text. */
	private int textChangeY;

	/** Y position of the search block. */
	private int posSearchY;

	/** Y offset from the search block to the search text. */
	private int textSearchYOffset;

	/** The padding for an option group title. */
	private final int optionGroupPadding;

	/** Whether or not a slider is currently being adjusted. */
	private boolean isAdjustingSlider;

	/** The current absolute x-coordinate of the selected slider. */
	private int sliderOptionStartX;

	/** The current width of the selected slider. */
	private int sliderOptionWidth;

	/** HashMap which contains dropdown menus corresponding to options. */
	private final HashMap<GameOption, DropdownMenu<Object>> dropdownMenus;

	/** The vertical padding to use when rendering a dropdown menu. */
	private int dropdownMenuPaddingY;

	/** The dropdown menu that is currently open. */
	private DropdownMenu<Object> openDropdownMenu;

	/** The old value of the open dropdown menu. */
	private String openDropdownMenuOldValue;

	/**
	 * The virtual Y position of the open dropdown menu.
	 * Used to calculate the maximum scrolling offset.
	 */
	private int openDropdownVirtualY;

	/** Kinetic scrolling. */
	private final KineticScrolling scrolling;

	/** The maximum scroll offset. */
	private int maxScrollOffset;

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

	/** Textfield used for searching options. */
	private TextField searchField;

	/** Last search text. */
	private String lastSearchText;

	/** Desired alpha values for specific colors. */
	private static final float
		BG_ALPHA = 0.7f,
		LINEALPHA = 0.8f,
		INDICATOR_ALPHA = 0.8f;

	/** Colors. */
	private static final
		Color COL_BG = new Color(Color.black),
		COL_WHITE = new Color(1f, 1f, 1f),
		COL_PINK = new Color(235, 117, 139),
		COL_CYAN = new Color(88, 218, 254),
		COL_GREY = new Color(55, 55, 57),
		COL_BLUE = new Color(Colors.BLUE_BACKGROUND),
		COL_COMBOBOX_HOVER = new Color(185, 19, 121),
		COL_INDICATOR = new Color(Color.black);

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
		int searchImgSize = (int) (Fonts.LARGE.getLineHeight() * 0.75f);
		searchImg = GameImage.SEARCH.getImage().getScaledCopy(searchImgSize, searchImgSize);

		// overlay positions
		this.x = 0;
		this.y = 0;
		this.targetWidth = (int) (containerWidth * 0.42f);
		this.height = containerHeight;

		// option positions
		this.paddingRight = (int) (containerWidth * 0.009375f); // not so accurate
		this.paddingLeft = (int) (containerWidth * 0.0180f); // not so accurate
		this.paddingTextLeft = paddingLeft + LINEWIDTH + (int) (containerWidth * 0.00625f); // not so accurate
		this.optionStartX = paddingTextLeft;
		this.textOptionsY = Fonts.LARGE.getLineHeight() * 2;
		this.textChangeY = textOptionsY + Fonts.LARGE.getLineHeight();
		this.posSearchY = textChangeY + Fonts.MEDIUM.getLineHeight() * 2;
		this.textSearchYOffset = Fonts.MEDIUM.getLineHeight() / 2;
		this.optionStartY = posSearchY + Fonts.MEDIUM.getLineHeight() + Fonts.LARGE.getLineHeight();
		int paddingX = 24;
		this.optionWidth = targetWidth - paddingX * 2;
		this.optionHeight = (int) (Fonts.MEDIUM.getLineHeight() * 1.3f);
		this.optionGroupPadding = (int) (Fonts.LARGE.getLineHeight() * 1.5f);
		this.optionTextOffsetY = (int) ((optionHeight - Fonts.MEDIUM.getLineHeight()) / 2f);
		this.controlImageSize = (int) (Fonts.MEDIUM.getLineHeight() * 0.7f);
		this.controlImagePadding = (optionHeight - controlImageSize) / 2;

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

		// initialize dropdown menus
		dropdownMenus = new HashMap<>();
		for (OptionGroup group : groups) {
			GameOption[] options = group.getOptions();
			if (options == null) {
				continue;
			}
			for (final GameOption option : options) {
				Object[] items = option.getItemList();
				if (items == null) {
					continue;
				}
				DropdownMenu<Object> menu = new DropdownMenu<Object>(container, items, 0, 0) {
					@Override
					public void itemSelected(int index, Object item) {
						option.selectItem(index, OptionsOverlay.this.container);
						// show restart button?
						if (!openDropdownMenuOldValue.equals(hoverOption.getValueString()) &&
							(option == GameOption.SCREEN_RESOLUTION || option == GameOption.SKIN)) {
							showRestartButton = true;
							UI.getNotificationManager().sendBarNotification("Restart to apply changes.");
						}
						openDropdownMenu = null;
						openDropdownMenuOldValue = null;
					}
				};
				menu.setBackgroundColor(COL_BG);
				menu.setBorderColor(Color.transparent);
				menu.setChevronDownColor(COL_WHITE);
				menu.setChevronRightColor(COL_BG);
				menu.setHighlightColor(COL_COMBOBOX_HOVER);
				menu.setTextColor(COL_WHITE);
				dropdownMenuPaddingY = (optionHeight - menu.getHeight()) / 2;
				dropdownMenus.put(option, menu);
			}
		}

		searchField = new TextField(container, null, 0, 0, 0, 0);
		searchField.setMaxLength(20);

		this.scrolling = new KineticScrolling();
		this.scrolling.setAllowOverScroll(true);
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

	public void setWidth(int width) {
		this.width = width;
		this.optionWidth = width - optionStartX - paddingRight;
	}

	public int getTargetWidth() { return targetWidth; }

	public void setAlpha(float alpha) {
		COL_BG.a = BG_ALPHA * alpha;
		COL_WHITE.a = alpha;
		COL_PINK.a = alpha;
		COL_CYAN.a = alpha;
		COL_GREY.a = alpha * LINEALPHA;
		COL_BLUE.a = alpha;
		COL_COMBOBOX_HOVER.a = alpha;
		COL_INDICATOR.a = alpha * (1f - (float) indicatorHideAnimationTime / INDICATORHIDEANIMATIONTIME) * INDICATOR_ALPHA;
	}

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
	public void activate() {
		this.active = true;
		scrolling.setPosition(0f);
		resetSearch();
		searchField.setFocus(true);
		for (DropdownMenu<Object> dropdownMenu : dropdownMenus.values()) {
			dropdownMenu.activate();
		}
	}

	/** Deactivates the component. */
	public void deactivate() {
		this.active = false;
		searchField.setFocus(false);
		for (DropdownMenu<Object> dropdownMenu : dropdownMenus.values()) {
			dropdownMenu.deactivate();
		}
		if (openDropdownMenu != null) {
			openDropdownMenu.reset();
			openDropdownMenu = null;
		}
	}

	/**
	 * Whether to consume all unprocessed events, and close the overlay.
	 * @param flag {@code true} to consume all events (default is {@code false})
	 */
	public void setConsumeAndClose(boolean flag) { this.consumeAndClose = flag; }

	@Override
	public void render(GUIContext container, Graphics g) throws SlickException {
		// background
		g.setColor(COL_BG);
		g.fillRect(x, y, width, height);

		// title
		String title = "Options";
		String subtitle = String.format("Change the way %s behaves", OpsuConstants.PROJECT_NAME);
		Utils.drawCentered(Fonts.LARGE, width, 0, (int) (textOptionsY - scrolling.getPosition()), title, COL_WHITE);
		Utils.drawCentered(Fonts.MEDIUM, width, 0, (int) (textChangeY - scrolling.getPosition()), subtitle, COL_PINK);

		// selected option indicator
		g.setColor(COL_INDICATOR);
		g.fillRect(0, indicatorRenderPos - scrolling.getPosition(), width, optionHeight);

		// options
		g.setClip((int) x, (int) y, width, containerHeight);
		renderOptions(g);
		if (openDropdownMenu != null) {
			openDropdownMenu.render(container, g);
			if (!openDropdownMenu.isOpen()) {
				openDropdownMenu = null;
			}
		}

		// search
		int ypos = (int) (posSearchY + textSearchYOffset - scrolling.getPosition());
		if (scrolling.getPosition() > posSearchY) {
			ypos = textSearchYOffset;
			g.setColor(COL_BG);
			g.fillRect(0, 0, width, textSearchYOffset * 2 + Fonts.LARGE.getLineHeight());
		}
		String searchText = "Type to search!";
		if (lastSearchText.length() > 0) {
			searchText = lastSearchText;
		}
		Utils.drawCentered(Fonts.LARGE, width, 0, ypos, searchText, COL_WHITE);
		int imgPosX = (width - Fonts.LARGE.getWidth(searchText)) / 2 - searchImg.getWidth() - 10;
		searchImg.draw(imgPosX, ypos + Fonts.LARGE.getLineHeight() * 0.25f, COL_WHITE);

		// back arrow
		backButton.setX(x + backButton.getImage().getWidth());
		backButton.setY(textSearchYOffset + backButton.getImage().getHeight() / 2);
		backButton.draw(COL_WHITE);

		// restart button
		if (showRestartButton) {
			restartButton.setX(x + width - restartButton.getImage().getWidth() * 1.5f);
			restartButton.setY(textSearchYOffset + restartButton.getImage().getHeight() / 2);
			restartButton.draw(COL_WHITE);
		}

		// scrollbar
		int scrollbarWidth = 10, scrollbarHeight = 45;
		float scrollbarX = x + width - scrollbarWidth;
		float scrollbarY = y + (scrolling.getPosition() / maxScrollOffset) * (containerHeight - scrollbarHeight);
		g.setColor(COL_WHITE);
		g.fillRect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight);
		g.clearClip();

		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			g.setColor(COL_BG);
			g.fillRect(0, 0, containerWidth, containerHeight);
			g.setColor(COL_WHITE);
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
	private void renderOptions(Graphics g) throws SlickException {
		int y = (int) (-scrolling.getPosition() + optionStartY);
		maxScrollOffset = optionStartY;
		boolean render = true;
		int groupIndex = 0;
		for (; groupIndex < groups.length && render; groupIndex++) {
			OptionGroup section = groups[groupIndex];
			if (section.isFiltered()) {
				continue;
			}
			int lineStartY = (int) (y + Fonts.LARGE.getLineHeight() * 0.6f);
			if (section.getOptions() == null) {
				Utils.drawRightAligned(Fonts.XLARGE, width, -paddingRight, (int) (y + Fonts.XLARGE.getLineHeight() * 0.3f), section.getName(), COL_CYAN);
			} else {
				Fonts.MEDIUMBOLD.drawString(paddingTextLeft, lineStartY, section.getName(), COL_WHITE);
			}
			y += Fonts.LARGE.getLineHeight() * 1.5f;
			maxScrollOffset += Fonts.LARGE.getLineHeight() * 1.5f;
			if (section.getOptions() == null) {
				continue;
			}
			int lineHeight = (int) (Fonts.LARGE.getLineHeight() * 0.9f);
			for (int optionIndex = 0; optionIndex < section.getOptions().length; optionIndex++) {
				GameOption option = section.getOptions()[optionIndex];
				if (option.isFiltered()) {
					continue;
				}
				if (y > -optionHeight || (openDropdownMenu != null && openDropdownMenu.equals(dropdownMenus.get(option)))) {
					renderOption(g, option, y);
				}
				y += optionHeight;
				maxScrollOffset += optionHeight;
				lineHeight += optionHeight;
				if (y > height) {
					render = false;
					maxScrollOffset += (section.getOptions().length - optionIndex - 1) * optionHeight;
					break;
				}
			}
			g.setColor(COL_GREY);
			g.fillRect(paddingLeft, lineStartY, LINEWIDTH, lineHeight);
		}
		// iterate over skipped options to correctly calculate max scroll offset
		for (; groupIndex < groups.length; groupIndex++) {
			maxScrollOffset += Fonts.LARGE.getLineHeight() * 1.5f;
			if (groups[groupIndex].getOptions() != null) {
				maxScrollOffset += groups[groupIndex].getOptions().length * optionHeight;
			}
		}
		if (openDropdownMenu != null) {
			maxScrollOffset = Math.max(maxScrollOffset, openDropdownVirtualY + openDropdownMenu.getHeight());
		}
		maxScrollOffset -= height * 2 / 3;
		if (maxScrollOffset < 0) {
			maxScrollOffset = 0;
		}
		scrolling.setMinMax(0, maxScrollOffset);
	}

	/**
	 * Renders the given option.
	 * @param g the graphics context
	 * @param option the game option
	 * @param cy the y coordinate
	 */
	private void renderOption(Graphics g, GameOption option, int cy) throws SlickException {
		OptionType type = option.getType();
		Object[] items = option.getItemList();
		if (items != null)
			renderListOption(g, option, cy);
		else if (type == OptionType.BOOLEAN)
			renderCheckOption(option, cy);
		else if (type == OptionType.NUMERIC)
			renderSliderOption(g, option, cy);
		else
			renderGenericOption(option, cy);
	}

	/**
	 * Renders a list option.
	 * @param g the graphics context
	 * @param option the game option
	 * @param y the y coordinate
	 */
	private void renderListOption(Graphics g, GameOption option, int y) throws SlickException {
		// draw option name
		int nameWith = Fonts.MEDIUM.getWidth(option.getName());
		Fonts.MEDIUM.drawString(optionStartX, y + optionTextOffsetY, option.getName(), COL_WHITE);
		nameWith += 15;
		int comboboxStartX = optionStartX + nameWith;
		int comboboxWidth = optionWidth - nameWith;
		DropdownMenu<Object> dropdown = dropdownMenus.get(option);
		if (dropdown == null) {
			return;
		}
		dropdown.setWidth(comboboxWidth);
		dropdown.setLocation(comboboxStartX, y + dropdownMenuPaddingY);
		if (dropdown.isOpen()) {
			openDropdownMenu = dropdown;
			if (openDropdownMenuOldValue == null) {
				openDropdownMenuOldValue = option.getValueString();
			}
			openDropdownVirtualY = maxScrollOffset;
			return;
		}
		if (openDropdownMenu == null) {
			dropdown.activate();
		} else {
			dropdown.deactivate();
		}
		dropdown.render(container, g);
	}

	/**
	 * Renders a boolean option.
	 * @param option the game option
	 * @param y the y coordinate
	 */
	private void renderCheckOption(GameOption option, int y) {
		// draw checkbox
		if (option.getBooleanValue()) {
			checkOnImg.draw(optionStartX, y + controlImagePadding, COL_PINK);
		} else {
			checkOffImg.draw(optionStartX, y + controlImagePadding, COL_PINK);
		}

		// draw option name
		Fonts.MEDIUM.drawString(optionStartX + 30, y + optionTextOffsetY, option.getName(), COL_WHITE);
	}

	/**
	 * Renders a slider option.
	 * @param g the graphics context
	 * @param option the game option
	 * @param y the y coordinate
	 */
	private void renderSliderOption(Graphics g, GameOption option, int y) {
		// draw option name and value
		final int padding = 10;
		String value = option.getValueString();
		int nameWidth = Fonts.MEDIUM.getWidth(option.getName());
		int valueWidth = Fonts.MEDIUM.getWidth(value);
		Fonts.MEDIUM.drawString(optionStartX, y + optionTextOffsetY, option.getName(), COL_WHITE);
		Fonts.MEDIUM.drawString(optionStartX + optionWidth - valueWidth, y + optionTextOffsetY, value, COL_BLUE);

		// calculate slider positions
		int sliderWidth = optionWidth - nameWidth - padding - padding - valueWidth;
		if (sliderWidth <= 1) {
			// menu hasn't slide in far enough to need to draw the slider
			return;
		}

		int sliderStartX = optionStartX + nameWidth + padding;
		if (hoverOption == option) {
			sliderOptionStartX = sliderStartX;
			if (!isAdjustingSlider) {
				sliderOptionWidth = sliderWidth;
			} else {
				sliderWidth = sliderOptionWidth;
			}
		}
		int sliderEndX = optionStartX + nameWidth + padding + sliderWidth;

		// draw slider
		float sliderValue = (float) (option.getIntegerValue() - option.getMinValue()) / (option.getMaxValue() - option.getMinValue());
		float sliderBallPos = sliderStartX + (int) ((sliderWidth - controlImageSize) * sliderValue);

		g.setLineWidth(3f);
		g.setColor(COL_PINK);
		if (sliderValue > 0.0001f) {
			g.drawLine(sliderStartX, y + optionHeight / 2, sliderBallPos, y + optionHeight / 2);
		}
		sliderBallImg.draw(sliderBallPos, y + controlImagePadding, COL_PINK);
		if (sliderValue < 0.999f) {
			float a = COL_PINK.a;
			COL_PINK.a *= 0.45f;
			g.setColor(COL_PINK);
			g.drawLine(sliderBallPos + controlImageSize + 1, y + optionHeight / 2, sliderEndX, y + optionHeight / 2);
			COL_PINK.a = a;
		}
	}

	/**
	 * Renders a generic option.
	 * @param option the game option
	 * @param y the y coordinate
	 */
	private void renderGenericOption(GameOption option, int y) {
		// draw option name and value
		String value = option.getValueString();
		int valueLen = Fonts.MEDIUM.getWidth(value);
		Fonts.MEDIUM.drawString(optionStartX, y + optionTextOffsetY, option.getName(), COL_WHITE);
		Fonts.MEDIUM.drawString(optionStartX + optionWidth - valueLen, y + optionTextOffsetY, value, COL_BLUE);
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
		updateIndicatorAlpha(delta);

		// selected option indicator position
		indicatorRenderPos = indicatorPos;
		if (indicatorMoveAnimationTime > 0) {
			indicatorMoveAnimationTime += delta;
			if (indicatorMoveAnimationTime > INDICATORMOVEANIMATIONTIME) {
				indicatorMoveAnimationTime = 0;
				indicatorRenderPos += indicatorOffsetToNextPos;
				indicatorOffsetToNextPos = 0;
				indicatorPos = indicatorRenderPos;
			} else {
				float progress = (float) indicatorMoveAnimationTime / INDICATORMOVEANIMATIONTIME;
				indicatorRenderPos += AnimationEquation.OUT_BACK.calc(progress) * indicatorOffsetToNextPos;
			}
		}

		if (!mouseMoved)
			return;

		// update slider option
		if (isAdjustingSlider)
			adjustSlider(mouseX, mouseY);

	}

	/**
	 * Updates the alpha value of the selected option indicator.
	 */
	private void updateIndicatorAlpha(int delta) {
		if (hoverOption == null) {
			if (indicatorHideAnimationTime < INDICATORHIDEANIMATIONTIME) {
				indicatorHideAnimationTime += delta;
				if (indicatorHideAnimationTime > INDICATORHIDEANIMATIONTIME) {
					indicatorHideAnimationTime = INDICATORHIDEANIMATIONTIME;
				}
				float progress = AnimationEquation.IN_CUBIC.calc((float) indicatorHideAnimationTime / INDICATORHIDEANIMATIONTIME);
				COL_INDICATOR.a = (1f - progress) * INDICATOR_ALPHA;
			}
		} else if (indicatorHideAnimationTime > 0) {
			indicatorHideAnimationTime -= delta * 3;
			if (indicatorHideAnimationTime < 0) {
				indicatorHideAnimationTime = 0;
			}
			COL_INDICATOR.a = (1f - (float) indicatorHideAnimationTime / INDICATORHIDEANIMATIONTIME) * INDICATOR_ALPHA;
		}
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
		if (openDropdownMenu != null || keyEntryLeft || keyEntryRight)
			return;
		if (selectedOption != null) {
			hoverOption = selectedOption;
			return;
		}

		if (mouseX > width) {
			hoverOption = null;
			return;
		}

		hoverOption = getOptionAtPosition(mouseX, mouseY);
	}

	/**
	 * Resets the search.
	 */
	private void resetSearch() {
		for (OptionGroup group : groups) {
			group.setFiltered(false);
			if (group.getOptions() == null) {
				continue;
			}
			for (GameOption opt : group.getOptions()) {
				opt.filter(null);
			}
		}
		searchField.setText("");
		lastSearchText = "";
	}

	/**
	 * Update the visible options to conform to the search string.
	 */
	private void updateSearch() {
		OptionGroup lastBigGroup = null;
		boolean lastBigSectionMatches = false;
		for (OptionGroup group : groups) {
			boolean sectionMatches = group.getName().toLowerCase().contains(lastSearchText);
			if (group.getOptions() == null) {
				lastBigSectionMatches = sectionMatches;
				lastBigGroup = group;
				group.setFiltered(true);
				continue;
			}
			boolean allOptionsHidden = true;
			for (int optionIndex = 0; optionIndex < group.getOptions().length; optionIndex++) {
				GameOption option = group.getOptions()[optionIndex];
				if (lastBigSectionMatches || sectionMatches) {
					allOptionsHidden = false;
					option.filter(null);
					continue;
				}
				if (!option.filter(lastSearchText)) {
					allOptionsHidden = false;
				}
			}
			if (allOptionsHidden) {
				group.setFiltered(true);
			} else {
				if (lastBigGroup != null) {
					lastBigGroup.setFiltered(false);
				}
				group.setFiltered(false);
			}
		}
		updateHoverOption(prevMouseX, prevMouseY);
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

		consumeEvent();

		// key entry state
		if (keyEntryRight) {
			Options.setGameKeyRight(key);
			keyEntryRight = false;
			return;
		} else if (keyEntryLeft) {
			Options.setGameKeyLeft(key);
			keyEntryLeft = false;
			return;
		}

		// esc: close open list option, otherwise close overlay
		if (key == Input.KEY_ESCAPE) {
			if (lastSearchText.length() > 0) {
				resetSearch();
			} else {
				listener.close();
			}
			return;
		}

		searchField.setFocus(true);
		searchField.keyPressed(key, c);
		if (!searchField.getText().equals(lastSearchText)) {
			lastSearchText = searchField.getText().toLowerCase();
			updateSearch();
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
		if (cy < y || cy < textSearchYOffset * 2 + Fonts.LARGE.getLineHeight() || cx < x + optionStartX || cx > x + optionStartX + optionWidth)
			return null;  // out of bounds

		int mouseVirtualY = (int) (scrolling.getPosition() + cy - y - optionStartY);
		for (OptionGroup group : groups) {
			if (group.isFiltered()) {
				continue;
			}
			mouseVirtualY -= optionGroupPadding;
			if (group.getOptions() == null) {
				continue;
			}
			for (int i = 0; i < group.getOptions().length; i++) {
				if (group.getOptions()[i].isFiltered()) {
					continue;
				}
				if (mouseVirtualY <= optionHeight) {
					if (mouseVirtualY >= 0) {
						int indicatorPos = (int) (scrolling.getPosition() + cy - mouseVirtualY);
						if (indicatorPos != this.indicatorPos + indicatorOffsetToNextPos) {
							this.indicatorPos += indicatorOffsetToNextPos; // finish the current moving animation
							indicatorOffsetToNextPos = indicatorPos - this.indicatorPos;
							indicatorMoveAnimationTime = 1; // starts animation
						}
						return group.getOption(i);
					}
					return null;
				}
				mouseVirtualY -= optionHeight;
			}
		}
		return null;
	}

	@Override
	public void setFocus(boolean focus) { /* does not currently use the "focus" concept */ }

	/**
	 * Resets all state.
	 */
	public void reset() {
		hoverOption = selectedOption = null;
		isAdjustingSlider = false;
		openDropdownMenu = null;
		sliderOptionStartX = sliderOptionWidth = 0;
		keyEntryLeft = keyEntryRight = false;
		mousePressY = -1;
		prevMouseX = prevMouseY = -1;
		sliderSoundDelay = 0;
		backButton.resetHover();
		restartButton.resetHover();
	}
}
