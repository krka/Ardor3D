/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding.core;

import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeMesh extends DaeTreeNode {
    private DaeList<DaeSource> sourceData;
    private DaeVertices vertices;
    private DaeList<DaeLines> lines;
    private DaeList<DaeLinestrips> linestrips;
    private DaeList<DaePolygons> polygons;
    private DaeList<DaePolylist> polylist;
    private DaeList<DaeTriangles> triangles;
    private DaeList<DaeTrifans> trifans;
    private DaeList<DaeTristrips> tristrips;

    public DaeList<DaeSource> getSourceData() {
        return sourceData;
    }

    public DaeVertices getVertices() {
        return vertices;
    }

    /**
     * @return the lines
     */
    public DaeList<DaeLines> getLines() {
        return lines;
    }

    /**
     * @return the linestrips
     */
    public DaeList<DaeLinestrips> getLinestrips() {
        return linestrips;
    }

    /**
     * @return the polygons
     */
    public DaeList<DaePolygons> getPolygons() {
        return polygons;
    }

    /**
     * @return the polylist
     */
    public DaeList<DaePolylist> getPolylist() {
        return polylist;
    }

    /**
     * @return the triangles
     */
    public DaeList<DaeTriangles> getTriangles() {
        return triangles;
    }

    /**
     * @return the trifans
     */
    public DaeList<DaeTrifans> getTrifans() {
        return trifans;
    }

    /**
     * @return the tristrips
     */
    public DaeList<DaeTristrips> getTristrips() {
        return tristrips;
    }
}
