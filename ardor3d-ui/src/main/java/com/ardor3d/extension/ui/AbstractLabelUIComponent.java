/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.Dimension;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.extension.ui.util.SubTexUtil;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.ui.text.BMFont;
import com.ardor3d.ui.text.BMText;
import com.ardor3d.ui.text.BMText.AutoFade;
import com.ardor3d.ui.text.BMText.AutoScale;

/**
 * A state component containing a text label and an icon. These are separated by an optional gap and can also be given a
 * specific alignment. By default, the text is aligned LEFT and has no icon or gap.
 */
public abstract class AbstractLabelUIComponent extends StateBasedUIComponent implements Textable {

    /** Distance between text and icon if both are present. */
    private int _gap = 0;

    /** Alignment value to use to position the icon/text within the overall dimensions of this component. */
    private Alignment _alignment = Alignment.LEFT;

    /** The icon the draw on this icon. */
    private SubTex _icon = null;

    /** The size to draw our icon at. */
    private final Dimension _iconDimensions = new Dimension();

    /** The text object to use for drawing label text. */
    private BMText _text;

    @Override
    public void updateMinimumSizeFromContents() {
        int width = 0;
        int height = 0;

        final String textVal = getText();
        if (textVal != null && textVal.length() > 0) {
            width += Math.round(_text.getWidth());
            height += Math.round(_text.getHeight());
        }

        if (_iconDimensions != null) {
            width += _iconDimensions.getWidth();
            if (textVal != null && textVal.length() > 0) {
                width += _gap;
            }

            height = Math.max(_iconDimensions.getHeight(), height);
        }

        setMinimumContentSize(width, height);
        if (getContentWidth() < width) {
            setContentWidth(width);
        }
        if (getContentHeight() < height) {
            setContentHeight(height);
        }
        fireComponentDirty();
    }

    /**
     * @return the currently set text value of this label.
     */
    public String getText() {
        return _text != null ? _text.getText() : null;
    }

    /**
     * Set the text for this component. Also updates the minimum size of the component.
     * 
     * @param text
     *            the new text
     */
    public void setText(String text) {
        if (text != null && text.length() == 0) {
            text = null;
        }

        if (text != null) {
            if (text.equals(getText())) {
                return;
            }
            if (_text != null) {
                _text.setText(text);
            } else {
                _text = AbstractLabelUIComponent.createText(text, getFont());
                _text.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
                _text.updateGeometricState(0);
            }
        } else {
            _text = null;
        }

        updateMinimumSizeFromContents();
    }

    @Override
    public void setFont(final BMFont font) {
        super.setFont(font);

        // Reset our BMText object, using the new font.
        final String text = getText();
        _text = null;
        setText(text);
    }

    public Alignment getAlignment() {
        return _alignment;
    }

    public void setAlignment(final Alignment alignment) {
        _alignment = alignment;
    }

    public int getGap() {
        return _gap;
    }

    /**
     * Note: Also updates the minimum size of the component.
     * 
     * @param gap
     *            the size of the gap, in pixels, between the text and the label text. This is only used if both icon
     *            and text are set.
     */
    public void setGap(final int gap) {
        _gap = gap;
        updateMinimumSizeFromContents();
    }

    public SubTex getIcon() {
        return _icon;
    }

    /**
     * Note: Also updates the minimum size of the component.
     * 
     * @param icon
     *            the new icon for this label.
     */
    public void setIcon(final SubTex icon) {
        _icon = icon;
        updateMinimumSizeFromContents();
        if (icon != null && _iconDimensions.getHeight() == 0 && _iconDimensions.getWidth() == 0) {
            updateIconDimensionsFromIcon();
        }
    }

    /**
     * Set the icon dimensions from the currently set icon. If no icon is set, the dimensions are set to 0x0.
     */
    public void updateIconDimensionsFromIcon() {
        if (_icon != null) {
            _iconDimensions.set(_icon.getWidth(), _icon.getHeight());
        } else {
            _iconDimensions.set(0, 0);
        }
        updateMinimumSizeFromContents();
    }

