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

import com.ardor3d.example.Exit;
import com.ardor3d.example.GameThread;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.Updater;
import com.ardor3d.input.Key;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * This is an example of a game-specific guice configuration module that specifies stuff that is always going to be
 * different for each game. Users are expected to write their own game-specific configuration modules.
 */
public class RotatingCubeModule extends AbstractModule {

    @Override
    protected void configure() {
        // the only specific configuration for a simple 'game' like this is to point out the class
        // that handles the game-specific logic. That is all 'normal' users of the API should need to do.
        bind(GameThread.class).in(Scopes.SINGLETON);
        bind(Exit.class).to(GameThread.class);
        bind(DefaultScene.class).in(Scopes.SINGLETON);
        bind(Scene.class).to(DefaultScene.class);
        bind(Updater.class).to(RotatingCubeGame.class).in(Scopes.SINGLETON);
        bind(Key.class).toInstance(Key.T);
    }
}
