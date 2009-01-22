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

import com.ardor3d.example.GameThread;
import com.ardor3d.example.LwjglModule;
import com.ardor3d.framework.ArdorModule;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameWork;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.lwjgl.LwjglCanvas;
import com.ardor3d.input.FocusWrapper;
import com.ardor3d.input.KeyboardWrapper;
import com.ardor3d.input.MouseWrapper;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.LogicalLayer;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Stage;

public class NativeRotatingCubeMain {

    public static void main(final String[] args) {

        final Module ardorModule = new ArdorModule();
        final Module lwjglModule = new LwjglModule();
        final Module gameModule = new RotatingCubeModule();
        final DisplaySettings displaySettings = new DisplaySettings(800, 600, 0, 0, 0, 8, 0, 0, false, false);
        final Provider<DisplaySettings> settingsProvider = new Provider<DisplaySettings>() {
            public DisplaySettings get() {
                return displaySettings;
            }
        };

        // create a Guice injector using the default ArdorModule and a game-specific RotatingCubeModule.
        // In the upcoming Guice 2.0, it will be possible to override modules, which is probably even nicer than
        // the way this is now, but this is still pretty much OK. It's possible to use an anonymous class instead of
        // the named module below.
        final Injector injector = Guice.createInjector(Stage.PRODUCTION, ardorModule, lwjglModule, gameModule,
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(DisplaySettings.class).toProvider(settingsProvider);
                    }
                });

        // final com.google.inject.Key<KeyboardWrapper<LwjglCanvas>> keyboardKey = Key.get(new
        // TypeLiteral<KeyboardWrapper<LwjglCanvas>>() {});
        // final com.google.inject.Key<MouseWrapper<LwjglCanvas>> mouseKey = Key.get(new
        // TypeLiteral<MouseWrapper<LwjglCanvas>>() {});
        // final com.google.inject.Key<FocusWrapper<LwjglCanvas>> focusKey = Key.get(new
        // TypeLiteral<FocusWrapper<LwjglCanvas>>() {});

        final LogicalLayer ll = injector.getInstance(LogicalLayer.class);
        final FrameWork frameWork = injector.getInstance(FrameWork.class);
        final GameThread gameThread = injector.getInstance(GameThread.class);
        final LwjglCanvas canvas = injector.getInstance(LwjglCanvas.class);
        final Updater updater = injector.getInstance(Updater.class);
        final KeyboardWrapper keyboardWrapper = injector.getInstance(KeyboardWrapper.class);
        final MouseWrapper mouseWrapper = injector.getInstance(MouseWrapper.class);
        final FocusWrapper focusWrapper = injector.getInstance(FocusWrapper.class);

        final PhysicalLayer physicalLayer = new PhysicalLayer(keyboardWrapper, mouseWrapper, focusWrapper);

        // physicalLayer.init();

        ll.registerInput(canvas, physicalLayer);
        frameWork.registerUpdater(updater);
        frameWork.registerCanvas(canvas);

        gameThread.start();

    }

}
