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

public class DaePolylist extends DaeTreeNode {
    private DaeList<DaeInputShared> inputs;
    private DaeSimpleIntegerArray pEntry;
    private DaeSimpleIntegerArray vCount;
    private int count;
    private String material;

    public DaeList<DaeInputShared> getInputs() {
        return inputs;
    }

    public DaeSimpleIntegerArray getPEntry() {
        return pEntry;
    }

    public DaeSimpleIntegerArray getVCount() {
        return vCount;
    }

    public int getCount() {
        return count;
    }

    public String getMaterial() {
        return material;
    }
}
