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
import com.ardor3d.math.Transform;
import com.google.common.collect.ImmutableList;

/**
 * TODO: document this class!
 *
 */
@Immutable
public class Joint {
    private final Transform _bindPoseTransform;
    private final ImmutableList<VertexInfluence> _vertexInfluences;
    private final ImmutableList<Joint> _children;

    public Joint(Transform bindPoseTransform, ImmutableList<VertexInfluence> vertexInfluences, ImmutableList<Joint> children) {
        this._children = children;
        this._bindPoseTransform = new Transform(bindPoseTransform);
        this._vertexInfluences = vertexInfluences;
    }

    public Transform getBindPoseTransform() {
        return _bindPoseTransform;
    }

    public ImmutableList<VertexInfluence> getVertexInfluences() {
        return _vertexInfluences;
    }

    public ImmutableList<Joint> getChildren() {
        return _children;
    }


    @Override
    public String toString() {
        return "Joint{" +
                "_bindPoseTransform=" + _bindPoseTransform +
                ", _vertexInfluences=" + _vertexInfluences +
                ", _children=" + _children +
                '}';
    }
}
