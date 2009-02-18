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

import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeAccessor extends DaeTreeNode {
    private String source;
    private int count;
    private int offset;
    private int stride;

    private DaeList<DaeParam> params;

    public String getSource() {
        return source;
    }

    public int getOffset() {
        return offset;
    }

    public int getCount() {
        return count;
    }

    public int getStride() {
        return stride;
    }

    public DaeList<DaeParam> getParams() {
        return params;
    }
}
