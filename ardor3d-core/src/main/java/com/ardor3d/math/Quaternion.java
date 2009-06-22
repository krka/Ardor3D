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

import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.Constants;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Quaternion represents a 4 value math object used in Ardor3D to describe rotations. It has the advantage of being able
 * to avoid lock by adding a 4th dimension to rotation.
 * 
 * Note: some algorithms in this class were ported from Eberly, Wolfram, Game Gems and others to Java by myself and
 * others, originally for jMonkeyEngine.
 */
public class Quaternion implements Cloneable, Savable, Externalizable, ReadOnlyQuaternion, Poolable {

    private static final long serialVersionUID = 1L;

    private static final ObjectPool<Quaternion> QUAT_POOL = ObjectPool.create(Quaternion.class, Constants.maxPoolSize);

    /**
     * x=0, y=0, z=0, w=1
     */
    public final static ReadOnlyQuaternion IDENTITY = new Quaternion(0, 0, 0, 1);

    protected double _x = 0;
    protected double _y = 0;
    protected double _z = 0;
    protected double _w = 1;

    /**
     * Constructs a new quaternion set to (0, 0, 0, 1).
     */
    public Quaternion() {
        this(IDENTITY);
    }

    /**
     * Constructs a new quaternion set to the (x, y, z, w) values of the given source quaternion.
     * 
     * @param source
     */
    public Quaternion(final ReadOnlyQuaternion source) {
        this(source.getX(), source.getY(), source.getZ(), source.getW());
    }

    /**
     * Constructs a new quaternion set to (x, y, z, w).
     * 
     * @param x
     * @param y
     * @param z
     * @param w
     */
    public Quaternion(final double x, final double y, final double z, final double w) {
        _x = x;
        _y = y;
        _z = z;
        _w = w;
    }

    public double getX() {
        return _x;
    }

    public double getY() {
        return _y;
    }

    public double getZ() {
        return _z;
    }

    public double getW() {
        return _w;
    }

    public float getXf() {
        return (float) _x;
    }

    public float getYf() {
        return (float) _y;
    }

    public float getZf() {
        return (float) _z;
    }

    public float getWf() {
        return (float) _w;
    }

    /**
     * Stores the double values of this quaternion in the given double array as (x,y,z,w).
     * 
     * @param store
     *            The array in which to store the values of this quaternion. If null, a new double[4] array is created.
     * @return the double array
     * @throws IllegalArgumentException
     *             if store is not null and is not at least length 4
     */
    public double[] toArray(final double[] store) {
        double[] result = store;
        if (result == null) {
            result = new double[4];
        } else if (result.length < 4) {
            throw new IllegalArgumentException("store array must have at least three elements");
        }
        result[0] = getX();
        result[1] = getY();
        result[2] = getZ();
        result[3] = getW();
        return result;
    }

    /**
     * Sets the x component of this quaternion to the given double value.
     * 
     * @param x
     */
    public void setX(final double x) {
        _x = x;
    }

    /**
     * Sets the y component of this quaternion to the given double value.
     * 
     * @param y
     */
    public void setY(final double y) {
        _y = y;
    }

    /**
     * Sets the z component of this quaternion to the given double value.
     * 
     * @param z
     */
    public void setZ(final double z) {
        _z = z;
    }

    /**
     * Sets the w component of this quaternion to the given double value.
     * 
     * @param w
     */
    public void setW(final double w) {
        _w = w;
    }

    /**
     * Sets the value of this quaternion to (x, y, z, w)
     * 
     * @param x
     * @param y
     * @param z
     * @param w
     * @return this quaternion for chaining
     */
    public Quaternion set(final double x, final double y, final double z, final double w) {
        setX(x);
        setY(y);
        setZ(z);
        setW(w);
        return this;
    }

    /**
     * Sets the value of this quaternion to the (x, y, z, w) values of the provided source quaternion.
     * 
     * @param source
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Quaternion set(final ReadOnlyQuaternion source) {
        setX(source.getX());
        setY(source.getY());
        setZ(source.getZ());
        setW(source.getW());
        return this;
    }

    /**
     * Updates this quaternion from the given Euler rotation angles, applied in the given order: heading, attitude,
     * bank.
     * 
     * @param angles
     *            the Euler angles of rotation (in radians) stored as heading, attitude, and bank.
     * @return this quaternion for chaining
     * @throws IllegalArgumentException
     *             if angles is not length 3
     * @throws NullPointerException
     *             if angles is null.
     */
    public Quaternion fromEulerAngles(final double[] angles) {
        if (angles.length != 3) {
            throw new IllegalArgumentException("Angles array must have three elements");
        }

        return fromEulerAngles(angles[0], angles[1], angles[2]);
    }

