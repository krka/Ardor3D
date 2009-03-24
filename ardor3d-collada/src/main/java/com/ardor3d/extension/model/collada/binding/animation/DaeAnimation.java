/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
package com.ardor3d.extension.model.collada.binding.animation;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;
import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.fx.DaeSampler;
import com.ardor3d.extension.model.collada.binding.core.DaeAsset;
import com.ardor3d.extension.model.collada.binding.core.DaeSource;

/**
 * TODO: document this class!
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
public class DaeAnimation extends DaeTreeNode {
    private DaeAsset asset;
    private DaeList<DaeAnimation> animations;
    private DaeList<DaeSource> sources;
    private DaeList<DaeSampler> samplers;
    private DaeList<DaeChannel> channels;

    public DaeAsset getAsset() {
        return asset;
    }

    public DaeList<DaeAnimation> getAnimations() {
        return animations;
    }

    public DaeList<DaeSource> getSources() {
        return sources;
    }

    public DaeList<DaeSampler> getSamplers() {
        return samplers;
    }

    public DaeList<DaeChannel> getChannels() {
        return channels;
    }
}
