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

public interface HeightmapPyramid {

    float getHeight(final int level, final int x, final int y);

    int getSize(final int level);

    int getHeightmapCount();

    boolean isReady(int levelIndex);
}