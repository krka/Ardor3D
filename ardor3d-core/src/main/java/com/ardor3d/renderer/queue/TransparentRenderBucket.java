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

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
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
        final RenderContext context = ContextManager.getCurrentContext();
        for (int i = 0; i < _currentListSize; i++) {
            final Spatial spatial = _currentList[i];

            if (_twoPassTransparent && spatial instanceof Mesh) {
                final Mesh mesh = (Mesh) spatial;
                if (mesh.getWorldRenderState(RenderState.StateType.Cull) == null) {
                    final RenderState oldCullState = context.getEnforcedState(StateType.Cull);
                    final RenderState oldZState = context.getEnforcedState(StateType.ZBuffer);

                    context.enforceState(_tranparentCull);
                    context.enforceState(_transparentZBuff);

                    // first render back-facing tris only
                    _tranparentCull.setCullFace(CullState.Face.Front);
                    mesh.draw(_renderer);

                    // then render front-facing tris only
                    // reset enforced zstate
                    if (oldZState != null) {
                        context.enforceState(oldZState);
                    } else {
                        context.clearEnforcedState(StateType.ZBuffer);
                    }
                    _tranparentCull.setCullFace(CullState.Face.Back);
                    mesh.draw(_renderer);
                    // reset enforced cull state
                    if (oldCullState != null) {
                        context.enforceState(oldCullState);
                    } else {
                        context.clearEnforcedState(StateType.Cull);
                    }
                    continue;
                }
            }
            spatial.draw(_renderer);
        }
    }

    public boolean isTwoPassTransparency() {
        return _twoPassTransparent;
    }

    /**
     * 
     * @param twoPassTransparent
     *            true to enable two pass drawing. In this mode, the Spatial will be draw twice, first with only back
     *            faces showing and second with only front faces showing. This results in more accurate and
     *            artifact-free results, but takes more drawing time and is not necessary for planar surfaces.
     */
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