    /**
     * Updates this quaternion from the given Euler rotation angles, applied in the given order: heading, attitude,
     * bank.
     * 
     * @param heading
     *            the Euler heading angle in radians. (rotation about the y axis)
     * @param attitude
     *            the Euler attitude angle in radians. (rotation about the z axis)
     * @param bank
     *            the Euler bank angle in radians. (rotation about the x axis)
     * @return this quaternion for chaining
     * @see http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToQuaternion/index.htm
     */
    public Quaternion fromEulerAngles(final double heading, final double attitude, final double bank) {
        double angle = heading * 0.5;
        final double sinHeading = MathUtils.sin(angle);
        final double cosHeading = MathUtils.cos(angle);
        angle = attitude * 0.5;
        final double sinAttitude = MathUtils.sin(angle);
        final double cosAttitude = MathUtils.cos(angle);
        angle = bank * 0.5;
        final double sinBank = MathUtils.sin(angle);
        final double cosBank = MathUtils.cos(angle);

        // variables used to reduce multiplication calls.
        final double cosHeadingXcosAttitude = cosHeading * cosAttitude;
        final double sinHeadingXsinAttitude = sinHeading * sinAttitude;
        final double cosHeadingXsinAttitude = cosHeading * sinAttitude;
        final double sinHeadingXcosAttitude = sinHeading * cosAttitude;

        final double w = (cosHeadingXcosAttitude * cosBank - sinHeadingXsinAttitude * sinBank);
        final double x = (cosHeadingXcosAttitude * sinBank + sinHeadingXsinAttitude * cosBank);
        final double y = (sinHeadingXcosAttitude * cosBank + cosHeadingXsinAttitude * sinBank);
        final double z = (cosHeadingXsinAttitude * cosBank - sinHeadingXcosAttitude * sinBank);

        set(x, y, z, w);

        return normalizeLocal();
    }

    /**
     * Converts this quaternion to Euler rotation angles in radians (heading, attitude, bank).
     * 
     * @param store
     *            the double[] array to store the computed angles in. If null, a new double[] will be created
     * @return the double[] array, filled with heading, attitude and bank in that order..
     * @throws IllegalArgumentException
     *             if non-null store is not at least length 3
     * @see http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/index.htm
     * @see #fromEulerAngles(double, double, double)
     */
    public double[] toEulerAngles(final double[] store) {
        double[] result = store;
        if (result == null) {
            result = new double[3];
        } else if (result.length < 3) {
            throw new IllegalArgumentException("store array must have at least three elements");
        }

        final double sqw = getW() * getW();
        final double sqx = getX() * getX();
        final double sqy = getY() * getY();
        final double sqz = getZ() * getZ();
        final double unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        // is correction factor
        final double test = getX() * getY() + getZ() * getW();
        if (test > 0.499 * unit) { // singularity at north pole
            result[0] = 2 * Math.atan2(getX(), getW());
            result[1] = MathUtils.HALF_PI;
            result[2] = 0;
        } else if (test < -0.499 * unit) { // singularity at south pole
            result[0] = -2 * Math.atan2(getX(), getW());
            result[1] = -MathUtils.HALF_PI;
            result[2] = 0;
        } else {
            result[0] = Math.atan2(2 * getY() * getW() - 2 * getX() * getZ(), sqx - sqy - sqz + sqw);
            result[1] = Math.asin(2 * test / unit);
            result[2] = Math.atan2(2 * getX() * getW() - 2 * getY() * getZ(), -sqx + sqy - sqz + sqw);
        }
        return result;
    }

    /**
     * Sets the value of this quaternion to the rotation described by the given matrix.
     * 
     * @param matrix
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if matrix is null.
     */
    public Quaternion fromRotationMatrix(final ReadOnlyMatrix3 matrix) {
        return fromRotationMatrix(matrix.getValue(0, 0), matrix.getValue(0, 1), matrix.getValue(0, 2), matrix.getValue(
                1, 0), matrix.getValue(1, 1), matrix.getValue(1, 2), matrix.getValue(2, 0), matrix.getValue(2, 1),
                matrix.getValue(2, 2));
    }

    /**
     * Sets the value of this quaternion to the rotation described by the given matrix values.
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
     * @return this quaternion for chaining
     */
    public Quaternion fromRotationMatrix(final double m00, final double m01, final double m02, final double m10,
            final double m11, final double m12, final double m20, final double m21, final double m22) {
        // Uses the Graphics Gems code, from
        // ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z
        // *NOT* the "Matrix and Quaternions FAQ", which has errors!

        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        final double t = m00 + m11 + m22;

        // we protect the division by s by ensuring that s>=1
        double x, y, z, w;
        if (t >= 0) { // |w| >= .5
            double s = Math.sqrt(t + 1); // |s|>=1 ...
            w = 0.5 * s;
            s = 0.5 / s; // so this division isn't bad
            x = (m21 - m12) * s;
            y = (m02 - m20) * s;
            z = (m10 - m01) * s;
        } else if ((m00 > m11) && (m00 > m22)) {
            double s = Math.sqrt(1.0 + m00 - m11 - m22); // |s|>=1
            x = s * 0.5; // |x| >= .5
            s = 0.5 / s;
            y = (m10 + m01) * s;
            z = (m02 + m20) * s;
            w = (m21 - m12) * s;
        } else if (m11 > m22) {
            double s = Math.sqrt(1.0 + m11 - m00 - m22); // |s|>=1
            y = s * 0.5; // |y| >= .5
            s = 0.5 / s;
            x = (m10 + m01) * s;
            z = (m21 + m12) * s;
            w = (m02 - m20) * s;
        } else {
            double s = Math.sqrt(1.0 + m22 - m00 - m11); // |s|>=1
            z = s * 0.5; // |z| >= .5
            s = 0.5 / s;
            x = (m02 + m20) * s;
            y = (m21 + m12) * s;
            w = (m10 - m01) * s;
        }

        return set(x, y, z, w);
    }

