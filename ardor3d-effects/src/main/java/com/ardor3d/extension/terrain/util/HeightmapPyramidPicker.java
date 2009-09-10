/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.util;

import com.ardor3d.extension.terrain.HeightmapPyramid;
import com.ardor3d.extension.terrain.util.AbstractBresenhamTracer.Direction;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Triangle;
import com.ardor3d.math.Vector3;

/**
 * A picking assistant to be used with HeightmapPyramid and an AbstractBresenhamTracer.
 */
public class HeightmapPyramidPicker {

    private final HeightmapPyramid _pyramid;
    private double _heightScale;
    private final AbstractBresenhamTracer _tracer;
    private double _maxDistance;

    private final Ray3 _workRay = new Ray3();
    private final Triangle _gridTriA = new Triangle(), _gridTriB = new Triangle();

    /**
     * Construct a new picker using the supplied pyramid, tracer and arguments.
     * 
     * @param pyramid
     *            the source for our height information. We will ask for height from the top of the pyramid.
     * @param heightScale
     *            a scalar affecting the height we receive from the pyramid. We will apply this scalar when creating
     *            triangles for testing.
     * @param tracer
     *            our bresenham tracer.
     * @param maxDistance
     *            the maximum distance along the pickray we will go when looking for a pick before giving up.
     */
    public HeightmapPyramidPicker(final HeightmapPyramid pyramid, final double heightScale,
            final AbstractBresenhamTracer tracer, final double maxDistance) {
        _pyramid = pyramid;
        setHeightScale(heightScale);
        _tracer = tracer;
        _maxDistance = maxDistance;
    }

    public Vector3 getTerrainIntersection(final Ray3 pickRay, final Vector3 store) {
        _workRay.set(pickRay);

        _tracer.startWalk(_workRay);

        if (_tracer.isRayPerpendicularToGrid()) {
            // no intersection
            return null;
        }

        final Vector3 intersection = store != null ? store : new Vector3();

        // walk our way along the ray, asking for intersections along the way
        while (_maxDistance > _tracer.getTotalTraveled()) {

            // check the triangles of main square for intersection.
            if (checkTriangles(_tracer.getGridLocation()[0], _tracer.getGridLocation()[1], intersection)) {
                // we found an intersection, so return that!
                return intersection;
            }

            // because of how we get our height coords, we will
            // sometimes be off be a grid spot, so we check the next
            // grid space up.
            int dx = 0, dy = 0;
            final Direction d = _tracer.getLastStepDirection();
            switch (d) {
                case PositiveX:
                case NegativeX:
                    dx = 0;
                    dy = 1;
                    break;
                case PositiveZ:
                case NegativeZ:
                    dx = 1;
                    dy = 0;
                    break;
            }

            if (checkTriangles(_tracer.getGridLocation()[0] + dx, _tracer.getGridLocation()[1] + dy, intersection)) {
                // we found an intersection, so return that!
                return intersection;
            }

            _tracer.next();
        }

        return null;
    }

    public double getMaxDistance() {
        return _maxDistance;
    }

    public void setMaxDistance(final double maxDistance) {
        _maxDistance = maxDistance;
    }

    public HeightmapPyramid getPyramid() {
        return _pyramid;
    }

    public AbstractBresenhamTracer getTracer() {
        return _tracer;
    }

    public void setHeightScale(final double heightScale) {
        _heightScale = heightScale;
    }

    public double getHeightScale() {
        return _heightScale;
    }

    /**
     * Check the two triangles of a given grid space for intersection.
     * 
     * @param gridX
     *            grid row
     * @param gridY
     *            grid column
     * @param store
     *            the store variable
     * @return true if a pick was found on these triangles.
     */
    private boolean checkTriangles(final int gridX, final int gridY, final Vector3 store) {
        if (!getTriangles(gridX, gridY)) {
            return false;
        }

        if (!_workRay.intersects(_gridTriA.getA(), _gridTriA.getB(), _gridTriA.getC(), store, true)) {
            return _workRay.intersects(_gridTriB.getA(), _gridTriB.getB(), _gridTriB.getC(), store, true);
        } else {
            return true;
        }
    }

    /**
     * Calculate the triangles (in world coordinate space) of a Pyramid that correspond to the given grid location. The
     * triangles are stored in the class fields _gridTriA and _gridTriB.
     * 
     * @param gridX
     *            grid row
     * @param gridY
     *            grid column
     * @return true if the grid point was found and can be represented as a triangle, false otherwise.
     */
    protected boolean getTriangles(final int gridX, final int gridY) {

        final double h1 = _pyramid.getHeight(0, gridX, gridY) * _heightScale;
        final double h2 = _pyramid.getHeight(0, gridX + 1, gridY) * _heightScale;
        final double h3 = _pyramid.getHeight(0, gridX, gridY + 1) * _heightScale;
        final double h4 = _pyramid.getHeight(0, gridX + 1, gridY + 1) * _heightScale;

        final Vector3 scaleVec = Vector3.fetchTempInstance();
        final Vector3 workVec = Vector3.fetchTempInstance();

        scaleVec.set(_tracer.getGridSpacing());

        // First triangle (h1, h3, h2)
        _tracer.get3DPoint(gridX, gridY, h1, workVec);
        workVec.multiplyLocal(scaleVec).addLocal(_tracer.getGridOrigin());
        _gridTriA.setA(workVec);

        _tracer.get3DPoint(gridX, gridY + 1, h3, workVec);
        workVec.multiplyLocal(scaleVec).addLocal(_tracer.getGridOrigin());
        _gridTriA.setB(workVec);

        _tracer.get3DPoint(gridX + 1, gridY, h2, workVec);
        workVec.multiplyLocal(scaleVec).addLocal(_tracer.getGridOrigin());
        _gridTriA.setC(workVec);

        // Second triangle (h2, h3, h4)
        _gridTriB.setA(_gridTriA.getC());
        _gridTriB.setB(_gridTriA.getB());

        _tracer.get3DPoint(gridX + 1, gridY + 1, h4, workVec);
        workVec.multiplyLocal(scaleVec).addLocal(_tracer.getGridOrigin());
        _gridTriB.setC(workVec);

        Vector3.releaseTempInstance(scaleVec);
        Vector3.releaseTempInstance(workVec);

        return true;
    }
}
