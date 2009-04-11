/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
package com.ardor3d.animations.runtime;

import com.ardor3d.animations.reference.Skeleton;

/**
 * TODO: document this class!
 *
 */
public class SkeletonInstance {
    private final Skeleton _skeleton;

    public SkeletonInstance(Skeleton skeleton) {
        this._skeleton = skeleton;
    }

    // this class needs to have runtime state information about the _skeleton: which positions are the joints in right now?
    // it possibly also should have the logic that is needed to interpolate joint positions
}