    /**
     * @param store
     *            the matrix to store our result in. If null, a new matrix is created.
     * @return the rotation matrix representation of this quaternion (normalized)
     * 
     *         if store is not null and is read only.
     */
    public Matrix3 toRotationMatrix(final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        final double norm = magnitudeSquared();
        final double s = (norm > 0.0 ? 2.0 / norm : 0.0);

        // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
        // will be used 2-4 times each.
        final double xs = getX() * s;
        final double ys = getY() * s;
        final double zs = getZ() * s;
        final double xx = getX() * xs;
        final double xy = getX() * ys;
        final double xz = getX() * zs;
        final double xw = getW() * xs;
        final double yy = getY() * ys;
        final double yz = getY() * zs;
        final double yw = getW() * ys;
        final double zz = getZ() * zs;
        final double zw = getW() * zs;

        // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
        result.setValue(0, 0, 1.0 - (yy + zz));
        result.setValue(0, 1, xy - zw);
        result.setValue(0, 2, xz + yw);
        result.setValue(1, 0, xy + zw);
        result.setValue(1, 1, 1.0 - (xx + zz));
        result.setValue(1, 2, yz - xw);
        result.setValue(2, 0, xz - yw);
        result.setValue(2, 1, yz + xw);
        result.setValue(2, 2, 1.0 - (xx + yy));

        return result;
    }

    /**
     * @param store
     *            the matrix to store our result in. If null, a new matrix is created.
     * @return the rotation matrix representation of this quaternion (normalized)
     */
    public Matrix4 toRotationMatrix(final Matrix4 store) {
        Matrix4 result = store;
        if (result == null) {
            result = new Matrix4();
        }

        final double norm = magnitude();
        final double s = (norm == 1.0 ? 2.0 : (norm > 0.0 ? 2.0 / norm : 0));

        // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
        // will be used 2-4 times each.
        final double xs = getX() * s;
        final double ys = getY() * s;
        final double zs = getZ() * s;
        final double xx = getX() * xs;
        final double xy = getX() * ys;
        final double xz = getX() * zs;
        final double xw = getW() * xs;
        final double yy = getY() * ys;
        final double yz = getY() * zs;
        final double yw = getW() * ys;
        final double zz = getZ() * zs;
        final double zw = getW() * zs;

        // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
        result.setValue(0, 0, 1.0 - (yy + zz));
        result.setValue(0, 1, xy - zw);
        result.setValue(0, 2, xz + yw);
        result.setValue(1, 0, xy + zw);
        result.setValue(1, 1, 1.0 - (xx + zz));
        result.setValue(1, 2, yz - xw);
        result.setValue(2, 0, xz - yw);
        result.setValue(2, 1, yz + xw);
        result.setValue(2, 2, 1.0 - (xx + yy));

        return result;
    }

    /**
     * @param index
     *            the 3x3 rotation matrix column to retrieve from this quaternion (normalized). Must be between 0 and 2.
     * @param store
     *            the vector object to store the result in. if null, a new one is created.
     * @return the column specified by the index.
     */
    public Vector3 getRotationColumn(final int index, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        final double norm = magnitude();
        final double s = (norm == 1.0 ? 2.0 : (norm > 0.0 ? 2.0 / norm : 0));

        // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
        // will be used 2-4 times each.
        final double xs = getX() * s;
        final double ys = getY() * s;
        final double zs = getZ() * s;
        final double xx = getX() * xs;
        final double xy = getX() * ys;
        final double xz = getX() * zs;
        final double xw = getW() * xs;
        final double yy = getY() * ys;
        final double yz = getY() * zs;
        final double yw = getW() * ys;
        final double zz = getZ() * zs;
        final double zw = getW() * zs;

        // using s=2/norm (instead of 1/norm) saves 3 multiplications by 2 here
        double x, y, z;
        switch (index) {
            case 0:
                x = 1.0 - (yy + zz);
                y = xy + zw;
                z = xz - yw;
                break;
            case 1:
                x = xy - zw;
                y = 1.0 - (xx + zz);
                z = yz + xw;
                break;
            case 2:
                x = xz + yw;
                y = yz - xw;
                z = 1.0 - (xx + yy);
                break;
            default:
                throw new IllegalArgumentException("Invalid column index. " + index);
        }

        return result.set(x, y, z);
    }

    /**
     * Sets the values of this quaternion to the values represented by a given angle and axis of rotation. Note that
     * this method creates an object, so use fromAngleNormalAxis if your axis is already normalized. If axis == 0,0,0
     * the quaternion is set to identity.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation.
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if axis is null
     */
    public Quaternion fromAngleAxis(final double angle, final ReadOnlyVector3 axis) {
        final Vector3 temp = Vector3.fetchTempInstance();
        final Quaternion quat = fromAngleNormalAxis(angle, axis.normalize(temp));
        Vector3.releaseTempInstance(temp);
        return quat;
    }

