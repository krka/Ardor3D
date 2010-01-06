/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.ui.text;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceSource;

/**
 * Loads a font generated by BMFont (http://www.angelcode.com/products/bmfont/).
 * <ul>
 * <li>Font info file *must* be in XML format.
 * <li>The texture should be saved in 32 bit PNG format - TGA does not appear to work.
 * <li>This class only supports a single page (see BMFont documentation for details on pages)
 * </ul>
 */
public class BMFont {
    private static Logger logger = Logger.getLogger(BMFont.class.getName());

    private final HashMap<Integer, Char> _charMap = new HashMap<Integer, Char>();
    private final HashMap<Integer, HashMap<Integer, Integer>> _kernMap = new HashMap<Integer, HashMap<Integer, Integer>>();

    private String _styleName; // e.g. "Courier-12-bold"
    private final ArrayList<Page> _pages = new ArrayList<Page>();
    private Texture _pageTexture;
    private RenderStateSetter _blendStateSetter = null;
    private RenderStateSetter _alphaStateSetter = null;
    private final boolean _useMipMaps;
    private int _maxCharAdv;
    private Common _common = null;
    private Info _info = null;

    /**
     * Reads an XML BMFont description file and loads corresponding texture. Note that the TGA written by BMFont does
     * not seem to be read properly by the Ardor3D loader. PNG works fine.
     * 
     * @param fileUrl
     *            - the location of the .fnt font file. Can not be null.
     * @param useMipMaps
     *            if true, use trilinear filtering with max anisotropy, else min filter is bilinear. MipMaps result in
     *            blurrier text, but less shimmering.
     * @throws IOException
     *             if there are any problems reading the .fnt file.
     */
    public BMFont(final ResourceSource source, final boolean useMipMaps) throws IOException {
        _useMipMaps = useMipMaps;

        parseFontFile(source);
        initialize(source);
    }

    /** apply default render states to spatial */
    public void applyRenderStatesTo(final Spatial spatial, final boolean useBlend) {
        if (useBlend) {
            if (_blendStateSetter == null) {
                _blendStateSetter = new RenderStateSetter(_pageTexture, true);
            }
            _blendStateSetter.applyTo(spatial);
        } else {
            if (_alphaStateSetter == null) {
                _alphaStateSetter = new RenderStateSetter(_pageTexture, false);
            }
            _alphaStateSetter.applyTo(spatial);
        }
    }

    public String getStyleName() {
        return _styleName;
    }

    public int getSize() {
        return Math.abs(_info.size);
    }

    public int getLineHeight() {
        return _common.lineHeight;
    }

    public int getBaseHeight() {
        return _common.base;
    }

    public int getTextureWidth() {
        return _common.scaleW;
    }

    public int getTextureHeight() {
        return _common.scaleH;
    }

    /**
     * @param chr
     *            ascii character code
     * @return character descriptor for chr. If character is not in the char set, return '?' (if '?' is not in the char
     *         set, return will be null)
     */
    public BMFont.Char getChar(int chr) {
        BMFont.Char retVal = _charMap.get(chr);
        if (retVal == null) {
            chr = '?';
            retVal = _charMap.get(chr);
            if (retVal == null) { // if still null, use the first char
                final Iterator<Char> it = _charMap.values().iterator();
                retVal = it.next();
            }
        }
        return retVal;
    }

    /**
     * @return kerning information for this character pair
     */
    public int getKerning(final int chr, final int nextChr) {
        final HashMap<Integer, Integer> map = _kernMap.get(chr);
        if (map != null) {
            final Integer amt = map.get(nextChr);
            if (amt != null) {
                return amt;
            }
        }
        return 0;
    }

    /**
     * @return the largest xadvance in this char set
     */
    public int getMaxCharAdvance() {
        return _maxCharAdv;
    }

    public int getOutlineWidth() {
        return _info.outline;
    }

    /**
     * Writes the XML for this font out to the OutputStream provided.
     * 
     * @param outputStream
     *            the OutputStream to which the XML for this font will be written to
     * @throws IOException
     *             thrown if there is any problem writing out to the OutputStream
     */
    public void writeXML(final OutputStream outputStream) throws IOException {
        final StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<font>\n");
        xml.append(generateInfoXML());
        xml.append(generateCommonXML());
        xml.append(generatePagesXML());
        xml.append(generateCharsXML());
        xml.append(generateKerningsXML());
        xml.append("</font>");

        // Write out to the output stream now
        outputStream.write(xml.toString().getBytes());
        outputStream.flush();

        return;
    }

