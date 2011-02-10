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

import com.ardor3d.extension.animation.skeletal.AnimationApplier;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.clip.TransformData;
import com.ardor3d.extension.animation.skeletal.clip.TriggerCallback;
import com.ardor3d.extension.animation.skeletal.clip.TriggerData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Very simple applier. Just applies joint transform data, calls any callbacks and updates the pose's global transforms.
 */
public class SimpleAnimationApplier implements AnimationApplier {

    private final Multimap<String, TriggerCallback> _triggerCallbacks = ArrayListMultimap.create();

    @Override
    public void applyTo(final SkeletonPose applyToPose, final AnimationManager manager) {
        final Map<String, ? extends Object> data = manager.getCurrentSourceData();

        // cycle through, pulling out and applying those we know about
        if (data != null) {
            for (final String name : data.keySet()) {
                final Object value = data.get(name);
                if (name.startsWith(JointChannel.JOINT_CHANNEL_NAME)) {
                    final int jointIndex = Integer.parseInt(name.substring(JointChannel.JOINT_CHANNEL_NAME.length()));
                    ((TransformData) value).applyTo(applyToPose.getLocalJointTransforms()[jointIndex]);
                } else if (value instanceof TriggerData) {
                    final TriggerData trigger = (TriggerData) value;
                    if (trigger.isArmed()) {
                        try {
                            // pull callback(s) for the current trigger key, if exists, and call.
                            for (final TriggerCallback cb : _triggerCallbacks.get(trigger.getCurrentTrigger())) {
                                cb.doTrigger(applyToPose, manager);
                            }
                        } finally {
                            trigger.setArmed(false);
                        }
                    }
                }
            }

            applyToPose.updateTransforms();
        }
    }

    @Override
    public void addTriggerCallback(final String key, final TriggerCallback callback) {
        _triggerCallbacks.put(key, callback);
    }

    @Override
    public boolean removeTriggerCallback(final String key, final TriggerCallback callback) {
        return _triggerCallbacks.remove(key, callback);
    }
}
