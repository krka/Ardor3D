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

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * Transform animation channel, specifically geared towards describing the motion of skeleton joints.
 */
@SavableFactory(factoryMethod = "initSavable")
public class JointChannel extends TransformChannel {

    /** A name prepended to joint indices to identify them as joint channels. */
    public static final String JOINT_CHANNEL_NAME = "_jnt";

    /** The human readable version of the name. */
    private final String _jointName;

    /**
     * Construct a new JointChannel.
     * 
     * @param jointIndex
     *            the index of the joint.
     * @param times
     *            our time offset values.
     * @param rotations
     *            the rotations to set on this channel at each time offset.
     * @param translations
     *            the translations to set on this channel at each time offset.
     * @param scales
     *            the scales to set on this channel at each time offset.
     */
    public JointChannel(final Joint joint, final float[] times, final ReadOnlyQuaternion[] rotations,
            final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        super(JointChannel.JOINT_CHANNEL_NAME + joint.getIndex(), times, rotations, translations, scales);
        _jointName = joint.getName();
    }

    /**
     * Construct a new JointChannel.
     * 
     * @param joint
     *            the index of the joint.
     * @param times
     *            our time offset values.
     * @param transforms
     *            the transform to set on this channel at each time offset.
     */
    public JointChannel(final Joint joint, final float[] times, final ReadOnlyTransform[] transforms) {
        super(JointChannel.JOINT_CHANNEL_NAME + joint.getIndex(), times, transforms);
        _jointName = joint.getName();
    }

    /**
     * @return the human readable version of the associated joint's name.
     */
    public String getJointName() {
        return _jointName;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends JointChannel> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_jointName, "jointName", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        final String jointName = capsule.readString("jointName", null);
        try {
            final Field field1 = this.getClass().getDeclaredField("_jointName");
            field1.setAccessible(true);
            field1.set(this, jointName);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static JointChannel initSavable() {
        return new JointChannel();
    }

    protected JointChannel() {
        super();
        _jointName = null;
    }
}
