/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.util;

import java.util.Map;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

/**
 * This class essentially just wraps a S->T HashMap, providing extra logging when a T is not found, or duplicate T
 * objects are added.
 */
public class LoggingMap<S, T> {

    /** our class logger */
    private static final Logger logger = Logger.getLogger(LoggingMap.class.getName());

    /** Our map of values. */
    protected final Map<S, T> _wrappedMap = Maps.newHashMap();

    /** A default value to return if a key is requested that does not exist. Defaults to null. */
    private T defaultValue = null;

    /** If true, we'll log anytime we set a key/value where the key already existed in the map. Defaults to true. */
    private boolean logOnReplace = true;

    /** If true, we'll log anytime we try to retrieve a value by a key that is not in the map. Defaults to true. */
    private boolean logOnMissing = true;

    /**
     * Add a value to the store. Logs a warning if a value by the same key was already in the store and logOnReplace is
     * true.
     * 
     * @param key
     *            the key to add.
     * @param value
     *            the value to add.
     */
    public void put(final S key, final T value) {
        if (_wrappedMap.put(key, value) != null) {
            if (isLogOnReplace()) {
                LoggingMap.logger.warning("Replaced value in map with same key. " + key);
            }
        }
    }

    /**
     * Retrieves a value from our store by key. Logs a warning if a value by that key is not found and logOnMissing is
     * true. If missing, defaultValue is returned.
     * 
     * @param key
     *            the key of the value to find.
     * @return the associated value, or null if none is found.
     */
    public T get(final S key) {
        final T value = _wrappedMap.get(key);
        if (value == null) {
            if (isLogOnMissing()) {
                LoggingMap.logger.warning("Value not found with key: " + key + " Returning defaultValue: "
                        + defaultValue);
            }
            return defaultValue;
        }
        return value;
    }

    /**
     * Removes the mapping for the given key.
     * 
     * @param key
     *            the key of the value to remove.
     * @return the previously associated value, or null if none was found.
     */
    public T remove(final S key) {
        return _wrappedMap.remove(key);
    }

    /**
     * @return the number of key-value pairs stored in this object.
     */
    public int size() {
        return _wrappedMap.size();
    }

    public void setDefaultValue(final T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setLogOnReplace(final boolean logOnReplace) {
        this.logOnReplace = logOnReplace;
    }

    public boolean isLogOnReplace() {
        return logOnReplace;
    }

    public void setLogOnMissing(final boolean logOnMissing) {
        this.logOnMissing = logOnMissing;
    }

    public boolean isLogOnMissing() {
        return logOnMissing;
    }
}
