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

public class DaeFloat extends DaeTreeNode {
    private float value;

    public String getFloatValue() {
        return "" + value;
    }

    void setFloatValue(final String floatValue) {
        if (floatValue == null) {
            throw new ColladaException("Null value not allowed", this);
        }

        try {
            value = Float.parseFloat(floatValue);
        } catch (final NumberFormatException e) {
            throw new ColladaException("Unable to parse value '" + floatValue + "' as float", this, e);
        }
    }

    @Override
    public String toString() {
        return getFloatValue();
    }

    public float getValue() {
        return value;
    }
}
