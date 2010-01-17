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

import java.util.List;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.blendtree.BlendTreeApplier;
import com.ardor3d.extension.animation.skeletal.blendtree.BlendTreeSource;
import com.ardor3d.util.Timer;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

public class AnimationManager {
    // A copy of our global timer
    private final Timer _globalTimer;

    // The pose this manager is manipulating
    private final SkeletonPose _applyToPose;

    // The clips assigned to this manager. These clips may be in use by multiple managers as one time.
    private final List<AnimationClip> _clips = Lists.newArrayList();

    // Local state information for the clips.
    private final Map<AnimationClip, AnimationClipState> _clipStates = new MapMaker().weakKeys().makeMap();

    // A logic object responsible for taking blend tree data and applying it to the the skeleton pose.
    private BlendTreeApplier _applier;

    // The root of our blend tree
    private BlendTreeSource _blendRoot;

    public AnimationManager(final Timer globalTimer, final SkeletonPose applyToPose) {
        _globalTimer = globalTimer;
        _applyToPose = applyToPose;
    }

    public Timer getGlobalTimer() {
        return _globalTimer;
    }

    public SkeletonPose getApplyToPose() {
        return _applyToPose;
    }

    public BlendTreeApplier getApplier() {
        return _applier;
    }

    public void setApplier(final BlendTreeApplier applier) {
        _applier = applier;
    }

    public void update() {
        // grab current global time
        final double globalTime = _globalTimer.getTimeInSeconds();

        // call update on each active clip, passing current global time.
        for (final AnimationClip clip : _clips) {
            final AnimationClipState clipState = getClipState(clip);
            if (clipState.isActive()) {
                double clockTime = clipState.getTimeScale() * (globalTime - clipState.getStartTime());
                final double maxTime = clip.getMaxTimeIndex();
                if (maxTime <= 0) {
                    continue;
                }

                // Check for looping.
                if (clipState.getLoopCount() == Integer.MAX_VALUE || clipState.getLoopCount() > 1
                        && maxTime * clipState.getLoopCount() <= clockTime) {
                    clockTime %= maxTime;
                }
                // Check for past max time
                if (clockTime > maxTime) {
                    clockTime = maxTime;
                    // signal to any listeners that we have ended our animation.
                    clipState.fireAnimationFinished();
                    // deactivate this instance of the clip
                    clipState.setActive(false);
                }
                // update the clip with the correct clip local time.
                clip.update(clockTime, clipState);
            }
        }

        // call apply on blend module, passing in pose
        _applier.applyTo(_applyToPose, _blendRoot);
    }

    public AnimationClipState getClipState(final AnimationClip clip) {
        AnimationClipState clipState = _clipStates.get(clip);
        if (clipState == null) {
            clipState = new AnimationClipState();
            clipState.setStartTime(_globalTimer.getTimeInSeconds());
            _clipStates.put(clip, clipState);
        }

        return clipState;
    }

    public void resetClip(final AnimationClip clip) {
        final AnimationClipState clipState = getClipState(clip);
        clipState.setStartTime(_globalTimer.getTimeInSeconds());
        clipState.setActive(true);
    }

    public List<AnimationClip> getActiveClips(final List<AnimationClip> store) {
        store.clear();
        for (final AnimationClip clip : _clipStates.keySet()) {
            final AnimationClipState state = _clipStates.get(clip);
            if (state.isActive()) {
                store.add(clip);
            }
        }
        return store;
    }

    public void addClip(final AnimationClip clip) {
        _clips.add(clip);
    }

    public boolean removeClip(final AnimationClip clip) {
        return _clips.remove(clip);
    }

    public void setBlendRoot(final BlendTreeSource blendRoot) {
        _blendRoot = blendRoot;
    }

    public BlendTreeSource getBlendRoot() {
        return _blendRoot;
    }
}
