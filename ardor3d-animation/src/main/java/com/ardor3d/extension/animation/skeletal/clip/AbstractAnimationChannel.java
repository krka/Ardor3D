/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.clip;

import java.io.IOException;
import java.lang.reflect.Field;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Base class for animation channels. An animation channel describes a single element of an animation (such as the
 * movement of a single joint, or the play back of a specific sound, etc.) These channels are grouped together in an
 * AnimationClip to describe a full animation.
 */
public abstract class AbstractAnimationChannel implements Savable {

    /** The name of this channel. */
    protected final String _channelName;

    /** Our time indices. Each index of the array should contain a value that is > the value in the previous index. */
    protected final float[] _times;

    /**
     * Construct a new channel.
     * 
     * @param channelName
     *            the name of our channel. This is immutable to this instance of the class.
     * @param times
     *            our time indices. Copied into the channel.
     */
    public AbstractAnimationChannel(final String channelName, final float[] times) {
        _channelName = channelName;
        _times = times == null ? null : new float[times.length];
        if (_times != null) {
            System.arraycopy(times, 0, _times, 0, times.length);
        }
    }

    public String getChannelName() {
        return _channelName;
    }

    /**
     * Update the given applyTo object with information from this channel at the given time position.
     * 
     * @param clockTime
     *            the current local clip time (where 0 == start of clip)
     * @param applyTo
     *            the Object to apply to. The type of the object and what data is set will depend on the Channel
     *            subclass.
     */
    public void updateSample(final double clockTime, final Object applyTo) {
        // figure out what frames we are between and by how much
        final int lastFrame = _times.length - 1;
        if (clockTime < 0 || _times.length == 1) {
            setCurrentSample(0, 0.0, applyTo);
        } else if (clockTime >= _times[lastFrame]) {
            setCurrentSample(lastFrame, 0.0, applyTo);
        } else {
            int startFrame = 0;

            for (int i = 0; i < _times.length - 1; i++) {
                if (_times[i] < clockTime) {
                    startFrame = i;
                }
            }
            final double progressPercent = (clockTime - _times[startFrame])
                    / (_times[startFrame + 1] - _times[startFrame]);

            setCurrentSample(startFrame, progressPercent, applyTo);
        }
    }

    /**
     * Sets data on the given applyTo Object for the given sampleIndex and a percent progress towards the sample
     * following it.
     * 
     * @param sampleIndex
     *            the sample to pull information from.
     * @param progressPercent
     *            a value [0.0, 1.0] representing progress from sampleIndex to sampleIndex+1
     * @param applyTo
     *            the data object to apply this channel's information to.
     */
    public abstract void setCurrentSample(int sampleIndex, double progressPercent, Object applyTo);

    /**
     * @return an Object suitable for storing information for this type of animation channel.
     */
    public abstract Object createStateDataObject();

    /**
     * @return the local time index of the last sample in this channel.
     */
    public float getMaxTime() {
        return _times[_times.length - 1];
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_channelName, "channelName", null);
        capsule.write(_times, "times", null);
    }

    public void read(final InputCapsule capsule) throws IOException {
        final String channelName = capsule.readString("channelName", null);
        final float[] times = capsule.readFloatArray("times", null);
        try {
            final Field field1 = AbstractAnimationChannel.class.getDeclaredField("_channelName");
            field1.setAccessible(true);
            field1.set(this, channelName);

            final Field field2 = AbstractAnimationChannel.class.getDeclaredField("_times");
            field2.setAccessible(true);
            field2.set(this, times);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
