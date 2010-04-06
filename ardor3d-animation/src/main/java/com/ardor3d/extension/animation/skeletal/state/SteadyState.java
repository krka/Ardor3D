/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.state;

import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.BlendTreeSource;
import com.ardor3d.extension.animation.skeletal.layer.AnimationLayer;
import com.google.common.collect.Maps;

/**
 * A "steady" state is an animation state that is concrete and stand-alone (vs. a state that handles transitioning
 * between two states, for example.)
 */
public class SteadyState extends AbstractFiniteState {

    /** The name of this state. */
    private final String _name;

    /** A map of possible transitions for moving from this state to another. */
    private final Map<String, AbstractTransitionState> _transitions = Maps.newHashMap();

    /** A transition to use if we reach the end of this state. May be null. */
    private AbstractTransitionState _endTransition;

    /** Our state may be a blend of multiple clips, etc. This is the root of our blend tree. */
    private BlendTreeSource _sourceTree;

    /**
     * Create a new steady state.
     * 
     * @param name
     *            the name of our new state. Immutable.
     */
    public SteadyState(final String name) {
        _name = name;
    }

    /**
     * @return the name of this state.
     */
    public String getName() {
        return _name;
    }

    /**
     * @return the transition to use if we reach the end of this state. May be null.
     */
    public AbstractTransitionState getEndTransition() {
        return _endTransition;
    }

    /**
     * @param endTransition
     *            a transition to use if we reach the end of this state. May be null.
     */
    public void setEndTransition(final AbstractTransitionState endTransition) {
        _endTransition = endTransition;
    }

    /**
     * @return the root of our blend tree
     */
    public BlendTreeSource getSourceTree() {
        return _sourceTree;
    }

    /**
     * @param tree
     *            the new root of our blend tree
     */
    public void setSourceTree(final BlendTreeSource tree) {
        _sourceTree = tree;
    }

    /**
     * Add a new possible transition to this state.
     * 
     * @param keyword
     *            the reference key for the added transition.
     * @param state
     *            the transition state to add.
     * @throws IllegalArgumentException
     *             if keyword or state are null.
     */
    public void addTransition(final String keyword, final AbstractTransitionState state) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null.");
        }
        if (keyword == null) {
            throw new IllegalArgumentException("keyword must not be null.");
        }
        _transitions.put(keyword, state);
    }

    @Override
    public AbstractFiniteState doTransition(final String key, final AnimationLayer layer) {
        if (_transitions.containsKey(key)) {
            final AbstractTransitionState state = _transitions.get(key);
            return state.doTransition(key, layer);
        }
        return null;
    }

    @Override
    public void update(final double globalTime, final AnimationLayer layer) {
        if (!getSourceTree().setTime(globalTime, layer.getManager())) {
            if (_endTransition != null) {
                // time to move to end transition
                final AbstractFiniteState newState = _endTransition.doTransition(null, layer);
                newState.resetClips(layer.getManager());
                if (this != newState) {
                    getLastStateOwner().replaceState(this, newState);
                }
            } else {
                // we're done. end.
                getLastStateOwner().replaceState(this, null);
            }
        }
    }

    @Override
    public Map<String, ? extends Object> getCurrentSourceData(final AnimationManager manager) {
        return getSourceTree().getSourceData(manager);
    }

    @Override
    public void resetClips(final AnimationManager manager, final double globalStartTime) {
        super.resetClips(manager, globalStartTime);
        getSourceTree().resetClips(manager, globalStartTime);
    }
}