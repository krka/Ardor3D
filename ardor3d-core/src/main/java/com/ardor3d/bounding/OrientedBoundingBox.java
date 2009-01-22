/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
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
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyPlane;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyRay3;
import com.ardor3d.math.type.ReadOnlyTriangle;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.type.ReadOnlyPlane.Side;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

public class OrientedBoundingBox extends BoundingVolume {

    private static final long serialVersionUID = 1L;

    /** X axis of the Oriented Box. */
    protected final Vector3 xAxis = new Vector3(1, 0, 0);

    /** Y axis of the Oriented Box. */
    protected final Vector3 yAxis = new Vector3(0, 1, 0);

    /** Z axis of the Oriented Box. */
    protected final Vector3 zAxis = new Vector3(0, 0, 1);

    /** Extents of the box along the x,y,z axis. */
    protected final Vector3 extent = new Vector3(0, 0, 0);

    /** Vector array used to store the array of 8 corners the box has. */
    protected final Vector3[] vectorStore = new Vector3[8];

    /**
     * If true, the box's vectorStore array correctly represents the box's corners.
     */
    public boolean correctCorners = false;

    public OrientedBoundingBox() {
        for (int x = 0; x < 8; x++) {
            vectorStore[x] = new Vector3();
        }
    }

    @Override
    public Type getType() {
        return Type.OBB;
    }

    @Override
    public BoundingVolume transform(final ReadOnlyQuaternion rotate, final ReadOnlyVector3 translate,
            final ReadOnlyVector3 scale, final BoundingVolume store) {
        final Matrix3 tempMa = Matrix3.fetchTempInstance();
        rotate.toRotationMatrix(tempMa);
        final BoundingVolume volume = transform(tempMa, translate, scale, store);
        Matrix3.releaseTempInstance(tempMa);
        return volume;
    }

    @Override
    public BoundingVolume transform(final ReadOnlyMatrix3 rotate, final ReadOnlyVector3 translate,
            final ReadOnlyVector3 scale, BoundingVolume store) {
        if (store == null || store.getType() != Type.OBB) {
            store = new OrientedBoundingBox();
        }
        final OrientedBoundingBox toReturn = (OrientedBoundingBox) store;
        toReturn.extent.set(Math.abs(extent.getX() * scale.getX()), Math.abs(extent.getY() * scale.getY()), Math
                .abs(extent.getZ() * scale.getZ()));
        rotate.applyPost(xAxis, toReturn.xAxis);
        rotate.applyPost(yAxis, toReturn.yAxis);
        rotate.applyPost(zAxis, toReturn.zAxis);
        center.multiply(scale, toReturn.center);
        rotate.applyPost(toReturn.center, toReturn.center);
        toReturn.center.addLocal(translate);
        toReturn.correctCorners = false;
        return toReturn;
    }

    @Override
    public Side whichSide(final ReadOnlyPlane plane) {
        final ReadOnlyVector3 planeNormal = plane.getNormal();
        final double fRadius = Math.abs(extent.getX() * (planeNormal.dot(xAxis)))
                + Math.abs(extent.getY() * (planeNormal.dot(yAxis)))
                + Math.abs(extent.getZ() * (planeNormal.dot(zAxis)));
        final double fDistance = plane.pseudoDistance(center);
        if (fDistance <= -fRadius) {
            return Plane.Side.Inside;
        } else if (fDistance >= fRadius) {
            return Plane.Side.Outside;
        } else {
            return Plane.Side.Neither;
        }
    }

    @Override
    public void computeFromPoints(final FloatBuffer points) {
        containAABB(points);
    }

    /**
     * Calculates an AABB of the given point values for this OBB.
     * 
     * @param points
     *            The points this OBB should contain.
     */
    private void containAABB(final FloatBuffer points) {
        if (points == null || points.limit() <= 2) { // we need at least a 3
            // double vector
            return;
        }

        final Vector3 compVect1 = Vector3.fetchTempInstance();
        BufferUtils.populateFromBuffer(compVect1, points, 0);
        double minX = compVect1.getX(), minY = compVect1.getY(), minZ = compVect1.getZ();
        double maxX = compVect1.getX(), maxY = compVect1.getY(), maxZ = compVect1.getZ();

        for (int i = 1, len = points.limit() / 3; i < len; i++) {
            BufferUtils.populateFromBuffer(compVect1, points, i);

            if (compVect1.getX() < minX) {
                minX = compVect1.getX();
            } else if (compVect1.getX() > maxX) {
                maxX = compVect1.getX();
            }

            if (compVect1.getY() < minY) {
                minY = compVect1.getY();
            } else if (compVect1.getY() > maxY) {
                maxY = compVect1.getY();
            }

            if (compVect1.getZ() < minZ) {
                minZ = compVect1.getZ();
            } else if (compVect1.getZ() > maxZ) {
                maxZ = compVect1.getZ();
            }
        }
        Vector3.releaseTempInstance(compVect1);

        center.set(minX + maxX, minY + maxY, minZ + maxZ);
        center.multiplyLocal(0.5);

        extent.set(maxX - center.getX(), maxY - center.getY(), maxZ - center.getZ());

        xAxis.set(1, 0, 0);
        yAxis.set(0, 1, 0);
        zAxis.set(0, 0, 1);

        correctCorners = false;
    }

    @Override
    public BoundingVolume merge(final BoundingVolume volume) {
        // clone ourselves into a new bounding volume, then merge.
        return clone(new OrientedBoundingBox()).mergeLocal(volume);
    }

    @Override
    public BoundingVolume mergeLocal(final BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {

            case OBB: {
                return mergeOBB((OrientedBoundingBox) volume);
            }

            case AABB: {
                return mergeAABB((BoundingBox) volume);
            }

            case Sphere: {
                return mergeSphere((BoundingSphere) volume);
            }

            default:
                return null;

        }
    }

    private BoundingVolume mergeSphere(final BoundingSphere volume) {
        final BoundingSphere mergeSphere = volume;
        if (!correctCorners) {
            computeCorners();
        }

        final FloatBuffer mergeBuf = BufferUtils.createFloatBufferOnHeap(16 * 3);

        mergeBuf.rewind();
        for (int i = 0; i < 8; i++) {
            mergeBuf.put((float) vectorStore[i].getX());
            mergeBuf.put((float) vectorStore[i].getY());
            mergeBuf.put((float) vectorStore[i].getZ());
        }
        mergeBuf.put((float) (mergeSphere.center.getX() + mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getY() + mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getZ() + mergeSphere.getRadius()));
        mergeBuf.put((float) (mergeSphere.center.getX() - mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getY() + mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getZ() + mergeSphere.getRadius()));
        mergeBuf.put((float) (mergeSphere.center.getX() + mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getY() - mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getZ() + mergeSphere.getRadius()));
        mergeBuf.put((float) (mergeSphere.center.getX() + mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getY() + mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getZ() - mergeSphere.getRadius()));
        mergeBuf.put((float) (mergeSphere.center.getX() - mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getY() - mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getZ() + mergeSphere.getRadius()));
        mergeBuf.put((float) (mergeSphere.center.getX() - mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getY() + mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getZ() - mergeSphere.getRadius()));
        mergeBuf.put((float) (mergeSphere.center.getX() + mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getY() - mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getZ() - mergeSphere.getRadius()));
        mergeBuf.put((float) (mergeSphere.center.getX() - mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getY() - mergeSphere.getRadius())).put(
                (float) (mergeSphere.center.getZ() - mergeSphere.getRadius()));
        containAABB(mergeBuf);
        correctCorners = false;
        return this;
    }

