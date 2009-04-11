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
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * TODO: document this class!
 *
 */
@Immutable
public class AnimationGroup {
    private final String _id;
    private final String _name;
    private final ImmutableList<Animation> _animations;

    public AnimationGroup(String id, String name, List<Animation> animations) {
        this._id = id;
        this._name = name;
        this._animations = ImmutableList.copyOf(animations);
    }

    public String getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public ImmutableList<Animation> getAnimations() {
        return _animations;
    }
}
