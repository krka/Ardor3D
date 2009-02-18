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

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeInputShared extends DaeTreeNode {
    private int offset;
    private String source;
    private String semantic;
    private int set;

    public int getOffset() {
        return offset;
    }

    public String getSource() {
        return source;
    }

    public String getSemantic() {
        return semantic;
    }

    public int getSet() {
        return set;
    }
}
