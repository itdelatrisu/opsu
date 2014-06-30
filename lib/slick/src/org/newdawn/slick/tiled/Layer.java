package org.newdawn.slick.tiled;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Log;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A layer of tiles on the map
 * 
 * @author kevin
 */
public class Layer {
	/** The code used to decode Base64 encoding */
	private static byte[] baseCodes = new byte[256];

	/**
	 * Static initialiser for the codes created against Base64
	 */
	static {
		for (int i = 0; i < 256; i++)
			baseCodes[i] = -1;
		for (int i = 'A'; i <= 'Z'; i++)
			baseCodes[i] = (byte) (i - 'A');
		for (int i = 'a'; i <= 'z'; i++)
			baseCodes[i] = (byte) (26 + i - 'a');
		for (int i = '0'; i <= '9'; i++)
			baseCodes[i] = (byte) (52 + i - '0');
		baseCodes['+'] = 62;
		baseCodes['/'] = 63;
	}

	/** The map this layer belongs to */
	private final TiledMap map;
	/** The index of this layer */
	public int index;
	/** The name of this layer - read from the XML */
	public String name;
	/**
	 * The tile data representing this data, index 0 = tileset, index 1 = tile
	 * id
	 */
	public int[][][] data;
	/** The width of this layer */
	public int width;
	/** The height of this layer */
	public int height;

	/** the properties of this layer */
	public Properties props;

