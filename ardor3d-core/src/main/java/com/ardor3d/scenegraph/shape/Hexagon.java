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

import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * A regular hexagon with each triangle having side length that is given in the constructor.
 */
public class Hexagon extends Mesh {
    private static final long serialVersionUID = 1L;

    private static final int NUM_POINTS = 7;

    private static final int NUM_TRIS = 6;

    private float _sideLength;

    public Hexagon() {}

    /**
     * Hexagon Constructor instantiates a new Hexagon. This element is center on 0,0,0 with all normals pointing up. The
     * user must move and rotate for positioning.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparision purposes.
     * @param sideLength
     *            The length of all the sides of the triangles
     */
    public Hexagon(final String name, final float sideLength) {
        super(name);
        _sideLength = sideLength;
        // allocate vertices
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
        _meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(NUM_POINTS)), 0);

        _meshData.setIndexBuffer(BufferUtils.createIntBuffer(3 * NUM_TRIS));

        setVertexData();
        setIndexData();
        setTextureData();
        setNormalData();

    }

    /**
     * Vertexes are set up like this: 0__1 / \ / \ 5/__\6/__\2 \ / \ / \ /___\ / 4 3 All lines on this diagram are
     * sideLength long. Therefore, the width of the hexagon is sideLength * 2, and the height is 2 * the height of one
     * equalateral triangle with all side = sideLength which is .866
     */
    private void setVertexData() {
        _meshData.getVertexBuffer().put(-(_sideLength / 2)).put(_sideLength * 0.866f).put(0.0f);
        _meshData.getVertexBuffer().put(_sideLength / 2).put(_sideLength * 0.866f).put(0.0f);
        _meshData.getVertexBuffer().put(_sideLength).put(0.0f).put(0.0f);
        _meshData.getVertexBuffer().put(_sideLength / 2).put(-_sideLength * 0.866f).put(0.0f);
        _meshData.getVertexBuffer().put(-(_sideLength / 2)).put(-_sideLength * 0.866f).put(0.0f);
        _meshData.getVertexBuffer().put(-_sideLength).put(0.0f).put(0.0f);
        _meshData.getVertexBuffer().put(0.0f).put(0.0f).put(0.0f);
    }

    /**
     * Sets up the indexes of the mesh. These go in a clockwise fashion and thus only the 'up' side of the hex is lit
     * properly. If you wish to have to either set two sided lighting or create two hexes back-to-back
     */

    private void setIndexData() {
        _meshData.getIndexBuffer().rewind();
        // tri 1
        _meshData.getIndexBuffer().put(0);
        _meshData.getIndexBuffer().put(6);
        _meshData.getIndexBuffer().put(1);
        // tri 2
        _meshData.getIndexBuffer().put(1);
        _meshData.getIndexBuffer().put(6);
        _meshData.getIndexBuffer().put(2);
        // tri 3
        _meshData.getIndexBuffer().put(2);
        _meshData.getIndexBuffer().put(6);
        _meshData.getIndexBuffer().put(3);
        // tri 4
        _meshData.getIndexBuffer().put(3);
        _meshData.getIndexBuffer().put(6);
        _meshData.getIndexBuffer().put(4);
        // tri 5
        _meshData.getIndexBuffer().put(4);
        _meshData.getIndexBuffer().put(6);
        _meshData.getIndexBuffer().put(5);
        // tri 6
        _meshData.getIndexBuffer().put(5);
        _meshData.getIndexBuffer().put(6);
        _meshData.getIndexBuffer().put(0);
    }

    private void setTextureData() {
        _meshData.getTextureCoords(0)._coords.put(0.25f).put(0);
        _meshData.getTextureCoords(0)._coords.put(0.75f).put(0);
        _meshData.getTextureCoords(0)._coords.put(1.0f).put(0.5f);
        _meshData.getTextureCoords(0)._coords.put(0.75f).put(1.0f);
        _meshData.getTextureCoords(0)._coords.put(0.25f).put(1.0f);
        _meshData.getTextureCoords(0)._coords.put(0.0f).put(0.5f);
        _meshData.getTextureCoords(0)._coords.put(0.5f).put(0.5f);
    }

    /**
     * Sets all the default vertex normals to 'up', +1 in the Z direction.
     */
    private void setNormalData() {
        final Vector3 zAxis = new Vector3(0, 0, 1);
        for (int i = 0; i < NUM_POINTS; i++) {
            BufferUtils.setInBuffer(zAxis, _meshData.getNormalBuffer(), i);
        }
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
