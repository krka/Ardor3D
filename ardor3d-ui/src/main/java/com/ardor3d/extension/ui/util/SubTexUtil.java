/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.util;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Transform;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.geom.BufferUtils;

/**
 * SubTexUtil is a utility for drawing SubTex objects to a renderer.
 */
public class SubTexUtil {

    private static final Mesh _mesh = SubTexUtil.createMesh();
    private static final float[] _vals = new float[8];
    private static final float[] _texc = new float[8];
    private static TextureState _tstate = new TextureState();
    private static final Transform _helperT = new Transform();

    /**
     * Draw the given SubTex, as-is, to the screen at the given location.
     * 
     * @param renderer
     *            the renderer to use
     * @param subTex
     *            the SubTex to draw
     * @param x
     *            the x coordinate of the screen location to draw at
     * @param y
     *            the y coordinate of the screen location to draw at
     */
    public static void drawSubTex(final Renderer renderer, final SubTex subTex, final int x, final int y) {
        SubTexUtil.drawSubTex(renderer, subTex, x, y, subTex.getWidth(), subTex.getHeight(), false);
    }

    /**
     * Draw the given SubTex to the screen at the given location. Use the given width and height instead of those
     * supplied in the SubTex.
     * 
     * @param renderer
     *            the renderer to use
     * @param subTex
     *            the SubTex to draw
     * @param x
     *            the x coordinate of the screen location to draw at
     * @param y
     *            the y coordinate of the screen location to draw at
     * @param width
     *            the width in screen pixels to use when drawing the SubTex.
     * @param height
     *            the height in screen pixels to use when drawing the SubTex.
     */
    public static void drawSubTex(final Renderer renderer, final SubTex subTex, final int x, final int y,
            final int width, final int height) {
        SubTexUtil.drawSubTex(renderer, subTex, x, y, width, height, false);
    }

    /**
     * Draw the given SubTex to the screen at the given location. Use the given width and height instead of those
     * supplied in the SubTex.
     * 
     * @param renderer
     *            the renderer to use
     * @param subTex
     *            the SubTex to draw
     * @param x
     *            the x coordinate of the screen location to draw at
     * @param y
     *            the y coordinate of the screen location to draw at
     * @param width
     *            the width in screen pixels to use when drawing the SubTex.
     * @param height
     *            the height in screen pixels to use when drawing the SubTex.
     */
    public static void drawSubTex(final Renderer renderer, final SubTex subTex, final double x, final double y,
            final double width, final double height) {
        SubTexUtil.drawSubTex(renderer, subTex, (int) Math.round(x), (int) Math.round(y), (int) Math.round(width),
                (int) Math.round(height), false);
    }

