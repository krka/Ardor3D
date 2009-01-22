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

import com.ardor3d.scenegraph.Mesh;

public interface ShadowGate {

    /**
     * Give a hint to the shadow render pass as to whether a given triangle mesh is a candidate for shadow updates. This
     * hint will be combined with other hints such as locking.
     * 
     * @param mesh
     *            the mesh to check
     * @return true if we think this mesh's shadows are ok to test for an update.
     */
    boolean shouldUpdateShadows(Mesh mesh);

    /**
     * Give a hint to the shadow render pass as to whether a given triangle mesh is a candidate for shadow drawing. This
     * hint will be combined with other hints such as locking.
     * 
     * @param mesh
     *            the mesh to check
     * @return true if we think this mesh's shadows are ok to draw
     */
    boolean shouldDrawShadows(Mesh mesh);

}
