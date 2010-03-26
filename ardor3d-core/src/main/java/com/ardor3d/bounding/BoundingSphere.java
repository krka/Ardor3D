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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyPlane;
import com.ardor3d.math.type.ReadOnlyRay3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.type.ReadOnlyPlane.Side;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>BoundingSphere</code> defines a sphere that defines a container for a group of vertices of a particular piece
 * of geometry. This sphere defines a radius and a center. <br>
 * <br>
 * A typical usage is to allow the class define the center and radius by calling either <code>containAABB</code> or
 * <code>averagePoints</code>. A call to <code>computeFramePoint</code> in turn calls <code>containAABB</code>.
 */
public class BoundingSphere extends BoundingVolume {
    private static final Logger logger = Logger.getLogger(BoundingSphere.class.getName());

    private static final long serialVersionUID = 1L;

    private double _radius;

    static final private double radiusEpsilon = 1 + 0.00001;

    /**
     * Default constructor instantiates a new <code>BoundingSphere</code> object.
     */
    public BoundingSphere() {}

    /**
     * Constructor instantiates a new <code>BoundingSphere</code> object.
     * 
     * @param r
     *            the radius of the sphere.
     * @param c
     *            the center of the sphere.
     */
    public BoundingSphere(final double r, final ReadOnlyVector3 c) {
        _center.set(c);
        setRadius(r);
    }

    @Override
    public Type getType() {
        return Type.Sphere;
    }

    @Override
    public BoundingVolume transform(final ReadOnlyTransform transform, final BoundingVolume store) {
        BoundingSphere sphere;
        if (store == null || store.getType() != BoundingVolume.Type.Sphere) {
            sphere = new BoundingSphere(1, new Vector3(0, 0, 0));
        } else {
            sphere = (BoundingSphere) store;
        }

        transform.applyForward(_center, sphere._center);

        if (!transform.isRotationMatrix()) {
            final Vector3 scale = new Vector3(1, 1, 1);
            transform.applyForwardVector(scale);
            sphere.setRadius(Math.abs(maxAxis(scale) * getRadius()) + radiusEpsilon - 1);
        } else {
            final ReadOnlyVector3 scale = transform.getScale();
            sphere.setRadius(Math.abs(maxAxis(scale) * getRadius()) + radiusEpsilon - 1);
        }

        return sphere;
    }

    private double maxAxis(final ReadOnlyVector3 scale) {
        return Math.max(Math.abs(scale.getX()), Math.max(Math.abs(scale.getY()), Math.abs(scale.getZ())));
    }

    /**
     * <code>getRadius</code> returns the radius of the bounding sphere.
     * 
     * @return the radius of the bounding sphere.
     */
    public double getRadius() {
        return _radius;
    }

    /**
     * <code>setRadius</code> sets the radius of this bounding sphere.
     * 
     * @param radius
     *            the new radius of the bounding sphere.
     */
    public void setRadius(final double radius) {
        _radius = radius;
    }

    /**
     * <code>computeFromPoints</code> creates a new Bounding Sphere from a given set of points. It uses the
     * <code>calcWelzl</code> method as default.
     * 
     * @param points
     *            the points to contain.
     */
    @Override
    public void computeFromPoints(final FloatBuffer points) {
        calcWelzl(points);
    }

    @Override
    public void computeFromPrimitives(final MeshData data, final int section, final int[] indices, final int start,
            final int end) {
        if (end - start <= 0) {
            return;
        }

        final int vertsPerPrimitive = data.getIndexMode(section).getVertexCount();
        final Vector3[] vertList = new Vector3[(end - start) * vertsPerPrimitive];
        Vector3[] store = new Vector3[vertsPerPrimitive];

        int count = 0;
        for (int i = start; i < end; i++) {
            store = data.getPrimitive(indices[i], section, store);
            for (int j = 0; j < vertsPerPrimitive; j++) {
                vertList[count++] = Vector3.fetchTempInstance().set(store[0]);
            }
        }

        averagePoints(vertList);
        for (int i = 0; i < vertList.length; i++) {
            Vector3.releaseTempInstance(vertList[i]);
        }
    }

