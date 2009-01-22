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

import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.Debug;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.pool.ObjectPool;

/**
 * Transform models a transformation in 3d space as: Y = M*X+T, with M being a Matrix3 and T is a Vector3. Generally M
 * will be a rotation only matrix in which case it is represented by the matrix and scale fields as R*S, where S is a
 * positive scale vector. For non-uniform scales and reflections, use setMatrix, which will consider M as being a
 * general 3x3 matrix and disregard anything set in scale.
 */
public class Transform implements Cloneable, Savable, Externalizable, ReadOnlyTransform {

    private static final long serialVersionUID = 1L;

    private static final TransformPool TRANS_POOL = new TransformPool(11);

    /**
     * Identity transform.
     */
    public static final ReadOnlyTransform IDENTITY = new Transform(Matrix3.IDENTITY, Vector3.XYZ_ONE, Vector3.ZERO,
            true, true, true);

    protected final Matrix3 _matrix = new Matrix3(Matrix3.IDENTITY);
    protected final Vector3 _translation = new Vector3(Vector3.ZERO);
    protected final Vector3 _scale = new Vector3(Vector3.XYZ_ONE);

    /**
     * true if this transform is guaranteed to be the identity matrix.
     */
    protected boolean _identity;

    /**
     * true if the matrix portion of this transform is only rotation.
     */
    protected boolean _rotationMatrix;

    /**
     * true if scale is used and scale is guaranteed to be uniform.
     */
    protected boolean _uniformScale;

    /**
     * Constructs a new Transform object.
     */
    public Transform() {
        _identity = true;
        _rotationMatrix = true;
        _uniformScale = true;

    }

    /**
     * Constructs a new Transform object from the information stored in the given source Transform.
     * 
     * @param source
     * @throws NullPointerException
     *             if source is null.
     */
    public Transform(final ReadOnlyTransform source) {
        _matrix.set(source.getMatrix());
        _scale.set(source.getScale());
        _translation.set(source.getTranslation());

        _identity = source.isIdentity();
        _rotationMatrix = source.isRotationMatrix();
        _uniformScale = source.isUniformScale();

    }

    /**
     * Internal only constructor, generally used for making an immutable transform.
     * 
     * @param matrix
     * @param scale
     * @param translation
     * @param identity
     * @param rotationMatrix
     * @param uniformScale
     * @throws NullPointerException
     *             if a param is null.
     */
    protected Transform(final ReadOnlyMatrix3 matrix, final ReadOnlyVector3 scale, final ReadOnlyVector3 translation,
            final boolean identity, final boolean rotationMatrix, final boolean uniformScale) {
        _matrix.set(matrix);
        _scale.set(scale);
        _translation.set(translation);

        _identity = identity;
        _rotationMatrix = rotationMatrix;
        _uniformScale = uniformScale;
    }

    public ReadOnlyMatrix3 getMatrix() {
        return _matrix;
    }

    public ReadOnlyVector3 getTranslation() {
        return _translation;
    }

    public ReadOnlyVector3 getScale() {
        return _scale;
    }

    /**
     * @return true if this transform is guaranteed to be the identity matrix.
     */
    public boolean isIdentity() {
        return _identity;
    }

    /**
     * @return true if the matrix portion of this transform is only rotation.
     */
    public boolean isRotationMatrix() {
        return _rotationMatrix;
    }

    /**
     * @return true if scale is used and scale is guaranteed to be uniform.
     */
    public boolean isUniformScale() {
        return _uniformScale;
    }

    /**
     * Resets this transform to identity and resets all flags.
     */
    public void setIdentity() {
        _matrix.set(Matrix3.IDENTITY);
        _scale.set(Vector3.XYZ_ONE);
        _translation.set(Vector3.ZERO);
        _identity = true;
        _rotationMatrix = true;
        _uniformScale = true;
    }

