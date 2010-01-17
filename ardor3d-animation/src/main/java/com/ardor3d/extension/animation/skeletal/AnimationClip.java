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

import com.google.common.collect.Lists;

public class AnimationClip {

    private final List<AbstractAnimationChannel<?>> _channels;
    private double _maxTime = 0;

    public AnimationClip() {
        _channels = Lists.newArrayList();
    }

    public AnimationClip(final List<AbstractAnimationChannel<?>> channels) {
        _channels = channels;
        updateMaxTimeIndex();
    }

    public void update(final double globalTime, final AnimationClipState clipState) {
        // Go through each channel and update clipState
        for (final AbstractAnimationChannel<?> channel : _channels) {
            final Object applyTo = clipState.getApplyTo(channel);
            channel.updateSample(globalTime, applyTo);
        }
    }

    public void addChannel(final AbstractAnimationChannel<?> channel) {
        _channels.add(channel);
        updateMaxTimeIndex();
    }

    public AbstractAnimationChannel<?> findChannelByName(final String channelName) {
        for (final AbstractAnimationChannel<?> channel : _channels) {
            if (channelName.equals(channel.getChannelName())) {
                return channel;
            }
        }
        return null;
    }

    public boolean removeChannel(final AbstractAnimationChannel<?> channel) {
        final boolean rVal = _channels.remove(channel);
        updateMaxTimeIndex();
        return rVal;
    }

    public double getMaxTimeIndex() {
        return _maxTime;
    }

    private void updateMaxTimeIndex() {
        _maxTime = 0;
        double max;
        for (final AbstractAnimationChannel<?> channel : _channels) {
            max = channel.getMaxTime();
            if (max > _maxTime) {
                _maxTime = max;
            }
        }
    }

    @Override
    public String toString() {
        return "AnimationClip [channel count=" + _channels.size() + ", max time=" + _maxTime + "]";
    }
}