    /**
     * Calculates a minimum bounding sphere for the set of points. The algorithm was originally found at
     * http://www.flipcode.com/cgi-bin/msg.cgi?showThread=COTD-SmallestEnclosingSpheres&forum=cotd&id=-1 in C++ and
     * translated to java by Cep21
     * 
     * @param points
     *            The points to calculate the minimum bounds from.
     */
    public void calcWelzl(final FloatBuffer points) {
        final float[] buf = new float[points.limit()];
        points.rewind();
        points.get(buf);
        recurseMini(buf, buf.length / 3, 0, 0);
    }

    /**
     * Used from calcWelzl. This function recurses to calculate a minimum bounding sphere a few points at a time.
     * 
     * @param points
     *            The array of points to look through.
     * @param p
     *            The size of the list to be used.
     * @param pnts
     *            The number of points currently considering to include with the sphere.
     * @param ap
     *            A variable simulating pointer arithmetic from C++, and offset in <code>points</code>.
     */
    private void recurseMini(final float[] points, final int p, final int pnts, final int ap) {
        final Vector3 tempA = Vector3.fetchTempInstance();
        final Vector3 tempB = Vector3.fetchTempInstance();
        final Vector3 tempC = Vector3.fetchTempInstance();
        switch (pnts) {
            case 0:
                setRadius(0);
                _center.set(0, 0, 0);
                break;
            case 1:
                setRadius(1f - radiusEpsilon);
                populateFromBuffer(_center, points, ap - 1);
                break;
            case 2:
                populateFromBuffer(tempA, points, ap - 1);
                populateFromBuffer(tempB, points, ap - 2);
                setSphere(tempA, tempB);
                break;
            case 3:
                populateFromBuffer(tempA, points, ap - 1);
                populateFromBuffer(tempB, points, ap - 2);
                populateFromBuffer(tempC, points, ap - 3);
                setSphere(tempA, tempB, tempC);
                break;
            case 4:
                final Vector3 tempD = Vector3.fetchTempInstance();
                populateFromBuffer(tempA, points, ap - 1);
                populateFromBuffer(tempB, points, ap - 2);
                populateFromBuffer(tempC, points, ap - 3);
                populateFromBuffer(tempD, points, ap - 4);
                setSphere(tempA, tempB, tempC, tempD);
                Vector3.releaseTempInstance(tempA);
                Vector3.releaseTempInstance(tempB);
                Vector3.releaseTempInstance(tempC);
                Vector3.releaseTempInstance(tempD);
                return;
        }
        for (int i = 0; i < p; i++) {
            populateFromBuffer(tempA, points, i + ap);
            if (tempA.distanceSquared(_center) - (getRadius() * getRadius()) > radiusEpsilon - 1f) {
                for (int j = i; j > 0; j--) {
                    populateFromBuffer(tempB, points, j + ap);
                    populateFromBuffer(tempC, points, j - 1 + ap);
                    setInBuffer(tempC, points, j + ap);
                    setInBuffer(tempB, points, j - 1 + ap);
                }
                recurseMini(points, i, pnts + 1, ap + 1);
            }
        }
        Vector3.releaseTempInstance(tempA);
        Vector3.releaseTempInstance(tempB);
        Vector3.releaseTempInstance(tempC);
    }

    public static void populateFromBuffer(final Vector3 vector, final float[] buf, final int index) {
        vector.setX(buf[index * 3]);
        vector.setY(buf[index * 3 + 1]);
        vector.setZ(buf[index * 3 + 2]);
    }

