/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example;

import com.ardor3d.framework.FrameHandler;
import com.google.inject.Inject;

public class GameThread extends Thread implements Exit {
    private final FrameHandler frameWork;
    private volatile boolean exit = false;

    @Inject
    public GameThread(final FrameHandler frameWork) {
        this.frameWork = frameWork;
    }

    @Override
    public void run() {
        try {
            frameWork.init();

            while (!exit) {
                frameWork.updateFrame();
            }
        } catch (final Throwable t) {
            System.err.println("Throwable caught in MainThread - exiting");
            t.printStackTrace(System.err);
        }
    }

    public void exit() {
        exit = true;
    }
}
