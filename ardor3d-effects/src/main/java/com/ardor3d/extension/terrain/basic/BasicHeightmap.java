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

import com.ardor3d.extension.terrain.Heightmap;

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

    public float getHeight(int x, int y) {
        x = wrap(x, size);
        y = wrap(y, size);
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
        x = wrap(x, size);
        y = wrap(y, size);
        heightData[y * size + x] = height;
    }

    private int wrap(final int value, final int size) {
        int wrappedValue = value % size;
        wrappedValue += wrappedValue < 0 ? size : 0;
        return wrappedValue;
    }
}
