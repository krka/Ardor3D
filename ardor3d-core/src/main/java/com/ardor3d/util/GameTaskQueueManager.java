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

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import com.ardor3d.renderer.RenderContext;
import com.google.common.collect.MapMaker;

/**
 * <code>GameTaskQueueManager</code> is just a simple Singleton class allowing easy access to task queues.
 */
public final class GameTaskQueueManager {

    private static final Map<RenderContext, GameTaskQueueManager> _managers = new MapMaker().weakKeys().makeMap();
    private static final RenderContext DEFAULT_CONTEXT = new RenderContext(null, null);

    private final ConcurrentMap<String, GameTaskQueue> _managedQueues = new ConcurrentHashMap<String, GameTaskQueue>(2);

    public static GameTaskQueueManager getManager() {
        return getManager(DEFAULT_CONTEXT);
    }

    public static GameTaskQueueManager getManager(final RenderContext context) {
        final RenderContext key = context != null ? context : DEFAULT_CONTEXT;

        synchronized (_managers) {
            GameTaskQueueManager manager = _managers.get(key);
            if (manager == null) {
                manager = new GameTaskQueueManager();
                _managers.put(key, manager);

                if (key != DEFAULT_CONTEXT && _managers.containsKey(DEFAULT_CONTEXT)) {
                    _managers.get(DEFAULT_CONTEXT).moveTasksTo(manager);
                }
            }
            return manager;
        }
    }

    private GameTaskQueueManager() {
        addQueue(GameTaskQueue.RENDER, new GameTaskQueue());
        addQueue(GameTaskQueue.UPDATE, new GameTaskQueue());
    }

    public void addQueue(final String name, final GameTaskQueue queue) {
        _managedQueues.put(name, queue);
    }

    public GameTaskQueue getQueue(final String name) {
        return _managedQueues.get(name);
    }

    public void moveTasksTo(final GameTaskQueueManager manager) {
        for (final String key : _managedQueues.keySet()) {
            final GameTaskQueue q = manager.getQueue(key);
            if (q != null) {
                q.enqueueAll(_managedQueues.get(key));
            }
        }
    }

    /**
     * Clears all tasks from the queues managed by this manager.
     */
    public void clearTasks() {
        for (final GameTaskQueue q : _managedQueues.values()) {
            q.clear();
        }
    }

    /**
     * This method adds <code>callable</code> to the queue to be invoked in the update() method in the OpenGL thread.
     * The Future returned may be utilized to cancel the task or wait for the return object.
     * 
     * @param callable
     * @return Future<V>
     */

    public <V> Future<V> update(final Callable<V> callable) {
        return getQueue(GameTaskQueue.UPDATE).enqueue(callable);
    }

    /**
     * This method adds <code>callable</code> to the queue to be invoked in the render() method in the OpenGL thread.
     * The Future returned may be utilized to cancel the task or wait for the return object.
     * 
     * @param callable
     * @return Future<V>
     */

    public <V> Future<V> render(final Callable<V> callable) {
        return getQueue(GameTaskQueue.RENDER).enqueue(callable);
    }
}
