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
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.Matrix4;
import com.ardor3d.scenegraph.Spatial;
import com.google.common.collect.ImmutableList;

/**
 * TODO: document this class!
 *
 */
@Immutable
public class Animatable {
    private final String _id;
    private final ReadOnlyMatrix4 _bindShapeMatrix;
    private final ImmutableList<Spatial> _bindShape;
    private final ImmutableList<Skeleton> _skeletons;

    public Animatable(String id, ReadOnlyMatrix4 bindShapeMatrix, ImmutableList<Spatial> bindShape, ImmutableList<Skeleton> skeletons) {
        this._id = id;
        this._bindShapeMatrix = new Matrix4(bindShapeMatrix);
        this._bindShape = bindShape;
        this._skeletons = skeletons;
    }

    @Override
    public String toString() {
        return "Animatable{" +
                "_id='" + _id + '\'' +
                ", _bindShapeMatrix=" + _bindShapeMatrix +
                ", _bindShape=" + _bindShape +
                ", _skeletons=" + _skeletons +
                '}';
    }

    public Skeleton getSkeleton() {
        return _skeletons.get(0);
    }

    public ImmutableList<Spatial> getBindShape() {
        return _bindShape;
    }
}