    /**
     * Overrides any currently set icon size. Call this after setting the icon to prevent overriding.
     * 
     * @param dimensions
     *            a new icon size.
     */
    public void setIconDimensions(final Dimension dimensions) {
        _iconDimensions.set(dimensions);
        updateMinimumSizeFromContents();
    }

    public Dimension getIconDimensions() {
        return _iconDimensions;
    }

    @Override
    protected void drawComponent(final Renderer renderer) {

        double x = 0;
        double y = 0;
        int width = 0;

        // Gather our width... check for icon and text and gap.
        if (_icon != null) {
            width = _iconDimensions.getWidth();
            if (getText() != null && getText().length() > 0) {
                width += _gap;
            }
        } else if (getText() == null) {
            // not text OR icon, so no content to render.
            return;
        }

        if (getText() != null) {
            width += Math.round(_text.getWidth());
        }

        // find left most x location of content (icon+text) based on alignment.
        x = _alignment.alignX(getContentWidth(), width);

        if (_icon != null) {
            // find bottom most y location of icon based on alignment.
            if (_text != null && _text.getHeight() > _iconDimensions.getHeight()) {
                final int trailing = _text.getFont().getLineHeight() - _text.getFont().getBaseHeight();
                y = _alignment.alignY(getContentHeight() - trailing, _iconDimensions.getHeight()) + trailing - 1;
            } else {
                y = _alignment.alignY(getContentHeight(), _iconDimensions.getHeight());
            }

            final double dix = getWorldTranslation().getX() + getTotalLeft();
            final double diy = getWorldTranslation().getY() + getTotalBottom();
            // draw icon
            SubTexUtil.drawStretchedIcon(renderer, _icon, dix + x, diy + y, _iconDimensions.getWidth()
                    * getWorldScale().getX(), _iconDimensions.getHeight() * getWorldScale().getY());
            // shift X over by width of icon and gap
            x += (_iconDimensions.getWidth() + _gap) * getWorldScale().getX();
        }

        if (getText() != null) {
            // find bottom most y location of text based on alignment.
            y = _alignment.alignY(getContentHeight(), Math.round(_text.getHeight())) * getWorldScale().getY();

            // set our text location
            _text.setWorldTranslation(x + getWorldTranslation().getX() + getTotalLeft() * getWorldScale().getX(), y
                    + getWorldTranslation().getY() + getTotalBottom() * getWorldScale().getY(), getWorldTranslation()
                    .getZ());

            // draw text using current foreground color and alpha.
            final ColorRGBA color = ColorRGBA.fetchTempInstance();
            color.set(getForegroundColor());
            color.setAlpha(color.getAlpha() * UIFrame.getCurrentOpacity());
            _text.setTextColor(color);
            _text.render(renderer);
            ColorRGBA.releaseTempInstance(color);
        }
    }

    // Create an instance of BMText for text rendering.
    private static BMText createText(final String text, final BMFont font) {
        final BMText tComp = new BMText("", text, font);
        tComp.setAutoFade(AutoFade.Off);
        tComp.setAutoScale(AutoScale.Off);
        tComp.setAutoRotate(false);
        tComp.setFontScale(font.getSize());
        tComp.setRotation(new Matrix3().fromAngles(-MathUtils.HALF_PI, 0, 0));

        final ZBufferState zState = new ZBufferState();
        zState.setEnabled(false);
        zState.setWritable(false);
        tComp.setRenderState(zState);

        final CullState cState = new CullState();
        cState.setEnabled(false);
        tComp.setRenderState(cState);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        blend.setTestEnabled(true);
        blend.setReference(0f);
        blend.setTestFunction(BlendState.TestFunction.GreaterThan);
        tComp.setRenderState(blend);

        tComp.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        tComp.getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
        tComp.updateModelBound();
        return tComp;
    }
}
