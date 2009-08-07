/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.controller.interpolation;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.spline.Curve;
import com.ardor3d.spline.Spline;

/**
 * CurveInterpolationController class interpolates a {@link Spatial}s vectors using a {@link Curve}.
 */
public class CurveInterpolationController extends Vector3InterpolationController {

    /** Serial UID */
    private static final long serialVersionUID = 1L;

    /** @see #setCurve(Curve) */
    private Curve _curve;

    @Override
    protected Vector3 interpolateVectors(final ReadOnlyVector3 from, final ReadOnlyVector3 to, final double delta,
            final Vector3 target) {

        assert (null != from) : "parameter 'from' can not be null";
        assert (null != to) : "parameter 'to' can not be null";

        final ReadOnlyVector3 p0 = getControlPointStart();
        final ReadOnlyVector3 p3 = getCotnrolPointEnd();

        final Spline spline = getCurve().getSpline();

        return spline.interpolate(p0, from, to, p3, delta, target);
    }

    /**
     * @return The initial control point, will not be <code>null</code>.
     */
    protected ReadOnlyVector3 getControlPointStart() {
        ReadOnlyVector3 control = null;

        final int fromIndex = getControls().indexOf(getControlFrom());

        switch (getRepeatType()) {
            case CLAMP:
                control = getControls().get(fromIndex - 1);
                break;

            case CYCLE:
                if (isCycleForward()) {
                    if (fromIndex == getMinimumIndex()) {
                        control = getControls().get(fromIndex + 1);
                    } else {
                        control = getControls().get(fromIndex - 1);
                    }
                } else {
                    if (fromIndex == getMaximumIndex()) {
                        control = getControls().get(fromIndex - 1);
                    } else {
                        control = getControls().get(fromIndex + 1);
                    }
                }
                break;

            case WRAP:
                if (fromIndex == getMaximumIndex()) {
                    control = getControls().get(getMaximumIndex() + 1);
                } else {
                    control = getControls().get(fromIndex - 1);
                }
                break;
        }

        return control;
    }

    /**
     * @return The final control point, will not be <code>null</code>.
     */
    protected ReadOnlyVector3 getCotnrolPointEnd() {
        ReadOnlyVector3 control = null;

        final int toIndex = getControls().indexOf(getControlTo());

        switch (getRepeatType()) {
            case CLAMP:
                control = getControls().get(toIndex + 1);
                break;

            case CYCLE:
                if (isCycleForward()) {
                    if (toIndex == getMaximumIndex()) {
                        control = getControls().get(toIndex - 1);
                    } else {
                        control = getControls().get(toIndex + 1);
                    }

                } else {
                    if (toIndex == getMinimumIndex()) {
                        control = getControls().get(toIndex + 1);
                    } else {
                        control = getControls().get(toIndex - 1);
                    }
                }
                break;

            case WRAP:
                if (toIndex == getMinimumIndex()) {
                    control = getControls().get(toIndex + 1);
                } else {
                    control = getControls().get(toIndex + 1);
                }
                break;
        }

        return control;
    }

    /**
     * Setting a new curve will automatically update the control points.
     * 
     * @param curve
     *            The new curve to follow, can not be <code>null</code>.
     * @see #getCurve()
     */
    public void setCurve(final Curve curve) {
        if (null == curve) {
            throw new IllegalArgumentException("curve can not be null!");
        }

        _curve = curve;

        setControls(_curve.getControlPoints());
    }

    /**
     * @return The curve being followed, will not <code>null</code>.
     * @see #setCurve(Curve)
     */
    public Curve getCurve() {
        assert (null != _curve) : "curve was null, it must be set before use!";

        return _curve;
    }

    /**
     * Since splines require at least 4 points to interpolate correctly the default maximum value is overridden to 1
     * less than normal.
     */
    @Override
    protected int getMaximumIndex() {
        return super.getMaximumIndex() - 1;
    }

    /**
     * Since splines require at least 4 points to interpolate correctly the default minimum value is overridden to 1
     * more than normal.
     */
    @Override
    protected int getMinimumIndex() {
        return super.getMinimumIndex() + 1;
    }

}
