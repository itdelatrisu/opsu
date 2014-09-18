package itdelatrisu.opsu;


import itdelatrisu.opsu.Resources.ImageResource;
import itdelatrisu.opsu.Resources.Origin;
import itdelatrisu.opsu.states.Options.OpsuOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public enum OpsuImages implements ImageResource {
	LOGO("logo", 1 / 1.2f),
	
	MENU_PLAY("menu-play") {
		@Override
		public Image scale(Image original, Resources resources,
				OpsuOptions options, int windowWidth, int windowHeight) {
			return original.getScaledCopy(resources.getResource(LOGO).getWidth() * .83f / original.getWidth());
		}
	},
	MENU_EXIT("menu-exit") {
		@Override
		public Image scale(Image original, Resources resources,
				OpsuOptions options, int windowWidth, int windowHeight) {
			return original.getScaledCopy(resources.getResource(LOGO).getWidth() * .66f / original.getWidth());
		}
	},
	MENU_BACKGROUND("menu-background") {
		@Override
		public Image scale(Image original, Resources resources,
				OpsuOptions options, int windowWidth, int windowHeight) {
			return original.getScaledCopy(windowWidth, windowHeight);
		}
	},
	MENU_BUTTON_BACKGROUND("menu-button-background"),
	MUSIC_PLAY("music-play"),
	MUSIC_PAUSE("music-pause"),
	MUSIC_NEXT("music-next"),
	MUSIC_PREVIOUS("music-previous"),
	
	EXIT_BUTTON_CENTER("button-middle") {
		@Override
		public Image scale(Image original, Resources resources,
				OpsuOptions options, int windowWidth, int windowHeight) {
			return original.getScaledCopy(windowWidth / 2, original.getHeight());
		}
	},
	EXIT_BUTTON_LEFT("button-left"),
	EXIT_BUTTON_RIGHT("button-right"),
	TAB("selection-tab", 0.033f),
	SEARCH("search"),
	OPTIONS("options"),
	MUSIC_NOTE("music-note"),
	LOADER("loader");

	/**
	 * @param name
	 * @param height 0 for no scaling, otherwise fraction of the window height
	 */
	private OpsuImages(String name, float height) {
		this.name = name;
		this.height = height;
	}
	
	private OpsuImages(String name) {
		this(name, 0);
	}

	String name;
	float height;
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<String> getExtensions() {
		return Arrays.asList(".png", ".jpg");
	}

	@Override
	public List<Origin> getOrigins() {
		return Collections.singletonList(Origin.GAME);
	}

	@Override
	public void unload(Image data) {
		try {
			data.destroy();
		} catch (SlickException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Image scale(Image original, Resources resources, OpsuOptions options,
			int windowWidth, int windowHeight) {
		if(height == 0)
			return null;
		return original.getScaledCopy(windowHeight * height / original.getHeight());
	}
	
}