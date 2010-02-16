/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.lang.ref.ReferenceQueue;
import java.nio.Buffer;
import java.util.Map;
import java.util.Set;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.util.Constants;
import com.ardor3d.util.ContextIdReference;
import com.ardor3d.util.GameTaskQueueManager;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;

public abstract class AbstractBufferData<T extends Buffer> {

    private static Map<AbstractBufferData<?>, Object> _identityCache = new MapMaker().weakKeys().makeMap();
    private static final Object STATIC_REF = new Object();

    private static ReferenceQueue<AbstractBufferData<?>> _vboRefQueue = new ReferenceQueue<AbstractBufferData<?>>();

    protected transient ContextIdReference<AbstractBufferData<T>> _vboIdCache;

    /** Buffer holding the data. */
    protected T _buffer;

    /** Access mode of the buffer when using Vertex Buffer Objects. */
    public enum VBOAccessMode {
        StaticDraw, StaticCopy, StaticRead, StreamDraw, StreamCopy, StreamRead, DynamicDraw, DynamicCopy, DynamicRead
    }

    /** VBO Access mode for this buffer. */
    protected VBOAccessMode vboAccessMode = VBOAccessMode.StaticDraw;

    /** Flag for notifying the renderer that the VBO buffer needs to be updated. */
    protected boolean needsRefresh = false;

    AbstractBufferData() {
        _identityCache.put(this, STATIC_REF);
    }

    /**
     * Gets the count.
     * 
     * @return the count
     */
    public int getBufferLimit() {
        if (_buffer != null) {
            return _buffer.limit();
        }

        return 0;
    }

    /**
     * Gets the count.
     * 
     * @return the count
     */
    public int getBufferCapacity() {
        if (_buffer != null) {
            return _buffer.capacity();
        }

        return 0;
    }

    /**
     * Get the buffer holding the data.
     * 
     * @return the buffer
     */
    public T getBuffer() {
        return _buffer;
    }

    /**
     * Set the buffer holding the data.
     * 
     * @param buffer
     *            the buffer to set
     */
    public void setBuffer(final T buffer) {
        _buffer = buffer;
    }

    /**
     * @param glContext
     *            the object representing the OpenGL context a vbo belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return the vbo id of a vbo in the given context. If the vbo is not found in the given context, 0 is returned.
     */
    public int getVBOID(final Object glContext) {
        if (_vboIdCache != null && _vboIdCache.containsKey(glContext)) {
            return _vboIdCache.get(glContext);
        }
        return 0;
    }

    /**
     * Removes any vbo id from this buffer's data for the given OpenGL context.
     * 
     * @param glContext
     *            the object representing the OpenGL context a vbo would belong to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return the id removed
     */
    public int removeVBOID(final Object glContext) {
        if (_vboIdCache != null) {
            return _vboIdCache.remove(glContext);
        } else {
            return -1;
        }
    }

    /**
     * Sets the id for a vbo based on this buffer's data in regards to the given OpenGL context.
     * 
     * @param glContextRep
     *            the object representing the OpenGL context a vbo belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @param vboId
     *            the vbo id of a vbo. To be valid, this must be greater than 0.
     * @throws IllegalArgumentException
     *             if vboId is less than or equal to 0.
     */
    public void setVBOID(final Object glContextRep, final int vboId) {
        if (vboId <= 0) {
            throw new IllegalArgumentException("vboId must be > 0");
        }

        if (_vboIdCache == null) {
            _vboIdCache = new ContextIdReference<AbstractBufferData<T>>(this, _vboRefQueue);
        }
        _vboIdCache.put(glContextRep, vboId);
    }

    public VBOAccessMode getVboAccessMode() {
        return vboAccessMode;
    }

    public void setVboAccessMode(final VBOAccessMode vboAccessMode) {
        this.vboAccessMode = vboAccessMode;
    }

    public boolean isNeedsRefresh() {
        return needsRefresh;
    }

    public void setNeedsRefresh(final boolean needsRefresh) {
        this.needsRefresh = needsRefresh;
    }

    public static void cleanAllVBOs(final Renderer deleter) {
        final Multimap<Object, Integer> idMap = ArrayListMultimap.create();

        // gather up expired vbos... these don't exist in our cache
        gatherGCdIds(idMap);

        // Walk through the cached items and delete those too.
        for (final AbstractBufferData<?> buf : _identityCache.keySet()) {
            if (buf._vboIdCache != null) {
                if (Constants.useMultipleContexts) {
                    final Set<Object> contextObjects = buf._vboIdCache.getContextObjects();
                    for (final Object o : contextObjects) {
                        // Add id to map
                        idMap.put(o, buf.getVBOID(o));
                    }
                } else {
                    idMap.put(ContextManager.getCurrentContext().getGlContextRep(), buf.getVBOID(null));
                }
            }
        }

        handleVBODelete(deleter, idMap);
    }

    /**
     * Clean any VBO ids from the hardware, using the given Renderer object to do the work immediately, if given. If
     * not, we will delete in the next execution of the appropriate context's game task render queue.
     * 
     * @param deleter
     *            the Renderer to use. If null, execution will not occur immediately.
     */
    public static void cleanExpiredVBOs(final Renderer deleter) {
        final Multimap<Object, Integer> idMap = ArrayListMultimap.create();

        // gather up expired vbos...
        gatherGCdIds(idMap);

        // send to be deleted (perhaps on next render.)
        handleVBODelete(deleter, idMap);
    }

    @SuppressWarnings("unchecked")
    private static void gatherGCdIds(final Multimap<Object, Integer> idMap) {
        // Pull all expired vbos from ref queue and add to an id multimap.
        ContextIdReference<AbstractBufferData<?>> ref;
        while ((ref = (ContextIdReference<AbstractBufferData<?>>) _vboRefQueue.poll()) != null) {
            if (Constants.useMultipleContexts) {
                final Set<Object> contextObjects = ref.getContextObjects();
                for (final Object o : contextObjects) {
                    // Add id to map
                    idMap.put(o, ref.get(o));
                }
            } else {
                idMap.put(ContextManager.getCurrentContext().getGlContextRep(), ref.get(null));
            }
            ref.clear();
        }
    }

    private static void handleVBODelete(final Renderer deleter, final Multimap<Object, Integer> idMap) {
        Object currentGLRef = null;
        // Grab the current context, if any.
        if (deleter != null && ContextManager.getCurrentContext() != null) {
            currentGLRef = ContextManager.getCurrentContext().getGlContextRep();
        }
        // For each affected context...
        for (final Object glref : idMap.keySet()) {
            // If we have a deleter and the context is current, immediately delete
            if (deleter != null && glref.equals(currentGLRef)) {
                deleter.deleteVBOs(idMap.get(glref));
            }
            // Otherwise, add a delete request to that context's render task queue.
            else {
                GameTaskQueueManager.getManager(ContextManager.getContextForRef(glref)).render(
                        new RendererCallable<Void>() {
                            public Void call() throws Exception {
                                getRenderer().deleteVBOs(idMap.get(glref));
                                return null;
                            }
                        });
            }
        }
    }
}
