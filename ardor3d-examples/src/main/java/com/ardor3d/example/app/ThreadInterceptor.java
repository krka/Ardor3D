/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.app;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.ardor3d.annotation.MainThread;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * An interceptor that checks that thread invocations are being made on the main thread only.
 */
@Singleton
public final class ThreadInterceptor implements MethodInterceptor {
    private final Thread theThread;

    @Inject
    public ThreadInterceptor(@MainThread final Provider<Thread> threadProvider) {
        theThread = threadProvider.get();
    }

    public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
        if (!(theThread == Thread.currentThread())) {
            throw new IllegalStateException("Attempt at calling method: " + methodInvocation.getMethod()
                    + " from thread: " + Thread.currentThread());
        }

        return methodInvocation.proceed();
    }
}
