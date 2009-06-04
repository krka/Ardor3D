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
 * Heightmap interface.
 */
public interface Heightmap {
    /**
     * Gets the height at a specific coordinate in the heightmap.
     * 
     * @param x
     *            the x
     * @param y
     *            the y
     * 
     * @return the height
     */
    float getHeight(int x, int y);

    /**
     * Gets the size of the heightmap (the side).
     * 
     * @return the size
     */
    int getSize();

    /**
     * Signals if the heightmap is ready to provide data
     * 
     * @return
     */
    boolean isReady();
}