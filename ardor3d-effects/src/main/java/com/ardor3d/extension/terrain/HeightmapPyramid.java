/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain;

/**
 * HeightmapPyramid interface.
 */
public interface HeightmapPyramid {
    /**
     * Gets the height at a specific coordinate in a heightmap.
     * 
     * @param level
     *            target heightmap level
     * @param x
     * @param y
     * @return
     */
    float getHeight(final int level, final int x, final int y);

    /**
     * Gets the size of the heightmap (the side).
     * 
     * @param level
     *            target heightmap level
     * @return
     */
    int getSize(final int level);

    /**
     * Get number of heightmaps maintained by this heightmap pyramid
     * 
     * @return
     */
    int getHeightmapCount();

    /**
     * Signals if a specific heightmap is ready to provide data
     * 
     * @param levelIndex
     * @return
     */
    boolean isReady(int levelIndex);
}