    public static void setInBuffer(final ReadOnlyVector3 vector, final float[] buf, final int index) {
        if (buf == null) {
            return;
        }
        if (vector == null) {
            buf[index * 3] = 0;
            buf[(index * 3) + 1] = 0;
            buf[(index * 3) + 2] = 0;
        } else {
            buf[index * 3] = vector.getXf();
            buf[(index * 3) + 1] = vector.getYf();
            buf[(index * 3) + 2] = vector.getZf();
        }
    }

    /**
     * Calculates the minimum bounding sphere of 4 points. Used in welzl's algorithm.
     * 
     * @param O
     *            The 1st point inside the sphere.
     * @param A
     *            The 2nd point inside the sphere.
     * @param B
     *            The 3rd point inside the sphere.
     * @param C
     *            The 4th point inside the sphere.
     * @see #calcWelzl(java.nio.FloatBuffer)
     */
    private void setSphere(final Vector3 O, final Vector3 A, final Vector3 B, final Vector3 C) {
        final Vector3 a = A.subtract(O, null);
        final Vector3 b = B.subtract(O, null);
        final Vector3 c = C.subtract(O, null);

        final double Denominator = 2.0 * (a.getX() * (b.getY() * c.getZ() - c.getY() * b.getZ()) - b.getX()
                * (a.getY() * c.getZ() - c.getY() * a.getZ()) + c.getX() * (a.getY() * b.getZ() - b.getY() * a.getZ()));
        if (Denominator == 0) {
            _center.set(0, 0, 0);
            setRadius(0);
        } else {
            final Vector3 o = a.cross(b, null).multiplyLocal(c.lengthSquared()).addLocal(
                    c.cross(a, null).multiplyLocal(b.lengthSquared())).addLocal(
                    b.cross(c, null).multiplyLocal(a.lengthSquared())).divideLocal(Denominator);

            setRadius(o.length() * radiusEpsilon);
            O.add(o, _center);
        }
    }

    /**
     * Calculates the minimum bounding sphere of 3 points. Used in welzl's algorithm.
     * 
     * @param O
     *            The 1st point inside the sphere.
     * @param A
     *            The 2nd point inside the sphere.
     * @param B
     *            The 3rd point inside the sphere.
     * @see #calcWelzl(java.nio.FloatBuffer)
     */
    private void setSphere(final Vector3 O, final Vector3 A, final Vector3 B) {
        final Vector3 a = A.subtract(O, null);
        final Vector3 b = B.subtract(O, null);
        final Vector3 acrossB = a.cross(b, null);

        final double Denominator = 2.0 * acrossB.dot(acrossB);

        if (Denominator == 0) {
            _center.set(0, 0, 0);
            setRadius(0);
        } else {

            final Vector3 o = acrossB.cross(a, null).multiplyLocal(b.lengthSquared()).addLocal(
                    b.cross(acrossB, null).multiplyLocal(a.lengthSquared())).divideLocal(Denominator);
            setRadius(o.length() * radiusEpsilon);
            O.add(o, _center);
        }
    }

    /**
     * Calculates the minimum bounding sphere of 2 points. Used in welzl's algorithm.
     * 
     * @param O
     *            The 1st point inside the sphere.
     * @param A
     *            The 2nd point inside the sphere.
     * @see #calcWelzl(java.nio.FloatBuffer)
     */
    private void setSphere(final Vector3 O, final Vector3 A) {
        setRadius(Math.sqrt(((A.getX() - O.getX()) * (A.getX() - O.getX()) + (A.getY() - O.getY())
                * (A.getY() - O.getY()) + (A.getZ() - O.getZ()) * (A.getZ() - O.getZ())) / 4f)
                + radiusEpsilon - 1);
        Vector3.lerp(O, A, .5, _center);
    }

