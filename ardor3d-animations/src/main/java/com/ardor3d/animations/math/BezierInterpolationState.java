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

import com.ardor3d.annotation.Immutable;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.animations.InterpolationState;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * TODO: document this class!
 *
 */
@Immutable
public class BezierInterpolationState implements InterpolationState {
    private final ReadOnlyVector3 _value;
    private final ReadOnlyVector3 _controlValue;

    public BezierInterpolationState(ReadOnlyVector3 value, ReadOnlyVector3 controlValue) {
        this._value = new Vector3(checkNotNull(value, "_value"));
        this._controlValue = new Vector3(checkNotNull(controlValue, "controlValue"));
    }

    public ReadOnlyVector3 getValue() {
        return _value;
    }

    public ReadOnlyVector3 getControlValue() {
        return _controlValue;
    }
}
