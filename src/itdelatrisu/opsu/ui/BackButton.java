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
import org.newdawn.slick.*;

import java.util.Arrays;

public class BackButton {

	/** Skinned back button. */
	private static MenuButton backButton;

	/** Colors. */
	private static final Color
		COLOR_PINK = new Color(238, 51, 153),
		COLOR_DARKPINK = new Color(186, 19, 121);

	private static final int  ANIMATION_TIME = 500;

	private int animationTime;
	private int firstButtonSize;
	private int buttonSize;
	private int buttonSlopeWidth;
	private int secondButtonSize;
	private boolean isHovered;
	private int textWidth;
	private float paddingY;
	private float paddingX;
	private float textOffset;
	private float chevronBaseSize;
	private int top;
	private Image buttonPart;
	private int realWidth;

	public BackButton(GameContainer container) {
		if (GameImage.MENU_BACK.getImage() != null && GameImage.MENU_BACK.getImage().getWidth() < 2) {
			textWidth = Fonts.MEDIUM.getWidth("back");
			paddingY = Fonts.MEDIUM.getHeight("back");
			// getHeight doesn't seem to be so accurate
			textOffset = paddingY * 0.264f;
			paddingY *= 0.736f;
			paddingX = paddingY / 2f;
			chevronBaseSize = paddingY * 3f / 2f;
			top = (int) (container.getHeight() - paddingY * 4f);
			buttonSize = (int) (paddingY * 3f);
			buttonSlopeWidth = (int) (buttonSize * 0.295f);
			firstButtonSize = buttonSize;
			secondButtonSize = (int) (buttonSlopeWidth + paddingX * 2 + textWidth);
			buttonPart = GameImage.MENU_BACK_BUTTON.getImage().getScaledCopy(buttonSize, buttonSize);
			return;
		}

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
		// draw image if it's skinned
		if (backButton != null) {
			backButton.draw();
			return;
		}

		// calc chevron size
		Float beatProgress = MusicController.getBeatProgress();
		if (beatProgress == null) {
			beatProgress = 0f;
		}
		int chevronSize = (int) (chevronBaseSize - (isHovered ? 6f : 3f) * beatProgress);

		// calc button sizes
		AnimationEquation anim;
		if (isHovered) {
			anim = AnimationEquation.OUT_ELASTIC;
		} else {
			anim = AnimationEquation.IN_ELASTIC;
		}
		float progress = anim.calc((float) animationTime / ANIMATION_TIME);
		float firstSize = firstButtonSize + (firstButtonSize - buttonSlopeWidth * 2) * progress;
		float secondSize = secondButtonSize + secondButtonSize * 0.25f * progress;
		realWidth = (int) (firstSize + secondSize);

		// right part
		g.setColor(COLOR_PINK);
		g.fillRect(0, top, firstSize + secondSize - buttonSlopeWidth, buttonSize);
		buttonPart.draw(firstSize + secondSize - buttonSize, top, COLOR_PINK);

		// left part
		Color hoverColor = new Color(0f, 0f, 0f);
		hoverColor.r = COLOR_PINK.r + (COLOR_DARKPINK.r - COLOR_PINK.r) * progress;
		hoverColor.g = COLOR_PINK.g + (COLOR_DARKPINK.g - COLOR_PINK.g) * progress;
		hoverColor.b = COLOR_PINK.b + (COLOR_DARKPINK.b - COLOR_PINK.b) * progress;
		g.setColor(hoverColor);
		g.fillRect(0, top, firstSize - buttonSlopeWidth, buttonSize);
		buttonPart.draw(firstSize - buttonSize, top, hoverColor);

		// chevron
		GameImage.MENU_BACK_CHEVRON.getImage().getScaledCopy(chevronSize, chevronSize).drawCentered((firstSize - buttonSlopeWidth / 2) / 2, top + paddingY * 1.5f);

		// text
		float textY = top + paddingY - textOffset;
		float textX = firstSize + (secondSize - paddingX * 2 - textWidth) / 2;
		Fonts.MEDIUM.drawString(textX, textY + 1, "back", Color.black);
		Fonts.MEDIUM.drawString(textX, textY, "back", Color.white);
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
		isHovered = top - paddingY < cy && cx < realWidth;
		if (isHovered) {
			if (!wasHovered) {
				animationTime = 0;
			}
			animationTime += delta;
			if (animationTime > ANIMATION_TIME) {
				animationTime = ANIMATION_TIME;
			}
		} else {
			if (wasHovered) {
				animationTime = ANIMATION_TIME;
			}
			animationTime -= delta;
			if (animationTime < 0) {
				animationTime = 0;
			}
		}
	}

	/**
	 * Returns true if the coordinates are within the button bounds.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public boolean contains(float cx, float cy) {
		if (backButton != null) {
			return backButton.contains(cx, cy);
		}
		return isHovered;
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
