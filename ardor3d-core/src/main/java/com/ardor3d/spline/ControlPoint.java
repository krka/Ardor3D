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

import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * ControlPoint class contains information about a curve control point, namely a point location and rotation.
 */
public class ControlPoint {

    /** @see #setPoint(ReadOnlyVector3) */
    private ReadOnlyVector3 _point;

    /** @see #setRotation(ReadOnlyQuaternion) */
    private ReadOnlyQuaternion rotation;

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

    public void setPoint(final ReadOnlyVector3 point) {
        _point = point;
    }

    public ReadOnlyVector3 getPoint() {
        return _point;
    }

    public void setRotation(final ReadOnlyQuaternion rotation) {
        this.rotation = rotation;
    }

    public ReadOnlyQuaternion getRotation() {
        return rotation;
    }

}
