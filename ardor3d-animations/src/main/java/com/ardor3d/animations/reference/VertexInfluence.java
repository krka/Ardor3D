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

/**
 * TODO: document this class!
 *
 */
@Immutable
public class VertexInfluence {
    private final int _vertexIndex;
    private final double _weight;

    public VertexInfluence(final int vertexIndex, final double weight) {
        this._vertexIndex = vertexIndex;
        this._weight = weight;
    }

    public int getVertexIndex() {
        return _vertexIndex;
    }

    public double getWeight() {
        return _weight;
    }

    @Override
    public String toString() {
        return "VertexInfluence{" +
                "_vertexIndex=" + _vertexIndex +
                ", _weight=" + _weight +
                '}';
    }
}
