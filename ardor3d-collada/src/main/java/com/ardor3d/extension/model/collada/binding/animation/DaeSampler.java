/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
package com.ardor3d.extension.model.collada.binding.animation;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;
import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.core.DaeInputUnshared;

/**
 * TODO: document this class!
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
public class DaeSampler extends DaeTreeNode {
    private DaeList<DaeInputUnshared> inputs;

    public DaeList<DaeInputUnshared> getInputs() {
        return inputs;
    }
}
