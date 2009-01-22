/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.shadow.stencil;

import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Mesh;

public class ProximityShadowGate implements ShadowGate {

    private final Camera camera;
    private final float distanceSQ;

    public ProximityShadowGate(final Camera cam, final float distance) {
        camera = cam;
        distanceSQ = distance * distance;
    }

    public boolean shouldDrawShadows(final Mesh mesh) {
        final ReadOnlyVector3 trans = mesh.getWorldTranslation();
        final ReadOnlyVector3 camLoc = camera.getLocation();
        final boolean isCloseEnough = (trans.distanceSquared(camLoc) <= distanceSQ);
        return isCloseEnough;
    }

    public boolean shouldUpdateShadows(final Mesh mesh) {
        // reuse same logic.
        return shouldDrawShadows(mesh);
    }

}
