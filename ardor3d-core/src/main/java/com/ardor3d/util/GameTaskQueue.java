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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <code>GameTaskQueue</code> is a simple queueing system to enqueue tasks that need to be accomplished in the OpenGL
 * thread and get back a Future object to be able to retrieve a return from the Callable that was passed in.
 * 
 * @see Future
 * @see Callable
 */
public class GameTaskQueue {

    public static final String RENDER = "render";
    public static final String UPDATE = "update";

    private final ConcurrentLinkedQueue<GameTask<?>> queue = new ConcurrentLinkedQueue<GameTask<?>>();
    private final AtomicBoolean executeAll = new AtomicBoolean();

	// Default execution time is 0, which means only 1 task will be executed at a time.
    private long _executionTime = 0;

    /**
     * The state of this <code>GameTaskQueue</code> if it will execute all enqueued Callables on an execute invokation.
     * 
     * @return boolean
     */
    public boolean isExecuteAll() {
        return executeAll.get();
    }

    /**
     * Sets the executeAll boolean value to determine if when execute() is invoked if it should simply execute one
     * Callable, or if it should invoke all. This defaults to false to keep the game moving more smoothly.
     * 
     * @param executeAll
     */
    public void setExecuteAll(final boolean executeAll) {
        this.executeAll.set(executeAll);
        if (executeAll == true) {
            _executionTime = Integer.MAX_VALUE;
        }
    }

    /**
     * Sets the minimum amount of time the queue will execute tasks per frame. If this is set, the execute() loop will
     * execute as many tasks as it can before the execution window threshold is passed. Any remaining tasks will be
     * executed in the following frame.
     * 
     * @param msecs
     */
    public void setExecutionTime(final int msecs) {
        _executionTime = msecs;
        executeAll.set(true);
    }

    /**
     * min time queue is permitted to execute tasks per frame
     * 
     * @return -1 if executeAll is false, else min time allocated for task execution per frame
     */
    public long getExecutionTime() {
        if (executeAll.get() == false) {
            return -1;
        }
        return _executionTime;
    }

    /**
     * Adds the Callable to the internal queue to invoked and returns a Future that wraps the return. This is useful for
     * checking the status of the task as well as being able to retrieve the return object from Callable asynchronously.
     * 
     * @param <V>
     * @param callable
     * @return
     */
    public <V> Future<V> enqueue(final Callable<V> callable) {
        final GameTask<V> task = new GameTask<V>(callable);
        queue.add(task);
        return task;
    }

    /**
     * This method should be invoked in the update() or render() method inside the main game to make sure the tasks are
     * invoked in the OpenGL thread.
     */
    public void execute() {
        final long beginTime = System.currentTimeMillis();
        long elapsedTime;
        GameTask<?> task = queue.poll();
        do {
            if (task == null) {
                return;
            }
            while (task.isCancelled()) {
                task = queue.poll();
                if (task == null) {
                    return;
                }
            }
            task.invoke();
            elapsedTime = System.currentTimeMillis() - beginTime;
        } while ((executeAll.get()) && (elapsedTime < _executionTime) && ((task = queue.poll()) != null));
    }
}
