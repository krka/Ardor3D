/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.bounding;

import java.util.Comparator;

import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;

public class TreeComparator implements Comparator<Integer> {
    enum Axis {
        X, Y, Z;
    }

    private Axis axis;

    private Vector3 center;

    private Mesh mesh;

    private final Vector3[] aCompare = new Vector3[3];

    private final Vector3[] bCompare = new Vector3[3];

    public void setAxis(final Axis axis) {
        this.axis = axis;
    }

    public void setMesh(final Mesh mesh) {
        this.mesh = mesh;
    }

    public void setCenter(final Vector3 center) {
        this.center = center;
    }

    public int compare(final Integer o1, final Integer o2) {
        final int a = o1;
        final int b = o2;

        if (a == b) {
            return 0;
        }

        Vector3 centerA = null;
        Vector3 centerB = null;
        PickingUtil.getTriangle(mesh, a, aCompare);
        PickingUtil.getTriangle(mesh, b, bCompare);
        centerA = aCompare[0].addLocal(aCompare[1].addLocal(aCompare[2])).subtractLocal(center);
        centerB = bCompare[0].addLocal(bCompare[1].addLocal(bCompare[2])).subtractLocal(center);

        switch (axis) {
            case X:
                if (centerA.getX() < centerB.getX()) {
                    return -1;
                }
                if (centerA.getX() > centerB.getX()) {
                    return 1;
                }
                return 0;
            case Y:
                if (centerA.getY() < centerB.getY()) {
                    return -1;
                }
                if (centerA.getY() > centerB.getY()) {
                    return 1;
                }
                return 0;
            case Z:
                if (centerA.getZ() < centerB.getZ()) {
                    return -1;
                }
                if (centerA.getZ() > centerB.getZ()) {
                    return 1;
                }
                return 0;
            default:
                return 0;
        }
    }
}
