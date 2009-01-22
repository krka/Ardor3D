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
import java.io.Serializable;
import java.nio.FloatBuffer;

import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyPlane;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyRay3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyTriangle;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.Savable;

public abstract class BoundingVolume implements Serializable, Savable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        Sphere, AABB, OBB;
    }

    protected int checkPlane = 0;

    protected final Vector3 center = new Vector3();

    public BoundingVolume() {}

    public BoundingVolume(final Vector3 center) {
        this.center.set(center);
    }

    /**
     * Grabs the checkplane we should check first.
     * 
     */
    public int getCheckPlane() {
        return checkPlane;
    }

    /**
     * Sets the index of the plane that should be first checked during rendering.
     * 
     * @param value
     */
    public final void setCheckPlane(final int value) {
        checkPlane = value;
    }

    /**
     * getType returns the type of bounding volume this is.
     */
    public abstract Type getType();

    /**
     * 
     * <code>transform</code> alters the location of the bounding volume by a rotation, translation and a scalar.
     * 
     * @param rotate
     *            the rotation to affect the bound.
     * @param translate
     *            the translation to affect the bound.
     * @param scale
     *            the scale to resize the bound.
     * @return the new bounding volume.
     */
    public final BoundingVolume transform(final ReadOnlyQuaternion rotate, final ReadOnlyVector3 translate,
            final ReadOnlyVector3 scale) {
        return transform(rotate, translate, scale, null);
    }

    /**
     * 
     * <code>transform</code> alters the location of the bounding volume by a rotation, translation and a scalar.
     * 
     * @param rotate
     *            the rotation to affect the bound.
     * @param translate
     *            the translation to affect the bound.
     * @param scale
     *            the scale to resize the bound.
     * @param store
     *            sphere to store result in
     * @return the new bounding volume.
     */
    public abstract BoundingVolume transform(ReadOnlyQuaternion rotate, ReadOnlyVector3 translate,
            ReadOnlyVector3 scale, BoundingVolume store);

    /**
     * 
     * <code>transform</code> alters the location of the bounding volume by a rotation, translation and a scalar.
     * 
     * @param rotate
     *            the rotation to affect the bound.
     * @param translate
     *            the translation to affect the bound.
     * @param scale
     *            the scale to resize the bound.
     * @param store
     *            sphere to store result in
     * @return the new bounding volume.
     */
    public abstract BoundingVolume transform(ReadOnlyMatrix3 rotate, ReadOnlyVector3 translate, ReadOnlyVector3 scale,
            BoundingVolume store);

    /**
     * 
     * <code>transform</code> alters the location of the bounding volume by a transform.
     * 
     * @param transform
     * @param store
     * @return
     */
    public BoundingVolume transform(final ReadOnlyTransform transform, final BoundingVolume store) {
        final ReadOnlyMatrix3 rotation = transform.getMatrix();
        final ReadOnlyVector3 translation = transform.getTranslation();
        final ReadOnlyVector3 scale = transform.getScale();

        final BoundingVolume volume = transform(rotation, translation, scale, store);
        return volume;
    }

    /**
     * 
     * <code>whichSide</code> returns the side on which the bounding volume lies on a plane. Possible values are
     * POSITIVE_SIDE, NEGATIVE_SIDE, and NO_SIDE.
     * 
     * @param plane
     *            the plane to check against this bounding volume.
     * @return the side on which this bounding volume lies.
     */
    public abstract ReadOnlyPlane.Side whichSide(ReadOnlyPlane plane);

    /**
     * 
     * <code>computeFromPoints</code> generates a bounding volume that encompasses a collection of points.
     * 
     * @param points
     *            the points to contain.
     */
    public abstract void computeFromPoints(FloatBuffer points);

    /**
     * <code>merge</code> combines two bounding volumes into a single bounding volume that contains both this bounding
     * volume and the parameter volume.
     * 
     * @param volume
     *            the volume to combine.
     * @return the new merged bounding volume.
     */
    public abstract BoundingVolume merge(BoundingVolume volume);

    /**
     * <code>mergeLocal</code> combines two bounding volumes into a single bounding volume that contains both this
     * bounding volume and the parameter volume. The result is stored locally.
     * 
     * @param volume
     *            the volume to combine.
     * @return this
     */
    public abstract BoundingVolume mergeLocal(BoundingVolume volume);

    /**
     * <code>clone</code> creates a new BoundingVolume object containing the same data as this one.
     * 
     * @param store
     *            where to store the cloned information. if null or wrong class, a new store is created.
     * @return the new BoundingVolume
     */
    public abstract BoundingVolume clone(BoundingVolume store);

    public final ReadOnlyVector3 getCenter() {
        return center;
    }

    public final void setCenter(final ReadOnlyVector3 newCenter) {
        center.set(newCenter);
    }

    /**
     * Find the distance from the center of this Bounding Volume to the given point.
     * 
     * @param point
     *            The point to get the distance to
     * @return distance
     */
    public final double distanceTo(final ReadOnlyVector3 point) {
        return center.distance(point);
    }

    /**
     * Find the squared distance from the center of this Bounding Volume to the given point.
     * 
     * @param point
     *            The point to get the distance to
     * @return distance
     */
    public final double distanceSquaredTo(final ReadOnlyVector3 point) {
        return center.distanceSquared(point);
    }

    /**
     * Find the distance from the nearest edge of this Bounding Volume to the given point.
     * 
     * @param point
     *            The point to get the distance to
     * @return distance
     */
    public abstract double distanceToEdge(ReadOnlyVector3 point);

    /**
     * determines if this bounding volume and a second given volume are intersecting. Intersecting being: one volume
     * contains another, one volume overlaps another or one volume touches another.
     * 
     * @param bv
     *            the second volume to test against.
     * @return true if this volume intersects the given volume.
     */
    public abstract boolean intersects(BoundingVolume bv);

    /**
     * determines if a ray intersects this bounding volume.
     * 
     * @param ray
     *            the ray to test.
     * @return true if this volume is intersected by a given ray.
     */
    public abstract boolean intersects(ReadOnlyRay3 ray);

    /**
     * determines if a ray intersects this bounding volume and if so, where.
     * 
     * @param ray
     *            the ray to test.
     * @return an IntersectionRecord containing information about any intersections made by the given Ray with this
     *         bounding
     */
    public abstract IntersectionRecord intersectsWhere(ReadOnlyRay3 ray);

    /**
     * determines if this bounding volume and a given bounding sphere are intersecting.
     * 
     * @param bs
     *            the bounding sphere to test against.
     * @return true if this volume intersects the given bounding sphere.
     */
    public abstract boolean intersectsSphere(BoundingSphere bs);

    /**
     * determines if this bounding volume and a given bounding box are intersecting.
     * 
     * @param bb
     *            the bounding box to test against.
     * @return true if this volume intersects the given bounding box.
     */
    public abstract boolean intersectsBoundingBox(BoundingBox bb);

    /**
     * determines if this bounding volume and a given bounding box are intersecting.
     * 
     * @param bb
     *            the bounding box to test against.
     * @return true if this volume intersects the given bounding box.
     */
    public abstract boolean intersectsOrientedBoundingBox(OrientedBoundingBox bb);

    /**
     * 
     * determines if a given point is contained within this bounding volume.
     * 
     * @param point
     *            the point to check
     * @return true if the point lies within this bounding volume.
     */
    public abstract boolean contains(ReadOnlyVector3 point);

    public void write(final Ardor3DExporter e) throws IOException {
        e.getCapsule(this).write(center, "center", new Vector3(Vector3.ZERO));
    }

    public void read(final Ardor3DImporter e) throws IOException {
        center.set((Vector3) e.getCapsule(this).readSavable("center", new Vector3(Vector3.ZERO)));
    }

    public Class<? extends BoundingVolume> getClassTag() {
        return this.getClass();
    }

    public abstract void computeFromTris(int[] triIndex, Mesh mesh, int start, int end);

    public abstract void computeFromTris(ReadOnlyTriangle[] tris, int start, int end);

    public abstract double getVolume();
}
