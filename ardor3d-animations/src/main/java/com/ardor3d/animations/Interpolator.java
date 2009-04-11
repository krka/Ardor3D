/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
package com.ardor3d.animations;

import com.ardor3d.math.Vector3;

/**
 * TODO: document this class!
 *
 */
public interface Interpolator {
    public Vector3 interpolate(InterpolationState start, InterpolationState end, double fraction);
}
