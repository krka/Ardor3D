/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

/**
 * 
 */

package com.ardor3d.spline;

import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * ControlPoint class contains information about a curve control point, namely a point location and rotation.
 */
public class ControlPoint {

    /** @see #setPoint(ReadOnlyVector3) */
    private ReadOnlyVector3 _point = Vector3.ZERO;

    /** @see #setRotation(ReadOnlyQuaternion) */
    private ReadOnlyQuaternion rotation = Quaternion.IDENTITY;

    /**
     * Creates a new instance of <code>ControlPoint</code>.
     * 
     * @param point
     *            see {@link #setPoint(ReadOnlyVector3)}
     * @param rotation
     *            see {@link #setRotation(ReadOnlyQuaternion)}
     */
    public ControlPoint(final ReadOnlyVector3 point, final ReadOnlyQuaternion rotation) {
        super();

        setPoint(point);
        setRotation(rotation);
    }

    /**
     * @param point
     *            The new point, can not be <code>null</code>.
     * @see #getPoint()
     */
    public void setPoint(final ReadOnlyVector3 point) {
        if (null == point) {
            throw new IllegalArgumentException("point can not be null!");
        }

        _point = point;
    }

    /**
     * @return The point, will not be <code>null</code>.
     * @see #setPoint(ReadOnlyVector3)
     */
    public ReadOnlyVector3 getPoint() {
        return _point;
    }

    /**
     * @param rotation
     *            The new rotation, can not be <code>null</code>.
     * @see #getRotation()
     */
    public void setRotation(final ReadOnlyQuaternion rotation) {
        if (null == rotation) {
            throw new IllegalArgumentException("rotation can not be null!");
        }

        this.rotation = rotation;
    }

    /**
     * @return The rotation, will not be <code>null</code>.
     * @see #setRotation(ReadOnlyQuaternion)
     */
    public ReadOnlyQuaternion getRotation() {
        return rotation;
    }

}
