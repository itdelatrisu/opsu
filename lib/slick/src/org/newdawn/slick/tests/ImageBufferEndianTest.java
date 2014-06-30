package org.newdawn.slick.tests;
import java.nio.ByteOrder;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.ImageBuffer;
import org.newdawn.slick.SlickException;

/**
 * Quick test for endianess in image buffers 
 * 
 * @author thaaks
 */
public class ImageBufferEndianTest extends BasicGame {
	/** The buffer filled with red pixels */
   private ImageBuffer redImageBuffer;
   /** The buffer filled with blue pixels */
   private ImageBuffer blueImageBuffer;
   /** The image created from red pixels */
   private Image fromRed;
   /** The image create from blue pixels */
   private Image fromBlue;
   /** The edian message */
   private String endian;
   
   /**
    * Create a new test
    */
   public ImageBufferEndianTest() {
      super("ImageBuffer Endian Test");
   }

   /**
    * Entry point to the test
    * 
    * @param args The arguments passed into the test
    */
   public static void main(String[] args) {
      try {
         AppGameContainer container = new AppGameContainer(new ImageBufferEndianTest());
         container.setDisplayMode(800,600,false);
         container.start();
      } catch (SlickException e) {
         e.printStackTrace();
      }
   }

   /*
    * (non-Javadoc)
    * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
    */
   public void render(GameContainer container, Graphics g) throws SlickException {
      g.setColor(Color.white);
      g.drawString("Endianness is " + endian, 10, 100);
      
      g.drawString("Image below should be red", 10, 200);
      g.drawImage(fromRed, 10, 220);
      g.drawString("Image below should be blue", 410, 200);
      g.drawImage(fromBlue, 410, 220);
   }

   /*
    * (non-Javadoc)
    * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
    */
   public void init(GameContainer container) throws SlickException {
      // detect what endian we have
      if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
             endian = "Big endian";
          } else if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
             endian = "Little endian";
          } else
             endian = "no idea";
      
      redImageBuffer = new ImageBuffer(100,100);
      fillImageBufferWithColor(redImageBuffer, Color.red, 100, 100);
      
      blueImageBuffer = new ImageBuffer(100,100);
      fillImageBufferWithColor(blueImageBuffer, Color.blue, 100, 100);
      
      fromRed = redImageBuffer.getImage();
      fromBlue = blueImageBuffer.getImage();
   }
   
   /**
    * Fill a buffer with a given color
    *
    * @param buffer The buffer to fill
    * @param c The color to apply
    * @param width The width of the image
    * @param height The height of the image
    */
   private void fillImageBufferWithColor(ImageBuffer buffer, Color c, int width, int height) {
      for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            buffer.setRGBA(x, y, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
         }
      }
   }

   /*
    * (non-Javadoc)
    * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
    */
   public void update(GameContainer container, int delta) throws SlickException {
      // nothing to do
   }

} 