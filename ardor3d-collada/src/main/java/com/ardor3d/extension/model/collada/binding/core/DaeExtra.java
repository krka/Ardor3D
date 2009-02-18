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

import java.util.ArrayList;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeExtra extends DaeTreeNode {
    private String type;
    private ArrayList<org.w3c.dom.Element> techniques;

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the techniques
     */
    public ArrayList<org.w3c.dom.Element> getTechniques() {
        return techniques;
    }
}
