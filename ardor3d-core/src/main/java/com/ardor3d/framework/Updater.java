/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

import com.ardor3d.annotation.MainThread;

/**
 * This class should maybe be named StateUpdater, and then the View class could analogously be named ViewUpdater.
 * Anyway, the purpose of this class is to own the update phase. There should be a default implementation that is good
 * enough for most needs, but by making it pluggable, people can change its behavior. It would probably be nice to
 * provide a multi-threaded Updater implementation, or maybe that could be included in the DefaultUpdater. I think
 * multithreading is for a little later.
 */
public interface Updater {
    @MainThread
    public void init();

    @MainThread
    public void update(final double tpf);
}