    /**
     * Sets the values of this quaternion to the values represented by a given angle and unit length axis of rotation.
     * If axis == 0,0,0 the quaternion is set to identity.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation (already normalized - unit length).
     * @throws NullPointerException
     *             if axis is null
     */
    public Quaternion fromAngleNormalAxis(final double angle, final ReadOnlyVector3 axis) {
        if (axis.equals(Vector3.ZERO)) {
            return setIdentity();
        }

        final double halfAngle = 0.5 * angle;
        final double sin = MathUtils.sin(halfAngle);
        final double w = MathUtils.cos(halfAngle);
        final double x = sin * axis.getX();
        final double y = sin * axis.getY();
        final double z = sin * axis.getZ();
        return set(x, y, z, w);
    }

    /**
     * Returns the rotation angle represented by this quaternion. If a non-null vector is provided, the axis of rotation
     * is stored in that vector as well.
     * 
     * @param axisStore
     *            the object we'll store the computed axis in. If null, no computations are done to determine axis.
     * @return the angle of rotation in radians.
     */
    public double toAngleAxis(final Vector3 axisStore) {
        final double sqrLength = getX() * getX() + getY() * getY() + getZ() * getZ();
        double angle;
        if (Math.abs(sqrLength) <= MathUtils.EPSILON) { // length is ~0
            angle = 0.0;
            if (axisStore != null) {
                axisStore.setX(1.0);
                axisStore.setY(0.0);
                axisStore.setZ(0.0);
            }
        } else {
            angle = (2.0 * Math.acos(getW()));
            if (axisStore != null) {
                final double invLength = (1.0 / Math.sqrt(sqrLength));
                axisStore.setX(getX() * invLength);
                axisStore.setY(getY() * invLength);
                axisStore.setZ(getZ() * invLength);
            }
        }

        return angle;
    }

    /**
     * Sets this quaternion to that which will rotate vector "from" into vector "to". from and to do not have to be the
     * same length.
     * 
     * @param from
     *            the source vector to rotate
     * @param to
     *            the destination vector into which to rotate the source vector
     * @return this quaternion for chaining
     */
    public Quaternion fromVectorToVector(final ReadOnlyVector3 from, final ReadOnlyVector3 to) {
        final ReadOnlyVector3 a = from;
        final ReadOnlyVector3 b = to;
        final double factor = a.length() * b.length();
        if (Math.abs(factor) > MathUtils.EPSILON) {
            // Vectors have length > 0
            final Vector3 pivotVector = Vector3.fetchTempInstance();
            try {
                final double dot = a.dot(b) / factor;
                final double theta = Math.acos(Math.max(-1.0, Math.min(dot, 1.0)));
                a.cross(b, pivotVector);
                if (dot < 0.0 && pivotVector.length() < MathUtils.EPSILON) {
                    // Vectors parallel and opposite direction, therefore a rotation of 180 degrees about any vector
                    // perpendicular to this vector will rotate vector a onto vector b.
                    //
                    // The following guarantees the dot-product will be 0.0.
                    int dominantIndex;
                    if (Math.abs(a.getX()) > Math.abs(a.getY())) {
                        if (Math.abs(a.getX()) > Math.abs(a.getZ())) {
                            dominantIndex = 0;
                        } else {
                            dominantIndex = 2;
                        }
                    } else {
                        if (Math.abs(a.getY()) > Math.abs(a.getZ())) {
                            dominantIndex = 1;
                        } else {
                            dominantIndex = 2;
                        }
                    }
                    pivotVector.setValue(dominantIndex, -a.getValue((dominantIndex + 1) % 3));
                    pivotVector.setValue((dominantIndex + 1) % 3, a.getValue(dominantIndex));
                    pivotVector.setValue((dominantIndex + 2) % 3, 0f);
                }
                return fromAngleAxis(theta, pivotVector);
            } finally {
                Vector3.releaseTempInstance(pivotVector);
            }
        } else {
            return setIdentity();
        }
    }

    /**
     * @param store
     *            the Quaternion to store the result in. if null, a new one is created.
     * @return a new quaternion that represents a unit length version of this Quaternion.
     */
    public Quaternion normalize(final Quaternion store) {
        Quaternion result = store;
        if (result == null) {
            result = new Quaternion();
        }

        final double n = 1.0 / magnitude();
        final double x = getX() * n;
        final double y = getY() * n;
        final double z = getZ() * n;
        final double w = getW() * n;
        return result.set(x, y, z, w);
    }

    /**
     * @return this quaternion, modified to be unit length, for chaining.
     */
    public Quaternion normalizeLocal() {
        final double n = 1.0 / magnitude();
        final double x = getX() * n;
        final double y = getY() * n;
        final double z = getZ() * n;
        final double w = getW() * n;
        return set(x, y, z, w);
    }

    /**
     * creates a new quaternion that holds the inverted values of this quaternion as if multiply(-1, store) had been
     * called.
     * 
     * @param store
     *            the Quaternion to store the result in. if null, a new one is created.
     * @return a new quaternion as described above.
     */
    public Quaternion invert(final Quaternion store) {
        return multiply(-1, store);
    }

