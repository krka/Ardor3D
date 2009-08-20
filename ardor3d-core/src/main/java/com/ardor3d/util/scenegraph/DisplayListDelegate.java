/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.scenegraph;

import java.lang.ref.ReferenceQueue;
import java.util.Map;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.SimpleContextIdReference;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;

public class DisplayListDelegate implements RenderDelegate {

    private static Map<DisplayListDelegate, Object> _identityCache = new MapMaker().weakKeys().makeMap();
    private static final Object STATIC_REF = new Object();

    private static ReferenceQueue<DisplayListDelegate> _refQueue = new ReferenceQueue<DisplayListDelegate>();

    private final SimpleContextIdReference<DisplayListDelegate> _id;

    public DisplayListDelegate(final int id, final Object glContext) {
        _id = new SimpleContextIdReference<DisplayListDelegate>(this, _refQueue, id, glContext);
        _identityCache.put(this, STATIC_REF);
    }

    public void render(final Spatial spatial, final Renderer renderer) {
        // do transforms
        renderer.doTransforms(spatial.getWorldTransform());

        // render display list.
        renderer.renderDisplayList(_id.getId());

        // Our states are in an unknown state at this point, so invalidate tracking.
        ContextManager.getCurrentContext().invalidateStates();
    }

    public static void cleanAllDisplayLists(final Renderer deleter) {
        final Multimap<Object, Integer> idMap = ArrayListMultimap.create();

        // gather up expired Display Lists... these don't exist in our cache
        gatherGCdIds(idMap);

        // Walk through the cached items and delete those too.
        for (final DisplayListDelegate buf : _identityCache.keySet()) {
            // Add id to map
            idMap.put(buf._id.getGlContext(), buf._id.getId());
        }

        handleDisplayListDelete(deleter, idMap);
    }

    public static void cleanExpiredDisplayLists(final Renderer deleter) {
        final Multimap<Object, Integer> idMap = ArrayListMultimap.create();

        // gather up expired display lists...
        gatherGCdIds(idMap);

        // send to be deleted on next render.
        handleDisplayListDelete(deleter, idMap);
    }

    @SuppressWarnings("unchecked")
    private static void gatherGCdIds(final Multimap<Object, Integer> idMap) {
        // Pull all expired display lists from ref queue and add to an id multimap.
        SimpleContextIdReference<DisplayListDelegate> ref;
        while ((ref = (SimpleContextIdReference<DisplayListDelegate>) _refQueue.poll()) != null) {
            idMap.put(ref.getGlContext(), ref.getId());
            ref.clear();
        }
    }

    private static void handleDisplayListDelete(final Renderer deleter, final Multimap<Object, Integer> idMap) {
        Object currentGLRef = null;
        // Grab the current context, if any.
        if (deleter != null && ContextManager.getCurrentContext() != null) {
            currentGLRef = ContextManager.getCurrentContext().getGlContextRep();
        }
        // For each affected context...
        for (final Object glref : idMap.keySet()) {
            // If we have a deleter and the context is current, immediately delete
            if (deleter != null && glref.equals(currentGLRef)) {
                deleter.deleteDisplayLists(idMap.get(glref));
            }
            // Otherwise, add a delete request to that context's render task queue.
            else {
                GameTaskQueueManager.getManager(ContextManager.getContextForRef(glref)).render(
                        new RendererCallable<Void>() {
                            public Void call() throws Exception {
                                getRenderer().deleteDisplayLists(idMap.get(glref));
                                return null;
                            }
                        });
            }
        }
    }
}
