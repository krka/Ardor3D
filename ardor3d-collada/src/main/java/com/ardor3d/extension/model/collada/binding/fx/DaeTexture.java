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

public class DaeTexture extends DaeTreeNode {
    private String texture;
    private String texcoord;

    /**
     * @return the texture
     */
    public String getTexture() {
        return texture;
    }

    /**
     * @return the texcoord
     */
    public String getTexcoord() {
        return texcoord;
    }
}
