/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.array;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ArrayTerrainDataProvider implements TerrainDataProvider {
    private static final int tileSize = 128;

    private final List<float[]> heightMaps;
    private final List<Integer> heightMapSizes;
    private final ReadOnlyVector3 scale;

    public ArrayTerrainDataProvider(final float[] data, final int size, final ReadOnlyVector3 scale) {
        this.scale = scale;

        // TODO: calculate clipLevelCount through size and tileSize
        final int clipLevelCount = 6;

        int currentSize = size;
        heightMaps = Lists.newArrayList();
        heightMapSizes = Lists.newArrayList();
        heightMaps.add(data);
        heightMapSizes.add(currentSize);
        float[] parentHeightMap = data;
        for (int i = 0; i < clipLevelCount; i++) {
            currentSize /= 2;
            final float[] heightMapMip = new float[currentSize * currentSize];
            heightMaps.add(heightMapMip);
            heightMapSizes.add(currentSize);
            for (int x = 0; x < currentSize; x++) {
                for (int z = 0; z < currentSize; z++) {
                    heightMapMip[z * currentSize + x] = parentHeightMap[z * currentSize * 4 + x * 2];
                }
            }
            parentHeightMap = heightMapMip;
        }

        Collections.reverse(heightMaps);
        Collections.reverse(heightMapSizes);
    }

    @Override
    public Map<Integer, String> getAvailableMaps() throws Exception {
        final Map<Integer, String> maps = Maps.newHashMap();
        maps.put(0, "ArrayBasedMap");

        return maps;
    }

    @Override
    public TerrainSource getTerrainSource(final int mapId) {
        return new ArrayTerrainSource(tileSize, heightMaps, heightMapSizes, scale);
    }

    @Override
    public TextureSource getTextureSource(final int mapId) {
        return new ArrayTextureSource(tileSize, heightMaps, heightMapSizes);
    }

}
