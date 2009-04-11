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

import com.ardor3d.annotation.Immutable;

import java.util.List;

/**
 * TODO: document this class!
 *
 */
@Immutable
public class Animation {
    // TODO: tie this in to a Skeleton somehow?!
    

    private final List<KeyFrame> _keyFrames;

    public Animation(List<KeyFrame> keyFrames) {
        this._keyFrames = keyFrames;
    }

    public List<KeyFrame> getKeyFrames() {
        return _keyFrames;
    }
}
