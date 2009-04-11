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
import com.ardor3d.scenegraph.Node;

/**
 * TODO: document this class!
 *
 */
public class AnimatableInstance {
    private final Animatable _animatable;
    private final Node _node;
    private final SkeletonInstance _skeletonInstance;


    public AnimatableInstance(Animatable animatable, Node node, SkeletonInstance skeletonInstance) {
        this._animatable = animatable;
        this._node = node;
        this._skeletonInstance = skeletonInstance;
    }

    // this guy should be updatable, which should mean that the current status of the skeleton gets applied to the skin.
    // the skin surely is available from the _node, right?

    public Animatable getAnimatable() {
        return _animatable;
    }

    public Node getNode() {
        return _node;
    }

    public SkeletonInstance getSkeletonInstance() {
        return _skeletonInstance;
    }
}
