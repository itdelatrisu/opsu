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

package itdelatrisu.opsu.options;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.OpsuConstants;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.options.Options.GameOption;
import itdelatrisu.opsu.options.Options.GameOption.OptionType;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.DropdownMenu;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.KineticScrolling;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.gui.AbstractComponent;
import org.newdawn.slick.gui.GUIContext;
import org.newdawn.slick.gui.TextField;

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

	/** The group that is visible on screen. */
	private OptionGroup activeGroup;

	/** The group that is being hovered in the navigation bar. */
	private OptionGroup hoveredNavigationGroup;

	/** The event listener. */
	private final OptionsOverlayListener listener;

	/** Control images. */
	private final Image sliderBallImg, checkOnImg, checkOffImg;

	/** Control image size. */
	private final int iconSize;

	/** Search image. */
	private Image searchImg;

	/** Target duration, in ms, of the move animation for the indicator. */
	private static final int INDICATOR_MOVE_ANIMATION_TIME = 166;

	/**  Selected option indicator virtual position. */
	private int indicatorPos;

	/**  Selected option indicator render position. */
	private int indicatorRenderPos;

	/** Selected option indicator offset to next position. */
	private int indicatorOffsetToNextPos;

	/** Selected option indicator move to next position animation time past. */
	private int indicatorMoveAnimationTime;

	/** Target duration, in ms, of the fadeout animation for the indicator. */
	private static final int INDICATOR_HIDE_ANIMATION_TIME = 500;

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

	/** Size of a button in the navigation bar. */
	private int navButtonSize;

	/** Start Y position of buttons in the navigation bar. */
	private int navStartY;

	/** The width of the navigation bar. */
	private int navTargetWidth;

	/** The real width of the navigation bar, altered by hiding state and animations. */
	private int navWidth;

	/** The width of the indicator in the navigation bar. */
	private int navIndicatorWidth;

	/** How long the mouse has been hovering over the navigation bar, for animations. */
	private int navHoverTime;

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
	private static final int LINE_WIDTH = 3;

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
	private int searchY;

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
	private Map<GameOption, DropdownMenu<Object>> dropdownMenus;

	/** The vertical padding to use when rendering a dropdown menu. */
	private int dropdownMenuPaddingY;

	/** The dropdown menu that is currently open. */
	private DropdownMenu<Object> openDropdownMenu;

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

	/** The rotation of the search text for the 'invalid search' animation. */
	private int invalidSearchImgRotation;

	/** The rotation of the search image for the 'invalid search' animation. */
	private int invalidSearchTextRotation;

	/** The 'invalid search' animation progress. */
	private AnimatedValue invalidSearchAnimation = new AnimatedValue(500, 1f, 0f, AnimationEquation.LINEAR);

	/** Desired alpha values for specific colors. */
	private static final float
		BG_ALPHA = 0.7f,
		LINEALPHA = 0.8f,
		INDICATOR_ALPHA = 0.8f;

	/** Colors. */
	private static final Color
		COLOR_BG = new Color(Color.black),
		COLOR_WHITE = new Color(Color.white),
		COLOR_PINK = new Color(Colors.PINK_OPTION),
		COLOR_CYAN = new Color(88, 218, 254),
		COLOR_GREY = new Color(55, 55, 57),
		COLOR_BLUE = new Color(Colors.BLUE_BACKGROUND),
		COLOR_COMBOBOX_HOVER = new Color(185, 19, 121),
		COLOR_INDICATOR = new Color(Color.black),
		COLOR_NAV_BG = new Color(COLOR_BG),
		COLOR_NAV_INDICATOR = new Color(COLOR_PINK),
		COLOR_NAV_WHITE = new Color(COLOR_WHITE),
		COLOR_NAV_FILTERED = new Color(37, 37, 37),
		COLOR_NAV_INACTIVE = new Color(153, 153, 153),
		COLOR_NAV_FILTERED_HOVERED = new Color(58, 58, 58);

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
		this.searchImg = GameImage.SEARCH.getImage().getScaledCopy(searchImgSize, searchImgSize);

		boolean isWidescreen = (int) (container.getAspectRatio() * 1000) > 1500; // 1333 = 4:3, 1777 = 16:9

		// overlay positions
		this.x = 0;
		this.y = 0;
		this.targetWidth = (int) (containerWidth * (isWidescreen ? 0.4f : 0.5f));
		this.height = containerHeight;

		// option positions
		float navIconWidthRatio = isWidescreen ? 0.046875f : 0.065f; // non-widescreen ratio is not accurate
		navButtonSize = (int) (container.getWidth() * navIconWidthRatio);
		navIndicatorWidth = navButtonSize / 10;
		navTargetWidth = (int) (targetWidth * 0.45f) - navButtonSize;
		this.paddingRight = (int) (containerWidth * 0.009375f); // not so accurate
		this.paddingLeft = navButtonSize + (int) (containerWidth * 0.0180f); // not so accurate
		this.paddingTextLeft = paddingLeft + LINE_WIDTH + (int) (containerWidth * 0.00625f); // not so accurate
		this.optionStartX = paddingTextLeft;
		this.textOptionsY = Fonts.LARGE.getLineHeight() * 2;
		this.textChangeY = textOptionsY + Fonts.LARGE.getLineHeight();
		this.searchY = textChangeY + Fonts.MEDIUM.getLineHeight() * 2;
		this.textSearchYOffset = Fonts.MEDIUM.getLineHeight() / 2;
		this.optionStartY = searchY + Fonts.MEDIUM.getLineHeight() + Fonts.LARGE.getLineHeight();
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

		// search field
		this.searchField = new TextField(container, null, 0, 0, 0, 0);
		searchField.setMaxLength(20);

		// kinetic scrolling
		this.scrolling = new KineticScrolling();
		scrolling.setAllowOverScroll(true);

		// calculate offset for navigation bar icons
		int navTotalHeight = 0;
		for (OptionGroup group : groups) {
			if (group.getOptions() == null) {
				navTotalHeight += navButtonSize;
			}
		}
		navStartY = (height - navTotalHeight) / 2;
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

	/** Returns the target width. */
	public int getTargetWidth() { return targetWidth; }

	/**
	 * Sets the alpha levels of the overlay.
	 * @param mainAlpha alpha value of the main elements
	 * @param navigationAlpha alpha value of the navigation bar elements
	 */
	public void setAlpha(float mainAlpha, float navigationAlpha) {
		COLOR_NAV_FILTERED.a = navigationAlpha;
		COLOR_NAV_INACTIVE.a = navigationAlpha;
		COLOR_NAV_FILTERED_HOVERED.a = navigationAlpha;
		COLOR_NAV_INDICATOR.a = navigationAlpha;
		COLOR_NAV_WHITE.a = navigationAlpha;
		COLOR_NAV_BG.a = navigationAlpha;
		COLOR_BG.a = BG_ALPHA * mainAlpha;
		COLOR_WHITE.a = mainAlpha;
		COLOR_PINK.a = mainAlpha;
		COLOR_CYAN.a = mainAlpha;
		COLOR_GREY.a = mainAlpha * LINEALPHA;
		COLOR_BLUE.a = mainAlpha;
		COLOR_COMBOBOX_HOVER.a = mainAlpha;
		COLOR_INDICATOR.a = mainAlpha * (1f - (float) indicatorHideAnimationTime / INDICATOR_HIDE_ANIMATION_TIME) * INDICATOR_ALPHA;
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
		if (active)
			return;

		this.active = true;
		navHoverTime = 0;
		activeGroup = groups[0];
		scrolling.setPosition(0f);
		invalidSearchAnimation.setTime(invalidSearchAnimation.getDuration());
		if (dropdownMenus == null)
			createDropdownMenus();
		resetSearch();
		for (Map.Entry<GameOption, DropdownMenu<Object>> entry : dropdownMenus.entrySet()) {
			GameOption option = entry.getKey();
			DropdownMenu<Object> menu = entry.getValue();

			// activate component
			menu.activate();

			// find the current value (maybe find a better way to do this...)
			String selectedValue = option.getValueString();
			for (int i = 0; i < menu.getItemCount(); i++) {
				if (menu.getItemAt(i).toString().equals(selectedValue)) {
					menu.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	/** Deactivates the component. */
	public void deactivate() {
		if (!active)
			return;

		this.active = false;
		searchField.setFocus(false);
		for (DropdownMenu<Object> menu : dropdownMenus.values())
			menu.deactivate();
		resetOpenDropdownMenu();
	}

	/**
	 * Whether to consume all unprocessed events, and close the overlay.
	 * @param flag {@code true} to consume all events (default is {@code false})
	 */
	public void setConsumeAndClose(boolean flag) { this.consumeAndClose = flag; }

	@Override
	public void render(GUIContext container, Graphics g) throws SlickException {
		g.setClip((int) x + navButtonSize, (int) y, width - navButtonSize, height);

		// background
		g.setColor(COLOR_BG);
		g.fillRect((int) x + navButtonSize, y, width, height);

		// title
		String title = "Options";
		String subtitle = String.format("Change the way %s behaves", OpsuConstants.PROJECT_NAME);
		Fonts.LARGE.drawString(
			x + navButtonSize + (width - navButtonSize - Fonts.LARGE.getWidth(title)) / 2,
			(int) (y + textOptionsY - scrolling.getPosition()),
			title, COLOR_WHITE
		);
		Fonts.MEDIUM.drawString(
			x + navButtonSize + (width - navButtonSize - Fonts.MEDIUM.getWidth(subtitle)) / 2,
			(int) (y + textChangeY - scrolling.getPosition()),
			subtitle, COLOR_PINK
		);

		// selected option indicator
		g.setColor(COLOR_INDICATOR);
		g.fillRect(x + navButtonSize, indicatorRenderPos - scrolling.getPosition(), width, optionHeight);

		// options
		renderOptions(g);
		if (openDropdownMenu != null) {
			openDropdownMenu.render(container, g);
			if (!openDropdownMenu.isOpen())
				openDropdownMenu = null;
		}

		// search
		int ypos = (int) (y + searchY + textSearchYOffset - scrolling.getPosition());
		if (scrolling.getPosition() > searchY) {
			ypos = (int) (y + textSearchYOffset);
			g.setColor(COLOR_BG);
			g.fillRect(x, y, width, textSearchYOffset * 2 + Fonts.LARGE.getLineHeight());
		}
		Color searchColor = COLOR_WHITE;
		float invalidProgress = 0f;
		if (!invalidSearchAnimation.isFinished()) {
			invalidProgress = 1f - invalidSearchAnimation.getValue();
			searchColor = new Color(0f, 0f, 0f, searchColor.a);
			searchColor.r = COLOR_PINK.r + (1f - COLOR_PINK.r) * invalidProgress;
			searchColor.g = COLOR_PINK.g + (1f - COLOR_PINK.g) * invalidProgress;
			searchColor.b = COLOR_PINK.b + (1f - COLOR_PINK.b) * invalidProgress;
			invalidProgress = 1f - invalidProgress;
		}
		String searchText = "Type to search!";
		if (lastSearchText.length() > 0)
			searchText = lastSearchText;
		int textWidth = width - navButtonSize;
		if (!invalidSearchAnimation.isFinished())
			g.rotate(navButtonSize + textWidth / 2, ypos, invalidProgress * invalidSearchTextRotation);
		int searchTextX = (int) (x + navButtonSize + (width - navButtonSize - Fonts.LARGE.getWidth(searchText) - searchImg.getWidth() - 10) / 2);
		Fonts.LARGE.drawString(searchTextX + searchImg.getWidth() + 10, ypos, searchText, searchColor);
		g.resetTransform();
		if (!invalidSearchAnimation.isFinished())
			g.rotate(searchTextX + searchImg.getWidth() / 2, ypos, invalidProgress * invalidSearchImgRotation);
		searchImg.draw(
			searchTextX,
			ypos + Fonts.LARGE.getLineHeight() * 0.25f,
			searchColor
		);
		g.resetTransform();

		// back arrow
		backButton.setX(x + backButton.getImage().getWidth());
		backButton.setY(y + textSearchYOffset + backButton.getImage().getHeight() / 2);
		backButton.draw(COLOR_WHITE);

		// restart button
		if (showRestartButton) {
			restartButton.setX(x + width - restartButton.getImage().getWidth() * 1.5f);
			restartButton.setY(y + textSearchYOffset + restartButton.getImage().getHeight() / 2);
			restartButton.draw(COLOR_WHITE);
		}

		// scrollbar
		int scrollbarWidth = 10, scrollbarHeight = 45;
		float scrollbarX = x + width - scrollbarWidth;
		float scrollbarY = y + (scrolling.getPosition() / maxScrollOffset) * (height - scrollbarHeight);
		g.setColor(COLOR_WHITE);
		g.fillRect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight);

		g.clearClip();

		// navigation bar
		renderNavigation(g);

		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			g.setColor(COLOR_BG);
			g.fillRect(0, 0, containerWidth, containerHeight);
			g.setColor(COLOR_WHITE);
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
	 * Renders the navigation bar
	 * @param g the graphics context
	 */
	private void renderNavigation(Graphics g) {
		navWidth = navButtonSize;
		if (navHoverTime >= 600)
			navWidth += navTargetWidth;
		else if (navHoverTime > 300) {
			AnimationEquation anim = AnimationEquation.IN_EXPO;
			if (input.getMouseX() < navWidth)
				anim = AnimationEquation.OUT_EXPO;
			float progress = anim.calc((navHoverTime - 300f) / 300f);
			navWidth += (int) (progress * navTargetWidth);
		}

		g.setClip((int) x, (int) y, navWidth, height);
		g.setColor(COLOR_NAV_BG);
		g.fillRect((int) x, (int) y, navWidth, height);
		int y = navStartY + (int) this.y;
		float iconSize = navButtonSize / 2.5f;
		float iconPadding = (int) x + iconSize * 0.75f;
		int fontOffsetX = (int) x + navButtonSize + navIndicatorWidth;
		int fontOffsetY = (int) this.y + (navButtonSize - Fonts.MEDIUM.getLineHeight()) / 2;
		for (OptionGroup group : groups) {
			if (group.getOptions() != null)
				continue;
			Color iconCol = COLOR_NAV_INACTIVE;
			Color fontCol = COLOR_NAV_WHITE;
			if (group == activeGroup) {
				iconCol = COLOR_NAV_WHITE;
				g.fillRect((int) x, y, navWidth, navButtonSize);
				g.setColor(COLOR_NAV_INDICATOR);
				g.fillRect((int) x + navWidth - navIndicatorWidth, y, navIndicatorWidth, navButtonSize);
			} else if (group == hoveredNavigationGroup)
				iconCol = COLOR_NAV_WHITE;
			if (!group.isVisible()) {
				iconCol = fontCol = COLOR_NAV_FILTERED;
				if (group == hoveredNavigationGroup)
					iconCol = COLOR_NAV_FILTERED_HOVERED;
			}
			group.getIcon().draw(iconPadding, y + iconPadding, iconSize, iconSize, iconCol);
			if (navHoverTime > 0)
				Fonts.MEDIUM.drawString(fontOffsetX, y + fontOffsetY, group.getName(), fontCol);
			y += navButtonSize;
		}
		g.clearClip();
	}

	/**
	 * Renders all options.
	 * @param g the graphics context
	 */
	private void renderOptions(Graphics g) throws SlickException {
		// render all headers and options
		int cy = (int) (y + -scrolling.getPosition() + optionStartY);
		int virtualY = 0;
		for (OptionGroup group : groups) {
			if (!group.isVisible())
				continue;

			// header
			int lineStartY = (int) (cy + Fonts.LARGE.getLineHeight() * 0.6f);
			if (group.getOptions() == null) {
				// section header
				float previousAlpha = COLOR_CYAN.a;
				if (group != activeGroup)
					COLOR_CYAN.a *= 0.2f;
				Fonts.XLARGE.drawString(
					x + width - Fonts.XLARGE.getWidth(group.getName()) - paddingRight,
					(int) (cy + Fonts.XLARGE.getLineHeight() * 0.3f),
					group.getName(), COLOR_CYAN
				);
				COLOR_CYAN.a = previousAlpha;
			} else {
				// subsection header
				Fonts.MEDIUMBOLD.drawString(x + paddingTextLeft, lineStartY, group.getName(), COLOR_WHITE);
			}
			cy += optionGroupPadding;
			virtualY += optionGroupPadding;

			if (group.getOptions() == null)
				continue;  // header only

			// options
			int lineHeight = (int) (Fonts.LARGE.getLineHeight() * 0.9f);
			boolean finished = false;
			for (GameOption option : group.getOptions()) {
				if (!option.isVisible())
					continue;

				// render the option if it fits, or is the open list option
				boolean isOpenOption = (openDropdownMenu != null && openDropdownMenu.equals(dropdownMenus.get(option)));
				if (cy > -optionHeight || isOpenOption) {
					renderOption(g, option, cy);
					if (isOpenOption)
						openDropdownVirtualY = virtualY;
				}
				cy += optionHeight;
				virtualY += optionHeight;
				lineHeight += optionHeight;

				if (cy > height) {
					finished = true;
					break;
				}
			}

			// container rectangle
			g.setColor(COLOR_GREY);
			g.fillRect(x + paddingLeft, lineStartY, LINE_WIDTH, lineHeight);

			if (finished)
				break;
		}

		// recompute max scroll offset
		int scrollOffset = 0;
		for (OptionGroup group : groups) {
			if (!group.isVisible())
				continue;
			scrollOffset += optionGroupPadding;
			if (group.getOptions() == null)
				continue;
			for (GameOption option : group.getOptions()) {
				if (!option.isVisible())
					continue;
				scrollOffset += optionHeight;
			}
		}
		if (openDropdownMenu != null)
			scrollOffset = Math.max(scrollOffset, openDropdownVirtualY + openDropdownMenu.getHeight());
		scrollOffset -= (int) ((height - optionStartY) * 0.9f);
		scrollOffset = Math.max(scrollOffset, 0);
		maxScrollOffset = scrollOffset;
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
	 * @param cy the y coordinate
	 */
	private void renderListOption(Graphics g, GameOption option, int cy) throws SlickException {
		DropdownMenu<Object> dropdown = dropdownMenus.get(option);
		if (dropdown == null)
			return;  // no options?

		// draw option name
		int nameWidth = Fonts.MEDIUM.getWidth(option.getName());
		Fonts.MEDIUM.drawString(x + optionStartX, cy + optionTextOffsetY, option.getName(), COLOR_WHITE);
		nameWidth += 15;
		int comboboxStartX = (int) (x + optionStartX + nameWidth);
		int comboboxWidth = optionWidth - nameWidth;
		if (comboboxWidth < controlImageSize)
			return;

		// draw dropdown menu
		dropdown.setWidth(comboboxWidth);
		dropdown.setLocation(comboboxStartX, cy + dropdownMenuPaddingY);
		if (dropdown.isOpen()) {
			openDropdownMenu = dropdown;
			return;
		}
		if (openDropdownMenu == null)
			dropdown.activate();
		else
			dropdown.deactivate();
		dropdown.render(container, g);
	}

	/**
	 * Renders a boolean option.
	 * @param option the game option
	 * @param cy the y coordinate
	 */
	private void renderCheckOption(GameOption option, int cy) {
		// draw checkbox
		if (option.getBooleanValue())
			checkOnImg.draw(x + optionStartX, cy + controlImagePadding, COLOR_PINK);
		else
			checkOffImg.draw(x + optionStartX, cy + controlImagePadding, COLOR_PINK);

		// draw option name
		Fonts.MEDIUM.drawString(x + optionStartX + 30, cy + optionTextOffsetY, option.getName(), COLOR_WHITE);
	}

	/**
	 * Renders a slider option.
	 * @param g the graphics context
	 * @param option the game option
	 * @param cy the y coordinate
	 */
	private void renderSliderOption(Graphics g, GameOption option, int cy) {
		// draw option name and value
		final int padding = 10;
		String value = option.getValueString();
		int nameWidth = Fonts.MEDIUM.getWidth(option.getName());
		int valueWidth = Fonts.MEDIUM.getWidth(value);
		Fonts.MEDIUM.drawString(x + optionStartX, cy + optionTextOffsetY, option.getName(), COLOR_WHITE);
		Fonts.MEDIUM.drawString(x + optionStartX + optionWidth - valueWidth, cy + optionTextOffsetY, value, COLOR_BLUE);

		// calculate slider positions
		int sliderWidth = optionWidth - nameWidth - padding - padding - valueWidth;
		if (sliderWidth <= 1)
			return;  // menu hasn't slid in far enough to need to draw the slider
		int sliderStartX = (int) (x + optionStartX + nameWidth + padding);
		if (hoverOption == option) {
			sliderOptionStartX = sliderStartX;
			if (!isAdjustingSlider)
				sliderOptionWidth = sliderWidth;
			else
				sliderWidth = sliderOptionWidth;
		}
		int sliderEndX = sliderStartX + sliderWidth;

		// draw slider
		float sliderValue = (float) (option.getIntegerValue() - option.getMinValue()) / (option.getMaxValue() - option.getMinValue());
		float sliderBallPos = sliderStartX + (int) ((sliderWidth - controlImageSize) * sliderValue);
		g.setLineWidth(3f);
		g.setColor(COLOR_PINK);
		if (sliderValue > 0.0001f)
			g.drawLine(sliderStartX, cy + optionHeight / 2, sliderBallPos, cy + optionHeight / 2);
		sliderBallImg.draw(sliderBallPos, cy + controlImagePadding, COLOR_PINK);
		if (sliderValue < 0.999f) {
			float oldAlpha = COLOR_PINK.a;
			COLOR_PINK.a *= 0.45f;
			g.setColor(COLOR_PINK);
			g.drawLine(sliderBallPos + controlImageSize + 1, cy + optionHeight / 2, sliderEndX, cy + optionHeight / 2);
			COLOR_PINK.a = oldAlpha;
		}
	}

	/**
	 * Renders a generic option.
	 * @param option the game option
	 * @param cy the y coordinate
	 */
	private void renderGenericOption(GameOption option, int cy) {
		// draw option name and value
		String value = option.getValueString();
		int valueWidth = Fonts.MEDIUM.getWidth(value);
		Fonts.MEDIUM.drawString(x + optionStartX, cy + optionTextOffsetY, option.getName(), COLOR_WHITE);
		Fonts.MEDIUM.drawString(x + optionStartX + optionWidth - valueWidth, cy + optionTextOffsetY, value, COLOR_BLUE);
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
		updateIndicatorAlpha(delta);
		int previousScrollingPosition = (int) scrolling.getPosition();
		scrolling.update(delta);
		boolean scrollingMoved = (int) scrolling.getPosition() != previousScrollingPosition;
		invalidSearchAnimation.update(delta);

		if (mouseMoved || scrollingMoved)
			updateHoverOption(mouseX, mouseY);

		if (mouseX < navWidth) {
			if (navHoverTime < 600)
				navHoverTime += delta;
		} else if (navHoverTime > 0)
			navHoverTime -= delta;
		navHoverTime = Utils.clamp(navHoverTime, 0, 600);
		updateActiveSection();
		updateNavigationHover(mouseX, mouseY);

		// selected option indicator position
		indicatorRenderPos = indicatorPos;
		if (indicatorMoveAnimationTime > 0) {
			indicatorMoveAnimationTime += delta;
			if (indicatorMoveAnimationTime > INDICATOR_MOVE_ANIMATION_TIME) {
				indicatorMoveAnimationTime = 0;
				indicatorRenderPos += indicatorOffsetToNextPos;
				indicatorOffsetToNextPos = 0;
				indicatorPos = indicatorRenderPos;
			} else {
				float progress = (float) indicatorMoveAnimationTime / INDICATOR_MOVE_ANIMATION_TIME;
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
	 * Updates the "hovered" navigation button based on the current mouse coordinates.
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 */
	private void updateNavigationHover(int mouseX, int mouseY) {
		hoveredNavigationGroup = null;
		if (mouseX >= (int) x + navWidth)
			return;
		int y = navStartY + (int) this.y;
		for (OptionGroup group : groups) {
			if (group.getOptions() != null)
				continue;
			int nextY = y + navButtonSize;
			if (y <= mouseY && mouseY < nextY)
				hoveredNavigationGroup = group;
			y = nextY;
		}
	}

	/**
	 * Updates the alpha value of the selected option indicator.
	 */
	private void updateIndicatorAlpha(int delta) {
		if (hoverOption == null) {
			if (indicatorHideAnimationTime < INDICATOR_HIDE_ANIMATION_TIME) {
				indicatorHideAnimationTime += delta;
				if (indicatorHideAnimationTime > INDICATOR_HIDE_ANIMATION_TIME)
					indicatorHideAnimationTime = INDICATOR_HIDE_ANIMATION_TIME;
				float progress = AnimationEquation.IN_CUBIC.calc((float) indicatorHideAnimationTime / INDICATOR_HIDE_ANIMATION_TIME);
				COLOR_INDICATOR.a = (1f - progress) * INDICATOR_ALPHA;
			}
		} else if (indicatorHideAnimationTime > 0) {
			indicatorHideAnimationTime -= delta * 3;
			if (indicatorHideAnimationTime < 0)
				indicatorHideAnimationTime = 0;
			COLOR_INDICATOR.a = (1f - (float) indicatorHideAnimationTime / INDICATOR_HIDE_ANIMATION_TIME) * INDICATOR_ALPHA;
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
	 * Updates the "active" section based on the position of the sections on the screen.
	 * The "active" section is the one that is visible in the top half of the screen.
	 */
	private void updateActiveSection() {
		activeGroup = groups[0];
		int virtualY = optionStartY;
		for (OptionGroup group : groups) {
			if (!group.isVisible())
				continue;
			virtualY += optionGroupPadding;
			if (virtualY > Utils.clamp(scrolling.getPosition(), scrolling.getMin(), scrolling.getMax()) + height / 2)
				return;
			if (group.getOptions() == null) {
				activeGroup = group;
				continue;
			}
			for (int optionIndex = 0; optionIndex < group.getOptions().length; optionIndex++) {
				GameOption option = group.getOption(optionIndex);
				if (!option.isVisible())
					continue;
				virtualY += optionHeight;
			}
		}
	}

	/**
	 * Updates the "hovered" option based on the current mouse coordinates.
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 */
	private void updateHoverOption(int mouseX, int mouseY) {
		if (mouseX < (int) x + navWidth && !isAdjustingSlider) {
			hoverOption = null;
			return;
		}
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

	/** Resets the open dropdown menu, if any. */
	private void resetOpenDropdownMenu() {
		if (openDropdownMenu != null) {
			openDropdownMenu.reset();
			openDropdownMenu = null;
		}
	}

	/** Shows or hides an option. */
	private void toggleOption(GameOption option, boolean visible) {
		option.setVisible(visible);

		DropdownMenu<Object> menu = dropdownMenus.get(option);
		if (menu != null) {
			if (visible)
				menu.activate();
			else
				menu.deactivate();
		}
	}

	/**
	 * Resets the search.
	 */
	private void resetSearch() {
		for (OptionGroup group : groups) {
			group.setVisible(true);
			if (group.getOptions() == null)
				continue;
			for (GameOption option : group.getOptions())
				toggleOption(option, true);
		}
		searchField.setText("");
		lastSearchText = "";
		resetOpenDropdownMenu();
	}

	/**
	 * Update the visible options to conform to the search string.
	 */
	private void updateSearch() {
		// matching a header name will match all sub-items
		OptionGroup lastHeader = null;
		boolean lastHeaderMatches = false;
		for (OptionGroup group : groups) {
			boolean groupMatches = group.getName().toLowerCase().contains(lastSearchText);
			if (group.getOptions() == null) {
				lastHeaderMatches = groupMatches;
				lastHeader = group;
				group.setVisible(false);
				continue;
			}
			boolean allOptionsHidden = true;
			for (GameOption option : group.getOptions()) {
				if (lastHeaderMatches || groupMatches) {
					allOptionsHidden = false;
					toggleOption(option, true);
					continue;
				}
				if (option.matches(lastSearchText)) {
					allOptionsHidden = false;
					toggleOption(option, true);
				} else
					toggleOption(option, false);
			}
			if (allOptionsHidden)
				group.setVisible(false);
			else {
				if (lastHeader != null)
					lastHeader.setVisible(true);
				group.setVisible(true);
			}
		}
		resetOpenDropdownMenu();
		updateHoverOption(prevMouseX, prevMouseY);
		updateActiveSection();
	}

	/**
	 * Checks if the specified search filter matches one or more options.
	 */
	private boolean hasSearchResults(String searchText) {
		for (OptionGroup group : groups) {
			if (group.getName().toLowerCase().contains(searchText))
				return true;
			if (group.getOptions() == null)
				continue;
			for (GameOption option : group.getOptions()) {
				if (option.matches(searchText))
					return true;
			}
		}
		return false;
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
		mousePressY = y;
		if (x < (int) this.x + navWidth)
			return;
		hoverOption = selectedOption = getOptionAtPosition(x, y);
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
				if (oldValue != hoverOption.getBooleanValue() && hoverOption.isRestartRequired()) {
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

		if (hoveredNavigationGroup != null && hoveredNavigationGroup.isVisible()) {
			int sectionPosition = 0;
			for (OptionGroup group : groups) {
				if (group == hoveredNavigationGroup)
					break;
				if (!group.isVisible())
					continue;
				sectionPosition += optionGroupPadding;
				if (group.getOptions() == null)
					continue;
				for (GameOption option : group.getOptions()) {
					if (option.isVisible())
						sectionPosition += optionHeight;
				}
			}
			sectionPosition = Utils.clamp(sectionPosition, (int) scrolling.getMin(), (int) scrolling.getMax());
			scrolling.scrollToPosition(sectionPosition);
		}
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (!active)
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
		if (UI.globalMouseWheelMoved(delta, true)) {
			consumeEvent();
			return;
		}

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
			if (lastSearchText.length() > 0)
				resetSearch();
			else
				listener.close();
			return;
		}

		if (UI.globalKeyPressed(key))
			return;

		searchField.setFocus(true);
		searchField.keyPressed(key, c);
		searchField.setFocus(false);
		if (!searchField.getText().equals(lastSearchText)) {
			String newSearchText = searchField.getText().toLowerCase();
			if (!hasSearchResults(newSearchText)) {
				searchField.setText(lastSearchText);
				invalidSearchAnimation.setTime(0);
				Random rand = new Random();
				invalidSearchImgRotation = 10 + rand.nextInt(10);
				invalidSearchTextRotation = 10 + rand.nextInt(10);
				if (rand.nextBoolean())
					invalidSearchImgRotation = -invalidSearchImgRotation;
				if (rand.nextBoolean())
					invalidSearchTextRotation = -invalidSearchTextRotation;
			} else {
				lastSearchText = newSearchText;
				updateSearch();
			}
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
			if (!group.isVisible())
				continue;

			mouseVirtualY -= optionGroupPadding;
			if (group.getOptions() == null)
				continue;

			for (GameOption option : group.getOptions()) {
				if (!option.isVisible())
					continue;

				if (mouseVirtualY <= optionHeight) {
					if (mouseVirtualY >= 0) {
						int indicatorPos = (int) (scrolling.getPosition() + cy - mouseVirtualY);
						if (indicatorPos != this.indicatorPos + indicatorOffsetToNextPos) {
							this.indicatorPos += indicatorOffsetToNextPos;  // finish the current moving animation
							indicatorOffsetToNextPos = indicatorPos - this.indicatorPos;
							indicatorMoveAnimationTime = 1;  // starts animation
						}
						return option;
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

	/** Creates the dropdown menus. */
	private void createDropdownMenus() {
		this.dropdownMenus = new IdentityHashMap<GameOption, DropdownMenu<Object>>();
		for (OptionGroup group : groups) {
			if (group.getOptions() == null)
				continue;

			for (final GameOption option : group.getOptions()) {
				Object[] items = option.getItemList();
				if (items == null)
					continue;

				// build dropdown menu
				DropdownMenu<Object> menu = new DropdownMenu<Object>(container, items, 0, 0) {
					@Override
					public void itemSelected(int index, Object item) {
						option.selectItem(index, OptionsOverlay.this.container);

						// show restart button?
						if (option.isRestartRequired()) {
							showRestartButton = true;
							UI.getNotificationManager().sendBarNotification("Restart to apply changes.");
						}
					}

					@Override
					public boolean menuClicked(int index) {
						if (input.getMouseX() < navWidth)
							return false;
						SoundController.playSound(SoundEffect.MENUCLICK);
						openDropdownMenu = null;
						return true;
					}
				};
				menu.setBackgroundColor(COLOR_BG);
				menu.setBorderColor(Color.transparent);
				menu.setChevronDownColor(COLOR_WHITE);
				menu.setChevronRightColor(COLOR_BG);
				menu.setHighlightColor(COLOR_COMBOBOX_HOVER);
				menu.setTextColor(COLOR_WHITE);

				dropdownMenuPaddingY = (optionHeight - menu.getHeight()) / 2;
				dropdownMenus.put(option, menu);
			}
		}
	}

	/**
	 * Resets all state.
	 */
	public void reset() {
		hoverOption = selectedOption = null;
		isAdjustingSlider = false;
		resetOpenDropdownMenu();
		sliderOptionStartX = sliderOptionWidth = 0;
		keyEntryLeft = keyEntryRight = false;
		mousePressY = -1;
		prevMouseX = prevMouseY = -1;
		sliderSoundDelay = 0;
		backButton.resetHover();
		restartButton.resetHover();
	}
}
