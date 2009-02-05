/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.ui.text;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class BasicText extends Mesh {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(BasicText.class.getName());

    /**
     * A default font contained in the ardor3d library.
     */
    public static final String DEFAULT_FONT = "com/ardor3d/ui/text/defaultfont.tga";

    private StringBuffer _text;
    private ColorRGBA _textColor = new ColorRGBA();

    public BasicText() {}

    /**
     * Creates a texture object that starts with the given text.
     * 
     * @see com.ardor3d.util.TextureManager
     * @param name
     *            the name of the scene element. This is required for identification and comparison purposes.
     * @param text
     *            The text to show.
     */
    public BasicText(final String name, final String text) {
        super(name);
        setCullHint(Spatial.CullHint.Never);
        _text = new StringBuffer(text);
        setRenderBucketType(RenderBucketType.Ortho);
    }

    /**
     * 
     * <code>print</code> sets the text to be rendered on the next render pass.
     * 
     * @param text
     *            the text to display.
     */
    public void print(final String text) {
        _text.replace(0, _text.length(), text);
    }

    /**
     * Sets the text to be rendered on the next render. This function is a more efficient version of print(String).
     * 
     * @param text
     *            The text to display.
     */
    public void print(final StringBuffer text) {
        _text.setLength(0);
        _text.append(text);
    }

    /**
     * 
     * <code>getText</code> retrieves the text string of this <code>Text</code> object.
     * 
     * @return the text string of this object.
     */
    public StringBuffer getText() {
        return _text;
    }

    @Override
    public void draw(final Renderer r) {
        if (!r.isProcessingQueue()) {
            if (r.checkAndAdd(this)) {
                return;
            }
        }

        r.draw(this);
    }

    /**
     * Sets the color of the text.
     * 
     * @param color
     *            Color to set.
     */
    public void setTextColor(final ColorRGBA color) {
        _textColor = color;
    }

    /**
     * Returns the current text color.
     * 
     * @return Current text color.
     */
    public ColorRGBA getTextColor() {
        return _textColor;
    }

    public double getWidth() {
        return 10 * _text.length() * getWorldTransform().getScale().getX();
    }

    public double getHeight() {
        return 16 * getWorldTransform().getScale().getY();
    }

    /**
     * @return a Text with {@link #DEFAULT_FONT} and correct blend state
     * @param name
     *            name of the spatial
     */
    public static BasicText createDefaultTextLabel(final String name) {
        return createDefaultTextLabel(name, "");
    }

    /**
     * @return a Text with {@link #DEFAULT_FONT} and correct blend state
     * @param name
     *            name of the spatial
     */
    public static BasicText createDefaultTextLabel(final String name, final String initialText) {
        final BasicText text = new BasicText(name, initialText);
        text.setCullHint(Spatial.CullHint.Never);
        text.setRenderState(getDefaultFontTextureState());
        text.setRenderState(getFontBlend());
        return text;
    }

    /*
     * @return an blend state for doing alpha transparency
     */
    public static BlendState getFontBlend() {
        final BlendState as1 = new BlendState();
        as1.setBlendEnabled(true);
        as1.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        as1.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        return as1;
    }

    /**
     * texture state for the default font.
     */
    private static TextureState defaultFontTextureState;

    /**
     * Creates the texture state if not created before.
     * 
     * @return texture state for the default font
     */
    public static TextureState getDefaultFontTextureState() {
        if (defaultFontTextureState == null) {
            defaultFontTextureState = new TextureState();
            final URL defaultUrl = BasicText.class.getClassLoader().getResource(DEFAULT_FONT);
            if (defaultUrl == null) {
                logger.warning("Default font not found: " + DEFAULT_FONT);
            }
            defaultFontTextureState.setTexture(TextureManager.load(defaultUrl, Texture.MinificationFilter.Trilinear,
                    Image.Format.GuessNoCompression, true));
            defaultFontTextureState.setEnabled(true);
        }
        return defaultFontTextureState;
    }

    /**
     * Cleans up the default font texture and state for the Text class.
     */
    public static void resetDefaultFontTexture(final Renderer r) {
        if (defaultFontTextureState != null && defaultFontTextureState.getTextureID(0) > 0) {
            r.deleteTextureId(defaultFontTextureState.getTextureID(0));
        }
        defaultFontTextureState = null;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_text.toString(), "textString", "");
        capsule.write(_textColor, "textColor", new ColorRGBA());
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _text = new StringBuffer(capsule.readString("textString", ""));
        _textColor = (ColorRGBA) capsule.readSavable("textColor", new ColorRGBA());

    }
}