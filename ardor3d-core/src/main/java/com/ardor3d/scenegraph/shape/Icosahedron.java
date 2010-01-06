/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

public class Icosahedron extends Mesh {
    private static final long serialVersionUID = 1L;

    private static final int NUM_POINTS = 12;
    private static final int NUM_TRIS = 20;

    private double _sideLength;

    public Icosahedron() {}

    /**
     * Creates an Icosahedron (think of 20-sided dice) with center at the origin. The length of the sides will be as
     * specified in sideLength.
     * 
     * @param name
     *            The name of the Icosahedron.
     * @param sideLength
     *            The length of each side of the Icosahedron.
     */
    public Icosahedron(final String name, final double sideLength) {
        super(name);
        _sideLength = sideLength;

        // allocate vertices
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(NUM_POINTS), 0);

        _meshData.setIndexBuffer(BufferUtils.createIntBuffer(3 * NUM_TRIS));

        setVertexData();
        setNormalData();
        setTextureData();
        setIndexData();

    }

    private void setIndexData() {
        final IntBuffer indices = _meshData.getIndexBuffer();
        indices.rewind();
        indices.put(0).put(8).put(4);
        indices.put(0).put(5).put(10);
        indices.put(2).put(4).put(9);
        indices.put(2).put(11).put(5);
        indices.put(1).put(6).put(8);
        indices.put(1).put(10).put(7);
        indices.put(3).put(9).put(6);
        indices.put(3).put(7).put(11);
        indices.put(0).put(10).put(8);
        indices.put(1).put(8).put(10);
        indices.put(2).put(9).put(11);
        indices.put(3).put(11).put(9);
        indices.put(4).put(2).put(0);
        indices.put(5).put(0).put(2);
        indices.put(6).put(1).put(3);
        indices.put(7).put(3).put(1);
        indices.put(8).put(6).put(4);
        indices.put(9).put(4).put(6);
        indices.put(10).put(5).put(7);
        indices.put(11).put(7).put(5);

        if (!true) { // outside view
            for (int i = 0; i < NUM_TRIS; i++) {
                final int iSave = _meshData.getIndexBuffer().get(3 * i + 1);
                _meshData.getIndexBuffer().put(3 * i + 1, _meshData.getIndexBuffer().get(3 * i + 2));
                _meshData.getIndexBuffer().put(3 * i + 2, iSave);
            }
        }

    }

    private void setTextureData() {
        final Vector2 tex = new Vector2();
        final Vector3 vert = new Vector3();
        for (int i = 0; i < NUM_POINTS; i++) {
            BufferUtils.populateFromBuffer(vert, _meshData.getVertexBuffer(), i);
            if (Math.abs(vert.getZ()) < _sideLength) {
                tex.setX(0.5 * (1.0 + Math.atan2(vert.getY(), vert.getX()) * MathUtils.INV_PI));
            } else {
                tex.setX(0.5);
            }
            tex.setY(Math.acos(vert.getZ() / _sideLength) * MathUtils.INV_PI);

            _meshData.getTextureCoords(0).getBuffer().put((float) tex.getX()).put((float) tex.getY());
        }
    }

    private void setNormalData() {
        final Vector3 norm = new Vector3();
        for (int i = 0; i < NUM_POINTS; i++) {
            BufferUtils.populateFromBuffer(norm, _meshData.getVertexBuffer(), i);
            norm.normalizeLocal();
            BufferUtils.setInBuffer(norm, _meshData.getNormalBuffer(), i);
        }
    }

    private void setVertexData() {
        final double dGoldenRatio = 0.5 * (1.0 + Math.sqrt(5.0));
        final double dInvRoot = 1.0 / Math.sqrt(1.0 + dGoldenRatio * dGoldenRatio);
        final float dU = (float) (dGoldenRatio * dInvRoot * _sideLength);
        final float dV = (float) (dInvRoot * _sideLength);

        final FloatBuffer vbuf = _meshData.getVertexBuffer();
        vbuf.rewind();
        vbuf.put(dU).put(dV).put(0.0f);
        vbuf.put(-dU).put(dV).put(0.0f);
        vbuf.put(dU).put(-dV).put(0.0f);
        vbuf.put(-dU).put(-dV).put(0.0f);
        vbuf.put(dV).put(0.0f).put(dU);
        vbuf.put(dV).put(0.0f).put(-dU);
        vbuf.put(-dV).put(0.0f).put(dU);
        vbuf.put(-dV).put(0.0f).put(-dU);
        vbuf.put(0.0f).put(dU).put(dV);
        vbuf.put(0.0f).put(-dU).put(dV);
        vbuf.put(0.0f).put(dU).put(-dV);
        vbuf.put(0.0f).put(-dU).put(-dV);
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_sideLength, "sideLength", 0);

    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _sideLength = capsule.readInt("sideLength", 0);

    }
}
