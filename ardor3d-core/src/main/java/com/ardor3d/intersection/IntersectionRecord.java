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

import com.ardor3d.math.Vector3;
import com.ardor3d.util.Ardor3dException;

public class IntersectionRecord {

    private double[] _distances;
    private Vector3[] _points;

    /**
     * Instantiates a new IntersectionRecord with no distances or points assigned.
     * 
     */
    public IntersectionRecord() {}

    /**
     * Instantiates a new IntersectionRecord defining the distances and points. If the size of the distance and point
     * arrays do not match, an exception is thrown.
     * 
     * @param distances
     *            the distances of this intersection.
     * @param points
     *            the points of this intersection.
     */
    public IntersectionRecord(final double[] distances, final Vector3[] points) {
        if (distances.length != points.length) {
            throw new Ardor3dException("The distances and points variables must have an equal number of elements.");
        }
        _distances = distances;
        _points = points;
    }

    /**
     * Returns the number of intersections that occurred.
     * 
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
     * Returns the smallest distance in the distance array.
     * 
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
