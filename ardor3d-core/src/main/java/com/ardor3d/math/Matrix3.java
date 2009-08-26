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
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.Constants;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Matrix3 represents a double precision 3x3 matrix.
 * 
 * Note: some algorithms in this class were ported from Eberly, Wolfram, Game Gems and others to Java by myself and
 * others, originally for jMonkeyEngine.
 */
public class Matrix3 implements Cloneable, Savable, Externalizable, ReadOnlyMatrix3, Poolable {

    private static final long serialVersionUID = 1L;

    private static final ObjectPool<Matrix3> MAT_POOL = ObjectPool.create(Matrix3.class, Constants.maxPoolSize);

    /**
     * <pre>
     * 1, 0, 0
     * 0, 1, 0
     * 0, 0, 1
     * </pre>
     */
    public final static ReadOnlyMatrix3 IDENTITY = new Matrix3(1, 0, 0, 0, 1, 0, 0, 0, 1);

    protected final double[][] _data = new double[3][3];

    /**
     * Constructs a new, mutable matrix set to identity.
     */
    public Matrix3() {
        this(IDENTITY);
    }

    /**
     * Constructs a new, mutable matrix using the given matrix values (names are mRC = m[ROW][COL])
     * 
     * @param m00
     * @param m01
     * @param m02
     * @param m10
     * @param m11
     * @param m12
     * @param m20
     * @param m21
     * @param m22
     */
    public Matrix3(final double m00, final double m01, final double m02, final double m10, final double m11,
            final double m12, final double m20, final double m21, final double m22) {

        _data[0][0] = m00;
        _data[0][1] = m01;
        _data[0][2] = m02;
        _data[1][0] = m10;
        _data[1][1] = m11;
        _data[1][2] = m12;
        _data[2][0] = m20;
        _data[2][1] = m21;
        _data[2][2] = m22;
    }

    /**
     * Constructs a new, mutable matrix using the values from the given matrix
     * 
     * @param source
     */
    public Matrix3(final ReadOnlyMatrix3 source) {
        set(source);
    }

    /**
     * @param row
     * @param column
     * @return the value stored in this matrix at row, column.
     * @throws ArrayIndexOutOfBoundsException
     *             if row and column are not in bounds [0, 2]
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
    public Matrix3 setIdentity() {
        return set(IDENTITY);
    }

    /**
     * @return true if this matrix equals the 3x3 identity matrix
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
     *             if row and column are not in bounds [0, 2]
     */
    public Matrix3 setValue(final int row, final int column, final double value) {
        _data[row][column] = value;
        return this;
    }

