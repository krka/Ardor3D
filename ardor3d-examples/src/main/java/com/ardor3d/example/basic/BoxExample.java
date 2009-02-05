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
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.logical.LogicalLayer;
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

public class BoxExample extends ExampleBase {

    private Mesh t;
    private final Matrix3 rotate = new Matrix3();
    private double angle = 0;
    private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();

    public static void main(final String[] args) {
        start(BoxExample.class);
    }

    @Inject
    public BoxExample(final LogicalLayer logicalLayer, final FrameHandler frameWork) {
        super(logicalLayer, frameWork);
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

    }
}
