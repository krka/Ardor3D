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
import com.ardor3d.extension.model.collada.binding.core.DaeParam;

public class DaeBindMaterial extends DaeTreeNode {
    private DaeList<DaeParam> params;
    private DaeList<DaeInstanceMaterial> commonInstanceMaterials;
    private DaeList<DaeTechnique> techniques;

    public DaeList<DaeParam> getParams() {
        return params;
    }

    public DaeList<DaeInstanceMaterial> getCommonInstanceMaterials() {
        return commonInstanceMaterials;
    }

    public DaeList<DaeTechnique> getTechniques() {
        return techniques;
    }
}
