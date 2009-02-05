/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.intersection;

import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.scenegraph.Mesh;

/**
 * Pick data for triangle accuracy picking including sort by distance to intersection point.
 */
public class TrianglePickData extends PickData {

    private static final Logger logger = Logger.getLogger(TrianglePickData.class.getName());

    private final Vector3[] _worldTriangle = new Vector3[] { new Vector3(), new Vector3(), new Vector3() };
    private final Vector3[] _vertices = new Vector3[] { new Vector3(), new Vector3(), new Vector3() };

    public TrianglePickData(final Ray3 ray, final Mesh targetMesh, final List<Integer> targetTris,
            final boolean calcPoints) {
        super(ray, targetMesh, targetTris, false); // hard coded to false

        if (calcPoints) {
            _intersectionRecord = calculateIntersectionPoints();
            _closestDistance = _intersectionRecord.getClosestDistance();
        }
    }

    protected IntersectionRecord calculateIntersectionPoints() {
        final List<Integer> tris = getTargetTris();
        if (tris.isEmpty()) {
            _intersectionRecord = new IntersectionRecord(new double[0], new Vector3[0]);
            _closestDistance = Double.MAX_VALUE;
            return _intersectionRecord;
        }

        final double[] distances = new double[tris.size()];
        for (int i = 0; i < tris.size(); i++) {
            final int triIndex = tris.get(i);
            PickingUtil.getTriangle(_targetMesh, triIndex, _vertices);
            final double triDistanceSq = getDistanceToTriangle(_vertices, _targetMesh.getWorldTransform());
            distances[i] = triDistanceSq;
        }

        // FIXME: optimize! ugly bubble sort for now
        boolean sorted = false;
        while (!sorted) {
            sorted = true;
            for (int sort = 0; sort < distances.length - 1; sort++) {
                if (distances[sort] > distances[sort + 1]) {
                    // swap
                    sorted = false;
                    final double temp = distances[sort + 1];
                    distances[sort + 1] = distances[sort];
                    distances[sort] = temp;

                    // swap tris too
                    final int temp2 = tris.get(sort + 1);
                    tris.set(sort + 1, tris.get(sort));
                    tris.set(sort, temp2);
                }
            }
        }

        final Vector3[] positions = new Vector3[distances.length];
        for (int i = 0; i < distances.length; i++) {
            positions[i] = _ray.getDirection().multiply(distances[0], new Vector3()).addLocal(_ray.getOrigin());
        }
        _intersectionRecord = new IntersectionRecord(distances, positions);
        _closestDistance = _intersectionRecord.getClosestDistance();
        return _intersectionRecord;
    }

    private double getDistanceToTriangle(final Vector3[] triangle, final ReadOnlyTransform worldTransform) {
        // Transform triangle to world space
        for (int i = 0; i < 3; i++) {
            worldTransform.applyForward(triangle[i], _worldTriangle[i]);
        }
        // Intersection test
        final Ray3 ray = getRay();
        final Vector3 intersect = Vector3.fetchTempInstance();
        try {
            if (ray.intersects(_worldTriangle[0], _worldTriangle[1], _worldTriangle[2], intersect, true)) {
                return ray.getOrigin().distance(intersect);
            }
        } finally {
            Vector3.releaseTempInstance(intersect);
        }

        // Should not happen
        logger.warning("Couldn't detect nearest triangle intersection!");
        return Double.POSITIVE_INFINITY;
    }
}