    /**
     * Sets the matrix portion of this transform to the given value, considering it as a normal 3x3 rotation. Calling
     * this allows scale to be set and used.
     * 
     * @param rotation
     * @throws NullPointerException
     *             if rotation is null.
     */
    public void setRotation(final ReadOnlyMatrix3 rotation) {
        _matrix.set(rotation);
        _identity = false;
        _rotationMatrix = true;
    }

    /**
     * Sets the matrix portion of this transform to the rotational value of the given Quaternion. Calling this allows
     * scale to be set and used.
     * 
     * @param rotation
     * @throws NullPointerException
     *             if rotation is null.
     */
    public void setRotation(final ReadOnlyQuaternion rotation) {
        _matrix.set(rotation);
        _identity = false;
        _rotationMatrix = true;
    }

    /**
     * Sets the matrix portion of this transform to the given value, considering it as a generic 3x3 matrix (eg.
     * rotation and a non-uniform or negative scale, etc.). Calling this means that the scale field of this transform is
     * ignored. Further attempts to set scale will result in an exception.
     * 
     * @param matrix
     * @throws NullPointerException
     *             if matrix is null.
     */
    public void setMatrix(final ReadOnlyMatrix3 matrix) {
        _matrix.set(matrix);
        _identity = false;
        _rotationMatrix = false;
        _uniformScale = false;
    }

    /**
     * Sets the translation portion of this transform to the given value.
     * 
     * @param translation
     * @throws NullPointerException
     *             if translation is null.
     */
    public void setTranslation(final ReadOnlyVector3 translation) {
        _translation.set(translation);
        _identity = false;
    }

    /**
     * Sets the translation portion of this transform to the given values.
     * 
     * @param x
     * @param y
     * @param z
     */
    public void setTranslation(final double x, final double y, final double z) {
        _translation.set(x, y, z);
        _identity = false;
    }

    /**
     * Sets the scale portion of this transform to the given value.
     * 
     * @param scale
     * @throws NullPointerException
     *             if scale is null.
     * @throws TransformException
     *             if this transform has a generic 3x3 matrix set.
     * @throws IllegalArgumentException
     *             if scale is (0,0,0)
     */
    public void setScale(final ReadOnlyVector3 scale) {
        if (!_rotationMatrix) {
            throw new TransformException(
                    "Scale is already provided by 3x3 matrix.  If this is a mistake, consider using setRotation instead of setMatrix.");
        }
        if (scale.getX() == 0.0 && scale.getY() == 0.0 && scale.getZ() == 0.0) {
            throw new IllegalArgumentException("scale may not be ZERO.");
        }

        _scale.set(scale);
        _identity = _identity && (scale.getX() == 1.0 && scale.getY() == 1.0 && scale.getZ() == 1.0);
        _uniformScale = scale.getX() == scale.getY() && scale.getY() == scale.getZ();
    }

    /**
     * Sets the scale portion of this transform to the given values.
     * 
     * @param x
     * @param y
     * @param z
     * @throws NullPointerException
     *             if scale is null.
     * @throws TransformException
     *             if this transform has a generic 3x3 matrix set.
     * @throws IllegalArgumentException
     *             if scale is (0,0,0)
     */
    public void setScale(final double x, final double y, final double z) {
        if (!_rotationMatrix) {
            throw new TransformException(
                    "Scale is already provided by 3x3 matrix.  If this is a mistake, consider using setRotation instead of setMatrix.");
        }
        if (x == 0.0 && y == 0.0 && z == 0.0) {
            throw new IllegalArgumentException("scale may not be ZERO.");
        }

        _scale.set(x, y, z);
        _identity = false;
        _uniformScale = x == y && y == z;
    }

    /**
     * Sets the scale portion of this transform to the given value as a vector (u, u, u)
     * 
     * @param uniformScale
     * @throws TransformException
     *             if this transform has a generic 3x3 matrix set.
     * @throws IllegalArgumentException
     *             if uniformScale is 0
     */
    public void setScale(final double uniformScale) {
        if (!_rotationMatrix) {
            throw new TransformException(
                    "Scale is already provided by 3x3 matrix.  If this is a mistake, consider using setRotation instead of setMatrix.");
        }
        if (uniformScale == 0.0) {
            throw new IllegalArgumentException("scale may not be ZERO.");
        }

        _scale.set(uniformScale, uniformScale, uniformScale);
        _identity = false;
        _uniformScale = true;
    }

