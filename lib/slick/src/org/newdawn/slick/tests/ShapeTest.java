package org.newdawn.slick.tests;

import java.util.ArrayList;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Circle;
import org.newdawn.slick.geom.Ellipse;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.RoundedRectangle;
import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.opengl.renderer.Renderer;

/**
 * A geomertry test
 *
 * @author kevin
 */
public class ShapeTest extends BasicGame {
    /** rectangle to display */
    private Rectangle rect;
    /** rounded rectangle to display */
    private RoundedRectangle roundRect;
    /** ellipse to display */
    private Ellipse ellipse;
    /** circle to display */
    private Circle circle;
    /** polygon to display */
    private Polygon polygon;
    /** list for drawing the shapes*/
    private ArrayList shapes;
    /** track key presses */
    private boolean keys[];
    /** since no modifiers, use this for shifted characters */
    private char lastChar[];
    /** The polgon randomly generated */
    private Polygon randomShape = new Polygon();
    
    /**
     * Create a new test of graphics context rendering
     */
    public ShapeTest() {
        super("Geom Test");
    }
	
	public void createPoly(float x, float y) {
		int size = 20;
		int change = 10;

		randomShape = new Polygon();
		// generate random polygon
		randomShape.addPoint(0 + (int)(Math.random() * change), 0 + (int)(Math.random() * change));
		randomShape.addPoint(size - (int)(Math.random() * change), 0 + (int)(Math.random() * change));
		randomShape.addPoint(size - (int)(Math.random() * change), size - (int)(Math.random() * change));
		randomShape.addPoint(0 + (int)(Math.random() * change), size - (int)(Math.random() * change));
	
		// center polygon
		randomShape.setCenterX(x);
		randomShape.setCenterY(y);
	}
	
    /**
     * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
     */
    public void init(GameContainer container) throws SlickException {
        shapes = new ArrayList();
        rect = new Rectangle(10, 10, 100, 80);
        shapes.add(rect);
        roundRect = new RoundedRectangle(150, 10, 60, 80, 20);
        shapes.add(roundRect);
        ellipse = new Ellipse(350, 40, 50, 30);
        shapes.add(ellipse);
        circle = new Circle(470, 60, 50);
        shapes.add(circle);
        polygon = new Polygon(new float[]{550, 10, 600, 40, 620, 100, 570, 130});
        shapes.add(polygon);
        
        keys = new boolean[256];
        lastChar = new char[256];
        createPoly(200,200);
    }

    /**
     * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
     */
    public void render(GameContainer container, Graphics g) {
        g.setColor(Color.green);
        
        for(int i=0;i<shapes.size();i++) {
            g.fill((Shape)shapes.get(i));
        }
        g.fill(randomShape);
        g.setColor(Color.black);
        g.setAntiAlias(true);
        g.draw(randomShape);
        g.setAntiAlias(false);
        
        g.setColor(Color.white);
        g.drawString("keys", 10, 300);
        g.drawString("wasd - move rectangle", 10, 315);
        g.drawString("WASD - resize rectangle", 10, 330);
        g.drawString("tgfh - move rounded rectangle", 10, 345);
        g.drawString("TGFH - resize rounded rectangle", 10, 360);
        g.drawString("ry - resize corner radius on rounded rectangle", 10, 375);
        g.drawString("ikjl - move ellipse", 10, 390);
        g.drawString("IKJL - resize ellipse", 10, 405);
        g.drawString("Arrows - move circle", 10, 420);
        g.drawString("Page Up/Page Down - resize circle", 10, 435);
        g.drawString("numpad 8546 - move polygon", 10, 450);
    }

