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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Text spatial which uses textures generated by BMFont
 */
public class BMText extends Mesh {
    protected BMFont _font;

    protected String _textString;
    private final int _tabSize = 4;

    protected double _fontScale = 1.0;
    protected boolean _autoRotate = true;

    protected final Vector2 _size = new Vector2(); // width and height of text string
    protected float[] _lineWidths = new float[64]; // size of each line of text

    protected ColorRGBA _textClr = new ColorRGBA(1, 1, 1, 1);
    protected ColorRGBA _tempClr = new ColorRGBA(1, 1, 1, 1);

    public enum AutoScale {
        /**
         * No auto scaling
         */
        Off,

        /**
         * Maintain native point size of font regardless of distance from camera
         */
        FixedScreenSize,

        /**
         * Do not auto scale if font screen size is smaller than native point size, otherwise maintain native point
         * size.
         */
        CapScreenSize;
    }

    protected AutoScale _autoScale = AutoScale.CapScreenSize;

    /**
     * @see setAutoFadeDistanceRange()
     * @see setAutoFadeFixedPixelSize()
     * @see setAutoFadeFalloff()
     */
    public enum AutoFade {
        /**
         * No auto fade.
         */
        Off,

        /**
         * Fade based on a fixed distance between text and camera.
         */
        DistanceRange,

        /**
         * Fade when screen size is less than fixed pixel size.
         */
        FixedPixelSize,

        /**
         * Fade when screen size is less than native size. Equivalent to FixedPixelSize +
         * setAutoFadeFixedPixelSize(font.getSize()).
         */
        CapScreenSize;
    }

    protected AutoFade _autoFade = AutoFade.FixedPixelSize;
    protected int _fixedPixelAlphaThresh = 14;
    protected float _screenSizeAlphaFalloff = 0.7f; // 0=instant, 1=half size
    protected final Vector2 _distanceAlphaRange = new Vector2(50, 75);
    protected boolean _useBlend;

    /**
     * Justification within a text block
     */
    public enum Justify {
        Left, Center, Right;
    }

    protected Justify _justify;
    protected int _spacing = 0; // additional spacing between characters

    /**
     * Alignment of the text block from the pivot point
     */
    public enum Align {
        North(-0.5f, 0.0f), NorthWest(0.0f, 0.0f), NorthEast(-1.0f, 0.0f), Center(-0.5f, -0.5f), West(0.0f, -0.5f), East(
                -1.0f, -0.5f), South(-0.5f, -1.0f), SouthWest(0.0f, -1.0f), SouthEast(-1.0f, -1.0f);
        public final float horizontal;
        public final float vertical;

        private Align(final float h, final float v) {
            horizontal = h;
            vertical = v;
        }
    }

    protected Align _align;
    protected final Vector2 _alignOffset = new Vector2();
    protected final Vector2 _fixedOffset = new Vector2();

    protected FloatBuffer _vertexBuffer = null;
    protected FloatBuffer _texCrdBuffer = null;
    protected IntBuffer _indexBuffer = null;
    protected final Vector3 _look = new Vector3();
    protected final Vector3 _left = new Vector3();
    protected final Matrix3 _rot = new Matrix3();

    /**
     * 
     * @param sName
     * @param text
     * @param font
     */
    public BMText(final String sName, final String text, final BMFont font) {
        this(sName, text, font, Align.SouthWest);
    }

    public BMText(final String sName, final String text, final BMFont font, final Align align) {
        this(sName, text, font, align, Justify.Left);
    }

    public BMText(final String sName, final String text, final BMFont font, final Align align, final Justify justify) {
        this(sName, text, font, align, justify, true);
    }

    /**
     * 
     * @param sName
     *            spatial name
     * @param text
     *            text to render.
     * @param font
     * @param align
     * @param justify
     * @param useBlend
     *            if true: use alpha blending and use transparent render bucket, else if false: alpha test only and use
     *            opaque render bucket
     */
    public BMText(final String sName, final String text, final BMFont font, final Align align, final Justify justify,
            final boolean useBlend) {
        super(sName);
        _font = font;
        _align = align;
        _justify = justify;
        _spacing = 0;
        _useBlend = useBlend;
        if (_font.getOutlineWidth() > 1) {
            _spacing = _font.getOutlineWidth() - 1;
        }
        getMeshData().setIndexMode(IndexMode.Quads);
        getSceneHints().setLightCombineMode(LightCombineMode.Off);
        getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);

