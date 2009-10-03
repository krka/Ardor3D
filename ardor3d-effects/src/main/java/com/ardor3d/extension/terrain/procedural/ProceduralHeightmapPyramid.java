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

import com.ardor3d.extension.terrain.HeightmapPyramid;

public class ProceduralHeightmapPyramid implements HeightmapPyramid {
    private final ProceduralHeightmap _heightMap;
    private final int _levelCount;

    public ProceduralHeightmapPyramid(final ProceduralHeightmap baseHeightmap, final int clipLevelCount) {
        _heightMap = baseHeightmap;
        _levelCount = clipLevelCount;
    }

    public float getHeight(final int level, final int x, final int y) {
        return _heightMap.getHeight(x * 1 << level, y * 1 << level);
    }

    public int getSize(final int level) {
        return -1;
    }

    public int getHeightmapCount() {
        return _levelCount;
    }

    public boolean isReady(final int level) {
        return true;
    }
}
