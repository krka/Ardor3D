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
import java.util.logging.Logger;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * An animation channel consisting of a series of transforms interpolated over time.
 */
@SavableFactory(factoryMethod = "initSavable")
public class TransformChannel extends AbstractAnimationChannel {

    private static final Logger logger = Logger.getLogger(TransformChannel.class.getName());

    // XXX: Perhaps we could optimize memory by reusing sample objects that are the same from one index to the next.
    // XXX: Could then also optimize execution time by checking object equality (==) and skipping (s)lerps.

    /** Our rotation samples. */
    private final ReadOnlyQuaternion[] _rotations;

    /** Our translation samples. */
    private final ReadOnlyVector3[] _translations;

    /** Our scale samples. */
    private final ReadOnlyVector3[] _scales;

    /**
     * Construct a new TransformChannel.
     * 
     * @param channelName
     *            our name.
     * @param times
     *            our time offset values.
     * @param rotations
     *            the rotations to set on this channel at each time offset.
     * @param translations
     *            the translations to set on this channel at each time offset.
     * @param scales
     *            the scales to set on this channel at each time offset.
     */
    public TransformChannel(final String channelName, final float[] times, final ReadOnlyQuaternion[] rotations,
            final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        super(channelName, times);

        if (rotations.length != times.length || translations.length != times.length || scales.length != times.length) {
            throw new IllegalArgumentException("All provided arrays must be the same length!");
        }

        // Construct our data
        _rotations = Arrays.copyOf((Quaternion[]) rotations, rotations.length);
        _translations = Arrays.copyOf(translations, translations.length);
        _scales = Arrays.copyOf(scales, scales.length);
    }

    /**
     * Construct a new TransformChannel.
     * 
     * @param channelName
     *            our name.
     * @param times
     *            our time offset values.
     * @param transforms
     *            the transform to set on this channel at each time offset. These are separated into rotation, scale and
     *            translation components. Note that supplying transforms with non-rotational matrices (with built in
     *            shear, scale.) will produce a warning and may not give you the expected result.
     */
    public TransformChannel(final String channelName, final float[] times, final ReadOnlyTransform[] transforms) {
        super(channelName, times);

        // Construct our data
        _rotations = new ReadOnlyQuaternion[transforms.length];
        _translations = new ReadOnlyVector3[transforms.length];
        _scales = new ReadOnlyVector3[transforms.length];

        for (int i = 0; i < transforms.length; i++) {
            final ReadOnlyTransform transform = transforms[i];
            if (!transform.isRotationMatrix()) {
                TransformChannel.logger
                        .warning("TransformChannel supplied transform with non-rotational matrices.  May have unexpected results.");
            }
            _rotations[i] = new Quaternion().fromRotationMatrix(transform.getMatrix());
            _translations[i] = new Vector3(transform.getTranslation());
            _scales[i] = new Vector3(transform.getScale());
        }
    }

    @Override
    public void setCurrentSample(final int sampleIndex, final double progressPercent, final Object applyTo) {
        final TransformData transformData = (TransformData) applyTo;

        // shortcut if we are fully on one sample or the next
        if (progressPercent == 0.0f) {
            transformData.setRotation(_rotations[sampleIndex]);
            transformData.setTranslation(_translations[sampleIndex]);
            transformData.setScale(_scales[sampleIndex]);
            return;
        } else if (progressPercent == 1.0f) {
            transformData.setRotation(_rotations[sampleIndex + 1]);
            transformData.setTranslation(_translations[sampleIndex + 1]);
            transformData.setScale(_scales[sampleIndex + 1]);
            return;
        }

        // Apply (s)lerp and set in transform
        final Quaternion workR = Quaternion.fetchTempInstance();
        workR.slerpLocal(_rotations[sampleIndex], _rotations[sampleIndex + 1], progressPercent);
        transformData.setRotation(workR);
        Quaternion.releaseTempInstance(workR);

        final Vector3 workTS = Vector3.fetchTempInstance();
        workTS.lerpLocal(_translations[sampleIndex], _translations[sampleIndex + 1], progressPercent);
        transformData.setTranslation(workTS);
        workTS.lerpLocal(_scales[sampleIndex], _scales[sampleIndex + 1], progressPercent);
        transformData.setScale(workTS);
        Vector3.releaseTempInstance(workTS);
    }

    /**
     * Apply a specific index of this channel to a TransformData object.
     * 
     * @param index
     *            the index to grab.
     * @param store
     *            the TransformData to store in. If null, a new one is created.
     * @return our resulting TransformData.
     */
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

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends TransformChannel> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(asSavableArray(_rotations), "rotations", null);
        capsule.write(asSavableArray(_scales), "scales", null);
        capsule.write(asSavableArray(_translations), "translations", null);
    }

    private Savable[] asSavableArray(final Object[] values) {
        final Savable[] rVal = new Savable[values.length];
        for (int i = 0; i < values.length; i++) {
            rVal[i] = (Savable) values[i];
        }
        return rVal;
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        final ReadOnlyQuaternion[] rotations = (ReadOnlyQuaternion[]) capsule.readSavableArray("rotations", null);
        final ReadOnlyVector3[] scales = (ReadOnlyVector3[]) capsule.readSavableArray("scales", null);
        final ReadOnlyVector3[] translations = (ReadOnlyVector3[]) capsule.readSavableArray("translations", null);
        try {
            final Field field1 = this.getClass().getDeclaredField("_rotations");
            field1.setAccessible(true);
            field1.set(this, rotations);

            final Field field2 = this.getClass().getDeclaredField("_scales");
            field2.setAccessible(true);
            field2.set(this, scales);

            final Field field3 = this.getClass().getDeclaredField("_translations");
            field3.setAccessible(true);
            field3.set(this, translations);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static TransformChannel initSavable() {
        return new TransformChannel();
    }

    protected TransformChannel() {
        super(null, null);
        _rotations = null;
        _translations = null;
        _scales = null;
    }
}