    private String generateInfoXML() {
        final StringBuilder xml = new StringBuilder();

        xml.append("  <info face=\"");
        xml.append(_info.face);
        xml.append("\" size=\"");
        xml.append(_info.size);
        xml.append("\" bold=\"");
        xml.append(_info.bold ? "1" : "0");
        xml.append("\" italic=\"");
        xml.append(_info.italic ? "1" : "0");
        xml.append("\" charset=\"");
        xml.append(_info.charset);
        xml.append("\" unicode=\"");
        xml.append(_info.unicode ? "1" : "0");
        xml.append("\" stretchH=\"");
        xml.append(_info.stretchH);
        xml.append("\" smooth=\"");
        xml.append(_info.smooth ? "1" : "0");
        xml.append("\" aa=\"");
        xml.append(_info.aa ? "1" : "0");
        xml.append("\" padding=\"");

        for (int i = 0; i < _info.padding.length; i++) {
            xml.append(_info.padding[i]);

            if (i < (_info.padding.length - 1)) {
                xml.append(",");
            }
        }

        xml.append("\" spacing=\"");

        for (int i = 0; i < _info.spacing.length; i++) {
            xml.append(_info.spacing[i]);

            if (i < (_info.spacing.length - 1)) {
                xml.append(",");
            }
        }

        xml.append("\" outline=\"");
        xml.append(_info.outline);
        xml.append("\"/>\n");

        return xml.toString();
    }

    private String generateCommonXML() {
        final StringBuilder xml = new StringBuilder();

        xml.append("  <common lineHeight=\"");
        xml.append(_common.lineHeight);
        xml.append("\" base=\"");
        xml.append(_common.base);
        xml.append("\" scaleW=\"");
        xml.append(_common.scaleW);
        xml.append("\" scaleH=\"");
        xml.append(_common.scaleH);
        xml.append("\" pages=\"");
        xml.append(_common.pages);
        xml.append("\" packed=\"");
        xml.append(_common.packed ? "1" : "0");
        xml.append("\" alphaChnl=\"");
        xml.append(_common.alphaChnl);
        xml.append("\" redChnl=\"");
        xml.append(_common.redChnl);
        xml.append("\" greenChnl=\"");
        xml.append(_common.greenChnl);
        xml.append("\" blueChnl=\"");
        xml.append(_common.blueChnl);
        xml.append("\"/>\n");

        return xml.toString();
    }

    private String generatePagesXML() {
        final StringBuilder xml = new StringBuilder();

        xml.append("  <pages>\n");

        for (final Iterator<Page> iterator = _pages.iterator(); iterator.hasNext();) {
            final Page page = iterator.next();

            xml.append("    <page id=\"");
            xml.append(page.id);
            xml.append("\" file=\"");
            xml.append(page.file);
            xml.append("\" />\n");
        }

        xml.append("  </pages>\n");

        return xml.toString();
    }

    private String generateCharsXML() {
        final StringBuilder xml = new StringBuilder();

        xml.append("  <chars count=\"");
        xml.append(_charMap.size());
        xml.append("\">\n");

        for (final Iterator<Integer> iterator = _charMap.keySet().iterator(); iterator.hasNext();) {
            final Integer key = iterator.next();
            final Char character = _charMap.get(key);

            xml.append("    <char id=\"");
            xml.append(character.id);
            xml.append("\" x=\"");
            xml.append(character.x);
            xml.append("\" y=\"");
            xml.append(character.y);
            xml.append("\" width=\"");
            xml.append(character.width);
            xml.append("\" height=\"");
            xml.append(character.height);
            xml.append("\" xoffset=\"");
            xml.append(character.xoffset);
            xml.append("\" yoffset=\"");
            xml.append(character.yoffset);
            xml.append("\" xadvance=\"");
            xml.append(character.xadvance);
            xml.append("\" page=\"");
            xml.append(character.page);
            xml.append("\" chnl=\"");
            xml.append(character.chnl);
            xml.append("\" />\n");
        }

        xml.append("  </chars>\n");

        return xml.toString();
    }

    private String generateKerningsXML() {
        final StringBuilder xml = new StringBuilder();
        int count = 0;

        for (final Iterator<Integer> iterator = _kernMap.keySet().iterator(); iterator.hasNext();) {
            final Integer first = iterator.next();
            final HashMap<Integer, Integer> amtHash = _kernMap.get(first);

            for (final Iterator<Integer> iterator2 = amtHash.keySet().iterator(); iterator2.hasNext();) {
                final Integer second = iterator2.next();
                final Integer amount = amtHash.get(second);

                xml.append("    <kerning first=\"");
                xml.append(first);
                xml.append("\" second=\"");
                xml.append(second);
                xml.append("\" amount=\"");
                xml.append(amount);
                xml.append("\" />\n");

                count++;
            }
        }

        final String xmlString = "  <kernings count=\"" + count + "\">\n" + xml.toString() + "  </kernings>\n";

        return xmlString;
    }

