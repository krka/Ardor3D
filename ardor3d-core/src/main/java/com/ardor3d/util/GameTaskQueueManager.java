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

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

/**
 * <code>GameTaskQueueManager</code> is just a simple Singleton class allowing easy access to task queues.
 */
public final class GameTaskQueueManager {

    private static final GameTaskQueueManager MANAGER_INSTANCE = new GameTaskQueueManager();

    protected final ConcurrentMap<String, GameTaskQueue> managedQueues = new ConcurrentHashMap<String, GameTaskQueue>(2);

    public static GameTaskQueueManager getManager() {
        return MANAGER_INSTANCE;
    }

    private GameTaskQueueManager() {
        addQueue(GameTaskQueue.RENDER, new GameTaskQueue());
        addQueue(GameTaskQueue.UPDATE, new GameTaskQueue());
    }

    public void addQueue(final String name, final GameTaskQueue queue) {
        managedQueues.put(name, queue);
    }

    public GameTaskQueue getQueue(final String name) {
        return managedQueues.get(name);
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
