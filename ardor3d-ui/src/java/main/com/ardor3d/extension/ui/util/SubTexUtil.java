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

import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
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
        SubTexUtil.drawIcon(renderer, subTex, x, y, 1.0, 1.0);
    }

    /**
     * Draw the given SubTex, rotated by a given angle around its center, to the screen at the given location.
     * 
     * @param renderer
     *            the renderer to use
     * @param subTex
     *            the SubTex to draw
     * @param x
     *            the x coordinate of the screen location to draw at
     * @param y
     *            the y coordinate of the screen location to draw at
     * @param angle
     *            the angle, in radians, to rotate
     */
    public static void drawIcon(final Renderer renderer, final SubTex subTex, final int x, final int y,
            final double angle) {
        SubTexUtil.drawStretchedIcon(renderer, subTex, x, y, subTex.getWidth(), subTex.getHeight(), angle);
    }

    /**
     * Draw the given SubTex, scaled by the given percentages, to the screen at the given location.
     * 
     * @param renderer
     *            the renderer to use
     * @param subTex
     *            the SubTex to draw
     * @param x
     *            the x coordinate of the screen location to draw at
     * @param y
     *            the y coordinate of the screen location to draw at
     * @param scaleX
     *            the horizontal scale, as a percentage, to draw the icon at. This is multiplied against the subTex's
     *            width to get a final width.
     * @param scaleY
     *            the vertical scale, as a percentage, to draw the icon at. This is multiplied against the subTex's
     *            height to get a final height.
     */
    public static void drawIcon(final Renderer renderer, final SubTex subTex, final double x, final double y,
            final double scaleX, final double scaleY) {
        SubTexUtil.drawStretchedIcon(renderer, subTex, x, y, subTex.getWidth() * scaleX, subTex.getHeight() * scaleY);
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
    public static void drawStretchedIcon(final Renderer renderer, final SubTex subTex, final int x, final int y,
            final int width, final int height) {
        SubTexUtil.drawStretchedIcon(renderer, subTex, x, y, width, height, 0, false);
    }

    /**
     * Draw the given SubTex to the screen at the given location. Use the given width and height instead of those
     * supplied in the SubTex. The width and height are rounded to the nearest integer value using Round-Half-Up.
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
    public static void drawStretchedIcon(final Renderer renderer, final SubTex subTex, final double x, final double y,
            final double width, final double height) {
        SubTexUtil.drawStretchedIcon(renderer, subTex, (int) (x + 0.5), (int) (y + 0.5), (int) (width + 0.5),
                (int) (height + 0.5), 0, false);
    }

    /**
     * Draw the given SubTex, rotated by a given angle around its center, to the screen at the given location. Use the
     * given width and height instead of those supplied in the SubTex.
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
     * @param angle
     *            the angle, in radians, to rotate
     */
    public static void drawStretchedIcon(final Renderer renderer, final SubTex subTex, final int x, final int y,
            final int width, final int height, final double angle) {
        SubTexUtil.drawStretchedIcon(renderer, subTex, x, y, width, height, angle, false);
    }

    /**
     * Draw the given SubTex, rotated by a given angle around its center and optionally inverted on the Y axis, to the
     * screen at the given location. Use the given width and height instead of those supplied in the SubTex.
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
     * @param angle
     *            the angle, in radians, to rotate
     * @param flipVertical
     *            if true, invert the image vertically before drawing
     */
    public static void drawStretchedIcon(final Renderer renderer, final SubTex subTex, final int x, final int y,
            final int width, final int height, final double angle, final boolean flipVertical) {

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
            defaultColor.setAlpha(UIFrame.getCurrentOpacity() * subTex.getTint().getAlpha());
        } else {
            defaultColor.set(ColorRGBA.WHITE);
            defaultColor.setAlpha(UIFrame.getCurrentOpacity());
        }
        SubTexUtil._mesh.setDefaultColor(defaultColor);
        ColorRGBA.releaseTempInstance(defaultColor);

        // Position ourselves in screen space.
        SubTexUtil._mesh.setWorldTranslation(x, y, 0);

        final float endY = subTex.getEndY();
        final float endX = subTex.getEndX();

        final float startX = subTex.getStartX();
        final float startY = subTex.getStartY();

        // Set up texture coords based on vertical flip
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

        // Set up our rotation, if angle is not 0
        if (angle == 0) {
            SubTexUtil._vals[0] = 0;
            SubTexUtil._vals[1] = 0;
            SubTexUtil._vals[2] = width;
            SubTexUtil._vals[3] = 0;
            SubTexUtil._vals[4] = width;
            SubTexUtil._vals[5] = height;
            SubTexUtil._vals[6] = 0;
            SubTexUtil._vals[7] = height;
        } else {
            // Rotate each of the 4 corners separately.
            final double halfWidth = width * .5;
            final double halfHeight = height * .5;
            final Vector2 tempV = Vector2.fetchTempInstance();
            tempV.set(-halfWidth, -halfHeight);
            tempV.rotateAroundOriginLocal(angle, true);
            SubTexUtil._vals[0] = (float) (tempV.getX() + halfWidth);
            SubTexUtil._vals[1] = (float) (tempV.getY() + halfHeight);

            tempV.set(halfWidth, -halfHeight);
            tempV.rotateAroundOriginLocal(angle, true);
            SubTexUtil._vals[2] = (float) (tempV.getX() + halfWidth);
            SubTexUtil._vals[3] = (float) (tempV.getY() + halfHeight);

            tempV.set(halfWidth, halfHeight);
            tempV.rotateAroundOriginLocal(angle, true);
            SubTexUtil._vals[4] = (float) (tempV.getX() + halfWidth);
            SubTexUtil._vals[5] = (float) (tempV.getY() + halfHeight);

            tempV.set(-halfWidth, halfHeight);
            tempV.rotateAroundOriginLocal(angle, true);
            SubTexUtil._vals[6] = (float) (tempV.getX() + halfWidth);
            SubTexUtil._vals[7] = (float) (tempV.getY() + halfHeight);
            Vector2.releaseTempInstance(tempV);
        }

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
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        mesh.setRenderState(blend);

        mesh.updateWorldRenderStates(false);

        return mesh;
    }
}
