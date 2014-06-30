package org.newdawn.slick;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.opengl.CursorLoader;
import org.newdawn.slick.opengl.ImageData;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.util.Log;

/**
 * A game container that displays the game as an applet. Note however that the
 * actual game container implementation is an internal class which can be
 * obtained with the getContainer() method - this is due to the Applet being a
 * class wrap than an interface.
 *
 * @author kevin
 */
public class AppletGameContainer extends Applet {
   /** The GL Canvas used for this container */
   protected ContainerPanel canvas;
   /** The actual container implementation */
   protected Container container;
   /** The parent of the display */
   protected Canvas displayParent;
   /** The thread that is looping for the game */
   protected Thread gameThread;
   /** Alpha background supported */
   protected boolean alphaSupport = true;
   
   /**
    * @see java.applet.Applet#destroy()
    */
   public void destroy() {
      if (displayParent != null) {
         remove(displayParent);
      }
      super.destroy();
      
      Log.info("Clear up");
   }

   /**
    * Clean up the LWJGL resources
    */
   private void destroyLWJGL() {
      container.stopApplet();
      
      try {
         gameThread.join();
      } catch (InterruptedException e) {
         Log.error(e);
      }
   }

   /**
    * @see java.applet.Applet#start()
    */
   public void start() {
      
   }
   
   /**
    * Start a thread to run LWJGL in
    */
   public void startLWJGL() {
      if (gameThread != null) {
         return;
      }
      
      gameThread = new Thread() {
         public void run() {
            try {
               canvas.start();
            }
            catch (Exception e) {
               e.printStackTrace();
               if (Display.isCreated()) {
                  Display.destroy();
               }
               displayParent.setVisible(false);//removeAll();
               add(new ConsolePanel(e));
               validate();
            }
         }
      };
      
      gameThread.start();
   }

   /**
    * @see java.applet.Applet#stop()
    */
   public void stop() {
   }

   /**
    * @see java.applet.Applet#init()
    */
   public void init() {
      removeAll();
      setLayout(new BorderLayout());
      setIgnoreRepaint(true);

      try {
         Game game = (Game) Class.forName(getParameter("game")).newInstance();
         
         container = new Container(game);
         canvas = new ContainerPanel(container);
         displayParent = new Canvas() {
            public final void addNotify() {
               super.addNotify();
               startLWJGL();
            }
            public final void removeNotify() {
               destroyLWJGL();
               super.removeNotify();
            }

         };

         displayParent.setSize(getWidth(), getHeight());
         add(displayParent);
         displayParent.setFocusable(true);
         displayParent.requestFocus();
         displayParent.setIgnoreRepaint(true);
         setVisible(true);
      } catch (Exception e) {
         Log.error(e);
         throw new RuntimeException("Unable to create game container");
      }
   }

   /**
    * Get the GameContainer providing this applet
    *
    * @return The game container providing this applet
    */
   public GameContainer getContainer() {
      return container;
   }

   /**
    * Create a new panel to display the GL context
    *
    * @author kevin
    */
   public class ContainerPanel {
      /** The container being displayed on this canvas */
      private Container container;

      /**
       * Create a new panel
       *
       * @param container The container we're running
       */
      public ContainerPanel(Container container) {
         this.container = container;
      }

      /**
       * Create the LWJGL display
       * 
       * @throws Exception Failure to create display
       */
      private void createDisplay() throws Exception {
         try {
            // create display with alpha
            Display.create(new PixelFormat(8,8,GameContainer.stencil ? 8 : 0));
            alphaSupport = true;
         } catch (Exception e) {
            // if we couldn't get alpha, let us know
            alphaSupport = false;
             Display.destroy();
             // create display without alpha
            Display.create();
         }
      }
      
      /**
       * Start the game container
       * 
       * @throws Exception Failure to create display
       */
      public void start() throws Exception {
         Display.setParent(displayParent);
         Display.setVSyncEnabled(true);
         
         try {
            createDisplay();
         } catch (LWJGLException e) {
            e.printStackTrace();
            // failed to create Display, apply workaround (sleep for 1 second) and try again
            Thread.sleep(1000);
            createDisplay();
         }
         
         initGL();
         displayParent.requestFocus();
         container.runloop();
      }

      /**
       * Initialise GL state
       */
      protected void initGL() {
         try {
            InternalTextureLoader.get().clear();
            SoundStore.get().clear();

            container.initApplet();
         } catch (Exception e) {
            Log.error(e);
            container.stopApplet();
         }
      }
   }

   /**
    * A game container to provide the applet context
    *
    * @author kevin
    */
   public class Container extends GameContainer {
      /**
       * Create a new container wrapped round the game
       *
       * @param game The game to be held in this container
       */
      public Container(Game game) {
         super(game);

         width = AppletGameContainer.this.getWidth();
         height = AppletGameContainer.this.getHeight();
      }