        setText(text);

        // -- never cull
        setModelBound(null);
        getSceneHints().setCullHint(CullHint.Never);

        // -- default to non-pickable
        getSceneHints().setAllPickingHints(false);

        _font.applyRenderStatesTo(this, useBlend);
    }

    public void setTextColor(final ReadOnlyColorRGBA clr) {
        _textClr.set(clr);
        setDefaultColor(_textClr);
    }

    public void setTextColor(final float r, final float g, final float b, final float a) {
        _textClr.set(r, g, b, a);
        setDefaultColor(_textClr);
    }

    /**
     * If AutoScale is enabled, this scale parameter acts as a bias. Setting the scale to 0.95 will sharpen the font and
     * increase readability a bit if you're using a bilinear min filter on the texture. When AutoScale is disabled, this
     * scales the font to world units, e.g. setScale(1) would make the font characters approximately 1 world unit in
     * size, regardless of the font point size.
     */
    public void setFontScale(final double scale) {
        _fontScale = scale;

        if (_autoScale == AutoScale.Off) {
            final double unit = 1.0 / _font.getSize();
            final double s = unit * _fontScale;
            this.setScale(s, s, -s);
        }
    }

    public double getFontScale() {
        return _fontScale;
    }

    /**
     * Set scaling policy
     */
    public void setAutoScale(final AutoScale autoScale) {
        _autoScale = autoScale;
        setFontScale(_fontScale);
    }

    public AutoScale getAutoScale() {
        return _autoScale;
    }

    public void setAutoFade(final AutoFade autoFade) {
        _autoFade = autoFade;
    }

    public AutoFade getAutoFade() {
        return _autoFade;
    }

    public void setAutoFadeFixedPixelSize(final int pixelSize) {
        _fixedPixelAlphaThresh = pixelSize;
    }

    public int getAutoFadeFixedPixelSize() {
        return _fixedPixelAlphaThresh;
    }

    /**
     * alpha falloff factor used when FixedPixelSize or CapScreenSize is used. Can be any positive value; useful range
     * is ~ 0-2
     * <ul>
     * <li>0 = transparent instantaneously
     * <li>1 = transparent when approximately 1/2 size
     * </ul>
     */
    public void setAutoFadeFalloff(final float factor) {
        _screenSizeAlphaFalloff = factor;
    }

    /**
     * @param nearOpaque
     *            text is completely opaque when distance between camera and text is less than this value
     * @param farTransparent
     *            text is completely transparent when distance between camera and text is greater than this value
     */
    public void setAutoFadeDistanceRange(final double nearOpaque, final double farTransparent) {
        _distanceAlphaRange.set(nearOpaque, farTransparent);
    }

    /**
     * automatically rotate test to face the camera
     */
    public void setAutoRotate(final boolean doAutoTransform) {
        _autoRotate = doAutoTransform;
    }

    public boolean getAutoRotate() {
        return _autoRotate;
    }

    @Override
    // -----------------------------------------------------
    public synchronized void draw(final Renderer r) {
        if (_textString.length() > 0) {
            final Camera cam = Camera.getCurrentCamera();

            if (!(_autoScale == AutoScale.Off && _autoFade == AutoFade.Off)) {
                updateScaleAndAlpha(cam, r);
            }
            correctTransform(cam);

            updateWorldBound(false);

            super.draw(r);
        }
    }

    /**
     * 
     * @param cam
     */
    // -----------------------------------------------------
    public void correctTransform(final Camera cam) {
        updateWorldTransform(false);

        if (_autoRotate) {
            // Billboard rotation
            _look.set(cam.getDirection());
            _left.set(cam.getLeft()).negateLocal();
            _rot.fromAxes(_left, _look, cam.getUp());
            _worldTransform.setRotation(_rot);
        }
        _worldTransform.setScale(_localTransform.getScale());
    }

    /**
     * Update the text's scale
     * 
     * @param cam
     */
    // -----------------------------------------------------
    public void updateScaleAndAlpha(final Camera cam, final Renderer r) {
        // get our depth distance
        _look.set(cam.getLocation());
        _look.negateLocal().addLocal(_worldTransform.getTranslation());

        final double zDepth = cam.getDirection().dot(_look);
        if (zDepth > cam.getFrustumFar() || zDepth < cam.getFrustumNear()) {
            // it is out of the picture.
            return;
        }

        // calculate the height in world units of the screen at that depth
        final double heightAtZ;
        if (cam.isParallelProjection()) {
            heightAtZ = cam.getFrustumBottom();
        } else {
            heightAtZ = zDepth * cam.getFrustumTop() / cam.getFrustumNear();
        }

        // determine a unit/pixel ratio using height
        final double screenHeight = cam.getHeight();
        final double pixelRatio = heightAtZ / screenHeight;

        final double capSize = 1.0 / (_fontScale * _font.getSize());

        // scale value used to maintain uniform size in screen coords.
        // when depthScale > unitFont, text is far away
        final double depthScale = 2 * pixelRatio;

        if (_autoScale != AutoScale.Off) {
            double finalScale = depthScale;
            if (_autoScale == AutoScale.CapScreenSize) {
                if (finalScale > capSize) {
                    finalScale = capSize;
                }
            }
            finalScale *= _fontScale;
            setScale(finalScale, finalScale, -finalScale);
        }

        // -- adjust alpha -------
        switch (_autoFade) {
            case Off:
                break;
            case DistanceRange:
                distanceAlphaFade(_distanceAlphaRange, _look.length());
                break;
            case FixedPixelSize:
                screenSizeCapAlphaFade(1.0 / _fixedPixelAlphaThresh, depthScale, _screenSizeAlphaFalloff);
                break;
            case CapScreenSize:
                screenSizeCapAlphaFade(capSize, depthScale, _screenSizeAlphaFalloff);
                break;
        }
    }

    /**
     * Set transparency based on native screen size.
     * 
     * @param capSize
     *            1/(font point size)
     * @param depthScale
     * @param alphaFallof
     */
    // -----------------------------------------------------
    protected void screenSizeCapAlphaFade(final double capSize, final double depthScale, final float alphaFallof) {
        if (capSize < depthScale) {
            final float unit = (float) ((depthScale - capSize) / capSize);
            float f = alphaFallof - unit;
            f = (f < 0) ? 0 : f / alphaFallof;
            final float alpha = _textClr.getAlpha() * f;
            _tempClr.set(_textClr);
            _tempClr.setAlpha(alpha);
            setDefaultColor(_tempClr);
        } else {
            setDefaultColor(_textClr);
        }
    }

    /**
     * Set transparency based on distance from camera to text. if (distance < range.x) then opaque, if (distance >
     * range.y) then transparent, else lerp
     */
    // -----------------------------------------------------
    protected void distanceAlphaFade(final ReadOnlyVector2 range, final double distance) {
        float alpha = 1;
        if (distance > range.getY()) {
            alpha = 0;
        } else if (distance > range.getX()) {
            final float a = (float) (distance - range.getX());
            final float r = (float) (range.getY() - range.getX());
            alpha = 1.0f - a / r;
        }
        _tempClr.set(_textClr);
        _tempClr.setAlpha(_textClr.getAlpha() * alpha);
        setDefaultColor(_tempClr);
    }

    /** get width in world units */
    public float getWidth() {
        return (_size.getXf() * _worldTransform.getScale().getXf());
    }

    /** get height in world units */
    public float getHeight() {
        return (_size.getYf() * _worldTransform.getScale().getYf());
    }

    protected void addToLineSizes(final float sizeX, final int lineIndex) {
        if (lineIndex >= _lineWidths.length) { // make sure array is big enough
            final float[] newLineSizes = new float[_lineWidths.length * 2];
            System.arraycopy(_lineWidths, 0, newLineSizes, 0, _lineWidths.length);
            _lineWidths = newLineSizes;
        }
        _lineWidths[lineIndex] = sizeX;
    }

    /**
     */
    // -----------------------------------------------------
    protected void calculateSize(final String text) {
        _size.set(0, 0);

        BMFont.Char chr;
        float cursorX = 0;
        float cursorY = 0;
        final float lineHeight = _font.getLineHeight();
        int lineIndex = 0;

        _lineWidths[0] = 0;
        final int strLen = _textString.length();
        for (int i = 0; i < strLen; i++) {
            final int charVal = _textString.charAt(i);
            if (charVal == '\n') { // newline special case

                addToLineSizes(cursorX, lineIndex);
                lineIndex++;
                if (cursorX > _size.getX()) {
                    _size.setX(cursorX);
                }
                cursorX = 0;
                cursorY = lineIndex * lineHeight;
            } else if (charVal == '\t') { // tab special case
                final float tabStop = _tabSize * _font.getMaxCharAdvance();
                final float stops = 1 + (float) Math.floor(cursorX / tabStop);
                cursorX = stops * tabStop;
            } else { // normal character
                chr = _font.getChar(charVal);
                int nextVal = 0;
                if (i < strLen - 1) {
                    nextVal = _textString.charAt(i + 1);
                }
                final int kern = _font.getKerning(charVal, nextVal);
                cursorX += chr.xadvance + kern + _spacing;
            }
        }
        addToLineSizes(cursorX, lineIndex);
        if (cursorX > _size.getX()) {
            _size.setX(cursorX);
        }
        _size.setY(cursorY + lineHeight);
    }

    /**
     */
    protected void calculateAlignmentOffset() {
        _alignOffset.set(0, 0);
        if (_align != null) {
            _alignOffset.setX(_size.getX() * _align.horizontal);
            _alignOffset.setY(_size.getY() * _align.vertical);
        }
    }

    /**
     * Check whether buffers have sufficient capacity to hold current string values; if not, increase capacity and set
     * the limit.
     * 
     * @param text
     */
    protected void checkBuffers(final String text) {
        final int chunkSize = 20;
        final int vertices = 4 * text.length();
        final int chunks = 1 + (vertices / chunkSize);
        final int required = chunks * chunkSize;
        if (_indexBuffer == null || _indexBuffer.capacity() < required) {
            _vertexBuffer = BufferUtils.createVector3Buffer(required);
            _texCrdBuffer = BufferUtils.createVector2Buffer(required);
            _indexBuffer = BufferUtils.createIntBuffer(required);
        }
        _vertexBuffer.limit(vertices * 3).rewind();
        _texCrdBuffer.limit(vertices * 2).rewind();
        _indexBuffer.limit(vertices).rewind();
    }

    protected float getJustificationXOffset(final int lineIndex) {
        float cursorX = 0;
        switch (_justify) {
            case Left:
                cursorX = 0;
                break;
            case Center:
                cursorX = 0.5f * (_size.getXf() - _lineWidths[lineIndex]);
                break;
            case Right:
                cursorX = _size.getXf() - _lineWidths[lineIndex];
                break;
        }
        return cursorX;
    }

    public BMFont getFont() {
        return _font;
    }

    public void setFont(final BMFont font) {
        _font = font;
        _font.applyRenderStatesTo(this, _useBlend);
        setFontScale(_fontScale);
        setText(_textString);
    }

    /**
     * @param useBlend
     *            if true: use alpha blending and use transparent render bucket, else if false: alpha test only and use
     *            opaque render bucket
     */
    public void setUseBlend(final boolean useBlend) {
        _useBlend = useBlend;
        _font.applyRenderStatesTo(this, _useBlend);
    }

    public boolean getUseBlend() {
        return _useBlend;
    }

    /**
     * Set text string and recreate geometry
     */
    // -----------------------------------------------------
    public synchronized void setText(final String text) {
        if (text == null) {
            _textString = "";
        } else {
            _textString = text;
        }

        checkBuffers(_textString);
        calculateSize(_textString);
        calculateAlignmentOffset();

        final FloatBuffer vertices = _vertexBuffer;
        final FloatBuffer texCrds = _texCrdBuffer;
        final IntBuffer indices = _indexBuffer;

        BMFont.Char chr;
        final float txW = _font.getTextureWidth();
        final float txH = _font.getTextureHeight();

        int lineIndex = 0;
        float cursorX = getJustificationXOffset(lineIndex);
        float cursorY = 0;
        final float lineHeight = _font.getLineHeight();
        float t, b, l, r;

        float alignX = _size.getXf() * _align.horizontal;
        float alignY = _size.getYf() * _align.vertical;
        alignX += _fixedOffset.getX();
        alignY += _fixedOffset.getY();

        final int strLen = _textString.length();
        for (int i = 0; i < strLen; i++) {
            final int charVal = _textString.charAt(i);

            if (charVal == '\n') { // newline special case
                lineIndex++;
                cursorX = getJustificationXOffset(lineIndex);
                cursorY += lineHeight;
                addEmptyCharacter(i, vertices, texCrds, indices);
            } else if (charVal == '\t') { // tab special case
                final float tabStop = _tabSize * _font.getMaxCharAdvance();
                final float stops = 1 + (float) Math.floor(cursorX / tabStop);
                cursorX = stops * tabStop;
                addEmptyCharacter(i, vertices, texCrds, indices);
            } else { // normal character
                chr = _font.getChar(charVal);

                // -- vertices -----------------
                l = alignX + cursorX + chr.xoffset;
                t = alignY + cursorY + chr.yoffset;
                r = alignX + cursorX + chr.xoffset + chr.width;
                b = alignY + cursorY + chr.yoffset + chr.height;

                vertices.put(l).put(0).put(t); // left top
                vertices.put(l).put(0).put(b); // left bottom
                vertices.put(r).put(0).put(b); // right bottom
                vertices.put(r).put(0).put(t); // right top

                // -- tex coords ----------------
                l = chr.x / txW;
                t = chr.y / txH;
                r = (chr.x + chr.width) / txW;
                b = (chr.y + chr.height) / txH;

                texCrds.put(l).put(t); // left top
                texCrds.put(l).put(b); // left bottom
                texCrds.put(r).put(b); // right bottom
                texCrds.put(r).put(t); // right top

                // -- indices -------------------
                indices.put(i * 4 + 0);
                indices.put(i * 4 + 1);
                indices.put(i * 4 + 2);
                indices.put(i * 4 + 3);

                int nextVal = 0;
                if (i < strLen - 1) {
                    nextVal = _textString.charAt(i + 1);
                }
                final int kern = _font.getKerning(charVal, nextVal);
                cursorX += chr.xadvance + kern + _spacing;
            }
        }
        reconstruct(vertices, null, null, new FloatBufferData(texCrds, 2), indices);
    }

    // this is inefficient yet incredibly convenient
    // used for tab and newline
    private void addEmptyCharacter(final int i, final FloatBuffer vertices, final FloatBuffer uvs,
            final IntBuffer indices) {
        vertices.put(0).put(0).put(0);
        vertices.put(0).put(0).put(0);
        vertices.put(0).put(0).put(0);
        vertices.put(0).put(0).put(0);
        uvs.put(0).put(0);
        uvs.put(0).put(0);
        uvs.put(0).put(0);
        uvs.put(0).put(0);
        indices.put(i * 4 + 0);
        indices.put(i * 4 + 1);
        indices.put(i * 4 + 2);
        indices.put(i * 4 + 3);
    }

    public String getText() {
        return _textString;
    }

    /**
     * @param align
     */
    public void setAlign(final Align align) {
        _align = align;
        setText(_textString);
    }

    public Align getAlign() {
        return _align;
    }

    public void setJustify(final Justify justify) {
        _justify = justify;
        setText(_textString);
    }

    public Justify getJustify() {
        return _justify;
    }

    /**
     * set a fixed offset from the alignment center of rotation IN FONT UNITS
     */
    public void setFixedOffset(double x, double y) {
        x *= _font.getSize();
        y *= _font.getSize();
        _fixedOffset.set(x, y);
    }

    /**
     * set a fixed offset from the alignment center of rotation IN FONT UNITS
     */
    public void setFixedOffset(final Vector2 offset) {
        final double x = offset.getX() * _font.getSize();
        final double y = offset.getY() * _font.getSize();
        _fixedOffset.set(x, y);
    }
}
