/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.util.List;

import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.google.common.collect.Lists;

public class AnimationItem {
    private final String _name;
    private final List<AnimationItem> _children = Lists.newArrayList();
    private AnimationClip _animationClip;

    public AnimationItem(final String name) {
        this._name = name;
    }

    public AnimationClip getAnimationClip() {
        return _animationClip;
    }

    public void setAnimationClip(final AnimationClip animationClip) {
        this._animationClip = animationClip;
    }

    public String getName() {
        return _name;
    }

    public List<AnimationItem> getChildren() {
        return _children;
    }

    @Override
    public String toString() {
        return "AnimationItem [name=" + _name + (_animationClip != null ? ", " + _animationClip.toString() : "") + "]";
    }
}