    /**
     * Copies the given transform values into this transform object.
     * 
     * @param source
     * @return this transform for chaining.
     * @throws NullPointerException
     *             if source is null.
     */
    public Transform set(final ReadOnlyTransform source) {
        if (source.isIdentity()) {
            setIdentity();
        } else {
            _matrix.set(source.getMatrix());
            _scale.set(source.getScale());
            _translation.set(source.getTranslation());

            _identity = false;
            _rotationMatrix = source.isRotationMatrix();
            _uniformScale = source.isUniformScale();
        }
        return this;
    }

    /**
     * Locally adds to the translation of this transform.
     * 
     * @param x
     * @param y
     * @param z
     * @return this transform for chaining.
     */
    public Transform translate(final double x, final double y, final double z) {
        _translation.addLocal(x, y, z);
        return this;
    }

    /**
     * Locally applies this transform to the given point: P' = M*P+T
     * 
     * @param point
     * @return the transformed point.
     * @throws NullPointerException
     *             if point is null.
     */
    public Vector3 applyForward(final Vector3 point) {
        if (point == null) {
            throw new NullPointerException();
        }

        if (_identity) {
            // No need to make changes
            // Y = X
            return point;
        }

        if (_rotationMatrix) {
            // Scale is separate from matrix
            // Y = R*S*X + T
            point.set(point.getX() * _scale.getX(), point.getY() * _scale.getY(), point.getZ() * _scale.getZ());
            _matrix.applyPost(point, point);
            point.addLocal(_translation);
            return point;
        }

        // scale is part of matrix.
        // Y = M*X + T
        _matrix.applyPost(point, point);
        point.addLocal(_translation);
        return point;

    }

    /**
     * Applies this transform to the given point and returns the result in the given store vector: P' = M*P+T
     * 
     * @param point
     * @param store
     *            the vector to store our result in. if null, a new vector will be created.
     * @return the transformed point.
     * @throws NullPointerException
     *             if point is null.
     */
    public Vector3 applyForward(final ReadOnlyVector3 point, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        result.set(point);
        return applyForward(result);
    }

    /**
     * Locally applies the inverse of this transform to the given point: P' = M^{-1}*(P-T)
     * 
     * @param point
     * @return the transformed point.
     * @throws NullPointerException
     *             if point is null.
     */
    public Vector3 applyInverse(final Vector3 point) {
        if (point == null) {
            throw new NullPointerException();
        }

        if (_identity) {
            // No need to make changes
            // P' = P
            return point;
        }

        // Back track translation
        point.subtractLocal(_translation);

        if (_rotationMatrix) {
            // Scale is separate from matrix so...
            // P' = S^{-1}*R^t*(P - T)
            _matrix.applyPre(point, point);
            if (_uniformScale) {
                point.divideLocal(_scale.getX());
            } else {
                point.setX(point.getX() / _scale.getX());
                point.setY(point.getY() / _scale.getY());
                point.setZ(point.getZ() / _scale.getZ());
            }
        } else {
            // P' = M^{-1}*(P - T)
            final Matrix3 invertedMatrix = _matrix.invert(Matrix3.fetchTempInstance());
            invertedMatrix.applyPost(point, point);
            Matrix3.releaseTempInstance(invertedMatrix);
        }

        return point;
    }

    /**
     * Applies the inverse of this transform to the given point and returns the result in the given store vector: P' =
     * M^{-1}*(P-T)
     * 
     * @param point
     * @param store
     *            the vector to store our result in. if null, a new vector will be created.
     * @return the transformed point.
     * @throws NullPointerException
     *             if point is null.
     */
    public Vector3 applyInverse(final ReadOnlyVector3 point, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        result.set(point);
        return applyInverse(result);
    }

