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
import java.util.Arrays;
import java.util.List;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.google.common.collect.Lists;

/**
 * An animation source channel consisting of keyword samples indicating when a specific trigger condition is met. Each
 * channel can only be in one keyword "state" at a given moment in time.
 */
public class TriggerChannel extends AbstractAnimationChannel {

    /** Our key samples. */
    private final String[] _keys;

    /**
     * Construct a new TriggerChannel.
     * 
     * @param channelName
     *            the name of this channel.
     * @param times
     *            the time samples
     * @param keys
     *            our key samples. Entries may be null.
     */
    public TriggerChannel(final String channelName, final float[] times, final String[] keys) {
        super(channelName, times);

        _keys = Arrays.copyOf(keys, keys.length);
    }

    @Override
    public TriggerData createStateDataObject() {
        return new TriggerData();
    }

    @Override
    public void setCurrentSample(final int sampleIndex, final double progressPercent, final Object applyTo) {
        final TriggerData triggerData = (TriggerData) applyTo;

        // set key
        final int index = progressPercent != 1.0 ? sampleIndex : sampleIndex + 1;
        triggerData.arm(_keys[index], index);
    }

    @Override
    public AbstractAnimationChannel getSubchannelBySample(final String name, final int startSample, final int endSample) {
        if (startSample > endSample) {
            throw new IllegalArgumentException("startSample > endSample");
        }
        if (endSample >= getSampleCount()) {
            throw new IllegalArgumentException("endSample >= getSampleCount()");
        }

        final int samples = endSample - startSample + 1;
        final float[] times = new float[samples];
        final String[] keys = new String[samples];

        for (int i = 0; i <= samples; i++) {
            times[i] = _times[i + startSample];
            keys[i] = _keys[i + startSample];
        }

        return new TriggerChannel(name, times, keys);
    }

    @Override
    public AbstractAnimationChannel getSubchannelByTime(final String name, final float startTime, final float endTime) {
        if (startTime > endTime) {
            throw new IllegalArgumentException("startTime > endTime");
        }
        final List<Float> times = Lists.newArrayList();
        final List<String> keys = Lists.newArrayList();

        final TriggerData tData = new TriggerData();

        // Add start sample
        updateSample(startTime, tData);
        times.add(0f);
        keys.add(tData.getCurrentTrigger());

        // Add mid samples
        for (int i = 0; i < getSampleCount(); i++) {
            final float time = _times[i];
            updateSample(time, tData);
            if (time > startTime && time < endTime) {
                times.add(time - startTime);
                keys.add(_keys[i]);
            }
        }

        // Add end sample
        updateSample(endTime, tData);
        times.add(endTime - startTime);
        keys.add(tData.getCurrentTrigger());

        final float[] timesArray = new float[times.size()];
        int i = 0;
        for (final float time : times) {
            timesArray[i++] = time;
        }
        // return
        return new TriggerChannel(name, timesArray, keys.toArray(new String[] {}));
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends TriggerChannel> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_keys, "keys", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        final String[] keys = capsule.readStringArray("keys", null);
        try {
            final Field field1 = TriggerChannel.class.getDeclaredField("_keys");
            field1.setAccessible(true);
            field1.set(this, keys);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static TriggerChannel initSavable() {
        return new TriggerChannel();
    }

    protected TriggerChannel() {
        super(null, null);
        _keys = null;
    }
}
