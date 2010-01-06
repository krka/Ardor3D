/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.streaming;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.terrain.Heightmap;
import com.ardor3d.extension.terrain.HeightmapPyramid;

/**
 * TODO: Yet to be implemented
 */
public class StreamingHeightmapPyramid implements HeightmapPyramid {
    private final List<Heightmap> heightmaps = new ArrayList<Heightmap>();

    // Interface

    public float getHeight(final int level, final int x, final int y) {
        return 0;
    }

    public int getSize(final int level) {
        return 0;
    }

    public int getHeightmapCount() {
        return 0;
    }

    public boolean isReady(final int levelIndex) {
        return true;
    }

    // Class

    class HeightmapBlock {
        public float[] heightData;
    }

    int gridCount = 3;
    int gridSize = 32; // clipmap size + margin
    HeightmapBlock[][] gridMap = new HeightmapBlock[3][3];
    int xOffset = 0;
    int zOffset = 0;

    public void setPosition(final int x, final int z) {
        final int xpos = x / gridSize;
        final int zpos = z / gridSize;
        for (int xx = xpos - 1; xx <= xpos + 1; xx++) {
            for (int zz = zpos - 1; zz <= zpos + 1; zz++) {
            }
        }
    }
}
