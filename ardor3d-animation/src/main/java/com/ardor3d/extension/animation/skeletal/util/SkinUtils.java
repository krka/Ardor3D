/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.util;

import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.visitor.Visitor;

public class SkinUtils {

    /**
     * Simple utility to turn on / off bounding volume updating on skinned mesh objects in a given scenegraph.
     * 
     * @param root
     *            the root node on the scenegraph
     * @param doUpdate
     *            if true, skinned mesh objects will automatically update their model bounds when applying pose.
     */
    public static void setAutoUpdateBounds(final Spatial root, final boolean doUpdate) {
        root.acceptVisitor(new Visitor() {
            public void visit(final Spatial spatial) {
                // we only care about SkinnedMesh
                if (spatial instanceof SkinnedMesh) {
                    ((SkinnedMesh) spatial).setAutoUpdateSkinBounds(doUpdate);
                }
            }
        }, true);
    }
}
