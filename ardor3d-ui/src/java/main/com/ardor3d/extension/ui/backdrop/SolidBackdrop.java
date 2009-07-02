/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.backdrop;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.util.UIQuad;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;

/**
 * This backdrop paints a solid rectangle of color behind a UI component.
 */
public class SolidBackdrop extends UIBackdrop {

    /** The color to draw */
    private final ColorRGBA _color = new ColorRGBA(ColorRGBA.GRAY);
    /** The quad used across all solid backdrops to render with. */
    private static UIQuad _standin = SolidBackdrop.createStandinQuad();

    /**
     * Construct this backdrop, using the given color.
     * 
     * @param color
     *            the color of the backdrop
     */
    public SolidBackdrop(final ReadOnlyColorRGBA color) {
        setColor(color);
    }

    public ReadOnlyColorRGBA getColor() {
        return _color;
    }

    public void setColor(final ReadOnlyColorRGBA color) {
        _color.set(color);
    }

    @Override
    public void draw(final Renderer renderer, final UIComponent comp) {

        final float oldA = _color.getAlpha();

        _color.setAlpha(oldA * UIFrame.getCurrentOpacity());
        SolidBackdrop._standin.setDefaultColor(_color);

        SolidBackdrop._standin.setWorldTranslation(
                (comp.getWorldTranslation().getX() + comp.getMargin().getLeft() + comp.getBorder().getLeft())
                        * comp.getWorldScale().getX(), (comp.getWorldTranslation().getY()
                        + comp.getMargin().getBottom() + comp.getBorder().getBottom())
                        * comp.getWorldScale().getY(), comp.getWorldTranslation().getZ());
        SolidBackdrop._standin.setWorldScale(comp.getWorldScale());

        final float width = UIBackdrop.getBackdropWidth(comp);
        final float height = UIBackdrop.getBackdropHeight(comp);
        SolidBackdrop._standin.resize(width, height);
        SolidBackdrop._standin.render(renderer);

        _color.setAlpha(oldA);
    }

    private static UIQuad createStandinQuad() {
        final UIQuad quad = new UIQuad("standin", 1, 1);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        quad.setRenderState(blend);
        quad.updateWorldRenderStates(false);

        return quad;
    }
}