    /**
     * internally inverts this quaternion's values as if multiplyLocal(-1) had been called.
     * 
     * @return this quaternion for chaining
     */
    public Quaternion invertLocal() {
        return multiplyLocal(-1);
    }

    /**
     * @param quat
     * @param store
     *            the Quaternion to store the result in. if null, a new one is created.
     * @return a quaternion representing the fields of this quaternion added to those of the given quaternion.
     */
    public Quaternion add(final ReadOnlyQuaternion quat, final Quaternion store) {
        Quaternion result = store;
        if (result == null) {
            result = new Quaternion();
        }

        return result.set(getX() + quat.getX(), getY() + quat.getY(), getZ() + quat.getZ(), getW() + quat.getW());
    }

    /**
     * Internally increments the fields of this quaternion with the field values of the given quaternion.
     * 
     * @param quat
     * @return this quaternion for chaining
     */
    public Quaternion addLocal(final ReadOnlyQuaternion quat) {
        setX(getX() + quat.getX());
        setY(getY() + quat.getY());
        setZ(getZ() + quat.getZ());
        setW(getW() + quat.getW());
        return this;
    }

    /**
     * @param quat
     * @param store
     *            the Quaternion to store the result in. if null, a new one is created.
     * @return a quaternion representing the fields of this quaternion subtracted from those of the given quaternion.
     */
    public Quaternion subtract(final ReadOnlyQuaternion quat, final Quaternion store) {
        Quaternion result = store;
        if (result == null) {
            result = new Quaternion();
        }

        return result.set(getX() - quat.getX(), getY() - quat.getY(), getZ() - quat.getZ(), getW() - quat.getW());
    }

    /**
     * Internally decrements the fields of this quaternion by the field values of the given quaternion.
     * 
     * @param quat
     * @return this quaternion for chaining.
     */
    public Quaternion subtractLocal(final ReadOnlyQuaternion quat) {
        setX(getX() - quat.getX());
        setY(getY() - quat.getY());
        setZ(getZ() - quat.getZ());
        setW(getW() - quat.getW());
        return this;
    }

    /**
     * Multiplies each value of this quaternion by the given scalar value.
     * 
     * @param scalar
     *            the quaternion to multiply this quaternion by.
     * @param store
     *            the Quaternion to store the result in. if null, a new one is created.
     * @return the resulting quaternion.
     */
    public Quaternion multiply(final double scalar, final Quaternion store) {
        Quaternion result = store;
        if (result == null) {
            result = new Quaternion();
        }

        return result.set(scalar * getX(), scalar * getY(), scalar * getZ(), scalar * getW());
    }

    /**
     * Multiplies each value of this quaternion by the given scalar value. The result is stored in this quaternion.
     * 
     * @param scalar
     *            the quaternion to multiply this quaternion by.
     * @return this quaternion for chaining.
     */
    public Quaternion multiplyLocal(final double scalar) {
        setX(getX() * scalar);
        setY(getY() * scalar);
        setZ(getZ() * scalar);
        setW(getW() * scalar);
        return this;
    }

    /**
     * Multiplies this quaternion by the supplied quaternion. The result is stored in the given store quaternion or a
     * new quaternion if store is null.
     * 
     * It IS safe for quat and store to be the same object.
     * 
     * @param quat
     *            the quaternion to multiply this quaternion by.
     * @param store
     *            the quaternion to store the result in.
     * @return the new quaternion.
     * 
     *         if the given store is read only.
     */
    public Quaternion multiply(final ReadOnlyQuaternion quat, Quaternion store) {
        if (store == null) {
            store = new Quaternion();
        }
        final double x = getX() * quat.getW() + getY() * quat.getZ() - getZ() * quat.getY() + getW() * quat.getX();
        final double y = -getX() * quat.getZ() + getY() * quat.getW() + getZ() * quat.getX() + getW() * quat.getY();
        final double z = getX() * quat.getY() - getY() * quat.getX() + getZ() * quat.getW() + getW() * quat.getZ();
        final double w = -getX() * quat.getX() - getY() * quat.getY() - getZ() * quat.getZ() + getW() * quat.getW();
        return store.set(x, y, z, w);
    }

