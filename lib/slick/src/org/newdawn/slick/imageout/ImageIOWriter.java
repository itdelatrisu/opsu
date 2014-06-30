package org.newdawn.slick.imageout;

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

/**
 * A utility to write a Slick image out using ImageIO
 * 
 * @author Jon
 */
public class ImageIOWriter implements ImageWriter {
	/**
	 * @see org.newdawn.slick.imageout.ImageWriter#saveImage(org.newdawn.slick.Image, 
	 * java.lang.String, java.io.OutputStream, boolean)
	 */
	public void saveImage(Image image, String format, OutputStream output, boolean hasAlpha)
			throws IOException {
		// conver the image into a byte buffer by reading each pixel in turn
		int len = 4 * image.getWidth() * image.getHeight();
		if (!hasAlpha) {
			len = 3 * image.getWidth() * image.getHeight();
		}
		
		ByteBuffer out = ByteBuffer.allocate(len);
		Color c;

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				c = image.getColor(x, y);

				out.put((byte) (c.r * 255.0f));
				out.put((byte) (c.g * 255.0f));
				out.put((byte) (c.b * 255.0f));
				if (hasAlpha) {
					out.put((byte) (c.a * 255.0f));
				}
			}
		}

		// create a raster of the correct format and fill it with our buffer
		DataBufferByte dataBuffer = new DataBufferByte(out.array(), len);
		
		PixelInterleavedSampleModel sampleModel;

		ColorModel cm;
		
		if (hasAlpha) {
			int[] offsets = { 0, 1, 2, 3 };
			sampleModel = new PixelInterleavedSampleModel(
					DataBuffer.TYPE_BYTE, image.getWidth(), image.getHeight(), 4,
					4 * image.getWidth(), offsets);
			
			cm = new ComponentColorModel(ColorSpace
					.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 },
					true, false, ComponentColorModel.TRANSLUCENT,
					DataBuffer.TYPE_BYTE);
		} else {
			int[] offsets = { 0, 1, 2};
			sampleModel = new PixelInterleavedSampleModel(
					DataBuffer.TYPE_BYTE, image.getWidth(), image.getHeight(), 3,
					3 * image.getWidth(), offsets);
			
			cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
	                new int[] {8,8,8,0},
	                false,
	                false,
	                ComponentColorModel.OPAQUE,
	                DataBuffer.TYPE_BYTE);
		}
		WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));

		// finally create the buffered image based on the data from the texture
		// and spit it through to ImageIO
		BufferedImage img = new BufferedImage(cm, raster, false, null);
		
		ImageIO.write(img, format, output);
	}
}
