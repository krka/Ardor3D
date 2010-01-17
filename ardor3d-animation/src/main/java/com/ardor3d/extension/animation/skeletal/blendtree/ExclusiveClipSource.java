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

import java.util.List;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationClip;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.JointChannel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Provides the associated clip as a BlendTreeSource, excluding a given set of channels by name.
 */
public class ExclusiveClipSource extends ClipSource {

    private List<String> _disabledChannels;

    public ExclusiveClipSource() {}

    public ExclusiveClipSource(final AnimationClip clip, final AnimationManager manager) {
        super(clip, manager);
    }

    public void setDisabledChannels(final List<String> disabledChannels) {
        if (_disabledChannels == null) {
            _disabledChannels = Lists.newArrayList(disabledChannels);
        } else {
            _disabledChannels.clear();
            _disabledChannels.addAll(disabledChannels);
        }
    }

    public void setDisabledJoints(final List<Integer> disabledJoints) {
        if (_disabledChannels == null) {
            _disabledChannels = Lists.newArrayList();
        } else {
            _disabledChannels.clear();
        }
        for (final Integer i : disabledJoints) {
            if (i != null) {
                _disabledChannels.add(JointChannel.JOINT_CHANNEL_NAME + i.intValue());
            }
        }
    }

    /**
     * @return a COPY of the disabled channel list.
     */
    public ImmutableList<String> getDisabledChannels() {
        return ImmutableList.copyOf(_disabledChannels);
    }

    @Override
    public Map<String, Object> getSourceData() {
        final Map<String, Object> orig = super.getSourceData();

        // make a copy, removing specific channels
        final Map<String, Object> data = Maps.newHashMap(orig);
        if (_disabledChannels != null) {
            for (final String key : _disabledChannels) {
                data.remove(key);
            }
        }

        return data;
    }

}
