package org.newdawn.slick.util.xml;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * Provides a method of parsing XML into an existing data model. This does not
 * provide the same functionality as JAXB or the variety of XML bindings out there. This
 * is a utility to map XML onto an existing data model. The idea being that the design level
 * model should not be driven by the XML schema thats defined. The two arn't always equal
 * and often you end up with a set of class that represent your XML that you then have
 * to traverse to extract into your normal data model.
 * 
 * This utility hopes to take a piece of XML and map it onto a previously designed data
 * model. At the moment it's way to tied to the structure of the XML but this will 
 * hopefully change with time.
 * 
 * XML element names must be mapped to class names. This can be done in two ways either:
 * 
 *   - Specify an explict mapping with addElementMapping()
 *   - Specify the default package name and use the element name as the class name
 * 
 * Each attribute in an element is mapped into a property of the element class, preferably
 * through a set<AttrName> bean method, but alternatively by direct injection into private
 * fields.
 * 
 * Each child element is added to the target class by call the method add() on it with a single
 * parameter of the type generated for the child element.
 * 
 * Classes can optionally implement setXMLElementName(String) and setXMLElementContent(String) to
 * recieve the name and content respectively of the XMLElement they were parsed from. This can 
 * help when mapping two elements to a single class.
 * 
 * To reiterate, I'm not sure this is a good idea yet. It helps me as a utility since I've done
 * this several times in the past but in the general case it may not be perfect. Consider a custom 
 * parser using XMLParser or JAXB (et al) seriously instead.
 * 
 * @author kevin
 *
 */
public class ObjectTreeParser {
	/** The mapping of XML element names to class names */
	private HashMap nameToClass = new HashMap();
	/** The default package where classes will be searched for */
	private String defaultPackage;
	/** The list of elements to ignore */
	private ArrayList ignored = new ArrayList();
	/** The name of the method to add an child object to it's parent */
	private String addMethod = "add";
	
	/**
	 * Create an object tree parser with no default package
	 */
	public ObjectTreeParser() {
	}
	
	/**
	 * Create an object tree parser specifing the default package
	 * where classes will be search for using the XML element name
	 * 
	 * @param defaultPackage The default package to be searched
	 */
	public ObjectTreeParser(String defaultPackage) {
		this.defaultPackage = defaultPackage;
	}
	
	/**
	 * Add a mapping between XML element name and class name
	 * 
	 * @param elementName The name of the XML element
	 * @param elementClass The class to be created for the given element
	 */
	public void addElementMapping(String elementName, Class elementClass) {
		nameToClass.put(elementName, elementClass);
	}
	
	/**
	 * Add a name to the list of elements ignored
	 * 
	 * @param elementName The name to ignore
	 */
	public void addIgnoredElement(String elementName) {
		ignored.add(elementName);
	}
	
	/**
	 * Set the name of the method to use to add child objects to their
	 * parents. This is sometimes useful to not clash with the existing 
	 * data model methods.
	 * 
	 * @param methodName The name of the method to call
	 */
	public void setAddMethodName(String methodName) {
		addMethod = methodName;
	}
	
	/**
	 * Set the default package which will be search for classes by their XML
	 * element name.
	 * 
	 * @param defaultPackage The default package to be searched
	 */
	public void setDefaultPackage(String defaultPackage) {
		this.defaultPackage = defaultPackage;
	}
	
	/**
	 * Parse the XML document located by the slick resource loader using the
	 * reference given.
	 * 
	 * @param ref The reference to the XML document
	 * @return The root element of the newly parse document
	 * @throws SlickXMLException Indicates a failure to parse the XML, most likely the 
	 * XML is malformed in some way.
	 */
	public Object parse(String ref) throws SlickXMLException {
		return parse(ref, ResourceLoader.getResourceAsStream(ref));
	}
	
	/**
	 * Parse the XML document that can be read from the given input stream
	 * 
	 * @param name The name of the document
	 * @param in The input stream from which the document can be read
	 * @return The root element of the newly parse document
	 * @throws SlickXMLException Indicates a failure to parse the XML, most likely the 
	 * XML is malformed in some way.
	 */
	public Object parse(String name, InputStream in) throws SlickXMLException {
		XMLParser parser = new XMLParser();
		XMLElement root = parser.parse(name, in);
		
		return traverse(root);
	}

	/**
	 * Parse the XML document located by the slick resource loader using the
	 * reference given.
	 * 
	 * @param ref The reference to the XML document
	 * @param target The top level object that represents the root node
	 * @return The root element of the newly parse document
	 * @throws SlickXMLException Indicates a failure to parse the XML, most likely the 
	 * XML is malformed in some way.
	 */
	public Object parseOnto(String ref, Object target) throws SlickXMLException {
		return parseOnto(ref, ResourceLoader.getResourceAsStream(ref), target);
	}
	
