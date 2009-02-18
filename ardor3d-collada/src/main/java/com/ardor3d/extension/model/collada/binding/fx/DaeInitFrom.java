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

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeInitFrom extends DaeTreeNode {

    public enum DaeSurfaceFace {
        POSITIVE_X, NEGATIVE_X, POSITIVE_Y, NEGATIVE_Y, POSITIVE_Z, NEGATIVE_Z;
    }

    private int mip;
    private int slice;
    private DaeSurfaceFace face;
    private String value;

    /**
     * @return the mip
     */
    public int getMip() {
        return mip;
    }

    /**
     * @return the slice
     */
    public int getSlice() {
        return slice;
    }

    /**
     * @return the face
     */
    public DaeSurfaceFace getFace() {
        return face;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }
}
