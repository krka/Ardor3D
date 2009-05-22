/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.nio.Buffer;
import java.util.WeakHashMap;

import com.ardor3d.renderer.RenderContext;

public abstract class AbstractBufferData<T extends Buffer> {

    private final transient WeakHashMap<Object, Integer> _vboIdCache = new WeakHashMap<Object, Integer>();

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
     * Get the buffer holding the data.
     * 
     * @return the buffer
     */
    public T getBuffer() {
        return _buffer;
    }

    /**
     * Set the buffer holding the data. This method should only be used internally.
     * 
     * @param buffer
     *            the buffer to set
     */
    void setBuffer(final T buffer) {
        _buffer = buffer;
    }

    /**
     * @param glContext
     *            the object representing the OpenGL context a vbo belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return the vbo id of a vbo in the given context. If the vbo is not found in the given context, 0 is returned.
     */
    public int getVBOID(final Object glContext) {
        if (_vboIdCache.containsKey(glContext)) {
            return _vboIdCache.get(glContext);
        }
        return 0;
    }

    /**
     * Sets the id for a vbo based on this buffer's data in regards to the given OpenGL context.
     * 
     * @param glContext
     *            the object representing the OpenGL context a vbo belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @param vboId
     *            the vbo id of a vbo. To be valid, this must be greater than 0.
     * @throws IllegalArgumentException
     *             if vboId is less than or equal to 0.
     */
    public void setVBOID(final Object glContext, final int vboId) {
        if (vboId <= 0) {
            throw new IllegalArgumentException("vboId must be > 0");
        }

        _vboIdCache.put(glContext, vboId);
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
}
