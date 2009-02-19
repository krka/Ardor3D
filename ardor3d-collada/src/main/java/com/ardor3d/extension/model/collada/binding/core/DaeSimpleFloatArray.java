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

public class DaeSimpleFloatArray extends DaeTreeNode {
    private float[] data;

    public void setTextData(final String textData) {
        final String[] split = textData.split("\\s");

        int index = 0;
        final float[] tmpData = new float[split.length]; // over-allocating

        for (final String s : split) {
            if (s.length() > 0) {
                try {
                    tmpData[index] = Float.parseFloat(s);
                    index++;
                } catch (final NumberFormatException e) {
                    throw new ColladaException("Unable to parse int array entry '" + s + "' at index: " + index, this);
                }
            }
        }

        data = new float[index];

        System.arraycopy(tmpData, 0, data, 0, index);
    }

    public String getTextData() {
        throw new Error("NOT IMPLEMENTED");
    }

    public float[] getData() {
        return data;
    }
}
