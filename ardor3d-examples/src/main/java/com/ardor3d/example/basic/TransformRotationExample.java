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
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Sphere;
import com.google.inject.Inject;

public class TransformRotationExample extends ExampleBase {

    public static void main(final String[] args) {
        start(TransformRotationExample.class);
    }

    @Inject
    public TransformRotationExample(final LogicalLayer logicalLayer, final FrameHandler frameWork) {
        super(logicalLayer, frameWork);
    }

    @Override
    protected void initExample() {
        final Sphere center = new Sphere("Center", 20, 20, 0.1);
        MaterialState ms = new MaterialState();
        ms.setDiffuse(new ColorRGBA(0.2f, 0.9f, 0.2f, 1f));
        ms.setSpecular(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f));
        ms.setEnabled(true);
        center.setRenderState(ms);
        center.setModelBound(new BoundingBox());
        center.updateModelBound();
        _root.attachChild(center);

        final Box box = new Box("Box", new Vector3(), 0.1, 0.1, 0.1);
        ms = new MaterialState();
        ms.setDiffuse(new ColorRGBA(0.9f, 0.2f, 0.2f, 1f));
        ms.setSpecular(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f));
        ms.setEnabled(true);
        box.setRenderState(ms);
        box.setModelBound(new BoundingBox());
        box.updateModelBound();

        final Node bt1 = new Node("BoxTransform1");
        final Node bt2 = new Node("BoxTransform2");
        final Node bt3 = new Node("BoxTransform3");

        bt2.setRotation(new Matrix3().fromAngleNormalAxis(Math.PI * 0.75, new Vector3(0, 0, 1)));

        bt3.setScale(5, 3, 2);
        bt3.setTranslation(-1, -1, 0.0);
        bt3.setRotation(new Matrix3().fromAngleNormalAxis(-Math.PI / 4, new Vector3(0, 0, 1)));

        bt3.attachChild(box);
        bt2.attachChild(bt3);
        bt1.attachChild(bt2);
        _root.attachChild(bt1);
    }
}
