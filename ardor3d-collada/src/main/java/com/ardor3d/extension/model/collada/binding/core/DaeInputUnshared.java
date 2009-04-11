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

public class DaeInputUnshared extends DaeTreeNode implements DaeInput {
    private String source;
    private String semantic;

    /**
     * Constructor for use by JiBX.
     */
    public DaeInputUnshared() {
    }

    /**
     * Constructor for use with test code - normally, these classes are instantiated by JiBX only.
     *
     */
    public DaeInputUnshared(String source, String semantic, DaeTreeNode parent) {
        this.source = source;
        this.semantic = semantic;

        registerParent(parent);
    }

    public String getSource() {
        return source;
    }

    public String getSemantic() {
        return semantic;
    }
}
