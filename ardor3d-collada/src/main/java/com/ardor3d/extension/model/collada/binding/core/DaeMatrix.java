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

public class DaeMatrix extends DaeTransform {
    private String value;

    public double[] getDoubleValues() {
        if (value != null) {
            final String[] split = value.split("\\s");

            int index = 0;
            final double[] tmpData = new double[split.length]; // over-allocating

            for (final String s : split) {
                if (s.length() > 0) {
                    try {
                        tmpData[index] = Float.parseFloat(s);
                        index++;
                    } catch (final NumberFormatException e) {
                        throw new ColladaException("Unable to parse float array entry '" + s + "' at index: " + index,
                                this);
                    }
                }
            }

            final double[] data = new double[index];

            System.arraycopy(tmpData, 0, data, 0, index);
            return data;
        }
        return null;
    }
}
