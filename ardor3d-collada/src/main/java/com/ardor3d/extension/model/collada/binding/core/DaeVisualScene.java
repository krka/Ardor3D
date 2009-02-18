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


public class DaeVisualScene extends DaeTreeNode {

    public static final String COLLADA_ROOT_NAME = "collada root";

    private DaeAsset asset;
    private DaeList<DaeNode> nodes;

    public DaeAsset getAsset() {
        return asset;
    }

    public DaeList<DaeNode> getNodes() {
        return nodes;
    }
}