    /**
     * load the texture and create default render states. Only a single page is supported.
     * 
     * @param fontUrl
     */
    // ----------------------------------------------------------
    protected void initialize(final ResourceSource source) throws MalformedURLException {
        _styleName = _info.face + "-" + _info.size;

        if (_info.bold) {
            _styleName += "-bold";
        } else {
            _styleName += "-medium";
        }

        if (_info.italic) {
            _styleName += "-italic";
        } else {
            _styleName += "-regular";
        }

        // only a single page is supported
        if (_pages.size() > 0) {
            final Page page = _pages.get(0);

            final ResourceSource texSrc = source.getRelativeSource("./" + page.file);

            Texture.MinificationFilter minFilter;
            Texture.MagnificationFilter magFilter;

            magFilter = Texture.MagnificationFilter.Bilinear;
            minFilter = Texture.MinificationFilter.BilinearNoMipMaps;
            if (_useMipMaps) {
                minFilter = Texture.MinificationFilter.Trilinear;
            }
            final TextureKey tkey = TextureKey.getKey(texSrc, false, Image.Format.GuessNoCompression, minFilter);
            _pageTexture = TextureManager.loadFromKey(tkey, null, null);
            _pageTexture.setMagnificationFilter(magFilter);

            // Add a touch higher mipmap selection.
            _pageTexture.setLodBias(-1);

            if (_useMipMaps) {
                _pageTexture.setAnisotropicFilterPercent(1.0f);
            }
        }
    }

