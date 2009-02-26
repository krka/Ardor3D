/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.DrawBufferTarget;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.StereoCamera;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Controller;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.util.TextureManager;
import com.google.inject.Inject;

/**
 * Similar to the BoxExample, but in stereo
 */
public class StereoExample extends ExampleBase {

    private Mesh _mesh;
    private StereoCamera _camera;
    private ColorMaskState noRed, redOnly;

    /**
     * Change this to true to use side-by-side rendering. false will turn on left/right buffer swapping.
     */
    private static final boolean _sideBySide = false;

    /**
     * Change this to true to use anaglyph style (red/green) 3d. False will do hardware based 3d.
     */
    private static final boolean _useAnaglyph = true;

    public static void main(final String[] args) {
        _stereo = !_sideBySide && !_useAnaglyph;
        start(StereoExample.class);
    }

    @Inject
    public StereoExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Stereo Example");

        redOnly = new ColorMaskState();
        redOnly.setAll(true);
        redOnly.setBlue(false);
        redOnly.setGreen(false);

        noRed = new ColorMaskState();
        noRed.setAll(true);
        noRed.setRed(false);

        // setup our stereo camera as the new canvas camera
        _camera = new StereoCamera(_canvas.getCanvasRenderer().getCamera());
        _canvas.getCanvasRenderer().setCamera(_camera);

        // Setup our left and right camera using the parameters on the stereo camera itself
        _camera.setFocalDistance(10.0);
        _camera.setEyeSeparation(_camera.getFocalDistance() / 30.0);
        _camera.setAperture(45.0 * MathUtils.DEG_TO_RAD);
        _camera.setSideBySideMode(_sideBySide);
        _camera.setupLeftRightCameras();

        _mesh = new Teapot("Teapot");
        _mesh.setModelBound(new BoundingBox());
        _mesh.updateModelBound();
        _mesh.setTranslation(new Vector3(0, 0, -15));
        _root.attachChild(_mesh);

        _mesh.addController(new Controller() {
            private static final long serialVersionUID = 1L;
            private double currentTime;
            private final Matrix3 _rotate = new Matrix3();
            private final Vector3 _axis = new Vector3(1, 1, 0.5f).normalizeLocal();
            private double _angle = 0;

            @Override
            public void update(final double time, final Spatial caller) {
                _angle = _angle + (time * 45);
                if (_angle > 360) {
                    _angle = 0;
                }

                _rotate.fromAngleNormalAxis(_angle * MathUtils.DEG_TO_RAD, _axis);
                caller.setRotation(_rotate);

                currentTime += time * 0.5;
                final ReadOnlyVector3 ttranslate = _mesh.getTranslation();
                caller.setTranslation(ttranslate.getX(), ttranslate.getY(), Math.sin(currentTime) * 10.0 - 15);
            }
        });

        final Box box = new Box("Box", new Vector3(), 50, 50, 1);
        box.setModelBound(new BoundingBox());
        box.setTranslation(new Vector3(0, 0, -75));
        _root.attachChild(box);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.GuessNoCompression, true));
        _root.setRenderState(ts);

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _root.setRenderState(ms);
    }

    @Override
    protected void renderExample(final Renderer renderer) {

        // Update left and right camera frames based on current camera.
        _camera.updateLeftRightCameraFrames();

        // Right Eye
        {
            if (!_sideBySide && !_useAnaglyph) {
                // Set right back buffer
                renderer.setDrawBuffer(DrawBufferTarget.BackRight);
                renderer.clearBuffers();
            } else if (_useAnaglyph) {
                renderer.clearBuffers();
            }

            // Set right cam
            _camera.switchToRightCamera(renderer);

            // draw scene
            if (_useAnaglyph) {
                ContextManager.getCurrentContext().enforceState(redOnly);
            }
            renderer.draw(_root);
            super.renderDebug(renderer);
            renderer.renderBuckets();
        }

        // Left Eye
        {
            if (!_sideBySide && !_useAnaglyph) {
                // Set left back buffer
                renderer.setDrawBuffer(DrawBufferTarget.BackLeft);
                renderer.clearBuffers();
            } else if (_useAnaglyph) {
                renderer.clearZBuffer();
            }

            // Set left cam
            _camera.switchToLeftCamera(renderer);

            // Draw scene
            if (_useAnaglyph) {
                ContextManager.getCurrentContext().enforceState(noRed);
            }
            renderer.draw(_root);
            super.renderDebug(renderer);
            renderer.renderBuckets();
        }
        if (_useAnaglyph) {
            ContextManager.getCurrentContext().clearEnforcedState(StateType.ColorMask);
        }
    }

    @Override
    protected void renderDebug(final Renderer renderer) {
    // ignore. We'll call super on the individual left/right renderings.
    }
}
