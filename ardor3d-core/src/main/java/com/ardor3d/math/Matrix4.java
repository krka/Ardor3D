/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.type.ReadOnlyVector4;
import com.ardor3d.util.Constants;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Matrix4 represents a double precision 4x4 matrix and contains a flag, set at object creation, indicating if the given
 * Matrix4 object is mutable.
 * 
 * Note: some algorithms in this class were ported from Eberly, Wolfram, Game Gems and others to Java by myself and
 * others, originally for jMonkeyEngine.
 */
public class Matrix4 implements Cloneable, Savable, Externalizable, ReadOnlyMatrix4, Poolable {

    private static final long serialVersionUID = 1L;

    private static final ObjectPool<Matrix4> MAT_POOL = ObjectPool.create(Matrix4.class, Constants.maxPoolSize);

    /**
     * <pre>
     * 1, 0, 0, 0
     * 0, 1, 0, 0
     * 0, 0, 1, 0
     * 0, 0, 0, 1
     * </pre>
     */
    public final static ReadOnlyMatrix4 IDENTITY = new Matrix4(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);

    protected final double[][] _data = new double[4][4];

    /**
     * Constructs a new matrix set to identity.
     */
    public Matrix4() {
        this(IDENTITY);
    }

    /**
     * Constructs a new matrix set to the given matrix values. (names are mRC = m[ROW][COL])
     * 
     * @param m00
     * @param m01
     * @param m02
     * @param m03
     * @param m10
     * @param m11
     * @param m12
     * @param m13
     * @param m20
     * @param m21
     * @param m22
     * @param m23
     * @param m30
     * @param m31
     * @param m32
     * @param m33
     */
    public Matrix4(final double m00, final double m01, final double m02, final double m03, final double m10,
            final double m11, final double m12, final double m13, final double m20, final double m21, final double m22,
            final double m23, final double m30, final double m31, final double m32, final double m33) {

        _data[0][0] = m00;
        _data[0][1] = m01;
        _data[0][2] = m02;
        _data[0][3] = m03;
        _data[1][0] = m10;
        _data[1][1] = m11;
        _data[1][2] = m12;
        _data[1][3] = m13;
        _data[2][0] = m20;
        _data[2][1] = m21;
        _data[2][2] = m22;
        _data[2][3] = m23;
        _data[3][0] = m30;
        _data[3][1] = m31;
        _data[3][2] = m32;
        _data[3][3] = m33;
    }

    /**
     * Constructs a new matrix set to the values of the given matrix.
     * 
     * @param source
     */
    public Matrix4(final ReadOnlyMatrix4 source) {
        set(source);
    }

    /**
     * @param row
     * @param column
     * @return the value stored in this matrix at row, column.
     * @throws ArrayIndexOutOfBoundsException
     *             if row and column are not in bounds [0, 3]
     */
    public double getValue(final int row, final int column) {
        return _data[row][column];
    }

    /**
     * @param row
     * @param column
     * @return the value stored in this matrix at row, column, pre-cast to a float for convenience.
     * @throws ArrayIndexOutOfBoundsException
     *             if row and column are not in bounds [0, 2]
     */
    public float getValuef(final int row, final int column) {
        return (float) getValue(row, column);
    }

    /**
     * Same as set(IDENTITY)
     * 
     * @return this matrix for chaining
     */
    public Matrix4 setIdentity() {
        return set(IDENTITY);
    }

    /**
     * @return true if this matrix equals the 4x4 identity matrix
     */
    public boolean isIdentity() {
        return equals(IDENTITY);
    }

    /**
     * Sets the value of this matrix at row, column to the given value.
     * 
     * @param row
     * @param column
     * @param value
     * @return this matrix for chaining
     * @throws ArrayIndexOutOfBoundsException
     *             if row and column are not in bounds [0, 3]
     */
    public Matrix4 setValue(final int row, final int column, final double value) {
        _data[row][column] = value;
        return this;
    }

    /**
     * Sets the values of this matrix to the values given.
     * 
     * @param m00
     * @param m01
     * @param m02
     * @param m03
     * @param m10
     * @param m11
     * @param m12
     * @param m13
     * @param m20
     * @param m21
     * @param m22
     * @param m23
     * @param m30
     * @param m31
     * @param m32
     * @param m33
     * @return this matrix for chaining
     */
    public Matrix4 set(final double m00, final double m01, final double m02, final double m03, final double m10,
            final double m11, final double m12, final double m13, final double m20, final double m21, final double m22,
            final double m23, final double m30, final double m31, final double m32, final double m33) {

        _data[0][0] = m00;
        _data[0][1] = m01;
        _data[0][2] = m02;
        _data[0][3] = m03;
        _data[1][0] = m10;
        _data[1][1] = m11;
        _data[1][2] = m12;
        _data[1][3] = m13;
        _data[2][0] = m20;
        _data[2][1] = m21;
        _data[2][2] = m22;
        _data[2][3] = m23;
        _data[3][0] = m30;
        _data[3][1] = m31;
        _data[3][2] = m32;
        _data[3][3] = m33;

        return this;
    }

