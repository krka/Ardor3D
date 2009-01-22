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

import com.ardor3d.math.type.ReadOnlyLineSegment3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.Debug;
import com.ardor3d.util.pool.ObjectPool;

public class LineSegment3 extends Line3Base implements ReadOnlyLineSegment3 {

    private static final long serialVersionUID = 1L;

    private static final LineSegment3Pool LINESEG3_POOL = new LineSegment3Pool(11);

    protected double _extent;

    /**
     * Constructs a new segment segment with an origin at (0,0,0) a direction of (0,0,1) and an extent of 0.5.
     */
    public LineSegment3() {
        super(Vector3.ZERO, Vector3.UNIT_Z);
    }

    /**
     * Constructs a new segment segment using the supplied origin point, unit length direction vector and extent
     * 
     * @param origin
     * @param direction
     *            - unit length
     * @param extent
     */
    public LineSegment3(final ReadOnlyVector3 origin, final ReadOnlyVector3 direction, final double extent) {
        super(origin, direction);
        _extent = extent;
    }

    /**
     * Constructs a new segment segment using the supplied start and end points
     * 
     * @param start
     * @param end
     */
    public LineSegment3(final ReadOnlyVector3 start, final ReadOnlyVector3 end) {
        this();
        _origin.set(start).addLocal(end).multiplyLocal(0.5);
        _direction.set(end).subtractLocal(start);
        _extent = 0.5 * _direction.length();
        _direction.normalizeLocal();
    }

    /**
     * Copies the values of the given source segment into this segment.
     * 
     * @param source
     * @return this segment for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public LineSegment3 set(final ReadOnlyLineSegment3 source) {
        _origin.set(source.getOrigin());
        _direction.set(source.getDirection());
        return this;
    }

    /**
     * @return this segment's extent value
     */
    public double getExtent() {
        return _extent;
    }

    /**
     * Sets the segment's extent to the provided value.
     * 
     * @param extent
     */
    public void setExtent(final double extent) {
        _extent = extent;
    }

    public Vector3 getPositiveEnd(final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        result.set(getDirection()).multiplyLocal(_extent);
        result.addLocal(getOrigin());
        return result;
    }

    public Vector3 getNegativeEnd(final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        result.set(getDirection()).multiplyLocal(-_extent);
        result.addLocal(getOrigin());
        return result;
    }

    /**
     * @param point
     * @param store
     *            if not null, the closest point is stored in this param
     * @return the squared distance from this segment to the given point.
     * @throws NullPointerException
     *             if the point is null.
     */
    public double distanceSquared(final ReadOnlyVector3 point, final Vector3 store) {
        final Vector3 vectorA = Vector3.fetchTempInstance();
        vectorA.set(point).subtractLocal(_origin);

        // Note: assumes direction is normalized
        final double t0 = _direction.dot(vectorA);

        if (-_extent < t0) {
            if (t0 < _extent) {
                // d = |P - (O + t*D)|
                vectorA.set(getDirection()).multiplyLocal(t0);
                vectorA.addLocal(getOrigin());
            } else {
                // ray is closest to positive (end) end point
                getPositiveEnd(vectorA);
            }
        } else {
            // ray is closest to negative (start) end point
            getNegativeEnd(vectorA);
        }

        // Save away the closest point if requested.
        if (store != null) {
            store.set(vectorA);
        }

        point.subtract(vectorA, vectorA);
        final double lSQ = vectorA.lengthSquared();
        Vector3.releaseTempInstance(vectorA);
        return lSQ;
    }

    /**
     * 
     * @param position
     *            a random position lying somewhere on this line segment.
     */
    public Vector3 random(final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        final double rand = MathUtils.nextRandomDouble();

        result.setX(2 * _extent * getOrigin().getX() * (1 - rand) + (getDirection().getX() * _extent) * (2 * rand - 1));
        result.setY(2 * _extent * getOrigin().getY() * (1 - rand) + (getDirection().getY() * _extent) * (2 * rand - 1));
        result.setZ(2 * _extent * getOrigin().getZ() * (1 - rand) + (getDirection().getZ() * _extent) * (2 * rand - 1));

        return result;
    }

    /**
     * Check a segment... if it is null or the values of its origin or direction or extent are NaN or infinite, return
     * false. Else return true.
     * 
     * @param segment
     *            the segment to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final ReadOnlyLineSegment3 segment) {
        if (segment == null) {
            return false;
        }

        return Vector3.isValid(segment.getDirection()) && Vector3.isValid(segment.getOrigin())
                && !Double.isInfinite(segment.getExtent()) && !Double.isNaN(segment.getExtent());
    }

    /**
     * @return the string representation of this segment.
     */
    @Override
    public String toString() {
        return "com.ardor3d.math.LineSegment3 [Origin: " + _origin + " - Direction: " + _direction + " - Extent: "
                + _extent + "]";
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this segment and the provided segment have the same constant and normal values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyLineSegment3)) {
            return false;
        }
        final ReadOnlyLineSegment3 comp = (ReadOnlyLineSegment3) o;
        return _origin.equals(comp.getOrigin()) && _direction.equals(comp.getDirection())
                && (Double.compare(_extent, comp.getExtent()) == 0);
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public LineSegment3 clone() {
        return (LineSegment3) super.clone();
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of LineSegment3 that is intended for temporary use in calculations and so forth. Multiple
     *         calls to the method should return instances of this class that are not currently in use.
     */
    public final static LineSegment3 fetchTempInstance() {
        if (Debug.useMathPools) {
            return LINESEG3_POOL.fetch();
        } else {
            return new LineSegment3();
        }
    }

    /**
     * Releases a LineSegment3 back to be used by a future call to fetchTempInstance. TAKE CARE: this LineSegment3
     * object should no longer have other classes referencing it or "Bad Things" will happen.
     * 
     * @param segment
     *            the LineSegment3 to release.
     */
    public final static void releaseTempInstance(final LineSegment3 segment) {
        if (Debug.useMathPools) {
            LINESEG3_POOL.release(segment);
        }
    }

    static final class LineSegment3Pool extends ObjectPool<LineSegment3> {
        public LineSegment3Pool(final int initialSize) {
            super(initialSize);
        }

        @Override
        protected LineSegment3 newInstance() {
            return new LineSegment3();
        }
    }
}
