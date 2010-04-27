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

    double getValue(int row, int column);

    float getValuef(int row, int column);

    boolean isIdentity();

    Vector4 getColumn(int index, Vector4 store);

    Vector4 getRow(int index, Vector4 store);

    DoubleBuffer toDoubleBuffer(DoubleBuffer store);

    DoubleBuffer toDoubleBuffer(DoubleBuffer store, boolean rowMajor);

    FloatBuffer toFloatBuffer(FloatBuffer store);

    FloatBuffer toFloatBuffer(FloatBuffer store, boolean rowMajor);

    double[] toArray(double[] store);

    double[] toArray(double[] store, boolean rowMajor);

    Matrix4 multiply(ReadOnlyMatrix4 matrix, Matrix4 store);

    Vector4 applyPre(ReadOnlyVector4 vec, Vector4 store);

    Vector4 applyPost(ReadOnlyVector4 vec, Vector4 store);

    Matrix4 multiplyDiagonalPre(ReadOnlyVector4 vec, Matrix4 store);

    Matrix4 multiplyDiagonalPost(ReadOnlyVector4 vec, Matrix4 store);

    Matrix4 add(ReadOnlyMatrix4 matrix, Matrix4 store);

    Matrix4 scale(ReadOnlyVector4 scale, Matrix4 store);

    Matrix4 transpose(Matrix4 store);

    Matrix4 invert(Matrix4 store);

    Matrix4 adjugate(Matrix4 store);

    double determinant();
}
