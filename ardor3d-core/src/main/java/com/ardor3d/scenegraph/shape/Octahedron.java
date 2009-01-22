/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;
import java.nio.IntBuffer;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * An eight faced polyhedron. It looks somewhat like two pyramids placed bottom to bottom.
 */
public class Octahedron extends Mesh {
    private static final long serialVersionUID = 1L;

    private static final int NUM_POINTS = 6;

    private static final int NUM_TRIS = 8;

    private double sideLength;

    public Octahedron() {}

    /**
     * Creates an octahedron with center at the origin. The lenght sides are given.
     * 
     * @param name
     *            The name of the octahedron.
     * @param sideLength
     *            The length of each side of the octahedron.
     */
    public Octahedron(final String name, final double sideLength) {
        super(name);
        this.sideLength = sideLength;

        // allocate vertices
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
        _meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(NUM_POINTS)), 0);

        _meshData.setIndexBuffer(BufferUtils.createIntBuffer(3 * NUM_TRIS));

        setVertexData();
        setNormalData();
        setTextureData();
        setIndexData();

    }

    private void setIndexData() {
        final IntBuffer indices = _meshData.getIndexBuffer();
        indices.rewind();
        indices.put(4).put(0).put(2);
        indices.put(4).put(2).put(1);
        indices.put(4).put(1).put(3);
        indices.put(4).put(3).put(0);
        indices.put(5).put(2).put(0);
        indices.put(5).put(1).put(2);
        indices.put(5).put(3).put(1);
        indices.put(5).put(0).put(3);

        if (!true) {
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
            if (Math.abs(vert.getZ()) < sideLength) {
                tex.setX(0.5 * (1.0 + Math.atan2(vert.getY(), vert.getX()) * MathUtils.INV_PI));
            } else {
                tex.setX(0.5);
            }
            tex.setY(Math.acos(vert.getZ()) * MathUtils.INV_PI);
            _meshData.getTextureCoords(0).coords.put((float) tex.getX()).put((float) tex.getY());
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
        final float floatSideLength = (float) sideLength;

        _meshData.getVertexBuffer().put(floatSideLength).put(0.0f).put(0.0f);
        _meshData.getVertexBuffer().put(-floatSideLength).put(0.0f).put(0.0f);
        _meshData.getVertexBuffer().put(0.0f).put(floatSideLength).put(0.0f);
        _meshData.getVertexBuffer().put(0.0f).put(-floatSideLength).put(0.0f);
        _meshData.getVertexBuffer().put(0.0f).put(0.0f).put(floatSideLength);
        _meshData.getVertexBuffer().put(0.0f).put(0.0f).put(-floatSideLength);
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(sideLength, "sideLength", 0);

    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        sideLength = capsule.readInt("sideLength", 0);

    }
}