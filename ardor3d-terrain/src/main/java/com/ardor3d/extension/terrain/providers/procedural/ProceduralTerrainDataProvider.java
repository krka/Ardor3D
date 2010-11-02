/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.procedural;

import java.util.Map;

import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.google.common.collect.Maps;

public class ProceduralTerrainDataProvider implements TerrainDataProvider {
    private final Function3D function;
    private final ReadOnlyVector3 scale;
    private final float minHeight;
    private final float maxHeight;

    public ProceduralTerrainDataProvider(final Function3D function, final ReadOnlyVector3 scale, final float minHeight,
            final float maxHeight) {
        this.function = function;
        this.scale = scale;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    @Override
    public Map<Integer, String> getAvailableMaps() throws Exception {
        final Map<Integer, String> maps = Maps.newHashMap();
        maps.put(0, "ProceduralMap");

        return maps;
    }

    @Override
    public TerrainSource getTerrainSource(final int mapId) {
        return new ProceduralTerrainSource(function, scale, minHeight, maxHeight);
    }

    @Override
    public TextureSource getTextureSource(final int mapId) {
        return new ProceduralTextureSource(function);
    }

}