    /**
     * Sets the values of this matrix to the values of the provided source matrix.
     * 
     * @param source
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Matrix4 set(final ReadOnlyMatrix4 source) {
        // Unrolled for better performance.
        _data[0][0] = source.getValue(0, 0);
        _data[1][0] = source.getValue(1, 0);
        _data[2][0] = source.getValue(2, 0);
        _data[3][0] = source.getValue(3, 0);

        _data[0][1] = source.getValue(0, 1);
        _data[1][1] = source.getValue(1, 1);
        _data[2][1] = source.getValue(2, 1);
        _data[3][1] = source.getValue(3, 1);

        _data[0][2] = source.getValue(0, 2);
        _data[1][2] = source.getValue(1, 2);
        _data[2][2] = source.getValue(2, 2);
        _data[3][2] = source.getValue(3, 2);

        _data[0][3] = source.getValue(0, 3);
        _data[1][3] = source.getValue(1, 3);
        _data[2][3] = source.getValue(2, 3);
        _data[3][3] = source.getValue(3, 3);

        return this;
    }

    /**
     * Sets the 3x3 rotation part of this matrix to the values of the provided source matrix.
     * 
     * @param source
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Matrix4 set(final ReadOnlyMatrix3 source) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                _data[i][j] = source.getValue(i, j);
            }
        }
        return this;
    }

    /**
     * Sets the values of this matrix to the rotational value of the given quaternion. Only modifies the 3x3 rotation
     * part of this matrix.
     * 
     * @param quaternion
     * @return this matrix for chaining
     */
    public Matrix4 set(final ReadOnlyQuaternion quaternion) {
        return quaternion.toRotationMatrix(this);
    }

    /**
     * @param source
     *            the buffer to read our matrix data from.
     * @return this matrix for chaining.
     */
    public Matrix4 fromDoubleBuffer(final DoubleBuffer source) {
        return fromDoubleBuffer(source, true);
    }