      /**
       * Initiliase based on Applet init
       *
       * @throws SlickException Indicates a failure to inialise the basic framework
       */
      public void initApplet() throws SlickException {
         initSystem();
         enterOrtho();

         try {
            getInput().initControllers();
         } catch (SlickException e) {
            Log.info("Controllers not available");
         } catch (Throwable e) {
            Log.info("Controllers not available");
         }

         game.init(this);
         getDelta();
      }

      /**
       * Check if the applet is currently running
       *
       * @return True if the applet is running
       */
      public boolean isRunning() {
         return running;
      }

      /**
       * Stop the applet play back
       */
      public void stopApplet() {
         running = false;
      }

      /**
       * @see org.newdawn.slick.GameContainer#getScreenHeight()
       */
      public int getScreenHeight() {
         return 0;
      }

      /**
       * @see org.newdawn.slick.GameContainer#getScreenWidth()
       */
      public int getScreenWidth() {
         return 0;
      }
      
      /**
       * Check if the display created supported alpha in the back buffer
       *
       * @return True if the back buffer supported alpha
       */
      public boolean supportsAlphaInBackBuffer() {
         return alphaSupport;
      }
      
      /**
       * @see org.newdawn.slick.GameContainer#hasFocus()
       */
      public boolean hasFocus() {
         return true;
      }
      
      /**
       * Returns the Applet Object
       * @return Applet Object
       */
      public Applet getApplet() {
         return AppletGameContainer.this;
      }
      
      /**
       * @see org.newdawn.slick.GameContainer#setIcon(java.lang.String)
       */
      public void setIcon(String ref) throws SlickException {
         // unsupported in an applet
      }

      /**
       * @see org.newdawn.slick.GameContainer#setMouseGrabbed(boolean)
       */
      public void setMouseGrabbed(boolean grabbed) {
         Mouse.setGrabbed(grabbed);
      }

	  /**
	   * @see org.newdawn.slick.GameContainer#isMouseGrabbed()
	   */
      public boolean isMouseGrabbed() {
    	  return Mouse.isGrabbed();
	  }
      
      /**
		 * @see org.newdawn.slick.GameContainer#setMouseCursor(java.lang.String,
		 *      int, int)
		 */
      public void setMouseCursor(String ref, int hotSpotX, int hotSpotY) throws SlickException {
         try {
            Cursor cursor = CursorLoader.get().getCursor(ref, hotSpotX, hotSpotY);
            Mouse.setNativeCursor(cursor);
         } catch (Throwable e) {
            Log.error("Failed to load and apply cursor.", e);
			throw new SlickException("Failed to set mouse cursor", e);
         }
      }

      /**
       * Get the closest greater power of 2 to the fold number
       * 
       * @param fold The target number
       * @return The power of 2
       */
      private int get2Fold(int fold) {
          int ret = 2;
          while (ret < fold) {
              ret *= 2;
          }
          return ret;
      }
      
      /**
       * {@inheritDoc}
       */
      public void setMouseCursor(Image image, int hotSpotX, int hotSpotY) throws SlickException {
          try {
             Image temp = new Image(get2Fold(image.getWidth()), get2Fold(image.getHeight()));
             Graphics g = temp.getGraphics();
             
             ByteBuffer buffer = BufferUtils.createByteBuffer(temp.getWidth() * temp.getHeight() * 4);
             g.drawImage(image.getFlippedCopy(false, true), 0, 0);
             g.flush();
             g.getArea(0,0,temp.getWidth(),temp.getHeight(),buffer);
             
             Cursor cursor = CursorLoader.get().getCursor(buffer, hotSpotX, hotSpotY,temp.getWidth(),temp.getHeight());
             Mouse.setNativeCursor(cursor);
          } catch (Throwable e) {
             Log.error("Failed to load and apply cursor.", e);
 			 throw new SlickException("Failed to set mouse cursor", e);
          }
       }
      
      /**
       * @see org.newdawn.slick.GameContainer#setIcons(java.lang.String[])
       */
      public void setIcons(String[] refs) throws SlickException {
         // unsupported in an applet
      }

      /**
       * @see org.newdawn.slick.GameContainer#setMouseCursor(org.newdawn.slick.opengl.ImageData, int, int)
       */
      public void setMouseCursor(ImageData data, int hotSpotX, int hotSpotY) throws SlickException {
         try {
            Cursor cursor = CursorLoader.get().getCursor(data, hotSpotX, hotSpotY);
            Mouse.setNativeCursor(cursor);
         } catch (Throwable e) {
            Log.error("Failed to load and apply cursor.", e);
			throw new SlickException("Failed to set mouse cursor", e);
         }
      }

