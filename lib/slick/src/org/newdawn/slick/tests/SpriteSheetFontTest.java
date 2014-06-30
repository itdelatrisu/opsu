package org.newdawn.slick.tests;

import org.newdawn.slick.*;
import org.newdawn.slick.util.Log;

/**
 * Tests the SpriteSheetFont.
 *
 * @author Onno Scheffers
 */
public class SpriteSheetFontTest extends BasicGame {
   /**
    * The font we're going to use to render
    */
   private Font font;

   /**
    * Create a new test for font rendering
    */
   public SpriteSheetFontTest() {
      super("SpriteSheetFont Test");
   }

   /**
    * @see org.newdawn.slick.Game#init(org.newdawn.slick.GameContainer)
    */
   public void init(GameContainer container) throws SlickException {
      SpriteSheet sheet = new SpriteSheet("testdata/spriteSheetFont.png", 32, 32);
      font = new SpriteSheetFont(sheet, ' ');
   }

   /**
    * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer,org.newdawn.slick.Graphics)
    */
   public void render(GameContainer container, Graphics g) {
      g.setBackground(Color.gray);
      font.drawString(80, 5, "A FONT EXAMPLE", Color.red);
      font.drawString(100, 50, "A MORE COMPLETE LINE");
   }

   /**
    * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer,int)
    */
   public void update(GameContainer container, int delta) throws SlickException {
   }

   /**
    * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
    */
   public void keyPressed(int key, char c) {
      if (key == Input.KEY_ESCAPE) {
         System.exit(0);
      }
      if (key == Input.KEY_SPACE) {
         try {
            container.setDisplayMode(640, 480, false);
         } catch (SlickException e) {
            Log.error(e);
         }
      }
   }

   /**
    * The container we're using
    */
   private static AppGameContainer container;

   /**
    * Entry point to our test
    *
    * @param argv The arguments passed in the test
    */
   public static void main(String[] argv) {
      try {
         container = new AppGameContainer(new SpriteSheetFontTest());
         container.setDisplayMode(800, 600, false);
         container.start();
      } catch (SlickException e) {
         e.printStackTrace();
      }
   }
}
