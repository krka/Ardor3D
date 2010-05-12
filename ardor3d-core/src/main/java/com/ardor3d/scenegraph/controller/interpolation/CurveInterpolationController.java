/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.controller.interpolation;

import java.util.logging.Logger;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.ComplexSpatialController;
import com.ardor3d.spline.ArcLengthTable;
import com.ardor3d.spline.Curve;
import com.ardor3d.spline.Spline;

/**
 * CurveInterpolationController class interpolates a {@link Spatial}s vectors using a {@link Curve}.
 */
public class CurveInterpolationController extends Vector3InterpolationController {

    /** Serial UID */
    private static final long serialVersionUID = 1L;

    /** Classes logger */
    private static final Logger LOGGER = Logger.getLogger(CurveInterpolationController.class.getName());

    /** @see #setCurve(Curve) */
    private Curve _curve;

    /** Look up table of arc lengths (used for constant speed) */
    private ArcLengthTable _arcLengths;

    /** Look up table of arc lengths (used for constant speed when travelling in reverse (repeat type = cycle)) */
    private ArcLengthTable _arcLengthsReverse;

    /** Distance travelled between control points (used for constant speed) */
    private double _distance = 0.0;

    /*
     * Overrides to handle constant speed updating
     */
    @Override
    protected double incrementDelta(final double by) {
        double delta;

        /*
         * If constant speed we need to also check we aren't clamped at max index before we call lookup in the arc
         * length table because there would be no point
         */
        if (isConstantSpeed() && (!RepeatType.CLAMP.equals(getRepeatType()) || getIndex() != getMaximumIndex())) {
            _distance += by;

            if (isCycleForward()) {
                assert (null != _arcLengths) : "You need to call generateArcLengths() to create the required arc length tables!";

                delta = _arcLengths.getDelta(getIndex(), _distance);
            } else {
                assert (null != _arcLengthsReverse) : "You need to call generateArcLengths() to create the required arc length tables!";

                delta = _arcLengthsReverse.getDelta(getIndex(), _distance);
            }

            setDelta(delta);
        } else {
            delta = super.incrementDelta(by);
        }

        return delta;
    }

    @Override
    protected int decrementIndex() {
        assert (null != _arcLengthsReverse) : "You need to call generateArcLengths() to create the required arc length tables!";

        _distance -= _arcLengthsReverse.getLength(getIndex());

        return super.decrementIndex();
    }

    @Override
    protected int incrementIndex() {
        assert (null != _arcLengths) : "You need to call generateArcLengths() to create the required arc length tables!";

        _distance -= _arcLengths.getLength(getIndex());

        return super.incrementIndex();
    }

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

        final int fromIndex = getIndex();

        switch (getRepeatType()) {
            case CLAMP:
                control = getControls().get(fromIndex - 1);
                break;

            case CYCLE:
                if (isCycleForward()) {
                    control = getControls().get(fromIndex - 1);
                } else {
                    control = getControls().get(fromIndex + 1);
                }
                break;

            case WRAP:
                control = getControls().get(fromIndex - 1);
                break;
        }

        return control;
    }

    /**
     * @return The final control point, will not be <code>null</code>.
     */
    protected ReadOnlyVector3 getCotnrolPointEnd() {
        ReadOnlyVector3 control = null;

        final int toIndex = getIndex() + 1;

        switch (getRepeatType()) {
            case CLAMP:
                control = getControls().get(toIndex + 1);
                break;

            case CYCLE:
                if (isCycleForward()) {
                    control = getControls().get(toIndex + 1);
                } else {
                    control = getControls().get(toIndex - 1);
                }
                break;

            case WRAP:
                control = getControls().get(toIndex + 1);
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

        if (isConstantSpeed()) {
            LOGGER
                    .warning("Constant speed is set to true, you will need to call generateArcLengths() to avoid errors during update.");
        }
    }

    /**
     * @return The curve being followed, will not <code>null</code>.
     * @see #setCurve(Curve)
     */
    public Curve getCurve() {
        assert (null != _curve) : "curve was null, it must be set before use!";

        return _curve;
    }

    /*
     * Overrides to provide a warning about generating arc lengths if constant speed is set to true and they haven't
     * been generated yet.
     */
    @Override
    public void setConstantSpeed(final boolean constantSpeed) {
        super.setConstantSpeed(constantSpeed);

        if (isConstantSpeed() && null == _arcLengths) {
            LOGGER
                    .warning("Constant speed was set to true, you will need to call generateArcLengths() to avoid errors during update.");
        }
    }

    /**
     * Generates the arc lengths, generates the reverse table if the
     * {@link #setRepeatType(com.ardor3d.scenegraph.controller.ComplexSpatialController.RepeatType) repeat type} is set
     * to {@link ComplexSpatialController.RepeatType#CYCLE cycle}
     * 
     * @param step
     *            'See Also:' method for more info.
     * @see #generateArcLengths(int, boolean)
     */
    public void generateArcLengths(final int step) {
        generateArcLengths(step, RepeatType.CYCLE.equals(getRepeatType()));
    }

    /**
     * Generates arc lengths which are required if you wish to have {@link #setConstantSpeed(boolean) constant speed}
     * interpolation.
     * 
     * @param step
     *            'See Also:' method for more info.
     * @param reverse
     *            'See Also:' method for more info.
     * @see ArcLengthTable#generate(int, boolean)
     */
    public void generateArcLengths(final int step, final boolean reverse) {
        _arcLengths = new ArcLengthTable(getCurve());
        _arcLengths.generate(step, false);

        if (reverse) {
            _arcLengthsReverse = new ArcLengthTable(getCurve());
            _arcLengthsReverse.generate(step, true);
        }
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

    @Override
    public void reset() {
        super.reset();

        _distance = 0.0;
    }

}
