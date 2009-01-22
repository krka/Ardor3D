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

import java.nio.DoubleBuffer;

import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;

public interface ReadOnlyTransform {
    public ReadOnlyMatrix3 getMatrix();

    public ReadOnlyVector3 getTranslation();

    public ReadOnlyVector3 getScale();

    public boolean isIdentity();

    public boolean isRotationMatrix();

    public boolean isUniformScale();

    public Vector3 applyForward(final Vector3 point);

    public Vector3 applyForward(final ReadOnlyVector3 point, final Vector3 store);

    public Vector3 applyInverse(final Vector3 point);

    public Vector3 applyInverse(final ReadOnlyVector3 point, final Vector3 store);

    public Vector3 applyForwardVector(final Vector3 vector);

    public Vector3 applyForwardVector(final ReadOnlyVector3 vector, final Vector3 store);

    public Vector3 applyInverseVector(final Vector3 vector);

    public Vector3 applyInverseVector(final ReadOnlyVector3 vector, final Vector3 store);

    public Transform multiply(final ReadOnlyTransform transformBy, final Transform store);

    public Transform invert(final Transform store);

    public Matrix4 getHomogeneousMatrix(final Matrix4 store);

    /**
     * Populates an nio double buffer with data from this transform to use as a model view matrix in OpenGL. This is
     * done as efficiently as possible, not touching positions 3, 7, 11 and 15.
     * 
     * @param store
     *            double buffer to store in. Assumes a size of 16 and that position 3, 7 and 11 are already set as 0.0
     *            and 15 is already 1.0.
     */
    public void getGLApplyMatrix(DoubleBuffer store);

}
