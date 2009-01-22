/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.shadow.stencil;

import java.io.IOException;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.Savable;

/**
 * <code>ShadowEdge</code> Holds the indices of two points that form an edge in a ShadowTriangle
 */
public class ShadowEdge implements Savable {
    /**
     * <code>triangle</code> (int) the triangle number (in an occluder) to which the edge is connected or
     * INVALID_TRIANGLE if not connected.
     */
    public int triangle = ShadowTriangle.INVALID_TRIANGLE;

    /** The indices of the two points comprising this edge. */
    public int p0, p1;

    /**
     * @param p0
     *            the first point
     * @param p1
     *            the second point
     */
    public ShadowEdge(final int p0, final int p1) {
        this.p0 = p0;
        this.p1 = p1;
    }

    public void write(final Ardor3DExporter e) throws IOException {
        e.getCapsule(this).write(p0, "p0", 0);
        e.getCapsule(this).write(p1, "p1", 0);
        e.getCapsule(this).write(triangle, "triangle", ShadowTriangle.INVALID_TRIANGLE);
    }

    public void read(final Ardor3DImporter e) throws IOException {
        p0 = e.getCapsule(this).readInt("p0", 0);
        p1 = e.getCapsule(this).readInt("p1", 0);
        triangle = e.getCapsule(this).readInt("triangle", ShadowTriangle.INVALID_TRIANGLE);
    }

    public Class<? extends ShadowEdge> getClassTag() {
        return this.getClass();
    }
}
