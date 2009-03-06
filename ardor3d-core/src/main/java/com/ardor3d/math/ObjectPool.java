/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math;

public abstract class ObjectPool<T extends Poolable> {
    private final ThreadLocal<Stack<T>> _pool = new ThreadLocal<Stack<T>>() {
        @Override
        protected synchronized Stack<T> initialValue() {
            return new Stack<T>(_maxSize);
        }
    };

    private final int _maxSize;

    protected ObjectPool(final int maxSize) {
        _maxSize = maxSize;
    }

    protected abstract T newInstance();

    public final T fetch() {
        return _pool.get().empty() ? newInstance() : _pool.get().pop();
    }

    public final void release(final T obj) {
        if (obj != null && _pool.get().size() < _maxSize) {
            _pool.get().push(obj);
        }
    }

    public static <T extends Poolable> ObjectPool<T> create(final Class<T> clazz, final int maxSize) {
        return new ObjectPool<T>(maxSize) {
            @Override
            protected T newInstance() {
                try {
                    return clazz.newInstance();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private static final class Stack<T> {
        private final T[] array;
        private int size = 0;

        @SuppressWarnings("unchecked")
        public Stack(final int maxSize) {
            array = (T[]) new Object[maxSize];
        }

        public boolean empty() {
            return size == 0;
        }

        public int size() {
            return size;
        }

        public void push(final T element) {
            array[size++] = element;
        }

        public T peek() {
            return array[size - 1];
        }

        public T pop() {
            final T element = array[--size];
            array[size] = null;

            return element;
        }
    }
}
