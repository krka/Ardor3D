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

import java.util.ArrayList;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeInstanceMaterial extends DaeTreeNode {
    private String target;
    private String symbol;
    private ArrayList<DaeBind> binds;
    private ArrayList<DaeBindVertexInput> bindVertexInputs;

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * @return the symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * @return the binds
     */
    public ArrayList<DaeBind> getBinds() {
        return binds;
    }

    /**
     * @return the bindVertexInputs
     */
    public ArrayList<DaeBindVertexInput> getBindVertexInputs() {
        return bindVertexInputs;
    }
}
