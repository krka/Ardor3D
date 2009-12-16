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
import java.util.List;
import java.util.StringTokenizer;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.DefaultJDOMFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.input.SAXHandler;
import org.xml.sax.SAXException;

import com.ardor3d.extension.model.collada.jdom.data.AssetData;
import com.ardor3d.extension.model.collada.jdom.data.ColladaOptions;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.GlobalData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.resource.RelativeResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

public class ColladaImporter {

    /**
     * Constructs a new ColladaImporter using default option values.
     */
    public ColladaImporter() {
        // Set our default options
        this(new ColladaOptions());
    }

    /**
     * Constructs a new ColladaImporter using given option values.
     * 
     * @param options
     *            options to use during import
     */
    public ColladaImporter(final ColladaOptions options) {
        GlobalData.getInstance().setOptions(options);
    }

    /**
     * Reads a Collada file from the given resource and returns it as a ColladaStorage object.
     * 
     * @param resource
     *            the name of the resource to find. ResourceLocatorTool will be used with TYPE_MODEL to find the
     *            resource.
     * @return a ColladaStorage data object containing the Collada scene and other useful elements.
     */
    public ColladaStorage readColladaFile(final String resource) {
        final ResourceSource source;
        if (!GlobalData.getInstance().getOptions().hasModelLocator()) {
            source = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, resource);
        } else {
            source = GlobalData.getInstance().getOptions().getModelLocator().locateResource(resource);
        }

        if (source == null) {
            throw new Error("Unable to locate '" + resource + "'");
        }

