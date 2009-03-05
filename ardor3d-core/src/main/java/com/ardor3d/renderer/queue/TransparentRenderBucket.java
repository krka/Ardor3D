/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.queue;

import java.util.Comparator;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;

public class TransparentRenderBucket extends AbstractRenderBucket {
    /** CullState for two pass transparency rendering. */
    private final CullState _tranparentCull;

    /** ZBufferState for two pass transparency rendering. */
    private final ZBufferState _transparentZBuff;

    /** boolean for enabling / disabling two pass transparency rendering. */
    private boolean _twoPassTransparent = true;

    public TransparentRenderBucket(final Renderer renderer) {
        super(renderer);

        _tranparentCull = new CullState();
        _transparentZBuff = new ZBufferState();
        _transparentZBuff.setWritable(false);
        _transparentZBuff.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);

        _comparator = new TransparentComparator();
    }

    @Override
    public void render() {
        for (int i = 0; i < _currentListSize; i++) {
            final Spatial spatial = _currentList[i];

            if (_twoPassTransparent
                    && spatial instanceof Mesh
                    && ((((Mesh) spatial)._getWorldRenderState(RenderState.StateType.Cull) == null || !((Mesh) spatial)
                            ._getWorldRenderState(RenderState.StateType.Cull).isEnabled()))) {
                final Mesh geom = (Mesh) spatial;
                final RenderState oldCullState = geom._getWorldRenderState(RenderState.StateType.Cull);
                geom._setWorldRenderState(_tranparentCull);
                final ZBufferState oldZState = (ZBufferState) geom._getWorldRenderState(RenderState.StateType.ZBuffer);
                geom._setWorldRenderState(_transparentZBuff);

                // first render back-facing tris only
                _tranparentCull.setCullFace(CullState.Face.Front);
                geom.draw(_renderer);

                // then render front-facing tris only
                if (oldZState != null) {
                    geom._setWorldRenderState(oldZState);
                } else {
                    geom._clearWorldRenderState(StateType.ZBuffer);
                }
                _tranparentCull.setCullFace(CullState.Face.Back);
                geom.draw(_renderer);
                if (oldCullState != null) {
                    geom._setWorldRenderState(oldCullState);
                } else {
                    geom._clearWorldRenderState(StateType.Cull);
                }
            } else {
                spatial.draw(_renderer);
            }
            // TODO: this optimization should not be in the Spatial
            // obj.queueDistance = Double.NEGATIVE_INFINITY;
        }
    }

    public boolean isTwoPassTransparency() {
        return _twoPassTransparent;
    }

    public void setTwoPassTransparency(final boolean twoPassTransparent) {
        _twoPassTransparent = twoPassTransparent;
    }

    private class TransparentComparator implements Comparator<Spatial> {
        public int compare(final Spatial o1, final Spatial o2) {
            final double d1 = distanceToCam(o1);
            final double d2 = distanceToCam(o2);
            if (d1 > d2) {
                return -1;
            } else if (d1 < d2) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}
