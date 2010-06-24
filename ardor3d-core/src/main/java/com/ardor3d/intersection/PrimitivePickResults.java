/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.intersection;

import com.ardor3d.math.Ray3;

/**
 * PrimitivePickResults implements the addPick of PickResults to use PickData objects that calculate primitive accurate
 * ray picks.
 */
public class PrimitivePickResults extends PickResults {
    @Override
    public void addPick(final Ray3 ray, final Pickable pickable) {
        if (pickable.intersectsWorldBound(ray)) {
            final PrimitivePickData data = new PrimitivePickData(ray, pickable);
            if (data.getIntersectionRecord() != null && data.getIntersectionRecord().getNumberOfIntersections() > 0) {
                addPickData(data);
            }
        }
    }
}