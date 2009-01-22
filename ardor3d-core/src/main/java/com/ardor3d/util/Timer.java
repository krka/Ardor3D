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

public class Timer {

    private static final long TIMER_RESOLUTION = 1000000000L;
    private static final double INVERSE_TIMER_RESOLUTION = 1.0 / TIMER_RESOLUTION;

    private long startTime;
    private long previousTime;
    private double tpf;
    private double fps;

    public Timer() {
        startTime = System.nanoTime();
    }

    public double getTimeInSeconds() {
        return getTime() * INVERSE_TIMER_RESOLUTION;
    }

    public long getTime() {
        return System.nanoTime() - startTime;
    }

    public long getResolution() {
        return TIMER_RESOLUTION;
    }

    public double getFrameRate() {
        return fps;
    }

    public double getTimePerFrame() {
        return tpf;
    }

    public void update() {
        tpf = (getTime() - previousTime) * INVERSE_TIMER_RESOLUTION;
        fps = 1.0 / tpf;
        previousTime = getTime();
    }

    public void reset() {
        startTime = System.nanoTime();
        previousTime = getTime();
    }
}
