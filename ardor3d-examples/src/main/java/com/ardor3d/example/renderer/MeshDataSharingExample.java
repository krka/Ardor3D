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

import java.util.Random;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.DataMode;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.SceneCopier;
import com.ardor3d.util.geom.SharedCopyLogic;
import com.google.inject.Inject;

public class MeshDataSharingExample extends ExampleBase {

    public static void main(final String[] args) {
        start(MeshDataSharingExample.class);
    }

    @Inject
    public MeshDataSharingExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    double counter = 0;
    int frames = 0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("TestSharedMesh");

        final Sphere sphere = new Sphere("Sphere", 8, 8, 1);
        sphere.setModelBound(new BoundingBox());
        sphere.getSceneHints().setDataMode(DataMode.VBO);

        final CullState cs = new CullState();
        cs.setCullFace(CullState.Face.Back);
        cs.setEnabled(true);
        _root.setRenderState(cs);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.Guess, true));

        sphere.setRenderState(ts);

        final Node n1 = new Node("n1");
        n1.setTranslation(new Vector3(-50, 0, -200));

        _root.attachChild(n1);

        final SharedCopyLogic sharedCopyLogic = new SharedCopyLogic();

        final Random rand = new Random(1337);
        for (int i = 0; i < 500; i++) {
            final Mesh sm = (Mesh) SceneCopier.makeCopy(sphere, sharedCopyLogic);

            sm.setTranslation(new Vector3(rand.nextDouble() * 100.0 - 50.0, rand.nextDouble() * 100.0 - 50.0, rand
                    .nextDouble() * 100.0 - 50.0));
            n1.attachChild(sm);
        }

        final Node n2 = (Node) SceneCopier.makeCopy(n1, sharedCopyLogic);
        n2.setTranslation(new Vector3(50, 0, -200));

        final MaterialState ms = new MaterialState();
        ms.setDiffuse(ColorRGBA.RED);
        n2.setRenderState(ms);

        _root.attachChild(n2);
    }
}
