/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.util.Timer;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Scopes;

/**
 * This Guice configuration module should initialize all system bindings to their default values. This should make it
 * easy for 95% of the users to just take those defaults and run, and those who a different configuration can copy this
 * example but not use it.
 */
public final class ArdorModule extends AbstractModule {

    public ArdorModule() {}

    @Override
    protected void configure() {
        bind(Timer.class).in(Scopes.SINGLETON);
        bind(FrameHandler.class).in(Scopes.SINGLETON);
        bind(LogicalLayer.class).in(Scopes.SINGLETON);
    }

    /**
     * Adds a method interceptor that checks that any call to a method annotated with @MainThread is actually made from
     * the main thread. Should probably only be done during testing as it will have some effect on performance.
     * 
     * @param injector
     *            injector to fetch instances from
     */
    public void injectInterceptors(final Injector injector) {
    // TODO: this crashes with an NPE in Guice now - they have made some changes that breaks this way of doing
    // things. For now, it is probably best to wait until 2.0 is out, that should make this workaround unnecessary
    // final ThreadInterceptor threadInterceptor = injector.getInstance(ThreadInterceptor.class);
    //
    // bindInterceptor(Matchers.any(), Matchers.annotatedWith(MainThread.class), threadInterceptor);
    }
}
