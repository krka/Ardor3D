/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.MapMaker;

public class ContextIdReference<T> extends PhantomReference<T> {

    /**
     * Keep a strong reference to these objects until their reference is cleared.
     */
    @SuppressWarnings("unchecked")
    private static final List<ContextIdReference> REFS = new LinkedList<ContextIdReference>();

    private final Map<Object, Integer> _idCache = new MapMaker().weakKeys().makeMap();

    public ContextIdReference(final T reference, final ReferenceQueue<? super T> queue) {
        super(reference, queue);
        REFS.add(this);
    }

    public boolean containsKey(final Object glContext) {
        return _idCache.containsKey(glContext);
    }

    public int get(final Object glContext) {
        return _idCache.get(glContext);
    }

    public int remove(final Object glContext) {
        return _idCache.remove(glContext);
    }

    public void put(final Object glContext, final int id) {
        _idCache.put(glContext, id);
    }

    public Set<Object> getContextObjects() {
        return _idCache.keySet();
    }

    @Override
    public void clear() {
        super.clear();
        REFS.remove(this);
    }
}
