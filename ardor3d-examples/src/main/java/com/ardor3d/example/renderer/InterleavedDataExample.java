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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.renderer.InterleavedFormat;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import com.google.inject.Inject;

public class InterleavedDataExample extends ExampleBase {

    public static void main(final String[] args) {
        start(InterleavedDataExample.class);
    }

    @Inject
    public InterleavedDataExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    double counter = 0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        counter += timer.getTimePerFrame();
        if (counter > 1) {
            counter = 0;
            System.out.printf("%7.1f FPS\n", timer.getFrameRate());
        }
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("InterleavedDataExample");

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.Guess, true));

        final Mesh manualMesh = createManualMesh();
        manualMesh.setRenderState(ts);
        manualMesh.updateModelBound();

        manualMesh.setTranslation(-50, -50, -200);

        _root.attachChild(manualMesh);
    }

    private Mesh createManualMesh() {
        final Mesh mesh = new Mesh();
        final MeshData meshData = mesh.getMeshData();

        final int xSize = 100;
        final int ySize = 100;
        final int totalSize = xSize * ySize;

        final FloatBuffer interleavedBuffer = BufferUtils.createFloatBuffer(totalSize * 6 * 3 + totalSize * 6 * 3
                + totalSize * 6 * 2);
        final IntBuffer indexBuffer = BufferUtils.createIntBuffer(totalSize * 6);

        for (int y = 0; y < ySize; y++) {
            for (int x = 0; x < xSize; x++) {
                interleavedBuffer.put(0).put(0);
                interleavedBuffer.put(0).put(0).put(1);
                interleavedBuffer.put(x).put(y).put(0);

                interleavedBuffer.put(1).put(0);
                interleavedBuffer.put(0).put(0).put(1);
                interleavedBuffer.put(x + 1).put(y).put(0);

                interleavedBuffer.put(0).put(1);
                interleavedBuffer.put(0).put(0).put(1);
                interleavedBuffer.put(x).put(y + 1).put(0);

                interleavedBuffer.put(1).put(0);
                interleavedBuffer.put(0).put(0).put(1);
                interleavedBuffer.put(x + 1).put(y).put(0);

                interleavedBuffer.put(1).put(1);
                interleavedBuffer.put(0).put(0).put(1);
                interleavedBuffer.put(x + 1).put(y + 1).put(0);

                interleavedBuffer.put(0).put(1);
                interleavedBuffer.put(0).put(0).put(1);
                interleavedBuffer.put(x).put(y + 1).put(0);
            }
        }

        for (int index = 0; index < totalSize * 6; index++) {
            indexBuffer.put(index);
        }

        meshData.setInterleavedBuffer(interleavedBuffer);
        meshData.setInterleavedFormat(InterleavedFormat.GL_T2F_N3F_V3F);

        meshData.setIndexBuffer(indexBuffer);

        return mesh;
    }
}