    /**
     * 
     * @param fontUrl
     * @throws IOException
     */
    protected void parseFontFile(final ResourceSource source) throws IOException {
        _maxCharAdv = 0;
        _charMap.clear();
        _pages.clear();
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.parse(source.openStream());

            doc.getDocumentElement().normalize();
            recurse(doc.getFirstChild());

            // db.reset();
        } catch (final Throwable t) {
            final IOException ex = new IOException("Error loading font file " + source.toString());
            ex.initCause(t);
            throw ex;
        }
    }

    private void recurse(final Node node) {
        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            processNode(child);
            recurse(child);
        }
    }

    private void processNode(final Node node) {
        final String tagName = node.getNodeName();
        if (tagName != null) {
            if (tagName.equals("info")) {
                processInfoNode(node);
            } else if (tagName.equals("common")) {
                processCommonNode(node);
            } else if (tagName.equals("page")) {
                processPageNode(node);
            } else if (tagName.equals("char")) {
                processCharNode(node);
            } else if (tagName.equals("kerning")) {
                procesKerningNode(node);
            }
        }
    }

    private void processInfoNode(final Node node) {
        final NamedNodeMap attribs = node.getAttributes();
        _info = new Info();
        _info.face = getStringAttrib("face", attribs);
        _info.size = getIntAttrib("size", attribs);
        _info.bold = getBoolAttrib("bold", attribs);
        _info.italic = getBoolAttrib("italic", attribs);
        _info.charset = getStringAttrib("charset", attribs);
        _info.unicode = getBoolAttrib("unicode", attribs);
        _info.stretchH = getIntAttrib("stretchH", attribs);
        _info.smooth = getBoolAttrib("smooth", attribs);
        _info.aa = getBoolAttrib("aa", attribs);
        _info.padding = getIntArrayAttrib("padding", attribs);
        _info.spacing = getIntArrayAttrib("spacing", attribs);
        _info.outline = getIntAttrib("outline", attribs);
    }

    private void processCommonNode(final Node node) {
        final NamedNodeMap attribs = node.getAttributes();
        _common = new Common();
        _common.lineHeight = getIntAttrib("lineHeight", attribs);
        _common.base = getIntAttrib("base", attribs);
        _common.scaleW = getIntAttrib("scaleW", attribs);
        _common.scaleH = getIntAttrib("scaleH", attribs);
        _common.pages = getIntAttrib("pages", attribs);
        _common.packed = getBoolAttrib("packed", attribs);
        _common.alphaChnl = getIntAttrib("alphaChnl", attribs);
        _common.redChnl = getIntAttrib("redChnl", attribs);
        _common.greenChnl = getIntAttrib("greenChnl", attribs);
        _common.blueChnl = getIntAttrib("blueChnl", attribs);
    }

    private void processCharNode(final Node node) {
        final NamedNodeMap attribs = node.getAttributes();
        final Char c = new Char();
        c.id = getIntAttrib("id", attribs);
        c.x = getIntAttrib("x", attribs);
        c.y = getIntAttrib("y", attribs);
        c.width = getIntAttrib("width", attribs);
        c.height = getIntAttrib("height", attribs);
        c.xoffset = getIntAttrib("xoffset", attribs);
        c.yoffset = getIntAttrib("yoffset", attribs);
        c.xadvance = getIntAttrib("xadvance", attribs);
        c.page = getIntAttrib("page", attribs);
        c.chnl = getIntAttrib("chnl", attribs);
        _charMap.put(c.id, c);
        if (c.xadvance > _maxCharAdv) {
            _maxCharAdv = c.xadvance;
        }
    }

    private void processPageNode(final Node node) {
        final NamedNodeMap attribs = node.getAttributes();
        final Page page = new Page();
        page.id = getIntAttrib("id", attribs);
        page.file = getStringAttrib("file", attribs);
        _pages.add(page);
        if (_pages.size() > 1) {
            logger.warning("multiple pages defined in font description file, but only a single page is supported.");
        }
    }

    private void procesKerningNode(final Node node) {
        final NamedNodeMap attribs = node.getAttributes();
        final int first = getIntAttrib("first", attribs);
        final int second = getIntAttrib("second", attribs);
        final int amount = getIntAttrib("amount", attribs);
        HashMap<Integer, Integer> amtHash;
        amtHash = _kernMap.get(first);
        if (amtHash == null) {
            amtHash = new HashMap<Integer, Integer>();
            _kernMap.put(first, amtHash);
        }
        amtHash.put(second, amount);
    }

    // == xml attribute getters ============================
    int getIntAttrib(final String name, final NamedNodeMap attribs) {
        final Node node = attribs.getNamedItem(name);
        return Integer.parseInt(node.getNodeValue());
    }

    String getStringAttrib(final String name, final NamedNodeMap attribs) {
        final Node node = attribs.getNamedItem(name);
        return node.getNodeValue();
    }

    boolean getBoolAttrib(final String name, final NamedNodeMap attribs) {
        final Node node = attribs.getNamedItem(name);
        return (Integer.parseInt(node.getNodeValue()) == 1);
    }

    int[] getIntArrayAttrib(final String name, final NamedNodeMap attribs) {
        final Node node = attribs.getNamedItem(name);
        final String str = node.getNodeValue();
        final StringTokenizer strtok = new StringTokenizer(str, ",");
        final int sz = strtok.countTokens();
        final int[] retVal = new int[sz];
        for (int i = 0; i < sz; i++) {
            retVal[i] = Integer.parseInt(strtok.nextToken());
        }
        return retVal;
    }

    // == support structs ==================================
    public class Info {
        public String face;
        public int size;
        public boolean bold;
        public boolean italic;
        public String charset;
        public boolean unicode;
        public int stretchH;
        public boolean smooth;
        public boolean aa;
        public int[] padding;
        public int[] spacing;
        public int outline;
    }

    public class Common {
        public int lineHeight;
        public int base;
        public int scaleW;
        public int scaleH;
        public int pages;
        public boolean packed;
        public int alphaChnl;
        public int redChnl;
        public int greenChnl;
        public int blueChnl;
    }

    public class Page {
        public int id;
        public String file;
    }

    public class Char {
        public int id;
        public int x;
        public int y;
        public int width;
        public int height;
        public int xoffset;
        public int yoffset;
        public int xadvance;
        public int page;
        public int chnl;
    }

    /**
     * utility to set default render states for text
     */
    public class RenderStateSetter {
        public TextureState textureState;
        public BlendState blendState;
        public ZBufferState zBuffState;

        float _blendDisabledTestRef = 0.3f;
        float _blendEnabledTestRef = 0.02f;

        boolean _useBlend;

        RenderStateSetter(final Texture texture, final boolean useBlend) {
            textureState = new TextureState();
            textureState.setTexture(texture);

            blendState = new BlendState();
            blendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
            blendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
            blendState.setTestEnabled(true);
            blendState.setTestFunction(BlendState.TestFunction.GreaterThan);

            zBuffState = new ZBufferState();
            zBuffState.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);

            setUseBlend(useBlend);
        }

        void applyTo(final Spatial spatial) {
            spatial.setRenderState(textureState);
            spatial.setRenderState(blendState);
            spatial.setRenderState(zBuffState);
            if (_useBlend) {
                spatial.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
            } else {
                spatial.getSceneHints().setRenderBucketType(RenderBucketType.Opaque);
            }
        }

        void setUseBlend(final boolean blend) {
            _useBlend = blend;
            if (blend == false) {
                blendState.setBlendEnabled(false);
                blendState.setReference(_blendDisabledTestRef);
                zBuffState.setWritable(true);
            } else {
                blendState.setBlendEnabled(true);
                blendState.setReference(_blendEnabledTestRef);
                zBuffState.setWritable(false);
            }
        }
    }
}
