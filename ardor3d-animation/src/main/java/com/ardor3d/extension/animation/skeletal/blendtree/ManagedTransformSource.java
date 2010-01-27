/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.blendtree;

import java.util.Map;

import com.ardor3d.extension.animation.skeletal.JointChannel;
import com.ardor3d.extension.animation.skeletal.TransformData;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Source useful for controlling a particular joint or set of joints directly (versus a pre-canned animation).
 */
public class ManagedTransformSource implements BlendTreeSource {

    private final Map<String, TransformData> data = Maps.newHashMap();

    public void setJointTransformData(final int jointIndex, final TransformData tData) {
        final String key = JointChannel.JOINT_CHANNEL_NAME + jointIndex;
        if (!data.containsKey(key)) {
            data.put(key, new TransformData(tData));
        } else {
            final TransformData old = data.get(key);
            old.set(tData);
        }
    }

    public void setJointTranslation(final int jointIndex, final ReadOnlyVector3 translation) {
        final String key = JointChannel.JOINT_CHANNEL_NAME + jointIndex;
        TransformData tData = data.get(key);
        if (tData == null) {
            tData = new TransformData();
            data.put(key, tData);
        }

        tData.setTranslation(translation);
    }

    public void setJointScale(final int jointIndex, final ReadOnlyVector3 scale) {
        final String key = JointChannel.JOINT_CHANNEL_NAME + jointIndex;
        TransformData tData = data.get(key);
        if (tData == null) {
            tData = new TransformData();
            data.put(key, tData);
        }

        tData.setScale(scale);
    }

    public void setJointRotation(final int jointIndex, final ReadOnlyQuaternion rotation) {
        final String key = JointChannel.JOINT_CHANNEL_NAME + jointIndex;
        TransformData tData = data.get(key);
        if (tData == null) {
            tData = new TransformData();
            data.put(key, tData);
        }

        tData.setRotation(rotation);
    }

    public Map<String, TransformData> getSourceData() {
        return ImmutableMap.copyOf(data);
    }
}
