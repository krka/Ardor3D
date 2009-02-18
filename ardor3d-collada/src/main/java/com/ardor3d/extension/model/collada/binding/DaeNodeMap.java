/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class DaeNodeMap<T extends DaeTreeNode> extends DaeTreeNode implements Collection<T> {
    private final HashMap<String, T> map;

    public DaeNodeMap() {
        map = new HashMap<String, T>();
    }

    public void preset(final Object parent) {
        // need to do this before instantiating any members of the rest of the collection
        registerParent(parent);
    }

    public boolean add(final T entry) {
        if (entry == null) {
            throw new NullPointerException("Null values not accepted in map!");
        }

        return map.put(entry.getId(), entry) != null;
    }

    public T get(final String id) {
        return map.get(id);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(final Object o) {
        // noinspection SuspiciousMethodCalls
        return map.containsValue(o);
    }

    public Iterator<T> iterator() {
        return map.values().iterator();
    }

    public Object[] toArray() {
        return map.values().toArray();
    }

    @SuppressWarnings("hiding")
    public <T> T[] toArray(final T[] a) {
        // noinspection SuspiciousToArrayCall
        return map.values().toArray(a);
    }

    public boolean remove(final Object o) {
        return map.remove(o) != null;
    }

    public boolean containsAll(final Collection<?> c) {
        return map.values().containsAll(c);
    }

    public boolean addAll(final Collection<? extends T> c) {
        boolean returnValue = false;

        for (final T item : c) {
            add(item);
            returnValue = true;
        }

        return returnValue;
    }

    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException("NOT IMPLENTED");
    }

    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    public void clear() {
        map.clear();
    }

}
