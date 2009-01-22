/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

public enum IndexMode {
    // TRIMESH
    /**
     * Every three vertices referenced by the indexbuffer will be considered a stand-alone triangle.
     */
    Triangles(true),
    /**
     * The first three vertices referenced by the indexbuffer create a triangle, from there, every additional vertex is
     * paired with the two preceding vertices to make a new triangle.
     */
    TriangleStrip(true),
    /**
     * The first three vertices (V0, V1, V2) referenced by the indexbuffer create a triangle, from there, every
     * additional vertex is paired with the preceding vertex and the initial vertex (V0) to make a new triangle.
     */
    TriangleFan(true),

    // QUADMESH
    /**
     * Every four vertices referenced by the indexbuffer will be considered a stand-alone quad.
     */
    Quads(true),
    /**
     * The first four vertices referenced by the indexbuffer create a triangle, from there, every two additional
     * vertices are paired with the two preceding vertices to make a new quad.
     */
    QuadStrip(true),

    // LINE
    /**
     * Every two vertices referenced by the indexbuffer will be considered a stand-alone line segment.
     */
    Lines(false),
    /**
     * The first two vertices referenced by the indexbuffer create a line, from there, every additional vertex is paired
     * with the preceding vertex to make a new, connected line.
     */
    LineStrip(false),
    /**
     * Identical to <i>LineStrip</i> except the final indexed vertex is then connected back to the initial vertex to
     * form a loop.
     */
    LineLoop(false),

    // POINT
    /**
     * Identical to <i>Connected</i> except the final indexed vertex is then connected back to the initial vertex to
     * form a loop.
     */
    Points(false),

    // POLYGON
    /**
     * ...
     */
    Polygon(true);

    private final boolean _hasPolygons;

    private IndexMode(final boolean hasPolygons) {
        _hasPolygons = hasPolygons;
    }

    public boolean hasPolygons() {
        return _hasPolygons;
    }
}
