/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.animations.math;

import com.ardor3d.math.Vector3;
import com.ardor3d.animations.Interpolator;
import com.ardor3d.animations.InterpolationState;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * TODO: document this class!
 *
 */
public class BezierInterpolator implements Interpolator {
    public Vector3 interpolate(InterpolationState start, InterpolationState end, double fraction) {
        checkArgument(fraction >= 0 && fraction <=1, "fraction has to be in the range [0, 1], actual value: " + fraction);

        BezierInterpolationState startState = castToBezier(start, "start");
        BezierInterpolationState endState = castToBezier(end, "end");

        // these calculations need to be optimised at some point, for now they are excruciatingly detailed
        // to make them easy to understand.

        // cubic bezier: B(s) = P0 * (1 - s)3 + 3 * C0s * (1 - s)2 + 3 * C1 * s2 * (1 - s) + P1 * s3
        // P0 = start value, P1 = end value
        // C0 = start control value, C1, end control value
        Vector3 firstTerm = new Vector3();
        Vector3 secondTerm = new Vector3();
        Vector3 thirdTerm = new Vector3();
        Vector3 fourthTerm = new Vector3();

        startState.getValue().multiply(Math.pow((1 - fraction), 3), firstTerm);
        startState.getControlValue().multiply(fraction * Math.pow(1 - fraction, 2), secondTerm);
        endState.getControlValue().multiply(Math.pow(fraction, 2) * (1 - fraction), thirdTerm);
        endState.getValue().multiply(Math.pow(fraction, 3), fourthTerm);

        return firstTerm.addLocal(secondTerm).addLocal(thirdTerm).addLocal(fourthTerm);
    }

    private BezierInterpolationState castToBezier(InterpolationState interpolationState, String name) {
        if (!(interpolationState instanceof BezierInterpolationState)) {
            throw new ClassCastException(name + " isn't an instance of BezierInterpolationState");
        }

        return (BezierInterpolationState) interpolationState;
    }
}
