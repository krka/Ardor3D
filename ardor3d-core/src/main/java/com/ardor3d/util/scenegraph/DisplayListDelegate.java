/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.scenegraph;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;

public class DisplayListDelegate implements RenderDelegate {

    private final int _id;

    public DisplayListDelegate(final int id) {
        _id = id;
    }

    public void render(final Spatial spatial, final Renderer renderer) {
        // do transforms
        renderer.doTransforms(spatial.getWorldTransform());

        // render display list.
        renderer.renderDisplayList(_id);

        // Our states are in an unknown state at this point, so invalidate tracking.
        ContextManager.getCurrentContext().invalidateStates();
    }
}
