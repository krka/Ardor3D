/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.basic;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.terrain.Heightmap;
import com.ardor3d.extension.terrain.HeightmapPyramid;

public class BasicHeightmapPyramid implements HeightmapPyramid {
    private final List<Heightmap> heightmaps = new ArrayList<Heightmap>();

    public BasicHeightmapPyramid() {}

    public BasicHeightmapPyramid(final Heightmap baseHeightmap, final int clipLevelCount) {
        heightmaps.add(0, baseHeightmap);
        buildLevels(clipLevelCount);
    }

    // interface

    public float getHeight(final int level, final int x, final int y) {
        return getHeightmap(level).getHeight(x, y);
    }

    public int getSize(final int level) {
        return getHeightmap(level).getSize();
    }

    public int getHeightmapCount() {
        return heightmaps.size();
    }

    public boolean isReady(final int level) {
        return getHeightmap(level).isReady();
    }

    // class

    public List<Heightmap> getHeightmaps() {
        return heightmaps;
    }

    private Heightmap getHeightmap(int level) {
        level = Math.min(heightmaps.size() - 1, level);
        return heightmaps.get(level);
    }

    public void buildLevels(final int levels) {
        if (heightmaps.isEmpty()) {
            return;
        }

        final int baseSize = heightmaps.get(0).getSize();
        System.out.println("currentSize: " + baseSize);
        for (int i = 1; i < levels; i++) {
            final Heightmap parentHeightmap = heightmaps.get(i - 1);
            final int currentSize = (int) (baseSize / Math.pow(2, i));

            final BasicHeightmap heightmap = new BasicHeightmap(currentSize);
            heightmaps.add(heightmap);

            System.out.println("currentSize: " + currentSize);

            for (int x = 0; x < currentSize; x++) {
                for (int y = 0; y < currentSize; y++) {
                    heightmap.setHeight(x, y, parentHeightmap.getHeight(x * 2, y * 2));
                }
            }

        }
    }
}
