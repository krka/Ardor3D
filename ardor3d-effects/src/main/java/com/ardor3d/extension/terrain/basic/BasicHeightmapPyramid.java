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
import java.util.logging.Logger;

import com.ardor3d.extension.terrain.Heightmap;
import com.ardor3d.extension.terrain.HeightmapPyramid;
import com.ardor3d.math.MathUtils;

public class BasicHeightmapPyramid implements HeightmapPyramid {
    private static final Logger logger = Logger.getLogger(BasicHeightmapPyramid.class.getName());

    private final List<BasicHeightmap> heightmaps = new ArrayList<BasicHeightmap>();
    private boolean doWrap = true;

    public BasicHeightmapPyramid() {}

    public BasicHeightmapPyramid(final BasicHeightmap baseHeightmap, final int clipLevelCount) {
        heightmaps.add(0, baseHeightmap);
        buildLevels(clipLevelCount);
    }

    // interface

    public float getHeight(final int level, int x, int y) {
        final Heightmap heightmap = getHeightmap(level);
        final int size = heightmap.getSize();

        if (doWrap) {
            x = MathUtils.moduloPositive(x, size);
            y = MathUtils.moduloPositive(y, size);
        } else if (x < 0 || x >= size || y < 0 || y >= size) {
            return -Float.MAX_VALUE;
        }

        return heightmap.getHeight(x, y);
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

    public List<BasicHeightmap> getHeightmaps() {
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
        logger.info("Original heightmap size: " + baseSize);

        for (int i = 1; i < levels; i++) {
            final Heightmap parentHeightmap = heightmaps.get(i - 1);
            final int currentSize = (int) (baseSize / Math.pow(2, i));

            BasicHeightmap heightmap = null;
            if (i >= heightmaps.size()) {
                heightmap = new BasicHeightmap(currentSize);
                heightmaps.add(heightmap);
            } else {
                heightmap = heightmaps.get(i);
            }

            logger.info("Building heightmap mipmap of size: " + currentSize);

            for (int x = 0; x < currentSize; x++) {
                for (int y = 0; y < currentSize; y++) {
                    heightmap.setHeight(x, y, parentHeightmap.getHeight(x * 2, y * 2));
                }
            }

        }
    }

    public boolean isDoWrap() {
        return doWrap;
    }

    public void setDoWrap(final boolean doWrap) {
        this.doWrap = doWrap;
    }
}
