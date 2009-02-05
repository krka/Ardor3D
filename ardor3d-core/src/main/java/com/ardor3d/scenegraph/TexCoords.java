/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Simple data class storing a buffer of floats and a number that indicates how many floats to group together to make up
 * a texture coordinate "tuple"
 */
public class TexCoords implements Savable {

    public FloatBuffer _coords;
    public int _perVert;

    public TexCoords() {}

    public TexCoords(final FloatBuffer coords) {
        this(coords, 2);
    }

    public TexCoords(final FloatBuffer coords, final int coordsPerVert) {
        _coords = coords;
        _perVert = coordsPerVert;
    }

    public static TexCoords makeNew(final Vector2[] coords) {
        if (coords == null) {
            return null;
        }

        return new TexCoords(BufferUtils.createFloatBuffer(coords), 2);
    }

    public static TexCoords makeNew(final Vector3[] coords) {
        if (coords == null) {
            return null;
        }

        return new TexCoords(BufferUtils.createFloatBuffer(coords), 3);
    }

    public static TexCoords makeNew(final float[] coords) {
        if (coords == null) {
            return null;
        }

        return new TexCoords(BufferUtils.createFloatBuffer(coords), 1);
    }

    /**
     * Check an incoming TexCoords object for null and correct size.
     * 
     * @param tc
     * @param vertexCount
     * @param perVert
     * @return tc if it is not null and the right size, otherwise it will be a new TexCoords object.
     */
    public static TexCoords ensureSize(final TexCoords tc, final int vertexCount, final int perVert) {
        if (tc == null) {
            return new TexCoords(BufferUtils.createFloatBuffer(vertexCount * perVert), perVert);
        }

        if (tc._coords.limit() == perVert * vertexCount && tc._perVert == perVert) {
            tc._coords.rewind();
            return tc;
        } else if (tc._coords.limit() == perVert * vertexCount) {
            tc._perVert = perVert;
        } else {
            tc._coords = BufferUtils.createFloatBuffer(vertexCount * perVert);
            tc._perVert = perVert;
        }

        return tc;
    }

    public Class<? extends TexCoords> getClassTag() {
        return getClass();
    }

    public void read(final Ardor3DImporter im) throws IOException {
        final InputCapsule cap = im.getCapsule(this);
        _coords = cap.readFloatBuffer("coords", null);
        _perVert = cap.readInt("perVert", 0);
    }

    public void write(final Ardor3DExporter ex) throws IOException {
        final OutputCapsule cap = ex.getCapsule(this);
        cap.write(_coords, "coords", null);
        cap.write(_perVert, "perVert", 0);
    }
}
