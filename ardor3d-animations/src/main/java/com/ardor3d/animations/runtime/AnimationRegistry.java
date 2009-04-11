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

import com.ardor3d.animations.reference.Animatable;
import com.ardor3d.animations.reference.Skeleton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO: document this class!
 *
 */
public class AnimationRegistry {
    private final Map<String, Skeleton> _skeletons;
    private final Set<Animatable> _animatables;
    private final Set<AnimatableInstance> _animatableInstances;

    public AnimationRegistry() {
        _skeletons = new HashMap<String, Skeleton>();
        _animatables = new HashSet<Animatable>();
        _animatableInstances = new HashSet<AnimatableInstance>();
    }


    public Skeleton getSkeleton(String name) {
        return _skeletons.get(name);
    }

    public void registerSkeleton(String name, Skeleton skeleton) {
        _skeletons.put(name, skeleton);
    }

    public void registerAnimatable(Animatable animatable) {
        _animatables.add(animatable);
    }

    public void registerAnimatableInstance(AnimatableInstance animatableInstance) {
        _animatableInstances.add(animatableInstance);
    }

    // TODO: these two getters should maybe not be here, or at least probably return safe copies
    public Map<String, Skeleton> getSkeletons() {
        return _skeletons;
    }

    public Set<Animatable> getAnimatables() {
        return _animatables;
    }

    public Set<AnimatableInstance> getAnimatableInstances() {
        return _animatableInstances;
    }
}
