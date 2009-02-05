/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.collision;

import java.nio.FloatBuffer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.CollisionTree;
import com.ardor3d.bounding.CollisionTreeManager;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.intersection.CollisionData;
import com.ardor3d.intersection.CollisionResults;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.TriangleCollisionResults;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Controller;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.shape.PQTorus;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.geom.BufferUtils;
import com.google.inject.Inject;

public class CollisionTreeExample extends ExampleBase {
    private final ReadOnlyColorRGBA[] colorSpread = { ColorRGBA.WHITE, ColorRGBA.GREEN, ColorRGBA.GRAY };

    private Mesh sphere, torus;
    private Node sphereNode, torusNode;

    private CollisionResults results;
    private CollisionData oldData;

    private int updateCounter = 0;

    public static void main(final String[] args) {
        start(CollisionTreeExample.class);
    }

    @Inject
    public CollisionTreeExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void initExample() {
        _lightState.setEnabled(false);

        CollisionTreeManager.getInstance().setTreeType(CollisionTree.Type.AABB);

        results = new TriangleCollisionResults();
        sphere = new Sphere("sphere", 10, 10, 2);

        sphere.setSolidColor(ColorRGBA.WHITE);
        sphere.setModelBound(new BoundingBox());
        sphere.updateModelBound();

        sphereNode = new Node("sphere node");

        torus = new PQTorus("torus", 5, 4, 2f, .5f, 128, 16);
        torus.setTranslation(new Vector3(0, 0, 0));
        torus.setSolidColor(ColorRGBA.WHITE);
        torus.setModelBound(new BoundingBox());
        torus.updateModelBound();

        torusNode = new Node("torus node");

        torus.addController(new Controller() {
            private static final long serialVersionUID = 1L;
            private double currentTime;

            @Override
            public void update(final double time, final Spatial caller) {
                currentTime += time * 0.2;
                final ReadOnlyVector3 t = torus.getTranslation();
                caller.setTranslation(Math.sin(currentTime) * 10.0, t.getY(), t.getZ());
            }
        });

        final FloatBuffer color1 = torus.getMeshData().getColorBuffer();
        color1.clear();
        for (int i = 0, bLength = color1.capacity(); i < bLength; i += 4) {
            final ReadOnlyColorRGBA c = colorSpread[i % 3];
            color1.put(c.getRed()).put(c.getGreen()).put(c.getBlue()).put(c.getAlpha());
        }
        color1.flip();
        final FloatBuffer color2 = sphere.getMeshData().getColorBuffer();
        color2.clear();
        for (int i = 0, bLength = color2.capacity(); i < bLength; i += 4) {
            final ReadOnlyColorRGBA c = colorSpread[i % 3];
            color2.put(c.getRed()).put(c.getGreen()).put(c.getBlue()).put(c.getAlpha());
        }
        color2.flip();

        sphereNode.attachChild(torus);
        torusNode.attachChild(sphere);

        _root.attachChild(sphereNode);
        _root.attachChild(torusNode);
    }

    @Override
    protected void updateExample(final double tpf) {
        updateCounter++;
        if (updateCounter < 5) {
            return;
        }
        updateCounter = 0;

        final int[] indexBuffer = new int[3];
        if (oldData != null) {
            for (int j = 0; j < oldData.getSourceTris().size(); j++) {
                final int triIndex = oldData.getSourceTris().get(j);
                PickingUtil.getTriangle(sphere, triIndex, indexBuffer);
                final FloatBuffer color1 = sphere.getMeshData().getColorBuffer();
                BufferUtils.setInBuffer(colorSpread[indexBuffer[0] % 3], color1, indexBuffer[0]);
                BufferUtils.setInBuffer(colorSpread[indexBuffer[1] % 3], color1, indexBuffer[1]);
                BufferUtils.setInBuffer(colorSpread[indexBuffer[2] % 3], color1, indexBuffer[2]);
            }

            for (int j = 0; j < oldData.getTargetTris().size(); j++) {
                final int triIndex = oldData.getTargetTris().get(j);
                PickingUtil.getTriangle(torus, triIndex, indexBuffer);
                final FloatBuffer color2 = torus.getMeshData().getColorBuffer();
                BufferUtils.setInBuffer(colorSpread[indexBuffer[0] % 3], color2, indexBuffer[0]);
                BufferUtils.setInBuffer(colorSpread[indexBuffer[1] % 3], color2, indexBuffer[1]);
                BufferUtils.setInBuffer(colorSpread[indexBuffer[2] % 3], color2, indexBuffer[2]);
            }
        }

        results.clear();
        PickingUtil.findCollisions(torusNode, sphereNode, results);

        if (results.getNumber() > 0) {
            oldData = results.getCollisionData(0);
            for (int i = 0; i < oldData.getSourceTris().size(); i++) {
                final FloatBuffer color1 = sphere.getMeshData().getColorBuffer();
                final int triIndex = oldData.getSourceTris().get(i);
                PickingUtil.getTriangle(sphere, triIndex, indexBuffer);
                BufferUtils.setInBuffer(ColorRGBA.RED, color1, indexBuffer[0]);
                BufferUtils.setInBuffer(ColorRGBA.RED, color1, indexBuffer[1]);
                BufferUtils.setInBuffer(ColorRGBA.RED, color1, indexBuffer[2]);
            }

            for (int i = 0; i < oldData.getTargetTris().size(); i++) {
                final int triIndex = oldData.getTargetTris().get(i);
                final FloatBuffer color2 = torus.getMeshData().getColorBuffer();
                PickingUtil.getTriangle(torus, triIndex, indexBuffer);
                BufferUtils.setInBuffer(ColorRGBA.BLUE, color2, indexBuffer[0]);
                BufferUtils.setInBuffer(ColorRGBA.BLUE, color2, indexBuffer[1]);
                BufferUtils.setInBuffer(ColorRGBA.BLUE, color2, indexBuffer[2]);
            }
        }
    }
}