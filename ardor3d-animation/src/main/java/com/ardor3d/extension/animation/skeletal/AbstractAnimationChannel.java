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

public abstract class AbstractAnimationChannel<T> {

    // XXX: Maybe generics are a waste here?

    private final String _channelName;
    private final float[] _times;

    public AbstractAnimationChannel(final String channelName, final float[] times) {
        _channelName = channelName;
        _times = times;
    }

    public String getChannelName() {
        return _channelName;
    }

    @SuppressWarnings("unchecked")
    public void updateSample(final double clockTime, final Object applyTo) {
        // figure out what frames we are between and by how much
        final int lastFrame = _times.length - 1;
        if (clockTime < 0 || _times.length == 1) {
            setCurrentSample(0, (T) applyTo);
        } else if (clockTime >= _times[lastFrame]) {
            setCurrentSample(lastFrame, (T) applyTo);
        } else {
            int startFrame = 0;

            for (int i = 0; i < _times.length - 1; i++) {
                if (_times[i] < clockTime) {
                    startFrame = i;
                }
            }
            final double progressPercent = (clockTime - _times[startFrame])
                    / (_times[startFrame + 1] - _times[startFrame]);

            setCurrentSample(startFrame, progressPercent, (T) applyTo);
        }
    }

    public abstract void setCurrentSample(int sampleIndex, T applyTo);

    public abstract void setCurrentSample(int sampleIndex, double progressPercent, T applyTo);

    public abstract T createStateDataObject();

    public double getMaxTime() {
        return _times[_times.length - 1];
    }
}
