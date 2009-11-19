/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.jdom.xpath.XPath;

import com.ardor3d.extension.model.collada.jdom.data.GlobalData;
import com.ardor3d.math.ColorRGBA;

/**
 * Utility class for finding specific nodes in the collada tree through id/sid or XPath expressions and for parsing
 * arrays and colors.
 */
public class ColladaDOMUtil {
    private static final Logger logger = Logger.getLogger(ColladaDOMUtil.class.getName());

    /**
     * Find element with specific id
     * 
     * @param baseUrl
     *            url specifying target id
     * @return element with specific id or null if not found
     */
    public static Element findTargetWithId(final String baseUrl) {
        return GlobalData.getInstance().getIdCache().get(ColladaDOMUtil.parseUrl(baseUrl));
    }

    /**
     * Find element with specific sid
     * 
     * @param baseUrl
     *            url specifying target sid
     * @return element with specific id or null if not found
     */
    public static Element findTargetWithSid(final String baseUrl) {
        return GlobalData.getInstance().getSidCache().get(ColladaDOMUtil.parseUrl(baseUrl));
    }

    /**
     * Select nodes through an XPath query and return all hits as a List
     * 
     * @param element
     *            root element to start search on
     * @param query
     *            XPath expression
     * @return the list of selected items, which may be of types: {@link Element}, {@link Attribute}, {@link Text},
     *         {@link CDATA}, {@link Comment}, {@link ProcessingInstruction}, Boolean, Double, or String.
     */
    public static List<?> selectNodes(final Element element, final String query) {
        final XPath xPathExpression = ColladaDOMUtil.getXPathExpression(query);

        try {
            return xPathExpression.selectNodes(element);
        } catch (final JDOMException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * Select nodes through an XPath query and returns the first hit
     * 
     * @param element
     *            root element to start search on
     * @param query
     *            XPath expression
     * @return the first selected item, which may be of types: {@link Element}, {@link Attribute}, {@link Text},
     *         {@link CDATA}, {@link Comment}, {@link ProcessingInstruction}, Boolean, Double, String, or
     *         <code>null</code> if no item was selected.
     */
    public static Object selectSingleNode(final Element element, final String query) {
        final XPath xPathExpression = ColladaDOMUtil.getXPathExpression(query);

        try {
            return xPathExpression.selectSingleNode(element);
        } catch (final JDOMException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String parseUrl(String baseUrl) {
        baseUrl = baseUrl.replace("#", "");
        return baseUrl;
    }

    /**
     * Compiles and return an XPath expression. Expressions are cached.
     * 
     * @param query
     *            XPath query to compile
     * @return new XPath expression object
     */
    private static XPath getXPathExpression(final String query) {
        if (GlobalData.getInstance().getxPathExpressions().containsKey(query)) {
            return GlobalData.getInstance().getxPathExpressions().get(query);
        }

        XPath xPathExpression = null;
        try {
            xPathExpression = XPath.newInstance(query);
        } catch (final JDOMException e) {
            e.printStackTrace();
        }

        GlobalData.getInstance().getxPathExpressions().put(query, xPathExpression);

        return xPathExpression;
    }

    /**
     * Parses the text under a node and returns it as a float array.
     * 
     * @param node
     *            node to parse content from
     * @return parsed float array
     */
    public static float[] parseFloatArray(final Element node) {
        if (GlobalData.getInstance().getFloatArrays().containsKey(node)) {
            return GlobalData.getInstance().getFloatArrays().get(node);
        }

        final String content = node.getText();

        final List<String> list = new ArrayList<String>();
        final StringTokenizer tokenizer = new StringTokenizer(content, " ");
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        final int listSize = list.size();
        final float[] floatArray = new float[listSize];
        for (int i = 0; i < listSize; i++) {
            floatArray[i] = Float.parseFloat(list.get(i).replace(",", "."));
        }

        GlobalData.getInstance().getFloatArrays().put(node, floatArray);

        return floatArray;
    }

    /**
     * Parses the text under a node and returns it as a double array.
     * 
     * @param node
     *            node to parse content from
     * @return parsed double array
     */
    public static double[] parseDoubleArray(final Element node) {
        if (GlobalData.getInstance().getDoubleArrays().containsKey(node)) {
            return GlobalData.getInstance().getDoubleArrays().get(node);
        }

        final String content = node.getText();

        final List<String> list = new ArrayList<String>();
        final StringTokenizer tokenizer = new StringTokenizer(content, " ");
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        final int listSize = list.size();
        final double[] doubleArray = new double[listSize];
        for (int i = 0; i < listSize; i++) {
            doubleArray[i] = Double.parseDouble(list.get(i).replace(",", "."));
        }

        GlobalData.getInstance().getDoubleArrays().put(node, doubleArray);

        return doubleArray;
    }

    /**
     * Parses the text under a node and returns it as an int array.
     * 
     * @param node
     *            node to parse content from
     * @return parsed int array
     */
    public static int[] parseIntArray(final Element node) {
        if (GlobalData.getInstance().getIntArrays().containsKey(node)) {
            return GlobalData.getInstance().getIntArrays().get(node);
        }

        final String content = node.getText();

        final List<String> list = new ArrayList<String>();
        final StringTokenizer tokenizer = new StringTokenizer(content, " ");
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        final int listSize = list.size();
        final int[] intArray = new int[listSize];
        for (int i = 0; i < listSize; i++) {
            intArray[i] = Integer.parseInt(list.get(i));
        }

        GlobalData.getInstance().getIntArrays().put(node, intArray);

        return intArray;
    }

    /**
     * Parses the text under a node and returns it as a boolean array.
     * 
     * @param node
     *            node to parse content from
     * @return parsed boolean array
     */
    public static boolean[] parseBooleanArray(final Element node) {
        if (GlobalData.getInstance().getDoubleArrays().containsKey(node)) {
            return GlobalData.getInstance().getBooleanArrays().get(node);
        }

        final String content = node.getText();

        final List<String> list = new ArrayList<String>();
        final StringTokenizer tokenizer = new StringTokenizer(content, " ");
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        final int listSize = list.size();
        final boolean[] booleanArray = new boolean[listSize];
        for (int i = 0; i < listSize; i++) {
            booleanArray[i] = Boolean.parseBoolean(list.get(i));
        }

        GlobalData.getInstance().getBooleanArrays().put(node, booleanArray);

        return booleanArray;
    }

    /**
     * Parses the text under a node and returns it as a string array.
     * 
     * @param node
     *            node to parse content from
     * @return parsed string array
     */
    public static String[] parseStringArray(final Element node) {
        if (GlobalData.getInstance().getStringArrays().containsKey(node)) {
            return GlobalData.getInstance().getStringArrays().get(node);
        }

        final String content = node.getText();

        final List<String> list = new ArrayList<String>();
        final StringTokenizer tokenizer = new StringTokenizer(content, " ");
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        final String[] stringArray = list.toArray(new String[list.size()]);

        GlobalData.getInstance().getStringArrays().put(node, stringArray);

        return stringArray;
    }

    /**
     * Strips the namespace from all nodes in a tree.
     * 
     * @param rootElement
     *            Root of strip operation
     */
    @SuppressWarnings("unchecked")
    public static void stripNamespace(final Element rootElement) {
        rootElement.setNamespace(null);

        final List children = rootElement.getChildren();
        final Iterator i = children.iterator();
        while (i.hasNext()) {
            final Element child = (Element) i.next();
            ColladaDOMUtil.stripNamespace(child);
        }
    }

    /**
     * Parse an int value in an attribute.
     * 
     * @param input
     *            Element containing the attribute
     * @param attributeName
     *            Attribute name to parse a value for
     * @return parsed integer
     */
    public static int getAttributeIntValue(final Element input, final String attributeName, final int defaultVal) {
        final Attribute attribute = input.getAttribute(attributeName);
        if (attribute != null) {
            try {
                return attribute.getIntValue();
            } catch (final DataConversionException e) {
                ColladaDOMUtil.logger.log(Level.WARNING, "Could not parse int value", e);
            }
        }
        return defaultVal;
    }

    /**
     * Convert a Collada color description into an Ardor3D ColorRGBA
     * 
     * @param colorDescription
     *            Collada color description
     * @return Ardor3d ColorRGBA
     */
    public static ColorRGBA getColor(final String colorDescription) {
        if (colorDescription == null) {
            throw new ColladaException("Null color description not allowed", colorDescription);
        }

        final String[] values = GlobalData.getInstance().getPattern().split(colorDescription.replace(",", "."));

        if (values.length < 3 || values.length > 4) {
            throw new ColladaException("Expected color definition of length 3 or 4 - got " + values.length
                    + " for description: " + colorDescription, colorDescription);
        }

        try {
            return new ColorRGBA(Float.parseFloat(values[0]), Float.parseFloat(values[1]), Float.parseFloat(values[2]),
                    values.length == 4 ? Float.parseFloat(values[3]) : 1.0f);
        } catch (final NumberFormatException e) {
            throw new ColladaException("Unable to parse float number", colorDescription, e);
        }
    }
}
