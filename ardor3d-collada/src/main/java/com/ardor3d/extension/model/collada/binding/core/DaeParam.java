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

import com.ardor3d.extension.model.collada.binding.ColladaException;
import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeParam extends DaeTreeNode {
    private DaeParamType type;

    /**
     * @return the type
     */
    public DaeParamType getType() {
        return type;
    }

    public void setTypeText(final String typeName) {
        try {
            type = DaeParamType.valueOf(typeName.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new ColladaException("Unable to set param type to: " + typeName, this);
        }
    }

    public String getTypeText() {
        return type.toString();
    }
}
