/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.intersection;

import java.util.List;

import com.ardor3d.math.Vector3;
import com.ardor3d.util.Ardor3dException;

public class IntersectionRecord {

    private final double[] _distances;
    private final Vector3[] _points;
    private final List<PrimitiveKey> _primitives;

    /**
     * Instantiates a new IntersectionRecord defining the distances and points.
     * 
     * @param distances
     *            the distances of this intersection.
     * @param points
     *            the points of this intersection.
     * @throws Ardor3dException
     *             if distances.length != points.length
     */
    public IntersectionRecord(final double[] distances, final Vector3[] points) {
        this(distances, points, null);
    }

    /**
     * Instantiates a new IntersectionRecord defining the distances and points.
     * 
     * @param distances
     *            the distances of this intersection.
     * @param points
     *            the points of this intersection.
     * @param primitives
     *            the primitives at each index. May be null.
     * @throws Ardor3dException
     *             if distances.length != points.length or points.length != primitives.size() (if primitives is not
     *             null)
     */
    public IntersectionRecord(final double[] distances, final Vector3[] points, final List<PrimitiveKey> primitives) {
        if (distances.length != points.length || (primitives != null && points.length != primitives.size())) {
            throw new Ardor3dException("All arguments must have an equal number of elements.");
        }
        _distances = distances;
        _points = points;
        _primitives = primitives;
    }

    /**
     * @return the number of intersections that occurred.
     */
    public int getNumberOfIntersections() {
        if (_points == null) {
            return 0;
        }
        return _points.length;
    }

    /**
     * Returns an intersection point at a provided index.
     * 
     * @param index
     *            the index of the point to obtain.
     * @return the point at the index of the array.
     */
    public Vector3 getIntersectionPoint(final int index) {
        return _points[index];
    }

    /**
     * Returns an intersection distance at a provided index.
     * 
     * @param index
     *            the index of the distance to obtain.
     * @return the distance at the index of the array.
     */
    public double getIntersectionDistance(final int index) {
        return _distances[index];
    }

    /**
     * @param index
     *            the index of the primitive to obtain.
     * @return the primitive at the given index.
     */
    public PrimitiveKey getIntersectionPrimitive(final int index) {
        if (_primitives == null) {
            return null;
        }
        return _primitives.get(index);
    }

    /**
     * @return the smallest distance in the distance array.
     */
    public double getClosestDistance() {
        double min = Double.MAX_VALUE;
        if (_distances != null) {
            for (final double val : _distances) {
                if (val < min) {
                    min = val;
                }
            }
        }
        return min;
    }

    /**
     * @return the index in this record with the smallest relative distance or -1 if there are no distances in this
     *         record.
     */
    public int getClosestIntersection() {
        double min = Double.MAX_VALUE;
        int index = -1;
        if (_distances != null) {
            for (int i = _distances.length; --i >= 0;) {
                final double val = _distances[i];
                if (val < min) {
                    min = val;
                    index = i;
                }
            }
        }
        return index;
    }

    /**
     * @return the index in this record with the largest relative distance or -1 if there are no distances in this
     *         record.
     */
    public int getFurthestIntersection() {
        double max = Double.MIN_VALUE;
        int index = -1;
        if (_distances != null) {
            for (int i = _distances.length; --i >= 0;) {
                final double val = _distances[i];
                if (val > max) {
                    max = val;
                    index = i;
                }
            }
        }
        return index;
    }

}
