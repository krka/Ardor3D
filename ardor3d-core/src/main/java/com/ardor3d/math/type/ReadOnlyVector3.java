/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.type;

import com.ardor3d.math.Vector3;

public interface ReadOnlyVector3 {

    public double getX();

    public double getY();

    public double getZ();

    public float getXf();

    public float getYf();

    public float getZf();

    public double getValue(final int index);

    public Vector3 add(final double x, final double y, final double z, final Vector3 store);

    public Vector3 add(final ReadOnlyVector3 source, final Vector3 store);

    public Vector3 subtract(final double x, final double y, final double z, final Vector3 store);

    public Vector3 subtract(final ReadOnlyVector3 source, final Vector3 store);

    public Vector3 multiply(final double scalar, final Vector3 store);

    public Vector3 multiply(final ReadOnlyVector3 scale, final Vector3 store);

    public Vector3 divide(final double scalar, final Vector3 store);

    public Vector3 divide(final ReadOnlyVector3 scale, final Vector3 store);

    public Vector3 scaleAdd(final double scale, final ReadOnlyVector3 add, final Vector3 store);

    public Vector3 negate(final Vector3 store);

    public Vector3 normalize(final Vector3 store);

    public Vector3 lerp(final ReadOnlyVector3 endVec, final double scalar, final Vector3 store);

    public double length();

    public double lengthSquared();

    public double distanceSquared(final double x, final double y, final double z);

    public double distanceSquared(final ReadOnlyVector3 destination);

    public double distance(final double x, final double y, final double z);

    public double distance(final ReadOnlyVector3 destination);

    public double dot(final double x, final double y, final double z);

    public double dot(final ReadOnlyVector3 vec);

    public Vector3 cross(final double x, final double y, final double z, final Vector3 store);

    public Vector3 cross(final ReadOnlyVector3 vec, final Vector3 store);

    public double determinant(final double x, final double y, final double z);

    public double determinant(final ReadOnlyVector3 vec);

    public double smallestAngleBetween(final ReadOnlyVector3 otherVector);
}
