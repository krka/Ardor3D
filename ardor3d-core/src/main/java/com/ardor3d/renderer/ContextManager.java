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

import com.ardor3d.util.WeakIdentityCache;

public class ContextManager {

    protected static RenderContext currentContext = null;

    protected static final WeakIdentityCache<Object, RenderContext> contextStore = new WeakIdentityCache<Object, RenderContext>();

    /**
     * @return a RenderContext object representing the current OpenGL context.
     */
    public static RenderContext getCurrentContext() {
        return currentContext;
    }

    public static RenderContext switchContext(final Object contextKey) {
        currentContext = contextStore.get(contextKey);
        if (currentContext == null) {
            throw new IllegalArgumentException("contextKey not found in context store.");
        }
        return currentContext;
    }

    public static void removeContext(final Object contextKey) {
        contextStore.remove(contextKey);
    }

    public static void addContext(final Object contextKey, final RenderContext context) {
        contextStore.put(contextKey, context);
    }
}
