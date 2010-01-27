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

import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * NOTE: Could optimize memory by reusing math objects that are the same from one index to the next. Could optimize
 * execution time by then checking object equality and skipping (s)lerps.
 */
public class TransformChannel extends AbstractAnimationChannel<TransformData> {

    private final ReadOnlyQuaternion[] _rotations;
    private final ReadOnlyVector3[] _translations;
    private final ReadOnlyVector3[] _scales;
    private final Quaternion _workR = new Quaternion();
    private final Vector3 _workT = new Vector3();
    private final Vector3 _workS = new Vector3();

    public TransformChannel(final String channelName, final float[] times, final ReadOnlyQuaternion[] rotations,
            final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        super(channelName, times);

        if (rotations.length != times.length || translations.length != times.length || scales.length != times.length) {
            throw new IllegalArgumentException("All provided arrays must be the same length!");
        }

        // Construct our data
        _rotations = TransformChannel.copyOf(rotations, new ReadOnlyQuaternion[rotations.length]);
        _translations = TransformChannel.copyOf(translations, new ReadOnlyVector3[translations.length]);
        _scales = TransformChannel.copyOf(scales, new ReadOnlyVector3[scales.length]);
    }

    /**
     * REPLACE THIS ONCE WE SWITCH TO JAVA 6.0 (with Arrays.copyOf)
     * 
     * @param <T>
     * @param srcArray
     * @param dstArray
     * @return
     */
    private static <T> T[] copyOf(final T[] srcArray, final T[] dstArray) {
        System.arraycopy(srcArray, 0, dstArray, 0, Math.min(srcArray.length, dstArray.length));
        return dstArray;
    }

    public TransformChannel(final String channelName, final float[] times, final ReadOnlyTransform[] transforms) {
        super(channelName, times);

        // Construct our data
        _rotations = new ReadOnlyQuaternion[transforms.length];
        _translations = new ReadOnlyVector3[transforms.length];
        _scales = new ReadOnlyVector3[transforms.length];

        for (int i = 0; i < transforms.length; i++) {
            final ReadOnlyTransform transform = transforms[i];
            if (!transform.isRotationMatrix()) {
                throw new IllegalArgumentException("Transforms that have non-rotational matrices are not supported.");
            }
            _rotations[i] = new Quaternion().fromRotationMatrix(transform.getMatrix());
            _translations[i] = new Vector3(transform.getTranslation());
            _scales[i] = new Vector3(transform.getScale());
        }
    }

    @Override
    public void setCurrentSample(final int sampleIndex, final TransformData applyTo) {
        applyTo.setRotation(_rotations[sampleIndex]);
        applyTo.setTranslation(_translations[sampleIndex]);
        applyTo.setScale(_scales[sampleIndex]);
    }

    @Override
    public void setCurrentSample(final int sampleIndex, final double progressPercent, final TransformData applyTo) {
        // shortcut
        if (progressPercent == 0.0f) {
            setCurrentSample(sampleIndex, applyTo);
            return;
        } else if (progressPercent == 1.0f) {
            setCurrentSample(sampleIndex + 1, applyTo);
            return;
        }

        // Apply lerp
        _workR.slerpLocal(_rotations[sampleIndex], _rotations[sampleIndex + 1], progressPercent);
        _workT.lerpLocal(_translations[sampleIndex], _translations[sampleIndex + 1], progressPercent);
        _workS.lerpLocal(_scales[sampleIndex], _scales[sampleIndex + 1], progressPercent);

        // Set in transform
        applyTo.setRotation(_workR);
        applyTo.setTranslation(_workT);
        applyTo.setScale(_workS);
    }

    public TransformData getTransformData(final int index, final TransformData store) {
        TransformData rVal = store;
        if (rVal == null) {
            rVal = new TransformData();
        }
        rVal.setRotation(_rotations[index]);
        rVal.setScale(_scales[index]);
        rVal.setTranslation(_translations[index]);
        return rVal;
    }

    @Override
    public TransformData createStateDataObject() {
        return new TransformData();
    }
}