    /**
     * Draw the given SubTex, optionally inverted on the Y axis, to the screen at the given location. Use the given
     * width and height instead of those supplied in the SubTex.
     * 
     * @param renderer
     *            the renderer to use
     * @param subTex
     *            the SubTex to draw
     * @param x
     *            the x coordinate of the screen location to draw at
     * @param y
     *            the y coordinate of the screen location to draw at
     * @param width
     *            the width in screen pixels to use when drawing the SubTex.
     * @param height
     *            the height in screen pixels to use when drawing the SubTex.
     * @param flipVertical
     *            if true, invert the image vertically before drawing
     */
    public static void drawSubTex(final Renderer renderer, final SubTex subTex, final int x, final int y,
            final int width, final int height, final boolean flipVertical) {

        if (width == 0 || height == 0 || subTex == null || subTex.getTexture() == null) {
            return; // no need to draw
        }

        // Optimization: Check to see if the given SubTex's Texture is the same one we used "last time".
        if (SubTexUtil._tstate.getNumberOfSetTextures() == 0 || SubTexUtil._tstate.getTexture().getTextureKey() == null
                || !SubTexUtil._tstate.getTexture().getTextureKey().equals(subTex.getTexture().getTextureKey())) {
            SubTexUtil._tstate.setTexture(subTex.getTexture());
            SubTexUtil._mesh.setRenderState(SubTexUtil._tstate);
            SubTexUtil._mesh.updateWorldRenderStates(false);
        }

        // Setup our tint color and alpha value.
        final ColorRGBA defaultColor = ColorRGBA.fetchTempInstance();
        if (subTex.getTint() != null) {
            defaultColor.set(subTex.getTint());
            defaultColor.setAlpha(UIComponent.getCurrentOpacity() * subTex.getTint().getAlpha());
        } else {
            defaultColor.set(ColorRGBA.WHITE);
            defaultColor.setAlpha(UIComponent.getCurrentOpacity());
        }
        SubTexUtil._mesh.setDefaultColor(defaultColor);
        ColorRGBA.releaseTempInstance(defaultColor);

        final float endY = subTex.getEndY();
        final float endX = subTex.getEndX();

        final float startX = subTex.getStartX();
        final float startY = subTex.getStartY();

        // Set up texture coordinates based on vertical flip
        if (!flipVertical) {
            SubTexUtil._texc[0] = startX;
            SubTexUtil._texc[1] = endY;
            SubTexUtil._texc[2] = endX;
            SubTexUtil._texc[3] = endY;
            SubTexUtil._texc[4] = endX;
            SubTexUtil._texc[5] = startY;
            SubTexUtil._texc[6] = startX;
            SubTexUtil._texc[7] = startY;
        } else {
            SubTexUtil._texc[0] = startX;
            SubTexUtil._texc[1] = startY;
            SubTexUtil._texc[2] = endX;
            SubTexUtil._texc[3] = startY;
            SubTexUtil._texc[4] = endX;
            SubTexUtil._texc[5] = endY;
            SubTexUtil._texc[6] = startX;
            SubTexUtil._texc[7] = endY;
        }

        SubTexUtil._vals[0] = 0;
        SubTexUtil._vals[1] = 0;
        SubTexUtil._vals[2] = width;
        SubTexUtil._vals[3] = 0;
        SubTexUtil._vals[4] = width;
        SubTexUtil._vals[5] = height;
        SubTexUtil._vals[6] = 0;
        SubTexUtil._vals[7] = height;

        // set screen space offsets
        SubTexUtil._mesh.setWorldTransform(Transform.IDENTITY);
        SubTexUtil._mesh.setWorldTranslation(x, y, 0);

        // set our vertices into the mesh
        SubTexUtil._mesh.getMeshData().getVertexBuffer().rewind();
        SubTexUtil._mesh.getMeshData().getVertexBuffer().put(SubTexUtil._vals);

        // set our texture coords into the mesh
        SubTexUtil._mesh.getMeshData().getTextureBuffer(0).rewind();
        SubTexUtil._mesh.getMeshData().getTextureBuffer(0).put(SubTexUtil._texc);

        // draw mesh
        SubTexUtil._mesh.render(renderer);
    }