    /**
     * Locally applies this transform to the given vector: V' = M*V
     * 
     * @param vector
     * @return the transformed vector.
     * @throws NullPointerException
     *             if vector is null.
     */
    public Vector3 applyForwardVector(final Vector3 vector) {
        if (vector == null) {
            throw new NullPointerException();
        }

        if (_identity) {
            // No need to make changes
            // V' = V
            return vector;
        }

        if (_rotationMatrix) {
            // Scale is separate from matrix
            // V' = R*S*V
            vector.set(vector.getX() * _scale.getX(), vector.getY() * _scale.getY(), vector.getZ() * _scale.getZ());
            _matrix.applyPost(vector, vector);
            return vector;
        }

        // scale is part of matrix.
        // V' = M*V
        _matrix.applyPost(vector, vector);
        return vector;

    }

    /**
     * Applies this transform to the given vector and returns the result in the given store vector: V' = M*V
     * 
     * @param vector
     * @param store
     *            the vector to store our result in. if null, a new vector will be created.
     * @return the transformed vector.
     * @throws NullPointerException
     *             if vector is null.
     */
    public Vector3 applyForwardVector(final ReadOnlyVector3 vector, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        result.set(vector);
        return applyForwardVector(result);
    }

    /**
     * Locally applies the inverse of this transform to the given vector: V' = M^{-1}*V
     * 
     * @param vector
     * @return the transformed vector.
     * @throws NullPointerException
     *             if vector is null.
     */
    public Vector3 applyInverseVector(final Vector3 vector) {
        if (vector == null) {
            throw new NullPointerException();
        }

        if (_identity) {
            // No need to make changes
            // V' = V
            return vector;
        }

        if (_rotationMatrix) {
            // Scale is separate from matrix so...
            // V' = S^{-1}*R^t*V
            _matrix.applyPre(vector, vector);
            if (_uniformScale) {
                vector.divideLocal(_scale.getX());
            } else {
                vector.setX(vector.getX() / _scale.getX());
                vector.setY(vector.getY() / _scale.getY());
                vector.setZ(vector.getZ() / _scale.getZ());
            }
        } else {
            // V' = M^{-1}*V
            final Matrix3 invertedMatrix = _matrix.invert(Matrix3.fetchTempInstance());
            invertedMatrix.applyPost(vector, vector);
            Matrix3.releaseTempInstance(invertedMatrix);
        }

        return vector;
    }

    /**
     * Applies the inverse of this transform to the given vector and returns the result in the given store vector: V' =
     * M^{-1}*V
     * 
     * @param vector
     * @param store
     *            the vector to store our result in. if null, a new vector will be created.
     * @return the transformed vector.
     * @throws NullPointerException
     *             if vector is null.
     */
    public Vector3 applyInverseVector(final ReadOnlyVector3 vector, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        result.set(vector);
        return applyInverseVector(result);
    }

