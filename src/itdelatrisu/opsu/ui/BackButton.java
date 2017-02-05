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
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * Back button.
 *
 * @author yugecin (https://github.com/yugecin)
 */
public class BackButton {
	/** Skinned back button. */
	private MenuButton backButton;

	/** Colors. */
	private static final Color
		COLOR_PINK = new Color(238, 51, 153),
		COLOR_DARKPINK = new Color(186, 19, 121);

	/** Target duration, in ms, of the button animations. */
	private static final int ANIMATION_TIME = 500;

	/** Button text. */
	private static final String BUTTON_TEXT = "back";

	/** How much time passed for the animations. */
	private int animationTime;

	/** The size of the slope image (square shape). */
	private int slopeImageSize;

	/** The width of the slope part in the slope image. */
	private int slopeImageSlopeWidth;

	/** The width of the first part of the button. */
	private int firstButtonWidth;

	/** The width of the second part of the button. */
	private int secondButtonWidth;

	/** Variable to hold the hovered state, to not recalculate it twice per frame. */
	private boolean isHovered;

	/** The width of the button text to draw. */
	private int textWidth;

	/** Y padding for the text and general positioning. */
	private float paddingY;

	/** X padding for the text. */
	private float paddingX;

	/** Y text offset because getHeight is not so accurate. */
	private float textOffset;

	/** The base size of the chevron. */
	private float chevronBaseSize;

	/** The Y position of where the button starts. */
	private int buttonYpos;

	/** Variable holding the slope image. */
	private Image slopeImage;

	/** The real button with, determined by the size and animations. */
	private int realButtonWidth;

	/**
	 * Creates the back button.
	 * @param container the game container
	 */
	public BackButton(GameContainer container) {
		// not skinned: dynamic button
		if (!GameImage.MENU_BACK.hasGameSkinImage()) {
			backButton = null;
			textWidth = Fonts.MEDIUM.getWidth(BUTTON_TEXT);
			paddingY = Fonts.MEDIUM.getHeight(BUTTON_TEXT);
			// getHeight doesn't seem to be so accurate
			textOffset = paddingY * 0.264f;
			paddingY *= 0.736f;
			paddingX = paddingY / 2f;
			chevronBaseSize = paddingY * 3f / 2f;
			buttonYpos = (int) (container.getHeight() - paddingY * 4f);
			slopeImageSize = (int) (paddingY * 3f);
			slopeImageSlopeWidth = (int) (slopeImageSize * 0.295f);
			firstButtonWidth = slopeImageSize;
			secondButtonWidth = (int) (slopeImageSlopeWidth + paddingX * 2 + textWidth);
			slopeImage = GameImage.MENU_BACK_SLOPE.getImage().getScaledCopy(slopeImageSize, slopeImageSize);
			return;
		}

		// skinned: static image
		if (GameImage.MENU_BACK.getImages() != null) {
			Animation back = GameImage.MENU_BACK.getAnimation();
			backButton = new MenuButton(back, back.getWidth() / 2f, container.getHeight() - (back.getHeight() / 2f));
		} else {
			Image back = GameImage.MENU_BACK.getImage();
			backButton = new MenuButton(back, back.getWidth() / 2f, container.getHeight() - (back.getHeight() / 2f));
		}
		backButton.setHoverAnimationDuration(350);
		backButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
		backButton.setHoverExpand(MenuButton.Expand.UP_RIGHT);
	}

	/**
	 * Draws the backbutton.
	 */
	public void draw(Graphics g) {
		if (backButton != null) {
			backButton.draw();
			return;
		}

		// calculate chevron size
		Float beatProgress = MusicController.getBeatProgress();
		if (beatProgress == null)
			beatProgress = 0f;
		else if (beatProgress < 0.2f)
			beatProgress = AnimationEquation.IN_QUINT.calc(beatProgress * 5f);
		else
			beatProgress = 1f - AnimationEquation.OUT_QUAD.calc((beatProgress - 0.2f) * 1.25f);
		int chevronSize = (int) (chevronBaseSize - (isHovered ? 6f : 3f) * beatProgress);

		// calculate button sizes
		AnimationEquation anim = isHovered ? AnimationEquation.OUT_ELASTIC : AnimationEquation.IN_ELASTIC;
		float progress = anim.calc((float) animationTime / ANIMATION_TIME);
		float firstWidth = firstButtonWidth + (firstButtonWidth - slopeImageSlopeWidth * 2) * progress;
		float secondWidth = secondButtonWidth + secondButtonWidth * 0.25f * progress;
		realButtonWidth = (int) (firstWidth + secondWidth);

		// right part
		g.setColor(COLOR_PINK);
		g.fillRect(0, buttonYpos, firstWidth + secondWidth - slopeImageSlopeWidth, slopeImageSize);
		slopeImage.draw(firstWidth + secondWidth - slopeImageSize, buttonYpos, COLOR_PINK);

		// left part
		Color hoverColor = new Color(0f, 0f, 0f);
		hoverColor.r = COLOR_PINK.r + (COLOR_DARKPINK.r - COLOR_PINK.r) * progress;
		hoverColor.g = COLOR_PINK.g + (COLOR_DARKPINK.g - COLOR_PINK.g) * progress;
		hoverColor.b = COLOR_PINK.b + (COLOR_DARKPINK.b - COLOR_PINK.b) * progress;
		g.setColor(hoverColor);
		g.fillRect(0, buttonYpos, firstWidth - slopeImageSlopeWidth, slopeImageSize);
		slopeImage.draw(firstWidth - slopeImageSize, buttonYpos, hoverColor);

		// chevron
		Image chevron = GameImage.MENU_BACK_CHEVRON.getImage().getScaledCopy(chevronSize, chevronSize);
		chevron.drawCentered((firstWidth - slopeImageSlopeWidth / 2) / 2, buttonYpos + paddingY * 1.5f);

		// text
		float textY = buttonYpos + paddingY - textOffset;
		float textX = firstWidth + (secondWidth - paddingX * 2 - textWidth) / 2;
		Fonts.MEDIUM.drawString(textX, textY + 1, BUTTON_TEXT, Color.black);
		Fonts.MEDIUM.drawString(textX, textY, BUTTON_TEXT, Color.white);
	}

	/**
	 * Processes a hover action depending on whether or not the cursor
	 * is hovering over the button.
	 * @param delta the delta interval
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public void hoverUpdate(int delta, int cx, int cy) {
		if (backButton != null) {
			backButton.hoverUpdate(delta, cx, cy);
			return;
		}

		boolean wasHovered = isHovered;
		isHovered = buttonYpos - paddingY < cy && cx < realButtonWidth;
		if (isHovered) {
			if (!wasHovered)
				animationTime = 0;
			animationTime += delta;
			if (animationTime > ANIMATION_TIME)
				animationTime = ANIMATION_TIME;
		} else {
			if (wasHovered)
				animationTime = ANIMATION_TIME;
			animationTime -= delta;
			if (animationTime < 0)
				animationTime = 0;
		}
	}

	/**
	 * Returns true if the coordinates are within the button bounds.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public boolean contains(float cx, float cy) {
		if (backButton != null)
			return backButton.contains(cx, cy);

		return buttonYpos - paddingY < cy && cx < realButtonWidth;
	}

	/**
	 * Resets the hover fields for the button.
	 */
	public void resetHover() {
		if (backButton != null) {
			backButton.resetHover();
			return;
		}

		isHovered = false;
		animationTime = 0;
	}
}
