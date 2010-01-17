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

import com.ardor3d.extension.animation.skeletal.AnimationClip;
import com.google.common.collect.Lists;

public class AnimationItem {
    private final String name;
    private final List<AnimationItem> children = Lists.newArrayList();
    private AnimationClip animationClip;

    public AnimationItem(final String name) {
        this.name = name;
    }

    public AnimationClip getAnimationClip() {
        return animationClip;
    }

    public void setAnimationClip(final AnimationClip animationClip) {
        this.animationClip = animationClip;
    }

    public String getName() {
        return name;
    }

    public List<AnimationItem> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return "AnimationItem [name=" + name + (animationClip != null ? ", " + animationClip.toString() : "") + "]";
    }
}
