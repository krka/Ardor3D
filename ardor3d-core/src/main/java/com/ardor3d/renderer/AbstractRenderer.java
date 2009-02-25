/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import java.util.EnumMap;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.queue.RenderQueue;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.util.Debug;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * Provides some common base level method implementations for Renderers.
 */
public abstract class AbstractRenderer implements Renderer {
    // clear color
    protected final ColorRGBA _backgroundColor = new ColorRGBA(ColorRGBA.BLACK);

    protected boolean _processingQueue;

    protected RenderQueue _queue;

    protected boolean _inOrthoMode;

    /** List of default rendering states for this specific renderer type */
    protected final EnumMap<RenderState.StateType, RenderState> defaultStateList = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    public AbstractRenderer() {
        for (final RenderState.StateType type : RenderState.StateType.values()) {
            final RenderState state = RenderState.createState(type);
            state.setEnabled(false);
            defaultStateList.put(type, state);
        }
    }

    public boolean isInOrthoMode() {
        return _inOrthoMode;
    }

    public ReadOnlyColorRGBA getBackgroundColor() {
        return _backgroundColor;
    }

    public RenderQueue getQueue() {
        return _queue;
    }

    public boolean isProcessingQueue() {
        return _processingQueue;
    }

    public void applyLightState(final LightState state) {
        if (Debug.stats) {
            StatCollector.startStat(StatType.STAT_STATES_TIMER);
        }

        final RenderContext context = ContextManager.getCurrentContext();

        RenderState tempState = null;
        final StateType type = StateType.Light;
        // first look up in enforced states
        tempState = context.getEnforcedState(type);

        // Not there? Look in the states we receive
        if (tempState == null) {
            tempState = state;
        }

        // Still missing? Use our default states.
        if (tempState == null) {
            tempState = defaultStateList.get(type);
        }

        if (!RenderState._quickCompare.contains(type) || tempState.needsRefresh()
                || tempState != context.getCurrentState(type)) {
            applyState(tempState);
            tempState.setNeedsRefresh(false);
        }

        if (Debug.stats) {
            StatCollector.endStat(StatType.STAT_STATES_TIMER);
        }
    }

    public void applyNonLightStates(final EnumMap<StateType, RenderState> states) {
        if (Debug.stats) {
            StatCollector.startStat(StatType.STAT_STATES_TIMER);
        }

        final RenderContext context = ContextManager.getCurrentContext();

        RenderState tempState = null;
        for (final StateType type : StateType.values) {
            if (type == StateType.Light) {
                continue;
            }
            // first look up in enforced states
            tempState = context.getEnforcedState(type);

            // Not there? Look in the states we receive
            if (tempState == null) {
                tempState = states.get(type);
            }

            // Still missing? Use our default states.
            if (tempState == null) {
                tempState = defaultStateList.get(type);
            }

            if (!RenderState._quickCompare.contains(type) || tempState.needsRefresh()
                    || tempState != context.getCurrentState(type)) {
                applyState(tempState);
                tempState.setNeedsRefresh(false);
            }
        }

        if (Debug.stats) {
            StatCollector.endStat(StatType.STAT_STATES_TIMER);
        }
    }
}
