/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

/**
 * 
 */

package com.ardor3d.spline;

import java.util.Collection;
import java.util.List;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Point;

/**
 * Curve class contains a list of control points and a spline. It also contains method for visualizing itself as a
 * renderable series of points or a line.
 */
public class Curve {

    /** @see #setControlPoints(List) */
    private List<ReadOnlyVector3> _controlPoints;

    /** @see #setSpline(Spline) */
    private Spline _spline = new CatmullRomSpline();

    /**
     * Creates a new instance of <code>Curve</code>.
     * 
     * @param controlPoints
     *            see {@link #setControlPoints(List)}
     * @param spline
     *            see {@link #setSpline(Spline)}
     */
    public Curve(final List<ReadOnlyVector3> controlPoints, final Spline spline) {
        super();

        setControlPoints(controlPoints);
        setSpline(spline);
    }

    /**
     * Creates a new <code>Point</code> from the control points making up this curve. It will have the name
     * <code>point</code>, no normals, colour or texture, these can be added to the returned point if needed.
     * 
     * @param includeEndPoints
     *            <code>true</code> to include the first and last points (which aren't actually part of the curve, they
     *            are just used as controls to interpolate the first/last points correctly)
     * @return A <code>Point</code> containing all the curve points, will not be <code>null</code>.
     */
    public Point toRenderablePoint(final boolean includeEndPoints) {
        final Collection<ReadOnlyVector3> points = getControlPoints();

        final int size = includeEndPoints ? points.size() : points.size() - 2;

        Vector3[] allPoints = new Vector3[points.size()];
        Vector3[] pointsArray;

        allPoints = points.toArray(allPoints);

        if (includeEndPoints) {
            pointsArray = allPoints;
        } else {
            pointsArray = new Vector3[size];

            System.arraycopy(allPoints, 1, pointsArray, 0, size);
        }

        return new Point("point", pointsArray, null, null, null);
    }

    /**
     * Creates a new <code>Line</code> from the control points making up this curve. It will have the name
     * <code>curve</code>, no normals, colour or texture, these can be added to the returned line if needed.
     * 
     * @param steps
     *            The number of iterations to perform between control points, the higher this number the smoother the
     *            returned line will be, but it will also contain more vertices, must be greater than one.
     * @throws IllegalArgumentException
     *             If steps <= 1.
     * @return A <code>Line</code> representing this curve, will not be <code>null</code>.
     */
    public Line toRenderableLine(final int steps) {
        if (steps <= 0) {
            throw new IllegalArgumentException("steps must be > 1! steps=" + steps);
        }

        final int vertices = (getControlPoints().size() * steps) - (3 * steps);

        final Vector3[] vertex = new Vector3[vertices];
        final Vector3[] normal = null;
        final ColorRGBA[] color = null;
        final Vector2[] texture = null;

        int index = 0;

        for (int i = 0; i < vertices; i++) {
            final int is = i % steps;

            if (0 == is && i > 0) {
                index++;
            }

            final double t = is / (steps - 1.0);

            vertex[i] = getSpline().interpolate(getControlPoints().get(index), getControlPoints().get(index + 1),
                    getControlPoints().get(index + 2), getControlPoints().get(index + 3), t);
        }

        final Line line = new Line("curve", vertex, normal, color, texture);
        line.getMeshData().setIndexMode(IndexMode.LineStrip);
        return line;
    }

    /**
     * @param controlPoints
     *            The new control points, can not be <code>null</code>.
     * @see #getControlPoints()
     */
    public void setControlPoints(final List<ReadOnlyVector3> controlPoints) {
        if (null == controlPoints) {
            throw new IllegalArgumentException("controlPoints can not be null!");
        }
        if (controlPoints.size() < 4) {
            throw new IllegalArgumentException("controlPoints must contain at least 4 elements for this class to work!");
        }

        _controlPoints = controlPoints;
    }

    /**
     * @return The control points making up this curve, will not be <code>null</code>.
     * @see #setControlPoints(List)
     */
    public List<ReadOnlyVector3> getControlPoints() {
        assert (null != _controlPoints) : "_controlPoints was null, it must be set before use!";
        assert (_controlPoints.size() >= 4) : "_controlPoints contained less than 4 elements, it must be contain at least 4 for this class to work!";

        return _controlPoints;
    }

    /**
     * @param spline
     *            The new spline, can not be <code>null</code>.
     * @see #getSpline()
     */
    public void setSpline(final Spline spline) {
        if (null == spline) {
            throw new IllegalArgumentException("spline can not be null!");
        }

        _spline = spline;
    }

    /**
     * The default is a {@link CatmullRomSpline}.
     * 
     * @return The spline, will not be <code>null</code>.
     * @see #setSpline(Spline)
     */
    public Spline getSpline() {
        return _spline;
    }
}
