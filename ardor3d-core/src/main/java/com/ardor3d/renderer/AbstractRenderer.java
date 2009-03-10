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
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.util.Constants;
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

    public void applyState(final StateType type, final RenderState state) {
        if (Constants.stats) {
            StatCollector.startStat(StatType.STAT_STATES_TIMER);
        }

        final RenderContext context = ContextManager.getCurrentContext();

        // first look up in enforced states
        RenderState tempState = context.getEnforcedState(type);

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
            doApplyState(tempState);
            tempState.setNeedsRefresh(false);
        }

        if (Constants.stats) {
            StatCollector.endStat(StatType.STAT_STATES_TIMER);
        }
    }

    protected abstract void doApplyState(RenderState state);
}
