/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.scenegraph.Spatial;

public abstract class AbstractFBOTextureRenderer implements TextureRenderer {
    private static final Logger logger = Logger.getLogger(AbstractFBOTextureRenderer.class.getName());

    /** List of states that override any set states on a spatial if not null. */
    protected final EnumMap<RenderState.StateType, RenderState> _enforcedStates = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    protected final Camera _camera = new Camera(1, 1);

    protected final ColorRGBA _backgroundColor = new ColorRGBA(1, 1, 1, 1);

    protected int _active;

    protected int _fboID = 0, _depthRBID = 0, _width = 0, _height = 0;

    protected IntBuffer _attachBuffer = null;
    protected boolean _usingDepthRB = false;

    protected final Renderer _parentRenderer;
    protected final Target _target;

    public AbstractFBOTextureRenderer(final DisplaySettings settings, final Target target,
            final Renderer parentRenderer, final ContextCapabilities caps) {
        _parentRenderer = parentRenderer;
        _target = target;

        int width = settings.getWidth();
        int height = settings.getHeight();
        if (!caps.isNonPowerOfTwoTextureSupported()) {
            // Check if we have non-power of two sizes. If so, find the smallest power of two size that is greater than
            // the provided size.
            if (!MathUtils.isPowerOfTwo(width)) {
                int newWidth = 2;
                do {
                    newWidth <<= 1;

                } while (newWidth < width);
                width = newWidth;
            }

            if (!MathUtils.isPowerOfTwo(height)) {
                int newHeight = 2;
                do {
                    newHeight <<= 1;

                } while (newHeight < height);
                height = newHeight;
            }
        }

        logger.fine("Creating FBO sized: " + width + " x " + height);

        _width = width;
        _height = height;

        _camera.resize(_width, _height);
        _camera.setFrustum(1.0f, 1000.0f, -0.50f, 0.50f, 0.50f, -0.50f);
        final Vector3 loc = new Vector3(0.0f, 0.0f, 0.0f);
        final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
        final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
        final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
        _camera.setFrame(loc, left, up, dir);
    }

    /**
     * <code>getCamera</code> retrieves the camera this renderer is using.
     * 
     * @return the camera this renderer is using.
     */
    public Camera getCamera() {
        return _camera;
    }

    public void setBackgroundColor(final ColorRGBA c) {
        _backgroundColor.set(c);
    }

    public ReadOnlyColorRGBA getBackgroundColor() {
        return _backgroundColor;
    }

    /**
     * <code>render</code> renders a scene. As it receives a base class of <code>Spatial</code> the renderer hands off
     * management of the scene to spatial for it to determine when a <code>Renderable</code> leaf is reached. The result
     * of the rendering is then copied into the given texture(s). What is copied is based on the Texture object's
     * rttSource field.
     * 
     * @param toDraw
     *            the scene to render.
     * @param tex
     *            the Texture(s) to render it to.
     */
    public void render(final Spatial toDraw, final Texture tex) {
        render(toDraw, tex, true);
    }

    /**
     * <code>render</code> renders a scene. As it receives a base class of <code>Spatial</code> the renderer hands off
     * management of the scene to spatial for it to determine when a <code>Renderable</code> leaf is reached. The result
     * of the rendering is then copied into the given texture(s). What is copied is based on the Texture object's
     * rttSource field.
     * 
     * @param toDraw
     *            the scene to render.
     * @param tex
     *            the Texture(s) to render it to.
     */
    public void render(final Spatial toDraw, final Texture tex, final boolean doClear) {
        try {
            activate();

            setupForSingleTexDraw(tex, doClear);

            doDraw(toDraw);

            takedownForSingleTexDraw(tex);

            deactivate();
        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture, boolean)", "Exception", e);
        }
    }

    protected abstract void activate();

    protected abstract void setupForSingleTexDraw(Texture tex, boolean doClear);

    protected abstract void takedownForSingleTexDraw(Texture tex);

    protected abstract void deactivate();

    public void render(final List<? extends Spatial> toDraw, final List<Texture> texs) {
        render(toDraw, texs, true);
    }

    private Camera _oldCamera;

    protected void switchCameraIn(final boolean doClear) {
        // grab non-rtt settings
        _oldCamera = ContextManager.getCurrentContext().getCurrentCamera();

        // swap to rtt settings
        _parentRenderer.getQueue().pushBuckets();

        // clear the scene
        if (doClear) {
            clearBuffers();
        }

        getCamera().update();
        getCamera().apply(_parentRenderer);
        ContextManager.getCurrentContext().setCurrentCamera(getCamera());
    }

    protected abstract void clearBuffers();

    protected void switchCameraOut() {
        _parentRenderer.flushFrame(false);

        // reset previous camera
        _oldCamera.update();
        _oldCamera.apply(_parentRenderer);
        ContextManager.getCurrentContext().setCurrentCamera(_oldCamera);

        // back to the non rtt settings
        _parentRenderer.getQueue().popBuckets();
    }

    protected void doDraw(final Spatial spat) {
        // Override parent's last frustum test to avoid accidental incorrect
        // cull
        if (spat.getParent() != null) {
            spat.getParent().setLastFrustumIntersection(Camera.FrustumIntersect.Intersects);
        }

        // do rtt scene render
        spat.onDraw(_parentRenderer);
    }

    protected void doDraw(final List<? extends Spatial> toDraw) {
        for (int x = 0, max = toDraw.size(); x < max; x++) {
            final Spatial spat = toDraw.get(x);
            doDraw(spat);
        }
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public void setMultipleTargets(final boolean multi) {
    // ignore. Does not matter to FBO.
    }

    public void enforceState(final RenderState state) {
        _enforcedStates.put(state.getType(), state);
    }

    public void clearEnforcedState(final RenderState state) {
        _enforcedStates.remove(state.getType());
    }

    public void clearEnforcedStates() {
        _enforcedStates.clear();
    }
}
