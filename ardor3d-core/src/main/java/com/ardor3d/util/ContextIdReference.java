/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
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

    private final Map<Object, Integer> _idCache;
    private int _singleContextId;

    public ContextIdReference(final T reference, final ReferenceQueue<? super T> queue) {
        super(reference, queue);
        if (Constants.useMultipleContexts) {
            _idCache = new MapMaker().initialCapacity(2).weakKeys().makeMap();
        } else {
            _idCache = null;
        }
        REFS.add(this);
    }

    public boolean containsKey(final Object glContext) {
        if (Constants.useMultipleContexts) {
            return _idCache.containsKey(glContext);
        } else {
            return true;
        }
    }

    public int get(final Object glContext) {
        if (Constants.useMultipleContexts) {
            return _idCache.get(glContext);
        } else {
            return _singleContextId;
        }
    }

    public int remove(final Object glContext) {
        if (Constants.useMultipleContexts) {
            final Integer i = _idCache.remove(glContext);
            if (i == null) {
                return -1;
            } else {
                return i;
            }
        } else {
            final int r = _singleContextId;
            _singleContextId = 0;
            return r;
        }
    }

    public void put(final Object glContext, final int id) {
        if (Constants.useMultipleContexts) {
            _idCache.put(glContext, id);
        } else {
            _singleContextId = id;
        }
    }

    public Set<Object> getContextObjects() {
        if (Constants.useMultipleContexts) {
            return _idCache.keySet();
        } else {
            return null;
        }
    }

    @Override
    public void clear() {
        super.clear();
        _singleContextId = 0;
        REFS.remove(this);
    }
}
