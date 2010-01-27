/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.blendtree;

import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationClip;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.google.common.collect.ImmutableMap;

public class ClipSource implements BlendTreeSource {

    private AnimationClip _clip;
    private AnimationManager _manager;

    public ClipSource() {}

    public ClipSource(final AnimationClip clip, final AnimationManager manager) {
        setClip(clip);
        setManager(manager);
    }

    public AnimationClip getClip() {
        return _clip;
    }

    public AnimationManager getManager() {
        return _manager;
    }

    public void setClip(final AnimationClip clip) {
        _clip = clip;
    }

    public void setManager(final AnimationManager manager) {
        _manager = manager;
    }

    public Map<String, Object> getSourceData() {
        return ImmutableMap.copyOf(getManager().getClipState(getClip()).getChannelData());
    }

}
