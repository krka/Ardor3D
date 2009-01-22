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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>GameTask</code> is used in <code>GameTaskQueue</code> to manage tasks that have yet to be accomplished.
 */
public class GameTask<V> implements Future<V> {
    private static final Logger logger = Logger.getLogger(GameTask.class.getName());

    private final Callable<V> callable;

    private V result;
    private ExecutionException exception;
    private boolean cancelled, finished;
    private final ReentrantLock stateLock = new ReentrantLock();
    private final Condition finishedCondition = stateLock.newCondition();

    public GameTask(final Callable<V> callable) {
        this.callable = callable;
    }

    public boolean cancel(final boolean mayInterruptIfRunning) {
        // TODO mayInterruptIfRunning was ignored in previous code, should this param be removed?
        stateLock.lock();
        try {
            if (result != null) {
                return false;
            }
            cancelled = true;

            finishedCondition.signalAll();

            return true;
        } finally {
            stateLock.unlock();
        }
    }

    public V get() throws InterruptedException, ExecutionException {
        stateLock.lock();
        try {
            while (!isDone()) {
                finishedCondition.await();
            }
            if (exception != null) {
                throw exception;
            }
            return result;
        } finally {
            stateLock.unlock();
        }
    }

    public V get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        stateLock.lock();
        try {
            if (!isDone()) {
                finishedCondition.await(timeout, unit);
            }
            if (exception != null) {
                throw exception;
            }
            if (result == null) {
                throw new TimeoutException("Object not returned in time allocated.");
            }
            return result;
        } finally {
            stateLock.unlock();
        }
    }

    public boolean isCancelled() {
        stateLock.lock();
        try {
            return cancelled;
        } finally {
            stateLock.unlock();
        }
    }

    public boolean isDone() {
        stateLock.lock();
        try {
            return finished || cancelled || (exception != null);
        } finally {
            stateLock.unlock();
        }
    }

    public Callable<V> getCallable() {
        return callable;
    }

    public void invoke() {
        try {
            final V tmpResult = callable.call();

            stateLock.lock();
            try {
                result = tmpResult;
                finished = true;

                finishedCondition.signalAll();
            } finally {
                stateLock.unlock();
            }
        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "invoke()", "Exception", e);

            stateLock.lock();
            try {
                exception = new ExecutionException(e);

                finishedCondition.signalAll();
            } finally {
                stateLock.unlock();
            }
        }
    }

}
