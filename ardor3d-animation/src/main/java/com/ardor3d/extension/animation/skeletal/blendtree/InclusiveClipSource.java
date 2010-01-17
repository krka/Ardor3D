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
 * Provides the associated clip as a BlendTreeSource, only including a given set of channels by name.
 */
public class InclusiveClipSource extends ClipSource {

    private List<String> _enabledChannels;

    public InclusiveClipSource() {}

    public InclusiveClipSource(final AnimationClip clip, final AnimationManager manager) {
        super(clip, manager);
    }

    public void setEnabledChannels(final List<String> enabledChannels) {
        if (_enabledChannels == null) {
            _enabledChannels = Lists.newArrayList(enabledChannels);
        } else {
            _enabledChannels.clear();
            _enabledChannels.addAll(enabledChannels);
        }
    }

    public void setEnabledJoints(final List<Integer> enabledJoints) {
        if (_enabledChannels == null) {
            _enabledChannels = Lists.newArrayList();
        } else {
            _enabledChannels.clear();
        }
        for (final Integer i : enabledJoints) {
            if (i != null) {
                _enabledChannels.add(JointChannel.JOINT_CHANNEL_NAME + i.intValue());
            }
        }
    }

    /**
     * @return a COPY of the enabled channel list.
     */
    public ImmutableList<String> getEnabledChannels() {
        return ImmutableList.copyOf(_enabledChannels);
    }

    @Override
    public Map<String, Object> getSourceData() {
        final Map<String, Object> orig = super.getSourceData();

        // make a copy, only bringing across specific channels
        final Map<String, Object> data = Maps.newHashMap();
        if (_enabledChannels != null) {
            for (final String key : _enabledChannels) {
                if (orig.containsKey(key)) {
                    data.put(key, orig.get(key));
                }
            }
        }

        return data;
    }

}