    /**
     * @param source
     *            the buffer to read our matrix data from.
     * @param rowMajor
     *            if true, data is stored row by row. Otherwise it is stored column by column.
     * @return this matrix for chaining.
     */
    public Matrix4 fromDoubleBuffer(final DoubleBuffer source, final boolean rowMajor) {
        if (rowMajor) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    _data[i][j] = source.get();
                }
            }
        } else {
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 4; i++) {
                    _data[i][j] = source.get();
                }
            }
        }

        return this;
    }

    /**
     * Note: data is cast to floats.
     * 
     * @param store
     *            the buffer to read our matrix data from.
     * @return this matrix for chaining.
     */
    public Matrix4 fromFloatBuffer(final FloatBuffer source) {
        return fromFloatBuffer(source, true);
    }

    /**
     * Note: data is cast to floats.
     * 
     * @param store
     *            the buffer to read our matrix data from.
     * @param rowMajor
     *            if true, data is stored row by row. Otherwise it is stored column by column.
     * @return this matrix for chaining.
     */
    public Matrix4 fromFloatBuffer(final FloatBuffer source, final boolean rowMajor) {
        if (rowMajor) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    _data[i][j] = source.get();
                }
            }
        } else {
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 4; i++) {
                    _data[i][j] = source.get();
                }
            }
        }

        return this;
    }

    /**
     * Sets the values of this matrix to the values of the provided double array.
     * 
     * @param source
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if source is null.
     * @throws ArrayIndexOutOfBoundsException
     *             if source array has a length less than 16.
     */
    public Matrix4 fromArray(final double[] source) {
        return fromArray(source, true);
    }

    /**
     * Sets the values of this matrix to the values of the provided double array.
     * 
     * @param source
     * @param rowMajor
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if source is null.
     * @throws ArrayIndexOutOfBoundsException
     *             if source array has a length less than 16.
     */
    public Matrix4 fromArray(final double[] source, final boolean rowMajor) {
        if (rowMajor) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    _data[i][j] = source[i * 4 + j];
                }
            }
        } else {
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 4; i++) {
                    _data[i][j] = source[j * 4 + i];
                }
            }
        }
        return this;
    }

    /**
     * Replaces a column in this matrix with the values of the given array.
     * 
     * @param columnIndex
     * @param columnData
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if columnData is null.
     * @throws ArrayIndexOutOfBoundsException
     *             if columnData has a length < 4
     * @throws ArrayIndexOutOfBoundsException
     *             if columnIndex is not in [0, 3]
     */
    public Matrix4 setColumn(final int columnIndex, final double[] columnData) {
        _data[0][columnIndex] = columnData[0];
        _data[1][columnIndex] = columnData[1];
        _data[2][columnIndex] = columnData[2];
        _data[3][columnIndex] = columnData[3];
        return this;
    }

    /**
     * Replaces a row in this matrix with the values of the given array.
     * 
     * @param rowIndex
     * @param rowData
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if rowData is null.
     * @throws ArrayIndexOutOfBoundsException
     *             if rowData has a length < 4
     * @throws ArrayIndexOutOfBoundsException
     *             if rowIndex is not in [0, 3]
     */
    public Matrix4 setRow(final int rowIndex, final double[] rowData) {
        _data[rowIndex][0] = rowData[0];
        _data[rowIndex][1] = rowData[1];
        _data[rowIndex][2] = rowData[2];
        _data[rowIndex][3] = rowData[3];
        return this;
    }

    /**
     * Sets the 3x3 rotation portion of this matrix to the rotation indicated by the given angle and axis of rotation.
     * Note: This method creates an object, so use fromAngleNormalAxis when possible, particularly if your axis is
     * already normalized.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation.
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if axis is null.
     */
    public Matrix4 fromAngleAxis(final double angle, final ReadOnlyVector3 axis) {
        final Vector3 normAxis = Vector3.fetchTempInstance();
        axis.normalize(normAxis);
        fromAngleNormalAxis(angle, normAxis);
        Vector3.releaseTempInstance(normAxis);
        return this;
    }

    /**
     * Sets the 3x3 rotation portion of this matrix to the rotation indicated by the given angle and a unit-length axis
     * of rotation.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation (already normalized).
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if axis is null.
     */
    public Matrix4 fromAngleNormalAxis(final double angle, final ReadOnlyVector3 axis) {
        final double fCos = MathUtils.cos(angle);
        final double fSin = MathUtils.sin(angle);
        final double fOneMinusCos = (1.0) - fCos;
        final double fX2 = axis.getX() * axis.getX();
        final double fY2 = axis.getY() * axis.getY();
        final double fZ2 = axis.getZ() * axis.getZ();
        final double fXYM = axis.getX() * axis.getY() * fOneMinusCos;
        final double fXZM = axis.getX() * axis.getZ() * fOneMinusCos;
        final double fYZM = axis.getY() * axis.getZ() * fOneMinusCos;
        final double fXSin = axis.getX() * fSin;
        final double fYSin = axis.getY() * fSin;
        final double fZSin = axis.getZ() * fSin;

        _data[0][0] = fX2 * fOneMinusCos + fCos;
        _data[0][1] = fXYM - fZSin;
        _data[0][2] = fXZM + fYSin;
        _data[1][0] = fXYM + fZSin;
        _data[1][1] = fY2 * fOneMinusCos + fCos;
        _data[1][2] = fYZM - fXSin;
        _data[2][0] = fXZM - fYSin;
        _data[2][1] = fYZM + fXSin;
        _data[2][2] = fZ2 * fOneMinusCos + fCos;

        return this;
    }

    /**
     * @param index
     * @param store
     *            the vector to store the result in. if null, a new one is created.
     * @return the column specified by the index.
     * @throws IllegalArgumentException
     *             if index is not in bounds [0, 3]
     */
    public Vector4 getColumn(final int index, final Vector4 store) {
        if (index < 0 || index > 3) {
            throw new IllegalArgumentException("Illegal column index: " + index);
        }

        Vector4 result = store;
        if (result == null) {
            result = new Vector4();
        }

        for (int i = 0; i < 4; i++) {
            result.setValue(i, _data[i][index]);
        }

        return result;
    }

    /**
     * @param index
     * @param store
     *            the vector to store the result in. if null, a new one is created.
     * @return the row specified by the index.
     * @throws IllegalArgumentException
     *             if index is not in bounds [0, 3]
     */
    public Vector4 getRow(final int index, final Vector4 store) {
        if (index < 0 || index > 3) {
            throw new IllegalArgumentException("Illegal row index: " + index);
        }

        Vector4 result = store;
        if (result == null) {
            result = new Vector4();
        }

        for (int i = 0; i < 4; i++) {
            result.setValue(i, _data[index][i]);
        }

        return result;
    }

    /**
     * @param store
     *            the buffer to store our matrix data in. If null, a new buffer is created.
     * @return matrix data as a DoubleBuffer in row major order. The position is at the end of the inserted data.
     */
    public DoubleBuffer toDoubleBuffer(final DoubleBuffer store) {
        return toDoubleBuffer(store, true);
    }

    /**
     * @param store
     *            the buffer to store our matrix data in. If null, a new buffer is created.
     * @param rowMajor
     *            if true, data is stored row by row. Otherwise it is stored column by column.
     * @return matrix data as a DoubleBuffer in the specified order. The position is at the end of the inserted data.
     */
    public DoubleBuffer toDoubleBuffer(final DoubleBuffer store, final boolean rowMajor) {
        DoubleBuffer result = store;
        if (result == null) {
            result = BufferUtils.createDoubleBuffer(16);
        }

        if (rowMajor) {
            for (int i = 0; i < 4; i++) {
                result.put(_data[i]);
            }
        } else {
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 4; i++) {
                    result.put(_data[i][j]);
                }
            }
        }

        return result;
    }

    /**
     * Note: data is cast to floats.
     * 
     * @param store
     *            the buffer to store our matrix data in. If null, a new buffer is created.
     * @return matrix data as a FloatBuffer in row major order. The position is at the end of the inserted data.
     */
    public FloatBuffer toFloatBuffer(final FloatBuffer store) {
        return toFloatBuffer(store, true);
    }

    /**
     * Note: data is cast to floats.
     * 
     * @param store
     *            the buffer to store our matrix data in. If null, a new buffer is created.
     * @param rowMajor
     *            if true, data is stored row by row. Otherwise it is stored column by column.
     * @return matrix data as a FloatBuffer in the specified order. The position is at the end of the inserted data.
     */
    public FloatBuffer toFloatBuffer(final FloatBuffer store, final boolean rowMajor) {
        FloatBuffer result = store;
        if (result == null) {
            result = BufferUtils.createFloatBuffer(16);
        }

        if (rowMajor) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    result.put((float) _data[i][j]);
                }
            }
        } else {
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 4; i++) {
                    result.put((float) _data[i][j]);
                }
            }
        }

        return result;
    }

    /**
     * @param store
     *            the double array to store our matrix data in. If null, a new array is created.
     * @return matrix data as a double array in row major order.
     * @throws IllegalArgumentException
     *             if the store is non-null and has a length < 16
     */
    public double[] toArray(final double[] store) {
        return toArray(store, true);
    }

    /**
     * @param store
     *            the double array to store our matrix data in. If null, a new array is created.
     * @param rowMajor
     *            if true, data is stored row by row. Otherwise it is stored column by column.
     * @return matrix data as a double array in the specified order.
     * @throws IllegalArgumentException
     *             if the store is non-null and has a length < 16
     */
    public double[] toArray(final double[] store, final boolean rowMajor) {
        double[] result = store;
        if (result == null) {
            result = new double[16];
        } else if (result.length < 16) {
            throw new IllegalArgumentException("store must be at least length 16.");
        }

        if (rowMajor) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    result[i * 4 + j] = _data[i][j];
                }
            }
        } else {
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 4; i++) {
                    result[j * 4 + i] = _data[i][j];
                }
            }
        }

        return result;
    }

    /**
     * Multiplies this matrix by the diagonal matrix formed by the given vector (v^D * M). If supplied, the result is
     * stored into the supplied "store" matrix.
     * 
     * @param vec
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store matrix, or a new matrix if store is null.
     * @throws NullPointerException
     *             if vec is null
     */
    public Matrix4 multiplyDiagonalPre(final ReadOnlyVector4 vec, final Matrix4 store) {
        Matrix4 result = store;
        if (result == null) {
            result = new Matrix4();
        }

        result.set(vec.getX() * _data[0][0], vec.getX() * _data[0][1], vec.getX() * _data[0][2], vec.getX()
                * _data[0][3], vec.getY() * _data[1][0], vec.getY() * _data[1][1], vec.getY() * _data[1][2], vec.getY()
                * _data[1][3], vec.getZ() * _data[2][0], vec.getZ() * _data[2][1], vec.getZ() * _data[2][2], vec.getZ()
                * _data[2][3], vec.getW() * _data[3][0], vec.getW() * _data[3][1], vec.getW() * _data[3][2], vec.getW()
                * _data[3][3]);

        return result;
    }

    /**
     * Multiplies this matrix by the diagonal matrix formed by the given vector (M * v^D). If supplied, the result is
     * stored into the supplied "store" matrix.
     * 
     * @param vec
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store matrix, or a new matrix if store is null.
     * @throws NullPointerException
     *             if vec is null
     */
    public Matrix4 multiplyDiagonalPost(final ReadOnlyVector4 vec, final Matrix4 store) {
        Matrix4 result = store;
        if (result == null) {
            result = new Matrix4();
        }

        result.set(vec.getX() * _data[0][0], vec.getY() * _data[0][1], vec.getZ() * _data[0][2], vec.getW()
                * _data[0][3], vec.getX() * _data[1][0], vec.getY() * _data[1][1], vec.getZ() * _data[1][2], vec.getW()
                * _data[1][3], vec.getX() * _data[2][0], vec.getY() * _data[2][1], vec.getZ() * _data[2][2], vec.getW()
                * _data[2][3], vec.getX() * _data[3][0], vec.getY() * _data[3][1], vec.getZ() * _data[3][2], vec.getW()
                * _data[3][3]);

        return result;
    }

    /**
     * @param matrix
     * @return This matrix for chaining, modified internally to reflect multiplication against the given matrix
     * @throws NullPointerException
     *             if matrix is null
     */
    public Matrix4 multiplyLocal(final ReadOnlyMatrix4 matrix) {
        return multiply(matrix, this);
    }

    /**
     * @param matrix
     * @param store
     *            a matrix to store the result in. if null, a new matrix is created. It is safe for the given matrix and
     *            this parameter to be the same object.
     * @return this matrix multiplied by the given matrix.
     * @throws NullPointerException
     *             if matrix is null.
     */
    public Matrix4 multiply(final ReadOnlyMatrix4 matrix, final Matrix4 store) {
        Matrix4 result = store;
        if (result == null) {
            result = new Matrix4();
        }

        final double temp00 = _data[0][0] * matrix.getValue(0, 0) + _data[0][1] * matrix.getValue(1, 0) + _data[0][2]
                * matrix.getValue(2, 0) + _data[0][3] * matrix.getValue(3, 0);
        final double temp01 = _data[0][0] * matrix.getValue(0, 1) + _data[0][1] * matrix.getValue(1, 1) + _data[0][2]
                * matrix.getValue(2, 1) + _data[0][3] * matrix.getValue(3, 1);
        final double temp02 = _data[0][0] * matrix.getValue(0, 2) + _data[0][1] * matrix.getValue(1, 2) + _data[0][2]
                * matrix.getValue(2, 2) + _data[0][3] * matrix.getValue(3, 2);
        final double temp03 = _data[0][0] * matrix.getValue(0, 3) + _data[0][1] * matrix.getValue(1, 3) + _data[0][2]
                * matrix.getValue(2, 3) + _data[0][3] * matrix.getValue(3, 3);

        final double temp10 = _data[1][0] * matrix.getValue(0, 0) + _data[1][1] * matrix.getValue(1, 0) + _data[1][2]
                * matrix.getValue(2, 0) + _data[1][3] * matrix.getValue(3, 0);
        final double temp11 = _data[1][0] * matrix.getValue(0, 1) + _data[1][1] * matrix.getValue(1, 1) + _data[1][2]
                * matrix.getValue(2, 1) + _data[1][3] * matrix.getValue(3, 1);
        final double temp12 = _data[1][0] * matrix.getValue(0, 2) + _data[1][1] * matrix.getValue(1, 2) + _data[1][2]
                * matrix.getValue(2, 2) + _data[1][3] * matrix.getValue(3, 2);
        final double temp13 = _data[1][0] * matrix.getValue(0, 3) + _data[1][1] * matrix.getValue(1, 3) + _data[1][2]
                * matrix.getValue(2, 3) + _data[1][3] * matrix.getValue(3, 3);

        final double temp20 = _data[2][0] * matrix.getValue(0, 0) + _data[2][1] * matrix.getValue(1, 0) + _data[2][2]
                * matrix.getValue(2, 0) + _data[2][3] * matrix.getValue(3, 0);
        final double temp21 = _data[2][0] * matrix.getValue(0, 1) + _data[2][1] * matrix.getValue(1, 1) + _data[2][2]
                * matrix.getValue(2, 1) + _data[2][3] * matrix.getValue(3, 1);
        final double temp22 = _data[2][0] * matrix.getValue(0, 2) + _data[2][1] * matrix.getValue(1, 2) + _data[2][2]
                * matrix.getValue(2, 2) + _data[2][3] * matrix.getValue(3, 2);
        final double temp23 = _data[2][0] * matrix.getValue(0, 3) + _data[2][1] * matrix.getValue(1, 3) + _data[2][2]
                * matrix.getValue(2, 3) + _data[2][3] * matrix.getValue(3, 3);

        final double temp30 = _data[3][0] * matrix.getValue(0, 0) + _data[3][1] * matrix.getValue(1, 0) + _data[3][2]
                * matrix.getValue(2, 0) + _data[3][3] * matrix.getValue(3, 0);
        final double temp31 = _data[3][0] * matrix.getValue(0, 1) + _data[3][1] * matrix.getValue(1, 1) + _data[3][2]
                * matrix.getValue(2, 1) + _data[3][3] * matrix.getValue(3, 1);
        final double temp32 = _data[3][0] * matrix.getValue(0, 2) + _data[3][1] * matrix.getValue(1, 2) + _data[3][2]
                * matrix.getValue(2, 2) + _data[3][3] * matrix.getValue(3, 2);
        final double temp33 = _data[3][0] * matrix.getValue(0, 3) + _data[3][1] * matrix.getValue(1, 3) + _data[3][2]
                * matrix.getValue(2, 3) + _data[3][3] * matrix.getValue(3, 3);

        result.set(temp00, temp01, temp02, temp03, temp10, temp11, temp12, temp13, temp20, temp21, temp22, temp23,
                temp30, temp31, temp32, temp33);

        return result;
    }

    /**
     * Internally scales all values of this matrix by the given scalar.
     * 
     * @param scalar
     * @return this matrix for chaining.
     */
    public Matrix4 multiplyLocal(final double scalar) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                _data[i][j] *= scalar;
            }
        }
        return this;
    }

    /**
     * @param matrix
     *            the matrix to add to this.
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created. Note that it IS safe for
     *            matrix and store to be the same object.
     * 
     * @return the result.
     * @throws NullPointerException
     *             if matrix is null
     */
    public Matrix4 add(final ReadOnlyMatrix4 matrix, final Matrix4 store) {
        Matrix4 result = store;
        if (result == null) {
            result = new Matrix4();
        }

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result._data[i][j] += matrix.getValue(i, j);
            }
        }
        return result;
    }

    /**
     * Internally adds the values of the given matrix to this matrix.
     * 
     * @param matrix
     *            the matrix to add to this.
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if matrix is null
     */
    public Matrix4 addLocal(final Matrix4 matrix) {
        return add(matrix, this);
    }

    /**
     * Applies the given scale to this matrix and returns the result as a new matrix
     * 
     * @param scale
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created.
     * @return the new matrix
     * @throws NullPointerException
     *             if scale is null.
     */
    public Matrix4 scale(final ReadOnlyVector4 scale, final Matrix4 store) {
        Matrix4 result = store;
        if (result == null) {
            result = new Matrix4();
        }

        return result.set(_data[0][0] * scale.getX(), _data[0][1] * scale.getY(), _data[0][2] * scale.getZ(),
                _data[0][3] * scale.getW(), _data[1][0] * scale.getX(), _data[1][1] * scale.getY(), _data[1][2]
                        * scale.getZ(), _data[1][3] * scale.getW(), _data[2][0] * scale.getX(), _data[2][1]
                        * scale.getY(), _data[2][2] * scale.getZ(), _data[2][3] * scale.getW(), _data[3][0]
                        * scale.getX(), _data[3][1] * scale.getY(), _data[3][2] * scale.getZ(), _data[3][3]
                        * scale.getW());
    }

    /**
     * Applies the given scale to this matrix values internally
     * 
     * @param scale
     * @return this matrix for chaining.
     * @throws NullPointerException
     *             if scale is null.
     */
    public Matrix4 scaleLocal(final ReadOnlyVector4 scale) {
        return set(_data[0][0] * scale.getX(), _data[0][1] * scale.getY(), _data[0][2] * scale.getZ(), _data[0][3]
                * scale.getW(), _data[1][0] * scale.getX(), _data[1][1] * scale.getY(), _data[1][2] * scale.getZ(),
                _data[1][3] * scale.getW(), _data[2][0] * scale.getX(), _data[2][1] * scale.getY(), _data[2][2]
                        * scale.getZ(), _data[2][3] * scale.getW(), _data[3][0] * scale.getX(), _data[3][1]
                        * scale.getY(), _data[3][2] * scale.getZ(), _data[3][3] * scale.getW());
    }

    /**
     * transposes this matrix as a new matrix, basically flipping it across the diagonal
     * 
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created.
     * @return this matrix for chaining.
     * @see <a href="http://en.wikipedia.org/wiki/Transpose">wikipedia.org-Transpose</a>
     */
    public Matrix4 transpose(final Matrix4 store) {
        Matrix4 result = store;
        if (result == null) {
            result = new Matrix4();
        }

        return result.set(_data[0][0], _data[1][0], _data[2][0], _data[3][0], _data[0][1], _data[1][1], _data[2][1],
                _data[3][1], _data[0][2], _data[1][2], _data[2][2], _data[3][2], _data[0][3], _data[1][3], _data[2][3],
                _data[3][3]);
    }

    /**
     * transposes this matrix in place
     * 
     * @return this matrix for chaining.
     * @see <a href="http://en.wikipedia.org/wiki/Transpose">wikipedia.org-Transpose</a>
     */
    public Matrix4 transposeLocal() {
        final double m01 = _data[0][1];
        final double m02 = _data[0][2];
        final double m03 = _data[0][3];
        final double m12 = _data[1][2];
        final double m13 = _data[1][3];
        final double m23 = _data[2][3];
        _data[0][1] = _data[1][0];
        _data[0][2] = _data[2][0];
        _data[0][3] = _data[3][0];
        _data[1][2] = _data[2][1];
        _data[1][3] = _data[3][1];
        _data[2][3] = _data[3][2];
        _data[1][0] = m01;
        _data[2][0] = m02;
        _data[3][0] = m03;
        _data[2][1] = m12;
        _data[3][1] = m13;
        _data[3][2] = m23;
        return this;
    }

    /**
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created.
     * @return a matrix that represents this matrix, inverted.
     * @throws ArithmeticException
     *             if this matrix can not be inverted.
     */
    public Matrix4 invert(final Matrix4 store) {
        Matrix4 result = store;
        if (result == null) {
            result = new Matrix4();
        }

        final double dA0 = _data[0][0] * _data[1][1] - _data[0][1] * _data[1][0];
        final double dA1 = _data[0][0] * _data[1][2] - _data[0][2] * _data[1][0];
        final double dA2 = _data[0][0] * _data[1][3] - _data[0][3] * _data[1][0];
        final double dA3 = _data[0][1] * _data[1][2] - _data[0][2] * _data[1][1];
        final double dA4 = _data[0][1] * _data[1][3] - _data[0][3] * _data[1][1];
        final double dA5 = _data[0][2] * _data[1][3] - _data[0][3] * _data[1][2];
        final double dB0 = _data[2][0] * _data[3][1] - _data[2][1] * _data[3][0];
        final double dB1 = _data[2][0] * _data[3][2] - _data[2][2] * _data[3][0];
        final double dB2 = _data[2][0] * _data[3][3] - _data[2][3] * _data[3][0];
        final double dB3 = _data[2][1] * _data[3][2] - _data[2][2] * _data[3][1];
        final double dB4 = _data[2][1] * _data[3][3] - _data[2][3] * _data[3][1];
        final double dB5 = _data[2][2] * _data[3][3] - _data[2][3] * _data[3][2];
        final double det = dA0 * dB5 - dA1 * dB4 + dA2 * dB3 + dA3 * dB2 - dA4 * dB1 + dA5 * dB0;

        if (Math.abs(det) <= MathUtils.EPSILON) {
            throw new ArithmeticException("This matrix cannot be inverted");
        }

        final double temp00 = +_data[1][1] * dB5 - _data[1][2] * dB4 + _data[1][3] * dB3;
        final double temp10 = -_data[1][0] * dB5 + _data[1][2] * dB2 - _data[1][3] * dB1;
        final double temp20 = +_data[1][0] * dB4 - _data[1][1] * dB2 + _data[1][3] * dB0;
        final double temp30 = -_data[1][0] * dB3 + _data[1][1] * dB1 - _data[1][2] * dB0;
        final double temp01 = -_data[0][1] * dB5 + _data[0][2] * dB4 - _data[0][3] * dB3;
        final double temp11 = +_data[0][0] * dB5 - _data[0][2] * dB2 + _data[0][3] * dB1;
        final double temp21 = -_data[0][0] * dB4 + _data[0][1] * dB2 - _data[0][3] * dB0;
        final double temp31 = +_data[0][0] * dB3 - _data[0][1] * dB1 + _data[0][2] * dB0;
        final double temp02 = +_data[3][1] * dA5 - _data[3][2] * dA4 + _data[3][3] * dA3;
        final double temp12 = -_data[3][0] * dA5 + _data[3][2] * dA2 - _data[3][3] * dA1;
        final double temp22 = +_data[3][0] * dA4 - _data[3][1] * dA2 + _data[3][3] * dA0;
        final double temp32 = -_data[3][0] * dA3 + _data[3][1] * dA1 - _data[3][2] * dA0;
        final double temp03 = -_data[2][1] * dA5 + _data[2][2] * dA4 - _data[2][3] * dA3;
        final double temp13 = +_data[2][0] * dA5 - _data[2][2] * dA2 + _data[2][3] * dA1;
        final double temp23 = -_data[2][0] * dA4 + _data[2][1] * dA2 - _data[2][3] * dA0;
        final double temp33 = +_data[2][0] * dA3 - _data[2][1] * dA1 + _data[2][2] * dA0;

        result.set(temp00, temp01, temp02, temp03, temp10, temp11, temp12, temp13, temp20, temp21, temp22, temp23,
                temp30, temp31, temp32, temp33);
        result.multiplyLocal(1.0 / det);

        return result;
    }

    /**
     * inverts this matrix locally.
     * 
     * @return this matrix inverted internally.
     * @throws ArithmeticException
     *             if this matrix can not be inverted.
     */
    public Matrix4 invertLocal() {
        return invert(this);
    }

    /**
     * @param store
     *            The matrix to store the result in. If null, a new matrix is created.
     * @return The adjugate, or classical adjoint, of this matrix
     * @see <a href="http://en.wikipedia.org/wiki/Adjugate_matrix">wikipedia.org-Adjugate_matrix</a>
     */
    public Matrix4 adjugate(final Matrix4 store) {
        Matrix4 result = store;
        if (result == null) {
            result = new Matrix4();
        }

        final double dA0 = _data[0][0] * _data[1][1] - _data[0][1] * _data[1][0];
        final double dA1 = _data[0][0] * _data[1][2] - _data[0][2] * _data[1][0];
        final double dA2 = _data[0][0] * _data[1][3] - _data[0][3] * _data[1][0];
        final double dA3 = _data[0][1] * _data[1][2] - _data[0][2] * _data[1][1];
        final double dA4 = _data[0][1] * _data[1][3] - _data[0][3] * _data[1][1];
        final double dA5 = _data[0][2] * _data[1][3] - _data[0][3] * _data[1][2];
        final double dB0 = _data[2][0] * _data[3][1] - _data[2][1] * _data[3][0];
        final double dB1 = _data[2][0] * _data[3][2] - _data[2][2] * _data[3][0];
        final double dB2 = _data[2][0] * _data[3][3] - _data[2][3] * _data[3][0];
        final double dB3 = _data[2][1] * _data[3][2] - _data[2][2] * _data[3][1];
        final double dB4 = _data[2][1] * _data[3][3] - _data[2][3] * _data[3][1];
        final double dB5 = _data[2][2] * _data[3][3] - _data[2][3] * _data[3][2];

        final double temp00 = +_data[1][1] * dB5 - _data[1][2] * dB4 + _data[1][3] * dB3;
        final double temp10 = -_data[1][0] * dB5 + _data[1][2] * dB2 - _data[1][3] * dB1;
        final double temp20 = +_data[1][0] * dB4 - _data[1][1] * dB2 + _data[1][3] * dB0;
        final double temp30 = -_data[1][0] * dB3 + _data[1][1] * dB1 - _data[1][2] * dB0;
        final double temp01 = -_data[0][1] * dB5 + _data[0][2] * dB4 - _data[0][3] * dB3;
        final double temp11 = +_data[0][0] * dB5 - _data[0][2] * dB2 + _data[0][3] * dB1;
        final double temp21 = -_data[0][0] * dB4 + _data[0][1] * dB2 - _data[0][3] * dB0;
        final double temp31 = +_data[0][0] * dB3 - _data[0][1] * dB1 + _data[0][2] * dB0;
        final double temp02 = +_data[3][1] * dA5 - _data[3][2] * dA4 + _data[3][3] * dA3;
        final double temp12 = -_data[3][0] * dA5 + _data[3][2] * dA2 - _data[3][3] * dA1;
        final double temp22 = +_data[3][0] * dA4 - _data[3][1] * dA2 + _data[3][3] * dA0;
        final double temp32 = -_data[3][0] * dA3 + _data[3][1] * dA1 - _data[3][2] * dA0;
        final double temp03 = -_data[2][1] * dA5 + _data[2][2] * dA4 - _data[2][3] * dA3;
        final double temp13 = +_data[2][0] * dA5 - _data[2][2] * dA2 + _data[2][3] * dA1;
        final double temp23 = -_data[2][0] * dA4 + _data[2][1] * dA2 - _data[2][3] * dA0;
        final double temp33 = +_data[2][0] * dA3 - _data[2][1] * dA1 + _data[2][2] * dA0;

        return result.set(temp00, temp01, temp02, temp03, temp10, temp11, temp12, temp13, temp20, temp21, temp22,
                temp23, temp30, temp31, temp32, temp33);
    }

    /**
     * @return this matrix, modified to represent its adjugate, or classical adjoint
     * @see <a href="http://en.wikipedia.org/wiki/Adjugate_matrix">wikipedia.org-Adjugate_matrix</a>
     */
    public Matrix4 adjugateLocal() {
        return adjugate(this);
    }

    /**
     * @return the determinate of this matrix
     * @see <a href="http://en.wikipedia.org/wiki/Determinant">wikipedia.org-Determinant</a>
     */
    public double determinant() {
        final double dA0 = _data[0][0] * _data[1][1] - _data[0][1] * _data[1][0];
        final double dA1 = _data[0][0] * _data[1][2] - _data[0][2] * _data[1][0];
        final double dA2 = _data[0][0] * _data[1][3] - _data[0][3] * _data[1][0];
        final double dA3 = _data[0][1] * _data[1][2] - _data[0][2] * _data[1][1];
        final double dA4 = _data[0][1] * _data[1][3] - _data[0][3] * _data[1][1];
        final double dA5 = _data[0][2] * _data[1][3] - _data[0][3] * _data[1][2];
        final double dB0 = _data[2][0] * _data[3][1] - _data[2][1] * _data[3][0];
        final double dB1 = _data[2][0] * _data[3][2] - _data[2][2] * _data[3][0];
        final double dB2 = _data[2][0] * _data[3][3] - _data[2][3] * _data[3][0];
        final double dB3 = _data[2][1] * _data[3][2] - _data[2][2] * _data[3][1];
        final double dB4 = _data[2][1] * _data[3][3] - _data[2][3] * _data[3][1];
        final double dB5 = _data[2][2] * _data[3][3] - _data[2][3] * _data[3][2];
        return dA0 * dB5 - dA1 * dB4 + dA2 * dB3 + dA3 * dB2 - dA4 * dB1 + dA5 * dB0;
    }

    /**
     * Multiplies the given vector by this matrix (v * M). If supplied, the result is stored into the supplied "store"
     * vector.
     * 
     * @param vector
     *            the vector to multiply this matrix by.
     * @param store
     *            the vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vector and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vector is null
     */
    public Vector4 applyPre(final ReadOnlyVector4 vector, Vector4 store) {
        if (store == null) {
            store = new Vector4();
        }

        final double x = vector.getX();
        final double y = vector.getY();
        final double z = vector.getZ();
        final double w = vector.getW();

        store.setX(_data[0][0] * x + _data[1][0] * y + _data[2][0] * z + _data[3][0] * w);
        store.setY(_data[0][1] * x + _data[1][1] * y + _data[2][1] * z + _data[3][1] * w);
        store.setZ(_data[0][2] * x + _data[1][2] * y + _data[2][2] * z + _data[3][2] * w);
        store.setW(_data[0][3] * x + _data[1][3] * y + _data[2][3] * z + _data[3][3] * w);

        return store;
    }

    /**
     * Multiplies the given vector by this matrix (M * v). If supplied, the result is stored into the supplied "store"
     * vector.
     * 
     * @param vector
     *            the vector to multiply this matrix by.
     * @param store
     *            the vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vector and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vector is null
     */
    public Vector4 applyPost(final ReadOnlyVector4 vector, Vector4 store) {
        if (store == null) {
            store = new Vector4();
        }

        final double x = vector.getX();
        final double y = vector.getY();
        final double z = vector.getZ();
        final double w = vector.getW();

        store.setX(_data[0][0] * x + _data[0][1] * y + _data[0][2] * z + _data[0][3] * w);
        store.setY(_data[1][0] * x + _data[1][1] * y + _data[1][2] * z + _data[1][3] * w);
        store.setZ(_data[2][0] * x + _data[2][1] * y + _data[2][2] * z + _data[2][3] * w);
        store.setW(_data[3][0] * x + _data[3][1] * y + _data[3][2] * z + _data[3][3] * w);

        return store;
    }

    /**
     * Check a matrix... if it is null or its doubles are NaN or infinite, return false. Else return true.
     * 
     * @param matrix
     *            the vector to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final ReadOnlyMatrix4 matrix) {
        if (matrix == null) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                final double val = matrix.getValue(i, j);
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return true if this Matrix is orthonormal - its rows are orthogonal, unit vectors.
     */
    public boolean isOrthonormal() {
        final Matrix4 transposed = transpose(null);
        final Matrix4 inverted = invert(null);
        return transposed.equals(inverted);
    }

    /**
     * @return the string representation of this matrix.
     */
    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer("com.ardor3d.math.Matrix4\n[\n");
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result.append(" ");
                result.append(_data[i][j]);
            }
            result.append(" \n");
        }
        result.append("]");
        return result.toString();
    }

    /**
     * @return returns a unique code for this matrix object based on its values. If two matrices are numerically equal,
     *         they will return the same hash code value.
     */
    @Override
    public int hashCode() {
        // TODO: Probably worth caching this.
        int result = 17;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                final long val = Double.doubleToLongBits(_data[i][j]);
                result += 31 * result + (int) (val ^ (val >>> 32));
            }
        }

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this matrix and the provided matrix have the double values that are within the
     *         MathUtils.ZERO_TOLERANCE.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyMatrix4)) {
            return false;
        }
        final ReadOnlyMatrix4 comp = (ReadOnlyMatrix4) o;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (Math.abs(getValue(i, j) - comp.getValue(i, j)) > MathUtils.ZERO_TOLERANCE) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this matrix and the provided matrix have the exact same double values.
     */
    public boolean strictEquals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyMatrix4)) {
            return false;
        }
        final ReadOnlyMatrix4 comp = (ReadOnlyMatrix4) o;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (getValue(i, j) != comp.getValue(i, j)) {
                    return false;
                }
            }
        }

        return true;
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Matrix4 clone() {
        try {
            return (Matrix4) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends Matrix4> getClassTag() {
        return this.getClass();
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                capsule.write(_data[i][j], ("m" + i) + j, IDENTITY.getValue(i, j));
            }
        }
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                _data[i][j] = capsule.readDouble(("m" + i) + j, IDENTITY.getValue(i, j));
            }
        }
    }

    // /////////////////
    // Methods for Externalizable
    // /////////////////

    /**
     * Used with serialization. Not to be called manually.
     * 
     * @param in
     *            ObjectInput
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                _data[i][j] = in.readDouble();
            }
        }
    }

    /**
     * Used with serialization. Not to be called manually.
     * 
     * @param out
     *            ObjectOutput
     * @throws IOException
     */
    public void writeExternal(final ObjectOutput out) throws IOException {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                out.writeDouble(_data[i][j]);
            }
        }
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Matrix4 that is intended for temporary use in calculations and so forth. Multiple calls to
     *         the method should return instances of this class that are not currently in use.
     */
    public final static Matrix4 fetchTempInstance() {
        if (Constants.useMathPools) {
            return MAT_POOL.fetch();
        } else {
            return new Matrix4();
        }
    }

    /**
     * Releases a Matrix4 back to be used by a future call to fetchTempInstance. TAKE CARE: this Matrix4 object should
     * no longer have other classes referencing it or "Bad Things" will happen.
     * 
     * @param mat
     *            the Matrix4 to release.
     */
    public final static void releaseTempInstance(final Matrix4 mat) {
        if (Constants.useMathPools) {
            MAT_POOL.release(mat);
        }
    }
}
