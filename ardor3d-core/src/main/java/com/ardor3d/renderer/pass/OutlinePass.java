/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.pass;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.CullState.Face;

/**
 * This Pass can be used for drawing an outline around geometry objects. It does this by first drawing the geometry as
 * normal, and then drawing an outline using the geometry's wireframe.
 */
public class OutlinePass extends RenderPass {

    private static final long serialVersionUID = 1L;

    public static final float DEFAULT_LINE_WIDTH = 3f;
    public static final ReadOnlyColorRGBA DEFAULT_OUTLINE_COLOR = new ColorRGBA(ColorRGBA.BLACK);

    // render states needed to draw the outline
    private final CullState frontCull;
    private final CullState backCull;
    private final WireframeState wireframeState;
    private final LightState noLights;
    private final TextureState noTexture;
    private BlendState blendState;

    public OutlinePass(final boolean antialiased) {
        wireframeState = new WireframeState();
        wireframeState.setFace(WireframeState.Face.FrontAndBack);
        wireframeState.setLineWidth(DEFAULT_LINE_WIDTH);
        wireframeState.setEnabled(true);

        frontCull = new CullState();
        frontCull.setCullFace(Face.Front);

        backCull = new CullState();
        backCull.setCullFace(Face.Back);

        wireframeState.setAntialiased(antialiased);

        noLights = new LightState();
        noLights.setGlobalAmbient(DEFAULT_OUTLINE_COLOR);
        noLights.setEnabled(true);

        noTexture = new TextureState();
        noTexture.setEnabled(true);

        blendState = new BlendState();
        blendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        blendState.setBlendEnabled(true);
        blendState.setEnabled(true);

    }

    @Override
    public void doRender(final Renderer renderer) {
        // if there's nothing to do
        if (_spatials.size() == 0) {
            return;
        }

        // normal render
        _context.enforceState(frontCull);
        super.doRender(renderer);

        // set up the render states
        // CullState.setFlippedCulling(true);
        _context.enforceState(backCull);
        _context.enforceState(wireframeState);
        _context.enforceState(noLights);
        _context.enforceState(noTexture);
        _context.enforceState(blendState);

        // this will draw the wireframe
        super.doRender(renderer);

        // revert state changes
        // CullState.setFlippedCulling(false);
        _context.clearEnforcedStates();
    }

    public void setOutlineWidth(final float width) {
        wireframeState.setLineWidth(width);
    }

    public float getOutlineWidth() {
        return wireframeState.getLineWidth();
    }

    public void setOutlineColor(final ReadOnlyColorRGBA outlineColor) {
        noLights.setGlobalAmbient(outlineColor);
    }

    public ReadOnlyColorRGBA getOutlineColor() {
        return noLights.getGlobalAmbient();
    }

    public BlendState getBlendState() {
        return blendState;
    }

    public void setBlendState(final BlendState alphaState) {
        blendState = alphaState;
    }
}
