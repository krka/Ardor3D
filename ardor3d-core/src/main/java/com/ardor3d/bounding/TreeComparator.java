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

    private Axis _axis;

    private Vector3 _center;

    private Mesh _mesh;

    private final Vector3[] _aCompare = new Vector3[3];

    private final Vector3[] _bCompare = new Vector3[3];

    public void setAxis(final Axis axis) {
        _axis = axis;
    }

    public void setMesh(final Mesh mesh) {
        _mesh = mesh;
    }

    public void setCenter(final Vector3 center) {
        _center = center;
    }

    public int compare(final Integer o1, final Integer o2) {
        final int a = o1;
        final int b = o2;

        if (a == b) {
            return 0;
        }

        Vector3 centerA = null;
        Vector3 centerB = null;
        PickingUtil.getTriangle(_mesh, a, _aCompare);
        PickingUtil.getTriangle(_mesh, b, _bCompare);
        centerA = _aCompare[0].addLocal(_aCompare[1].addLocal(_aCompare[2])).subtractLocal(_center);
        centerB = _bCompare[0].addLocal(_bCompare[1].addLocal(_bCompare[2])).subtractLocal(_center);

        switch (_axis) {
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
