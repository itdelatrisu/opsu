package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

/**
 * A test for transforming the graphics context
 *
 * @author Jesse Aldridge
 */
public class TransformTest2 extends BasicGame {
   /** The current scale applied to the graphics context */
   private float scale = 1;
   /** True if we should be scaling up */
   private boolean scaleUp;
   /** True if we should be scaling down */
   private boolean scaleDown;

   /** The camera's position */
   private float camX = 320;
   /** The camera's position */
   private float camY = 240;   
   /** True if the camera should be moving in the given direction */
   private boolean moveLeft;
   /** True if the camera should be moving in the given direction */
   private boolean moveUp;
   /** True if the camera should be moving in the given direction */
   private boolean moveRight;
   /** True if the camera should be moving in the given direction */
   private boolean moveDown;
   
   
   /**
    * Create a new test of graphics context rendering
    */
   public TransformTest2() {
      super("Transform Test");
   }
   
   /**
    * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
    */
   public void init(GameContainer container) throws SlickException {
      container.setTargetFrameRate(100);
   }

   /**
    * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
    */
   public void render(GameContainer contiainer, Graphics g) {
      g.translate(320,240);
      
      g.translate( -camX * scale, -camY * scale);
      
            
      g.scale(scale, scale);

      g.setColor(Color.red);
      for (int x=0;x<10;x++) {
         for (int y=0;y<10;y++) {
            g.fillRect(-500+(x*100), -500+(y*100), 80, 80);
         }
      }
      
      g.setColor(new Color(1,1,1,0.5f));
      g.fillRect(-320,-240,640,480);
      g.setColor(Color.white);
      g.drawRect(-320,-240,640,480);
   }

   /**
    * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
    */
   public void update(GameContainer container, int delta) {
      if (scaleUp) {
         scale += delta * 0.001f;
      }
      if (scaleDown) {
         scale -= delta * 0.001f;
      }
      
      float moveSpeed = delta * 0.4f * (1/scale);
      
      if( moveLeft ) {
         camX -= moveSpeed;
      }
      if( moveUp ) {
         camY -= moveSpeed;
      }
      if( moveRight) {
         camX += moveSpeed;
      }
      if( moveDown ) {
         camY += moveSpeed;
      }
   }

   /**
    * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
    */
   public void keyPressed(int key, char c) {
      if (key == Input.KEY_ESCAPE) {
         System.exit(0);
      }
      if (key == Input.KEY_Q) {
         scaleUp = true;
      }
      if (key == Input.KEY_A) {
         scaleDown = true;
      }

      if( key == Input.KEY_LEFT) {
         moveLeft = true;
      }
      if( key == Input.KEY_UP ) {
         moveUp = true;
      }
      if( key == Input.KEY_RIGHT ) {
         moveRight = true;
      }
      if( key == Input.KEY_DOWN ) {
         moveDown = true;
      }
   }

   /**
    * @see org.newdawn.slick.BasicGame#keyReleased(int, char)
    */
   public void keyReleased(int key, char c) {
      if (key == Input.KEY_Q) {
         scaleUp = false;
      }
      if (key == Input.KEY_A) {
         scaleDown = false;
      }
      
      if( key == Input.KEY_LEFT) {
         moveLeft = false;
      }
      if( key == Input.KEY_UP ) {
         moveUp = false;
      }
      if( key == Input.KEY_RIGHT ) {
         moveRight = false;
      }
      if( key == Input.KEY_DOWN ) {
         moveDown = false;
      }
   }
   
   /**
    * Entry point to our test
    *
    * @param argv The arguments passed to the test
    */
   public static void main(String[] argv) {
      try {
         AppGameContainer container = new AppGameContainer(new TransformTest2());
         container.setDisplayMode(640,480,false);
         container.start();
      } catch (SlickException e) {
         e.printStackTrace();
      }
   }
} 
