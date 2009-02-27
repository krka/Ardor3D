/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.intersection;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.Ray3;
import com.ardor3d.scenegraph.Mesh;

/**
 * PrimitivePickResults creates a PickResults object that calculates picking to the primitive accuracy. PickData objects
 * are added to the pick list as they happen, these data objects refer to the two meshes, as well as their primitive
 * lists. While PrimitivePickResults defines a processPick method, it is empty and should be further defined by the user
 * if so desired.
 * 
 * NOTE: Only Mesh objects may obtain primitive accuracy, all others will result in Bounding accuracy.
 */
public class PrimitivePickResults extends PickResults {

    /**
     * <code>addPick</code> adds a Mesh object to the pick list.
     * 
     * @param ray
     *            the ray that is doing the picking.
     * @param mesh
     *            the Mesh to add to the pick list.
     */
    @Override
    public void addPick(final Ray3 ray, final Mesh mesh) {
        // find the primitive that is being hit. Add this node and the primitive to the CollisionResults list.
        final List<PrimitiveKey> resultTriangles = new ArrayList<PrimitiveKey>();
        PickingUtil.findTrianglePick(mesh, ray, resultTriangles);
        final PickData data = new PrimitivePickData(ray, mesh, resultTriangles, willCheckDistance());
        addPickData(data);
    }

    /**
     * <code>processPick</code> will handle processing of the pick list. This is very application specific and therefore
     * left as an empty method. Applications wanting an automated picking system should extend PrimitivePickResults and
     * override this method.
     * 
     * @see com.ardor3d.intersection.PickResults#processPick()
     */
    @Override
    public void processPick() {

    }

}