    private BoundingVolume mergeAABB(final BoundingBox volume) {
        final BoundingBox mergeBox = volume;
        if (!correctCorners) {
            computeCorners();
        }

        final FloatBuffer mergeBuf = BufferUtils.createFloatBufferOnHeap(16 * 3);

        mergeBuf.rewind();
        for (int i = 0; i < 8; i++) {
            mergeBuf.put((float) vectorStore[i].getX());
            mergeBuf.put((float) vectorStore[i].getY());
            mergeBuf.put((float) vectorStore[i].getZ());
        }
        mergeBuf.put((float) (mergeBox.center.getX() + mergeBox.getXExtent())).put(
                (float) (mergeBox.center.getY() + mergeBox.getYExtent())).put(
                (float) (mergeBox.center.getZ() + mergeBox.getZExtent()));
        mergeBuf.put((float) (mergeBox.center.getX() - mergeBox.getXExtent())).put(
                (float) (mergeBox.center.getY() + mergeBox.getYExtent())).put(
                (float) (mergeBox.center.getZ() + mergeBox.getZExtent()));
        mergeBuf.put((float) (mergeBox.center.getX() + mergeBox.getXExtent())).put(
                (float) (mergeBox.center.getY() - mergeBox.getYExtent())).put(
                (float) (mergeBox.center.getZ() + mergeBox.getZExtent()));
        mergeBuf.put((float) (mergeBox.center.getX() + mergeBox.getXExtent())).put(
                (float) (mergeBox.center.getY() + mergeBox.getYExtent())).put(
                (float) (mergeBox.center.getZ() - mergeBox.getZExtent()));
        mergeBuf.put((float) (mergeBox.center.getX() - mergeBox.getXExtent())).put(
                (float) (mergeBox.center.getY() - mergeBox.getYExtent())).put(
                (float) (mergeBox.center.getZ() + mergeBox.getZExtent()));
        mergeBuf.put((float) (mergeBox.center.getX() - mergeBox.getXExtent())).put(
                (float) (mergeBox.center.getY() + mergeBox.getYExtent())).put(
                (float) (mergeBox.center.getZ() - mergeBox.getZExtent()));
        mergeBuf.put((float) (mergeBox.center.getX() + mergeBox.getXExtent())).put(
                (float) (mergeBox.center.getY() - mergeBox.getYExtent())).put(
                (float) (mergeBox.center.getZ() - mergeBox.getZExtent()));
        mergeBuf.put((float) (mergeBox.center.getX() - mergeBox.getXExtent())).put(
                (float) (mergeBox.center.getY() - mergeBox.getYExtent())).put(
                (float) (mergeBox.center.getZ() - mergeBox.getZExtent()));
        containAABB(mergeBuf);
        correctCorners = false;
        return this;
    }

