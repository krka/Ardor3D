/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import com.ardor3d.scenegraph.Mesh;

/**
 * A data object matching an Ardor3D Mesh object to the vertex indices it uses from the original <mesh><vertices>
 * Collada data. This allows us to match weights, joint and such that are indexed to the original vertex positions to
 * their final positions in the Mesh.
 */
public class MeshVertPairs {
    /**
     * The referenced Ardor3D Mesh.
     */
    public final Mesh mesh;

    /**
     * The Collada indices. This array should be as big as the vertex count of the Mesh.
     */
    public final int[] indices;

    public MeshVertPairs(final Mesh mesh, final int[] indices) {
        this.mesh = mesh;
        this.indices = indices;
    }
}
