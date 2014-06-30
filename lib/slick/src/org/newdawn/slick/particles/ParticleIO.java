package org.newdawn.slick.particles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.newdawn.slick.Color;
import org.newdawn.slick.geom.Vector2f;
import org.newdawn.slick.particles.ConfigurableEmitter.ColorRecord;
import org.newdawn.slick.particles.ConfigurableEmitter.LinearInterpolator;
import org.newdawn.slick.particles.ConfigurableEmitter.RandomValue;
import org.newdawn.slick.particles.ConfigurableEmitter.SimpleValue;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Utility methods to (de)serialize ConfigureEmitters to and from XML
 * 
 * @author kevin
 */
public class ParticleIO {

	/**
	 * Load a set of configured emitters into a single system
	 * 
	 * @param ref
	 *            The reference to the XML file (file or classpath)
	 * @param mask
	 * @return A configured particle system
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ParticleSystem loadConfiguredSystem(String ref, Color mask)
			throws IOException {
		return loadConfiguredSystem(ResourceLoader.getResourceAsStream(ref),
            null, null, mask);
	}
	
	/**
	 * Load a set of configured emitters into a single system
	 * 
	 * @param ref
	 *            The reference to the XML file (file or classpath)
	 * @return A configured particle system
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ParticleSystem loadConfiguredSystem(String ref)
			throws IOException {
		return loadConfiguredSystem(ResourceLoader.getResourceAsStream(ref),
            null, null, null);
	}

	/**
	 * Load a set of configured emitters into a single system
	 * 
	 * @param ref
	 *            The XML file to read
	 * @return A configured particle system
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ParticleSystem loadConfiguredSystem(File ref)
			throws IOException {
      return loadConfiguredSystem(new FileInputStream(ref), null, null, null);
	}

	/**
	 * Load a set of configured emitters into a single system
	 * 
	 * @param ref
	 *            The stream to read the XML from
	 * @param mask The mask used to make the particle image transparent
	 * @return A configured particle system
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ParticleSystem loadConfiguredSystem(InputStream ref, Color mask)
			throws IOException {
      return loadConfiguredSystem(ref, null, null, mask);
	}
	
	/**
	 * Load a set of configured emitters into a single system
	 * 
	 * @param ref
	 *            The stream to read the XML from
	 * @return A configured particle system
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ParticleSystem loadConfiguredSystem(InputStream ref)
			throws IOException {
      return loadConfiguredSystem(ref, null, null, null);
	}

	/**
	 * Load a set of configured emitters into a single system
	 * 
	 * @param ref
	 *            The reference to the XML file (file or classpath)
	 * @return A configured particle system
	 * @param factory
	 *            The factory used to create the emitter than will be poulated
	 *            with loaded data.
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ParticleSystem loadConfiguredSystem(String ref,
			ConfigurableEmitterFactory factory) throws IOException {
		return loadConfiguredSystem(ResourceLoader.getResourceAsStream(ref),
            factory, null, null);
	}

	/**
	 * Load a set of configured emitters into a single system
	 * 
	 * @param ref
	 *            The XML file to read
	 * @return A configured particle system
	 * @param factory
	 *            The factory used to create the emitter than will be poulated
	 *            with loaded data.
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ParticleSystem loadConfiguredSystem(File ref,
			ConfigurableEmitterFactory factory) throws IOException {
      return loadConfiguredSystem(new FileInputStream(ref), factory, null, null);
	}

	/**
	 * Load a set of configured emitters into a single system
	 * 
	 * @param ref
	 *            The stream to read the XML from
	 * @return A configured particle system
	 * @param factory
	 *            The factory used to create the emitter than will be poulated
	 *            with loaded data.
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ParticleSystem loadConfiguredSystem(InputStream ref,
			ConfigurableEmitterFactory factory) throws IOException {
            return loadConfiguredSystem(ref, factory, null, null);
         }

   /**
    * Load a set of configured emitters into a single system
    *
    * @param ref
    *            The stream to read the XML from
    * @param factory
    *            The factory used to create the emitter than will be poulated
    *            with loaded data.
    * @param system The particle system that will be loaded into
    * @param mask The mask used to make the image background transparent
    * @return A configured particle system
    * @throws IOException
    *             Indicates a failure to find, read or parse the XML file
    */
   public static ParticleSystem loadConfiguredSystem(InputStream ref,
         ConfigurableEmitterFactory factory, ParticleSystem system, Color mask) throws IOException {
		if (factory == null) {
			factory = new ConfigurableEmitterFactory() {
				public ConfigurableEmitter createEmitter(String name) {
					return new ConfigurableEmitter(name);
				}
			};
		}
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document document = builder.parse(ref);

			Element element = document.getDocumentElement();
			if (!element.getNodeName().equals("system")) {
				throw new IOException("Not a particle system file");
			}
			
         if (system == null) {
         system = new ParticleSystem("org/newdawn/slick/data/particle.tga",
					2000, mask);
         }
			boolean additive = "true".equals(element.getAttribute("additive"));
			if (additive) {
				system.setBlendingMode(ParticleSystem.BLEND_ADDITIVE);
			} else {
				system.setBlendingMode(ParticleSystem.BLEND_COMBINE);
			}
			boolean points = "true".equals(element.getAttribute("points"));
			system.setUsePoints(points);

			NodeList list = element.getElementsByTagName("emitter");
			for (int i = 0; i < list.getLength(); i++) {
				Element em = (Element) list.item(i);
				ConfigurableEmitter emitter = factory.createEmitter("new");
				elementToEmitter(em, emitter);

				system.addEmitter(emitter);
			}

			system.setRemoveCompletedEmitters(false);
			return system;
		} catch (IOException e) {
			Log.error(e);
			throw e;
		} catch (Exception e) {
			Log.error(e);
			throw new IOException("Unable to load particle system config");
		}
	}

	/**
	 * Save a particle system with only ConfigurableEmitters in to an XML file
	 * 
	 * @param file
	 *            The file to save to
	 * @param system
	 *            The system to store
	 * @throws IOException
	 *             Indicates a failure to save or encode the system XML.
	 */
	public static void saveConfiguredSystem(File file, ParticleSystem system)
			throws IOException {
		saveConfiguredSystem(new FileOutputStream(file), system);
	}

	/**
	 * Save a particle system with only ConfigurableEmitters in to an XML file
	 * 
	 * @param out
	 *            The location to which we'll save
	 * @param system
	 *            The system to store
	 * @throws IOException
	 *             Indicates a failure to save or encode the system XML.
	 */
	public static void saveConfiguredSystem(OutputStream out,
			ParticleSystem system) throws IOException {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document document = builder.newDocument();

			Element root = document.createElement("system");
			root
					.setAttribute(
							"additive",
							""
									+ (system.getBlendingMode() == ParticleSystem.BLEND_ADDITIVE));
			root.setAttribute("points", "" + (system.usePoints()));

			document.appendChild(root);
			for (int i = 0; i < system.getEmitterCount(); i++) {
				ParticleEmitter current = system.getEmitter(i);
				if (current instanceof ConfigurableEmitter) {
					Element element = emitterToElement(document,
							(ConfigurableEmitter) current);
					root.appendChild(element);
				} else {
					throw new RuntimeException(
							"Only ConfigurableEmitter instances can be stored");
				}
			}

			Result result = new StreamResult(new OutputStreamWriter(out,
					"utf-8"));
			DOMSource source = new DOMSource(document);

			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer xformer = factory.newTransformer();
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");

			xformer.transform(source, result);
		} catch (Exception e) {
			Log.error(e);
			throw new IOException("Unable to save configured particle system");
		}
	}

	/**
	 * Load a single emitter from an XML file
	 * 
	 * @param ref
	 *            The reference to the emitter XML file to load (classpath or
	 *            file)
	 * @return The configured emitter
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ConfigurableEmitter loadEmitter(String ref)
			throws IOException {
		return loadEmitter(ResourceLoader.getResourceAsStream(ref), null);
	}

	/**
	 * Load a single emitter from an XML file
	 * 
	 * @param ref
	 *            The XML file to read
	 * @return The configured emitter
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ConfigurableEmitter loadEmitter(File ref) throws IOException {
		return loadEmitter(new FileInputStream(ref), null);

	}

	/**
	 * Load a single emitter from an XML file
	 * 
	 * @param ref
	 *            The stream to read the XML from
	 * @return The configured emitter
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ConfigurableEmitter loadEmitter(InputStream ref)
			throws IOException {
		return loadEmitter(ref, null);
	}

	/**
	 * Load a single emitter from an XML file
	 * 
	 * @param ref
	 *            The reference to the emitter XML file to load (classpath or
	 *            file)
	 * @return The configured emitter
	 * @param factory
	 *            The factory used to create the emitter than will be poulated
	 *            with loaded data.
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ConfigurableEmitter loadEmitter(String ref,
			ConfigurableEmitterFactory factory) throws IOException {
		return loadEmitter(ResourceLoader.getResourceAsStream(ref), factory);
	}

	/**
	 * Load a single emitter from an XML file
	 * 
	 * @param ref
	 *            The XML file to read
	 * @return The configured emitter
	 * @param factory
	 *            The factory used to create the emitter than will be poulated
	 *            with loaded data.
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ConfigurableEmitter loadEmitter(File ref,
			ConfigurableEmitterFactory factory) throws IOException {
		return loadEmitter(new FileInputStream(ref), factory);

	}

	/**
	 * Load a single emitter from an XML file
	 * 
	 * @param ref
	 *            The stream to read the XML from
	 * @param factory
	 *            The factory used to create the emitter than will be poulated
	 *            with loaded data.
	 * @return The configured emitter
	 * @throws IOException
	 *             Indicates a failure to find, read or parse the XML file
	 */
	public static ConfigurableEmitter loadEmitter(InputStream ref,
			ConfigurableEmitterFactory factory) throws IOException {
		if (factory == null) {
			factory = new ConfigurableEmitterFactory() {
				public ConfigurableEmitter createEmitter(String name) {
					return new ConfigurableEmitter(name);
				}
			};
		}
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document document = builder.parse(ref);

			if (!document.getDocumentElement().getNodeName().equals("emitter")) {
				throw new IOException("Not a particle emitter file");
			}

			ConfigurableEmitter emitter = factory.createEmitter("new");
			elementToEmitter(document.getDocumentElement(), emitter);

			return emitter;
		} catch (IOException e) {
			Log.error(e);
			throw e;
		} catch (Exception e) {
			Log.error(e);
			throw new IOException("Unable to load emitter");
		}
	}

	/**
	 * Save a single emitter to the XML file
	 * 
	 * @param file
	 *            The file to save the emitter to
	 * @param emitter
	 *            The emitter to store to the XML file
	 * @throws IOException
	 *             Indicates a failure to write or encode the XML
	 */
	public static void saveEmitter(File file, ConfigurableEmitter emitter)
			throws IOException {
		saveEmitter(new FileOutputStream(file), emitter);
	}

	/**
	 * Save a single emitter to the XML file
	 * 
	 * @param out
	 *            The location to which we should save
	 * @param emitter
	 *            The emitter to store to the XML file
	 * @throws IOException
	 *             Indicates a failure to write or encode the XML
	 */
	public static void saveEmitter(OutputStream out, ConfigurableEmitter emitter)
			throws IOException {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document document = builder.newDocument();

			document.appendChild(emitterToElement(document, emitter));
			Result result = new StreamResult(new OutputStreamWriter(out,
					"utf-8"));
			DOMSource source = new DOMSource(document);

			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer xformer = factory.newTransformer();
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");

			xformer.transform(source, result);
		} catch (Exception e) {
			Log.error(e);
			throw new IOException("Failed to save emitter");
		}
	}

	/**
	 * Get the first child named as specified from the passed XML element
	 * 
	 * @param element
	 *            The element whose children are interogated
	 * @param name
	 *            The name of the element to retrieve
	 * @return The requested element
	 */
	private static Element getFirstNamedElement(Element element, String name) {
		NodeList list = element.getElementsByTagName(name);
		if (list.getLength() == 0) {
			return null;
		}

		return (Element) list.item(0);
	}

	/**
	 * Convert from an XML element to an configured emitter
	 * 
	 * @param element
	 *            The XML element to convert
	 * @param emitter
	 *            The emitter that will be configured based on the XML
	 */
	private static void elementToEmitter(Element element,
			ConfigurableEmitter emitter) {
		emitter.name = element.getAttribute("name");
		emitter.setImageName(element.getAttribute("imageName"));

		String renderType = element.getAttribute("renderType");
		emitter.usePoints = Particle.INHERIT_POINTS;
		if (renderType.equals("quads")) {
			emitter.usePoints = Particle.USE_QUADS;
		}
		if (renderType.equals("points")) {
			emitter.usePoints = Particle.USE_POINTS;
		}

		String useOriented = element.getAttribute("useOriented");
		if (useOriented != null)
			emitter.useOriented = "true".equals(useOriented);

		String useAdditive = element.getAttribute("useAdditive");
		if (useAdditive != null)
			emitter.useAdditive = "true".equals(useAdditive);

		parseRangeElement(getFirstNamedElement(element, "spawnInterval"),
				emitter.spawnInterval);
		parseRangeElement(getFirstNamedElement(element, "spawnCount"),
				emitter.spawnCount);
		parseRangeElement(getFirstNamedElement(element, "initialLife"),
				emitter.initialLife);
		parseRangeElement(getFirstNamedElement(element, "initialSize"),
				emitter.initialSize);
		parseRangeElement(getFirstNamedElement(element, "xOffset"),
				emitter.xOffset);
		parseRangeElement(getFirstNamedElement(element, "yOffset"),
				emitter.yOffset);
		parseRangeElement(getFirstNamedElement(element, "initialDistance"),
				emitter.initialDistance);
		parseRangeElement(getFirstNamedElement(element, "speed"), emitter.speed);
		parseRangeElement(getFirstNamedElement(element, "length"),
				emitter.length);
		parseRangeElement(getFirstNamedElement(element, "emitCount"),
				emitter.emitCount);

		parseValueElement(getFirstNamedElement(element, "spread"),
				emitter.spread);
		parseValueElement(getFirstNamedElement(element, "angularOffset"),
				emitter.angularOffset);
		parseValueElement(getFirstNamedElement(element, "growthFactor"),
				emitter.growthFactor);
		parseValueElement(getFirstNamedElement(element, "gravityFactor"),
				emitter.gravityFactor);
		parseValueElement(getFirstNamedElement(element, "windFactor"),
				emitter.windFactor);
		parseValueElement(getFirstNamedElement(element, "startAlpha"),
				emitter.startAlpha);
		parseValueElement(getFirstNamedElement(element, "endAlpha"),
				emitter.endAlpha);
		parseValueElement(getFirstNamedElement(element, "alpha"), emitter.alpha);
		parseValueElement(getFirstNamedElement(element, "size"), emitter.size);
		parseValueElement(getFirstNamedElement(element, "velocity"),
				emitter.velocity);
		parseValueElement(getFirstNamedElement(element, "scaleY"),
				emitter.scaleY);

		Element color = getFirstNamedElement(element, "color");
		NodeList steps = color.getElementsByTagName("step");
		emitter.colors.clear();
		for (int i = 0; i < steps.getLength(); i++) {
			Element step = (Element) steps.item(i);
			float offset = Float.parseFloat(step.getAttribute("offset"));
			float r = Float.parseFloat(step.getAttribute("r"));
			float g = Float.parseFloat(step.getAttribute("g"));
			float b = Float.parseFloat(step.getAttribute("b"));

			emitter.addColorPoint(offset, new Color(r, g, b, 1));
		}

		// generate new random play length
		emitter.replay();
	}

	/**
	 * Convert from an emitter to a XML element description
	 * 
	 * @param document
	 *            The document the element will be part of
	 * @param emitter
	 *            The emitter to convert
	 * @return The XML element based on the configured emitter
	 */
	private static Element emitterToElement(Document document,
			ConfigurableEmitter emitter) {
		Element root = document.createElement("emitter");
		root.setAttribute("name", emitter.name);
		root.setAttribute("imageName", emitter.imageName == null ? ""
				: emitter.imageName);
		root
				.setAttribute("useOriented", emitter.useOriented ? "true"
						: "false");
		root
				.setAttribute("useAdditive", emitter.useAdditive ? "true"
						: "false");

		if (emitter.usePoints == Particle.INHERIT_POINTS) {
			root.setAttribute("renderType", "inherit");
		}
		if (emitter.usePoints == Particle.USE_POINTS) {
			root.setAttribute("renderType", "points");
		}
		if (emitter.usePoints == Particle.USE_QUADS) {
			root.setAttribute("renderType", "quads");
		}

		root.appendChild(createRangeElement(document, "spawnInterval",
				emitter.spawnInterval));
		root.appendChild(createRangeElement(document, "spawnCount",
				emitter.spawnCount));
		root.appendChild(createRangeElement(document, "initialLife",
				emitter.initialLife));
		root.appendChild(createRangeElement(document, "initialSize",
				emitter.initialSize));
		root.appendChild(createRangeElement(document, "xOffset",
				emitter.xOffset));
		root.appendChild(createRangeElement(document, "yOffset",
				emitter.yOffset));
		root.appendChild(createRangeElement(document, "initialDistance",
				emitter.initialDistance));
		root.appendChild(createRangeElement(document, "speed", emitter.speed));
		root
				.appendChild(createRangeElement(document, "length",
						emitter.length));
		root.appendChild(createRangeElement(document, "emitCount",
				emitter.emitCount));

		root
				.appendChild(createValueElement(document, "spread",
						emitter.spread));
		root.appendChild(createValueElement(document, "angularOffset",
				emitter.angularOffset));
		root.appendChild(createValueElement(document, "growthFactor",
				emitter.growthFactor));
		root.appendChild(createValueElement(document, "gravityFactor",
				emitter.gravityFactor));
		root.appendChild(createValueElement(document, "windFactor",
				emitter.windFactor));
		root.appendChild(createValueElement(document, "startAlpha",
				emitter.startAlpha));
		root.appendChild(createValueElement(document, "endAlpha",
				emitter.endAlpha));
		root.appendChild(createValueElement(document, "alpha", emitter.alpha));
		root.appendChild(createValueElement(document, "size", emitter.size));
		root.appendChild(createValueElement(document, "velocity",
				emitter.velocity));
		root
				.appendChild(createValueElement(document, "scaleY",
						emitter.scaleY));

		Element color = document.createElement("color");
		ArrayList list = emitter.colors;
		for (int i = 0; i < list.size(); i++) {
			ColorRecord record = (ColorRecord) list.get(i);
			Element step = document.createElement("step");
			step.setAttribute("offset", "" + record.pos);
			step.setAttribute("r", "" + record.col.r);
			step.setAttribute("g", "" + record.col.g);
			step.setAttribute("b", "" + record.col.b);

			color.appendChild(step);
		}

		root.appendChild(color);

		return root;
	}

	/**
	 * Create an XML element based on a configured range
	 * 
	 * @param document
	 *            The document the element will be part of
	 * @param name
	 *            The name to give the new element
	 * @param range
	 *            The configured range
	 * @return A configured XML element on the range
	 */
	private static Element createRangeElement(Document document, String name,
			ConfigurableEmitter.Range range) {
		Element element = document.createElement(name);
		element.setAttribute("min", "" + range.getMin());
		element.setAttribute("max", "" + range.getMax());
		element.setAttribute("enabled", "" + range.isEnabled());

		return element;
	}

	/**
	 * Create an XML element based on a configured value
	 * 
	 * @param document
	 *            The document the element will be part of
	 * @param name
	 *            The name to give the new element
	 * @param value
	 *            The configured value
	 * @return A configure XML element based on the value
	 */
	private static Element createValueElement(Document document, String name,
			ConfigurableEmitter.Value value) {
		Element element = document.createElement(name);

		// void: now writes the value type
		if (value instanceof SimpleValue) {
			element.setAttribute("type", "simple");
			element.setAttribute("value", "" + value.getValue(0));
		} else if (value instanceof RandomValue) {
			element.setAttribute("type", "random");
			element
					.setAttribute("value", ""
							+ ((RandomValue) value).getValue());
		} else if (value instanceof LinearInterpolator) {
			element.setAttribute("type", "linear");
			element.setAttribute("min", ""
					+ ((LinearInterpolator) value).getMin());
			element.setAttribute("max", ""
					+ ((LinearInterpolator) value).getMax());
			element.setAttribute("active", ""
					+ ((LinearInterpolator) value).isActive());

			ArrayList curve = ((LinearInterpolator) value).getCurve();
			for (int i = 0; i < curve.size(); i++) {
				Vector2f point = (Vector2f) curve.get(i);

				Element pointElement = document.createElement("point");
				pointElement.setAttribute("x", "" + point.x);
				pointElement.setAttribute("y", "" + point.y);

				element.appendChild(pointElement);
			}
		} else {
			Log.warn("unkown value type ignored: " + value.getClass());
		}

		return element;
	}

	/**
	 * Parse an XML element into a configured range
	 * 
	 * @param element
	 *            The XML element to parse
	 * @param range
	 *            The range to configure based on the XML
	 */
	private static void parseRangeElement(Element element,
			ConfigurableEmitter.Range range) {
		if (element == null) {
			return;
		}
		range.setMin(Float.parseFloat(element.getAttribute("min")));
		range.setMax(Float.parseFloat(element.getAttribute("max")));
		range.setEnabled("true".equals(element.getAttribute("enabled")));
	}

	/**
	 * Parse an XML element into a configured value
	 * 
	 * @param element
	 *            The XML element to parse
	 * @param value
	 *            The value to configure based on the XML
	 */
	private static void parseValueElement(Element element,
			ConfigurableEmitter.Value value) {
		if (element == null) {
			return;
		}

		String type = element.getAttribute("type");
		String v = element.getAttribute("value");

		if (type == null || type.length() == 0) {
			// support for old style which did not write the type
			if (value instanceof SimpleValue) {
				((SimpleValue) value).setValue(Float.parseFloat(v));
			} else if (value instanceof RandomValue) {
				((RandomValue) value).setValue(Float.parseFloat(v));
			} else {
				Log.warn("problems reading element, skipping: " + element);
			}
		} else {
			// type given: this is the new style
			if (type.equals("simple")) {
				((SimpleValue) value).setValue(Float.parseFloat(v));
			} else if (type.equals("random")) {
				((RandomValue) value).setValue(Float.parseFloat(v));
			} else if (type.equals("linear")) {
				String min = element.getAttribute("min");
				String max = element.getAttribute("max");
				String active = element.getAttribute("active");

				NodeList points = element.getElementsByTagName("point");

				ArrayList curve = new ArrayList();
				for (int i = 0; i < points.getLength(); i++) {
					Element point = (Element) points.item(i);

					float x = Float.parseFloat(point.getAttribute("x"));
					float y = Float.parseFloat(point.getAttribute("y"));

					curve.add(new Vector2f(x, y));
				}

				((LinearInterpolator) value).setCurve(curve);
				((LinearInterpolator) value).setMin(Integer.parseInt(min));
				((LinearInterpolator) value).setMax(Integer.parseInt(max));
				((LinearInterpolator) value).setActive("true".equals(active));
			} else {
				Log.warn("unkown type detected: " + type);
			}
		}
	}
}
