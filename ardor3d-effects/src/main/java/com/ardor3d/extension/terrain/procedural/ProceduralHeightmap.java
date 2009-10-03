/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.procedural;

import com.ardor3d.extension.terrain.Heightmap;
import com.ardor3d.math.function.Function3D;

public class ProceduralHeightmap implements Heightmap {

    private final Function3D _function;

    public ProceduralHeightmap(final Function3D function) {
        _function = function;
    }

    public float getHeight(final int x, final int y) {
        return (float) _function.eval(x, y, 0);
    }

    public int getSize() {
        return -1;
    }

    public boolean isReady() {
        return true;
    }

}
