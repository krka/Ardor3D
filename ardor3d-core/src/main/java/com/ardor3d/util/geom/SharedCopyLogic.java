/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

public class SharedCopyLogic implements CopyLogic {
    public Spatial copy(final Spatial source, final AtomicBoolean recurse) {
        recurse.set(false);
        if (source instanceof Node) {
            recurse.set(true);
            return clone((Node) source);
        } else if (source instanceof Mesh) {
            final Mesh result = clone((Mesh) source);
            result.setMeshData(((Mesh) source).getMeshData());
            result.updateModelBound();
            return result;
        }
        return source.clone();
    }

    protected Mesh clone(final Mesh original) {
        final Mesh copy = new Mesh(original.getName());
        copy.getSceneHints().set(original.getSceneHints());
        copy.setTransform(original.getTransform());

        for (final StateType type : StateType.values()) {
            final RenderState state = original.getLocalRenderState(type);
            if (state != null) {
                copy.setRenderState(state);
            }
        }
        return copy;
    }

    protected Node clone(final Node original) {
        final Node copy = new Node(original.getName());
        copy.getSceneHints().set(original.getSceneHints());
        copy.setTransform(original.getTransform());

        for (final StateType type : StateType.values()) {
            final RenderState state = original.getLocalRenderState(type);
            if (state != null) {
                copy.setRenderState(state);
            }
        }
        return copy;
    }
}
