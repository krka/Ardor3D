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

import java.io.IOException;
import java.lang.reflect.Field;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public abstract class AbstractAnimationChannel<T> implements Savable {

    // XXX: Maybe generics are a waste here?

    protected final String _channelName;
    protected final float[] _times;

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
            setCurrentSample(0, 0.0, (T) applyTo);
        } else if (clockTime >= _times[lastFrame]) {
            setCurrentSample(lastFrame, 0.0, (T) applyTo);
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

    public abstract void setCurrentSample(int sampleIndex, double progressPercent, T applyTo);

    public abstract T createStateDataObject();

    public double getMaxTime() {
        return _times[_times.length - 1];
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_channelName, "rotation", null);
        capsule.write(_times, "scale", null);
    }

    public void read(final InputCapsule capsule) throws IOException {
        final String channelName = capsule.readString("channelName", null);
        final float[] times = capsule.readFloatArray("times", null);
        try {
            final Field field1 = this.getClass().getDeclaredField("_channelName");
            field1.setAccessible(true);
            field1.set(this, channelName);

            final Field field2 = this.getClass().getDeclaredField("_times");
            field2.setAccessible(true);
            field2.set(this, times);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