    /**
     * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
     */
    public void update(GameContainer container, int delta) {
        
        createPoly(200,200);
        if(keys[Input.KEY_ESCAPE]) {
            System.exit(0);
        }
        if(keys[Input.KEY_W]) {
            if(lastChar[Input.KEY_W] == 'w') {
                rect.setY(rect.getY() - 1);
            }
            else {
                rect.setHeight(rect.getHeight() - 1);
            }
        }
        if(keys[Input.KEY_S]) {
            if(lastChar[Input.KEY_S] == 's') {
                rect.setY(rect.getY() + 1);
            }
            else {
                rect.setHeight(rect.getHeight() + 1);
            }
        }
        if(keys[Input.KEY_A]) {
            if(lastChar[Input.KEY_A] == 'a') {
                rect.setX(rect.getX() - 1);
            }
            else {
                rect.setWidth(rect.getWidth() - 1);
            }
        }
        if(keys[Input.KEY_D]) {
            if(lastChar[Input.KEY_D] == 'd') {
                rect.setX(rect.getX() + 1);
            }
            else {
                rect.setWidth(rect.getWidth() + 1);
            }
        }
        if(keys[Input.KEY_T]) {
            if(lastChar[Input.KEY_T] == 't') {
                roundRect.setY(roundRect.getY() - 1);
            }
            else {
                roundRect.setHeight(roundRect.getHeight() - 1);
            }
        }
        if(keys[Input.KEY_G]) {
            if(lastChar[Input.KEY_G] == 'g') {
                roundRect.setY(roundRect.getY() + 1);
            }
            else {
                roundRect.setHeight(roundRect.getHeight() + 1);
            }
        }
        if(keys[Input.KEY_F]) {
            if(lastChar[Input.KEY_F] == 'f') {
                roundRect.setX(roundRect.getX() - 1);
            }
            else {
                roundRect.setWidth(roundRect.getWidth() - 1);
            }
        }
        if(keys[Input.KEY_H]) {
            if(lastChar[Input.KEY_H] == 'h') {
                roundRect.setX(roundRect.getX() + 1);
            }
            else {
                roundRect.setWidth(roundRect.getWidth() + 1);
            }
        }
        if(keys[Input.KEY_R]) {
            roundRect.setCornerRadius(roundRect.getCornerRadius() - 1);
        }
        if(keys[Input.KEY_Y]) {
            roundRect.setCornerRadius(roundRect.getCornerRadius() + 1);
        }
        if(keys[Input.KEY_I]) {
            if(lastChar[Input.KEY_I] == 'i') {
                ellipse.setCenterY(ellipse.getCenterY() - 1);
            }
            else {
                ellipse.setRadius2(ellipse.getRadius2() - 1);
            }
        }
        if(keys[Input.KEY_K]) {
            if(lastChar[Input.KEY_K] == 'k') {
                ellipse.setCenterY(ellipse.getCenterY() + 1);
            }
            else {
                ellipse.setRadius2(ellipse.getRadius2() + 1);
            }
        }
        if(keys[Input.KEY_J]) {
            if(lastChar[Input.KEY_J] == 'j') {
                ellipse.setCenterX(ellipse.getCenterX() - 1);
            }
            else {
                ellipse.setRadius1(ellipse.getRadius1() - 1);
            }
        }
        if(keys[Input.KEY_L]) {
            if(lastChar[Input.KEY_L] == 'l') {
                ellipse.setCenterX(ellipse.getCenterX() + 1);
            }
            else {
                ellipse.setRadius1(ellipse.getRadius1() + 1);
            }
        }
        if(keys[Input.KEY_UP]) {
            circle.setCenterY(circle.getCenterY() - 1);
        }
        if(keys[Input.KEY_DOWN]) {
            circle.setCenterY(circle.getCenterY() + 1);
        }
        if(keys[Input.KEY_LEFT]) {
            circle.setCenterX(circle.getCenterX() - 1);
        }
        if(keys[Input.KEY_RIGHT]) {
            circle.setCenterX(circle.getCenterX() + 1);
        }
        if(keys[Input.KEY_PRIOR]) {
            circle.setRadius(circle.getRadius() - 1);
        }
        if(keys[Input.KEY_NEXT]) {
            circle.setRadius(circle.getRadius() + 1);
        }
        if(keys[Input.KEY_NUMPAD8]) {
            polygon.setY(polygon.getY() - 1);
        }
        if(keys[Input.KEY_NUMPAD5]) {
            polygon.setY(polygon.getY() + 1);
        }
        if(keys[Input.KEY_NUMPAD4]) {
            polygon.setX(polygon.getX() - 1);
        }
        if(keys[Input.KEY_NUMPAD6]) {
            polygon.setX(polygon.getX() + 1);
        }
    }

    /**
     * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
     */
    public void keyPressed(int key, char c) {
        keys[key] = true;
        lastChar[key] = c;
    }
    
    /**
     * @see org.newdawn.slick.BasicGame#keyReleased(int, char)
     */
    public void keyReleased(int key, char c) {
        keys[key] = false;
    }

    /**
     * Entry point to our test
     * 
     * @param argv The arguments passed to the test
     */
    public static void main(String[] argv) {
        try {
        	Renderer.setRenderer(Renderer.VERTEX_ARRAY_RENDERER);
            AppGameContainer container = new AppGameContainer(new ShapeTest());
            container.setDisplayMode(800,600,false);
            container.start();
        } catch (SlickException e) {
            e.printStackTrace();
        }
    }
}
