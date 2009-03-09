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
import com.ardor3d.framework.jogl.JoglCanvas;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.input.FocusWrapper;
import com.ardor3d.input.KeyboardWrapper;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.MouseWrapper;
import com.ardor3d.input.awt.AwtFocusWrapper;
import com.ardor3d.input.awt.AwtKeyboardWrapper;
import com.ardor3d.input.awt.AwtMouseManager;
import com.ardor3d.input.awt.AwtMouseWrapper;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import java.awt.*;

/**
 * Guice configuration module for use with a single Jogl window.
 */
public class JoglModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(FocusWrapper.class).to(AwtFocusWrapper.class).in(Scopes.SINGLETON);
        bind(KeyboardWrapper.class).to(AwtKeyboardWrapper.class).in(Scopes.SINGLETON);
        bind(MouseWrapper.class).to(AwtMouseWrapper.class).in(Scopes.SINGLETON);
        bind(MouseManager.class).to(AwtMouseManager.class).in(Scopes.SINGLETON);
        bind(JoglCanvas.class).in(Scopes.SINGLETON);
        bind(Component.class).to(JoglCanvas.class);
        bind(NativeCanvas.class).to(JoglCanvas.class);
        bind(CanvasRenderer.class).to(JoglCanvasRenderer.class).in(Scopes.SINGLETON);
    }
}
