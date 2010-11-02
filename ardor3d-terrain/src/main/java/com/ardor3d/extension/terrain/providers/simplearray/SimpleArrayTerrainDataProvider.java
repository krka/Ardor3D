/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.simplearray;

import java.util.Map;

import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.google.common.collect.Maps;

public class SimpleArrayTerrainDataProvider implements TerrainDataProvider {
    private static final int tileSize = 128;

    private final float[] heightData;
    private final byte[] colorData;
    private final int side;

    public SimpleArrayTerrainDataProvider(final float[] heightData, final byte[] colorData, final int side) {
        this.heightData = heightData;
        this.colorData = colorData;
        this.side = side;
    }

    @Override
    public Map<Integer, String> getAvailableMaps() throws Exception {
        final Map<Integer, String> maps = Maps.newHashMap();
        maps.put(0, "InMemoryData");

        return maps;
    }

    @Override
    public TerrainSource getTerrainSource(final int mapId) {
        return new SimpleArrayTerrainSource(tileSize, heightData, side);
    }

    @Override
    public TextureSource getTextureSource(final int mapId) {
        return new SimpleArrayTextureSource(tileSize, colorData, side);
    }

}
