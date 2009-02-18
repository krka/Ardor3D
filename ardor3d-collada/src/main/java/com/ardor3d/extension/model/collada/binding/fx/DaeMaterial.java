/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding.fx;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeMaterial extends DaeTreeNode {
    private DaeInstanceEffect instanceEffect;

    @Override
    public String toString() {
        return "Material: " + idToString();
    }

    /**
     * @return the instanceEffect
     */
    public DaeInstanceEffect getInstanceEffect() {
        return instanceEffect;
    }
}
