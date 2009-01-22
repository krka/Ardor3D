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
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>ShadowTriangle</code> A class that holds the edge information of a single face (triangle) of an occluder
 */
public class ShadowTriangle implements Savable {

    /**
     * <code>INVALID_TRIANGLE</code> (int) indicates that an edge is not connected
     */
    public final static int INVALID_TRIANGLE = -1;

    // The edges of the triangle
    public ShadowEdge edge1 = null;
    public ShadowEdge edge2 = null;
    public ShadowEdge edge3 = null;

    public ShadowTriangle() {
        edge1 = new ShadowEdge(0, 0);
        edge2 = new ShadowEdge(0, 0);
        edge3 = new ShadowEdge(0, 0);
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule cap = e.getCapsule(this);
        cap.write(edge1, "edge1", new ShadowEdge(0, 0));
        cap.write(edge2, "edge2", new ShadowEdge(0, 0));
        cap.write(edge3, "edge3", new ShadowEdge(0, 0));
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule cap = e.getCapsule(this);
        edge1 = (ShadowEdge) cap.readSavable("edge1", new ShadowEdge(0, 0));
        edge2 = (ShadowEdge) cap.readSavable("edge1", new ShadowEdge(0, 0));
        edge3 = (ShadowEdge) cap.readSavable("edge1", new ShadowEdge(0, 0));
    }

    public Class<? extends ShadowTriangle> getClassTag() {
        return this.getClass();
    }
}
