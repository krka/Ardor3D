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

import java.util.List;

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.ComplexSpatialController;
import com.ardor3d.scenegraph.Spatial;

/**
 * CurveController class is an implementation of Controller that can be used to make a spatial follow a {@link Curve}.
 */
public class CurveController extends ComplexSpatialController<Spatial> {

    /** Serial UID */
    private static final long serialVersionUID = 1L;

    /** @see #setCurve(Curve) */
    private Curve _curve;

    /** @see #setInterpolateRotation(boolean) */
    private boolean _interpolateRotation;

    /** Used to interpolate between two given control points 0 <= _t <= 1 */
    private double _t;

    /** Last control point used for interpolation */
    private int _lastIndex;

    /** Used when the repeat type is CYCLE to specify the direction moving through control points */
    private boolean _cycleForward = true;

    /**
     * Creates a new instance of <code>CurveController</code>.
     * 
     * @param curve
     *            see {@link #setCurve(Curve)}
     * @param interpolateRotation
     *            see {@link #setInterpolateRotation(boolean)}
     */
    public CurveController(final Curve curve, final boolean interpolateRotation) {
        super();

        setCurve(curve);
        setInterpolateRotation(interpolateRotation);
    }

    /**
     * If this controller isn't {@link #setActive(boolean) active}, or the {@link #setSpeed(double) speed} is <= 0, or
     * the {@link #setMaxTime(double) max time} is <= 0, or <code>caller</code> is <code>null</code>, no update is
     * performed and this method just returns silently.
     * 
     * @param time
     *            The time since update was last called.
     * @param caller
     *            The spatial to update.
     */
    @Override
    public void update(final double time, final Spatial caller) {

        if (isActive() && getSpeed() > 0.0 && getMaxTime() > 0.0 && null != caller) {

            /* Adjust the time based on the speed */
            final double actualTimeBetweenPoints = getMaxTime() / getSpeed();

            /* Calculate the distance between control points */
            _t += ((1.0 / actualTimeBetweenPoints) * time);

            /* If > 1 then we need to start interpolating between next set of points */
            if (_t >= 1.0) {
                _t -= 1.0;

                if (_cycleForward) {
                    _lastIndex++;
                } else {
                    _lastIndex--;
                }
            }

            /* The vectors to interpolate, actual depend on the cycle type used */
            ReadOnlyVector3 p0 = null;
            ReadOnlyVector3 p1 = null;
            ReadOnlyVector3 p2 = null;
            ReadOnlyVector3 p3 = null;
            ReadOnlyQuaternion startQuat = null;
            ReadOnlyQuaternion endQuat = null;

            final Vector3 result = (Vector3) caller.getTranslation();

            final List<ControlPoint> controlPoints = getCurve().getControlPoints();

            switch (getRepeatType()) {
                case CLAMP:
                    if (_lastIndex >= (controlPoints.size() - 3)) {
                        /* Set point to last (non control) point to ensure we are precisely at the end */
                        result.set(controlPoints.get(controlPoints.size() - 2).getPoint());
                        caller.setTranslation(result);

                        /* Reset these just to be on the safe side */
                        _lastIndex = controlPoints.size();
                        _t = 0.0;

                        /* No reason to do anything further so return */
                        return;

                    } else {
                        p0 = controlPoints.get(_lastIndex).getPoint();
                        p1 = controlPoints.get(_lastIndex + 1).getPoint();
                        p2 = controlPoints.get(_lastIndex + 2).getPoint();
                        p3 = controlPoints.get(_lastIndex + 3).getPoint();

                        startQuat = controlPoints.get(_lastIndex + 1).getRotation();
                        endQuat = controlPoints.get(_lastIndex + 2).getRotation();
                    }

                    break;

                case CYCLE:
                    // TODO: Implement cycle repeat type
                    return;

                case WRAP:
                    if (_lastIndex >= controlPoints.size()) {
                        _lastIndex = 0;

                        p0 = controlPoints.get(_lastIndex).getPoint();
                        p1 = controlPoints.get(_lastIndex + 1).getPoint();
                        p2 = controlPoints.get(_lastIndex + 2).getPoint();
                        p3 = controlPoints.get(_lastIndex + 3).getPoint();

                        startQuat = controlPoints.get(_lastIndex + 1).getRotation();
                        endQuat = controlPoints.get(_lastIndex + 2).getRotation();

                    } else if (_lastIndex >= (controlPoints.size() - 1)) {
                        p0 = controlPoints.get(_lastIndex).getPoint();

                        _lastIndex = 0;

                        p1 = controlPoints.get(_lastIndex).getPoint();
                        p2 = controlPoints.get(_lastIndex + 1).getPoint();
                        p3 = controlPoints.get(_lastIndex + 2).getPoint();

                        startQuat = controlPoints.get(_lastIndex).getRotation();
                        endQuat = controlPoints.get(_lastIndex + 1).getRotation();

                    } else if (_lastIndex >= (controlPoints.size() - 2)) {
                        p0 = controlPoints.get(_lastIndex).getPoint();
                        p1 = controlPoints.get(_lastIndex + 1).getPoint();

                        _lastIndex = 0;

                        p2 = controlPoints.get(_lastIndex).getPoint();
                        p3 = controlPoints.get(_lastIndex + 1).getPoint();

                        startQuat = controlPoints.get(_lastIndex + 1).getRotation();
                        endQuat = controlPoints.get(_lastIndex).getRotation();

                    } else if (_lastIndex >= (controlPoints.size() - 3)) {
                        p0 = controlPoints.get(_lastIndex).getPoint();
                        p1 = controlPoints.get(_lastIndex + 1).getPoint();
                        p2 = controlPoints.get(_lastIndex + 2).getPoint();

                        _lastIndex = 0;

                        p3 = controlPoints.get(_lastIndex).getPoint();

                        startQuat = controlPoints.get(_lastIndex + 1).getRotation();
                        endQuat = controlPoints.get(_lastIndex + 2).getRotation();

                    } else {
                        p0 = controlPoints.get(_lastIndex).getPoint();
                        p1 = controlPoints.get(_lastIndex + 1).getPoint();
                        p2 = controlPoints.get(_lastIndex + 2).getPoint();
                        p3 = controlPoints.get(_lastIndex + 3).getPoint();

                        startQuat = controlPoints.get(_lastIndex + 1).getRotation();
                        endQuat = controlPoints.get(_lastIndex + 2).getRotation();
                    }

                    break;
            }

            caller.setTranslation(getCurve().getSpline().interpolate(p0, p1, p2, p3, _t, result));

            if (isInterpolateRotation()) {
                final Quaternion quat = Quaternion.fetchTempInstance();

                quat.fromRotationMatrix(caller.getRotation());
                quat.slerpLocal(startQuat, endQuat, _t);

                final Matrix3 rotMat = (Matrix3) caller.getRotation();

                quat.toRotationMatrix(rotMat);

                caller.setRotation(rotMat);

                Quaternion.releaseTempInstance(quat);
            }
        }
    }