	/**
	 * Parse the XML document that can be read from the given input stream
	 * 
	 * @param name The name of the document
	 * @param in The input stream from which the document can be read
	 * @param target The top level object that represents the root node
	 * @return The root element of the newly parse document
	 * @throws SlickXMLException Indicates a failure to parse the XML, most likely the 
	 * XML is malformed in some way.
	 */
	public Object parseOnto(String name, InputStream in, Object target) throws SlickXMLException {
		XMLParser parser = new XMLParser();
		XMLElement root = parser.parse(name, in);
		
		return traverse(root, target);
	}
	
	/**
	 * Deterine the name of the class that should be used for a given 
	 * XML element name.
	 * 
	 * @param name The name of the XML element
	 * @return The class to be used or null if none can be found
	 */
	private Class getClassForElementName(String name) {
		Class clazz = (Class) nameToClass.get(name);
		if (clazz != null) {
			return clazz;
		}
		
		if (defaultPackage != null) {
			try {
				return Class.forName(defaultPackage+"."+name);
			} catch (ClassNotFoundException e) {
				// ignore, it's just not there
			}
		}
		
		return null;
	}

	/**
	 * Traverse the XML element specified generating the appropriate object structure
	 * for it and it's children
	 * 
	 * @param current The XML element to process
	 * @return The object created for the given element
	 * @throws SlickXMLException 
	 */
	private Object traverse(XMLElement current) throws SlickXMLException {
		return traverse(current, null);
	}
	
	/**
	 * Traverse the XML element specified generating the appropriate object structure
	 * for it and it's children
	 * 
	 * @param current The XML element to process
	 * @param instance The instance to parse onto, normally null
	 * @return The object created for the given element
	 * @throws SlickXMLException 
	 */
	private Object traverse(XMLElement current, Object instance) throws SlickXMLException {
		String name = current.getName();
		if (ignored.contains(name)) {
			return null;
		}
		
		Class clazz;
		
		if (instance == null) {
			clazz = getClassForElementName(name);
		} else {
			clazz = instance.getClass();
		}
		
		if (clazz == null) {
			throw new SlickXMLException("Unable to map element "+name+" to a class, define the mapping");
		}
		
		try {
			if (instance == null) {
				instance = clazz.newInstance();
				
				Method elementNameMethod = getMethod(clazz, "setXMLElementName", new Class[] {String.class});
				if (elementNameMethod != null) {
					invoke(elementNameMethod, instance, new Object[] {name});
				}
				Method contentMethod = getMethod(clazz, "setXMLElementContent", new Class[] {String.class});
				if (contentMethod != null) {
					invoke(contentMethod, instance, new Object[] {current.getContent()});
				}
			}
			
			String[] attrs = current.getAttributeNames();
			for (int i=0;i<attrs.length;i++) {
				String methodName = "set"+attrs[i];
				
				Method method = findMethod(clazz, methodName);
				
				if (method == null) {
					Field field = findField(clazz, attrs[i]);
					if (field != null) {
						String value = current.getAttribute(attrs[i]);
						Object typedValue = typeValue(value, field.getType());
						setField(field, instance, typedValue);
					} else {
						Log.info("Unable to find property on: "+clazz+" for attribute: "+attrs[i]);
					}
				} else {
					String value = current.getAttribute(attrs[i]);
					Object typedValue = typeValue(value, method.getParameterTypes()[0]);
					invoke(method, instance, new Object[] {typedValue});
				}
			}
			
			XMLElementList children = current.getChildren();
			for (int i=0;i<children.size();i++) {
				XMLElement element = children.get(i);
				
				Object child = traverse(element);
				if (child != null) {
					String methodName = addMethod;
					
					Method method = findMethod(clazz, methodName, child.getClass());
					if (method == null) {
						Log.info("Unable to find method to add: "+child+" to "+clazz);
					} else {
						invoke(method, instance, new Object[] {child});
					}
				}
			}
			
			return instance;
		} catch (InstantiationException e) {
			throw new SlickXMLException("Unable to instance "+clazz+" for element "+name+", no zero parameter constructor?", e);
		} catch (IllegalAccessException e) {
			throw new SlickXMLException("Unable to instance "+clazz+" for element "+name+", no zero parameter constructor?", e);
		}
	}
	
	/**
	 * Convert a given value to a given type
	 * 
	 * @param value The value to convert
	 * @param clazz The class that the returned object must be
	 * @return The value as the given type
	 * @throws SlickXMLException Indicates there is no automatic way of converting the value to the type
	 */
	private Object typeValue(String value, Class clazz) throws SlickXMLException {
		if (clazz == String.class) {
			return value;
		}

		try {
			clazz = mapPrimitive(clazz);
			return clazz.getConstructor(new Class[] {String.class}).newInstance(new Object[] {value});
		} catch (Exception e) {
			throw new SlickXMLException("Failed to convert: "+value+" to the expected primitive type: "+clazz, e);
		}
	}
	
