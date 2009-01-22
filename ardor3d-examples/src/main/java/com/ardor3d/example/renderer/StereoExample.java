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

import org.lwjgl.opengl.GL11;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.FrameWork;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.TextureManager;
import com.google.inject.Inject;

/**
 * Similar to the BoxExample, but in stereo
 */
public class StereoExample extends ExampleBase {

    private Mesh t;
    private final Matrix3 rotate = new Matrix3();
    private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();
    private double angle = 0;
    private final Camera _leftCamera = new Camera(640, 480);
    private final Camera _rightCamera = new Camera(640, 480);

    public static void main(final String[] args) {
        _stereo = true;
        start(StereoExample.class);
    }

    @Inject
    public StereoExample(final LogicalLayer layer, final FrameWork frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void updateExample(final double tpf) {
        if (tpf < 1) {
            angle = angle + (tpf * 25);
            if (angle > 360) {
                angle = 0;
            }
        }

        rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
        t.setRotation(rotate);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Stereo Example");

        final Vector3 max = new Vector3(5, 5, 5);
        final Vector3 min = new Vector3(-5, -5, -5);

        t = new Box("Box", min, max);
        t.setModelBound(new BoundingBox());
        t.updateModelBound();
        t.setTranslation(new Vector3(0, 0, -15));
        _root.attachChild(t);

        t.setRandomColors();

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
        // Save old camera
        final Camera oldCam = ContextManager.getCurrentContext().getCurrentCamera();

        // Update left and right cameras based on current camera position and orientation.
        updateStereoCameras(oldCam, _leftCamera, _rightCamera);

        // Set left cam
        ContextManager.getCurrentContext().setCurrentCamera(_leftCamera);
        _leftCamera.update();
        _leftCamera.apply(renderer);

        // Set left back buffer
        GL11.glDrawBuffer(GL11.GL_BACK_LEFT);

        // clear
        renderer.clearBuffers();

        // Draw scene
        renderer.draw(_root);
        super.renderDebug(renderer);
        renderer.renderBuckets();

        // Set right cam
        ContextManager.getCurrentContext().setCurrentCamera(_rightCamera);
        _rightCamera.update();
        _rightCamera.apply(renderer);

        // Set right back buffer
        GL11.glDrawBuffer(GL11.GL_BACK_RIGHT);

        // clear
        renderer.clearBuffers();

        // draw scene
        renderer.draw(_root);
        super.renderDebug(renderer);
        renderer.renderBuckets();

        // replace old camera
        ContextManager.getCurrentContext().setCurrentCamera(oldCam);
        oldCam.update();
        oldCam.apply(renderer);

        // go back to back buffer drawing
        GL11.glDrawBuffer(GL11.GL_BACK);
    }

    public static void updateStereoCameras(final Camera sourceCamera, final Camera leftCamera, final Camera rightCamera) {

    }

    @Override
    protected void renderDebug(final Renderer renderer) {
    // ignore. We'll call super on the individual left/right renderings.
    }
}