      /**
       * @see org.newdawn.slick.GameContainer#setMouseCursor(org.lwjgl.input.Cursor, int, int)
       */
      public void setMouseCursor(Cursor cursor, int hotSpotX, int hotSpotY) throws SlickException {
         try {
            Mouse.setNativeCursor(cursor);
         } catch (Throwable e) {
            Log.error("Failed to load and apply cursor.", e);
			throw new SlickException("Failed to set mouse cursor", e);
         }
      }

      /**
       * @see org.newdawn.slick.GameContainer#setDefaultMouseCursor()
       */
      public void setDefaultMouseCursor() {
      }

      public boolean isFullscreen() {
         return Display.isFullscreen();
      }

      public void setFullscreen(boolean fullscreen) throws SlickException {
         if (fullscreen == isFullscreen()) {
            return;
         }

         try {
            if (fullscreen) {
               // get current screen resolution
               int screenWidth = Display.getDisplayMode().getWidth();
               int screenHeight = Display.getDisplayMode().getHeight();

               // calculate aspect ratio
               float gameAspectRatio = (float) width / height;
               float screenAspectRatio = (float) screenWidth
                     / screenHeight;

               int newWidth;
               int newHeight;

               // get new screen resolution to match aspect ratio

               if (gameAspectRatio >= screenAspectRatio) {
                  newWidth = screenWidth;
                  newHeight = (int) (height / ((float) width / screenWidth));
               } else {
                  newWidth = (int) (width / ((float) height / screenHeight));
                  newHeight = screenHeight;
               }

               // center new screen
               int xoffset = (screenWidth - newWidth) / 2;
               int yoffset = (screenHeight - newHeight) / 2;

               // scale game to match new resolution
               GL11.glViewport(xoffset, yoffset, newWidth, newHeight);

               enterOrtho();

               // fix input to match new resolution
               this.getInput().setOffset(
                     -xoffset * (float) width / newWidth,
                     -yoffset * (float) height / newHeight);

               this.getInput().setScale((float) width / newWidth,
                     (float) height / newHeight);

               width = screenWidth;
               height = screenHeight;
               Display.setFullscreen(true);
            } else {
               // restore input
               this.getInput().setOffset(0, 0);
               this.getInput().setScale(1, 1);
               width = AppletGameContainer.this.getWidth();
               height = AppletGameContainer.this.getHeight();
               GL11.glViewport(0, 0, width, height);

               enterOrtho();

               Display.setFullscreen(false);
            }
         } catch (LWJGLException e) {
            Log.error(e);
         }

      }

      /**
       * The running game loop
       * 
       * @throws Exception Indicates a failure within the game's loop rather than the framework
       */
      public void runloop() throws Exception {
         while (running) {
            int delta = getDelta();

            updateAndRender(delta);

            updateFPS();
            Display.update();
         }

         Display.destroy();
      }
   }
   
   /**
    * A basic console to display an error message if the applet crashes.
    * This will prevent the applet from just freezing in the browser
    * and give the end user an a nice gui where the error message can easily
    * be viewed and copied.
    */
   public class ConsolePanel extends Panel {
      /** The area display the console output */
      TextArea textArea = new TextArea();
      
      /**
       * Create a new panel to display the console output
       * 
       * @param e The exception causing the console to be displayed
       */
      public ConsolePanel(Exception e) {
         setLayout(new BorderLayout());
         setBackground(Color.black);
         setForeground(Color.white);
         
         Font consoleFont = new Font("Arial", Font.BOLD, 14);
         
         Label slickLabel = new Label("SLICK CONSOLE", Label.CENTER);
         slickLabel.setFont(consoleFont);
         add(slickLabel, BorderLayout.PAGE_START);
         
         StringWriter sw = new StringWriter();
         e.printStackTrace(new PrintWriter(sw));
         
         textArea.setText(sw.toString());
         textArea.setEditable(false);
         add(textArea, BorderLayout.CENTER);
         
         // add a border on both sides of the console
         add(new Panel(), BorderLayout.LINE_START);
         add(new Panel(), BorderLayout.LINE_END);
         
         Panel bottomPanel = new Panel();
         bottomPanel.setLayout(new GridLayout(0, 1));
         Label infoLabel1 = new Label("An error occured while running the applet.", Label.CENTER);
         Label infoLabel2 = new Label("Plese contact support to resolve this issue.", Label.CENTER);
         infoLabel1.setFont(consoleFont);
         infoLabel2.setFont(consoleFont);
         bottomPanel.add(infoLabel1);
         bottomPanel.add(infoLabel2);
         add(bottomPanel, BorderLayout.PAGE_END);
      }
   }
}