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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Point;

/**
 * Curve class contains a list of control points and a spline. It also contains method for visualising itself as a
 * renderable series of points or a line.
 */
public class Curve {

    /** @see #setControlPoints(List) */
    private List<ControlPoint> _controlPoints;

    /** @see #setSpline(Spline) */
    private Spline _spline;

    /**
     * Creates a new instance of <code>Curve</code>.
     * 
     * @param controlPoints
     *            see {@link #setControlPoints(List)}
     * @param spline
     *            see {@link #setSpline(Spline)}
     */
    public Curve(final List<ControlPoint> controlPoints, final Spline spline) {
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
        final Collection<Vector3> points = getPoints(includeEndPoints);

        final Vector3[] pointsArray = new Vector3[points.size()];

        points.toArray(pointsArray);

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

        final Vector3[] vertex = new Vector3[vertices * 2];
        final Vector3[] normal = null;
        final ColorRGBA[] color = null;
        final Vector2[] texture = null;

        int index = 0;

        for (int i = 0; i < vertices; i++) {
            final int even = i % 2;

            final int is = i % steps;

            if (0 != even || 0 == is) {
                if (0 == is && i > 0) {
                    index++;
                }

                final double t = is / (steps - 1.0);

                vertex[i] = getSpline().interpolate(getControlPoints().get(index).getPoint(),
                        getControlPoints().get(index + 1).getPoint(), getControlPoints().get(index + 2).getPoint(),
                        getControlPoints().get(index + 3).getPoint(), t);
            } else {
                vertex[i] = vertex[i - 1];
            }
        }

        return new Line("curve", vertex, normal, color, texture);
    }

    public List<Vector3> getPoints(final boolean includeEndPoints) {
        final List<Vector3> points = new ArrayList<Vector3>();

        for (final ControlPoint cp : getControlPoints()) {
            points.add((Vector3) cp.getPoint()); // TODO: Potentially unsafe cast here
        }

        if (!includeEndPoints && points.size() >= 2) {
            points.remove(0);
            points.remove(points.size() - 1);
        }

        return points;
    }

    public void setControlPoints(final List<ControlPoint> controlPoints) {
        _controlPoints = controlPoints;
    }

    public List<ControlPoint> getControlPoints() {
        return _controlPoints;
    }

    public void setSpline(final Spline spline) {
        _spline = spline;
    }

    public Spline getSpline() {
        return _spline;
    }
}
