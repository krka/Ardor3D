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

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
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
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.SceneCopier;
import com.ardor3d.util.geom.SharedCopyLogic;
import com.google.inject.Inject;

@Purpose(htmlDescription = "Demonstrates using MathUtils.matrixLookAt.", //
thumbnailPath = "/com/ardor3d/example/media/thumbnails/basic_MatrixLookAtExample.jpg", //
maxHeapMemory = 64)
public class MatrixLookAtExample extends ExampleBase {

    private Mesh targetMesh;
    private final List<Mesh> boxes = new ArrayList<Mesh>();

    private double time = 0.0;

    private final Vector3 at = new Vector3(0.0, 5.0, 10.0);
    private final Vector3 up = new Vector3(0.0, 1.0, 0.0);

    private final Matrix3 rotationMatrix = new Matrix3();

    public static void main(final String[] args) {
        start(MatrixLookAtExample.class);
    }

    @Inject
    public MatrixLookAtExample(final LogicalLayer logicalLayer, final FrameHandler frameWork) {
        super(logicalLayer, frameWork);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        time += timer.getTimePerFrame();

        at.set(Math.sin(time) * 20, Math.cos(time * 1.4) * 20, 0.0);
        targetMesh.setTranslation(at);

        for (final Mesh mesh : boxes) {
            MathUtils.matrixLookAt(mesh.getWorldTranslation(), at, up, rotationMatrix);
            mesh.setRotation(rotationMatrix);
        }
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Matrix LookAt Test");

        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 0, 140));

        final Box box = new Box("Box", new Vector3(), 1, 1, 4);
        box.setModelBound(new BoundingBox());
        box.setRandomColors();

        targetMesh = new Sphere("Target", 8, 8, 2);
        // update the default bounding sphere
        targetMesh.updateModelBound();
        _root.attachChild(targetMesh);
        targetMesh.setRandomColors();

        final SharedCopyLogic sharedCopyLogic = new SharedCopyLogic();
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                final Mesh sm = (Mesh) SceneCopier.makeCopy(box, sharedCopyLogic);

                sm.setTranslation((x - 5.0) * 10.0, (y - 5.0) * 10.0, -10.0);
                _root.attachChild(sm);

                boxes.add(sm);
            }
        }

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