    /**
     * Calculates the product of this transform with the given "transformBy" transform (P = this * T) and stores this in
     * store.
     * 
     * @param transformBy
     * @param store
     *            the transform to store the result in for return. If null, a new transform object is created and
     *            returned. It is NOT safe for store to be the same object as transformBy or "this".
     * @return the product
     * @throws NullPointerException
     *             if transformBy is null.
     */
    public Transform multiply(final ReadOnlyTransform transformBy, final Transform store) {
        Transform result = store;
        if (result == null) {
            result = new Transform();
        } else {
            result.setIdentity();
        }

        if (_identity) {
            return result.set(transformBy);
        }

        if (transformBy.isIdentity()) {
            return result.set(this);
        }

        if (_rotationMatrix && transformBy.isRotationMatrix()) {
            if (_uniformScale) {
                final Matrix3 newRotation = result._matrix;
                newRotation.set(_matrix).multiplyLocal(transformBy.getMatrix());
                // make sure flags are set.
                result.setRotation(newRotation);

                final Vector3 newTranslation = result._translation.set(transformBy.getTranslation());
                _matrix.applyPost(newTranslation, newTranslation);
                // uniform scale, so just use X.
                newTranslation.multiplyLocal(_scale.getX());
                newTranslation.addLocal(_translation);
                // make sure flags are set.
                result.setTranslation(newTranslation);

                if (transformBy.isUniformScale()) {
                    result.setScale(_scale.getX() * transformBy.getScale().getX());
                } else {
                    final Vector3 scale = result._scale.set(transformBy.getScale());
                    scale.multiplyLocal(_scale.getX());
                    // make sure flags are set.
                    result.setScale(scale);
                }

                return result;
            }
        }

        // In all remaining cases, the matrix cannot be written as R*S*X+T.
        final ReadOnlyMatrix3 matrixA = (_rotationMatrix ? _matrix.multiplyDiagonalPost(_scale, Matrix3
                .fetchTempInstance()) : _matrix);

        final ReadOnlyMatrix3 matrixB = (transformBy.isRotationMatrix() ? transformBy.getMatrix().multiplyDiagonalPost(
                transformBy.getScale(), Matrix3.fetchTempInstance()) : transformBy.getMatrix());

        final Matrix3 newMatrix = result._matrix;
        newMatrix.set(matrixA).multiplyLocal(matrixB);
        // make sure flags are set.
        result.setMatrix(newMatrix);

        final Vector3 newTranslate = result._translation;
        matrixA.applyPost(transformBy.getTranslation(), newTranslate).addLocal(getTranslation());
        // make sure flags are set.
        result.setTranslation(newTranslate);

        if (isRotationMatrix()) {
            Matrix3.releaseTempInstance((Matrix3) matrixA);
        }
        if (transformBy.isRotationMatrix()) {
            Matrix3.releaseTempInstance((Matrix3) matrixB);
        }

        return result;
    }

    /**
     * Calculates the inverse of this transform.
     * 
     * @param store
     *            the transform to store the result in for return. If null, a new transform object is created and
     *            returned. It IS safe for store to be the same object as "this".
     * @return the inverted transform
     */
    public Transform invert(final Transform store) {
        Transform result = store;
        if (result == null) {
            result = new Transform();
        }

        if (_identity) {
            result.setIdentity();
            return result;
        }

        final Matrix3 newMatrix = result._matrix.set(_matrix);
        if (_rotationMatrix) {
            if (_uniformScale) {
                newMatrix.transposeLocal().multiplyLocal(1.0 / _scale.getX());
            } else {
                newMatrix.multiplyDiagonalPost(_scale, newMatrix).invertLocal();
            }
        } else {
            newMatrix.invertLocal();
        }

        result._matrix.applyPost(_translation, result._translation).negateLocal();
        result._identity = false;
        result._rotationMatrix = false;
        result._uniformScale = false;

        return result;
    }