    /**
     * Draw the given TransformedSubTex, optionally inverted on the Y axis, to the screen at the given location. Use the
     * given width and height instead of those supplied in the TransformedSubTex.
     * 
     * @param renderer
     *            the renderer to use
     * @param subTex
     *            the TransformedSubTex to draw
     * @param x
     *            the x coordinate of the screen location to draw at
     * @param y
     *            the y coordinate of the screen location to draw at
     * @param width
     *            the width in screen pixels to use when drawing the TransformedSubTex.
     * @param height
     *            the height in screen pixels to use when drawing the TransformedSubTex.
     * @param flipVertical
     *            if true, invert the image vertically before drawing
     */
    public static void drawTransformedSubTex(final Renderer renderer, final TransformedSubTex subTex, final int x,
            final int y, final int width, final int height, final boolean flipVertical) {

        if (width == 0 || height == 0 || subTex == null || subTex.getTexture() == null) {
            return; // no need to draw
        }

        // Optimization: Check to see if the given SubTex's Texture is the same one we used "last time".
        if (SubTexUtil._tstate.getNumberOfSetTextures() == 0 || SubTexUtil._tstate.getTexture().getTextureKey() == null
                || !SubTexUtil._tstate.getTexture().getTextureKey().equals(subTex.getTexture().getTextureKey())) {
            SubTexUtil._tstate.setTexture(subTex.getTexture());
            SubTexUtil._mesh.setRenderState(SubTexUtil._tstate);
            SubTexUtil._mesh.updateWorldRenderStates(false);
        }

        // Setup our tint color and alpha value.
        final ColorRGBA defaultColor = ColorRGBA.fetchTempInstance();
        if (subTex.getTint() != null) {
            defaultColor.set(subTex.getTint());
            defaultColor.setAlpha(UIComponent.getCurrentOpacity() * subTex.getTint().getAlpha());
        } else {
            defaultColor.set(ColorRGBA.WHITE);
            defaultColor.setAlpha(UIComponent.getCurrentOpacity());
        }
        SubTexUtil._mesh.setDefaultColor(defaultColor);
        ColorRGBA.releaseTempInstance(defaultColor);

        final float endY = subTex.getEndY();
        final float endX = subTex.getEndX();

        final float startX = subTex.getStartX();
        final float startY = subTex.getStartY();

        // Set up texture coordinates based on vertical flip
        if (!flipVertical) {
            SubTexUtil._texc[0] = startX;
            SubTexUtil._texc[1] = endY;
            SubTexUtil._texc[2] = endX;
            SubTexUtil._texc[3] = endY;
            SubTexUtil._texc[4] = endX;
            SubTexUtil._texc[5] = startY;
            SubTexUtil._texc[6] = startX;
            SubTexUtil._texc[7] = startY;
        } else {
            SubTexUtil._texc[0] = startX;
            SubTexUtil._texc[1] = startY;
            SubTexUtil._texc[2] = endX;
            SubTexUtil._texc[3] = startY;
            SubTexUtil._texc[4] = endX;
            SubTexUtil._texc[5] = endY;
            SubTexUtil._texc[6] = startX;
            SubTexUtil._texc[7] = endY;
        }

        final float leftW = -subTex.getPivot().getXf() * width;
        final float rightW = (1f - subTex.getPivot().getXf()) * width;
        final float leftH = -subTex.getPivot().getYf() * height;
        final float rightH = (1f - subTex.getPivot().getYf()) * height;
        SubTexUtil._vals[0] = leftW;
        SubTexUtil._vals[1] = leftH;
        SubTexUtil._vals[2] = rightW;
        SubTexUtil._vals[3] = leftH;
        SubTexUtil._vals[4] = rightW;
        SubTexUtil._vals[5] = rightH;
        SubTexUtil._vals[6] = leftW;
        SubTexUtil._vals[7] = rightH;

        // set screen space offsets
        SubTexUtil._helperT.set(subTex.getTransform());
        SubTexUtil._helperT.translate(x, y, 0);
        SubTexUtil._mesh.setWorldTransform(SubTexUtil._helperT);

        // set our vertices into the mesh
        SubTexUtil._mesh.getMeshData().getVertexBuffer().rewind();
        SubTexUtil._mesh.getMeshData().getVertexBuffer().put(SubTexUtil._vals);

        // set our texture coords into the mesh
        SubTexUtil._mesh.getMeshData().getTextureBuffer(0).rewind();
        SubTexUtil._mesh.getMeshData().getTextureBuffer(0).put(SubTexUtil._texc);

        // draw mesh
        SubTexUtil._mesh.render(renderer);
    }

    private static Mesh createMesh() {
        final Mesh mesh = new Mesh();
        mesh.getMeshData().setVertexCoords(new FloatBufferData(BufferUtils.createVector2Buffer(4), 2));
        mesh.getMeshData().setTextureBuffer(BufferUtils.createVector2Buffer(4), 0);
        mesh.getMeshData().setIndexMode(IndexMode.Quads);

        mesh.setRenderState(SubTexUtil._tstate);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(SourceFunction.SourceAlpha);
        blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
        mesh.setRenderState(blend);

        mesh.updateWorldRenderStates(false);

        return mesh;
    }
}
