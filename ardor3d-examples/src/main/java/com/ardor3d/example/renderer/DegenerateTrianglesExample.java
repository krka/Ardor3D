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
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.scenegraph.Spatial.CullHint;
import com.ardor3d.scenegraph.Spatial.LightCombineMode;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import com.google.inject.Inject;

public class DegenerateTrianglesExample extends ExampleBase {

    private BasicText t;
    private boolean showDegenerateMesh = false;

    private final int xSize = 200;
    private final int ySize = 200;
    private final int totalSize = xSize * ySize;

    public static void main(final String[] args) {
        start(DegenerateTrianglesExample.class);
    }

    @Inject
    public DegenerateTrianglesExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    private double counter = 0;

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
        _canvas.setTitle("Degenerate vs MultiStrip Example");

        t = BasicText.createDefaultTextLabel("Text", "[SPACE] MultiStrip Mesh");
        t.setRenderBucketType(RenderBucketType.Ortho);
        t.setLightCombineMode(LightCombineMode.Off);
        t.setTranslation(new Vector3(0, 20, 0));
        _root.attachChild(t);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.Guess, true));

        final double maxSize = Math.max(xSize, ySize);
        final double requiredDistance = (maxSize / 2)
                / Math.tan(_canvas.getCanvasRenderer().getCamera().getFovY() * MathUtils.DEG_TO_RAD * 0.5);

        final Mesh multiStripMesh = createMultiStripMesh();
        multiStripMesh.setRenderState(ts);
        multiStripMesh.updateModelBound();
        multiStripMesh.setTranslation(-xSize * 0.5, -ySize * 0.5, -requiredDistance);
        _root.attachChild(multiStripMesh);

        final Mesh degenerateStripMesh = createDegenerateStripMesh();
        degenerateStripMesh.setRenderState(ts);
        degenerateStripMesh.updateModelBound();
        degenerateStripMesh.setTranslation(-xSize * 0.5, -ySize * 0.5, -requiredDistance);
        _root.attachChild(degenerateStripMesh);

        degenerateStripMesh.setCullHint(CullHint.Always);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final InputState inputState, final double tpf) {
                showDegenerateMesh = !showDegenerateMesh;
                if (showDegenerateMesh) {
                    t.setText("[SPACE] Degenerate Mesh");
                    multiStripMesh.setCullHint(CullHint.Always);
                    degenerateStripMesh.setCullHint(CullHint.Inherit);
                } else {
                    t.setText("[SPACE] MultiStrip Mesh");
                    multiStripMesh.setCullHint(CullHint.Inherit);
                    degenerateStripMesh.setCullHint(CullHint.Always);
                }
            }
        }));
    }

    private Mesh createMultiStripMesh() {
        final Mesh mesh = new Mesh();
        final MeshData meshData = mesh.getMeshData();

        final FloatBuffer vertexBuffer = BufferUtils.createVector3Buffer(totalSize);
        final FloatBuffer normalBuffer = BufferUtils.createVector3Buffer(totalSize);
        meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(totalSize)), 0);
        final FloatBuffer textureBuffer = meshData.getTextureCoords(0).coords;

        final IntBuffer indexBuffer = BufferUtils.createIntBuffer((ySize - 1) * xSize * 2);
        final int[] indexLengths = new int[ySize - 1];

        for (int y = 0; y < ySize; y++) {
            for (int x = 0; x < xSize; x++) {
                vertexBuffer.put(x).put(y).put(0);
                normalBuffer.put(0).put(0).put(1);
                textureBuffer.put(x).put(y);
            }
        }

        for (int y = 0; y < ySize - 1; y++) {
            for (int x = 0; x < xSize; x++) {
                final int index = y * xSize + x;
                indexBuffer.put(index);
                indexBuffer.put(index + xSize);
            }
            indexLengths[y] = xSize * 2;
        }

        meshData.setVertexBuffer(vertexBuffer);
        meshData.setNormalBuffer(normalBuffer);

        meshData.setIndexBuffer(indexBuffer);
        meshData.setIndexLengths(indexLengths);
        meshData.setIndexMode(IndexMode.TriangleStrip);

        return mesh;
    }

    private Mesh createDegenerateStripMesh() {
        final Mesh mesh = new Mesh();
        final MeshData meshData = mesh.getMeshData();

        final FloatBuffer vertexBuffer = BufferUtils.createVector3Buffer(totalSize);
        final FloatBuffer normalBuffer = BufferUtils.createVector3Buffer(totalSize);
        meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(totalSize)), 0);
        final FloatBuffer textureBuffer = meshData.getTextureCoords(0).coords;

        final IntBuffer indexBuffer = BufferUtils.createIntBuffer((ySize - 1) * xSize * 2 + (ySize - 1) * 2);

        for (int y = 0; y < ySize; y++) {
            for (int x = 0; x < xSize; x++) {
                vertexBuffer.put(x).put(y).put(0);
                normalBuffer.put(0).put(0).put(1);
                textureBuffer.put(x).put(y);
            }
        }

        for (int y = 0; y < ySize - 1; y++) {
            for (int x = 0; x < xSize; x++) {
                final int index = y * xSize + x;
                indexBuffer.put(index);
                indexBuffer.put(index + xSize);
            }

            final int index = (y + 1) * xSize;
            indexBuffer.put(index + xSize - 1);
            indexBuffer.put(index);
        }

        meshData.setVertexBuffer(vertexBuffer);
        meshData.setNormalBuffer(normalBuffer);

        meshData.setIndexBuffer(indexBuffer);
        meshData.setIndexMode(IndexMode.TriangleStrip);

        return mesh;
    }
}
