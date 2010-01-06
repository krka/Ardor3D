/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.bounding;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyPlane;
import com.ardor3d.math.type.ReadOnlyRay3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.type.ReadOnlyPlane.Side;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>BoundingBox</code> defines an axis-aligned cube that defines a container for a group of vertices of a
 * particular piece of geometry. This box defines a center and extents from that center along the x, y and z axis. <br>
 * <br>
 * A typical usage is to allow the class define the center and radius by calling either <code>containAABB</code> or
 * <code>averagePoints</code>. A call to <code>computeFramePoint</code> in turn calls <code>containAABB</code>.
 */
public class BoundingBox extends BoundingVolume {

    private static final long serialVersionUID = 1L;

    private double _xExtent, _yExtent, _zExtent;

    /**
     * Default constructor instantiates a new <code>BoundingBox</code> object.
     */
    public BoundingBox() {}

    /**
     * Constructor instantiates a new <code>BoundingBox</code> object with given values.
     */
    public BoundingBox(final BoundingBox other) {
        this(other.getCenter(), other.getXExtent(), other.getYExtent(), other.getZExtent());
    }

    /**
     * Constructor instantiates a new <code>BoundingBox</code> object with given values.
     */
    public BoundingBox(final ReadOnlyVector3 c, final double x, final double y, final double z) {
        _center.set(c);
        setXExtent(x);
        setYExtent(y);
        setZExtent(z);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof BoundingBox)) {
            return false;
        }
        final BoundingBox b = (BoundingBox) other;
        return _center.equals(b._center) && _xExtent == b._xExtent && _yExtent == b._yExtent && _zExtent == b._zExtent;
    }

    @Override
    public Type getType() {
        return Type.AABB;
    }

    public void setXExtent(final double xExtent) {
        _xExtent = xExtent;
    }

    public double getXExtent() {
        return _xExtent;
    }

    public void setYExtent(final double yExtent) {
        _yExtent = yExtent;
    }

    public double getYExtent() {
        return _yExtent;
    }

    public void setZExtent(final double zExtent) {
        _zExtent = zExtent;
    }

    public double getZExtent() {
        return _zExtent;
    }

    // Some transform matrices are not in decomposed form and in this
    // situation we need to use a different, more robust, algorithm
    // for computing the new bounding box.
    @Override
    public BoundingVolume transform(final ReadOnlyTransform transform, final BoundingVolume store) {

        if (transform.isRotationMatrix()) {
            return transformRotational(transform, store);
        }

        BoundingBox box;
        if (store == null || store.getType() != Type.AABB) {
            box = new BoundingBox();
        } else {
            box = (BoundingBox) store;
        }

        final Vector3[] corners = new Vector3[8];
        for (int i = 0; i < corners.length; i++) {
            corners[i] = Vector3.fetchTempInstance();
        }
        int index = 0;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    corners[index++].set(_center.getX() + ((x == 0) ? 1 : -1) * _xExtent, _center.getY()
                            + ((y == 0) ? 1 : -1) * _yExtent, _center.getZ() + ((z == 0) ? 1 : -1) * _zExtent);
                }
            }
        }
        // Transform all of these points by the transform
        for (int i = 0; i < corners.length; i++) {
            transform.applyForward(corners[i]);
        }
        // Now compute based on these transformed points
        double minX = corners[0].getX();
        double minY = corners[0].getY();
        double minZ = corners[0].getZ();
        double maxX = minX;
        double maxY = minY;
        double maxZ = minZ;
        for (int i = 1; i < corners.length; i++) {
            final double curX = corners[i].getX();
            final double curY = corners[i].getY();
            final double curZ = corners[i].getZ();
            minX = Math.min(minX, curX);
            minY = Math.min(minY, curY);
            minZ = Math.min(minZ, curZ);
            maxX = Math.max(maxX, curX);
            maxY = Math.max(maxY, curY);
            maxZ = Math.max(maxZ, curZ);
        }

        final double ctrX = (maxX + minX) * 0.5;
        final double ctrY = (maxY + minY) * 0.5;
        final double ctrZ = (maxZ + minZ) * 0.5;

        box._center.set(ctrX, ctrY, ctrZ);
        box._xExtent = maxX - ctrX;
        box._yExtent = maxY - ctrY;
        box._zExtent = maxZ - ctrZ;

        for (int i = 0; i < corners.length; i++) {
            Vector3.releaseTempInstance(corners[i]);
        }

        return box;
    }

    public BoundingVolume transformRotational(final ReadOnlyTransform transform, final BoundingVolume store) {

        final ReadOnlyMatrix3 rotate = transform.getMatrix();
        final ReadOnlyVector3 scale = transform.getScale();
        final ReadOnlyVector3 translate = transform.getTranslation();

        BoundingBox box;
        if (store == null || store.getType() != Type.AABB) {
            box = new BoundingBox();
        } else {
            box = (BoundingBox) store;
        }

        _center.multiply(scale, box._center);
        rotate.applyPost(box._center, box._center);
        box._center.addLocal(translate);

        final Vector3 compVect1 = Vector3.fetchTempInstance();

        final Matrix3 transMatrix = Matrix3.fetchTempInstance();
        transMatrix.set(rotate);
        // Make the rotation matrix all positive to get the maximum x/y/z extent
        transMatrix.setValue(0, 0, Math.abs(transMatrix.getValue(0, 0)));
        transMatrix.setValue(0, 1, Math.abs(transMatrix.getValue(0, 1)));
        transMatrix.setValue(0, 2, Math.abs(transMatrix.getValue(0, 2)));
        transMatrix.setValue(1, 0, Math.abs(transMatrix.getValue(1, 0)));
        transMatrix.setValue(1, 1, Math.abs(transMatrix.getValue(1, 1)));
        transMatrix.setValue(1, 2, Math.abs(transMatrix.getValue(1, 2)));
        transMatrix.setValue(2, 0, Math.abs(transMatrix.getValue(2, 0)));
        transMatrix.setValue(2, 1, Math.abs(transMatrix.getValue(2, 1)));
        transMatrix.setValue(2, 2, Math.abs(transMatrix.getValue(2, 2)));

        compVect1.set(getXExtent() * scale.getX(), getYExtent() * scale.getY(), getZExtent() * scale.getZ());
        transMatrix.applyPost(compVect1, compVect1);
        // Assign the biggest rotations after scales.
        box.setXExtent(Math.abs(compVect1.getX()));
        box.setYExtent(Math.abs(compVect1.getY()));
        box.setZExtent(Math.abs(compVect1.getZ()));

        Vector3.releaseTempInstance(compVect1);
        Matrix3.releaseTempInstance(transMatrix);

        return box;
    }

    /**
     * <code>computeFromPoints</code> creates a new Bounding Box from a given set of points. It uses the
     * <code>containAABB</code> method as default.
     * 
     * @param points
     *            the points to contain.
     */
    @Override
    public void computeFromPoints(final FloatBuffer points) {
        containAABB(points);
    }

    @Override
    public void computeFromPrimitives(final MeshData data, final int section, final int[] indices, final int start,
            final int end) {
        if (end - start <= 0) {
            return;
        }

        final Vector3 min = Vector3.fetchTempInstance().set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY);
        final Vector3 max = Vector3.fetchTempInstance().set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY);

        final int vertsPerPrimitive = data.getIndexMode(section).getVertexCount();
        Vector3[] store = new Vector3[vertsPerPrimitive];

        for (int i = start; i < end; i++) {
            store = data.getPrimitive(indices[i], section, store);
            for (int j = 0; j < store.length; j++) {
                checkMinMax(min, max, store[j]);
            }
        }

        _center.set(min.addLocal(max));
        _center.multiplyLocal(0.5);

        setXExtent(max.getX() - _center.getX());
        setYExtent(max.getY() - _center.getY());
        setZExtent(max.getZ() - _center.getZ());

        Vector3.releaseTempInstance(min);
        Vector3.releaseTempInstance(max);
    }

    private void checkMinMax(final Vector3 min, final Vector3 max, final ReadOnlyVector3 point) {
        if (point.getX() < min.getX()) {
            min.setX(point.getX());
        }
        if (point.getX() > max.getX()) {
            max.setX(point.getX());
        }

        if (point.getY() < min.getY()) {
            min.setY(point.getY());
        }
        if (point.getY() > max.getY()) {
            max.setY(point.getY());
        }

        if (point.getZ() < min.getZ()) {
            min.setZ(point.getZ());
        }
        if (point.getZ() > max.getZ()) {
            max.setZ(point.getZ());
        }
    }

    /**
     * <code>containAABB</code> creates a minimum-volume axis-aligned bounding box of the points, then selects the
     * smallest enclosing sphere of the box with the sphere centered at the boxes center.
     * 
     * @param points
     *            the list of points.
     */
    public void containAABB(final FloatBuffer points) {
        if (points == null) {
            return;
        }

        points.rewind();
        if (points.remaining() <= 2) {
            return;
        }

        final Vector3 compVect = Vector3.fetchTempInstance();
        BufferUtils.populateFromBuffer(compVect, points, 0);
        double minX = compVect.getX(), minY = compVect.getY(), minZ = compVect.getZ();
        double maxX = compVect.getX(), maxY = compVect.getY(), maxZ = compVect.getZ();

        for (int i = 1, len = points.remaining() / 3; i < len; i++) {
            BufferUtils.populateFromBuffer(compVect, points, i);

            if (compVect.getX() < minX) {
                minX = compVect.getX();
            } else if (compVect.getX() > maxX) {
                maxX = compVect.getX();
            }

            if (compVect.getY() < minY) {
                minY = compVect.getY();
            } else if (compVect.getY() > maxY) {
                maxY = compVect.getY();
            }

            if (compVect.getZ() < minZ) {
                minZ = compVect.getZ();
            } else if (compVect.getZ() > maxZ) {
                maxZ = compVect.getZ();
            }
        }
        Vector3.releaseTempInstance(compVect);

        _center.set(minX + maxX, minY + maxY, minZ + maxZ);
        _center.multiplyLocal(0.5);

        setXExtent(maxX - _center.getX());
        setYExtent(maxY - _center.getY());
        setZExtent(maxZ - _center.getZ());
    }

    /**
     * <code>whichSide</code> takes a plane (typically provided by a view frustum) to determine which side this bound is
     * on.
     * 
     * @param plane
     *            the plane to check against.
     */
    @Override
    public Side whichSide(final ReadOnlyPlane plane) {
        final ReadOnlyVector3 normal = plane.getNormal();
        final double radius = Math.abs(getXExtent() * normal.getX()) + Math.abs(getYExtent() * normal.getY())
                + Math.abs(getZExtent() * normal.getZ());

        final double distance = plane.pseudoDistance(_center);

        if (distance < -radius) {
            return Plane.Side.Inside;
        } else if (distance > radius) {
            return Plane.Side.Outside;
        } else {
            return Plane.Side.Neither;
        }
    }

    /**
     * <code>merge</code> combines this sphere with a second bounding sphere. This new sphere contains both bounding
     * spheres and is returned.
     * 
     * @param volume
     *            the sphere to combine with this sphere.
     * @return the new sphere
     */
    @Override
    public BoundingVolume merge(final BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {
            case AABB: {
                final BoundingBox vBox = (BoundingBox) volume;
                return merge(vBox._center, vBox.getXExtent(), vBox.getYExtent(), vBox.getZExtent(), new BoundingBox(
                        new Vector3(0, 0, 0), 0, 0, 0));
            }

            case Sphere: {
                final BoundingSphere vSphere = (BoundingSphere) volume;
                return merge(vSphere._center, vSphere.getRadius(), vSphere.getRadius(), vSphere.getRadius(),
                        new BoundingBox(new Vector3(0, 0, 0), 0, 0, 0));
            }

            case OBB: {
                final OrientedBoundingBox box = (OrientedBoundingBox) volume;
                final BoundingBox rVal = (BoundingBox) this.clone(null);
                return rVal.mergeOBB(box);
            }

            default:
                return null;
        }
    }

    /**
     * <code>mergeLocal</code> combines this sphere with a second bounding sphere locally. Altering this sphere to
     * contain both the original and the additional sphere volumes;
     * 
     * @param volume
     *            the sphere to combine with this sphere.
     * @return this
     */
    @Override
    public BoundingVolume mergeLocal(final BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {
            case AABB: {
                final BoundingBox vBox = (BoundingBox) volume;
                return merge(vBox._center, vBox.getXExtent(), vBox.getYExtent(), vBox.getZExtent(), this);
            }

            case Sphere: {
                final BoundingSphere vSphere = (BoundingSphere) volume;
                return merge(vSphere._center, vSphere.getRadius(), vSphere.getRadius(), vSphere.getRadius(), this);
            }

            case OBB: {
                return mergeOBB((OrientedBoundingBox) volume);
            }

            default:
                return null;
        }
    }

    /**
     * Merges this AABB with the given OBB.
     * 
     * @param volume
     *            the OBB to merge this AABB with.
     * @return This AABB extended to fit the given OBB.
     */
    private BoundingBox mergeOBB(final OrientedBoundingBox volume) {
        if (!volume.correctCorners) {
            volume.computeCorners();
        }

        double minX, minY, minZ;
        double maxX, maxY, maxZ;

        minX = _center.getX() - getXExtent();
        minY = _center.getY() - getYExtent();
        minZ = _center.getZ() - getZExtent();

        maxX = _center.getX() + getXExtent();
        maxY = _center.getY() + getYExtent();
        maxZ = _center.getZ() + getZExtent();

        for (int i = 1; i < volume._vectorStore.length; i++) {
            final Vector3 temp = volume._vectorStore[i];
            if (temp.getX() < minX) {
                minX = temp.getX();
            } else if (temp.getX() > maxX) {
                maxX = temp.getX();
            }

            if (temp.getY() < minY) {
                minY = temp.getY();
            } else if (temp.getY() > maxY) {
                maxY = temp.getY();
            }

            if (temp.getZ() < minZ) {
                minZ = temp.getZ();
            } else if (temp.getZ() > maxZ) {
                maxZ = temp.getZ();
            }
        }

        _center.set(minX + maxX, minY + maxY, minZ + maxZ);
        _center.multiplyLocal(0.5);

        setXExtent(maxX - _center.getX());
        setYExtent(maxY - _center.getY());
        setZExtent(maxZ - _center.getZ());
        return this;
    }

    /**
     * <code>merge</code> combines this bounding box with another box which is defined by the center, x, y, z extents.
     * 
     * @param boxCenter
     *            the center of the box to merge with
     * @param boxX
     *            the x extent of the box to merge with.
     * @param boxY
     *            the y extent of the box to merge with.
     * @param boxZ
     *            the z extent of the box to merge with.
     * @param rVal
     *            the resulting merged box.
     * @return the resulting merged box.
     */
    private BoundingBox merge(final Vector3 boxCenter, final double boxX, final double boxY, final double boxZ,
            final BoundingBox rVal) {
        final Vector3 compVect1 = Vector3.fetchTempInstance();
        final Vector3 compVect2 = Vector3.fetchTempInstance();

        compVect1.setX(_center.getX() - getXExtent());
        if (compVect1.getX() > boxCenter.getX() - boxX) {
            compVect1.setX(boxCenter.getX() - boxX);
        }
        compVect1.setY(_center.getY() - getYExtent());
        if (compVect1.getY() > boxCenter.getY() - boxY) {
            compVect1.setY(boxCenter.getY() - boxY);
        }
        compVect1.setZ(_center.getZ() - getZExtent());
        if (compVect1.getZ() > boxCenter.getZ() - boxZ) {
            compVect1.setZ(boxCenter.getZ() - boxZ);
        }

        compVect2.setX(_center.getX() + getXExtent());
        if (compVect2.getX() < boxCenter.getX() + boxX) {
            compVect2.setX(boxCenter.getX() + boxX);
        }
        compVect2.setY(_center.getY() + getYExtent());
        if (compVect2.getY() < boxCenter.getY() + boxY) {
            compVect2.setY(boxCenter.getY() + boxY);
        }
        compVect2.setZ(_center.getZ() + getZExtent());
        if (compVect2.getZ() < boxCenter.getZ() + boxZ) {
            compVect2.setZ(boxCenter.getZ() + boxZ);
        }

        _center.set(compVect2).addLocal(compVect1).multiplyLocal(0.5);

        setXExtent(compVect2.getX() - _center.getX());
        setYExtent(compVect2.getY() - _center.getY());
        setZExtent(compVect2.getZ() - _center.getZ());

        Vector3.releaseTempInstance(compVect1);
        Vector3.releaseTempInstance(compVect2);

        return rVal;
    }

    /**
     * <code>clone</code> creates a new BoundingBox object containing the same data as this one.
     * 
     * @param store
     *            where to store the cloned information. if null or wrong class, a new store is created.
     * @return the new BoundingBox
     */
    @Override
    public BoundingVolume clone(final BoundingVolume store) {
        if (store != null && store.getType() == Type.AABB) {
            final BoundingBox rVal = (BoundingBox) store;
            rVal._center.set(_center);
            rVal.setXExtent(_xExtent);
            rVal.setYExtent(_yExtent);
            rVal.setZExtent(_zExtent);
            rVal._checkPlane = _checkPlane;
            return rVal;
        }

        final BoundingBox rVal = new BoundingBox(_center, getXExtent(), getYExtent(), getZExtent());
        return rVal;
    }

    /**
     * <code>toString</code> returns the string representation of this object. The form is:
     * "Radius: RRR.SSSS Center: <Vector>".
     * 
     * @return the string representation of this.
     */
    @Override
    public String toString() {
        return "com.ardor3d.scene.BoundingBox [Center: " + _center + "  xExtent: " + getXExtent() + "  yExtent: "
                + getYExtent() + "  zExtent: " + getZExtent() + "]";
    }

    /**
     * intersects determines if this Bounding Box intersects with another given bounding volume. If so, true is
     * returned, otherwise, false is returned.
     * 
     * @see com.ardor3d.bounding.BoundingVolume#intersects(com.ardor3d.bounding.BoundingVolume)
     */
    @Override
    public boolean intersects(final BoundingVolume bv) {
        if (bv == null) {
            return false;
        }

        return bv.intersectsBoundingBox(this);
    }

    /**
     * determines if this bounding box intersects a given bounding sphere.
     * 
     * @see com.ardor3d.bounding.BoundingVolume#intersectsSphere(com.ardor3d.bounding.BoundingSphere)
     */
    @Override
    public boolean intersectsSphere(final BoundingSphere bs) {
        if (!Vector3.isValid(_center) || !Vector3.isValid(bs._center)) {
            return false;
        }

        if (Math.abs(_center.getX() - bs.getCenter().getX()) < bs.getRadius() + getXExtent()
                && Math.abs(_center.getY() - bs.getCenter().getY()) < bs.getRadius() + getYExtent()
                && Math.abs(_center.getZ() - bs.getCenter().getZ()) < bs.getRadius() + getZExtent()) {
            return true;
        }

        return false;
    }

    /**
     * determines if this bounding box intersects a given bounding box. If the two boxes intersect in any way, true is
     * returned. Otherwise, false is returned.
     * 
     * @see com.ardor3d.bounding.BoundingVolume#intersectsBoundingBox(com.ardor3d.bounding.BoundingBox)
     */
    @Override
    public boolean intersectsBoundingBox(final BoundingBox bb) {
        if (!Vector3.isValid(_center) || !Vector3.isValid(bb._center)) {
            return false;
        }

        if (_center.getX() + getXExtent() < bb._center.getX() - bb.getXExtent()
                || _center.getX() - getXExtent() > bb._center.getX() + bb.getXExtent()) {
            return false;
        } else if (_center.getY() + getYExtent() < bb._center.getY() - bb.getYExtent()
                || _center.getY() - getYExtent() > bb._center.getY() + bb.getYExtent()) {
            return false;
        } else if (_center.getZ() + getZExtent() < bb._center.getZ() - bb.getZExtent()
                || _center.getZ() - getZExtent() > bb._center.getZ() + bb.getZExtent()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * determines if this bounding box intersects with a given oriented bounding box.
     * 
     * @see com.ardor3d.bounding.BoundingVolume#intersectsOrientedBoundingBox(com.ardor3d.bounding.OrientedBoundingBox)
     */
    @Override
    public boolean intersectsOrientedBoundingBox(final OrientedBoundingBox obb) {
        return obb.intersectsBoundingBox(this);
    }

    /**
     * determines if this bounding box intersects with a given ray object. If an intersection has occurred, true is
     * returned, otherwise false is returned.
     * 
     * @see com.ardor3d.bounding.BoundingVolume#intersects(com.ardor3d.math.Ray)
     */
    @Override
    public boolean intersects(final ReadOnlyRay3 ray) {
        if (!Vector3.isValid(_center)) {
            return false;
        }

        final Vector3 compVect1 = Vector3.fetchTempInstance();
        final Vector3 compVect2 = Vector3.fetchTempInstance();

        try {
            final Vector3 diff = ray.getOrigin().subtract(getCenter(), compVect1);

            final double fWdU0 = ray.getDirection().dot(Vector3.UNIT_X);
            final double fAWdU0 = Math.abs(fWdU0);
            final double fDdU0 = diff.dot(Vector3.UNIT_X);
            final double fADdU0 = Math.abs(fDdU0);
            if (fADdU0 > getXExtent() && fDdU0 * fWdU0 >= 0.0) {
                return false;
            }

            final double fWdU1 = ray.getDirection().dot(Vector3.UNIT_Y);
            final double fAWdU1 = Math.abs(fWdU1);
            final double fDdU1 = diff.dot(Vector3.UNIT_Y);
            final double fADdU1 = Math.abs(fDdU1);
            if (fADdU1 > getYExtent() && fDdU1 * fWdU1 >= 0.0) {
                return false;
            }

            final double fWdU2 = ray.getDirection().dot(Vector3.UNIT_Z);
            final double fAWdU2 = Math.abs(fWdU2);
            final double fDdU2 = diff.dot(Vector3.UNIT_Z);
            final double fADdU2 = Math.abs(fDdU2);
            if (fADdU2 > getZExtent() && fDdU2 * fWdU2 >= 0.0) {
                return false;
            }

            final Vector3 wCrossD = ray.getDirection().cross(diff, compVect2);

            final double fAWxDdU0 = Math.abs(wCrossD.dot(Vector3.UNIT_X));
            double rhs = getYExtent() * fAWdU2 + getZExtent() * fAWdU1;
            if (fAWxDdU0 > rhs) {
                return false;
            }

            final double fAWxDdU1 = Math.abs(wCrossD.dot(Vector3.UNIT_Y));
            rhs = getXExtent() * fAWdU2 + getZExtent() * fAWdU0;
            if (fAWxDdU1 > rhs) {
                return false;
            }

            final double fAWxDdU2 = Math.abs(wCrossD.dot(Vector3.UNIT_Z));
            rhs = getXExtent() * fAWdU1 + getYExtent() * fAWdU0;
            if (fAWxDdU2 > rhs) {
                return false;
            }

            return true;
        } finally {
            Vector3.releaseTempInstance(compVect1);
            Vector3.releaseTempInstance(compVect2);
        }
    }

    /**
     * @see com.ardor3d.bounding.BoundingVolume#intersectsWhere(com.ardor3d.math.Ray)
     */
    @Override
    public IntersectionRecord intersectsWhere(final ReadOnlyRay3 ray) {
        final Vector3 compVect1 = Vector3.fetchTempInstance();
        final Vector3 compVect2 = Vector3.fetchTempInstance();

        final Vector3 diff = ray.getOrigin().subtract(_center, compVect1);

        final ReadOnlyVector3 direction = ray.getDirection();

        final double[] t = { 0.0, Double.POSITIVE_INFINITY };

        final double saveT0 = t[0], saveT1 = t[1];
        final boolean notEntirelyClipped = clip(direction.getX(), -diff.getX() - getXExtent(), t)
                && clip(-direction.getX(), diff.getX() - getXExtent(), t)
                && clip(direction.getY(), -diff.getY() - getYExtent(), t)
                && clip(-direction.getY(), diff.getY() - getYExtent(), t)
                && clip(direction.getZ(), -diff.getZ() - getZExtent(), t)
                && clip(-direction.getZ(), diff.getZ() - getZExtent(), t);

        if (notEntirelyClipped && (t[0] != saveT0 || t[1] != saveT1)) {
            if (t[1] > t[0]) {
                final double[] distances = t;
                final Vector3[] points = new Vector3[] {
                        new Vector3(ray.getDirection()).multiplyLocal(distances[0]).addLocal(ray.getOrigin()),
                        new Vector3(ray.getDirection()).multiplyLocal(distances[1]).addLocal(ray.getOrigin()) };
                final IntersectionRecord record = new IntersectionRecord(distances, points);
                Vector3.releaseTempInstance(compVect1);
                Vector3.releaseTempInstance(compVect2);
                return record;
            }

            final double[] distances = new double[] { t[0] };
            final Vector3[] points = new Vector3[] { new Vector3(ray.getDirection()).multiplyLocal(distances[0])
                    .addLocal(ray.getOrigin()), };
            final IntersectionRecord record = new IntersectionRecord(distances, points);

            Vector3.releaseTempInstance(compVect1);
            Vector3.releaseTempInstance(compVect2);
            return record;
        }

        return new IntersectionRecord();

    }

    @Override
    public boolean contains(final ReadOnlyVector3 point) {
        return Math.abs(_center.getX() - point.getX()) < getXExtent()
                && Math.abs(_center.getY() - point.getY()) < getYExtent()
                && Math.abs(_center.getZ() - point.getZ()) < getZExtent();
    }

    @Override
    public double distanceToEdge(final ReadOnlyVector3 point) {
        // compute coordinates of point in box coordinate system
        final Vector3 closest = point.subtract(_center, Vector3.fetchTempInstance());

        // project test point onto box
        double sqrDistance = 0.0;
        double delta;

        if (closest.getX() < -getXExtent()) {
            delta = closest.getX() + getXExtent();
            sqrDistance += delta * delta;
            closest.setX(-getXExtent());
        } else if (closest.getX() > getXExtent()) {
            delta = closest.getX() - getXExtent();
            sqrDistance += delta * delta;
            closest.setX(getXExtent());
        }

        if (closest.getY() < -getYExtent()) {
            delta = closest.getY() + getYExtent();
            sqrDistance += delta * delta;
            closest.setY(-getYExtent());
        } else if (closest.getY() > getYExtent()) {
            delta = closest.getY() - getYExtent();
            sqrDistance += delta * delta;
            closest.setY(getYExtent());
        }

        if (closest.getZ() < -getZExtent()) {
            delta = closest.getZ() + getZExtent();
            sqrDistance += delta * delta;
            closest.setZ(-getZExtent());
        } else if (closest.getZ() > getZExtent()) {
            delta = closest.getZ() - getZExtent();
            sqrDistance += delta * delta;
            closest.setZ(getZExtent());
        }
        Vector3.releaseTempInstance(closest);

        return Math.sqrt(sqrDistance);
    }

    /**
     * <code>clip</code> determines if a line segment intersects the current test plane.
     * 
     * @param denom
     *            the denominator of the line segment.
     * @param numer
     *            the numerator of the line segment.
     * @param t
     *            test values of the plane.
     * @return true if the line segment intersects the plane, false otherwise.
     */
    private boolean clip(final double denom, final double numer, final double[] t) {
        // Return value is 'true' if line segment intersects the current test
        // plane. Otherwise 'false' is returned in which case the line segment
        // is entirely clipped.
        if (denom > 0.0) {
            if (numer > denom * t[1]) {
                return false;
            }
            if (numer > denom * t[0]) {
                t[0] = numer / denom;
            }
            return true;
        } else if (denom < 0.0) {
            if (numer > denom * t[0]) {
                return false;
            }
            if (numer > denom * t[1]) {
                t[1] = numer / denom;
            }
            return true;
        } else {
            return numer <= 0.0;
        }
    }

    /**
     * Query extent.
     * 
     * @param store
     *            where extent gets stored - null to return a new vector
     * @return store / new vector
     */
    public Vector3 getExtent(Vector3 store) {
        if (store == null) {
            store = new Vector3();
        }
        store.set(getXExtent(), getYExtent(), getZExtent());
        return store;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(getXExtent(), "xExtent", 0);
        capsule.write(getYExtent(), "yExtent", 0);
        capsule.write(getZExtent(), "zExtent", 0);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        setXExtent(capsule.readDouble("xExtent", 0));
        setYExtent(capsule.readDouble("yExtent", 0));
        setZExtent(capsule.readDouble("zExtent", 0));
    }

    @Override
    public double getVolume() {
        return (8 * getXExtent() * getYExtent() * getZExtent());
    }
}