    /**
     * @param store
     *            the matrix to store the result in for return. If null, a new matrix object is created and returned.
     * @return this transform represented as a 4x4 matrix:
     * 
     *         <pre>
     * R R R Tx
     * R R R Ty
     * R R R Tz
     * 0 0 0 1
     * </pre>
     */
    public Matrix4 getHomogeneousMatrix(final Matrix4 store) {
        Matrix4 result = store;
        if (result == null) {
            result = new Matrix4();
        }

        if (_rotationMatrix) {
            result._data[0][0] = _scale.getX() * _matrix._data[0][0];
            result._data[0][1] = _scale.getX() * _matrix._data[0][1];
            result._data[0][2] = _scale.getX() * _matrix._data[0][2];
            result._data[0][3] = 0.0;
            result._data[1][0] = _scale.getY() * _matrix._data[1][0];
            result._data[1][1] = _scale.getY() * _matrix._data[1][1];
            result._data[1][2] = _scale.getY() * _matrix._data[1][2];
            result._data[1][3] = 0.0;
            result._data[2][0] = _scale.getZ() * _matrix._data[2][0];
            result._data[2][1] = _scale.getZ() * _matrix._data[2][1];
            result._data[2][2] = _scale.getZ() * _matrix._data[2][2];
            result._data[2][3] = 0.0;
        } else {
            result._data[0][0] = _matrix._data[0][0];
            result._data[0][1] = _matrix._data[0][1];
            result._data[0][2] = _matrix._data[0][2];
            result._data[0][3] = 0.0;
            result._data[1][0] = _matrix._data[1][0];
            result._data[1][1] = _matrix._data[1][1];
            result._data[1][2] = _matrix._data[1][2];
            result._data[1][3] = 0.0;
            result._data[2][0] = _matrix._data[2][0];
            result._data[2][1] = _matrix._data[2][1];
            result._data[2][2] = _matrix._data[2][2];
            result._data[2][3] = 0.0;
        }

        result._data[0][3] = _translation.getX();
        result._data[1][3] = _translation.getY();
        result._data[2][3] = _translation.getZ();
        result._data[3][3] = 1.0;

        return result;
    }

    public void getGLApplyMatrix(final DoubleBuffer store) {
        store.position(0);
        if (_rotationMatrix) {
            store.put(_scale.getX() * _matrix._data[0][0]);
            store.put(_scale.getX() * _matrix._data[1][0]);
            store.put(_scale.getX() * _matrix._data[2][0]);
            store.position(4);
            store.put(_scale.getY() * _matrix._data[0][1]);
            store.put(_scale.getY() * _matrix._data[1][1]);
            store.put(_scale.getY() * _matrix._data[2][1]);
            store.position(8);
            store.put(_scale.getZ() * _matrix._data[0][2]);
            store.put(_scale.getZ() * _matrix._data[1][2]);
            store.put(_scale.getZ() * _matrix._data[2][2]);
            store.position(12);
        } else {
            store.put(_matrix._data[0][0]);
            store.put(_matrix._data[1][0]);
            store.put(_matrix._data[2][0]);
            store.position(4);
            store.put(_matrix._data[0][1]);
            store.put(_matrix._data[1][1]);
            store.put(_matrix._data[2][1]);
            store.position(8);
            store.put(_matrix._data[0][2]);
            store.put(_matrix._data[1][2]);
            store.put(_matrix._data[2][2]);
            store.position(12);
        }

        store.put(_translation.getX());
        store.put(_translation.getY());
        store.put(_translation.getZ());
        store.rewind();
    }

    /**
     * Reads in a 4x4 matrix as a 3x3 matrix and translation.
     * 
     * @param matrix
     * @return this matrix for chaining.
     * @throws NullPointerException
     *             if matrix is null.
     */
    public Transform fromHomogeneousMatrix(final ReadOnlyMatrix4 matrix) {
        return fromHomogeneousMatrix(matrix, false);
    }

    /**
     * Reads in a 4x4 matrix as a 3x3 matrix and translation.
     * 
     * @param matrix
     * @param orthonormal
     *            true if the rotation portion of the given matrix is orthonormal
     * @return this matrix for chaining.
     * @throws NullPointerException
     *             if matrix is null.
     */
    public Transform fromHomogeneousMatrix(final ReadOnlyMatrix4 matrix, final boolean orthonormal) {
        _matrix.set(matrix.getValue(0, 0), matrix.getValue(0, 1), matrix.getValue(0, 2), matrix.getValue(1, 0), matrix
                .getValue(1, 1), matrix.getValue(1, 2), matrix.getValue(2, 0), matrix.getValue(2, 1), matrix.getValue(
                2, 2));
        _translation.set(matrix.getValue(0, 3), matrix.getValue(1, 3), matrix.getValue(2, 3));

        _identity = false;
        _rotationMatrix = orthonormal;
        _uniformScale = false;
        return this;
    }

