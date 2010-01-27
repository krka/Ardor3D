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
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.TransformData;

/**
 * Very simple applier. Just applies joint transform data.
 */
public class TransformApplier implements BlendTreeApplier {
    public void applyTo(final SkeletonPose applyToPose, final BlendTreeSource blendTreeRoot) {
        final Map<String, ? extends Object> data = blendTreeRoot.getSourceData();

        // cycle through, pulling out and applying those we know about
        for (final String name : data.keySet()) {
            if (name.startsWith(JointChannel.JOINT_CHANNEL_NAME)) {
                final int jointIndex = Integer.parseInt(name.substring(JointChannel.JOINT_CHANNEL_NAME.length()));
                ((TransformData) data.get(name)).applyTo(applyToPose.getLocalJointTransforms()[jointIndex]);
            }
        }
    }
}