        return readColladaFile(source);
    }

    /**
     * Reads a Collada file from the given resource and returns it as a ColladaStorage object.
     * 
     * @param resource
     *            the name of the resource to find.
     * @return a ColladaStorage data object containing the Collada scene and other useful elements.
     */
    public ColladaStorage readColladaFile(final ResourceSource resource) {
        try {
            // Pull in the DOM tree of the Collada resource.
            final Element collada = readCollada(resource);

            // if we don't specify a texture locator, add a temporary texture locator at the location of this model
            // resource..
            final boolean addLocator = !GlobalData.getInstance().getOptions().hasTextureLocator();

            final RelativeResourceLocator loc;
            if (addLocator) {
                loc = new RelativeResourceLocator(resource);
                ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc);
            } else {
                loc = null;
            }

            final AssetData assetData = ColladaNodeUtils.parseAsset(collada.getChild("asset"));

            // Collada may or may not have a scene, so this can return null.
            final Node scene = ColladaNodeUtils.getVisualScene(collada);

            // Pull out our storage
            final ColladaStorage storage = GlobalData.getInstance().getColladaStorage();

            // set our scene into storage
            storage.setScene(scene);

            // set our asset data into storage
            storage.setAssetData(assetData);

            // Drop caches, etc. after import.
            GlobalData.disposeInstance();

            // drop our added locator if needed.
            if (addLocator) {
                ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc);
            }

            // return storage
            return storage;
        } catch (final Exception e) {
            throw new RuntimeException("Unable to load collada resource from URL: " + resource, e);
        }

    }

    /**
     * Reads the whole Collada DOM tree from the given resource and returns its root element. Exceptions may be thrown
     * by underlying tools; these will be wrapped in a RuntimeException and rethrown.
     * 
     * @param resource
     *            the ResourceSource to read the resource from
     * @return the Collada root element
     */
    private Element readCollada(final ResourceSource resource) {
        try {
            final SAXBuilder builder = new SAXBuilder() {
                @Override
                protected SAXHandler createContentHandler() {
                    final SAXHandler contentHandler = new SAXHandler(new ArdorFactory()) {
                        @Override
                        public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
                        // Just kill what's usually done here...
                        }

                    };
                    return contentHandler;
                }
            };
            //            
            // final SAXBuilder builder = new SAXBuilder();
            // builder.setFactory(new ArdorSimpleFactory());

            final Document doc = builder.build(resource.openStream());
            final Element collada = doc.getRootElement();

            // ColladaDOMUtil.stripNamespace(collada);

            return collada;
        } catch (final Exception e) {
            throw new RuntimeException("Unable to load collada resource from source: " + resource, e);
        }
    }

    final class ArdorSimpleFactory extends DefaultJDOMFactory {
        @Override
        public Text text(final String text) {
            return new Text(Text.normalizeString(text));
        }

        @Override
        public void setAttribute(final Element parent, final Attribute a) {
            if ("id".equals(a.getName())) {
                GlobalData.getInstance().getIdCache().put(a.getValue(), parent);
            } else if ("sid".equals(a.getName())) {
                GlobalData.getInstance().getSidCache().put(a.getValue(), parent);
            }

            super.setAttribute(parent, a);
        }
    }

    private enum BufferType {
        None, Float, Double, Int, String, P
    };

    /**
     * A JDOMFactory that normalizes all text (strips extra whitespace etc)
     */
    final class ArdorFactory extends DefaultJDOMFactory {
        private Element currentElement;
        private BufferType bufferType = BufferType.None;
        private int count = 0;
        private final List<String> list = new ArrayList<String>();

        @Override
        public Text text(final String text) {
            switch (bufferType) {
                case Float: {
                    final String normalizedText = Text.normalizeString(text);
                    if (normalizedText.length() == 0) {
                        return new Text("");
                    }
                    final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
                    final float[] floatArray = new float[count];
                    for (int i = 0; i < count; i++) {
                        floatArray[i] = Float.parseFloat(tokenizer.nextToken().replace(",", "."));
                    }

                    GlobalData.getInstance().getFloatArrays().put(currentElement, floatArray);

                    return new Text("");
                }
                case Double: {
                    final String normalizedText = Text.normalizeString(text);
                    if (normalizedText.length() == 0) {
                        return new Text("");
                    }
                    final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
                    final double[] doubleArray = new double[count];
                    for (int i = 0; i < count; i++) {
                        doubleArray[i] = Double.parseDouble(tokenizer.nextToken().replace(",", "."));
                    }

                    GlobalData.getInstance().getDoubleArrays().put(currentElement, doubleArray);

                    return new Text("");
                }
                case Int: {
                    final String normalizedText = Text.normalizeString(text);
                    if (normalizedText.length() == 0) {
                        return new Text("");
                    }
                    final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
                    final int[] intArray = new int[count];
                    int i = 0;
                    while (tokenizer.hasMoreTokens()) {
                        intArray[i++] = Integer.parseInt(tokenizer.nextToken());
                    }

                    GlobalData.getInstance().getIntArrays().put(currentElement, intArray);

                    return new Text("");
                }
                case P: {
                    list.clear();
                    final String normalizedText = Text.normalizeString(text);
                    if (normalizedText.length() == 0) {
                        return new Text("");
                    }
                    final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
                    while (tokenizer.hasMoreTokens()) {
                        list.add(tokenizer.nextToken());
                    }
                    final int listSize = list.size();
                    final int[] intArray = new int[listSize];
                    for (int i = 0; i < listSize; i++) {
                        intArray[i] = Integer.parseInt(list.get(i));
                    }

                    GlobalData.getInstance().getIntArrays().put(currentElement, intArray);

                    return new Text("");
                }
            }
            return new Text(Text.normalizeString(text));
        }

        @Override
        public void setAttribute(final Element parent, final Attribute a) {
            if ("id".equals(a.getName())) {
                GlobalData.getInstance().getIdCache().put(a.getValue(), parent);
            } else if ("sid".equals(a.getName())) {
                GlobalData.getInstance().getSidCache().put(a.getValue(), parent);
            } else if ("count".equals(a.getName())) {
                try {
                    count = a.getIntValue();
                } catch (final DataConversionException e) {
                    e.printStackTrace();
                }
            }

            super.setAttribute(parent, a);
        }

        @Override
        public Element element(final String name, final Namespace namespace) {
            currentElement = super.element(name);
            handleTypes(name);
            return currentElement;
        }

        @Override
        public Element element(final String name, final String prefix, final String uri) {
            currentElement = super.element(name);
            handleTypes(name);
            return currentElement;
        }

        @Override
        public Element element(final String name, final String uri) {
            currentElement = super.element(name);
            handleTypes(name);
            return currentElement;
        }

        @Override
        public Element element(final String name) {
            currentElement = super.element(name);
            handleTypes(name);
            return currentElement;
        }

        private void handleTypes(final String name) {
            if ("float_array".equals(name)) {
                bufferType = BufferType.Float;
            } else if ("double_array".equals(name)) {
                bufferType = BufferType.Double;
            } else if ("int_array".equals(name)) {
                bufferType = BufferType.Int;
            } else if ("p".equals(name)) {
                bufferType = BufferType.P;
            } else {
                bufferType = BufferType.None;
            }
        }

    }
}