    /**
     * Check a transform... if it is null or one of its members are invalid, return false. Else return true.
     * 
     * @param transform
     *            the transform to check
     * 
     * @return true or false as stated above.
     */
    public static boolean isValid(final ReadOnlyTransform transform) {
        if (transform == null) {
            return false;
        }
        if (!Vector3.isValid(transform.getScale()) || !Vector3.isValid(transform.getTranslation())
                || !Matrix3.isValid(transform.getMatrix())) {
            return false;
        }

        return true;
    }

    /**
     * @return the string representation of this triangle.
     */
    @Override
    public String toString() {
        return "com.ardor3d.math.Transform [\n M: " + _matrix + "\n S: " + _scale + "\n T: " + _translation + "\n]";
    }

    /**
     * @return returns a unique code for this transform object based on its values.
     */
    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + _matrix.hashCode();
        result += 31 * result + _scale.hashCode();
        result += 31 * result + _translation.hashCode();

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this transform and the provided transform have the same values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyTransform)) {
            return false;
        }
        final ReadOnlyTransform comp = (ReadOnlyTransform) o;
        return (_matrix.equals(comp.getMatrix()) && _scale.equals(comp.getScale()) && _translation.equals(comp
                .getTranslation()));
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Transform clone() {
        try {
            final Transform t = (Transform) super.clone();
            t._matrix.set(_matrix);
            t._scale.set(_scale);
            t._translation.set(_translation);
            return t;
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends Transform> getClassTag() {
        return this.getClass();
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_matrix, "rotation", new Matrix3(Matrix3.IDENTITY));
        capsule.write(_scale, "scale", new Vector3(Vector3.XYZ_ONE));
        capsule.write(_translation, "translation", new Vector3(Vector3.ZERO));
        capsule.write(_identity, "identity", true);
        capsule.write(_rotationMatrix, "rotationMatrix", true);
        capsule.write(_uniformScale, "uniformScale", true);
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);
        _matrix.set((Matrix3) capsule.readSavable("rotation", new Matrix3(Matrix3.IDENTITY)));
        _scale.set((Vector3) capsule.readSavable("scale", new Vector3(Vector3.XYZ_ONE)));
        _translation.set((Vector3) capsule.readSavable("translation", new Vector3(Vector3.ZERO)));
        _identity = capsule.readBoolean("identity", true);
        _rotationMatrix = capsule.readBoolean("rotationMatrix", true);
        _uniformScale = capsule.readBoolean("uniformScale", true);
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
        _matrix.set((Matrix3) in.readObject());
        _scale.set((Vector3) in.readObject());
        _translation.set((Vector3) in.readObject());
        _identity = in.readBoolean();
        _rotationMatrix = in.readBoolean();
        _uniformScale = in.readBoolean();
    }

    /*
     * Used with serialization. Not to be called manually.
     * 
     * @param out ObjectOutput
     * 
     * @throws IOException
     */
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(_translation);
        out.writeObject(_matrix);
        out.writeObject(_scale);
        out.writeBoolean(_identity);
        out.writeBoolean(_rotationMatrix);
        out.writeBoolean(_uniformScale);
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Transform that is intended for temporary use in calculations and so forth. Multiple calls
     *         to the method should return instances of this class that are not currently in use.
     */
    public final static Transform fetchTempInstance() {
        if (Debug.useMathPools) {
            return TRANS_POOL.fetch();
        } else {
            return new Transform();
        }
    }

    /**
     * Releases a Transform back to be used by a future call to fetchTempInstance. TAKE CARE: this Transform object
     * should no longer have other classes referencing it or "Bad Things" will happen.
     * 
     * @param trans
     *            the Transform to release.
     */
    public final static void releaseTempInstance(final Transform trans) {
        if (Debug.useMathPools) {
            TRANS_POOL.release(trans);
        }
    }

    static final class TransformPool extends ObjectPool<Transform> {
        public TransformPool(final int initialSize) {
            super(initialSize);
        }

        @Override
        protected Transform newInstance() {
            return new Transform();
        }
    }
}