    private BoundingVolume mergeOBB(final OrientedBoundingBox volume) {
        // OrientedBoundingBox mergeBox=(OrientedBoundingBox) volume;
        // if (!correctCorners) this.computeCorners();
        // if (!mergeBox.correctCorners) mergeBox.computeCorners();
        // Vector3[] mergeArray=new Vector3[16];
        // for (int i=0;i<vectorStore.length;i++){
        // mergeArray[i*2+0]=this .vectorStore[i];
        // mergeArray[i*2+1]=mergeBox.vectorStore[i];
        // }
        // containAABB(mergeArray);
        // correctCorners=false;
        // return this;
        // construct a box that contains the input boxes
        // Box3<Real> kBox;
        final OrientedBoundingBox rkBox0 = this;
        final OrientedBoundingBox rkBox1 = volume;

        // The first guess at the box center. This value will be updated later
        // after the input box vertices are projected onto axes determined by an
        // average of box axes.
        final Vector3 kBoxCenter = (rkBox0.center.add(rkBox1.center, Vector3.fetchTempInstance())).multiplyLocal(.5);

        // A box's axes, when viewed as the columns of a matrix, form a rotation
        // matrix. The input box axes are converted to quaternions. The average
        // quaternion is computed, then normalized to unit length. The result is
        // the slerp of the two input quaternions with t-value of 1/2. The
        // result is converted back to a rotation matrix and its columns are
        // selected as the merged box axes.
        final Quaternion kQ0 = Quaternion.fetchTempInstance(), kQ1 = Quaternion.fetchTempInstance();
        kQ0.fromAxes(rkBox0.xAxis, rkBox0.yAxis, rkBox0.zAxis);
        kQ1.fromAxes(rkBox1.xAxis, rkBox1.yAxis, rkBox1.zAxis);

        if (kQ0.dot(kQ1) < 0.0) {
            kQ1.multiplyLocal(-1.0);
        }

        final Quaternion kQ = kQ0.addLocal(kQ1);
        kQ.normalizeLocal();

        final Matrix3 kBoxaxis = kQ.toRotationMatrix(Matrix3.fetchTempInstance());
        final Vector3 newXaxis = kBoxaxis.getColumn(0, Vector3.fetchTempInstance());
        final Vector3 newYaxis = kBoxaxis.getColumn(1, Vector3.fetchTempInstance());
        final Vector3 newZaxis = kBoxaxis.getColumn(2, Vector3.fetchTempInstance());

        // Project the input box vertices onto the merged-box axes. Each axis
        // D[i] containing the current center C has a minimum projected value
        // pmin[i] and a maximum projected value pmax[i]. The corresponding end
        // points on the axes are C+pmin[i]*D[i] and C+pmax[i]*D[i]. The point C
        // is not necessarily the midpoint for any of the intervals. The actual
        // box center will be adjusted from C to a point C' that is the midpoint
        // of each interval,
        // C' = C + sum_{i=0}^1 0.5*(pmin[i]+pmax[i])*D[i]
        // The box extents are
        // e[i] = 0.5*(pmax[i]-pmin[i])

        int i;
        double fDot;
        final Vector3 kDiff = Vector3.fetchTempInstance();
        final Vector3 kMin = Vector3.fetchTempInstance();
        final Vector3 kMax = Vector3.fetchTempInstance();

        if (!rkBox0.correctCorners) {
            rkBox0.computeCorners();
        }
        for (i = 0; i < 8; i++) {
            rkBox0.vectorStore[i].subtract(kBoxCenter, kDiff);

            fDot = kDiff.dot(newXaxis);
            if (fDot > kMax.getX()) {
                kMax.setX(fDot);
            } else if (fDot < kMin.getX()) {
                kMin.setX(fDot);
            }

            fDot = kDiff.dot(newYaxis);
            if (fDot > kMax.getY()) {
                kMax.setY(fDot);
            } else if (fDot < kMin.getY()) {
                kMin.setY(fDot);
            }

            fDot = kDiff.dot(newZaxis);
            if (fDot > kMax.getZ()) {
                kMax.setZ(fDot);
            } else if (fDot < kMin.getZ()) {
                kMin.setZ(fDot);
            }

        }

        if (!rkBox1.correctCorners) {
            rkBox1.computeCorners();
        }
        for (i = 0; i < 8; i++) {
            rkBox1.vectorStore[i].subtract(kBoxCenter, kDiff);

            fDot = kDiff.dot(newXaxis);
            if (fDot > kMax.getX()) {
                kMax.setX(fDot);
            } else if (fDot < kMin.getX()) {
                kMin.setX(fDot);
            }

            fDot = kDiff.dot(newYaxis);
            if (fDot > kMax.getY()) {
                kMax.setY(fDot);
            } else if (fDot < kMin.getY()) {
                kMin.setY(fDot);
            }

            fDot = kDiff.dot(newZaxis);
            if (fDot > kMax.getZ()) {
                kMax.setZ(fDot);
            } else if (fDot < kMin.getZ()) {
                kMin.setZ(fDot);
            }
        }

        xAxis.set(newXaxis);
        yAxis.set(newYaxis);
        zAxis.set(newZaxis);

        final Vector3 tempVec = Vector3.fetchTempInstance();
        extent.setX(.5 * (kMax.getX() - kMin.getX()));
        kBoxCenter.addLocal(xAxis.multiply(.5 * (kMax.getX() + kMin.getX()), tempVec));

        extent.setY(.5 * (kMax.getY() - kMin.getY()));
        kBoxCenter.addLocal(yAxis.multiply(.5 * (kMax.getY() + kMin.getY()), tempVec));

        extent.setZ(.5 * (kMax.getZ() - kMin.getZ()));
        kBoxCenter.addLocal(zAxis.multiply(.5 * (kMax.getZ() + kMin.getZ()), tempVec));

        center.set(kBoxCenter);

        correctCorners = false;

        Quaternion.releaseTempInstance(kQ0);
        Quaternion.releaseTempInstance(kQ1);
        Matrix3.releaseTempInstance(kBoxaxis);
        Vector3.releaseTempInstance(kBoxCenter);
        Vector3.releaseTempInstance(newXaxis);
        Vector3.releaseTempInstance(newYaxis);
        Vector3.releaseTempInstance(newZaxis);
        Vector3.releaseTempInstance(kDiff);
        Vector3.releaseTempInstance(kMin);
        Vector3.releaseTempInstance(kMax);
        Vector3.releaseTempInstance(tempVec);

        return this;
    }

    @Override
    public BoundingVolume clone(final BoundingVolume store) {
        OrientedBoundingBox toReturn;
        if (store instanceof OrientedBoundingBox) {
            toReturn = (OrientedBoundingBox) store;
        } else {
            toReturn = new OrientedBoundingBox();
        }
        toReturn.extent.set(extent);
        toReturn.xAxis.set(xAxis);
        toReturn.yAxis.set(yAxis);
        toReturn.zAxis.set(zAxis);
        toReturn.center.set(center);
        toReturn.checkPlane = checkPlane;
        for (int x = vectorStore.length; --x >= 0;) {
            toReturn.vectorStore[x].set(vectorStore[x]);
        }
        toReturn.correctCorners = correctCorners;
        return toReturn;
    }

    /**
     * Sets the vectorStore information to the 8 corners of the box.
     */
    public void computeCorners() {
        final Vector3 tempAxis0 = xAxis.multiply(extent.getX(), Vector3.fetchTempInstance());
        final Vector3 tempAxis1 = yAxis.multiply(extent.getY(), Vector3.fetchTempInstance());
        final Vector3 tempAxis2 = zAxis.multiply(extent.getZ(), Vector3.fetchTempInstance());

        vectorStore[0].set(center).subtractLocal(tempAxis0).subtractLocal(tempAxis1).subtractLocal(tempAxis2);
        vectorStore[1].set(center).addLocal(tempAxis0).subtractLocal(tempAxis1).subtractLocal(tempAxis2);
        vectorStore[2].set(center).addLocal(tempAxis0).addLocal(tempAxis1).subtractLocal(tempAxis2);
        vectorStore[3].set(center).subtractLocal(tempAxis0).addLocal(tempAxis1).subtractLocal(tempAxis2);
        vectorStore[4].set(center).subtractLocal(tempAxis0).subtractLocal(tempAxis1).addLocal(tempAxis2);
        vectorStore[5].set(center).addLocal(tempAxis0).subtractLocal(tempAxis1).addLocal(tempAxis2);
        vectorStore[6].set(center).addLocal(tempAxis0).addLocal(tempAxis1).addLocal(tempAxis2);
        vectorStore[7].set(center).subtractLocal(tempAxis0).addLocal(tempAxis1).addLocal(tempAxis2);

        Vector3.releaseTempInstance(tempAxis0);
        Vector3.releaseTempInstance(tempAxis1);
        Vector3.releaseTempInstance(tempAxis2);
        correctCorners = true;
    }

