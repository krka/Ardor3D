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
import java.util.List;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.queue.RenderQueue;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.scenegraph.FloatBufferData;
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

    protected int _stencilClearValue;

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

        // Not there? Use the state we received
        if (tempState == null) {
            tempState = state;
        }

        // Still null? Use our default state
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

    protected void addStats(final IndexMode indexMode, final int vertCount) {
        final int primCount = IndexMode.getPrimitiveCount(indexMode, vertCount);
        switch (indexMode) {
            case Triangles:
            case TriangleFan:
            case TriangleStrip:
                StatCollector.addStat(StatType.STAT_TRIANGLE_COUNT, primCount);
                break;
            case Lines:
            case LineLoop:
            case LineStrip:
                StatCollector.addStat(StatType.STAT_LINE_COUNT, primCount);
                break;
            case Points:
                StatCollector.addStat(StatType.STAT_POINT_COUNT, primCount);
                break;
            case Quads:
            case QuadStrip:
                StatCollector.addStat(StatType.STAT_QUAD_COUNT, primCount);
                break;
        }
    }

    protected int getTotalInterleavedSize(final RenderContext context, final FloatBufferData vertexCoords,
            final FloatBufferData normalCoords, final FloatBufferData colorCoords,
            final List<FloatBufferData> textureCoords) {
        final ContextCapabilities caps = context.getCapabilities();

        int bufferSize = 0;
        if (normalCoords != null) {
            bufferSize += normalCoords.getBufferLimit() * 4;
        }
        if (colorCoords != null) {
            bufferSize += colorCoords.getBufferLimit() * 4;
        }
        if (textureCoords != null) {
            final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
            int offset = 0;
            if (ts != null) {
                offset = ts.getTextureCoordinateOffset();

                for (int i = 0; i < ts.getNumberOfSetTextures() && i < caps.getNumberOfFragmentTexCoordUnits(); i++) {
                    if (textureCoords == null || i >= textureCoords.size()) {
                        continue;
                    }

                    final FloatBufferData textureBufferData = textureCoords.get(i + offset);
                    if (textureBufferData != null) {
                        bufferSize += textureBufferData.getBufferLimit() * 4;
                    }
                }
            }
        }
        if (vertexCoords != null) {
            bufferSize += vertexCoords.getBufferLimit() * 4;
        }

        return bufferSize;
    }

    public int getStencilClearValue() {
        return _stencilClearValue;
    }

    public void setStencilClearValue(final int stencilClearValue) {
        _stencilClearValue = stencilClearValue;
    }

    public boolean isClipTestEnabled() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        return record.isClippingTestEnabled();
    }
}
