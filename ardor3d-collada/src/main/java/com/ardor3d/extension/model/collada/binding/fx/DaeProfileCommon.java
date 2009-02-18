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

import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.DaeTreeNode;
import com.ardor3d.extension.model.collada.binding.core.DaeAsset;
import com.ardor3d.extension.model.collada.binding.core.DaeExtra;

public class DaeProfileCommon extends DaeTreeNode {
    private DaeAsset asset;
    private DaeList<DaeNewparam> newparams;
    private DaeTechnique technique;
    private DaeList<DaeExtra> extras;

    public DaeAsset getAsset() {
        return asset;
    }

    public DaeList<DaeNewparam> getNewparams() {
        return newparams;
    }

    public DaeTechnique getTechnique() {
        return technique;
    }

    public DaeList<DaeExtra> getExtras() {
        return extras;
    }
}
