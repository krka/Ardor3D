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

import com.ardor3d.math.Ray3;
import com.ardor3d.scenegraph.Mesh;

/**
 * 
 * PickData contains information about a picking operation (or Ray/Volume intersection). This data contains the mesh the
 * ray hit, the triangles it hit, and the ray itself.
 */
public class PickData {

    protected Ray3 _ray;
    protected Mesh _targetMesh;
    protected List<Integer> _targetTris;
    protected IntersectionRecord _intersectionRecord;
    protected double _closestDistance;

    public PickData(final Ray3 ray, final Mesh targetMesh, final boolean calcPoints) {
        this(ray, targetMesh, null, calcPoints);
    }

    /**
     * instantiates a new PickData object. Note: subclasses may want to make calc points null to prevent this extra
     * work.
     */
    public PickData(final Ray3 ray, final Mesh targetMesh, final List<Integer> targetTris, final boolean calcPoints) {
        _ray = ray;
        _targetMesh = targetMesh;
        _targetTris = targetTris;

        if (calcPoints) {
            _intersectionRecord = targetMesh.getWorldBound().intersectsWhere(ray);
            _closestDistance = _intersectionRecord.getClosestDistance();
        }
    }

    /**
     * 
     * <code>getTargetMesh</code> returns the geometry that was hit by the ray.
     * 
     * @return the geometry hit by the ray.
     */
    public Mesh getTargetMesh() {
        return _targetMesh;
    }

    /**
     * 
     * <code>setTargetMesh</code> sets the geometry hit by the ray.
     * 
     * @param mesh
     *            the geometry hit by the ray.
     */
    public void setTargetMesh(final Mesh mesh) {
        _targetMesh = mesh;
    }

    /**
     * @return Returns the target.
     */
    public List<Integer> getTargetTris() {
        return _targetTris;
    }

    /**
     * @param target
     *            The target to set.
     */
    public void setTargetTris(final List<Integer> target) {
        _targetTris = target;
    }

    /**
     * @return Returns the ray.
     */
    public Ray3 getRay() {
        return _ray;
    }

    /**
     * @param ray
     *            The ray to set.
     */
    public void setRay(final Ray3 ray) {
        _ray = ray;
    }

    public IntersectionRecord getIntersectionRecord() {
        return _intersectionRecord;
    }

    public double getClosestDistance() {
        return _closestDistance;
    }
}