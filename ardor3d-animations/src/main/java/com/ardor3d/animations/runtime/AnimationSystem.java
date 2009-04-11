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

/**
 * TODO: document this class!
 *
 */
public class AnimationSystem {
    private final AnimationRegistry _animationRegistry;

    public AnimationSystem(AnimationRegistry animationRegistry) {
        this._animationRegistry = animationRegistry;
    }

    public AnimationRegistry getAnimationRegistry() {
        return _animationRegistry;
    }
}
