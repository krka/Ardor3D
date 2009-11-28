/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.framework.lwjgl.LwjglCanvas;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.input.ControllerWrapper;
import com.ardor3d.input.FocusWrapper;
import com.ardor3d.input.KeyboardWrapper;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.MouseWrapper;
import com.ardor3d.input.lwjgl.LwjglControllerWrapper;
import com.ardor3d.input.lwjgl.LwjglKeyboardWrapper;
import com.ardor3d.input.lwjgl.LwjglMouseManager;
import com.ardor3d.input.lwjgl.LwjglMouseWrapper;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Guice configuration module for use with a single native LWJGL window.
 */
public class LwjglModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(KeyboardWrapper.class).to(LwjglKeyboardWrapper.class).in(Scopes.SINGLETON);
        bind(MouseWrapper.class).to(LwjglMouseWrapper.class).in(Scopes.SINGLETON);
        bind(FocusWrapper.class).to(LwjglCanvas.class).in(Scopes.SINGLETON);
        bind(ControllerWrapper.class).to(LwjglControllerWrapper.class).in(Scopes.SINGLETON);
        bind(LwjglCanvas.class).in(Scopes.SINGLETON);
        bind(NativeCanvas.class).to(LwjglCanvas.class);
        bind(CanvasRenderer.class).to(LwjglCanvasRenderer.class).in(Scopes.SINGLETON);
        bind(MouseManager.class).to(LwjglMouseManager.class).in(Scopes.SINGLETON);
    }
}
