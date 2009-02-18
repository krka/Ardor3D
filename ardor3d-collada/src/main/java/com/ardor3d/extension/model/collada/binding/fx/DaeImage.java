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

public class DaeImage extends DaeTreeNode {
    private String format;
    private int height;
    private int width;
    private int depth;
    private String data;
    private String initFrom;

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * @return the initFrom
     */
    public String getInitFrom() {
        return initFrom;
    }
}
