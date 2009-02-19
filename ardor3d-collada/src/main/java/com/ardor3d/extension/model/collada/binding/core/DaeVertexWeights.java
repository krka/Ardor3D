/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding.core;

import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeVertexWeights extends DaeTreeNode {
    private int count;
    private DaeList<DaeInputShared> inputs;
    private DaeSimpleIntegerArray vcount;
    private DaeSimpleIntegerArray v;

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @return the inputs
     */
    public DaeList<DaeInputShared> getInputs() {
        return inputs;
    }

    /**
     * @return the vcount
     */
    public DaeSimpleIntegerArray getVcount() {
        return vcount;
    }

    /**
     * @return the v
     */
    public DaeSimpleIntegerArray getV() {
        return v;
    }
}
