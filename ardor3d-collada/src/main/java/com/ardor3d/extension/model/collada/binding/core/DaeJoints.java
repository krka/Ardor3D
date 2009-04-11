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

public class DaeJoints extends DaeTreeNode {
    private DaeList<DaeInputUnshared> inputs;

    /**
     * Constructor for use by JiBX.
     */
    public DaeJoints() {
    }

    /**
     * Constructor for use with test code - normally, these classes are instantiated by JiBX only.
     *
     */
    public DaeJoints(DaeList<DaeInputUnshared> inputs) {
        this.inputs = inputs;
    }

    /**
     * @return the inputs
     */
    public DaeList<DaeInputUnshared> getInputs() {
        return inputs;
    }
}