    /**
     * @param curve
     *            The new curve to follow, can not be <code>null</code>.
     * @throws IllegalArgumentException
     *             If a <code>null</code> argument is passed.
     * @see #getCurve()
     */
    public void setCurve(final Curve curve) {
        if (null == curve) {
            throw new IllegalArgumentException("curve can not be null!");
        }

        _curve = curve;

        /* Reset these as they depend on the curve being followed */
        _lastIndex = 0;
        _t = 0.0;
        _cycleForward = true;
    }

    /**
     * @return The curve being followed, will not <code>null</code>.
     * @see #setCurve(Curve)
     */
    public Curve getCurve() {
        return _curve;
    }

    /**
     * @param interpolateRotation
     *            <code>true</code> to also interpolate the rotation of the spatial.
     * @see #isInterpolateRotation()
     */
    public void setInterpolateRotation(final boolean interpolateRotation) {
        _interpolateRotation = interpolateRotation;
    }

    /**
     * @return <code>true</code> if the rotation of the spatial is also being interpolated.
     * @see #setInterpolateRotation(boolean)
     */
    public boolean isInterpolateRotation() {
        return _interpolateRotation;
    }

    /*
     * (non-javadoc)
     */
    @Override
    public void setRepeatType(final RepeatType repeatType) {
        if (getRepeatType() != repeatType) {
            /* Reset cycle type for safety */
            _cycleForward = true;
        }

        super.setRepeatType(repeatType);
    }

}
