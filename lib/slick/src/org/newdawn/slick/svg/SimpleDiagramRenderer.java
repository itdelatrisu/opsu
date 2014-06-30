package org.newdawn.slick.svg;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.geom.ShapeRenderer;
import org.newdawn.slick.geom.TexCoordGenerator;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;

/**
 * A very primtive implementation for rendering a diagram. This simply
 * sticks the shapes on the screen in the right fill and stroke colours
 *
 * @author kevin
 */
public class SimpleDiagramRenderer {
	/** The renderer to use for all GL operations */
	protected static SGL GL = Renderer.get();
	
	/** The diagram to be rendered */
	public Diagram diagram;
	/** The display list representing the diagram */
	public int list = -1;
	
	/**
	 * Create a new simple renderer
	 * 
	 * @param diagram The diagram to be rendered
	 */
	public SimpleDiagramRenderer(Diagram diagram) {
		this.diagram = diagram;
	}
	
	/**
	 * Render the diagram to the given graphics context
	 * 
	 * @param g The graphics context to which we should render the diagram
	 */
	public void render(Graphics g) {
		// last list generation
		if (list == -1) {
			list = GL.glGenLists(1);
			GL.glNewList(list, SGL.GL_COMPILE);
				render(g, diagram);
			GL.glEndList();
		}
		
		GL.glCallList(list);
		
		TextureImpl.bindNone();
	}
	
	/**
	 * Utility method to render a diagram in immediate mode
	 * 
	 * @param g The graphics context to render to
	 * @param diagram The diagram to render
	 */
	public static void render(Graphics g, Diagram diagram) {
		for (int i=0;i<diagram.getFigureCount();i++) {
			Figure figure = diagram.getFigure(i);

			if (figure.getData().isFilled()) {
				if (figure.getData().isColor(NonGeometricData.FILL)) {
					g.setColor(figure.getData().getAsColor(NonGeometricData.FILL));
					g.fill(diagram.getFigure(i).getShape());
					g.setAntiAlias(true);
					g.draw(diagram.getFigure(i).getShape());
					g.setAntiAlias(false);
				}
				
				String fill = figure.getData().getAsReference(NonGeometricData.FILL);
				if (diagram.getPatternDef(fill) != null){
					System.out.println("PATTERN");
				}
				if (diagram.getGradient(fill) != null) {
					Gradient gradient = diagram.getGradient(fill);
					Shape shape = diagram.getFigure(i).getShape();
					TexCoordGenerator fg = null;
					if (gradient.isRadial()) {
						fg = new RadialGradientFill(shape, diagram.getFigure(i).getTransform(), gradient);	
					} else {
						fg = new LinearGradientFill(shape, diagram.getFigure(i).getTransform(), gradient);
					}
					
			        Color.white.bind();
					ShapeRenderer.texture(shape, gradient.getImage(), fg);
				}
			}
			
			if (figure.getData().isStroked()) {
				if (figure.getData().isColor(NonGeometricData.STROKE)) {
					g.setColor(figure.getData().getAsColor(NonGeometricData.STROKE));
					g.setLineWidth(figure.getData().getAsFloat(NonGeometricData.STROKE_WIDTH));
					g.setAntiAlias(true);
					g.draw(diagram.getFigure(i).getShape());
					g.setAntiAlias(false);
					g.resetLineWidth();
				}
			}
	
			// DEBUG VERSION
//			g.setColor(Color.black);
//			g.draw(diagram.getFigure(i).getShape());
//			g.setColor(Color.red);
//			g.fill(diagram.getFigure(i).getShape());
		}
	}
}
