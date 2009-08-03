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

public class DaeDoubleArray extends DaeTreeNode {
    private int count;
    private double[] data;

    public void setTextData(final String textData) {
        final String[] split = textData.split("\\s");

        int index = 0;
        final double[] tmpData = new double[split.length]; // over-allocating

        for (final String s : split) {
            if (s.length() > 0) {
                try {
                    tmpData[index] = Double.parseDouble(s);
                    index++;
                } catch (final NumberFormatException e) {
                    throw new ColladaException("Unable to parse double array entry '" + s + "' at index: " + index,
                            this);
                }
            }
        }

        data = new double[index];

        System.arraycopy(tmpData, 0, data, 0, index);
    }

    public String getTextData() {
        throw new Error("NOT IMPLEMENTED");
    }

    public void validate() {
        if (count != data.length) {
            throw new ColladaException("Expected count " + count + " to be the same as data length: " + data.length,
                    this);
        }
    }

    public int getCount() {
        return count;
    }

    public double[] getData() {
        return data;
    }
}