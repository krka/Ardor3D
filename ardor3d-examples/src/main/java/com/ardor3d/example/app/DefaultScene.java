/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.app;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Scene;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;

/**
 * The framework should provide a default implementation of a scene, probably with a root node and a stats node, just
 * like today. Probably, controllers don't belong here, but I also don't think they belong in the Node API.
 */
public final class DefaultScene implements Scene {
    private final Node root;

    public DefaultScene() {
        root = new Node("root");
    }

    public Node getRoot() {
        return root;
    }

    @MainThread
    public boolean renderUnto(final Renderer renderer) {
        renderer.draw(root);
        return true;
    }

    public PickResults doPick(final Ray3 pickRay) {
        // does nothing.
        return null;
    }
}