    /**
     * Multiplies this quaternion by the supplied quaternion. The result is stored locally.
     * 
     * @param quat
     *            The Quaternion to multiply this one by.
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if quat is null.
     */
    public Quaternion multiplyLocal(final ReadOnlyQuaternion quat) {
        return multiplyLocal(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
    }

    /**
     * Multiplies this quaternion by the supplied matrix. The result is stored locally.
     * 
     * @param matrix
     *            the matrix to apply to this quaternion.
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if matrix is null.
     */
    public Quaternion multiplyLocal(final ReadOnlyMatrix3 matrix) {
        final double oldX = getX(), oldY = getY(), oldZ = getZ(), oldW = getW();
        fromRotationMatrix(matrix);
        final double tempX = getX(), tempY = getY(), tempZ = getZ(), tempW = getW();

        final double x = oldX * tempW + oldY * tempZ - oldZ * tempY + oldW * tempX;
        final double y = -oldX * tempZ + oldY * tempW + oldZ * tempX + oldW * tempY;
        final double z = oldX * tempY - oldY * tempX + oldZ * tempW + oldW * tempZ;
        final double w = -oldX * tempX - oldY * tempY - oldZ * tempZ + oldW * tempW;
        return set(x, y, z, w);
    }

    /**
     * Multiplies this quaternion by the supplied quaternion values. The result is stored locally.
     * 
     * @param qx
     * @param qy
     * @param qz
     * @param qw
     * @return this quaternion for chaining
     */
    public Quaternion multiplyLocal(final double qx, final double qy, final double qz, final double qw) {
        final double x = getX() * qw + getY() * qz - getZ() * qy + getW() * qx;
        final double y = -getX() * qz + getY() * qw + getZ() * qx + getW() * qy;
        final double z = getX() * qy - getY() * qx + getZ() * qw + getW() * qz;
        final double w = -getX() * qx - getY() * qy - getZ() * qz + getW() * qw;
        return set(x, y, z, w);
    }

    /**
     * Rotates the given vector by this quaternion. If supplied, the result is stored into the supplied "store" vector.
     * 
     * @param vec
     *            the vector to multiply this quaternion by.
     * @param store
     *            the vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vec is null
     * 
     *             if the given store is read only.
     */
    public Vector3 apply(final ReadOnlyVector3 vec, Vector3 store) {
        if (store == null) {
            store = new Vector3();
        }
        if (vec.equals(Vector3.ZERO)) {
            store.set(0, 0, 0);
        } else {
            final double x = getW() * getW() * vec.getX() + 2 * getY() * getW() * vec.getZ() - 2 * getZ() * getW()
                    * vec.getY() + getX() * getX() * vec.getX() + 2 * getY() * getX() * vec.getY() + 2 * getZ()
                    * getX() * vec.getZ() - getZ() * getZ() * vec.getX() - getY() * getY() * vec.getX();
            final double y = 2 * getX() * getY() * vec.getX() + getY() * getY() * vec.getY() + 2 * getZ() * getY()
                    * vec.getZ() + 2 * getW() * getZ() * vec.getX() - getZ() * getZ() * vec.getY() + getW() * getW()
                    * vec.getY() - 2 * getX() * getW() * vec.getZ() - getX() * getX() * vec.getY();
            final double z = 2 * getX() * getZ() * vec.getX() + 2 * getY() * getZ() * vec.getY() + getZ() * getZ()
                    * vec.getZ() - 2 * getW() * getY() * vec.getX() - getY() * getY() * vec.getZ() + 2 * getW()
                    * getX() * vec.getY() - getX() * getX() * vec.getZ() + getW() * getW() * vec.getZ();
            store.set(x, y, z);
        }
        return store;
    }

    /**
     * Updates this quaternion to represent a rotation formed by the given three axes. These axes are assumed to be
     * orthogonal and no error checking is applied. It is the user's job to insure that the three axes being provided
     * indeed represent a proper right handed coordinate system.
     * 
     * @param axes
     *            the array containing the three vectors representing the coordinate system.
     * @return this quaternion for chaining
     * @throws IllegalArgumentException
     *             if the given axes array is smaller than 3 elements.
     */
    public Quaternion fromAxes(final ReadOnlyVector3[] axes) {
        if (axes.length < 3) {
            throw new IllegalArgumentException("axes array must have at least three elements");
        }
        return fromAxes(axes[0], axes[1], axes[2]);
    }

    /**
     * Updates this quaternion to represent a rotation formed by the given three axes. These axes are assumed to be
     * orthogonal and no error checking is applied. It is the user's job to insure that the three axes being provided
     * indeed represent a proper right handed coordinate system.
     * 
     * @param xAxis
     *            vector representing the x-axis of the coordinate system.
     * @param yAxis
     *            vector representing the y-axis of the coordinate system.
     * @param zAxis
     *            vector representing the z-axis of the coordinate system.
     * @return this quaternion for chaining
     */
    public Quaternion fromAxes(final ReadOnlyVector3 xAxis, final ReadOnlyVector3 yAxis, final ReadOnlyVector3 zAxis) {
        return fromRotationMatrix(xAxis.getX(), yAxis.getX(), zAxis.getX(), xAxis.getY(), yAxis.getY(), zAxis.getY(),
                xAxis.getZ(), yAxis.getZ(), zAxis.getZ());
    }

    /**
     * Converts this quaternion to a rotation matrix and then extracts rotation axes.
     * 
     * @param axes
     *            the array of vectors to be filled.
     * @throws IllegalArgumentException
     *             if the given axes array is smaller than 3 elements.
     */
    public void toAxes(final Vector3 axes[]) {
        if (axes.length < 3) {
            throw new IllegalArgumentException("axes array must have at least three elements");
        }
        final Matrix3 tempMat = toRotationMatrix(Matrix3.fetchTempInstance());
        axes[0] = tempMat.getColumn(0, axes[0]);
        axes[1] = tempMat.getColumn(1, axes[1]);
        axes[2] = tempMat.getColumn(2, axes[2]);
        Matrix3.releaseTempInstance(tempMat);
    }

    /**
     * Does a spherical linear interpolation between this quaternion and the given end quaternion by the given change
     * amount.
     * 
     * @param endQuat
     * @param changeAmnt
     * @param store
     *            the quaternion to store the result in for return. If null, a new quaternion object is created and
     *            returned.
     * @return a new quaternion containing the result.
     */
    public Quaternion slerp(final ReadOnlyQuaternion endQuat, final double changeAmnt, final Quaternion store) {
        return slerp(this, endQuat, changeAmnt, store);
    }

    /**
     * Does a spherical linear interpolation between this quaternion and the given end quaternion by the given change
     * amount. Stores the results locally in this quaternion.
     * 
     * @param endQuat
     * @param changeAmnt
     * @return this quaternion for chaining.
     */
    public Quaternion slerpLocal(final ReadOnlyQuaternion endQuat, final double changeAmnt) {
        return slerpLocal(this, endQuat, changeAmnt);
    }

    /**
     * Does a spherical linear interpolation between the given start and end quaternions by the given change amount.
     * Returns the result as a new quaternion.
     * 
     * @param startQuat
     * @param endQuat
     * @param changeAmnt
     * @param store
     *            the quaternion to store the result in for return. If null, a new quaternion object is created and
     *            returned.
     * @return the new quaternion
     */
    public static Quaternion slerp(final ReadOnlyQuaternion startQuat, final ReadOnlyQuaternion endQuat,
            final double changeAmnt, final Quaternion store) {
        Quaternion result = store;
        if (result == null) {
            result = new Quaternion();
        }

        final Quaternion q2 = Quaternion.fetchTempInstance().set(endQuat);
        // Check for equality and skip operation.
        if (startQuat.equals(q2)) {
            return result.set(startQuat);
        }

        double dotP = startQuat.dot(q2);

        if (dotP < 0.0) {
            // Negate the second quaternion and the result of the dot product
            q2.multiplyLocal(-1);
            dotP = -dotP;
        }

        // Set the first and second scale for the interpolation
        double scale0 = 1 - changeAmnt;
        double scale1 = changeAmnt;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - dotP) > 0.1) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            final double theta = Math.acos(dotP);
            final double invSinTheta = 1f / MathUtils.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = MathUtils.sin((1 - changeAmnt) * theta) * invSinTheta;
            scale1 = MathUtils.sin((changeAmnt * theta)) * invSinTheta;
        }

        // Calculate the x, y, z and w values for the quaternion by using a
        // special form of linear interpolation for quaternions.
        final double x = (scale0 * startQuat.getX()) + (scale1 * q2.getX());
        final double y = (scale0 * startQuat.getY()) + (scale1 * q2.getY());
        final double z = (scale0 * startQuat.getZ()) + (scale1 * q2.getZ());
        final double w = (scale0 * startQuat.getW()) + (scale1 * q2.getW());

        Quaternion.releaseTempInstance(q2);

        // Return the interpolated quaternion
        return result.set(x, y, z, w);
    }

    /**
     * Does a spherical linear interpolation between the given start and end quaternions by the given change amount.
     * Stores the result locally.
     * 
     * @param startQuat
     * @param endQuat
     * @param changeAmnt
     * @return this quaternion for chaining.
     * 
     * 
     * @throws NullPointerException
     *             if startQuat or endQuat are null.
     */
    public Quaternion slerpLocal(final ReadOnlyQuaternion startQuat, final ReadOnlyQuaternion endQuat,
            final double changeAmnt) {
        // Check for equality and skip operation.
        if (startQuat.equals(endQuat)) {
            this.set(startQuat);
            return this;
        }

        double result = startQuat.dot(endQuat);
        final Quaternion end = Quaternion.fetchTempInstance().set(endQuat);

        if (result < 0.0) {
            // Negate the second quaternion and the result of the dot product
            end.multiplyLocal(-1);
            result = -result;
        }

        // Set the first and second scale for the interpolation
        double scale0 = 1 - changeAmnt;
        double scale1 = changeAmnt;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - result) > 0.1) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            final double theta = Math.acos(result);
            final double invSinTheta = 1f / MathUtils.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = MathUtils.sin((1 - changeAmnt) * theta) * invSinTheta;
            scale1 = MathUtils.sin((changeAmnt * theta)) * invSinTheta;
        }

        // Calculate the x, y, z and w values for the quaternion by using a
        // special form of linear interpolation for quaternions.
        final double x = (scale0 * startQuat.getX()) + (scale1 * end.getX());
        final double y = (scale0 * startQuat.getY()) + (scale1 * end.getY());
        final double z = (scale0 * startQuat.getZ()) + (scale1 * end.getZ());
        final double w = (scale0 * startQuat.getW()) + (scale1 * end.getW());
        set(x, y, z, w);

        Quaternion.releaseTempInstance(end);

        // Return the interpolated quaternion
        return this;
    }

    /**
     * Modifies this quaternion to equal the rotation required to point the z-axis at 'direction' and the y-axis to
     * 'up'.
     * 
     * @param direction
     *            where to 'look' at
     * @param up
     *            a vector indicating the local up direction.
     */
    public void lookAt(final ReadOnlyVector3 direction, final ReadOnlyVector3 up) {
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
     * @return the squared magnitude of this quaternion.
     */
    public double magnitudeSquared() {
        return getW() * getW() + getX() * getX() + getY() * getY() + getZ() * getZ();
    }

    /**
     * @return the magnitude of this quaternion. basically sqrt({@link #magnitude()})
     */
    public double magnitude() {
        final double magnitudeSQ = magnitudeSquared();
        if (magnitudeSQ == 1.0) {
            return 1.0;
        }

        return Math.sqrt(magnitudeSQ);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param w
     * @return the dot product of this quaternion with the given x,y,z and w values.
     */
    public double dot(final double x, final double y, final double z, final double w) {
        return getX() * x + getY() * y + getZ() * z + getW() * w;
    }

    /**
     * @param quat
     * @return the dot product of this quaternion with the given quaternion.
     */
    public double dot(final ReadOnlyQuaternion quat) {
        return dot(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
    }

    /**
     * Sets the value of this quaternion to (0, 0, 0, 1). Equivalent to calling set(0, 0, 0, 1)
     * 
     * @return this quaternion for chaining
     */
    public Quaternion setIdentity() {
        return set(0, 0, 0, 1);
    }

    /**
     * @return true if this quaternion is (0, 0, 0, 1)
     */
    public boolean isIdentity() {
        if (equals(IDENTITY)) {
            return true;
        }

        return false;
    }

    /**
     * Check a quaternion... if it is null or its doubles are NaN or infinite, return false. Else return true.
     * 
     * @param quat
     *            the quaternion to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final ReadOnlyQuaternion quat) {
        if (quat == null) {
            return false;
        }
        if (Double.isNaN(quat.getX()) || Double.isInfinite(quat.getX())) {
            return false;
        }
        if (Double.isNaN(quat.getY()) || Double.isInfinite(quat.getY())) {
            return false;
        }
        if (Double.isNaN(quat.getZ()) || Double.isInfinite(quat.getZ())) {
            return false;
        }
        if (Double.isNaN(quat.getW()) || Double.isInfinite(quat.getW())) {
            return false;
        }
        return true;
    }

    /**
     * @return the string representation of this quaternion.
     */
    @Override
    public String toString() {
        return "com.ardor3d.math.Quaternion [X=" + getX() + ", Y=" + getY() + ", Z=" + getZ() + ", W=" + getW() + "]";
    }

    /**
     * @return returns a unique code for this quaternion object based on its values. If two quaternions are numerically
     *         equal, they will return the same hash code value.
     */
    @Override
    public int hashCode() {
        int result = 17;

        final long x = Double.doubleToLongBits(getX());
        result += 31 * result + (int) (x ^ (x >>> 32));

        final long y = Double.doubleToLongBits(getY());
        result += 31 * result + (int) (y ^ (y >>> 32));

        final long z = Double.doubleToLongBits(getZ());
        result += 31 * result + (int) (z ^ (z >>> 32));

        final long w = Double.doubleToLongBits(getW());
        result += 31 * result + (int) (w ^ (w >>> 32));

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this quaternion and the provided quaternion have the same x, y, z and w values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyQuaternion)) {
            return false;
        }
        final ReadOnlyQuaternion comp = (ReadOnlyQuaternion) o;
        return getX() == comp.getX() && getY() == comp.getY() && getZ() == comp.getZ() && getW() == comp.getW();

    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Quaternion clone() {
        try {
            return (Quaternion) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends Quaternion> getClassTag() {
        return this.getClass();
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(getX(), "x", 0);
        capsule.write(getY(), "y", 0);
        capsule.write(getY(), "z", 0);
        capsule.write(getY(), "w", 1);
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);
        setX(capsule.readDouble("x", 0));
        setY(capsule.readDouble("y", 0));
        setZ(capsule.readDouble("z", 0));
        setW(capsule.readDouble("w", 1));
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
        setX(in.readDouble());
        setY(in.readDouble());
        setZ(in.readDouble());
        setW(in.readDouble());
    }

    /**
     * Used with serialization. Not to be called manually.
     * 
     * @param out
     *            ObjectOutput
     * @throws IOException
     */
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeDouble(getX());
        out.writeDouble(getY());
        out.writeDouble(getZ());
        out.writeDouble(getW());
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Quaternion that is intended for temporary use in calculations and so forth. Multiple calls
     *         to the method should return instances of this class that are not currently in use.
     */
    public final static Quaternion fetchTempInstance() {
        if (Constants.useMathPools) {
            return QUAT_POOL.fetch();
        } else {
            return new Quaternion();
        }
    }

    /**
     * Releases a Quaternion back to be used by a future call to fetchTempInstance. TAKE CARE: this Quaternion object
     * should no longer have other classes referencing it or "Bad Things" will happen.
     * 
     * @param mat
     *            the Quaternion to release.
     */
    public final static void releaseTempInstance(final Quaternion mat) {
        if (Constants.useMathPools) {
            QUAT_POOL.release(mat);
        }
    }
}
