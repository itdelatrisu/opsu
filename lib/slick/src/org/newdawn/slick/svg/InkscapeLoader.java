package org.newdawn.slick.svg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Transform;
import org.newdawn.slick.svg.inkscape.DefsProcessor;
import org.newdawn.slick.svg.inkscape.ElementProcessor;
import org.newdawn.slick.svg.inkscape.EllipseProcessor;
import org.newdawn.slick.svg.inkscape.GroupProcessor;
import org.newdawn.slick.svg.inkscape.LineProcessor;
import org.newdawn.slick.svg.inkscape.PathProcessor;
import org.newdawn.slick.svg.inkscape.PolygonProcessor;
import org.newdawn.slick.svg.inkscape.RectProcessor;
import org.newdawn.slick.svg.inkscape.UseProcessor;
import org.newdawn.slick.util.ResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A loader specifically for the SVG that is produced from Inkscape
 * 
 * @author kevin
 */
public class InkscapeLoader implements Loader {
	/**
	 * The number of times to over trigulate to get enough tesselation for
	 * smooth shading
	 */
	public static int RADIAL_TRIANGULATION_LEVEL = 1;

	/** The list of XML element processors */
	private static ArrayList processors = new ArrayList();

	/** The diagram loaded */
	private Diagram diagram;

	static {
		addElementProcessor(new RectProcessor());
		addElementProcessor(new EllipseProcessor());
		addElementProcessor(new PolygonProcessor());
		addElementProcessor(new PathProcessor());
		addElementProcessor(new LineProcessor());
		addElementProcessor(new GroupProcessor());
		addElementProcessor(new DefsProcessor());
		addElementProcessor(new UseProcessor());
	}

	/**
	 * Add an <code>ElementProcessor</code> which will be passed
	 * each element read as the Inkscape SVG document is processed.
	 * 
	 * @param proc The processor to be added
	 */
	public static void addElementProcessor(ElementProcessor proc) {
		processors.add(proc);
	}
	
	/**
	 * Load a SVG document into a diagram
	 * 
	 * @param ref
	 *            The reference in the classpath to load the diagram from
	 * @param offset
	 *            Offset the diagram for the height of the document
	 * @return The diagram loaded
	 * @throws SlickException
	 *             Indicates a failure to process the document
	 */
	public static Diagram load(String ref, boolean offset)
			throws SlickException {
		return load(ResourceLoader.getResourceAsStream(ref), offset);
	}

	/**
	 * Load a SVG document into a diagram
	 * 
	 * @param ref
	 *            The reference in the classpath to load the diagram from
	 * @return The diagram loaded
	 * @throws SlickException
	 *             Indicates a failure to process the document
	 */
	public static Diagram load(String ref) throws SlickException {
		return load(ResourceLoader.getResourceAsStream(ref), false);
	}

	/**
	 * Load a SVG document into a diagram
	 * 
	 * @param offset
	 *            Offset the diagram for the height of the document
	 * @param in
	 *            The input stream from which to read the SVG
	 * @return The diagram loaded
	 * @throws SlickException
	 *             Indicates a failure to process the document
	 */
	public static Diagram load(InputStream in, boolean offset)
			throws SlickException {
		return new InkscapeLoader().loadDiagram(in, offset);
	}

	/**
	 * Private, you should be using the static method
	 */
	private InkscapeLoader() {
	}

	/**
	 * Load a SVG document into a diagram
	 * 
	 * @param in
	 *            The input stream from which to read the SVG
	 * @return The diagram loaded
	 * @throws SlickException
	 *             Indicates a failure to process the document
	 */
	private Diagram loadDiagram(InputStream in) throws SlickException {
		return loadDiagram(in, false);
	}

	/**
	 * Load a SVG document into a diagram
	 * 
	 * @param in
	 *            The input stream from which to read the SVG
	 * @param offset
	 *            Offset the diagram for the height of the document
	 * @return The diagram loaded
	 * @throws SlickException
	 *             Indicates a failure to process the document
	 */
	private Diagram loadDiagram(InputStream in, boolean offset)
			throws SlickException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(true);

			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId,
						String systemId) throws SAXException, IOException {
					return new InputSource(
							new ByteArrayInputStream(new byte[0]));
				}
			});

			Document doc = builder.parse(in);
			Element root = doc.getDocumentElement();

			String widthString = root.getAttribute("width");
			while (Character.isLetter(widthString
					.charAt(widthString.length() - 1))) {
				widthString = widthString.substring(0, widthString.length() - 1);
			}

			String heightString = root.getAttribute("height");
			while (Character.isLetter(heightString
					.charAt(heightString.length() - 1))) {
				heightString = heightString.substring(0,heightString.length() - 1);
			}

			float docWidth = Float.parseFloat(widthString);
			float docHeight = Float.parseFloat(heightString);

			diagram = new Diagram(docWidth, docHeight);
			if (!offset) {
				docHeight = 0;
			}
			loadChildren(root, Transform
					.createTranslateTransform(0, -docHeight));

			return diagram;
		} catch (Exception e) {
			throw new SlickException("Failed to load inkscape document", e);
		}
	}

	/**
	 * @see org.newdawn.slick.svg.Loader#loadChildren(org.w3c.dom.Element,
	 *      org.newdawn.slick.geom.Transform)
	 */
	public void loadChildren(Element element, Transform t)
			throws ParsingException {
		NodeList list = element.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i) instanceof Element) {
				loadElement((Element) list.item(i), t);
			}
		}
	}

	/**
	 * Load a single element into the diagram
	 * 
	 * @param element
	 *            The element ot be loaded
	 * @param t
	 *            The transform to apply to the loaded element from the parent
	 * @throws ParsingException
	 *             Indicates a failure to parse the element
	 */
	private void loadElement(Element element, Transform t)
			throws ParsingException {
		for (int i = 0; i < processors.size(); i++) {
			ElementProcessor processor = (ElementProcessor) processors.get(i);

			if (processor.handles(element)) {
				processor.process(this, element, diagram, t);
			}
		}
	}
}
