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

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;

@SavableFactory(factoryMethod = "initSavable")
public class JointChannel extends TransformChannel {

    public static final String JOINT_CHANNEL_NAME = "_jnt";

    public JointChannel(final int jointIndex, final float[] times, final ReadOnlyQuaternion[] rotations,
            final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        super(JointChannel.JOINT_CHANNEL_NAME + jointIndex, times, rotations, translations, scales);
    }

    public JointChannel(final int jointIndex, final float[] times, final ReadOnlyTransform[] transforms) {
        super(JointChannel.JOINT_CHANNEL_NAME + jointIndex, times, transforms);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends JointChannel> getClassTag() {
        return this.getClass();
    }

    public static JointChannel initSavable() {
        return new JointChannel();
    }

    protected JointChannel() {
        super();
    }
}