	/**
	 * Map a primitive class type to it's real object wrapper
	 * 
	 * @param clazz The primitive type class
	 * @return The object wrapper class
	 */
	private Class mapPrimitive(Class clazz) {
		if (clazz == Integer.TYPE) {
			return Integer.class;
		}
		if (clazz == Double.TYPE) {
			return Double.class;
		}
		if (clazz == Float.TYPE) {
			return Float.class;
		}
		if (clazz == Boolean.TYPE) {
			return Boolean.class;
		}
		if (clazz == Long.TYPE) {
			return Long.class;
		}
		
		throw new RuntimeException("Unsupported primitive: "+clazz);
	}
	
	/**
	 * Find a field in a class by it's name. Note that this method is 
	 * only needed because the general reflection method is case
	 * sensitive
	 * 
	 * @param clazz The clazz to search
	 * @param name The name of the field to search for
	 * @return The field or null if none could be located
	 */
	private Field findField(Class clazz, String name) {
		Field[] fields = clazz.getDeclaredFields();
		for (int i=0;i<fields.length;i++) {
			if (fields[i].getName().equalsIgnoreCase(name)) {
				if (fields[i].getType().isPrimitive()) {
					return fields[i];
				}
				if (fields[i].getType() == String.class) {
					return fields[i];
				}
			}
		}
		
		return null;
	}

	/**
	 * Find a method in a class by it's name. Note that this method is 
	 * only needed because the general reflection method is case
	 * sensitive
	 * 
	 * @param clazz The clazz to search
	 * @param name The name of the method to search for
	 * @return The method or null if none could be located
	 */
	private Method findMethod(Class clazz, String name) {
		Method[] methods = clazz.getDeclaredMethods();
		for (int i=0;i<methods.length;i++) {
			if (methods[i].getName().equalsIgnoreCase(name)) {
				Method method = methods[i];
				Class[] params = method.getParameterTypes();
				
				if (params.length == 1) {
					return method;
				}
			}
		}
		
		return null;
	}

	/**
	 * Find a method on a class with a single given parameter.
	 * 
	 * @param clazz The clazz to search through
	 * @param name The name of the method to locate
	 * @param parameter The type the single parameter must have
	 * @return The method or null if none could be located
	 */
	private Method findMethod(Class clazz, String name, Class parameter) {
		Method[] methods = clazz.getDeclaredMethods();
		for (int i=0;i<methods.length;i++) {
			if (methods[i].getName().equalsIgnoreCase(name)) {
				Method method = methods[i];
				Class[] params = method.getParameterTypes();
				
				if (params.length == 1) {
					if (method.getParameterTypes()[0].isAssignableFrom(parameter)) {
						return method;
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Set a field value on a object instance
	 * 
	 * @param field The field to be set
	 * @param instance The instance of the object to set it on
	 * @param value The value to set 
	 * @throws SlickXMLException Indicates a failure to set or access the field
	 */
	private void setField(Field field, Object instance, Object value) throws SlickXMLException {
		try {
			field.setAccessible(true);
			field.set(instance, value);
		} catch (IllegalArgumentException e) {
			throw new SlickXMLException("Failed to set: "+field+" for an XML attribute, is it valid?", e);
		} catch (IllegalAccessException e) {
			throw new SlickXMLException("Failed to set: "+field+" for an XML attribute, is it valid?", e);
		} finally {
			field.setAccessible(false);
		}
	}
	
	/**
	 * Call a method on a object 
	 *  
	 * @param method The method to call
	 * @param instance The objet to call the method on
	 * @param params The parameters to pass
	 * @throws SlickXMLException Indicates a failure to call or access the method
	 */
	private void invoke(Method method, Object instance, Object[] params) throws SlickXMLException {
		try {
			method.setAccessible(true);
			method.invoke(instance, params);
		} catch (IllegalArgumentException e) {
			throw new SlickXMLException("Failed to invoke: "+method+" for an XML attribute, is it valid?", e);
		} catch (IllegalAccessException e) {
			throw new SlickXMLException("Failed to invoke: "+method+" for an XML attribute, is it valid?", e);
		} catch (InvocationTargetException e) {
			throw new SlickXMLException("Failed to invoke: "+method+" for an XML attribute, is it valid?", e);
		} finally {
			method.setAccessible(false);
		}
	}
	
	/**
	 * Get a method on a given class. Only here for tidy purposes, 
	 * hides the the big exceptions.
	 * 
	 * @param clazz The class to search
	 * @param name The name of the method
	 * @param params The parameters for the method
	 * @return The method or null if none can be found
	 */
	private Method getMethod(Class clazz, String name, Class[] params) {
		try {
			return clazz.getMethod(name, params);
		} catch (SecurityException e) {
			return null;
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
}