    /**
     * Sets the values of this matrix to the values given.
     * 
     * @param m00
     * @param m01
     * @param m02
     * @param m10
     * @param m11
     * @param m12
     * @param m20
     * @param m21
     * @param m22
     * @return this matrix for chaining
     */
    public Matrix3 set(final double m00, final double m01, final double m02, final double m10, final double m11,
            final double m12, final double m20, final double m21, final double m22) {

        _data[0][0] = m00;
        _data[0][1] = m01;
        _data[0][2] = m02;
        _data[1][0] = m10;
        _data[1][1] = m11;
        _data[1][2] = m12;
        _data[2][0] = m20;
        _data[2][1] = m21;
        _data[2][2] = m22;

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
    public Matrix3 set(final ReadOnlyMatrix3 source) {
        // Unrolled for better performance.
        _data[0][0] = source.getValue(0, 0);
        _data[1][0] = source.getValue(1, 0);
        _data[2][0] = source.getValue(2, 0);

        _data[0][1] = source.getValue(0, 1);
        _data[1][1] = source.getValue(1, 1);
        _data[2][1] = source.getValue(2, 1);

        _data[0][2] = source.getValue(0, 2);
        _data[1][2] = source.getValue(1, 2);
        _data[2][2] = source.getValue(2, 2);

        return this;
    }

    /**
     * Sets the values of this matrix to the rotational value of the given quaternion.
     * 
     * @param quaternion
     * @return this matrix for chaining
     */
    public Matrix3 set(final ReadOnlyQuaternion quaternion) {
        return quaternion.toRotationMatrix(this);
    }

    /**
     * Sets the values of this matrix to the values of the provided double array.
     * 
     * @param source
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if source is null.
     * @throws ArrayIndexOutOfBoundsException
     *             if source array has a length less than 9.
     */
    public Matrix3 fromArray(final double[] source) {
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
     *             if source array has a length less than 9.
     */
    public Matrix3 fromArray(final double[] source, final boolean rowMajor) {
        if (rowMajor) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    _data[i][j] = source[i * 3 + j];
                }
            }
        } else {
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 3; i++) {
                    _data[i][j] = source[j * 3 + i];
                }
            }
        }
        return this;
    }

    /**
     * Replaces a column in this matrix with the values of the given vector.
     * 
     * @param columnIndex
     * @param columnData
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if columnData is null.
     * @throws ArrayIndexOutOfBoundsException
     *             if columnIndex is not in [0, 2]
     */
    public Matrix3 setColumn(final int columnIndex, final ReadOnlyVector3 columnData) {
        _data[0][columnIndex] = columnData.getX();
        _data[1][columnIndex] = columnData.getY();
        _data[2][columnIndex] = columnData.getZ();
        return this;
    }

    /**
     * Replaces a row in this matrix with the values of the given vector.
     * 
     * @param rowIndex
     * @param rowData
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if rowData is null.
     * @throws ArrayIndexOutOfBoundsException
     *             if rowIndex is not in [0, 2]
     */
    public Matrix3 setRow(final int rowIndex, final ReadOnlyVector3 rowData) {
        _data[rowIndex][0] = rowData.getX();
        _data[rowIndex][1] = rowData.getY();
        _data[rowIndex][2] = rowData.getZ();
        return this;
    }

    /**
     * Set the values of this matrix from the axes (columns) provided.
     * 
     * @param uAxis
     * @param vAxis
     * @param wAxis
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if any of the axes are null.
     */
    public Matrix3 fromAxes(final ReadOnlyVector3 uAxis, final ReadOnlyVector3 vAxis, final ReadOnlyVector3 wAxis) {
        setColumn(0, uAxis);
        setColumn(1, vAxis);
        setColumn(2, wAxis);
        return this;
    }

    /**
     * Sets this matrix to the rotation indicated by the given angle and axis of rotation. Note: This method creates an
     * object, so use fromAngleNormalAxis when possible, particularly if your axis is already normalized.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation.
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if axis is null.
     */
    public Matrix3 fromAngleAxis(final double angle, final ReadOnlyVector3 axis) {
        final Vector3 normAxis = Vector3.fetchTempInstance();
        axis.normalize(normAxis);
        fromAngleNormalAxis(angle, normAxis);
        Vector3.releaseTempInstance(normAxis);
        return this;
    }

    /**
     * Sets this matrix to the rotation indicated by the given angle and a unit-length axis of rotation.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation (already normalized).
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if axis is null.
     */
    public Matrix3 fromAngleNormalAxis(final double angle, final ReadOnlyVector3 axis) {
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
     * Updates this matrix from the given Euler rotation angles (y,r,p). Note that we are applying in order: roll,
     * pitch, yaw but we've ordered them in x, y, and z for convenience. See:
     * http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToMatrix/index.htm
     * 
     * @param yaw
     *            the Euler yaw of rotation (in radians). (aka Bank, often rot around x)
     * @param roll
     *            the Euler roll of rotation (in radians). (aka Heading, often rot around y)
     * @param pitch
     *            the Euler pitch of rotation (in radians). (aka Attitude, often rot around z)
     * @return this matrix for chaining
     */
    public Matrix3 fromAngles(final double yaw, final double roll, final double pitch) {
        final double ch = Math.cos(roll);
        final double sh = Math.sin(roll);
        final double cp = Math.cos(pitch);
        final double sp = Math.sin(pitch);
        final double cy = Math.cos(yaw);
        final double sy = Math.sin(yaw);

        _data[0][0] = ch * cp;
        _data[0][1] = sh * sy - ch * sp * cy;
        _data[0][2] = ch * sp * sy + sh * cy;
        _data[1][0] = sp;
        _data[1][1] = cp * cy;
        _data[1][2] = -cp * sy;
        _data[2][0] = -sh * cp;
        _data[2][1] = sh * sp * cy + ch * sy;
        _data[2][2] = -sh * sp * sy + ch * cy;
        return this;
    }

    /**
     * @param index
     * @param store
     *            the vector object to store the result in. if null, a new one is created.
     * @return the column specified by the index.
     * @throws IllegalArgumentException
     *             if index is not in bounds [0, 2]
     */
    public Vector3 getColumn(final int index, final Vector3 store) {
        if (index < 0 || index > 2) {
            throw new IllegalArgumentException("Illegal column index: " + index);
        }

        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        result.setX(_data[0][index]);
        result.setY(_data[1][index]);
        result.setZ(_data[2][index]);

        return result;
    }

    /**
     * @param index
     * @param store
     *            the vector object to store the result in. if null, a new one is created.
     * @return the row specified by the index.
     * @throws IllegalArgumentException
     *             if index is not in bounds [0, 2]
     */
    public Vector3 getRow(final int index, final Vector3 store) {
        if (index < 0 || index > 2) {
            throw new IllegalArgumentException("Illegal row index: " + index);
        }

        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        result.setX(_data[index][0]);
        result.setY(_data[index][1]);
        result.setZ(_data[index][2]);

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
            result = BufferUtils.createDoubleBuffer(9);
        }

        if (rowMajor) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    result.put(_data[i][j]);
                }
            }
        } else {
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 3; i++) {
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
            result = BufferUtils.createFloatBuffer(9);
        }

        if (rowMajor) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    result.put((float) _data[i][j]);
                }
            }
        } else {
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 3; i++) {
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
     *             if the store is non-null and has a length < 9
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
     *             if the store is non-null and has a length < 9
     */
    public double[] toArray(final double[] store, final boolean rowMajor) {
        double[] result = store;
        if (result == null) {
            result = new double[9];
        } else if (result.length < 9) {
            throw new IllegalArgumentException("store must be at least length 9.");
        }

        if (rowMajor) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    result[i * 3 + j] = _data[i][j];
                }
            }
        } else {
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 3; i++) {
                    result[j * 3 + i] = _data[i][j];
                }
            }
        }

        return result;
    }

    /**
     * converts this matrix to Euler rotation angles (yaw, roll, pitch). See
     * http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToEuler/index.htm
     * 
     * @param store
     *            the double[] array to store the computed angles in. If null, a new double[] will be created
     * @return the double[] array.
     * @throws IllegalArgumentException
     *             if non-null store is not at least length 3
     */
    public double[] toAngles(final double[] store) {
        double[] result = store;
        if (result == null) {
            result = new double[3];
        } else if (result.length < 3) {
            throw new IllegalArgumentException("store array must have at least three elements");
        }

        double heading, attitude, bank;
        if (_data[1][0] > 0.998) { // singularity at north pole
            heading = Math.atan2(_data[0][2], _data[2][2]);
            attitude = Math.PI / 2;
            bank = 0;
        } else if (_data[1][0] < -0.998) { // singularity at south pole
            heading = Math.atan2(_data[0][2], _data[2][2]);
            attitude = -Math.PI / 2;
            bank = 0;
        } else {
            heading = Math.atan2(-_data[2][0], _data[0][0]);
            bank = Math.atan2(-_data[1][2], _data[1][1]);
            attitude = Math.asin(_data[1][0]);
        }
        result[0] = heading;
        result[1] = attitude;
        result[2] = bank;

        return result;
    }

    /**
     * @param matrix
     * @return This matrix for chaining, modified internally to reflect multiplication against the given matrix
     * @throws NullPointerException
     *             if matrix is null
     */
    public Matrix3 multiplyLocal(final ReadOnlyMatrix3 matrix) {
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
    public Matrix3 multiply(final ReadOnlyMatrix3 matrix, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }
        final double temp00 = _data[0][0] * matrix.getValue(0, 0) + _data[0][1] * matrix.getValue(1, 0) + _data[0][2]
                * matrix.getValue(2, 0);
        final double temp01 = _data[0][0] * matrix.getValue(0, 1) + _data[0][1] * matrix.getValue(1, 1) + _data[0][2]
                * matrix.getValue(2, 1);
        final double temp02 = _data[0][0] * matrix.getValue(0, 2) + _data[0][1] * matrix.getValue(1, 2) + _data[0][2]
                * matrix.getValue(2, 2);
        final double temp10 = _data[1][0] * matrix.getValue(0, 0) + _data[1][1] * matrix.getValue(1, 0) + _data[1][2]
                * matrix.getValue(2, 0);
        final double temp11 = _data[1][0] * matrix.getValue(0, 1) + _data[1][1] * matrix.getValue(1, 1) + _data[1][2]
                * matrix.getValue(2, 1);
        final double temp12 = _data[1][0] * matrix.getValue(0, 2) + _data[1][1] * matrix.getValue(1, 2) + _data[1][2]
                * matrix.getValue(2, 2);
        final double temp20 = _data[2][0] * matrix.getValue(0, 0) + _data[2][1] * matrix.getValue(1, 0) + _data[2][2]
                * matrix.getValue(2, 0);
        final double temp21 = _data[2][0] * matrix.getValue(0, 1) + _data[2][1] * matrix.getValue(1, 1) + _data[2][2]
                * matrix.getValue(2, 1);
        final double temp22 = _data[2][0] * matrix.getValue(0, 2) + _data[2][1] * matrix.getValue(1, 2) + _data[2][2]
                * matrix.getValue(2, 2);

        result.set(temp00, temp01, temp02, temp10, temp11, temp12, temp20, temp21, temp22);

        return result;
    }

    /**
     * Multiplies the given vector by this matrix (v * M). If supplied, the result is stored into the supplied "store"
     * vector.
     * 
     * @param vec
     *            the vector to multiply this matrix by.
     * @param store
     *            a vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vec is null
     */
    public Vector3 applyPre(final ReadOnlyVector3 vec, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        final double x = vec.getX();
        final double y = vec.getY();
        final double z = vec.getZ();

        result.setX(_data[0][0] * x + _data[1][0] * y + _data[2][0] * z);
        result.setY(_data[0][1] * x + _data[1][1] * y + _data[2][1] * z);
        result.setZ(_data[0][2] * x + _data[1][2] * y + _data[2][2] * z);
        return result;
    }

    /**
     * Multiplies the given vector by this matrix (M * v). If supplied, the result is stored into the supplied "store"
     * vector.
     * 
     * @param vec
     *            the vector to multiply this matrix by.
     * @param store
     *            a vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vec is null
     */
    public Vector3 applyPost(final ReadOnlyVector3 vec, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        final double x = vec.getX();
        final double y = vec.getY();
        final double z = vec.getZ();

        result.setX(_data[0][0] * x + _data[0][1] * y + _data[0][2] * z);
        result.setY(_data[1][0] * x + _data[1][1] * y + _data[1][2] * z);
        result.setZ(_data[2][0] * x + _data[2][1] * y + _data[2][2] * z);
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
    public Matrix3 multiplyDiagonalPre(final ReadOnlyVector3 vec, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        result.set(vec.getX() * _data[0][0], vec.getX() * _data[0][1], vec.getX() * _data[0][2], vec.getY()
                * _data[1][0], vec.getY() * _data[1][1], vec.getY() * _data[1][2], vec.getZ() * _data[2][0], vec.getZ()
                * _data[2][1], vec.getZ() * _data[2][2]);

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
    public Matrix3 multiplyDiagonalPost(final ReadOnlyVector3 vec, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        result.set(vec.getX() * _data[0][0], vec.getY() * _data[0][1], vec.getZ() * _data[0][2], vec.getX()
                * _data[1][0], vec.getY() * _data[1][1], vec.getZ() * _data[1][2], vec.getX() * _data[2][0], vec.getY()
                * _data[2][1], vec.getZ() * _data[2][2]);

        return result;
    }

    /**
     * Internally scales all values of this matrix by the given scalar.
     * 
     * @param scalar
     * @return this matrix for chaining.
     */
    public Matrix3 multiplyLocal(final double scalar) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
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
    public Matrix3 add(final ReadOnlyMatrix3 matrix, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
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
    public Matrix3 addLocal(final ReadOnlyMatrix3 matrix) {
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
    public Matrix3 scale(final ReadOnlyVector3 scale, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        return result.set(_data[0][0] * scale.getX(), _data[0][1] * scale.getY(), _data[0][2] * scale.getZ(),
                _data[1][0] * scale.getX(), _data[1][1] * scale.getY(), _data[1][2] * scale.getZ(), _data[2][0]
                        * scale.getX(), _data[2][1] * scale.getY(), _data[2][2] * scale.getZ());
    }

    /**
     * Applies the given scale to this matrix values internally
     * 
     * @param scale
     * @return this matrix for chaining.
     * @throws NullPointerException
     *             if scale is null.
     */
    public Matrix3 scaleLocal(final ReadOnlyVector3 scale) {
        return set(_data[0][0] * scale.getX(), _data[0][1] * scale.getY(), _data[0][2] * scale.getZ(), _data[1][0]
                * scale.getX(), _data[1][1] * scale.getY(), _data[1][2] * scale.getZ(), _data[2][0] * scale.getX(),
                _data[2][1] * scale.getY(), _data[2][2] * scale.getZ());
    }

    /**
     * transposes this matrix as a new matrix, basically flipping it across the diagonal
     * 
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created.
     * @return this matrix for chaining.
     * @see {@link http://en.wikipedia.org/wiki/Transpose}
     */
    public Matrix3 transpose(final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        return result.set(_data[0][0], _data[1][0], _data[2][0], _data[0][1], _data[1][1], _data[2][1], _data[0][2],
                _data[1][2], _data[2][2]);
    }

    /**
     * transposes this matrix in place
     * 
     * @return this matrix for chaining.
     * @see {@link http://en.wikipedia.org/wiki/Transpose}
     */
    public Matrix3 transposeLocal() {
        final double m01 = _data[0][1];
        final double m02 = _data[0][2];
        final double m12 = _data[1][2];
        _data[0][1] = _data[1][0];
        _data[0][2] = _data[2][0];
        _data[1][2] = _data[2][1];
        _data[1][0] = m01;
        _data[2][0] = m02;
        _data[2][1] = m12;
        return this;
    }

    /**
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created.
     * @return a matrix that represents this matrix, inverted.
     * 
     *         if store is not null and is read only
     * @throws ArithmeticException
     *             if this matrix can not be inverted.
     */
    public Matrix3 invert(final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        final double det = determinant();
        if (Math.abs(det) <= MathUtils.EPSILON) {
            throw new ArithmeticException("This matrix cannot be inverted.");
        }

        final double temp00 = _data[1][1] * _data[2][2] - _data[1][2] * _data[2][1];
        final double temp01 = _data[0][2] * _data[2][1] - _data[0][1] * _data[2][2];
        final double temp02 = _data[0][1] * _data[1][2] - _data[0][2] * _data[1][1];
        final double temp10 = _data[1][2] * _data[2][0] - _data[1][0] * _data[2][2];
        final double temp11 = _data[0][0] * _data[2][2] - _data[0][2] * _data[2][0];
        final double temp12 = _data[0][2] * _data[1][0] - _data[0][0] * _data[1][2];
        final double temp20 = _data[1][0] * _data[2][1] - _data[1][1] * _data[2][0];
        final double temp21 = _data[0][1] * _data[2][0] - _data[0][0] * _data[2][1];
        final double temp22 = _data[0][0] * _data[1][1] - _data[0][1] * _data[1][0];
        result.set(temp00, temp01, temp02, temp10, temp11, temp12, temp20, temp21, temp22);
        result.multiplyLocal(1.0 / det);
        return result;
    }

    /**
     * Inverts this matrix locally.
     * 
     * @return this matrix inverted internally.
     * @throws ArithmeticException
     *             if this matrix can not be inverted.
     */
    public Matrix3 invertLocal() {
        return invert(this);
    }

    /**
     * @param store
     *            The matrix to store the result in. If null, a new matrix is created.
     * @return The adjugate, or classical adjoint, of this matrix
     * @see http://en.wikipedia.org/wiki/Adjugate_matrix
     */
    public Matrix3 adjugate(final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        final double temp00 = _data[1][1] * _data[2][2] - _data[1][2] * _data[2][1];
        final double temp01 = _data[0][2] * _data[2][1] - _data[0][1] * _data[2][2];
        final double temp02 = _data[0][1] * _data[1][2] - _data[0][2] * _data[1][1];
        final double temp10 = _data[1][2] * _data[2][0] - _data[1][0] * _data[2][2];
        final double temp11 = _data[0][0] * _data[2][2] - _data[0][2] * _data[2][0];
        final double temp12 = _data[0][2] * _data[1][0] - _data[0][0] * _data[1][2];
        final double temp20 = _data[1][0] * _data[2][1] - _data[1][1] * _data[2][0];
        final double temp21 = _data[0][1] * _data[2][0] - _data[0][0] * _data[2][1];
        final double temp22 = _data[0][0] * _data[1][1] - _data[0][1] * _data[1][0];

        return result.set(temp00, temp01, temp02, temp10, temp11, temp12, temp20, temp21, temp22);
    }

    /**
     * @return this matrix, modified to represent its adjugate, or classical adjoint
     * @see {@link http://en.wikipedia.org/wiki/Adjugate_matrix}
     */
    public Matrix3 adjugateLocal() {
        return adjugate(this);
    }

    /**
     * A function for creating a rotation matrix that rotates a vector called "start" into another vector called "end".
     * 
     * @param start
     *            normalized non-zero starting vector
     * @param end
     *            normalized non-zero ending vector
     * @return this matrix, for chaining
     * @see "Tomas Möller, John Hughes \"Efficiently Building a Matrix to Rotate \ One Vector to
     *      Another\" Journal of Graphics Tools, 4(4):1-4, 1999"
     */
    public Matrix3 fromStartEndLocal(final ReadOnlyVector3 start, final ReadOnlyVector3 end) {
        final Vector3 v = new Vector3();
        double e, h, f;

        start.cross(end, v);
        e = start.dot(end);
        f = (e < 0) ? -e : e;

        // if "from" and "to" vectors are nearly parallel
        if (f > 1.0 - MathUtils.ZERO_TOLERANCE) {
            final Vector3 u = new Vector3();
            final Vector3 x = new Vector3();
            double c1, c2, c3; /* coefficients for later use */
            int i, j;

            x.setX((start.getX() > 0.0) ? start.getX() : -start.getX());
            x.setY((start.getY() > 0.0) ? start.getY() : -start.getY());
            x.setZ((start.getZ() > 0.0) ? start.getZ() : -start.getZ());

            if (x.getX() < x.getY()) {
                if (x.getX() < x.getZ()) {
                    x.set(1.0, 0.0, 0.0);
                } else {
                    x.set(0.0, 0.0, 1.0);
                }
            } else {
                if (x.getY() < x.getZ()) {
                    x.set(0.0, 1.0, 0.0);
                } else {
                    x.set(0.0, 0.0, 1.0);
                }
            }

            u.set(x).subtractLocal(start);
            v.set(x).subtractLocal(end);

            c1 = 2.0 / u.dot(u);
            c2 = 2.0 / v.dot(v);
            c3 = c1 * c2 * u.dot(v);

            for (i = 0; i < 3; i++) {
                for (j = 0; j < 3; j++) {
                    final double val = -c1 * u.getValue(i) * u.getValue(j) - c2 * v.getValue(i) * v.getValue(j) + c3
                            * v.getValue(i) * u.getValue(j);
                    setValue(i, j, val);
                }
                final double val = _data[i][i];
                setValue(i, i, val + 1.0);
            }
        } else {
            // the most common case, unless "start"="end", or "start"=-"end"
            double hvx, hvz, hvxy, hvxz, hvyz;
            h = 1.0 / (1.0 + e);
            hvx = h * v.getX();
            hvz = h * v.getZ();
            hvxy = hvx * v.getY();
            hvxz = hvx * v.getZ();
            hvyz = hvz * v.getY();
            setValue(0, 0, e + hvx * v.getX());
            setValue(0, 1, hvxy - v.getZ());
            setValue(0, 2, hvxz + v.getY());

            setValue(1, 0, hvxy + v.getZ());
            setValue(1, 1, e + h * v.getY() * v.getY());
            setValue(1, 2, hvyz - v.getX());

            setValue(2, 0, hvxz - v.getY());
            setValue(2, 1, hvyz + v.getX());
            setValue(2, 2, e + hvz * v.getZ());
        }
        return this;
    }

    /**
     * @return the determinate of this matrix
     * @see {@link http://en.wikipedia.org/wiki/Determinant}
     */
    public double determinant() {
        final double fCo00 = _data[1][1] * _data[2][2] - _data[1][2] * _data[2][1];
        final double fCo10 = _data[1][2] * _data[2][0] - _data[1][0] * _data[2][2];
        final double fCo20 = _data[1][0] * _data[2][1] - _data[1][1] * _data[2][0];
        final double fDet = _data[0][0] * fCo00 + _data[0][1] * fCo10 + _data[0][2] * fCo20;
        return fDet;
    }

    /**
     * Modifies this matrix to equal the rotation required to point the z-axis at 'direction' and the y-axis to 'up'.
     * 
     * @param direction where to 'look' at
     * @param up a vector indicating the local up direction.
     */
    public void lookAt(final ReadOnlyVector3 direction, final Vector3 up) {
        final Vector3 xAxis = Vector3.fetchTempInstance();
        final Vector3 yAxis = Vector3.fetchTempInstance();
        final Vector3 zAxis = Vector3.fetchTempInstance();
        direction.normalize(zAxis);
        up.normalize(xAxis).crossLocal(zAxis);
        zAxis.cross(xAxis, yAxis);

        fromAxes(xAxis, yAxis, zAxis);

        Vector3.releaseTempInstance(xAxis);
        Vector3.releaseTempInstance(yAxis);
        Vector3.releaseTempInstance(zAxis);
    }

    /**
     * Check a matrix... if it is null or its doubles are NaN or infinite, return false. Else return true.
     * 
     * @param matrix the vector to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final ReadOnlyMatrix3 matrix) {
        if (matrix == null) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final double val = matrix.getValue(i, j);
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return the string representation of this matrix.
     */
    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer("com.ardor3d.math.Matrix3\n[\n");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
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

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final long val = Double.doubleToLongBits(_data[i][j]);
                result += 31 * result + (int) (val ^ (val >>> 32));
            }
        }

        return result;
    }

    /**
     * @param o the object to compare for equality
     * @return true if this matrix and the provided matrix have the same double values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyMatrix3)) {
            return false;
        }
        final ReadOnlyMatrix3 comp = (ReadOnlyMatrix3) o;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
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
    public Matrix3 clone() {
        try {
            return (Matrix3) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends Matrix3> getClassTag() {
        return this.getClass();
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                capsule.write(_data[i][j], ("m" + i) + j, IDENTITY.getValue(i, j));
            }
        }
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
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
     * @param in ObjectInput
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                _data[i][j] = in.readDouble();
            }
        }
    }

    /**
     * Used with serialization. Not to be called manually.
     * 
     * @param out ObjectOutput
     * @throws IOException
     */
    public void writeExternal(final ObjectOutput out) throws IOException {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                out.writeDouble(_data[i][j]);
            }
        }
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Matrix3 that is intended for temporary use in calculations and so forth. Multiple calls to
     *         the method should return instances of this class that are not currently in use.
     */
    public final static Matrix3 fetchTempInstance() {
        if (Constants.useMathPools) {
            return MAT_POOL.fetch();
        } else {
            return new Matrix3();
        }
    }

    /**
     * Releases a Matrix3 back to be used by a future call to fetchTempInstance. TAKE CARE: this Matrix3 object should
     * no longer have other classes referencing it or "Bad Things" will happen.
     * 
     * @param mat the Matrix3 to release.
     */
    public final static void releaseTempInstance(final Matrix3 mat) {
        if (Constants.useMathPools) {
            MAT_POOL.release(mat);
        }
    }
}
