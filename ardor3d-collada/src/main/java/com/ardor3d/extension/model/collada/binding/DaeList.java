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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DaeList<T extends DaeTreeNode> extends DaeTreeNode implements Collection<T> {
    private final List<T> list;

    public DaeList() {
        list = new LinkedList<T>();
    }

    public void preset(final Object parent) {
        // need to do this before instantiating any members of the rest of the collection
        registerParent(parent);
    }

    public boolean add(final T entry) {
        if (entry == null) {
            throw new NullPointerException("Null values not accepted in library!");
        }

        return list.add(entry);
    }

    public T get(final int index) {
        return list.get(index);
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean contains(final Object o) {
        return list.contains(o);
    }

    public Iterator<T> iterator() {
        return list.iterator();
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public <S> S[] toArray(final S[] a) {
        return list.toArray(a);
    }

    public boolean remove(final Object o) {
        return list.remove(o);
    }

    public boolean containsAll(final Collection<?> c) {
        return list.containsAll(c);
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
        return list.removeAll(c);
    }

    public boolean retainAll(final Collection<?> c) {
        return list.retainAll(c);
    }

    public void clear() {
        list.clear();
    }

}