	/**
	 * Create a new layer based on the XML definition
	 * 
	 * @param element
	 *            The XML element describing the layer
	 * @param map
	 *            The map this layer is part of
	 * @throws SlickException
	 *             Indicates a failure to parse the XML layer
	 */
	public Layer(TiledMap map, Element element) throws SlickException {
		this.map = map;
		name = element.getAttribute("name");
		width = Integer.parseInt(element.getAttribute("width"));
		height = Integer.parseInt(element.getAttribute("height"));
		data = new int[width][height][3];

		// now read the layer properties
		Element propsElement = (Element) element.getElementsByTagName(
				"properties").item(0);
		if (propsElement != null) {
			NodeList properties = propsElement.getElementsByTagName("property");
			if (properties != null) {
				props = new Properties();
				for (int p = 0; p < properties.getLength(); p++) {
					Element propElement = (Element) properties.item(p);

					String name = propElement.getAttribute("name");
					String value = propElement.getAttribute("value");
					props.setProperty(name, value);
				}
			}
		}

		Element dataNode = (Element) element.getElementsByTagName("data").item(
				0);
		String encoding = dataNode.getAttribute("encoding");
		String compression = dataNode.getAttribute("compression");

		if (encoding.equals("base64") && compression.equals("gzip")) {
			try {
				Node cdata = dataNode.getFirstChild();
				char[] enc = cdata.getNodeValue().trim().toCharArray();
				byte[] dec = decodeBase64(enc);
				GZIPInputStream is = new GZIPInputStream(
						new ByteArrayInputStream(dec));

				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int tileId = 0;
						tileId |= is.read();
						tileId |= is.read() << 8;
						tileId |= is.read() << 16;
						tileId |= is.read() << 24;

						if (tileId == 0) {
							data[x][y][0] = -1;
							data[x][y][1] = 0;
							data[x][y][2] = 0;
						} else {
							TileSet set = map.findTileSet(tileId);

							if (set != null) {
								data[x][y][0] = set.index;
								data[x][y][1] = tileId - set.firstGID;
							}
							data[x][y][2] = tileId;
						}
					}
				}
			} catch (IOException e) {
				Log.error(e);
				throw new SlickException("Unable to decode base 64 block");
			}
		} else {
			throw new SlickException("Unsupport tiled map type: " + encoding
					+ "," + compression + " (only gzip base64 supported)");
		}
	}

	/**
	 * Get the gloal ID of the tile at the specified location in this layer
	 * 
	 * @param x
	 *            The x coorindate of the tile
	 * @param y
	 *            The y coorindate of the tile
	 * @return The global ID of the tile
	 */
	public int getTileID(int x, int y) {
		return data[x][y][2];
	}

	/**
	 * Set the global tile ID at a specified location
	 * 
	 * @param x
	 *            The x location to set
	 * @param y
	 *            The y location to set
	 * @param tile
	 *            The tile value to set
	 */
	public void setTileID(int x, int y, int tile) {
		if (tile == 0) {
			data[x][y][0] = -1;
			data[x][y][1] = 0;
			data[x][y][2] = 0;
		} else {
			TileSet set = map.findTileSet(tile);

			data[x][y][0] = set.index;
			data[x][y][1] = tile - set.firstGID;
			data[x][y][2] = tile;
		}
	}

	/**
	 * Render a section of this layer
	 * 
	 * @param x
	 *            The x location to render at
	 * @param y
	 *            The y location to render at
	 * @param sx
	 *            The x tile location to start rendering
	 * @param sy
	 *            The y tile location to start rendering
	 * @param width
	 *            The number of tiles across to render
	 * @param ty
	 *            The line of tiles to render
	 * @param lineByLine
	 *            True if we should render line by line, i.e. giving us a chance
	 *            to render something else between lines
	 * @param mapTileWidth
	 *            the tile width specified in the map file
	 * @param mapTileHeight
	 *            the tile height specified in the map file
	 */
	public void render(int x, int y, int sx, int sy, int width, int ty,
			boolean lineByLine, int mapTileWidth, int mapTileHeight) {
		for (int tileset = 0; tileset < map.getTileSetCount(); tileset++) {
			TileSet set = null;

			for (int tx = 0; tx < width; tx++) {
				if ((sx + tx < 0) || (sy + ty < 0)) {
					continue;
				}
				if ((sx + tx >= this.width) || (sy + ty >= this.height)) {
					continue;
				}

				if (data[sx + tx][sy + ty][0] == tileset) {
					if (set == null) {
						set = map.getTileSet(tileset);
						set.tiles.startUse();
					}

					int sheetX = set.getTileX(data[sx + tx][sy + ty][1]);
					int sheetY = set.getTileY(data[sx + tx][sy + ty][1]);

					int tileOffsetY = set.tileHeight - mapTileHeight;

					// set.tiles.renderInUse(x+(tx*set.tileWidth),
					// y+(ty*set.tileHeight), sheetX, sheetY);
					set.tiles.renderInUse(x + (tx * mapTileWidth), y
							+ (ty * mapTileHeight) - tileOffsetY, sheetX,
							sheetY);
				}
			}

			if (lineByLine) {
				if (set != null) {
					set.tiles.endUse();
					set = null;
				}
				map.renderedLine(ty, ty + sy, index);
			}

			if (set != null) {
				set.tiles.endUse();
			}
		}
	}

	/**
	 * Decode a Base64 string as encoded by TilED
	 * 
	 * @param data
	 *            The string of character to decode
	 * @return The byte array represented by character encoding
	 */
	private byte[] decodeBase64(char[] data) {
		int temp = data.length;
		for (int ix = 0; ix < data.length; ix++) {
			if ((data[ix] > 255) || baseCodes[data[ix]] < 0) {
				--temp;
			}
		}

		int len = (temp / 4) * 3;
		if ((temp % 4) == 3)
			len += 2;
		if ((temp % 4) == 2)
			len += 1;

		byte[] out = new byte[len];

		int shift = 0;
		int accum = 0;
		int index = 0;

		for (int ix = 0; ix < data.length; ix++) {
			int value = (data[ix] > 255) ? -1 : baseCodes[data[ix]];

			if (value >= 0) {
				accum <<= 6;
				shift += 6;
				accum |= value;
				if (shift >= 8) {
					shift -= 8;
					out[index++] = (byte) ((accum >> shift) & 0xff);
				}
			}
		}

		if (index != out.length) {
			throw new RuntimeException(
					"Data length appears to be wrong (wrote " + index
							+ " should be " + out.length + ")");
		}

		return out;
	}
}