    /**
     * <code>averagePoints</code> selects the sphere center to be the average of the points and the sphere radius to be
     * the smallest value to enclose all points.
     * 
     * @param points
     *            the list of points to contain.
     */
    public void averagePoints(final Vector3[] points) {
        _center.set(points[0]);

        for (int i = 1; i < points.length; i++) {
            _center.addLocal(points[i]);
        }

        final double quantity = 1.0 / points.length;
        _center.multiplyLocal(quantity);

        double maxRadiusSqr = 0;
        final Vector3 temp = Vector3.fetchTempInstance();
        for (int i = 0; i < points.length; i++) {
            final Vector3 diff = points[i].subtract(_center, temp);
            final double radiusSqr = diff.lengthSquared();
            if (radiusSqr > maxRadiusSqr) {
                maxRadiusSqr = radiusSqr;
            }
        }
        Vector3.releaseTempInstance(temp);

        setRadius(Math.sqrt(maxRadiusSqr) + radiusEpsilon - 1f);

    }

    /**
     * <code>whichSide</code> takes a plane (typically provided by a view frustum) to determine which side this bound is
     * on.
     * 
     * @param plane
     *            the plane to check against.
     * @return side
     */
    @Override
    public Side whichSide(final ReadOnlyPlane plane) {
        final double distance = plane.pseudoDistance(_center);

        if (distance <= -getRadius()) {
            return Plane.Side.Inside;
        } else if (distance >= getRadius()) {
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
     * @return a new sphere
     */
    @Override
    public BoundingVolume merge(final BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {

            case Sphere: {
                final BoundingSphere sphere = (BoundingSphere) volume;
                final double temp_radius = sphere.getRadius();
                final ReadOnlyVector3 tempCenter = sphere.getCenter();
                final BoundingSphere rVal = new BoundingSphere();
                return merge(temp_radius, tempCenter, rVal);
            }

            case AABB: {
                final BoundingBox box = (BoundingBox) volume;
                final Vector3 radVect = new Vector3(box.getXExtent(), box.getYExtent(), box.getZExtent());
                final Vector3 tempCenter = box._center;
                final BoundingSphere rVal = new BoundingSphere();
                return merge(radVect.length(), tempCenter, rVal);
            }

            case OBB: {
                final OrientedBoundingBox box = (OrientedBoundingBox) volume;
                final BoundingSphere rVal = (BoundingSphere) this.clone(null);
                return rVal.mergeLocalOBB(box);
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

            case Sphere: {
                final BoundingSphere sphere = (BoundingSphere) volume;
                final double temp_radius = sphere.getRadius();
                final ReadOnlyVector3 temp_center = sphere.getCenter();
                return merge(temp_radius, temp_center, this);
            }

            case AABB: {
                final BoundingBox box = (BoundingBox) volume;
                final Vector3 radVect = Vector3.fetchTempInstance();
                radVect.set(box.getXExtent(), box.getYExtent(), box.getZExtent());
                final Vector3 temp_center = box._center;
                final double radius = radVect.length();
                Vector3.releaseTempInstance(radVect);
                return merge(radius, temp_center, this);
            }

            case OBB: {
                return mergeLocalOBB((OrientedBoundingBox) volume);
            }

            default:
                return null;
        }
    }

    /**
     * Merges this sphere with the given OBB.
     * 
     * @param volume
     *            The OBB to merge.
     * @return This sphere, after merging.
     */
    private BoundingSphere mergeLocalOBB(final OrientedBoundingBox volume) {
        // compute edge points from the obb
        if (!volume.correctCorners) {
            volume.computeCorners();
        }

        final FloatBuffer mergeBuf = BufferUtils.createFloatBufferOnHeap(8 * 3);

        for (int i = 0; i < 8; i++) {
            mergeBuf.put((float) volume._vectorStore[i].getX());
            mergeBuf.put((float) volume._vectorStore[i].getY());
            mergeBuf.put((float) volume._vectorStore[i].getZ());
        }

        // remember old radius and center
        final double oldRadius = getRadius();
        final Vector3 oldCenter = Vector3.fetchTempInstance().set(_center);

        // compute new radius and center from obb points
        computeFromPoints(mergeBuf);
        final Vector3 newCenter = Vector3.fetchTempInstance().set(_center);
        final double newRadius = getRadius();

        // restore old center and radius
        _center.set(oldCenter);
        setRadius(oldRadius);

        // merge obb points result
        merge(newRadius, newCenter, this);

        Vector3.releaseTempInstance(oldCenter);
        Vector3.releaseTempInstance(newCenter);

        return this;
    }

    private BoundingVolume merge(final double otherRadius, final ReadOnlyVector3 otherCenter, final BoundingSphere store) {
        final Vector3 diff = otherCenter.subtract(_center, Vector3.fetchTempInstance());
        final double lengthSquared = diff.lengthSquared();
        final double radiusDiff = otherRadius - getRadius();
        final double radiusDiffSqr = radiusDiff * radiusDiff;

        // if one sphere wholly contains the other
        if (radiusDiffSqr >= lengthSquared) {
            Vector3.releaseTempInstance(diff);
            // if we contain the other
            if (radiusDiff <= 0.0) {
                store.setCenter(_center);
                store.setRadius(_radius);
                return store;
            }
            // else the other contains us
            else {
                store.setCenter(otherCenter);
                store.setRadius(otherRadius);
                return store;
            }
        }

        // distance between sphere centers
        final double length = Math.sqrt(lengthSquared);

        // init a center var using our center
        final Vector3 rCenter = Vector3.fetchTempInstance();
        rCenter.set(_center);

        // if our centers are at least a tiny amount apart from each other...
        if (length > MathUtils.EPSILON) {
            // place us between the two centers, weighted by radii
            final double coeff = (length + radiusDiff) / (2.0 * length);
            rCenter.addLocal(diff.multiplyLocal(coeff));
        }
        Vector3.releaseTempInstance(diff);

        // set center on our resulting bounds
        store.setCenter(rCenter);
        Vector3.releaseTempInstance(rCenter);

        // Set radius
        store.setRadius(0.5 * (length + getRadius() + otherRadius));
        return store;
    }

    /**
     * <code>clone</code> creates a new BoundingSphere object containing the same data as this one.
     * 
     * @param store
     *            where to store the cloned information. if null or wrong class, a new store is created.
     * @return the new BoundingSphere
     */
    @Override
    public BoundingVolume clone(final BoundingVolume store) {
        if (store != null && store.getType() == Type.Sphere) {
            final BoundingSphere rVal = (BoundingSphere) store;
            rVal._center.set(_center);
            rVal.setRadius(_radius);
            rVal._checkPlane = _checkPlane;
            return rVal;
        }

        return new BoundingSphere(getRadius(), _center);
    }

    @Override
    public String toString() {
        return "com.ardor3d.scene.BoundingSphere [Radius: " + getRadius() + " Center: " + _center + "]";
    }

    @Override
    public boolean intersects(final BoundingVolume bv) {
        if (bv == null) {
            return false;
        }

        return bv.intersectsSphere(this);
    }

    @Override
    public boolean intersectsSphere(final BoundingSphere bs) {
        if (!Vector3.isValid(_center) || !Vector3.isValid(bs._center)) {
            return false;
        }

        final Vector3 diff = Vector3.fetchTempInstance().set(getCenter()).subtractLocal(bs.getCenter());
        final double rsum = getRadius() + bs.getRadius();
        final boolean intersect = (diff.dot(diff) <= rsum * rsum);
        Vector3.releaseTempInstance(diff);
        return intersect;
    }

    @Override
    public boolean intersectsBoundingBox(final BoundingBox bb) {
        if (!Vector3.isValid(_center) || !Vector3.isValid(bb._center)) {
            return false;
        }

        if (Math.abs(bb._center.getX() - getCenter().getX()) < getRadius() + bb.getXExtent()
                && Math.abs(bb._center.getY() - getCenter().getY()) < getRadius() + bb.getYExtent()
                && Math.abs(bb._center.getZ() - getCenter().getZ()) < getRadius() + bb.getZExtent()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean intersectsOrientedBoundingBox(final OrientedBoundingBox obb) {
        return obb.intersectsSphere(this);
    }

    @Override
    public boolean intersects(final ReadOnlyRay3 ray) {
        if (!Vector3.isValid(_center)) {
            return false;
        }

        final Vector3 diff = ray.getOrigin().subtract(getCenter(), Vector3.fetchTempInstance());
        final double radiusSquared = getRadius() * getRadius();
        final double a = diff.dot(diff) - radiusSquared;
        if (a <= 0.0) {
            // in sphere
            Vector3.releaseTempInstance(diff);
            return true;
        }

        // outside sphere
        final Vector3 dir = Vector3.fetchTempInstance().set(ray.getDirection());
        final double b = dir.dot(diff);
        Vector3.releaseTempInstance(dir);
        Vector3.releaseTempInstance(diff);
        if (b >= 0.0) {
            return false;
        }
        return b * b >= a;
    }

    @Override
    public IntersectionRecord intersectsWhere(final ReadOnlyRay3 ray) {

        final Vector3 diff = ray.getOrigin().subtract(getCenter(), Vector3.fetchTempInstance());
        final double a = diff.dot(diff) - (getRadius() * getRadius());
        double a1, discr, root;
        if (a <= 0.0) {
            // inside sphere
            a1 = ray.getDirection().dot(diff);
            discr = (a1 * a1) - a;
            root = Math.sqrt(discr);
            final double[] distances = new double[] { root - a1 };
            final Vector3[] points = new Vector3[] { ray.getDirection().multiply(distances[0], new Vector3()).addLocal(
                    ray.getOrigin()) };
            final IntersectionRecord record = new IntersectionRecord(distances, points);
            Vector3.releaseTempInstance(diff);
            return record;
        }

        a1 = ray.getDirection().dot(diff);
        Vector3.releaseTempInstance(diff);
        if (a1 >= 0.0) {
            return new IntersectionRecord();
        }

        discr = a1 * a1 - a;
        if (discr < 0.0) {
            return new IntersectionRecord();
        } else if (discr >= MathUtils.ZERO_TOLERANCE) {
            root = Math.sqrt(discr);
            final double[] distances = new double[] { -a1 - root, -a1 + root };
            final Vector3[] points = new Vector3[] {
                    ray.getDirection().multiply(distances[0], new Vector3()).addLocal(ray.getOrigin()),
                    ray.getDirection().multiply(distances[1], new Vector3()).addLocal(ray.getOrigin()) };
            final IntersectionRecord record = new IntersectionRecord(distances, points);
            return record;
        }

        final double[] distances = new double[] { -a1 };
        final Vector3[] points = new Vector3[] { ray.getDirection().multiply(distances[0], new Vector3()).addLocal(
                ray.getOrigin()) };
        final IntersectionRecord record = new IntersectionRecord(distances, points);
        return record;
    }

    @Override
    public boolean contains(final ReadOnlyVector3 point) {
        return getCenter().distanceSquared(point) < (getRadius() * getRadius());
    }

    @Override
    public double distanceToEdge(final ReadOnlyVector3 point) {
        return _center.distance(point) - getRadius();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        try {
            capsule.write(getRadius(), "radius", 0);
        } catch (final IOException ex) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "write(Ardor3DExporter)", "Exception", ex);
        }
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        try {
            setRadius(capsule.readDouble("radius", 0));
        } catch (final IOException ex) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "read(Ardor3DImporter)", "Exception", ex);
        }
    }

    @Override
    public double getVolume() {
        return 4 * MathUtils.ONE_THIRD * MathUtils.PI * getRadius() * getRadius() * getRadius();
    }
}