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
import org.newdawn.slick.state.StateBasedGame;

public class BackButton {

	/** Skinned back button. */
	private static MenuButton backButton;

	/** Colors. */
	private static final Color
		COLOR_PINK = new Color(238, 51, 153),
		COLOR_DARKPINK = new Color(186, 19, 121);

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;

	public BackButton(GameContainer container, StateBasedGame game) {
		this.container = container;
		this.game = game;

		if (!GameImage.MENU_BACK.hasGameSkinImage()) {
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

		int textWidth = Fonts.MEDIUM.getWidth("back");
		float paddingY = Fonts.MEDIUM.getHeight("back");
		// getHeight doesn't seem to be so accurate
		float textOffset = paddingY * 0.264f;
		paddingY *= 0.736f;
		float paddingX = paddingY / 2f;
		float chevronSize = paddingY * 3f / 2f;
		Float beatProgress = MusicController.getBeatProgress();
		if (beatProgress == null) {
			beatProgress = 0f;
		}
		chevronSize += 3 * beatProgress;

		float top = container.getHeight() - paddingY * 4;
		int buttonSize = (int) (paddingY * 3f);
		g.setColor(COLOR_PINK);
		g.fillRect(0, top, buttonSize + paddingX * 2 + textWidth, buttonSize);
		Image buttonPart = GameImage.MENU_BACK_BUTTON.getImage().getScaledCopy(buttonSize, buttonSize);
		buttonPart.draw(0, top, COLOR_PINK);
		buttonPart.draw(buttonSize + paddingX * 2 + textWidth - buttonSize * 0.722f, top, COLOR_PINK);

		GameImage.MENU_BACK_CHEVRON.getImage().getScaledCopy((int) chevronSize, (int) chevronSize).drawCentered((buttonSize * 0.813f) / 2, top + paddingY * 1.5f);


		float textY = container.getHeight() - paddingY * 3 - textOffset;
		Fonts.MEDIUM.drawString(buttonSize + paddingX, textY + 1, "back", Color.black);
		Fonts.MEDIUM.drawString(buttonSize + paddingX, textY, "back", Color.white);
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
		return false;
	}

	/**
	 * Resets the hover fields for the button.
	 */
	public void resetHover() {
		if (backButton != null) {
			backButton.resetHover();
			return;
		}

	}

}
