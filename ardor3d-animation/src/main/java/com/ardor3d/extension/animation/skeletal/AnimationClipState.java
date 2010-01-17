/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AnimationClipState {

    private boolean _active = true;
    private int _loopCount = 0;
    private double _timeScale = 1.0;
    private double _startTime = 0.0;
    private final Map<String, Object> _clipStateObjects = Maps.newHashMap();
    private List<AnimationListener> animationListeners = null;

    public void addAnimationListener(final AnimationListener animationListener) {
        if (animationListeners == null) {
            animationListeners = Lists.newArrayList();
        }
        animationListeners.add(animationListener);
    }

    public void removeAnimationListener(final AnimationListener animationListener) {
        if (animationListeners == null) {
            return;
        }
        animationListeners.remove(animationListener);
        if (animationListeners.isEmpty()) {
            animationListeners = null;
        }
    }

    /**
     * @return an immutable copy of the list of action listeners.
     */
    public List<AnimationListener> getAnimationListeners() {
        if (animationListeners == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(animationListeners);
    }

    public boolean isActive() {
        return _active;
    }

    public void setActive(final boolean active) {
        _active = active;
    }

    public int getLoopCount() {
        return _loopCount;
    }

    public void setLoopCount(final int loopCount) {
        _loopCount = loopCount;
    }

    public double getTimeScale() {
        return _timeScale;
    }

    public void setTimeScale(final double timeScale) {
        _timeScale = timeScale;
    }

    public double getStartTime() {
        return _startTime;
    }

    public void setStartTime(final double startTime) {
        _startTime = startTime;
    }

    public Object getApplyTo(final AbstractAnimationChannel<?> channel) {
        final String channelName = channel.getChannelName();
        Object rVal = _clipStateObjects.get(channelName);
        if (rVal == null) {
            rVal = channel.createStateDataObject();
            _clipStateObjects.put(channelName, rVal);
        }
        return rVal;
    }

    public Map<String, Object> getChannelData() {
        return _clipStateObjects;
    }

    public void fireAnimationFinished() {
        if (animationListeners == null) {
            return;
        }

        for (final AnimationListener animationListener : animationListeners) {
            animationListener.animationFinished();
        }
    }
}
