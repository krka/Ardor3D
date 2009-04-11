/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
package com.ardor3d.animations.reference;

import com.ardor3d.animations.InterpolationState;
import com.ardor3d.annotation.Immutable;

/**
 * TODO: document this class!
 *
 */
@Immutable
public class KeyFrame {
    private final double _startTime;
    private final InterpolationMethod _interpolationMethod; // interpolation from this frame

    // something that indicates the transformation at the start time, probably a set of interpolation states
    // for each relevant variable: transalation x, y, z and rotation x, y and z. interpolation state is
    // different for different interpolation methods, which could complicate things if the interpolation method
    // would change in a given animation. Probably disallow that case initially.

    private final InterpolationState translate;

    private final InterpolationState rotate;

    public KeyFrame(double startTime, InterpolationMethod interpolationMethod, InterpolationState translate, InterpolationState rotate) {
        this._startTime = startTime;
        this._interpolationMethod = interpolationMethod;
        this.translate = translate;
        this.rotate = rotate;
    }

    public double getStartTime() {
        return _startTime;
    }

    public InterpolationMethod getInterpolationMethod() {
        return _interpolationMethod;
    }

    public InterpolationState getTranslate() {
        return translate;
    }

    public InterpolationState getRotate() {
        return rotate;
    }
}
