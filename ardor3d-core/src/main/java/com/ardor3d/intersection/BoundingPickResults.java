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

import com.ardor3d.math.Ray3;
import com.ardor3d.scenegraph.Mesh;

/**
 * BoundingPickResults creates a PickResults object that only cares about bounding volume accuracy. PickData objects are
 * added to the pick list as they happen, these data objects only refer to the two meshes, not their triangle lists.
 * While BoundingPickResults defines a processPick method, it is empty and should be further defined by the user if so
 * desired.
 */
public class BoundingPickResults extends PickResults {

    /**
     * adds a PickData object to this results list, the objects only refer to the picked meshes, not the triangles.
     */
    @Override
    public void addPick(final Ray3 ray, final Mesh g) {
        final PickData data = new PickData(ray, g, willCheckDistance());
        addPickData(data);
    }

    /**
     * empty implementation, it is highly recommended that you override this method to handle any picks as needed.
     * 
     * @see com.ardor3d.intersection.PickResults#processPick()
     */
    @Override
    public void processPick() {

    }

}
