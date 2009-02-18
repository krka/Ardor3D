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
import com.ardor3d.math.ColorRGBA;

public class DaeColor extends DaeTreeNode {
    private float red;
    private float green;
    private float blue;
    private float alpha; // only present in profile_COMMON context; see COLLADA spec

    public void setColor(final String colorDescription) {
        if (colorDescription == null) {
            throw new ColladaException("Null color description not allowed", this);
        }

        final String[] values = colorDescription.trim().split("\\s+");

        if (values.length < 3 || values.length > 4) {
            throw new ColladaException("Expected color definition of length 3 or 4 - got " + values.length
                    + " for description: " + colorDescription, this);
        }

        try {
            red = Float.parseFloat(values[0]);
            green = Float.parseFloat(values[1]);
            blue = Float.parseFloat(values[2]);

            if (values.length == 4) {
                alpha = Float.parseFloat(values[3]);
            } else {
                alpha = -1;
            }
        } catch (final NumberFormatException e) {
            throw new ColladaException("Unable to parse float number", this, e);
        }
    }

    public String getColor() {
        if (alpha < 0) {
            return String.format("%f %f %f", red, green, blue);
        } else {
            return String.format("%f %f %f %f", red, green, blue, alpha);
        }
    }

    public ColorRGBA asColorRGBA() {
        return new ColorRGBA(red, green, blue, alpha >= 0 ? alpha : 1.0f);
    }

    @Override
    public String toString() {
        return "Color: " + getColor();
    }
}