    @Override
    public void computeFromTris(final int[] indices, final Mesh mesh, final int start, final int end) {
        if (end - start <= 0) {
            return;
        }
        final Vector3[] verts = new Vector3[3];
        final Vector3 min = Vector3.fetchTempInstance().set(
                new Vector3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        final Vector3 max = Vector3.fetchTempInstance().set(
                new Vector3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        Vector3 point;
        for (int i = start; i < end; i++) {
            PickingUtil.getTriangle(mesh, indices[i], verts);
            point = verts[0];
            if (point.getX() < min.getX()) {
                min.setX(point.getX());
            } else if (point.getX() > max.getX()) {
                max.setX(point.getX());
            }
            if (point.getY() < min.getY()) {
                min.setY(point.getY());
            } else if (point.getY() > max.getY()) {
                max.setY(point.getY());
            }
            if (point.getZ() < min.getZ()) {
                min.setZ(point.getZ());
            } else if (point.getZ() > max.getZ()) {
                max.setZ(point.getZ());
            }

            point = verts[1];
            if (point.getX() < min.getX()) {
                min.setX(point.getX());
            } else if (point.getX() > max.getX()) {
                max.setX(point.getX());
            }
            if (point.getY() < min.getY()) {
                min.setY(point.getY());
            } else if (point.getY() > max.getY()) {
                max.setY(point.getY());
            }
            if (point.getZ() < min.getZ()) {
                min.setZ(point.getZ());
            } else if (point.getZ() > max.getZ()) {
                max.setZ(point.getZ());
            }

            point = verts[2];
            if (point.getX() < min.getX()) {
                min.setX(point.getX());
            } else if (point.getX() > max.getX()) {
                max.setX(point.getX());
            }

            if (point.getY() < min.getY()) {
                min.setY(point.getY());
            } else if (point.getY() > max.getY()) {
                max.setY(point.getY());
            }

            if (point.getZ() < min.getZ()) {
                min.setZ(point.getZ());
            } else if (point.getZ() > max.getZ()) {
                max.setZ(point.getZ());
            }
        }

        center.set(min.addLocal(max));
        center.multiplyLocal(0.5);

        extent.set(max.getX() - center.getX(), max.getY() - center.getY(), max.getZ() - center.getZ());

        xAxis.set(1, 0, 0);
        yAxis.set(0, 1, 0);
        zAxis.set(0, 0, 1);

        Vector3.releaseTempInstance(min);
        Vector3.releaseTempInstance(max);

        correctCorners = false;
    }

    @Override
    public void computeFromTris(final ReadOnlyTriangle[] tris, final int start, final int end) {
        if (end - start <= 0) {
            return;
        }

        final Vector3 min = Vector3.fetchTempInstance().set(tris[start].getA());
        final Vector3 max = Vector3.fetchTempInstance().set(min);
        ReadOnlyVector3 point;
        for (int i = start; i < end; i++) {

            point = tris[i].getA();
            if (point.getX() < min.getX()) {
                min.setX(point.getX());
            } else if (point.getX() > max.getX()) {
                max.setX(point.getX());
            }
            if (point.getY() < min.getY()) {
                min.setY(point.getY());
            } else if (point.getY() > max.getY()) {
                max.setY(point.getY());
            }
            if (point.getZ() < min.getZ()) {
                min.setZ(point.getZ());
            } else if (point.getZ() > max.getZ()) {
                max.setZ(point.getZ());
            }

            point = tris[i].getB();
            if (point.getX() < min.getX()) {
                min.setX(point.getX());
            } else if (point.getX() > max.getX()) {
                max.setX(point.getX());
            }
            if (point.getY() < min.getY()) {
                min.setY(point.getY());
            } else if (point.getY() > max.getY()) {
                max.setY(point.getY());
            }
            if (point.getZ() < min.getZ()) {
                min.setZ(point.getZ());
            } else if (point.getZ() > max.getZ()) {
                max.setZ(point.getZ());
            }

            point = tris[i].getC();
            if (point.getX() < min.getX()) {
                min.setX(point.getX());
            } else if (point.getX() > max.getX()) {
                max.setX(point.getX());
            }
            if (point.getY() < min.getY()) {
                min.setY(point.getY());
            } else if (point.getY() > max.getY()) {
                max.setY(point.getY());
            }
            if (point.getZ() < min.getZ()) {
                min.setZ(point.getZ());
            } else if (point.getZ() > max.getZ()) {
                max.setZ(point.getZ());
            }
        }

        center.set(min.addLocal(max));
        center.multiplyLocal(0.5);

        extent.set(max.getX() - center.getX(), max.getY() - center.getY(), max.getZ() - center.getZ());

        xAxis.set(1, 0, 0);
        yAxis.set(0, 1, 0);
        zAxis.set(0, 0, 1);

        Vector3.releaseTempInstance(min);
        Vector3.releaseTempInstance(max);

        correctCorners = false;
    }

    public boolean intersection(final OrientedBoundingBox box1) {
        // Cutoff for cosine of angles between box axes. This is used to catch
        // the cases when at least one pair of axes are parallel. If this
        // happens,
        // there is no need to test for separation along the Cross(A[i],B[j])
        // directions.
        final OrientedBoundingBox box0 = this;
        final double cutoff = 0.999999;
        boolean parallelPairExists = false;
        int i;

        // convenience variables
        final Vector3 akA[] = new Vector3[] { box0.getXAxis(), box0.getYAxis(), box0.getZAxis() };
        final Vector3[] akB = new Vector3[] { box1.getXAxis(), box1.getYAxis(), box1.getZAxis() };
        final Vector3 afEA = box0.extent;
        final Vector3 afEB = box1.extent;

        // compute difference of box centers, D = C1-C0
        final Vector3 kD = box1.center.subtract(box0.center, Vector3.fetchTempInstance());

        final double[][] aafC = { new double[3], new double[3], new double[3] };

        final double[][] aafAbsC = { new double[3], new double[3], new double[3] };

        final double[] afAD = new double[3];
        double fR0, fR1, fR; // interval radii and distance between centers
        double fR01; // = R0 + R1

        try {
            // axis C0+t*A0
            for (i = 0; i < 3; i++) {
                aafC[0][i] = akA[0].dot(akB[i]);
                aafAbsC[0][i] = Math.abs(aafC[0][i]);
                if (aafAbsC[0][i] > cutoff) {
                    parallelPairExists = true;
                }
            }
            afAD[0] = akA[0].dot(kD);
            fR = Math.abs(afAD[0]);
            fR1 = afEB.getX() * aafAbsC[0][0] + afEB.getY() * aafAbsC[0][1] + afEB.getZ() * aafAbsC[0][2];
            fR01 = afEA.getX() + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A1
            for (i = 0; i < 3; i++) {
                aafC[1][i] = akA[1].dot(akB[i]);
                aafAbsC[1][i] = Math.abs(aafC[1][i]);
                if (aafAbsC[1][i] > cutoff) {
                    parallelPairExists = true;
                }
            }
            afAD[1] = akA[1].dot(kD);
            fR = Math.abs(afAD[1]);
            fR1 = afEB.getX() * aafAbsC[1][0] + afEB.getY() * aafAbsC[1][1] + afEB.getZ() * aafAbsC[1][2];
            fR01 = afEA.getY() + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A2
            for (i = 0; i < 3; i++) {
                aafC[2][i] = akA[2].dot(akB[i]);
                aafAbsC[2][i] = Math.abs(aafC[2][i]);
                if (aafAbsC[2][i] > cutoff) {
                    parallelPairExists = true;
                }
            }
            afAD[2] = akA[2].dot(kD);
            fR = Math.abs(afAD[2]);
            fR1 = afEB.getX() * aafAbsC[2][0] + afEB.getY() * aafAbsC[2][1] + afEB.getZ() * aafAbsC[2][2];
            fR01 = afEA.getZ() + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*B0
            fR = Math.abs(akB[0].dot(kD));
            fR0 = afEA.getX() * aafAbsC[0][0] + afEA.getY() * aafAbsC[1][0] + afEA.getZ() * aafAbsC[2][0];
            fR01 = fR0 + afEB.getX();
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*B1
            fR = Math.abs(akB[1].dot(kD));
            fR0 = afEA.getX() * aafAbsC[0][1] + afEA.getY() * aafAbsC[1][1] + afEA.getZ() * aafAbsC[2][1];
            fR01 = fR0 + afEB.getY();
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*B2
            fR = Math.abs(akB[2].dot(kD));
            fR0 = afEA.getX() * aafAbsC[0][2] + afEA.getY() * aafAbsC[1][2] + afEA.getZ() * aafAbsC[2][2];
            fR01 = fR0 + afEB.getZ();
            if (fR > fR01) {
                return false;
            }
        } finally {
            // Make sure we release the temp vars
            Vector3.releaseTempInstance(kD);
        }

        // At least one pair of box axes was parallel, so the separation is
        // effectively in 2D where checking the "edge" normals is sufficient for
        // the separation of the boxes.
        if (parallelPairExists) {
            return true;
        }

        // axis C0+t*A0xB0
        fR = Math.abs(afAD[2] * aafC[1][0] - afAD[1] * aafC[2][0]);
        fR0 = afEA.getY() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[1][0];
        fR1 = afEB.getY() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][1];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A0xB1
        fR = Math.abs(afAD[2] * aafC[1][1] - afAD[1] * aafC[2][1]);
        fR0 = afEA.getY() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[1][1];
        fR1 = afEB.getX() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A0xB2
        fR = Math.abs(afAD[2] * aafC[1][2] - afAD[1] * aafC[2][2]);
        fR0 = afEA.getY() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[1][2];
        fR1 = afEB.getX() * aafAbsC[0][1] + afEB.getY() * aafAbsC[0][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A1xB0
        fR = Math.abs(afAD[0] * aafC[2][0] - afAD[2] * aafC[0][0]);
        fR0 = afEA.getX() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[0][0];
        fR1 = afEB.getY() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][1];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A1xB1
        fR = Math.abs(afAD[0] * aafC[2][1] - afAD[2] * aafC[0][1]);
        fR0 = afEA.getX() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[0][1];
        fR1 = afEB.getX() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A1xB2
        fR = Math.abs(afAD[0] * aafC[2][2] - afAD[2] * aafC[0][2]);
        fR0 = afEA.getX() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[0][2];
        fR1 = afEB.getX() * aafAbsC[1][1] + afEB.getY() * aafAbsC[1][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A2xB0
        fR = Math.abs(afAD[1] * aafC[0][0] - afAD[0] * aafC[1][0]);
        fR0 = afEA.getX() * aafAbsC[1][0] + afEA.getY() * aafAbsC[0][0];
        fR1 = afEB.getY() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][1];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A2xB1
        fR = Math.abs(afAD[1] * aafC[0][1] - afAD[0] * aafC[1][1]);
        fR0 = afEA.getX() * aafAbsC[1][1] + afEA.getY() * aafAbsC[0][1];
        fR1 = afEB.getX() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A2xB2
        fR = Math.abs(afAD[1] * aafC[0][2] - afAD[0] * aafC[1][2]);
        fR0 = afEA.getX() * aafAbsC[1][2] + afEA.getY() * aafAbsC[0][2];
        fR1 = afEB.getX() * aafAbsC[2][1] + afEB.getY() * aafAbsC[2][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        return true;
    }

    @Override
    public boolean intersects(final BoundingVolume bv) {
        if (bv == null) {
            return false;
        }

        return bv.intersectsOrientedBoundingBox(this);
    }

    @Override
    public boolean intersectsSphere(final BoundingSphere bs) {
        if (!Vector3.isValid(center) || !Vector3.isValid(bs.center)) {
            return false;
        }

        final Vector3 compVect1 = Vector3.fetchTempInstance().set(bs.getCenter()).subtractLocal(center);
        final Matrix3 tempMa = Matrix3.fetchTempInstance().fromAxes(xAxis, yAxis, zAxis);

        tempMa.applyPost(compVect1, compVect1);

        boolean result = false;
        if (Math.abs(compVect1.getX()) < bs.getRadius() + extent.getX()
                && Math.abs(compVect1.getY()) < bs.getRadius() + extent.getY()
                && Math.abs(compVect1.getZ()) < bs.getRadius() + extent.getZ()) {
            result = true;
        }

        Vector3.releaseTempInstance(compVect1);
        Matrix3.releaseTempInstance(tempMa);
        return result;
    }

    @Override
    public boolean intersectsBoundingBox(final BoundingBox bb) {
        if (!Vector3.isValid(center) || !Vector3.isValid(bb.center)) {
            return false;
        }

        // Cutoff for cosine of angles between box axes. This is used to catch
        // the cases when at least one pair of axes are parallel. If this
        // happens,
        // there is no need to test for separation along the Cross(A[i],B[j])
        // directions.
        final double cutoff = 0.999999f;
        boolean parallelPairExists = false;
        int i;

        // convenience variables
        final Vector3 akA[] = new Vector3[] { xAxis, yAxis, zAxis };
        final Vector3[] akB = new Vector3[] { Vector3.fetchTempInstance(), Vector3.fetchTempInstance(),
                Vector3.fetchTempInstance() };
        final Vector3 afEA = extent;
        final Vector3 afEB = Vector3.fetchTempInstance().set(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());

        // compute difference of box centers, D = C1-C0
        final Vector3 kD = bb.getCenter().subtract(center, Vector3.fetchTempInstance());

        final double[][] aafC = { new double[3], new double[3], new double[3] };

        final double[][] aafAbsC = { new double[3], new double[3], new double[3] };

        final double[] afAD = new double[3];
        double fR0, fR1, fR; // interval radii and distance between centers
        double fR01; // = R0 + R1

        try {

            // axis C0+t*A0
            for (i = 0; i < 3; i++) {
                aafC[0][i] = akA[0].dot(akB[i]);
                aafAbsC[0][i] = Math.abs(aafC[0][i]);
                if (aafAbsC[0][i] > cutoff) {
                    parallelPairExists = true;
                }
            }
            afAD[0] = akA[0].dot(kD);
            fR = Math.abs(afAD[0]);
            fR1 = afEB.getX() * aafAbsC[0][0] + afEB.getY() * aafAbsC[0][1] + afEB.getZ() * aafAbsC[0][2];
            fR01 = afEA.getX() + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A1
            for (i = 0; i < 3; i++) {
                aafC[1][i] = akA[1].dot(akB[i]);
                aafAbsC[1][i] = Math.abs(aafC[1][i]);
                if (aafAbsC[1][i] > cutoff) {
                    parallelPairExists = true;
                }
            }
            afAD[1] = akA[1].dot(kD);
            fR = Math.abs(afAD[1]);
            fR1 = afEB.getX() * aafAbsC[1][0] + afEB.getY() * aafAbsC[1][1] + afEB.getZ() * aafAbsC[1][2];
            fR01 = afEA.getY() + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A2
            for (i = 0; i < 3; i++) {
                aafC[2][i] = akA[2].dot(akB[i]);
                aafAbsC[2][i] = Math.abs(aafC[2][i]);
                if (aafAbsC[2][i] > cutoff) {
                    parallelPairExists = true;
                }
            }
            afAD[2] = akA[2].dot(kD);
            fR = Math.abs(afAD[2]);
            fR1 = afEB.getX() * aafAbsC[2][0] + afEB.getY() * aafAbsC[2][1] + afEB.getZ() * aafAbsC[2][2];
            fR01 = afEA.getZ() + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*B0
            fR = Math.abs(akB[0].dot(kD));
            fR0 = afEA.getX() * aafAbsC[0][0] + afEA.getY() * aafAbsC[1][0] + afEA.getZ() * aafAbsC[2][0];
            fR01 = fR0 + afEB.getX();
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*B1
            fR = Math.abs(akB[1].dot(kD));
            fR0 = afEA.getX() * aafAbsC[0][1] + afEA.getY() * aafAbsC[1][1] + afEA.getZ() * aafAbsC[2][1];
            fR01 = fR0 + afEB.getY();
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*B2
            fR = Math.abs(akB[2].dot(kD));
            fR0 = afEA.getX() * aafAbsC[0][2] + afEA.getY() * aafAbsC[1][2] + afEA.getZ() * aafAbsC[2][2];
            fR01 = fR0 + afEB.getZ();
            if (fR > fR01) {
                return false;
            }

            // At least one pair of box axes was parallel, so the separation is
            // effectively in 2D where checking the "edge" normals is sufficient for
            // the separation of the boxes.
            if (parallelPairExists) {
                return true;
            }

            // axis C0+t*A0xB0
            fR = Math.abs(afAD[2] * aafC[1][0] - afAD[1] * aafC[2][0]);
            fR0 = afEA.getY() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[1][0];
            fR1 = afEB.getY() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][1];
            fR01 = fR0 + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A0xB1
            fR = Math.abs(afAD[2] * aafC[1][1] - afAD[1] * aafC[2][1]);
            fR0 = afEA.getY() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[1][1];
            fR1 = afEB.getX() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][0];
            fR01 = fR0 + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A0xB2
            fR = Math.abs(afAD[2] * aafC[1][2] - afAD[1] * aafC[2][2]);
            fR0 = afEA.getY() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[1][2];
            fR1 = afEB.getX() * aafAbsC[0][1] + afEB.getY() * aafAbsC[0][0];
            fR01 = fR0 + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A1xB0
            fR = Math.abs(afAD[0] * aafC[2][0] - afAD[2] * aafC[0][0]);
            fR0 = afEA.getX() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[0][0];
            fR1 = afEB.getY() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][1];
            fR01 = fR0 + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A1xB1
            fR = Math.abs(afAD[0] * aafC[2][1] - afAD[2] * aafC[0][1]);
            fR0 = afEA.getX() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[0][1];
            fR1 = afEB.getX() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][0];
            fR01 = fR0 + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A1xB2
            fR = Math.abs(afAD[0] * aafC[2][2] - afAD[2] * aafC[0][2]);
            fR0 = afEA.getX() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[0][2];
            fR1 = afEB.getX() * aafAbsC[1][1] + afEB.getY() * aafAbsC[1][0];
            fR01 = fR0 + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A2xB0
            fR = Math.abs(afAD[1] * aafC[0][0] - afAD[0] * aafC[1][0]);
            fR0 = afEA.getX() * aafAbsC[1][0] + afEA.getY() * aafAbsC[0][0];
            fR1 = afEB.getY() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][1];
            fR01 = fR0 + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A2xB1
            fR = Math.abs(afAD[1] * aafC[0][1] - afAD[0] * aafC[1][1]);
            fR0 = afEA.getX() * aafAbsC[1][1] + afEA.getY() * aafAbsC[0][1];
            fR1 = afEB.getX() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][0];
            fR01 = fR0 + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A2xB2
            fR = Math.abs(afAD[1] * aafC[0][2] - afAD[0] * aafC[1][2]);
            fR0 = afEA.getX() * aafAbsC[1][2] + afEA.getY() * aafAbsC[0][2];
            fR1 = afEB.getX() * aafAbsC[2][1] + afEB.getY() * aafAbsC[2][0];
            fR01 = fR0 + fR1;
            if (fR > fR01) {
                return false;
            }

            return true;
        } finally {
            // Make sure we release the temp vars
            Vector3.releaseTempInstance(kD);
            Vector3.releaseTempInstance(afEB);
            for (final Vector3 vec : akB) {
                Vector3.releaseTempInstance(vec);
            }

        }
    }

    @Override
    public boolean intersectsOrientedBoundingBox(final OrientedBoundingBox obb) {
        if (!Vector3.isValid(center) || !Vector3.isValid(obb.center)) {
            return false;
        }

        // Cutoff for cosine of angles between box axes. This is used to catch
        // the cases when at least one pair of axes are parallel. If this
        // happens,
        // there is no need to test for separation along the Cross(A[i],B[j])
        // directions.
        final double cutoff = 0.999999f;
        boolean parallelPairExists = false;
        int i;

        // convenience variables
        final Vector3 akA[] = new Vector3[] { xAxis, yAxis, zAxis };
        final Vector3[] akB = new Vector3[] { obb.xAxis, obb.yAxis, obb.zAxis };
        final Vector3 afEA = extent;
        final Vector3 afEB = obb.extent;

        // compute difference of box centers, D = C1-C0
        final Vector3 kD = obb.center.subtract(center, Vector3.fetchTempInstance());

        final double[][] aafC = { new double[3], new double[3], new double[3] };

        final double[][] aafAbsC = { new double[3], new double[3], new double[3] };

        final double[] afAD = new double[3];
        double fR0, fR1, fR; // interval radii and distance between centers
        double fR01; // = R0 + R1

        try {
            // axis C0+t*A0
            for (i = 0; i < 3; i++) {
                aafC[0][i] = akA[0].dot(akB[i]);
                aafAbsC[0][i] = Math.abs(aafC[0][i]);
                if (aafAbsC[0][i] > cutoff) {
                    parallelPairExists = true;
                }
            }
            afAD[0] = akA[0].dot(kD);
            fR = Math.abs(afAD[0]);
            fR1 = afEB.getX() * aafAbsC[0][0] + afEB.getY() * aafAbsC[0][1] + afEB.getZ() * aafAbsC[0][2];
            fR01 = afEA.getX() + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A1
            for (i = 0; i < 3; i++) {
                aafC[1][i] = akA[1].dot(akB[i]);
                aafAbsC[1][i] = Math.abs(aafC[1][i]);
                if (aafAbsC[1][i] > cutoff) {
                    parallelPairExists = true;
                }
            }
            afAD[1] = akA[1].dot(kD);
            fR = Math.abs(afAD[1]);
            fR1 = afEB.getX() * aafAbsC[1][0] + afEB.getY() * aafAbsC[1][1] + afEB.getZ() * aafAbsC[1][2];
            fR01 = afEA.getY() + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*A2
            for (i = 0; i < 3; i++) {
                aafC[2][i] = akA[2].dot(akB[i]);
                aafAbsC[2][i] = Math.abs(aafC[2][i]);
                if (aafAbsC[2][i] > cutoff) {
                    parallelPairExists = true;
                }
            }
            afAD[2] = akA[2].dot(kD);
            fR = Math.abs(afAD[2]);
            fR1 = afEB.getX() * aafAbsC[2][0] + afEB.getY() * aafAbsC[2][1] + afEB.getZ() * aafAbsC[2][2];
            fR01 = afEA.getZ() + fR1;
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*B0
            fR = Math.abs(akB[0].dot(kD));
            fR0 = afEA.getX() * aafAbsC[0][0] + afEA.getY() * aafAbsC[1][0] + afEA.getZ() * aafAbsC[2][0];
            fR01 = fR0 + afEB.getX();
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*B1
            fR = Math.abs(akB[1].dot(kD));
            fR0 = afEA.getX() * aafAbsC[0][1] + afEA.getY() * aafAbsC[1][1] + afEA.getZ() * aafAbsC[2][1];
            fR01 = fR0 + afEB.getY();
            if (fR > fR01) {
                return false;
            }

            // axis C0+t*B2
            fR = Math.abs(akB[2].dot(kD));
            fR0 = afEA.getX() * aafAbsC[0][2] + afEA.getY() * aafAbsC[1][2] + afEA.getZ() * aafAbsC[2][2];
            fR01 = fR0 + afEB.getZ();
            if (fR > fR01) {
                return false;
            }
        } finally {
            Vector3.releaseTempInstance(kD);
        }

        // At least one pair of box axes was parallel, so the separation is
        // effectively in 2D where checking the "edge" normals is sufficient for
        // the separation of the boxes.
        if (parallelPairExists) {
            return true;
        }

        // axis C0+t*A0xB0
        fR = Math.abs(afAD[2] * aafC[1][0] - afAD[1] * aafC[2][0]);
        fR0 = afEA.getY() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[1][0];
        fR1 = afEB.getY() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][1];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A0xB1
        fR = Math.abs(afAD[2] * aafC[1][1] - afAD[1] * aafC[2][1]);
        fR0 = afEA.getY() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[1][1];
        fR1 = afEB.getX() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A0xB2
        fR = Math.abs(afAD[2] * aafC[1][2] - afAD[1] * aafC[2][2]);
        fR0 = afEA.getY() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[1][2];
        fR1 = afEB.getX() * aafAbsC[0][1] + afEB.getY() * aafAbsC[0][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A1xB0
        fR = Math.abs(afAD[0] * aafC[2][0] - afAD[2] * aafC[0][0]);
        fR0 = afEA.getX() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[0][0];
        fR1 = afEB.getY() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][1];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A1xB1
        fR = Math.abs(afAD[0] * aafC[2][1] - afAD[2] * aafC[0][1]);
        fR0 = afEA.getX() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[0][1];
        fR1 = afEB.getX() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A1xB2
        fR = Math.abs(afAD[0] * aafC[2][2] - afAD[2] * aafC[0][2]);
        fR0 = afEA.getX() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[0][2];
        fR1 = afEB.getX() * aafAbsC[1][1] + afEB.getY() * aafAbsC[1][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A2xB0
        fR = Math.abs(afAD[1] * aafC[0][0] - afAD[0] * aafC[1][0]);
        fR0 = afEA.getX() * aafAbsC[1][0] + afEA.getY() * aafAbsC[0][0];
        fR1 = afEB.getY() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][1];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A2xB1
        fR = Math.abs(afAD[1] * aafC[0][1] - afAD[0] * aafC[1][1]);
        fR0 = afEA.getX() * aafAbsC[1][1] + afEA.getY() * aafAbsC[0][1];
        fR1 = afEB.getX() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        // axis C0+t*A2xB2
        fR = Math.abs(afAD[1] * aafC[0][2] - afAD[0] * aafC[1][2]);
        fR0 = afEA.getX() * aafAbsC[1][2] + afEA.getY() * aafAbsC[0][2];
        fR1 = afEB.getX() * aafAbsC[2][1] + afEB.getY() * aafAbsC[2][0];
        fR01 = fR0 + fR1;
        if (fR > fR01) {
            return false;
        }

        return true;
    }

    @Override
    public boolean intersects(final ReadOnlyRay3 ray) {
        if (!Vector3.isValid(center)) {
            return false;
        }

        double rhs;
        final ReadOnlyVector3 rayDir = ray.getDirection();
        final Vector3 diff = Vector3.fetchTempInstance().set(ray.getOrigin()).subtractLocal(center);
        final Vector3 wCrossD = Vector3.fetchTempInstance();

        final double[] fWdU = new double[3];
        final double[] fAWdU = new double[3];
        final double[] fDdU = new double[3];
        final double[] fADdU = new double[3];
        final double[] fAWxDdU = new double[3];

        try {
            fWdU[0] = rayDir.dot(xAxis);
            fAWdU[0] = Math.abs(fWdU[0]);
            fDdU[0] = diff.dot(xAxis);
            fADdU[0] = Math.abs(fDdU[0]);
            if (fADdU[0] > extent.getX() && fDdU[0] * fWdU[0] >= 0.0) {
                return false;
            }

            fWdU[1] = rayDir.dot(yAxis);
            fAWdU[1] = Math.abs(fWdU[1]);
            fDdU[1] = diff.dot(yAxis);
            fADdU[1] = Math.abs(fDdU[1]);
            if (fADdU[1] > extent.getY() && fDdU[1] * fWdU[1] >= 0.0) {
                return false;
            }

            fWdU[2] = rayDir.dot(zAxis);
            fAWdU[2] = Math.abs(fWdU[2]);
            fDdU[2] = diff.dot(zAxis);
            fADdU[2] = Math.abs(fDdU[2]);
            if (fADdU[2] > extent.getZ() && fDdU[2] * fWdU[2] >= 0.0) {
                return false;
            }

            rayDir.cross(diff, wCrossD);

            fAWxDdU[0] = Math.abs(wCrossD.dot(xAxis));
            rhs = extent.getY() * fAWdU[2] + extent.getZ() * fAWdU[1];
            if (fAWxDdU[0] > rhs) {
                return false;
            }

            fAWxDdU[1] = Math.abs(wCrossD.dot(yAxis));
            rhs = extent.getX() * fAWdU[2] + extent.getZ() * fAWdU[0];
            if (fAWxDdU[1] > rhs) {
                return false;
            }

            fAWxDdU[2] = Math.abs(wCrossD.dot(zAxis));
            rhs = extent.getX() * fAWdU[1] + extent.getY() * fAWdU[0];
            if (fAWxDdU[2] > rhs) {
                return false;

            }

            return true;
        } finally {
            Vector3.releaseTempInstance(diff);
            Vector3.releaseTempInstance(wCrossD);
        }
    }

    @Override
    public IntersectionRecord intersectsWhere(final ReadOnlyRay3 ray) {
        final ReadOnlyVector3 rayDir = ray.getDirection();
        final ReadOnlyVector3 rayOrigin = ray.getOrigin();

        final Vector3 diff = rayOrigin.subtract(getCenter(), Vector3.fetchTempInstance());
        // convert ray to box coordinates
        final ReadOnlyVector3 direction = rayDir;
        final double[] t = { 0, Double.POSITIVE_INFINITY };

        try {
            final double saveT0 = t[0], saveT1 = t[1];
            final boolean notEntirelyClipped = clip(+direction.getX(), -diff.getX() - extent.getX(), t)
                    && clip(-direction.getX(), +diff.getX() - extent.getX(), t)
                    && clip(+direction.getY(), -diff.getY() - extent.getY(), t)
                    && clip(-direction.getY(), +diff.getY() - extent.getY(), t)
                    && clip(+direction.getZ(), -diff.getZ() - extent.getZ(), t)
                    && clip(-direction.getZ(), +diff.getZ() - extent.getZ(), t);

            if (notEntirelyClipped && (t[0] != saveT0 || t[1] != saveT1)) {
                if (t[1] > t[0]) {
                    final double[] distances = t;
                    final Vector3[] points = new Vector3[] {
                            rayDir.multiply(distances[0], new Vector3()).addLocal(rayOrigin),
                            rayDir.multiply(distances[1], new Vector3()).addLocal(rayOrigin) };
                    final IntersectionRecord record = new IntersectionRecord(distances, points);
                    return record;
                }

                final double[] distances = new double[] { t[0] };
                final Vector3[] points = new Vector3[] { rayDir.multiply(distances[0], new Vector3()).addLocal(
                        rayOrigin) };
                final IntersectionRecord record = new IntersectionRecord(distances, points);
                return record;
            }

            return new IntersectionRecord();
        } finally {
            Vector3.releaseTempInstance(diff);
        }

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

    public void setXAxis(final Vector3 axis) {
        xAxis.set(axis);
        correctCorners = false;
    }

    public void setYAxis(final Vector3 axis) {
        yAxis.set(axis);
        correctCorners = false;
    }

    public void setZAxis(final Vector3 axis) {
        zAxis.set(axis);
        correctCorners = false;
    }

    public void setExtent(final Vector3 ext) {
        extent.set(ext);
        correctCorners = false;
    }

    public Vector3 getXAxis() {
        return xAxis;
    }

    public Vector3 getYAxis() {
        return yAxis;
    }

    public Vector3 getZAxis() {
        return zAxis;
    }

    public Vector3 getExtent() {
        return extent;
    }

    @Override
    public boolean contains(final ReadOnlyVector3 point) {
        final Vector3 compVect1 = Vector3.fetchTempInstance().set(point).subtractLocal(center);
        double coeff = compVect1.dot(xAxis);
        if (Math.abs(coeff) > extent.getX()) {
            return false;
        }

        coeff = compVect1.dot(yAxis);
        if (Math.abs(coeff) > extent.getY()) {
            return false;
        }

        coeff = compVect1.dot(zAxis);
        if (Math.abs(coeff) > extent.getZ()) {
            return false;
        }

        Vector3.releaseTempInstance(compVect1);
        return true;
    }

    @Override
    public double distanceToEdge(final ReadOnlyVector3 point) {
        // compute coordinates of point in box coordinate system
        final Vector3 diff = point.subtract(center, Vector3.fetchTempInstance());
        final Vector3 closest = Vector3.fetchTempInstance().set(diff.dot(xAxis), diff.dot(yAxis), diff.dot(zAxis));
        Vector3.releaseTempInstance(diff);

        // project test point onto box
        double sqrDistance = 0.0;
        double delta;

        if (closest.getX() < -extent.getX()) {
            delta = closest.getX() + extent.getX();
            sqrDistance += delta * delta;
            closest.setX(-extent.getX());
        } else if (closest.getX() > extent.getX()) {
            delta = closest.getX() - extent.getX();
            sqrDistance += delta * delta;
            closest.setX(extent.getX());
        }

        if (closest.getY() < -extent.getY()) {
            delta = closest.getY() + extent.getY();
            sqrDistance += delta * delta;
            closest.setY(-extent.getY());
        } else if (closest.getY() > extent.getY()) {
            delta = closest.getY() - extent.getY();
            sqrDistance += delta * delta;
            closest.setY(extent.getY());
        }

        if (closest.getZ() < -extent.getZ()) {
            delta = closest.getZ() + extent.getZ();
            sqrDistance += delta * delta;
            closest.setZ(-extent.getZ());
        } else if (closest.getZ() > extent.getZ()) {
            delta = closest.getZ() - extent.getZ();
            sqrDistance += delta * delta;
            closest.setZ(extent.getZ());
        }

        Vector3.releaseTempInstance(closest);
        return Math.sqrt(sqrDistance);
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(xAxis, "xAxis", new Vector3(Vector3.UNIT_X));
        capsule.write(yAxis, "yAxis", new Vector3(Vector3.UNIT_Y));
        capsule.write(zAxis, "zAxis", new Vector3(Vector3.UNIT_Z));
        capsule.write(extent, "extent", new Vector3(Vector3.ZERO));
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        xAxis.set((Vector3) capsule.readSavable("xAxis", new Vector3(Vector3.UNIT_X)));
        yAxis.set((Vector3) capsule.readSavable("yAxis", new Vector3(Vector3.UNIT_Y)));
        zAxis.set((Vector3) capsule.readSavable("zAxis", new Vector3(Vector3.UNIT_Z)));
        extent.set((Vector3) capsule.readSavable("extent", new Vector3(Vector3.ZERO)));
        correctCorners = false;
    }

    @Override
    public double getVolume() {
        return (8 * extent.getX() * extent.getY() * extent.getZ());
    }
}