/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.basic;

import com.ardor3d.extension.terrain.Heightmap;
import com.ardor3d.math.MathUtils;

public class BasicHeightmap implements Heightmap {
    protected int size;
    protected float[] heightData;

    public BasicHeightmap() {
        size = 1;
        heightData = new float[1];
    }

    public BasicHeightmap(final int size) {
        this.size = size;
        heightData = new float[size * size];
    }

    public BasicHeightmap(final float[] heightData, final int size) {
        this.heightData = heightData;
        this.size = size;
    }

    // interface

    public float getHeight(final int x, final int y) {
        return heightData[y * size + x];
    }

    public int getSize() {
        return size;
    }

    public boolean isReady() {
        return heightData != null;
    }

    // class

    public void setHeightData(final float[] heightData, final int size) {
        this.heightData = heightData;
        this.size = size;
    }

    public void setHeight(int x, int y, final float height) {
        x = MathUtils.moduloPositive(x, size);
        y = MathUtils.moduloPositive(y, size);
        heightData[y * size + x] = height;
    }
}
