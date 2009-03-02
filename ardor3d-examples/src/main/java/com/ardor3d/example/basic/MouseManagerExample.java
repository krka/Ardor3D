/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.basic;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.Key;
import com.ardor3d.input.InputState;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.TextureManager;
import com.google.inject.Inject;

import java.net.URL;
import java.io.IOException;

public class MouseManagerExample extends ExampleBase {
    private final MouseManager _mouseManager;

    private Mesh t;
    private final Matrix3 rotate = new Matrix3();
    private double angle = 0;
    private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();

    private boolean useCursorOne = true;
    private MouseCursor _cursor1;
    private MouseCursor _cursor2;

    public static void main(final String[] args) {
        start(MouseManagerExample.class);
    }

    @Inject
    public MouseManagerExample(final LogicalLayer logicalLayer, final FrameHandler frameWork, final MouseManager mouseManager) {
        super(logicalLayer, frameWork);
        _mouseManager = mouseManager;
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
        _canvas.setTitle("Vertex Colors");

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

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _root.setRenderState(ms);

        _root.setRenderState(ts);

        // TODO:
        // init cursors with images
        // ensure valid guice config for each window type (SWT, AWT, LWJGL)
        // implement AWT and SWT MouseManager versions
        // implement proper LWJGL image handling
        // get sample cursors
        AWTImageLoader awtImageLoader = new AWTImageLoader();

        try {
            _cursor1 = createMouseCursor(awtImageLoader, "/com/ardor3d/example/media/input/cursor1.png");
            _cursor2 = createMouseCursor(awtImageLoader, "/com/ardor3d/example/media/input/test.PNG");

            _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.H),
                    new TriggerAction() {
                        public void perform(final Canvas source, final InputState inputState, final double tpf) {
                            if (useCursorOne) {
                                _mouseManager.setCursor(_cursor1);
                            }
                            else {
                                _mouseManager.setCursor(_cursor2);
                            }
                            useCursorOne = !useCursorOne;
                        }
                    }));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private MouseCursor createMouseCursor(final AWTImageLoader awtImageLoader, final String resourceName) throws IOException {
        Image image = awtImageLoader.load(getClass().getResourceAsStream(resourceName), true);

        return new MouseCursor("cursor1", image, 0, image.getHeight() - 1);
    }
}