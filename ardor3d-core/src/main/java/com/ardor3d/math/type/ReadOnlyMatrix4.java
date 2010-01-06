/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.type;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Vector4;

public interface ReadOnlyMatrix4 {

    public double getValue(final int row, final int column);

    public float getValuef(final int row, final int column);

    public boolean isIdentity();

    public Vector4 getColumn(final int index, final Vector4 store);

    public Vector4 getRow(final int index, final Vector4 store);

    public DoubleBuffer toDoubleBuffer(final DoubleBuffer store);

    public DoubleBuffer toDoubleBuffer(final DoubleBuffer store, final boolean rowMajor);

    public FloatBuffer toFloatBuffer(final FloatBuffer store);

    public FloatBuffer toFloatBuffer(final FloatBuffer store, final boolean rowMajor);

    public double[] toArray(final double[] store);

    public double[] toArray(final double[] store, final boolean rowMajor);

    public Matrix4 multiply(final ReadOnlyMatrix4 matrix, final Matrix4 store);

    public Vector4 applyPre(final ReadOnlyVector4 vec, final Vector4 store);

    public Vector4 applyPost(final ReadOnlyVector4 vec, final Vector4 store);

    public Matrix4 multiplyDiagonalPre(final ReadOnlyVector4 vec, final Matrix4 store);

    public Matrix4 multiplyDiagonalPost(final ReadOnlyVector4 vec, final Matrix4 store);

    public Matrix4 add(final ReadOnlyMatrix4 matrix, final Matrix4 store);

    public Matrix4 scale(final ReadOnlyVector4 scale, final Matrix4 store);

    public Matrix4 transpose(final Matrix4 store);

    public Matrix4 invert(final Matrix4 store);

    public Matrix4 adjugate(final Matrix4 store);

    public double determinant();
}
