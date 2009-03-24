/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
package com.ardor3d.extension.model.collada.binding.animation;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

/**
 * TODO: document this class!
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
public class DaeChannel extends DaeTreeNode {
    private String source;
    private String target;

    public String getTarget() {
        return target;
    }

    public String getSource() {
        return source;
    }
}
